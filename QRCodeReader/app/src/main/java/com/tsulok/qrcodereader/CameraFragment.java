package com.tsulok.qrcodereader;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.tsulok.qrcodereader.common.NamedFragment;
import com.tsulok.qrcodereader.helper.CameraHelper;
import com.tsulok.qrcodereader.helper.UIHelper;
import com.tsulok.qrcodereader.utils.AutoFitTextureView;

import java.util.ArrayList;

public class CameraFragment extends NamedFragment implements IQRFound, ISettingsLoaded{

    private CameraHelper cameraHelper;

    private boolean isPhotoMode = true;
    private boolean isAutomaticMode = true;
    private ArrayList<Integer> supportedExposureList;
    private int selectedExpPosition = 0;
    private ArrayList<Integer> supportedIsoList;
    private int selectedIsoPosition = 0;

    private MenuItem switchMenu;
    private MenuItem switchModeMenu;
    private AutoFitTextureView mTextureView;
    private ImageButton captureBtn;
    private TextView qrLastTxt;
    private View photoSettingsView;

    // Iso settings
    private View isoSettingsContainer;
    private TextView actualIsoValueTxt;
    private TextView maxIsoValueTxt;
    private ImageButton prevIsoValueBtn;
    private ImageButton nextIsoValueBtn;
    private TextView isoSeparatorTxt;

