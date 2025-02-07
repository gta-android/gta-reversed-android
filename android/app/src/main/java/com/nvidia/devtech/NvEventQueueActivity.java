//----------------------------------------------------------------------------------
// File:            libs\src\com\nvidia\devtech\NvEventQueueActivity.java
// Samples Version: Android NVIDIA samples 2
// Email:           tegradev@nvidia.com
// Forum:           http://developer.nvidia.com/tegra/forums/tegra-forums/android-development
//
// Copyright 2009-2010 NVIDIA(R) Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//----------------------------------------------------------------------------------
package com.nvidia.devtech;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL11;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;
import android.widget.LinearLayout;

/**
 * A base class used to provide a native-code event-loop interface to an
 * application.  This class is designed to be subclassed by the application
 * with very little need to extend the Java.  Paired with its native static-link
 * library, libnv_event.a, this package makes it possible for native applciations
 * to avoid any direct use of Java code.  In addition, input and other events are
 * automatically queued and provided to the application in native code via a
 * classic event queue-like API.  EGL functionality such as bind/unbind and swap
 * are also made available to the native code for ease of application porting.
 * Please see the external SDK documentation for an introduction to the use of
 * this class and its paired native library.
 */
public abstract class NvEventQueueActivity
        extends Activity
        implements SensorEventListener {

    private static final int EGL_RENDERABLE_TYPE = 0x3040;
    private static final int EGL_OPENGL_ES2_BIT = 0x0004;
    private static final int EGL_OPENGL_ES3_BIT = 0x0040;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    protected SurfaceHolder holder;
    protected SurfaceHolder movieTextHolder;
    protected SurfaceView movieTextView;
    SharedPreferences prefs;
    public View splashView;
    protected SurfaceHolder vidHolder;
    protected SurfaceView vidView;
    protected SurfaceView view;
    
    public boolean isNativeApp = false;

    public Handler handler = null;

    public boolean paused = false;

    protected boolean wantsMultitouch = false;

    protected boolean supportPauseResume = true;

    protected boolean GetGLExtensions = false;
    protected boolean isFailedError = false;
    protected boolean delayInputForStore = false;
    protected long lastResumeTime = 0;
    private boolean inputPaused = false;
    private AssetManager assetMgr = null;
    int SwapBufferSkip = 0;
    protected boolean IsShowingKeyboard = false;
    boolean capsLockOn = false;
    protected boolean isShieldTV = false;
    protected int maxDisplayWidth = 1920;
    protected int maxDisplayHeight = 1080;
    public boolean delaySetContentView = false;
    public boolean Use2Touches = true;
    protected String movieText = "Loading...";
    int vidViewWidth = 1024;
    int vidViewHeight = 600;
    int mVideoWidth = 640;
    int mVideoHeight = 480;
    boolean InVideview = false;
    ViewGroup.LayoutParams myLayout = new ViewGroup.LayoutParams(-2, -2);

    //accelerometer related
    protected boolean wantsAccelerometer = false;
    protected SensorManager mSensorManager = null;
    protected int mSensorDelay = SensorManager.SENSOR_DELAY_GAME; //other options: SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_NORMAL and SensorManager.SENSOR_DELAY_UI
    protected Display display = null;

    EGL10 egl = null;
    protected GL11 gl = null;

    private boolean ranInit = false;
    protected EGLSurface eglSurface = null;
    protected EGLDisplay eglDisplay = null;
    protected EGLContext eglContext = null;
    protected EGLConfig eglConfig = null;

    protected SurfaceView warView = null;
    protected boolean vidViewCreated = false;
    protected boolean vidViewIsActive = false;
    protected boolean viewIsActive = false;
    protected boolean movieIsStopping = false;
    protected boolean noVidSurface = true;
    protected boolean movieIsStarting = false;
    protected boolean creatingMediaplayer = false;
    protected boolean movieTextViewCreated = false;
    protected boolean movieTextViewIsActive = false;
    protected boolean movieTextViewFirstDestroy = false;
    public String glExtensions = null;
    protected String glVendor = null;
    protected String glRenderer = null;
    protected String glVersion = null;

    public SurfaceHolder cachedSurfaceHolder = null;
    protected int surfaceWidth = 0;
    protected int surfaceHeight = 0;

    protected boolean ResumeEventDone = false;
    protected boolean UseSubtitles = false;
    public boolean HasGLExtensions = false;
    boolean waitingForResume = false;

    protected class gSurfaceView extends SurfaceView {
        NvEventQueueActivity myActivity;

        public gSurfaceView(Context context) {
            super(context);
            myActivity = null;
        }

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && IsShowingKeyboard) {
                myActivity.imeClosed();
                IsShowingKeyboard = false;
            }
            return false;
        }
    }

    public View GetMainView() {
        return view;
    }

    public void nativeCrashed() {
        System.err.println("nativeCrashed");
        if (prefs != null) {
            try {
                System.err.println("saved game was:\n" + prefs.getString("savedGame", ""));
            } catch (Exception e) {
            }
        }
        new RuntimeException("crashed here (native trace should follow after the Java trace)").printStackTrace();
    }

    /**
     * Helper class used to pass raw data around.
     */
    public class RawData {
        /**
         * The actual data bytes.
         */
        public byte[] data;
        /**
         * The length of the data.
         */
        public int length;
    }

    /**
     * Helper class used to pass a raw texture around.
     */
    public class RawTexture extends RawData {
        /**
         * The width of the texture.
         */
        public int width;
        /**
         * The height of the texture.
         */
        public int height;
    }

    /**
     * Helper function to load a file into a {@link NvEventQueueActivity.RawData} object.
     * It'll first try loading the file from "/data/" and if the file doesn't
     * exist there, it'll try loading it from the assets directory inside the
     * .APK file. This is to allow the files inside the apk to be overridden
     * or not be part of the .APK at all during the development phase of the
     * application, decreasing the size needed to be transmitted to the device
     * between changes to the code.
     *
     * @param filename The file to load.
     * @return The RawData object representing the file's fully loaded data,
     * or null if loading failed.
     */
    public RawData loadFile(String filename) {
        InputStream is = null;
        RawData ret = new RawData();
        try {
            try {
                is = new FileInputStream("/data/" + filename);
            } catch (Exception e) {
                try {
                    is = getAssets().open(filename);
                } catch (Exception e2) {
                }
            }
            int size = is.available();
            ret.length = size;
            ret.data = new byte[size];
            is.read(ret.data);
        } catch (IOException ioe) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return ret;
    }

    /**
     * Helper function to load a texture file into a {@link NvEventQueueActivity.RawTexture} object.
     * It'll first try loading the texture from "/data/" and if the file doesn't
     * exist there, it'll try loading it from the assets directory inside the
     * .APK file. This is to allow the files inside the apk to be overridden
     * or not be part of the .APK at all during the development phase of the
     * application, decreasing the size needed to be transmitted to the device
     * between changes to the code.
     * <p>
     * The texture data will be flipped and bit-twiddled to fit being loaded directly
     * into OpenGL ES via the glTexImage2D call.
     *
     * @param filename The file to load.
     * @return The RawTexture object representing the texture's fully loaded data,
     * or null if loading failed.
     */
    public RawTexture loadTexture(String filename) {
        RawTexture ret = new RawTexture();
        try {
            InputStream is = null;
            try {
                is = new FileInputStream("/data/" + filename);
            } catch (Exception e) {
                try {
                    is = getAssets().open(filename);
                } catch (Exception e2) {
                }
            }

            Bitmap bmp = BitmapFactory.decodeStream(is);
            ret.width = bmp.getWidth();
            ret.height = bmp.getHeight();
            int[] pixels = new int[bmp.getWidth() * bmp.getHeight()];
            bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

            // Flip texture
            int[] tmp = new int[bmp.getWidth()];
            final int w = bmp.getWidth();
            final int h = bmp.getHeight();
            for (int i = 0; i < h >> 1; i++) {
                System.arraycopy(pixels, i * w, tmp, 0, w);
                System.arraycopy(pixels, (h - 1 - i) * w, pixels, i * w, w);
                System.arraycopy(tmp, 0, pixels, (h - 1 - i) * w, w);
            }

            // Convert from ARGB -> RGBA and put into the byte array
            ret.length = pixels.length * 4;
            ret.data = new byte[ret.length];
            int pos = 0;
            int bpos = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++, pos++) {
                    int p = pixels[pos];
                    ret.data[bpos++] = (byte) ((p >> 16) & 0xff);
                    ret.data[bpos++] = (byte) ((p >> 8) & 0xff);
                    ret.data[bpos++] = (byte) ((p >> 0) & 0xff);
                    ret.data[bpos++] = (byte) ((p >> 24) & 0xff);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public boolean IsPortrait() {
        return false;
    }

    public void setGameWindowSize(int w, int h) {
        if ((IsPortrait() && w > h) || (!IsPortrait() && h > w)) {
            setWindowSize(h, w);
        } else {
            setWindowSize(w, h);
        }
    }

    /**
     * Function called when app requests accelerometer events.
     * Applications need/should NOT overide this function - it will provide
     * accelerometer events into the event queue that is accessible
     * via the calls in nv_event.h
     *
     * @param values0: values[0] passed to onSensorChanged(). For accelerometer: Acceleration minus Gx on the x-axis.
     * @param values1: values[1] passed to onSensorChanged(). For accelerometer: Acceleration minus Gy on the y-axis.
     * @param values2: values[2] passed to onSensorChanged(). For accelerometer: Acceleration minus Gz on the z-axis.
     * @return True if the event was handled.
     */
    public native boolean accelerometerEvent(float values0, float values1, float values2);

    /**
     * The following indented function implementations are defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    public native void cleanup();

    public native void imeClosed();

    public native boolean init(boolean z);

    public native void jniNvAPKInit(Object obj);

    public native boolean keyEvent(int action, int keycode, int unicodeChar, int metaState,  KeyEvent event);

    public native void lowMemoryEvent();

    public native boolean multiTouchEvent(int action, int count, int x0, int y0, int x1, int y1, MotionEvent event);

    public native boolean multiTouchEvent4(int action, int count, int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, MotionEvent motionEvent);

    public native void pauseEvent();

    public native void quitAndWait();

    public native void resumeEvent();

    public native void setWindowSize(int w, int h);

    public native boolean touchEvent(int action, int x, int y, MotionEvent event);

    /**
     * END indented block, see in comment at top of block
     */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("**** NvEventQueueActivity onCreate");
        NvUtil.getInstance().setActivity(this);
        super.onCreate(savedInstanceState);
        handler = new Handler();
        if (!isFailedError) {
            if (wantsAccelerometer && (mSensorManager == null))
                mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            NvAPKFileHelper.getInstance().setContext(this);
            NvAPKFile file = new NvAPKFile();
            file.is = null;
            try {
                assetMgr = getAssets();
                jniNvAPKInit(assetMgr);
            } catch (UnsatisfiedLinkError e) {
            }
            display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
            systemInit();
        }
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    @Override
    protected void onResume() {
        System.out.println("**** NvEventQueueActivity onResume");
        lastResumeTime = SystemClock.uptimeMillis() + 300;
        super.onResume();
        if (mSensorManager != null)
            mSensorManager.registerListener(
                    this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    mSensorDelay);
        paused = false;
        inputPaused = false;
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    @Override
    protected void onRestart() {
        System.out.println("**** NvEventQueueActivity onRestart");
        super.onRestart();
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    @Override
    protected void onPause() {
        System.out.println("**** NvEventQueueActivity onPause");
        super.onPause();
        if (ResumeEventDone) {
            pauseEvent();
        }
        paused = true;
        inputPaused = true;
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    @Override
    protected void onStop() {
        System.out.println("**** NvEventQueueActivity onStop");
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        super.onStop();
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application should *probably* not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     * <p>
     * NOTE: An application may need to override this if the app has an
     * in-process instance of the Service class and the native side wants to
     * keep running. The app would want to execute the content of the
     * if(supportPauseResume) clause when it is time to exit.
     */
    @Override
    public void onDestroy() {
        System.out.println("**** NvEventQueueActivity onDestroy");
        if (supportPauseResume) {
            quitAndWait();
            finish();
        }
        super.onDestroy();
        systemCleanup();
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Auto-generated method stub
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            float roll = 0.0f;
            float pitch = 0.0f;
            switch (display.getRotation()) {
                case Surface.ROTATION_0:
                    roll = -event.values[0];
                    pitch = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    roll = event.values[1];
                    pitch = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    roll = event.values[0];
                    pitch = event.values[1];
                    break;
                case Surface.ROTATION_270:
                    roll = -event.values[1];
                    pitch = event.values[0];
                    break;
            }
            accelerometerEvent(roll, pitch, event.values[2]);
        }

    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);
        if (inputPaused || (delayInputForStore && event.getEventTime() < lastResumeTime)) {
            return false;
        }
        if (delayInputForStore && event.getEventTime() > lastResumeTime + 5000) {
            delayInputForStore = false;
        }
        if (!ret) {
            if (wantsMultitouch) {
                int count = 0;
                int x1 = 0, y1 = 0, x2 = 0, y2 = 0, x3 = 0, y3 = 0, x4 = 0, y4 = 0;
                // marshal up the data.
                int numEvents = event.getPointerCount();
                for (int i = 0; i < numEvents; i++) {
                    if (count == 0) {
                        x1 = (int) event.getX(i);
                        y1 = (int) event.getY(i);
                        count++;
                    } else if (count == 1) {
                        x2 = (int) event.getX(i);
                        y2 = (int) event.getY(i);
                        count++;
                    } else if (!Use2Touches && count == 2) {
                        x3 = (int) event.getX(i);
                        y3 = (int) event.getY(i);
                        count++;
                    } else if (!Use2Touches && count == 3) {
                        x4 = (int) event.getX(i);
                        y4 = (int) event.getY(i);
                        count++;
                    }
                }
                if (Use2Touches) {
                    ret = multiTouchEvent(event.getAction(), count, x1, y1, x2, y2, event);
                } else {
                    ret = multiTouchEvent4(event.getAction(), count, x1, y1, x2, y2, x3, y3, x4, y4, event);
                }
            } else // old style input.*/
            {
                ret = touchEvent(event.getAction(), (int) event.getX(), (int) event.getY(), event);
            }
        }
        return ret;
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean ret = false;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return super.onKeyDown(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            return false;
        }
        if (keyCode != KeyEvent.KEYCODE_MENU && keyCode != KeyEvent.KEYCODE_BACK) {
            ret = super.onKeyDown(keyCode, event);
        }
        if (!ret)
            ret = keyEvent(event.getAction(), keyCode, event.getUnicodeChar(), event.getMetaState(), event);
        return ret;
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    @SuppressLint("ObsoleteSdkInt")
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_CAPS_LOCK && Build.VERSION.SDK_INT >= 11) {
            capsLockOn = event.isCapsLockOn();
            keyEvent(capsLockOn ? 3 : 4, KeyEvent.KEYCODE_CAPS_LOCK, 0, 0, event);
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            return false;
        }
        boolean ret = super.onKeyUp(keyCode, event);
        if (!ret)
            ret = keyEvent(event.getAction(), keyCode, event.getUnicodeChar(), event.getMetaState(), event);
        return ret;
    }

    public void GetGLExtensions() {
        if (!HasGLExtensions && gl != null && cachedSurfaceHolder != null) {
            glVendor = gl.glGetString(GL11.GL_VENDOR);
            glExtensions = gl.glGetString(GL11.GL_EXTENSIONS);
            glRenderer = gl.glGetString(GL11.GL_RENDERER);
            glVersion = gl.glGetString(GL11.GL_VERSION);
            System.out.println("Vendor: " + glVendor);
            System.out.println("Extensions " + glExtensions);
            System.out.println("Renderer: " + glRenderer);
            System.out.println("glVersion: " + glVersion);
            if (glVendor != null) {
                HasGLExtensions = true;
            }
        }
    }

    public boolean InitEGLAndGLES2(int EGLVersion) {
        System.out.println("InitEGLAndGLES2");
        if (cachedSurfaceHolder == null) {
            System.out.println("InitEGLAndGLES2 failed, cachedSurfaceHolder is null");
            return false;
        }

        boolean eglInitialized = true;
        if (eglContext == null) {
            eglInitialized = false;
            if (EGLVersion >= 3) {
                try {
                    eglInitialized = initEGL(3, 24);
                } catch (Exception e) {
                }
                System.out.println("initEGL 3 " + eglInitialized);
            }
            if (!eglInitialized) {
                configAttrs = null;
                try {
                    eglInitialized = initEGL(2, GetDepthBits());
                } catch (Exception e2) {
                }
                System.out.println("initEGL 2 " + eglInitialized);
                if (!eglInitialized) {
                    eglInitialized = initEGL(2, GetDepthBits());
                    System.out.println("initEGL 2 " + eglInitialized);
                }
            }
        }
        if (eglInitialized) {
            System.out.println("Should we create a surface?");
            if (!viewIsActive) {
                System.out.println("Yes! Calling create surface");
                createEGLSurface(cachedSurfaceHolder);
                System.out.println("Done creating surface");
            }
            viewIsActive = true;
            SwapBufferSkip = 1;
            return true;
        } else {
            System.out.println("initEGLAndGLES2 failed, core EGL init failure");
            return false;
        }
    }

    public void GamepadReportSurfaceCreated(SurfaceHolder holder) {
    }

    public void mSleep(long milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
        }
    }

    public void DoResumeEvent() {
        if (!waitingForResume) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    waitingForResume = true;
                    while (cachedSurfaceHolder == null) {
                        NvEventQueueActivity.this.mSleep(1000L);
                    }
                    waitingForResume = false;
                    NvEventQueueActivity.this.resumeEvent();
                    ResumeEventDone = true;
                }
            }).start();
        }
    }

    /**
     * Implementation function: defined in libnvevent.a
     * The application does not and should not overide this; nv_event handles this internally
     * And remaps as needed into the native calls exposed by nv_event.h
     */
    protected boolean systemInit() {
        System.out.println("In systemInit");
        if (!GetGLExtensions && supportPauseResume) {
            init(GetGLExtensions);
        }

        // Setting up layouts and views
        if (warView == null) {
            view = new gSurfaceView(this);
            ((gSurfaceView) view).myActivity = this;
        } else {
            view = warView;
        }

        holder = view.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
        holder.setKeepScreenOn(true);
        if (isShieldTV) {
            holder.setFixedSize(maxDisplayWidth, maxDisplayHeight);
        }
        holder.addCallback(new Callback() {
            // @Override
            public void surfaceCreated(SurfaceHolder holder) {
                System.out.println("systemInit.surfaceCreated");
                @SuppressWarnings("unused")
                boolean firstRun = cachedSurfaceHolder == null;
                cachedSurfaceHolder = holder;

                if (!firstRun && ResumeEventDone) {
                    resumeEvent();
                }

                ranInit = true;
                if (supportPauseResume) {
                    init(GetGLExtensions);
                }

                System.out.println("surfaceCreated: w:" + surfaceWidth + ", h:" + surfaceHeight);
                setGameWindowSize(surfaceWidth, surfaceHeight);
                if (GetGLExtensions && supportPauseResume && firstRun) {
                    init(GetGLExtensions);
                }
                if (firstRun) {
                    GamepadReportSurfaceCreated(holder);
                }
            }

            /**
             * Implementation function: defined in libnvevent.a
             * The application does not and should not overide this; nv_event handles this internally
             * And remaps as needed into the native calls exposed by nv_event.h
             */
            // @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                System.out.println("Surface changed: " + width + ", " + height);
                surfaceWidth = width;
                surfaceHeight = height;
                setGameWindowSize(surfaceWidth, surfaceHeight);
                hideSystemUI();
            }

            /**
             * Implementation function: defined in libnvevent.a
             * The application does not and should not overide this; nv_event handles this internally
             * And remaps as needed into the native calls exposed by nv_event.h
             */
            // @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                System.out.println("systemInit.surfaceDestroyed");
                pauseEvent();
                destroyEGLSurface();
                viewIsActive = false;
            }
        });
        if (!delaySetContentView) {
            if (view.getParent() != null) {
                System.out.println("view.getParent() != null");
                setContentView((View) view.getParent());
            } else {
                setContentView(view);
            }
        }
        view.setFocusable(true);
        view.setFocusableInTouchMode(true);


        if (!noVidSurface) {
            vidView = new SurfaceView(this);
            vidHolder = vidView.getHolder();
            vidHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    vidViewIsActive = true;
                    if (!vidViewCreated) {
                        if (UseSubtitles) {
                            movieTextView.setVisibility(View.VISIBLE);
                        } else {
                            vidView.setVisibility(View.INVISIBLE);
                        }
                        vidViewCreated = true;
                    } else if (UseSubtitles) {
                        movieTextView.setVisibility(View.VISIBLE);
                    }
                    InVideview = false;
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    vidViewWidth = width;
                    vidViewHeight = height;
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    if (UseSubtitles) {
                        movieTextView.setVisibility(View.INVISIBLE);
                    }
                    vidViewIsActive = false;
                    movieIsStopping = false;
                    InVideview = false;
                }
            });
            vidHolder.setType(3);
            if (UseSubtitles) {
                movieTextView = new SurfaceView(this);
                movieTextHolder = movieTextView.getHolder();
                movieTextHolder.addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public final void surfaceCreated(SurfaceHolder holder) {
                        System.out.println("surface2222222Created called - subView");
                        Canvas canvas = movieTextHolder.lockCanvas();
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        movieTextHolder.unlockCanvasAndPost(canvas);
                        if (!movieTextViewCreated) {
                            movieTextViewCreated = true;
                        }
                        movieTextViewIsActive = true;
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                        System.out.println("surfaceChanged called - movieTextView");
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        System.out.println("surfaceDestroyed called - movieTextView");
                        movieTextViewFirstDestroy = true;
                        movieTextViewIsActive = false;
                    }
                });
                movieTextHolder.setType(0);
            }
        }
        if (!noVidSurface) {
            vidView.setZOrderOnTop(true);
            vidHolder.setFormat(-3);
            LinearLayout.LayoutParams myParams = new LinearLayout.LayoutParams(-2, -2);
            myParams.gravity = 17;
            addContentView(vidView, myParams);
            if (UseSubtitles) {
                movieTextHolder.setFormat(-3);
                addContentView(movieTextView, new ViewGroup.LayoutParams(-1, -1));
            }
        }
        return true;
    }

    @SuppressLint("ObsoleteSdkInt")
    public void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= 19 && view != null) {
            try {
                view.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            } catch (Exception e) {
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public void showSystemUI() {
        if (Build.VERSION.SDK_INT >= 19 && view != null) {
            try {
                view.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                );
            } catch (Exception e) {
            }
        }
    }

    public int GetDepthBits() {
        return depthSize;
    }


    /**
     * The number of bits requested for the red component
     */
    protected int redSize = 5;
    /**
     * The number of bits requested for the green component
     */
    protected int greenSize = 6;
    /**
     * The number of bits requested for the blue component
     */
    protected int blueSize = 5;
    /**
     * The number of bits requested for the alpha component
     */
    protected int alphaSize = 0;
    /**
     * The number of bits requested for the stencil component
     */
    protected int stencilSize = 0;
    /**
     * The number of bits requested for the depth component
     */
    protected int depthSize = 16;

    /**
     * Attributes used when selecting the EGLConfig
     */
    protected int[] configAttrs = null;
    /**
     * Attributes used when creating the context
     */
    protected int[] contextAttrs = null;

    /**
     * Called to initialize EGL. This function should not be called by the inheriting
     * activity, but can be overridden if needed.
     *
     * @return True if successful
     */
    @SuppressLint("ObsoleteSdkInt")
    protected boolean initEGL(int esVersion, int depthBits) {
        if (esVersion > 2 && Build.VERSION.SDK_INT < 21) {
            return false;
        }
        if (configAttrs == null)
            configAttrs = new int[]{EGL10.EGL_NONE};
        int[] oldConf = configAttrs;

        configAttrs = new int[3 + oldConf.length - 1];
        int i = 0;
        for (i = 0; i < oldConf.length - 1; i++)
            configAttrs[i] = oldConf[i];
        configAttrs[i++] = EGL_RENDERABLE_TYPE;
        if (esVersion == 3) {
            configAttrs[i++] = EGL_OPENGL_ES3_BIT;
        } else {
            configAttrs[i++] = EGL_OPENGL_ES2_BIT;
        }
        configAttrs[i++] = EGL10.EGL_NONE;

        contextAttrs = new int[]
                {
                        EGL_CONTEXT_CLIENT_VERSION, esVersion,
                        EGL10.EGL_NONE
                };

        if (configAttrs == null)
            configAttrs = new int[]{EGL10.EGL_NONE};
        int[] oldConfES2 = configAttrs;

        configAttrs = new int[13 + oldConfES2.length - 1];
        for (i = 0; i < oldConfES2.length - 1; i++)
            configAttrs[i] = oldConfES2[i];
        configAttrs[i++] = EGL10.EGL_RED_SIZE;
        configAttrs[i++] = redSize;
        configAttrs[i++] = EGL10.EGL_GREEN_SIZE;
        configAttrs[i++] = greenSize;
        configAttrs[i++] = EGL10.EGL_BLUE_SIZE;
        configAttrs[i++] = blueSize;
        configAttrs[i++] = EGL10.EGL_ALPHA_SIZE;
        configAttrs[i++] = alphaSize;
        configAttrs[i++] = EGL10.EGL_STENCIL_SIZE;
        configAttrs[i++] = stencilSize;
        configAttrs[i++] = EGL10.EGL_DEPTH_SIZE;
        configAttrs[i++] = depthBits;
        configAttrs[i++] = EGL10.EGL_NONE;

        egl = (EGL10) EGLContext.getEGL();
        egl.eglGetError();
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        System.out.println("eglDisplay: " + eglDisplay + ", err: " + egl.eglGetError());
        int[] version = new int[2];
        boolean ret = egl.eglInitialize(eglDisplay, version);
        System.out.println("EglInitialize returned: " + ret);
        if (!ret) {
            return false;
        }
        int eglErr = egl.eglGetError();
        if (eglErr != EGL10.EGL_SUCCESS)
            return false;
        System.out.println("eglInitialize err: " + eglErr);

        final EGLConfig[] config = new EGLConfig[20];
        int num_configs[] = new int[1];
        egl.eglChooseConfig(eglDisplay, configAttrs, config, config.length, num_configs);
        System.out.println("eglChooseConfig err: " + egl.eglGetError());
        System.out.println("num_configs " + num_configs[0]);
        int score = 1 << 24; // to make sure even worst score is better than this, like 8888 when request 565...
        int val[] = new int[1];
        for (i = 0; i < num_configs[0]; i++) {
            boolean cont = true;
            int currScore = 0;
            int r, g, b, a, d, s;
            for (int j = 0; j < (oldConf.length - 1) >> 1; j++) {
                egl.eglGetConfigAttrib(eglDisplay, config[i], configAttrs[j * 2], val);
                if ((val[0] & configAttrs[j * 2 + 1]) != configAttrs[j * 2 + 1]) {
                    cont = false; // Doesn't match the "must have" configs
                    break;
                }
            }
            if (!cont)
                continue;
            egl.eglGetConfigAttrib(eglDisplay, config[i], EGL10.EGL_RED_SIZE, val);
            r = val[0];
            egl.eglGetConfigAttrib(eglDisplay, config[i], EGL10.EGL_GREEN_SIZE, val);
            g = val[0];
            egl.eglGetConfigAttrib(eglDisplay, config[i], EGL10.EGL_BLUE_SIZE, val);
            b = val[0];
            egl.eglGetConfigAttrib(eglDisplay, config[i], EGL10.EGL_ALPHA_SIZE, val);
            a = val[0];
            egl.eglGetConfigAttrib(eglDisplay, config[i], EGL10.EGL_DEPTH_SIZE, val);
            d = val[0];
            egl.eglGetConfigAttrib(eglDisplay, config[i], EGL10.EGL_STENCIL_SIZE, val);
            s = val[0];

            System.out.println(">>> EGL Config [" + i + "] R" + r + "G" + g + "B" + b + "A" + a + " D" + d + "S" + s);

            currScore = (Math.abs(r - redSize) + Math.abs(g - greenSize) + Math.abs(b - blueSize) + Math.abs(a - alphaSize)) << 16;
            currScore += Math.abs(d - depthBits) << 8;
            currScore += Math.abs(s - stencilSize);

            if (currScore < score) {
                System.out.println("--------------------------");
                System.out.println("New config chosen: " + i);
                for (int j = 0; j < (configAttrs.length - 1) >> 1; j++) {
                    egl.eglGetConfigAttrib(eglDisplay, config[i], configAttrs[j * 2], val);
                    if (val[0] >= configAttrs[j * 2 + 1])
                        System.out.println("setting " + j + ", matches: " + val[0]);
                }

                score = currScore;
                eglConfig = config[i];
            }
        }
        if (eglConfig == null) {
            configAttrs = null;
            return false;
        }
        eglContext = egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttrs);
        System.out.println("eglCreateContext: " + egl.eglGetError());

        gl = (GL11) eglContext.getGL();
        return true;
    }

    /**
     * Called to create the EGLSurface to be used for rendering. This function should not be called by the inheriting
     * activity, but can be overridden if needed.
     *
     * @param surface The SurfaceHolder that holds the surface that we are going to render to.
     * @return True if successful
     */
    protected boolean createEGLSurface(SurfaceHolder surface) {
        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surface, null);
        System.out.println("eglSurface: " + eglSurface + ", err: " + egl.eglGetError());
        int sizes[] = new int[1];

        egl.eglQuerySurface(eglDisplay, eglSurface, EGL10.EGL_WIDTH, sizes);
        surfaceWidth = sizes[0];
        egl.eglQuerySurface(eglDisplay, eglSurface, EGL10.EGL_HEIGHT, sizes);
        surfaceHeight = sizes[0];

        System.out.println("checking glVendor == null?");
        if (glVendor == null) {
            System.out.println("Making current and back");
            makeCurrent();
            unMakeCurrent();
        }
        System.out.println("Done. Making current and back");

        return true;
    }

    /**
     * Destroys the EGLSurface used for rendering. This function should not be called by the inheriting
     * activity, but can be overridden if needed.
     */
    protected void destroyEGLSurface() {
        if (eglDisplay != null && eglSurface != null)
            egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        if (eglSurface != null)
            egl.eglDestroySurface(eglDisplay, eglSurface);
        eglSurface = null;
    }

    /**
     * Called to clean up egl. This function should not be called by the inheriting
     * activity, but can be overridden if needed.
     */
    protected void cleanupEGL() {
        System.out.println("cleanupEGL");
        destroyEGLSurface();
        if (eglDisplay != null)
            egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        if (eglContext != null)
            egl.eglDestroyContext(eglDisplay, eglContext);
        if (eglDisplay != null)
            egl.eglTerminate(eglDisplay);

        eglDisplay = null;
        eglContext = null;
        eglSurface = null;

        ranInit = false;
        eglConfig = null;

        cachedSurfaceHolder = null;
        surfaceWidth = 0;
        surfaceHeight = 0;
    }

    /**
     * Implementation function:
     * The application does not and should not overide or call this directly
     * Instead, the application should call NVEventEGLSwapBuffers(),
     * which is declared in nv_event.h
     */
    public boolean swapBuffers() {
        if (SwapBufferSkip > 0) {
            SwapBufferSkip--;
            System.out.println("swapBuffer wait");
            return true;
        }

        //long stopTime;
        //long startTime = nvGetSystemTime();
        if (eglSurface == null) {
            System.out.println("eglSurface is NULL");
            return false;
        }

        if (egl.eglSwapBuffers(eglDisplay, eglSurface)) {
            return true;
        }

        //stopTime = nvGetSystemTime();
        //String s = String.format("%d ms in eglSwapBuffers", (int)(stopTime - startTime));
        //Log.v("EventAccelerometer", s);

        System.out.println("eglSwapBufferrr: " + egl.eglGetError());
        return false;
    }

    public boolean getSupportPauseResume() {
        return supportPauseResume;
    }

    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    public int getSurfaceHeight() {
        return surfaceHeight;
    }

    /**
     * Implementation function:
     * The application does not and should not overide or call this directly
     * Instead, the application should call NVEventEGLMakeCurrent(),
     * which is declared in nv_event.h
     */
    public boolean makeCurrent() {
        if (eglContext == null) {
            System.out.println("eglContext is NULL");
            return false;
        }
        if (eglSurface == null) {
            System.out.println("eglSurface is NULL");
            return false;
        }
        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            System.out.println("eglMakeCurrent err: " + egl.eglGetError());

            // R* Are You Crazy?
            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                return false;
            }
        }

        // This must be called after we have bound an EGL context
        GetGLExtensions();
        return true;
    }

    public int getOrientation() {
        return display.getOrientation();
    }

    /**
     * Implementation function:
     * The application does not and should not overide or call this directly
     * Instead, the application should call NVEventEGLUnmakeCurrent(),
     * which is declared in nv_event.h
     */
    public boolean unMakeCurrent() {
        if (!egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)) {
            System.out.println("egl(Un)MakeCurrent err: " + egl.eglGetError());
            return false;
        }

        return true;
    }

    /**
     * Called when the Activity is exiting and it is time to cleanup.
     * Kept separate from the {@link #cleanup()} function so that subclasses
     * in their simplest form do not need to call any of the parent class' functions. This to make
     * it easier for pure C/C++ application so that these do not need to call java functions from C/C++
     * code.
     *
     * @see #cleanup()
     */
    protected void systemCleanup() {
        if (ranInit)
            cleanup();
        cleanupEGL();
    }
}
