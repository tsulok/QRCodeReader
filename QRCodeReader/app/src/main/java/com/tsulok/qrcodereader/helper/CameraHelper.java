package com.tsulok.qrcodereader.helper;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.tsulok.qrcodereader.App;
import com.tsulok.qrcodereader.utils.AutoFitTextureView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraHelper {

    private static final String TAG = "CameraHelper";
    private CameraHelper instance;

    private CameraManager cameraManager;
    private Activity hostActivity;
    private AutoFitTextureView hostTextureView;

    /**
     * Listeners
     */
    private MyStateCallback mStateCallback;
    private MySurfaceTextureListener surfaceTextureListener;

    /**
     * For the camera preview
     */
    private CaptureRequest.Builder previewRequestBuilder;
    private CaptureRequest previewRequest;
    private CameraCaptureSession captureSession;

    /**
     * A Semaphore to prevent the app from exiting before closing the camera.
     */
    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    /**
     * Reference & ID of the current CameraDevice.
     */
    private String mCameraId;
    private CameraDevice cameraDevice;

    /**
     * The Size of camera preview.
     */
    private Size previewSize;

    /**
     * An additional thread & handler for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    /**
     * An {@link android.media.ImageReader} that handles still image capture.
     */
    private ImageReader imageReaderPreviewYUV;

    public CameraHelper(Activity hostActivity, AutoFitTextureView textureView){
        this.cameraManager = (CameraManager) App.getAppContext().getSystemService(Context.CAMERA_SERVICE);
        this.hostTextureView = textureView;
        this.hostActivity = hostActivity;
        this.mStateCallback = new MyStateCallback();
        this.surfaceTextureListener = new MySurfaceTextureListener();
    }

    public void handleOnResume(){
        startBackgroundThread();
        if(hostTextureView.isAvailable()){
            openCamera(hostTextureView.getWidth(), hostTextureView.getHeight());
        } else {
            hostTextureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    public void handleOnPause(){
        stopBackgroundThread();
        closeCamera();
    }

    /**
     * Basic camera functionality
     * *********************************************************************************************
     */

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private void setUpCameraOutputs(int width, int height) {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = cameraManager.getCameraCharacteristics(cameraId);

                // Don't use a front facing camera.
                if (characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CameraHelper.CompareSizesByArea());
//                imageReaderPreviewYUV = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
//                        ImageFormat.JPEG, /*maxImages*/2);
//                imageReaderPreviewYUV.setOnImageAvailableListener(
//                        new MyImageAvailableListener(), mBackgroundHandler);


                previewSize = CameraHelper.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);

//                imageReaderPreviewYUV = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
//                        ImageFormat.JPEG, /*maxImages*/2);
                imageReaderPreviewYUV = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                        ImageFormat.YUV_420_888, /*maxImages*/2);
                imageReaderPreviewYUV.setOnImageAvailableListener(
                        new MyImageAvailableListener(), mBackgroundHandler);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = App.getAppContext().getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    hostTextureView.setAspectRatio(
                            previewSize.getWidth(), previewSize.getHeight());
                } else {
                    hostTextureView.setAspectRatio(
                            previewSize.getHeight(), previewSize.getWidth());
                }

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Camera2API is used but not supported on the device.
            UIHelper.alert(hostActivity, "Error", "Device is not supported for Camera2API");
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = hostTextureView.getSurfaceTexture();
            // run for exception if not available
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            previewRequestBuilder
                    = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            previewRequestBuilder.addTarget(imageReaderPreviewYUV.getSurface());

            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReaderPreviewYUV.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == cameraDevice) {
                                return;
                            }
                            // When the session is ready, we start displaying the preview.
                            captureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
//                                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
//                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                // Finally, we start displaying the camera preview.
                                previewRequest = previewRequestBuilder.build();
                                captureSession.setRepeatingRequest(previewRequest,
                                        new MyCaptureCallback(), mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            UIHelper.makeToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == hostTextureView || null == previewSize || null == hostActivity) {
            return;
        }
        int rotation = hostActivity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        hostTextureView.setTransform(matrix);
    }

    /**
     * Opens the camera specified by mCameraId.
     */
    private void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != imageReaderPreviewYUV) {
                imageReaderPreviewYUV.close();
                imageReaderPreviewYUV = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    /**
     * Listener implementations
     * *********************************************************************************************
     */

    private final class MyImageAvailableListener implements ImageReader.OnImageAvailableListener{

        @Override
        public void onImageAvailable(final ImageReader reader) {
            mBackgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    // aquire the next JPEG image
                    Image image = reader.acquireNextImage();

                    reader.close();
                }
            });
        }
    }

    private final class MyCaptureCallback extends CameraCaptureSession.CaptureCallback{
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
//            Log.d(TAG, "TimeStamp: " + timestamp);
//            Log.d(TAG, "FrameNumber: " + frameNumber);
//            Log.d(TAG, "");
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        private boolean asd = true;

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            if(asd){
                asd = false;
                for (CaptureResult.Key<?> key : result.getKeys()) {
                    Log.d(TAG, key.getName());
                }
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            super.onCaptureSequenceAborted(session, sequenceId);
        }
    }

    private final class MySurfaceTextureListener implements TextureView.SurfaceTextureListener  {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    // private final  mStateCallback = new CameraDevice.StateCallback() {
    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final class MyStateCallback extends CameraDevice.StateCallback{

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            cameraOpenCloseLock.release();
            CameraHelper.this.cameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            CameraHelper.this.cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            CameraHelper.this.cameraDevice = null;
            if (null != hostActivity) {
                hostActivity.finish();
            }
        }
    };

    /**
     * Additional static helpers
     * *********************************************************************************************
     */

    /**
     * Compares two {@code Size}s based on their areas.
     */
    public static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    public static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }
}