    // Exposure settings
    private View expoSettingsContainer;
    private TextView actualExpValueTxt;
    private TextView maxExpValueTxt;
    private ImageButton prevExpValueBtn;
    private ImageButton nextExpValueBtn;

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
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_camera, menu);
        switchMenu = menu.findItem(R.id.action_mode_switch);
        switchModeMenu = menu.findItem(R.id.action_mode_manual_switch);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_mode_switch:
                isPhotoMode = !isPhotoMode;
                changeCameraMode();
                return true;
            case R.id.action_mode_manual_switch:
                 isAutomaticMode = !isAutomaticMode;
                changeAutomaticMode();
                return true;
            default:
                return onOptionsItemSelected(item);
        }
    }

    @Override
    public void inflateObjects(View v) {
        mTextureView = (AutoFitTextureView) v.findViewById(R.id.texture);
        captureBtn = (ImageButton) v.findViewById(R.id.capture);
        qrLastTxt = (TextView) v.findViewById(R.id.qr_data);
        photoSettingsView = v.findViewById(R.id.photoSettings);

        isoSettingsContainer = v.findViewById(R.id.isoSettings);
        actualIsoValueTxt = (TextView) isoSettingsContainer.findViewById(R.id.actualData);
        maxIsoValueTxt = (TextView) isoSettingsContainer.findViewById(R.id.maxValue);
        prevIsoValueBtn = (ImageButton) isoSettingsContainer.findViewById(R.id.prevValue);
        nextIsoValueBtn = (ImageButton) isoSettingsContainer.findViewById(R.id.nextValue);
        isoSeparatorTxt = (TextView) isoSettingsContainer.findViewById(R.id.separator);

        expoSettingsContainer = v.findViewById(R.id.exposureTimeSettings);
        actualExpValueTxt = (TextView) expoSettingsContainer.findViewById(R.id.actualData);
        maxExpValueTxt = (TextView) expoSettingsContainer.findViewById(R.id.maxValue);
        prevExpValueBtn = (ImageButton) expoSettingsContainer.findViewById(R.id.prevValue);
        nextExpValueBtn = (ImageButton) expoSettingsContainer.findViewById(R.id.nextValue);
    }

    @Override
    public void initObjects(View v) {
        cameraHelper = new CameraHelper(getActivity(), mTextureView, this, this);
        isoSeparatorTxt.setText("ISO ");
    }

    @Override
    public void initEventHandlers(View v) {
        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraHelper.takePicture();
            }
        });

        prevIsoValueBtn.setOnClickListener(new SettingsIsoChangeOnClick());
        nextIsoValueBtn.setOnClickListener(new SettingsIsoChangeOnClick());
        prevExpValueBtn.setOnClickListener(new SettingsExpChangeOnClick());
        nextExpValueBtn.setOnClickListener(new SettingsExpChangeOnClick());
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

    /**
     * Changes the camera mode to QR or Photo mode according to the latest settings
     */
    private void changeCameraMode(){
        switchMenu.setIcon(isPhotoMode ? R.drawable.icon_qr : R.drawable.icon_material_camera);
        switchModeMenu.setVisible(isPhotoMode);
        UIHelper.makeToast(isPhotoMode ? R.string.mode_photo : R.string.mode_qr);
        cameraHelper.changeMode(isPhotoMode);

        if(isPhotoMode){
            qrLastTxt.setText(R.string.qr_nodata);
        }

        qrLastTxt.setVisibility(isPhotoMode ? View.GONE : View.VISIBLE);
        photoSettingsView.setVisibility(isPhotoMode ? View.VISIBLE : View.GONE);
    }

    private void changeAutomaticMode(){
        switchModeMenu.setIcon(isAutomaticMode ? R.drawable.icon_manual : R.drawable.icon_automatic);
        UIHelper.makeToast(isAutomaticMode ? R.string.mode_automatic : R.string.mode_manual);
        isoSettingsContainer.setVisibility(isAutomaticMode ? View.INVISIBLE : View.VISIBLE);
        expoSettingsContainer.setVisibility(isAutomaticMode ? View.INVISIBLE : View.VISIBLE);

        if(isAutomaticMode){
            cameraHelper.switchToAutoMode();
        } else {
            cameraHelper.switchToManualMode();
        }
    }

    @Override
    public void onFound(String data) {
        qrLastTxt.setText(data);
    }

    @Override
    public void onExposureTimeRangeLoaded(ArrayList<Integer> supportedExposures, int selectedPosition) {
        this.supportedExposureList = supportedExposures;
        selectedExpPosition = selectedPosition;
        actualExpValueTxt.setText("1");
        maxExpValueTxt.setText(Integer.toString(supportedExposureList.get(selectedExpPosition)));
    }

    @Override
    public void onIsoRangeLoaded(ArrayList<Integer> supportedIsoList, int selectedPosition) {
        this.supportedIsoList = supportedIsoList;
        selectedIsoPosition = selectedPosition;
        maxIsoValueTxt.setText(Integer.toString(supportedIsoList.get(selectedIsoPosition)));
    }

    private final class SettingsIsoChangeOnClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.nextValue:
                    if(selectedIsoPosition < supportedIsoList.size() - 1){
                        selectedIsoPosition++;
                        if(selectedIsoPosition == supportedIsoList.size() - 1){
                            nextIsoValueBtn.setImageDrawable(
                                    getResources().getDrawable(R.drawable.icon_navigation_next_disabled));
                        }
                        prevIsoValueBtn.setImageDrawable(
                                getResources().getDrawable(R.drawable.icon_navigation_prev));
                    }
                    break;
                case R.id.prevValue:
                    if(selectedIsoPosition > 0){
                        selectedIsoPosition--;
                        if(selectedIsoPosition == 0){
                            prevIsoValueBtn.setImageDrawable(
                                    getResources().getDrawable(R.drawable.icon_navigation_prev_disabled));
                        }
                        nextIsoValueBtn.setImageDrawable(
                                getResources().getDrawable(R.drawable.icon_navigation_next));
                    }
                    break;
            }
            maxIsoValueTxt.setText(
                    Integer.toString(supportedIsoList.get(selectedIsoPosition)));
            cameraHelper.setNewIso(supportedIsoList.get(selectedIsoPosition));
        }
    }

    private final class SettingsExpChangeOnClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.nextValue:
                    if(selectedExpPosition < supportedExposureList.size() - 1){
                        selectedExpPosition++;
                        if(selectedExpPosition == supportedExposureList.size() - 1){
                            nextExpValueBtn.setImageDrawable(
                                    getResources().getDrawable(R.drawable.icon_navigation_next_disabled));
                        }
                        prevExpValueBtn.setImageDrawable(
                                getResources().getDrawable(R.drawable.icon_navigation_prev));
                    }
                    break;
                case R.id.prevValue:
                    if(selectedExpPosition > 0){
                        selectedExpPosition--;
                        if(selectedExpPosition == 0){
                            prevExpValueBtn.setImageDrawable(
                                    getResources().getDrawable(R.drawable.icon_navigation_prev_disabled));
                        }
                        nextExpValueBtn.setImageDrawable(
                                getResources().getDrawable(R.drawable.icon_navigation_next));
                    }
                    break;
            }
            maxExpValueTxt.setText(
                    Integer.toString(supportedExposureList.get(selectedExpPosition)));
            cameraHelper.setNewExposureTime(supportedExposureList.get(selectedExpPosition));
        }
    }
}
