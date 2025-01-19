package com.gta.reversed;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.Constants;
import com.wardrumstudios.utils.WarMedia;
import java.io.File;

import com.bytedance.shadowhook.ShadowHook;

public class GTASA extends WarMedia {
    private static final String TAG = "GTASA";
    public static GTASA gtasaSelf;
    static String vmVersion;
    boolean UseExpansionPack = false;

    static {
        ShadowHook.init(new ShadowHook.ConfigBuilder()
                .setMode(ShadowHook.Mode.UNIQUE)
                .build());

        Log.i(TAG, "**** Loading SO's");
        try {
            vmVersion = System.getProperty("java.vm.version");
            Log.i(TAG, "vmVersion " + vmVersion);
            //System.loadLibrary("ImmEmulatorJ");
            System.loadLibrary("SCAnd");
            System.loadLibrary("GTASA");
            System.loadLibrary("reGTA");
        } catch (ExceptionInInitializerError | UnsatisfiedLinkError e) {
            Log.e(TAG, e.getMessage());
        }
    }
    @Override
    public void onCreate(Bundle bundle) {
        Log.i(TAG, "**** onCreate");
        gtasaSelf = this;
        this.expansionFileName = "main.8." + getPackageName() + ".obb";
        this.patchFileName = "patch.8." + getPackageName() + ".obb";
        this.apkFileName = GetPackageName(getPackageName());
        Log.i(TAG, "apkFileName " + this.apkFileName);
        this.baseDirectory = GetGameBaseDirectory();
        this.AllowLongPressForExit = true;
        String[] strArr = {"anim", "audio", Constants.ScionAnalytics.MessageType.DATA_MESSAGE, "models", "texdb"};
        for (int i = 0; i < 5; i++) {
            String str = strArr[i];
            File file = new File(this.baseDirectory + str);
            if (file.exists() && file.isDirectory()) {
                Log.i(TAG, "Using lite data.");
                this.UseExpansionPack = false;
            } else {
                Log.i(TAG, "Using obb.");
                this.UseExpansionPack = true;
            }
        }
        if (this.UseExpansionPack) {
            this.xAPKS = new WarMedia.XAPKFile[2];
            this.xAPKS[0] = new WarMedia.XAPKFile(true, 8, 1967561852L);
            this.xAPKS[1] = new WarMedia.XAPKFile(false, 8, 625313014L);
        }
        this.wantsMultitouch = true;
        this.wantsAccelerometer = true;
        RestoreCurrentLanguage();
        super.onCreate(bundle);
        SetReportPS3As360(false);

        Toast.makeText(this, "reGTA Started", Toast.LENGTH_SHORT).show();

    }

    @Override
    public boolean ServiceAppCommand(String str, String str2) {
        if (str.equalsIgnoreCase("SetLocale")) {
            SetLocale(str2);
        }
        return false;
    }

    @Override
    public int ServiceAppCommandValue(String str, String str2) {
        if (str.equalsIgnoreCase("GetDownloadBytes")) {
            return 0;
        }
        if (str.equalsIgnoreCase("GetDownloadState")) {
            return 4;
        }
        return (str.equalsIgnoreCase("GetNetworkState") && isNetworkAvailable()) ? 1 : 0;
    }

    @Override
    public void onStart() {
        Log.i(TAG, "**** onStart");
        super.onStart();
    }

    @Override
    public void onRestart() {
        Log.i(TAG, "**** onRestart");
        super.onRestart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "**** onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "**** onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "**** onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "**** onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean CustomLoadFunction() {
        return CheckIfNeedsReadPermission(gtasaSelf);
    }

    public static void staticEnterSocialClub() {
        gtasaSelf.EnterSocialClub();
    }

    public static void staticExitSocialClub() {
        gtasaSelf.ExitSocialClub();
    }

    public void EnterSocialClub() {
        Log.i(TAG, "**** EnterSocialClub");
    }

    public void ExitSocialClub() {
        Log.i(TAG, "**** ExitSocialClub");
    }
}
