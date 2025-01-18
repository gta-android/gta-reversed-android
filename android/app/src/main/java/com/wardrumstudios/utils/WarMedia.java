package com.wardrumstudios.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Process;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.nvidia.devtech.NvUtil;

public class WarMedia extends WarGamepad {
    private static final String TAG = "WarMedia";
    protected boolean AllowLongPressForExit;
    String DeviceCountry;
    public String DeviceLocale;
    boolean DoLog;
    boolean GameIsFocused;
    boolean IsInValidation;
    private boolean IsScreenPaused = false;
    int IsShowingBackMessage;
    protected int SpecialBuildType;
    protected boolean UseExpansionFiles;
    boolean UsingSounds;
    Activity activity;
    protected String apkFileName = "";
    protected int appStatusIcon;
    int availableMemory;
    public String baseDirectory;
    public String baseDirectoryRoot;
    protected int baseDisplayHeight;
    protected int baseDisplayWidth;
    protected int cachedSizeRead;
    protected boolean checkForMaxDisplaySize;
    int currentTempID;
    boolean downloadViewCreated;
    AlertDialog exitDialog;
    protected String expansionFileName = "";
    protected boolean hasTouchScreen;
    boolean isCompleting;
    boolean isPhone;
    private boolean isUserPresent = true;
    protected int lastNetworkAvailable;
    public LinearLayout llSplashView;
    private Locale locale;
    private final BroadcastReceiver mReceiver;
    private WifiManager mWifiManager;
    ActivityManager.MemoryInfo memInfo;
    int memoryThreshold;
    protected DisplayMetrics metrics;
    ActivityManager mgr;
    int[] myPid;
    private Vibrator myVib;
    private PowerManager.WakeLock myWakeLock;
    protected String patchFileName = "";
    float screenWidthInches;
    boolean skipSound;
    boolean soundLog;
    int totalMemory;
    long[][] vibrateEffects;
    public XAPKFile[] xAPKS = null;

    private native void initTouchSense(Context context);
    public native void NativeNotifyNetworkChange(int i);
    public native void setTouchSenseFilepath(String str);

    public WarMedia() {
         DoLog = !FinalRelease;
         skipSound = false;
         isCompleting = false;
         soundLog = false;
         SpecialBuildType = 0;
         activity = null;
         appStatusIcon = 0;
         UseExpansionFiles = false;
         AllowLongPressForExit = false;
         hasTouchScreen = true;
         isPhone = false;
         currentTempID = 100000;
         baseDirectory = Environment.getExternalStorageDirectory().getPath();
         baseDirectoryRoot = Environment.getExternalStorageDirectory().getPath();
         IsShowingBackMessage = 0;
         exitDialog = null;
         cachedSizeRead = 0;
         UsingSounds = false;
         memoryThreshold = 0;
         availableMemory = 0;
         totalMemory = 0;
         screenWidthInches = 0.0f;
         baseDisplayWidth = 1920;
         baseDisplayHeight = 1080;
         lastNetworkAvailable = -1;
         checkForMaxDisplaySize = false;
         mWifiManager = null;
         downloadViewCreated = false;
         GameIsFocused = false;
         mReceiver = new BroadcastReceiver() {
             public void onReceive(Context context, Intent intent) {
                 String action = intent.getAction();
                 if (!FinalRelease) {
                     Log.d(TAG, "BroadcastReceiver WarMedia " + action);
                 }

                 if (action.equals("android.intent.action.SCREEN_OFF")) {
                     if (!FinalRelease) {
                         Log.d(TAG, "BroadcastReceiver ACTION_SCREEN_OFF");
                     }

                     isUserPresent = false;
                 } else if (action.equals("android.intent.action.USER_PRESENT") || action.equals("android.intent.action.SCREEN_ON")) {
                     if (!FinalRelease) {
                         Log.d(TAG, "BroadcastReceiver " + action);
                     }

                     KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                     if (!FinalRelease) {
                         Log.d(TAG, "inKeyguardRestrictedInputMode " + keyguardManager.inKeyguardRestrictedInputMode());
                     }

                     if (!keyguardManager.inKeyguardRestrictedInputMode()) {
                         isUserPresent = true;
                         if (IsScreenPaused) {
                             IsScreenPaused = false;
                             if (viewIsActive) {
                                 resumeEvent();
                                 if (cachedSurfaceHolder != null) {
                                     cachedSurfaceHolder.setKeepScreenOn(true);
                                 }
                             }
                         }
                     }
                 } else if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                     NetworkChange();
                 } else if (DoLog) {
                     Log.d(TAG, "Received " + action);
                 }
             }
         };

