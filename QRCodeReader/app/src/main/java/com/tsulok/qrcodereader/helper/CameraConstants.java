package com.tsulok.qrcodereader.helper;

import android.util.SparseIntArray;
import android.view.Surface;

/**
 * Constants used in {@code CameraHelper}
 */
public class CameraConstants {

    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Camera state: Showing camera preview.
     */
    public static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    public static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    public static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    public static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    public static final int STATE_PICTURE_TAKEN = 4;
}
