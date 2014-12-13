package com.tsulok.qrcodereader;

import java.util.ArrayList;

public interface ISettingsLoaded {

    /**
     * Called when exposure range has been queried from the camera
     * @param supportedExposures The supported exposure list
     * @param selectedPosition The initial value position in the list
     */
    public void onExposureTimeRangeLoaded(ArrayList<Integer> supportedExposures, int selectedPosition);

    /**
     * Called when iso range has been queried from the camera
     * @param supportedIsoList The supported iso list
     * @param selectedPosition The initial value position in the list
     */
    public void onIsoRangeLoaded(ArrayList<Integer> supportedIsoList, int selectedPosition);
}
