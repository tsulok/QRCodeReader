package com.tsulok.qrcodereader.helper;

import android.util.SparseIntArray;
import android.view.Surface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Constants used in {@code CameraHelper}
 */
public class CameraConstants {

    public static final long SEC_IN_NANO = 1000000000l;

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

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

    /**
     * Accepted and well known exposure times
     */
    public static final List<Integer> validExposureTimes =
            new ArrayList<Integer>(Arrays.asList(2, 4, 6, 8, 15, 30, 60, 100,
                    125, 250, 500, 750, 1000, 1500, 2000, 3000, 4000, 5000,
                    6000, 8000, 10000, 20000, 30000, 75000));

    /**
     * Accepted and well known iso list
     */
    public static final List<Integer> validIsoRanges =
            new ArrayList<>(Arrays.asList(40, 50, 80, 100, 200, 300,
                    400, 600, 800, 1000, 1600, 2000, 3200, 4000, 6400, 8000, 10000));
}
