package com.regta.core;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class REGTA extends GTASA {

    private static native void initreGTA();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        activity = this;

        try {
            initreGTA();
        } catch (UnsatisfiedLinkError e) {
            if (e.getMessage() != null) {
                Log.e("Error", e.getMessage());
            }
        }

        super.onCreate(savedInstanceState);
    }

    public static Activity activity;
}
