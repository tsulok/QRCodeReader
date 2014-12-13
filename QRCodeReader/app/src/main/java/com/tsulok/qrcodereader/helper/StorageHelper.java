package com.tsulok.qrcodereader.helper;

import android.os.Environment;
import android.util.Log;

import com.tsulok.qrcodereader.App;

import java.io.File;

public class StorageHelper {
    private static final String TAG = "StorageHelper";

    /**
     * Checks if external storage is available for read and write
     *
     * @return
     */
    public static boolean isExternalStorageWritable() {
        final String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if external storage is available to at least read
     *
     * @return
     */
    public static boolean isExternalStorageReadable() {
        final String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Creates a file path
     *
     * @param file
     * @return
     */
    public static File getExternalStorageFile(final String file) {
        return new File(App.getAppContext().getExternalFilesDir(null), file);
    }

    /**
     * Get path for the file on external storage. If external storage is not currently mounted this will fail.
     *
     * @param file
     */
    public static void deleteExternalStorageFile(final File file) {
        if ((file != null) && StorageHelper.hasExternalStorageFile(file)) {
            file.delete();
            Log.d(StorageHelper.TAG, "File deleted: " + file.getName());
        }
    }

    /**
     * Get path for the file on external storage. If external storage is not currently mounted this will fail.
     *
     * @param file
     * @return
     */
    public static boolean hasExternalStorageFile(final File file) {
        return (file != null) && file.exists();
    }
}