         myVib = null;
         vibrateEffects = new long[][]{new long[]{0, 100, 100, 100, 100}, new long[]{0, 100, 50, 75, 100, 50, 100}, new long[]{0, 25, 50, 100, 50, 25, 100}, new long[]{0, 25, 50, 25, 100, 100, 100}, new long[]{0, 50, 50, 50, 50, 25, 100}};
         mgr = null;
         memInfo = null;
         myPid = null;
         DeviceLocale = "";
         DeviceCountry = "";
         locale = null;
         IsInValidation = false;
         llSplashView = null;
    }

    public static class XAPKFile {
        public final long mFileSize;
        public final int mFileVersion;
        public final boolean mIsMain;

        public XAPKFile(boolean isMain, int fileVersion, long fileSize) {
            mIsMain = isMain;
            mFileVersion = fileVersion;
            mFileSize = fileSize;
        }
    }

    void GetMaxDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        Log.d(TAG, "width=" + size.x + " height=" + size.y);
        if (maxDisplayWidth < size.x) {
            maxDisplayWidth = size.x;
            maxDisplayHeight = size.y;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode[] modes = new Display.Mode[0];

            modes = display.getSupportedModes();

            for (int i = 0; i < modes.length; i++) {
                Display.Mode mode = modes[i];
                if (maxDisplayWidth < mode.getPhysicalWidth()) {
                    maxDisplayWidth = mode.getPhysicalWidth();
                    maxDisplayHeight = mode.getPhysicalHeight();
                }
                Log.d(TAG, "mode " + i + " width=" + mode.getPhysicalWidth() + " height=" + mode.getPhysicalHeight());
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (isNativeApp) {
            super.onCreate(savedInstanceState);
        }

        if (DoLog) {
            Log.d(TAG, "**** onCreate");
        }

        ClearSystemNotification();
        GetGameBaseDirectory();

        GetGLExtensions = false;

        if (IsPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        metrics = getResources().getDisplayMetrics();
        Log.d(TAG, "Display Metrics Info:\n\tdensity:\t\t" + metrics.density + "\n\tdensityDPI:\t\t" + metrics.densityDpi + "\n\tscaledDensity:\t\t" + metrics.scaledDensity + "\n\twidthDPI:\t\t" + metrics.xdpi + "\n\theightDPI:\t\t" + metrics.ydpi + "\n\twidthPixels:\t\t" + metrics.widthPixels + "\n\theightPixels:\t\t" + metrics.heightPixels + "\n\tscreenlayout=" + getResources().getConfiguration().screenLayout);
        maxDisplayWidth = metrics.widthPixels;
        maxDisplayHeight = metrics.heightPixels;
        baseDisplayWidth = metrics.widthPixels;
        baseDisplayHeight = metrics.heightPixels;
        if (!IsPortrait() && metrics.widthPixels < metrics.heightPixels) {
            //noinspection SuspiciousNameCombination
            maxDisplayWidth = metrics.heightPixels;
            //noinspection SuspiciousNameCombination
            maxDisplayHeight = metrics.widthPixels;
            //noinspection SuspiciousNameCombination
            baseDisplayWidth = metrics.heightPixels;
            //noinspection SuspiciousNameCombination
            baseDisplayHeight = metrics.widthPixels;
        }

        if (Build.MODEL.startsWith("ADT")) {
            IsAndroidTV = true;
        }

        if (Build.MANUFACTURER.startsWith("NVIDIA") && Build.MODEL.startsWith("SHIELD Android TV")) {
            if (checkForMaxDisplaySize) {
                GetMaxDisplaySize();
            }

            isShieldTV = true;
        }

        NvUtil.getInstance().setActivity(this);
        NvUtil.getInstance().setAppLocalValue("STORAGE_ROOT", baseDirectory);
        NvUtil.getInstance().setAppLocalValue("STORAGE_ROOT_BASE", baseDirectoryRoot);

        hasTouchScreen = getResources().getConfiguration().touchscreen != 1;
        Log.d(TAG, "hastouchscreen " + hasTouchScreen + " touchscreen " + getResources().getConfiguration().touchscreen);

        activity = this;

        GetRealLocale();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        myWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "War:WarEngine");
        myWakeLock.setReferenceCounted(false);

        isPhone = IsPhone();
        screenWidthInches = ((float) metrics.widthPixels) / metrics.xdpi;
        if (isPhone) {
            if (screenWidthInches < 3.5f) {
                screenWidthInches = 3.5f;
            }

            if (screenWidthInches > 6.0f) {
                screenWidthInches = 6.0f;
            }
        } else {
            if (screenWidthInches < 6.0f) {
                screenWidthInches = 6.0f;
            }

            if (screenWidthInches > 10.0f) {
                screenWidthInches = 10.0f;
            }
        }

        int processors = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "availableProcessors " + processors + " cpu " + getNumberOfProcessors());

        GetMemoryInfo(true);

        if (!isNativeApp) {
            super.onCreate(savedInstanceState);
        }

        if (!CustomLoadFunction()) {
            localHasGameData();
        }

        NetworkChange();

        try {
            initTouchSense(this);
        } catch (UnsatisfiedLinkError e) {
            /* ~ */
        }
    }

    public boolean isTV() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == 4;
    }

    public boolean isWiFiAvailable() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }

        return mWifiManager != null && mWifiManager.isWifiEnabled();
    }

    public boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null || (activeNetworkInfo = connectivityManager.getActiveNetworkInfo()) == null) {
            return false;
        }

        return activeNetworkInfo.isConnected();
    }

    protected void localHasGameData() {
        if (!FinalRelease) {
            Log.d(TAG, "localHasGameData");
        }

        if (xAPKS == null || expansionFilesDelivered()) {
            AfterDownloadFunction();
        }
        else {
            Toast.makeText(this, "You do not have data or obb.", Toast.LENGTH_SHORT).show();
        }
    }

    public String GetGameBaseDirectory() {
        if (Environment.getExternalStorageState().equals("mounted")) {
            try {
                File f = getExternalFilesDir(null);
                String base = f.getAbsolutePath();
                baseDirectoryRoot = base.substring(0, base.indexOf("/Android"));
                return (f.getAbsolutePath() + "/");
            } catch (NullPointerException e) {
                /* ~ */
            }
        }

        ShowSDErrorDialog();
        return "";
    }

    void ShowSDErrorDialog() {
        Context context = this;
        runOnUiThread(() -> {
            exitDialog = new AlertDialog.Builder(context).setTitle("Cannot find storage. Is SDCard mounted?").setPositiveButton("Exit Game", (i, a) -> finish()).setCancelable(false).show();
            exitDialog.setCanceledOnTouchOutside(true);
        });
    }

    boolean expansionFilesDelivered() {
        File root = Environment.getExternalStorageDirectory();
        String path = root.toString() + "/Android/obb/" + getPackageName();

        expansionFileName = path + "/main." + xAPKS[0].mFileVersion + "." + getPackageName() + ".obb";
        patchFileName = path + "/patch." + xAPKS[1].mFileVersion + "." + getPackageName() + ".obb";

        return new File(expansionFileName).exists() && new File(patchFileName).exists();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (ResumeEventDone && isUserPresent && viewIsActive && !IsScreenPaused && !paused) {
            if (GameIsFocused && !hasFocus) {
                pauseEvent();
            } else if (!GameIsFocused && hasFocus) {
                resumeEvent();
            }

            GameIsFocused = hasFocus;
        }

        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if (DoLog) {
            Log.d(TAG, "Listener - onConfigurationChanged orient " + newConfig.orientation + " " + newConfig);
        }

        if (IsShowingKeyboard && newConfig.keyboard == 2 && 1 == newConfig.hardKeyboardHidden) {
            IsShowingKeyboard = false;
            imeClosed();
        }

        super.onConfigurationChanged(newConfig);
    }

    public void onLowMemory() {
        lowMemoryEvent();
    }

    protected void NetworkChange() {
         int curNetwork = isWiFiAvailable() ? 2 : isNetworkAvailable() ? 1 : 0;
         if (curNetwork != lastNetworkAvailable) {
             NativeNotifyNetworkChange(curNetwork);
             lastNetworkAvailable = curNetwork;
         }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.USER_PRESENT");
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.intent.action.SCREEN_ON");
        if (DoLog) {
            Log.d(TAG, "registerReceiver");
        }

        registerReceiver(mReceiver, filter);
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DoLog) {
            Log.d(TAG, "unregisterReceiver");
        }

        unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        if (DoLog) {
            Log.d(TAG, "**** onResume viewIsActive " + viewIsActive + " isUserPresent " + isUserPresent);
        }

        super.onResume();

        for (int i = 0; i < MAX_GAME_PADS; i++) {
            if (GamePads[i].active && GamePads[i].mogaController != null) {
                GamePads[i].mogaController.onResume();
            }
        }

        if (isUserPresent) {
            if (viewIsActive && ResumeEventDone) {
                resumeEvent();
                if (cachedSurfaceHolder != null) {
                    cachedSurfaceHolder.setKeepScreenOn(true);
                }
            }

            IsScreenPaused = false;
        }

        paused = false;
    }

    @SuppressWarnings("unused")
    public void VibratePhone(int numMilliseconds) {
        if (!FinalRelease) {
            Log.d(TAG, "VibratePhone " + numMilliseconds);
        }

        if (myVib == null) {
            myVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (myVib != null) {
            myVib.vibrate(numMilliseconds);
        }
    }

    @SuppressWarnings("unused")
    public void VibratePhoneEffect(int effect) {
        if (!FinalRelease) {
            Log.d(TAG, "VibratePhoneEffect " + effect);
        }

        if (myVib == null) {
            myVib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (myVib != null) {
            myVib.vibrate(vibrateEffects[effect], -1);
        }
    }

    @Override
    protected void onPause() {
        if (DoLog) {
            Log.d(TAG, "Listener -  onPause");
        }

        if (cachedSurfaceHolder != null) {
            cachedSurfaceHolder.setKeepScreenOn(false);
        }

        for (int i = 0; i < MAX_GAME_PADS; i++) {
            if (GamePads[i].active && GamePads[i].mogaController != null) {
                GamePads[i].mogaController.onPause();
            }
        }

        super.onPause();
        GetMemoryInfo(true);
        IsScreenPaused = true;
        paused = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (IsShowingBackMessage != 2) {
            return super.onTouchEvent(event);
        }

        if (DoLog) {
            Log.d(TAG, "onTouchEvent exitDialog " + exitDialog);
        }

        if (exitDialog != null) {
            exitDialog.dismiss();
        }

        IsShowingBackMessage = 0;
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!AllowLongPressForExit || keyCode != 4 || event.isAltPressed() || !event.isLongPress()) {
            return super.onKeyDown(keyCode, event);
        }

        IsShowingBackMessage = 1;
        if (DoLog) {
            Log.d(TAG, "ShowExitDialog KeyDown");
        }

        ShowExitDialog();
        return true;
    }

    void ShowExitDialog() {
        Context context = this;
        handler.post(() -> {
            exitDialog = new AlertDialog.Builder(context).setTitle("Press back again to exit").setOnKeyListener((dlg, KeyCode, event) -> {
                if (DoLog) {
                    Log.d(TAG, "ShowExitDialog onKey action " + event.getAction() + " IsShowingBackMessage " + IsShowingBackMessage + " KeyCode " + KeyCode);
                }

                if (IsShowingBackMessage == 2) {
                    IsShowingBackMessage = 0;
                    if (KeyCode == 4) {
                        finish();
                    } else {
                        dlg.dismiss();
                    }
                } else if (event.getAction() == 1) {
                    IsShowingBackMessage = 2;
                }
                return true;
            }).setCancelable(false).show();
            exitDialog.setCanceledOnTouchOutside(true);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (DoLog) {
            Log.d(TAG, "Listener - onStop");
        }

        super.onStop();
    }

    @Override
    protected void onRestart() {
        if (DoLog) {
            Log.d(TAG, "Listener - onRestart");
        }

        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        if (DoLog) {
            Log.d(TAG, "Listener - onDestroy isFinishing " + isFinishing());
        }

        Process.killProcess(Process.myPid());
        super.onDestroy();
        Process.killProcess(Process.myPid());
    }

    public void finish() {
        onDestroy();
        super.finish();
    }

    public int GetMemoryInfo(boolean allProcesses) {
        if (mgr == null) {
            mgr = (ActivityManager) super.getSystemService(Context.ACTIVITY_SERVICE);
            memInfo = new ActivityManager.MemoryInfo();
        }
        if (mgr == null) {
            Log.d(TAG, "GetMemoryInfo mgr NULL");
            return 0;
        }
        mgr.getMemoryInfo(memInfo);
        memoryThreshold = (int) (memInfo.threshold / 1024);
        availableMemory = (int) ((memInfo.availMem / 1024) / 1024);
        if (Build.VERSION.SDK_INT >= 16) {
            totalMemory = (int) ((memInfo.totalMem / 1024) / 1024);
        } else {
            totalMemory = 256;
        }
        if (allProcesses) {
            try {
                mgr.getRunningAppProcesses();
                List<ActivityManager.RunningAppProcessInfo> l = mgr.getRunningAppProcesses();
                int[] pids = new int[l.size()];
                for (int i = 0; i < l.size(); i++) {
                    pids[i] = l.get(i).pid;
                }
            } catch (Exception ex) {
                Log.d(TAG, "getRunningAppProcesses null " + ex.getMessage());
            }
        } else if (myPid != null) {
            mgr.getProcessMemoryInfo(myPid);
        }
        return (int) ((memInfo.availMem / 1024) / 1024);
    }

    public boolean IsPhone() {
        return (getResources().getConfiguration().screenLayout & 15) < 3;
    }

    public void ClearSystemNotification() {
        runOnUiThread(() -> ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll());
    }

    @SuppressWarnings("unused")
    public void MovieSetSkippable(boolean skippable) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void StopMovie() {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void PlayMovieInWindow(final String inFilename, int x, int y, int width, int height, final float inVolume, final int inOffset, final int inLength, int looping, boolean forceSize) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void PlayMovieInFile(String filename, float volume, int offset, int length) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void PlayMovie(String filename, float Volume) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public int IsMoviePlaying() {
        return 0;
    }

    public String GetConfigSetting(String key) {
        String value = getPreferences(0).getString(key, "");
        if (DoLog) {
            Log.d(TAG, "GetConfigSetting " + key + " value " + value);
        }
        return value;
    }

    public void SetConfigSetting(String key, String value) {
        SharedPreferences.Editor prefEditor = getPreferences(0).edit();
        prefEditor.putString(key, value);
        prefEditor.apply();

        if (DoLog) {
            Log.d(TAG, "SetConfigSetting " + key + " value " + value);
        }
    }

    @SuppressWarnings("unused")
    public String OBFU_GetDeviceID() {
        return UUID.randomUUID().toString();
    }

    public void GetRealLocale() {
        Locale langLocal = Locale.getDefault();
        String lang = Locale.getDefault().getLanguage();
        String locale2 = Locale.getDefault().getDisplayLanguage();
        DeviceCountry = Locale.getDefault().getCountry();
        if (DoLog) {
            Log.d(TAG, "SetLocale getDefault " + lang + " langLocal " + langLocal + " locale " + locale2 + " DeviceCountry " + DeviceCountry);
        }
        DeviceLocale = lang;
    }

    @SuppressWarnings("unused")
    public int GetDeviceLocale() {
        String lang = DeviceLocale;
        if (lang.equals("en")) {
            return 0;
        }
        if (lang.equals("fr")) {
            return 1;
        }
        if (lang.equals("de")) {
            return 2;
        }
        if (lang.equals("it")) {
            return 3;
        }
        if (lang.equals("es")) {
            return 4;
        }
        if (lang.equals("ja")) {
            return 5;
        }
        if (lang.equals("ko")) {
            return 6;
        }
        if (lang.equals("sv")) {
            return 7;
        }
        if (lang.equals("no") || lang.equals("nb") || lang.equals("nn")) {
            return 8;
        }
        if (lang.equals("ru")) {
            return 9;
        }
        return 0;
    }

    @SuppressWarnings("unused")
    public int GetLocale() {
        String lang = GetConfigSetting("currentLanguage");
        if (lang.equals("en")) {
            return 0;
        }
        if (lang.equals("fr")) {
            return 1;
        }
        if (lang.equals("de")) {
            return 2;
        }
        if (lang.equals("it")) {
            return 3;
        }
        if (lang.equals("es")) {
            return 4;
        }
        if (lang.equals("ja")) {
            return 5;
        }
        if (lang.equals("ko")) {
            return 6;
        }
        if (lang.equals("sv")) {
            return 7;
        }
        if (lang.equals("no") || lang.equals("nb") || lang.equals("nn")) {
            return 8;
        }
        return 0;
    }

    public void SetLocale(String lStr) {
        GetRealLocale();
        if (DoLog) {
            Log.d(TAG, "SetLocale " + lStr);
        }
        String lang = GetConfigSetting("currentLanguage");
        if (DoLog) {
            Log.d(TAG, "SetLocale oldlang " + lang);
        }

        String countyStr = "";
        if (lStr.equals("en")) {
            if (DeviceCountry.equals("GB")) {
                countyStr = DeviceCountry.equals("GB") ? "GB" : "US";
            }
        }

        locale = new Locale(lStr, countyStr);
        Locale.setDefault(locale);
        SetConfigSetting("currentLanguage", lStr);
    }

    public void RestoreCurrentLanguage() {
        String lang = GetConfigSetting("currentLanguage");
        String countyStr = "";
        if (!lang.equals("")) {
            if (lang.equals("en")) {
                countyStr = DeviceCountry.equals("GB") ? "GB" : "US";
            }

            locale = new Locale(lang, countyStr);
            Locale.setDefault(locale);
        }
    }

    @SuppressWarnings("unused")
    public void SetLocale(int newLang) {
        String lStr;
        switch (newLang) {
            case 1:
                lStr = "fr";
                break;
            case 2:
                lStr = "de";
                break;
            case 3:
                lStr = "it";
                break;
            case 4:
                lStr = "es";
                break;
            case 5:
                lStr = "ja";
                break;
            case 6:
                lStr = "ko";
                break;
            case 7:
                lStr = "sv";
                break;
            case 8:
                lStr = "no";
                break;
            default:
                lStr = "en";
                break;
        }

        if (DoLog) {
            Log.d(TAG, "SetLocale " + newLang + " lStr " + lStr);
        }

        String lang = GetConfigSetting("currentLanguage");
        if (DoLog) {
            Log.d(TAG, "SetLocale oldlang " + lang);
        }

        locale = new Locale(lStr);
        Locale.setDefault(locale);
        SetConfigSetting("currentLanguage", lStr);
    }

    @SuppressWarnings("unused")
    public boolean DeleteFile(String filename) {
        return false;
    }

    @SuppressWarnings("unused")
    public boolean FileRename(String oldfile, String newfile, int overWrite) {
        return false;
    }

    @SuppressWarnings("unused")
    public String FileGetArchiveName(int type) {
        switch (type) {
            case 0:
                return apkFileName;
            case 1:
                return expansionFileName;
            case 2:
                return patchFileName;
            default:
                return "";
        }
    }

    public boolean IsTV() {
        return IsAndroidTV;
    }

    @SuppressWarnings("unused")
    public String GetAndroidBuildinfo(int index) {
        switch (index) {
            case 0:
                return Build.MANUFACTURER;
            case 1:
                return Build.PRODUCT;
            case 2:
                return Build.MODEL;
            case 3:
                return Build.HARDWARE;
            default:
                return "UNKNOWN";
        }
    }

    @SuppressWarnings("unused")
    public int GetDeviceInfo(int index) {
        switch (index) {
            case 0:
                return getNumberOfProcessors();
            case 1:
                return hasTouchScreen ? 1 : 0;
            default:
                return -1;
        }
    }

    @SuppressWarnings("unused")
    public int GetDeviceType() {
        int i = 0;
        Log.d(TAG, "Build info version device  " + Build.DEVICE);
        Log.d(TAG, "Build MANUFACTURER  " + Build.MANUFACTURER);
        Log.d(TAG, "Build BOARD  " + Build.BOARD);
        Log.d(TAG, "Build DISPLAY  " + Build.DISPLAY);
        Log.d(TAG, "Build CPU_ABI  " + Build.CPU_ABI);
        Log.d(TAG, "Build CPU_ABI2  " + Build.CPU_ABI2);
        Log.d(TAG, "Build HARDWARE  " + Build.HARDWARE);
        Log.d(TAG, "Build MODEL  " + Build.MODEL);
        Log.d(TAG, "Build PRODUCT  " + Build.PRODUCT);

        int isTegra = glVendor.contains("NVIDIA") ? 2 : 0;

        int numProcs = getNumberOfProcessors() * 4;
        int mem = availableMemory * 64;
        if (isPhone) {
            i = 1;
        }

        int ret = i + isTegra + numProcs + mem;
        if (!FinalRelease) {
            Log.d(TAG, "renderer '" + glVendor + "' ret=" + ret);
        }

        return ret;
    }

    private int getNumberOfProcessors() {
        try {
            return Runtime.getRuntime().availableProcessors();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 1;
    }

    @SuppressWarnings("unused")
    public void ShowKeyboard(int show) {
        if (getResources().getConfiguration().hardKeyboardHidden != 1) {
            InputMethodManager myImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (show != 0) {
                myImm.toggleSoftInput(2, 0);
                IsShowingKeyboard = true;
                return;
            }

            View view = getCurrentFocus();
            if (view != null) {
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            myImm.toggleSoftInput(0, 0);
            IsShowingKeyboard = false;
            Log.d(TAG, "hideSystemUI");
            hideSystemUI();
        }
    }

    @SuppressWarnings("unused")
    public boolean IsKeyboardShown() {
        return IsShowingKeyboard;
    }

    public String GetPackageName(String appname) {
        try {
            return getPackageManager().getPackageInfo(appname, 0).packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    @SuppressWarnings("unused")
    public boolean IsAppInstalled(String appname) {
        try {
            getPackageManager().getPackageInfo(appname, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    @SuppressWarnings("unused")
    public void OpenLink(String link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(link));
        startActivity(browserIntent);
    }

    @SuppressWarnings("unused")
    public boolean IsCloudAvailable() {
        return false;
    }

    @SuppressWarnings("unused")
    public void LoadAllGamesFromCloud() {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public String LoadGameFromCloud(int slot, byte[] array) {
        return null;
    }

    @SuppressWarnings("unused")
    public void SaveGameToCloud(int slot, byte[] array, int numbytes) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public boolean NewCloudSaveAvailable(int slot) {
        return false;
    }

    @SuppressWarnings("unused")
    public void MovieKeepAspectRatio(boolean keep) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void MovieSetTextScale(int scale) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void MovieClearText(boolean isSubtitle) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void MovieSetText(String text, boolean DisplayNow, boolean isSubtitle) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void MovieDisplayText(boolean display) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public int GetSpecialBuildType() {
        return SpecialBuildType;
    }

    public boolean CustomLoadFunction() {
        return false;
    }

    public void AfterDownloadFunction() {
        DoResumeEvent();
    }

    @SuppressWarnings("unused")
    public void SendStatEvent(String eventId, boolean timedEvent) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void SendTimedStatEventEnd(String eventId) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public void SendStatEvent(String eventId, String paramName, String paramValue, boolean timedEvent) {
        /* ~ */
    }

    @SuppressWarnings("unused")
    public int GetTotalMemory() {
        return totalMemory;
    }

    @SuppressWarnings("unused")
    public float GetScreenWidthInches() {
        return screenWidthInches;
    }

    @SuppressWarnings("unused")
    public int GetLowThreshhold() {
        return memoryThreshold;
    }

    @SuppressWarnings("unused")
    public int GetAvailableMemory() {
        GetMemoryInfo(false);
        return availableMemory;
    }

    @SuppressWarnings("unused")
    public String GetAppId() {
        return getPackageName();
    }

    @SuppressWarnings("unused")
    public void ScreenSetWakeLock(boolean enable) {
        if (enable) {
            myWakeLock.acquire(10 * 60 * 1000L /* 10 minutes */);
        }
        else {
            myWakeLock.release();
        }
    }

    @SuppressWarnings("unused")
    public void CreateTextBox(int id, int x, int y, int x2, int y2) {
        /* ~ */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case 8001:
                if (grantResults.length > 0 && grantResults[0] == 0) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        break;
                    }

                    // CheckIfNeedsBluetoothPermission();
                    localHasGameData();
                    break;
                }

                Log.d(TAG, "Exiting App 8001");
                finish();
                break;
            case 8002:
                if (grantResults.length > 0 && grantResults[0] == 0) {
                    localHasGameData();
                    break;
                }

                Log.d(TAG, "Exiting App 8002");
                finish();
                break;
            default:
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean CheckIfNeedsReadPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // CheckIfNeedsBluetoothPermission(activity);
            return false;
        }

        delaySetContentView = true;

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 8001);
            return true;
        }

        return false;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean CheckIfNeedsBluetoothPermission(Activity activity) {
        delaySetContentView = true;

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 8002);
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unused")
    public boolean ConvertToBitmap(byte[] data, int length) {
        return false;
    }

    @SuppressWarnings("unused")
    public boolean ServiceAppCommand(String cmd, String args) {
        return false;
    }

    @SuppressWarnings("unused")
    public int ServiceAppCommandValue(String cmd, String args) {
        return 0;
    }

    @SuppressWarnings("unused")
    public boolean ServiceAppCommandInt(String cmd, int args) {
        return false;
    }
}
