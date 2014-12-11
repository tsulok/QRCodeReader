package com.tsulok.qrcodereader;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.tsulok.qrcodereader.common.NamedFragment;
import com.tsulok.qrcodereader.helper.CameraHelper;
import com.tsulok.qrcodereader.utils.AutoFitTextureView;

public class CameraFragment extends NamedFragment {

    private AutoFitTextureView mTextureView;
    private CameraHelper cameraHelper;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_camera;
    }

    @Override
    public int getNameId() {
        return R.string.title_main;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void inflateObjects(View v) {
        mTextureView = (AutoFitTextureView) v.findViewById(R.id.texture);
    }

    @Override
    public void initObjects(View v) {
        cameraHelper = new CameraHelper(getActivity(), mTextureView);
    }

    @Override
    public void initEventHandlers(View v) {

    }

    @Override
    public void onResume() {
        super.onResume();
        cameraHelper.handleOnResume();
    }

    @Override
    public void onPause() {
        cameraHelper.handleOnPause();
        super.onPause();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
