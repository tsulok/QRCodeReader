package com.tsulok.qrcodereader;

import android.app.Application;
import android.content.Context;

public class App extends Application{

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this.getApplicationContext();
    }

    public static Context getAppContext() {
        return appContext;
    }
}
