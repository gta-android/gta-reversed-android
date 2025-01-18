package com.wardrumstudios.utils;

import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;

import com.bda.controller.Controller;
import com.bda.controller.ControllerListener;
import com.bda.controller.StateEvent;

import java.nio.ByteBuffer;

public class WarGamepad extends WarBilling implements ControllerListener {
    private static final int COMMAND_DOWN = 2;
    private static final int COMMAND_FIRE = 16;
    private static final int COMMAND_LEFT = 8;
    private static final int COMMAND_RIGHT = 4;
    private static final int COMMAND_STATUS = 64;
    private static final int COMMAND_STOP = 32;
    private static final int COMMAND_UP = 1;
    static int MAX_GAME_PADS = 4;
    private static final int OSGT_60beat = 2;
    private static final int OSGT_AmazonGamepad = 12;
    private static final int OSGT_AmazonRemote = 11;
    private static final int OSGT_AndroidTV = 13;
    private static final int OSGT_Gamestop = 3;
    private static final int OSGT_Generic = 5;
    private static final int OSGT_IOSExtended = 9;
    private static final int OSGT_IOSSimple = 10;
    private static final int OSGT_Moga = 4;
    private static final int OSGT_MogaPro = 7;
    private static final int OSGT_Nyko = 6;
    private static final int OSGT_PS3 = 8;
    private static final int OSGT_Xbox360 = 0;
    private static final int OSGT_XperiaPlay = 1;
    private static final int OSX360_A = 1;
    private static final int OSX360_AXIS_L2 = 4;
    private static final int OSX360_AXIS_R2 = 5;
    private static final int OSX360_AXIS_X1 = 0;
    private static final int OSX360_AXIS_X2 = 2;
    private static final int OSX360_AXIS_Y1 = 1;
    private static final int OSX360_AXIS_Y2 = 3;
    private static final int OSX360_B = 2;
    private static final int OSX360_BACK = 32;
    private static final int OSX360_DPADDOWN = 512;
    private static final int OSX360_DPADLEFT = 1024;
    private static final int OSX360_DPADRIGHT = 2048;
    private static final int OSX360_DPADUP = 256;
    private static final int OSX360_L1 = 64;
    private static final int OSX360_L3 = 4096;
    private static final int OSX360_R1 = 128;
    private static final int OSX360_R3 = 8192;
    private static final int OSX360_START = 16;
    private static final int OSX360_X = 4;
    private static final int OSX360_Y = 8;
    private static final int OSXP_BACK = 16384;
    private static final int OSXP_GP_MENU = 32768;
    private static final int OSXP_MENU = 4096;
    private static final int OSXP_SEARCH = 8192;
    private static final String TAG = "WarGamepad";
    public GamePad[] GamePads;
    protected boolean IsAndroidTV = false;
    Controller mogaController = null;

    public native boolean processTouchpadAsPointer(ViewParent viewParent, boolean z);

    public class GamePad {
        boolean DpadIsAxis;
        public float[] GamepadAxes;
        public int GamepadButtonMask;
        public int GamepadDpadHack;
        public boolean GamepadTouchReversed;
        public int[] GamepadTouches;
        public int GamepadType;
        int NykoCheckHacks;
        boolean active;
        int deviceId;
        boolean is360;
        private boolean isXperia;
        public long lastConnect;
        public long lastDisconnect;
        private UsbDeviceConnection mGamepadConnection;
        private UsbDevice mGamepadDevice;
        private UsbEndpoint mGamepadEndpointIntr;
        private Thread mGamepadThread;
        private InputDevice mLastGamepadInputDevice;
        boolean mightBeNyko;
        float mobiX;
        float mobiY;
        Controller mogaController;
        public int numGamepadTouchSamples;
        public boolean reportPS3as360;

        public GamePad() {
            is360 = true;
            reportPS3as360 = true;
            active = false;
            GamepadType = -1;
            GamepadAxes = new float[6];
            GamepadTouches = new int[16];
            GamepadDpadHack = 0;
            GamepadButtonMask = 0;
            mightBeNyko = false;
            NykoCheckHacks = 0;
            DpadIsAxis = false;
            mogaController = null;
            mobiX = 0.0f;
            mobiY = 0.0f;
        }
    }

    int GetDeviceIndex(int deviceId, int source) {
        if ((source & 16) != 0) {
            for (int i = 0; i < MAX_GAME_PADS; i++) {
                if (GamePads[i].active && GamePads[i].deviceId == deviceId) {
                    return i;
                }
            }
        }

        return -1;
    }

    @SuppressWarnings("UnusedParameters")
    int GetDeviceIndexByName(String gamepadString) {
        return -1;
    }

    public void SetReportPS3As360(boolean reportPS3as360) {
       for (int i = 0; i < MAX_GAME_PADS; i++) {
           GamePads[i].reportPS3as360 = reportPS3as360;
       }
    }

    int GetFreeIndex(int deviceId, int source) {
        if ((source & 16) == 0) {
            return -1;
        }

        for (int i = 0; i < MAX_GAME_PADS; i++) {
            if (GamePads[i].active && GamePads[i].deviceId == deviceId) {
                return i;
            }
        }

        for (int i = 0; i < MAX_GAME_PADS; i++) {
            if (!GamePads[i].active) {
                GamePads[i].deviceId = deviceId;
                GamePads[i].active = true;
                return i;
            }
        }

        return -1;
    }

    @SuppressWarnings("unused")
    public int InitMogaController(int index) {
        try {
            GamePads[index].mogaController = Controller.getInstance(this);
            GamePads[index].mogaController.init();
            GamePads[index].mogaController.setListener(this, new Handler(Looper.getMainLooper()));
            GamePads[index].active = true;
            System.out.println("*****Set Moga as index 0");
            return index;
        } catch (IllegalArgumentException e) {
            GamePads[index].mogaController = null;
            return -1;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       try {
           mogaController = Controller.getInstance(this);
           mogaController.init();
           mogaController.setListener(this, new Handler(Looper.getMainLooper()));
       } catch (IllegalArgumentException e) {
           mogaController = null;
       }

       GamePads = new GamePad[MAX_GAME_PADS];
       for (int i = 0; i < MAX_GAME_PADS; i++) {
           GamePads[i] = new GamePad();
       }

       if (Build.PRODUCT.contains("R800")) {
           Log.i(TAG, "Xperia Play detected.");
           GamePads[0].isXperia = true;
           CheckNavigation(getResources().getConfiguration());
           return;
       }

       Log.i(TAG, "Product " + Build.PRODUCT);
       Log.i(TAG, "Device " + Build.DEVICE);
    }

    @Override
    protected void onDestroy() {
        if (mogaController != null) {
            mogaController.exit();
        }

        for (int i = 0; i < MAX_GAME_PADS; i++) {
            if (GamePads[i].mogaController != null) {
                GamePads[i].mogaController.exit();
            }
        }

        super.onDestroy();
    }

    @Override
    public void SetGamepad(String gamepadString) {
        int index = GetDeviceIndexByName(gamepadString);
        if (index != -1) {
            if (gamepadString.equals("GS controller")) {
                GamePads[index].GamepadType = OSGT_Gamestop;
            } else if (gamepadString.equals("")) {
                GamePads[index].GamepadType = -1;
            }
        }
    }

    @Override
    public void USBDeviceAttached(UsbDevice device, String name) {
        Log.i(TAG, "Device Attached : " + device);
        if (device == null) {
            Log.e(TAG, "Given null device?");
        } else if (device.getInterfaceCount() != 1) {
            Log.e(TAG, "could not find interface");
        } else {
            UsbInterface intf = device.getInterface(0);
            if (intf.getEndpointCount() != 1) {
                Log.e(TAG, "could not find endpoint");
                return;
            }

            UsbEndpoint ep = intf.getEndpoint(0);
            if (ep.getType() != 3) {
                Log.e(TAG, "endpoint is not interrupt type");
                return;
            }

            final int index = GetFreeIndex(device.getDeviceId(), 16);
            if (index != -1) {
                GamePads[index].is360 = true;
                UsbDeviceConnection connection = mUsbManager.openDevice(device);
                if (connection == null || !connection.claimInterface(intf, true)) {
                    Log.e(TAG, "Failed to open USB gamepad");
                    GamePads[index].GamepadType = -1;
                    GamePads[index].mGamepadDevice = null;
                    GamePads[index].mGamepadConnection = null;
                    GamePads[index].active = false;
                } else {
                    Log.i(TAG, "Success, I have a USB gamepad " + device.toString());
                    GamePads[index].GamepadType = OSGT_Xbox360;
                    if (device.toString().contains("PLAYSTATION")) {
                        GamePads[index].is360 = false;
                        if (!GamePads[index].reportPS3as360) {
                            GamePads[index].GamepadType = OSGT_PS3;
                        }
                    }

                    GamePads[index].mGamepadDevice = device;
                    GamePads[index].mGamepadEndpointIntr = ep;
                    GamePads[index].mGamepadConnection = connection;
                    GamePads[index].mGamepadThread = new Thread(() -> this.USBDeviceRun(index));
                    GamePads[index].mGamepadThread.start();
                    GamePads[index].lastConnect = SystemClock.uptimeMillis();
                }

                super.USBDeviceAttached(device, name);
            }
        }
    }

    @Override
    public void USBDeviceDetached(UsbDevice device, String name) {
        int index = GetDeviceIndex(device.getDeviceId(), 16);
        if (index == -1) {
            Log.e(TAG, "Disconnected gamepad, device not connected");
            return;
        }

        if (GamePads[index].GamepadType != -1) {
            Log.e(TAG, "Disconnected gamepad, stopping usb thread");
            GamePads[index].GamepadType = -1;
            GamePads[index].mGamepadDevice = null;
            GamePads[index].GamepadButtonMask = 0;
            if (GamePads[index].mGamepadThread != null && !GamePads[index].mGamepadThread.isInterrupted()) {
                GamePads[index].mGamepadThread.interrupt();
            }

            GamePads[index].lastDisconnect = SystemClock.uptimeMillis();
        }

        GamePads[index].active = false;
        super.USBDeviceDetached(device, name);
    }

    public void USBDeviceRun(int index) {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        UsbRequest request = new UsbRequest();
        request.initialize(GamePads[index].mGamepadConnection, GamePads[index].mGamepadEndpointIntr);
        byte status = -1;
        while (true) {
            request.queue(buffer, 1);
            sendCommand(index, 64);
            if (GamePads[index].mGamepadConnection.requestWait() == request) {
                byte newStatus = buffer.get(0);
                Log.i(TAG, "****got status " + ((int) newStatus));
                if (newStatus != status) {
                    Log.i(TAG, "got status " + ((int) newStatus));
                    status = newStatus;
                    if ((status & 16) != 0) {
                        sendCommand(index, 32);
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    /* ~ */
                }
            } else {
                Log.e(TAG, "requestWait failed, exiting");
                return;
            }
        }
    }

    private void sendCommand(int index, int control) {
        if (control != 64) {
            Log.i(TAG, "sendMove " + control);
        }

        if (GamePads[index].mGamepadConnection != null) {
            byte[] message = {(byte) control};
            GamePads[index].mGamepadConnection.controlTransfer(33, 9, 512, 0, message, message.length, 0);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public boolean onGenericMotionEvent(MotionEvent event) {
        int index = GetDeviceIndex(event.getDeviceId(), event.getSource());
        if (index == -1) {
            index = GetFreeIndex(event.getDeviceId(), event.getDevice() != null ? event.getDevice().getSources() : 0);
            if (index == -1) {
                if ((event.getSource() & 16) != 0) {
                    System.out.println("********************ERROR********* cannot assign controller MotionEvent " + event);
                }
                return super.onGenericMotionEvent(event);
            }
        }

        if (GamePads[index].isXperia) {
            return false;
        }

        if ((event.getSource() & 16) == 0 || event.getAction() != 2) {
            return super.onGenericMotionEvent(event);
        }

        if (GamePads[index].mLastGamepadInputDevice == null || GamePads[index].mLastGamepadInputDevice.getId() != event.getDeviceId()) {
            GamePads[index].mLastGamepadInputDevice = event.getDevice();
            if (GamePads[index].mLastGamepadInputDevice == null) {
                return false;
            }

            GamePads[index].GamepadType = -1;
            GamePads[index].mightBeNyko = false;
            if (GamePads[index].mLastGamepadInputDevice.getName().contains("NYKO")) {
                GamePads[index].GamepadType = OSGT_Nyko;
            } else {
                GamePads[index].mLastGamepadInputDevice.getName().contains("Broadcom Bluetooth HID");
                InputDevice.MotionRange gas = GamePads[index].mLastGamepadInputDevice.getMotionRange(22);
                InputDevice.MotionRange brake = GamePads[index].mLastGamepadInputDevice.getMotionRange(23);
                if (!(gas == null || brake == null)) {
                    GamePads[index].mightBeNyko = true;
                    GamePads[index].NykoCheckHacks = 0;
                }
            }
        }

        if (GamePads[index].GamepadType == -1 || GamePads[index].GamepadType == 11 || GamePads[index].GamepadType == 3) {
            GamePads[index].mLastGamepadInputDevice = event.getDevice();
            String name = GamePads[index].mLastGamepadInputDevice.getName();
            if (name.contains("Thunder") || name.contains("Amazon Fire Game Controller")) {
                GamePads[index].GamepadType = OSGT_AmazonGamepad;
                GamePads[index].GamepadButtonMask = 0;
                GamePads[index].lastConnect = SystemClock.uptimeMillis();
                System.out.println("Setting GamepadType to Amazon Controller");
            }
        }

        if (GamePads[index].GamepadType == -1 || GamePads[index].GamepadType == 3) {
            if (SystemClock.uptimeMillis() - GamePads[index].lastDisconnect < 250) {
                return false;
            }

            try {
                GamePads[index].mLastGamepadInputDevice = event.getDevice();
                Log.e(TAG, "FIXME! Received joystick event without a valid joystick. " + GamePads[index].mLastGamepadInputDevice);
                GamePads[index].GamepadType = OSGT_Xbox360;
                if (IsAndroidTV) {
                    GamePads[index].GamepadType = OSGT_AndroidTV;
                }
                if (GamePads[index].mLastGamepadInputDevice != null) {
                    Log.i(TAG, "mLastGamepadInputDevice.getName() " + GamePads[index].mLastGamepadInputDevice.getName());
                    if (GamePads[index].mLastGamepadInputDevice.getName().contains("PLAYSTATION")) {
                        GamePads[index].is360 = false;
                        if (!GamePads[index].reportPS3as360) {
                            GamePads[index].GamepadType = OSGT_PS3;
                        }
                    } else {
                        GamePads[index].is360 = true;
                    }
                }
                GamePads[index].GamepadButtonMask = 0;
                GamePads[index].lastConnect = SystemClock.uptimeMillis();
            } catch (Exception e) {
                /* ~ */
            }
        }

        int historySize = event.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            processJoystickInput(event, i);
        }

        processJoystickInput(event, -1);
        return true;
    }

    private void ClearBadJoystickAxis(GamePad myGamepad) {
        if (myGamepad.GamepadAxes[0] == -1.0f && myGamepad.GamepadAxes[1] == -1.0f && myGamepad.GamepadAxes[2] == -1.0f && myGamepad.GamepadAxes[3] == -1.0f) {
            System.out.println("Clearing Bad Joystick Axis");
            myGamepad.GamepadAxes[0] = 0.0f;
            myGamepad.GamepadAxes[1] = 0.0f;
            myGamepad.GamepadAxes[2] = 0.0f;
            myGamepad.GamepadAxes[3] = 0.0f;
            myGamepad.GamepadType = -1;
        }
    }

    private void processJoystickInput(MotionEvent event, int historyPos) {
        int index = GetDeviceIndex(event.getDeviceId(), event.getSource());
        if (index == -1) {
            index = GetFreeIndex(event.getDeviceId(), event.getDevice() != null ? event.getDevice().getSources() : 0);
            if (index == -1) {
                if ((event.getSource() & 16) != 0) {
                    System.out.println("********************ERROR********* cannot assign controller");
                    return;
                }
                return;
            }
        }

        GamePad myGamePad = GamePads[index];
        myGamePad.GamepadAxes[0] = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 0, historyPos);
        myGamePad.GamepadAxes[1] = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 1, historyPos);
        myGamePad.GamepadAxes[2] = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 11, historyPos);
        myGamePad.GamepadAxes[3] = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 14, historyPos);
        if (IsValidAxis(event, myGamePad.mLastGamepadInputDevice, 17) || IsValidAxis(event, myGamePad.mLastGamepadInputDevice, 23)) {
            float valL2 = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 17, historyPos);
            if (valL2 == 0.0f) {
                float[] fArr = myGamePad.GamepadAxes;
                valL2 = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 23, historyPos);
                fArr[4] = valL2;
            }
            myGamePad.GamepadAxes[4] = valL2;
        }

        if (IsValidAxis(event, myGamePad.mLastGamepadInputDevice, 18) || IsValidAxis(event, myGamePad.mLastGamepadInputDevice, 22)) {
            float valR2 = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 18, historyPos);
            if (valR2 == 0.0f) {
                float[] fArr2 = myGamePad.GamepadAxes;
                valR2 = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 22, historyPos);
                fArr2[5] = valR2;
            }
            myGamePad.GamepadAxes[5] = valR2;
        }

        if (myGamePad.GamepadType != 6 && SystemClock.uptimeMillis() - myGamePad.lastConnect > 1000) {
            float DPAD_X = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 15, historyPos);
            float DPAD_Y = getCenteredAxis(event, myGamePad.mLastGamepadInputDevice, 16, historyPos);
            if (myGamePad.mightBeNyko) {
                if (myGamePad.GamepadAxes[0] > 0.75f && DPAD_X > 0.75f) {
                    myGamePad.NykoCheckHacks |= 1;
                }

                if (myGamePad.GamepadAxes[0] < -0.75f && DPAD_X < -0.75f) {
                    myGamePad.NykoCheckHacks |= 2;
                }

                if (myGamePad.GamepadAxes[1] > 0.75f && DPAD_Y > 0.75f) {
                    myGamePad.NykoCheckHacks |= 4;
                }

                if (myGamePad.GamepadAxes[1] < -0.7f && DPAD_Y < 0.75f) {
                    myGamePad.NykoCheckHacks |= 8;
                }

                if (myGamePad.GamepadAxes[0] > 0.75f && DPAD_X == 0.0f) {
                    myGamePad.mightBeNyko = false;
                }

                if (myGamePad.GamepadAxes[0] < -0.75f && DPAD_X == 0.0f) {
                    myGamePad.mightBeNyko = false;
                }

                if (myGamePad.GamepadAxes[1] > 0.75f && DPAD_Y == 0.0f) {
                    myGamePad.mightBeNyko = false;
                }

                if (myGamePad.GamepadAxes[1] < -0.75f && DPAD_Y == 0.0f) {
                    myGamePad.mightBeNyko = false;
                }

                if (myGamePad.NykoCheckHacks == 15) {
                    myGamePad.GamepadType = OSGT_Nyko;
                    System.out.println("detecting NYKO controller");
                }
            }

            if (myGamePad.GamepadDpadHack == 1) {
                if (DPAD_X > 0.5f || DPAD_X < -0.5f || DPAD_Y > 0.5f || DPAD_Y < -0.5f) {
                    myGamePad.GamepadDpadHack = 0;
                } else {
                    return;
                }
            }

            if (DPAD_X > 0.5f) {
                myGamePad.GamepadButtonMask |= 2048;
            } else {
                myGamePad.GamepadButtonMask &= -2049;
            }

            if (DPAD_X < -0.5f) {
                myGamePad.GamepadButtonMask |= 1024;
            } else {
                myGamePad.GamepadButtonMask &= -1025;
            }

            if (DPAD_Y > 0.5f) {
                myGamePad.GamepadButtonMask |= 512;
            } else {
                myGamePad.GamepadButtonMask &= -513;
            }

            if (DPAD_Y < -0.5f) {
                myGamePad.GamepadButtonMask |= 256;
            } else {
                myGamePad.GamepadButtonMask &= -257;
            }
        }
    }

    private static boolean IsValidAxis(MotionEvent event, InputDevice device, int axis) {
        return device.getMotionRange(axis, event.getSource()) != null;
    }

    private static float getCenteredAxis(MotionEvent event, InputDevice device, int axis, int historyPos) {
        float value;
        InputDevice.MotionRange range = device.getMotionRange(axis, event.getSource());
        if (range != null) {
            float flat = range.getFlat();
            if (historyPos < 0) {
                value = event.getAxisValue(axis);
            } else {
                value = event.getHistoricalAxisValue(axis, historyPos);
            }

            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0.0f;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
        int index = GetDeviceIndex(keyEvent.getDeviceId(), keyEvent.getSource());
        if (index == -1) {
            index = GetFreeIndex(keyEvent.getDeviceId(), keyEvent.getDevice() != null ? keyEvent.getDevice().getSources() : 0);
            if (index == -1) {
                if ((keyEvent.getSource() & 16) != 0) {
                    System.out.println("********************ERROR********* cannot assign controller keyEvent " + keyEvent);
                }
                return super.onKeyUp(keyCode, keyEvent);
            }
        }

        GamePad myGamePad = GamePads[index];
        int buttonMask = 0;
        switch (keyCode) {
            case 96:
                if (!myGamePad.is360) {
                    buttonMask = 4;
                } else {
                    buttonMask = 1;
                }
                break;
            case 97:
                if (!myGamePad.is360) {
                    buttonMask = 8;
                } else {
                    buttonMask = 2;
                }
                break;
            case 99:
                if (!myGamePad.is360) {
                    buttonMask = 1;
                } else {
                    buttonMask = 4;
                }
                break;
            case 100:
                if (!myGamePad.is360) {
                    buttonMask = 2;
                } else {
                    buttonMask = 8;
                }
                break;
            case 102:
                buttonMask = 64;
                break;
            case 103:
                buttonMask = 128;
                break;
            case 104:
                myGamePad.GamepadAxes[4] = 0.0f;
                break;
            case 105:
                myGamePad.GamepadAxes[5] = 0.0f;
                break;
            case 106:
                buttonMask = 4096;
                break;
            case 107:
                buttonMask = 8192;
                break;
            case 108:
                buttonMask = 16;
                break;
            case 109:
                buttonMask = 32;
                break;
        }

        if (buttonMask == 0 && !myGamePad.isXperia) {
            switch (keyCode) {
                case 19:
                    buttonMask = 256;
                    break;
                case 20:
                    buttonMask = 512;
                    break;
                case 21:
                    buttonMask = 1024;
                    break;
                case 22:
                    buttonMask = 2048;
                    break;
                case 23:
                    buttonMask = 1;
                    break;
            }

            if (buttonMask != 0) {
                myGamePad.GamepadDpadHack = 1;
            }
        }

        if (buttonMask == 0 && myGamePad.isXperia && myGamePad.GamepadType != -1) {
            switch (keyCode) {
                case 4:
                    buttonMask = 2;
                    if (keyEvent.getScanCode() == 158) {
                        buttonMask = 2 | 16384;
                        break;
                    }
                    break;
                case 19:
                    buttonMask = 256;
                    break;
                case 20:
                    buttonMask = 512;
                    break;
                case 21:
                    buttonMask = 1024;
                    break;
                case 22:
                    buttonMask = 2048;
                    break;
                case 23:
                    buttonMask = 1;
                    break;
                case 82:
                    buttonMask = 4096;
                    if (keyEvent.getScanCode() == 226) {
                        buttonMask = 4096 | 32768;
                        break;
                    }
                    break;
                case 84:
                    buttonMask = 8192;
                    break;
            }
        }

        if (myGamePad.GamepadType == 3) {
            if (keyCode == 111) {
                buttonMask |= 32;
                keyCode = 109;
            } else if (keyCode == 66) {
                buttonMask |= 16;
                keyCode = 108;
            }
        }

        if (buttonMask != 0) {
            myGamePad.GamepadButtonMask &= ~buttonMask;
        }
        return super.onKeyUp(keyCode, keyEvent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        int index = GetDeviceIndex(keyEvent.getDeviceId(), keyEvent.getSource());
        if (index == -1) {
            if (keyEvent.getDevice() != null) {
                keyEvent.getDevice().getSources();
            }

            index = GetFreeIndex(keyEvent.getDeviceId(), keyEvent.getDevice().getSources());
            if (index == -1) {
                if ((keyEvent.getSource() & 16) != 0) {
                    System.out.println("********************ERROR********* cannot assign controller keyEvent " + keyEvent);
                }
                return super.onKeyDown(keyCode, keyEvent);
            }
        }

        GamePad myGamePad = GamePads[index];
        int buttonMask = 0;
        try {
            if (!myGamePad.isXperia && keyEvent.getDeviceId() > 0 && myGamePad.GamepadType != 11) {
                boolean mightBeGamestop = (keyCode >= 7 && keyCode <= 16) || (keyCode >= 20 && keyCode <= 22) || ((keyCode >= 37 && keyCode <= 40) || keyCode == 47 || keyCode == 29 || keyCode == 32 || keyCode == 51);
                if (keyEvent.getDevice() == null || (keyEvent.getDevice().getSources() & 16) == 0) {
                    if (mightBeGamestop) {
                        myGamePad.GamepadType = 3;
                    } else if (keyEvent.getDevice() != null && (keyEvent.getDevice().getName().equals("GS controller") || keyEvent.getDevice().getName().equals("Broadcom Bluetooth HID"))) {
                        myGamePad.GamepadType = 3;
                    }
                } else if (myGamePad.GamepadType == -1 || myGamePad.GamepadType == 3) {
                    myGamePad.GamepadType = 5;
                }
            }
        } catch (Exception | NoSuchMethodError e) {
            /* ~ */
        }

        if (keyEvent.getDevice() != null && (myGamePad.GamepadType == -1 || myGamePad.GamepadType == 12 || myGamePad.GamepadType == 11 || myGamePad.GamepadType == 3)) {
            myGamePad.mLastGamepadInputDevice = keyEvent.getDevice();
            if (myGamePad.mLastGamepadInputDevice != null) {
                String name = myGamePad.mLastGamepadInputDevice.getName();
                if (myGamePad.GamepadType != 12 && (name.contains("Thunder") || name.contains("Amazon Fire Game Controller"))) {
                    myGamePad.GamepadType = 12;
                    myGamePad.lastConnect = SystemClock.uptimeMillis();
                    System.out.println("Setting GamepadType to Amazon Controller onKeyDown");
                } else if (myGamePad.GamepadType != 11 && myGamePad.mLastGamepadInputDevice.getName().equals("Amazon")) {
                    myGamePad.GamepadType = 11;
                    myGamePad.lastConnect = SystemClock.uptimeMillis();
                    System.out.println("Setting GamepadType to Amazon Remote");
                }
            }
        }

        switch (keyCode) {
            case 96:
                if (!myGamePad.is360) {
                    buttonMask = 4;
                } else {
                    buttonMask = 1;
                }
                break;
            case 97:
                if (!myGamePad.is360) {
                    buttonMask = 8;
                } else {
                    buttonMask = 2;
                }
                break;
            case 99:
                if (!myGamePad.is360) {
                    buttonMask = 1;
                } else {
                    buttonMask = 4;
                }
                break;
            case 100:
                if (!myGamePad.is360) {
                    buttonMask = 2;
                } else {
                    buttonMask = 8;
                }
                break;
            case 102:
                buttonMask = 64;
                break;
            case 103:
                buttonMask = 128;
                break;
            case 104:
                myGamePad.GamepadAxes[4] = 1.0f;
                break;
            case 105:
                myGamePad.GamepadAxes[5] = 1.0f;
                break;
            case 106:
                buttonMask = 4096;
                break;
            case 107:
                buttonMask = 8192;
                break;
            case 108:
                buttonMask = 16;
                break;
            case 109:
                buttonMask = 32;
                break;
        }

        if (buttonMask == 0 && !myGamePad.isXperia) {
            switch (keyCode) {
                case 19:
                    buttonMask = 256;
                    break;
                case 20:
                    buttonMask = 512;
                    break;
                case 21:
                    buttonMask = 1024;
                    break;
                case 22:
                    buttonMask = 2048;
                    break;
                case 23:
                    buttonMask = 1;
                    break;
            }

            if (buttonMask != 0) {
                myGamePad.GamepadDpadHack = 1;
            }
        }

        if (buttonMask == 0 && myGamePad.isXperia && myGamePad.GamepadType != -1) {
            switch (keyCode) {
                case 4:
                    buttonMask = 2;
                    if (keyEvent.getScanCode() == 158) {
                        buttonMask = 2 | 16384;
                        break;
                    }
                    break;
                case 19:
                    buttonMask = 256;
                    break;
                case 20:
                    buttonMask = 512;
                    break;
                case 21:
                    buttonMask = 1024;
                    break;
                case 22:
                    buttonMask = 2048;
                    break;
                case 23:
                    buttonMask = 1;
                    break;
                case 82:
                    buttonMask = 4096;
                    if (keyEvent.getScanCode() == 226) {
                        buttonMask = 4096 | 32768;
                        break;
                    }
                    break;
                case 84:
                    buttonMask = 8192;
                    break;
            }
        }

        if (myGamePad.GamepadType == OSGT_Gamestop) {
            if (keyCode == 111) {
                buttonMask |= 32;
                keyCode = 109;
            } else if (keyCode == 66) {
                buttonMask |= 16;
                keyCode = 108;
            }
        }

        if (buttonMask != 0 && SystemClock.uptimeMillis() - myGamePad.lastConnect > 1000) {
            myGamePad.GamepadButtonMask |= buttonMask;
        }

        return super.onKeyDown(keyCode, keyEvent);
    }

    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        if (GamePads[0].isXperia) {
            CheckNavigation(newConfig);
        }

        super.onConfigurationChanged(newConfig);
    }

    public void CheckNavigation(Configuration withConfig) {
        if (withConfig.navigationHidden == 1 && GamePads[0].GamepadType == -1) {
            Log.i(TAG, "Attached xPeria play gamepad.");
            GamePads[0].GamepadType = OSGT_XperiaPlay;
            GamePads[0].is360 = true;
            GamePads[0].GamepadButtonMask = 0;
            GamePads[0].lastConnect = SystemClock.uptimeMillis();
        } else if (withConfig.navigationHidden == 2 && GamePads[0].GamepadType != -1) {
            Log.i(TAG, "Detaching xPeria play gamepad.");
            GamePads[0].GamepadType = -1;
            GamePads[0].GamepadButtonMask = 0;
            GamePads[0].lastDisconnect = SystemClock.uptimeMillis();
        }
    }

    private void setProcessTouchpadAsPointer(boolean processAsPointer) {
        ViewParent viewRoot;
        try {
            try {
                View root = getWindow().getDecorView().getRootView();
                if (root != null && (viewRoot = root.getParent()) != null) {
                    if (processTouchpadAsPointer(viewRoot, processAsPointer)) {
                        System.out.println("Processing touchpad as pointer succeeded");
                    } else {
                        System.out.println("Processing touchpad as pointer failed");
                    }
                }
            } catch (NoClassDefFoundError e) {
                /* ~ */
            }
        } catch (Exception e2) {
            System.out.println("Unable to set processTouchpadAsPointer: " + e2.toString());
        }
    }

    @Override
    public void GamepadReportSurfaceCreated(SurfaceHolder holder) {
        System.out.println("Processing touchpad as pointer...");
        setProcessTouchpadAsPointer(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int gIndex = GetDeviceIndex(event.getDeviceId(), event.getSource());
        if (gIndex == -1) {
            gIndex = GetFreeIndex(event.getDeviceId(), event.getDevice() != null ? event.getDevice().getSources() : 0);
            if (gIndex == -1) {
                if ((event.getSource() & 16) != 0) {
                    System.out.println("********************ERROR********* cannot assign controller - onTouchEvent " + event);
                }
                return super.onTouchEvent(event);
            }
        }

        ClearBadJoystickAxis(GamePads[gIndex]);
        if (event.getSource() != InputDevice.SOURCE_TOUCHPAD) {
            return super.onTouchEvent(event);
        }

        int count = 0;
        int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
        int numEvents = event.getPointerCount();
        for (int i = 0; i < numEvents; i++) {
            event.getPointerId(i);
            if (count == 0) {
                x1 = (int) event.getX(i);
                y1 = (int) event.getY(i);
                count++;
            } else if (count == 1) {
                x2 = (int) event.getX(i);
                y2 = (int) event.getY(i);
                count++;
            }
        }

        TouchpadEvent(event.getAction(), count, x1, y1, x2, y2);
        return true;
    }

    @SuppressWarnings("UnusedParameters")
    public void TouchpadEvent(int touchAction, int count, int x1, int y1, int x2, int y2) {
        if (touchAction == MotionEvent.ACTION_DOWN || touchAction == MotionEvent.ACTION_UP) {
            GamePads[0].numGamepadTouchSamples = 0;
            if (touchAction == 1) {
                UpdateTrack(0, 0, 0, true);
                UpdateTrack(1, 0, 0, true);
                return;
            }
        } else {
            GamePads[0].numGamepadTouchSamples++;
        }

        boolean fullChange = false;
        if (touchAction == MotionEvent.ACTION_POINTER_UP) {
            GamePads[0].GamepadTouchReversed = true;
            fullChange = true;
        } else if (touchAction != MotionEvent.ACTION_MOVE) {
            GamePads[0].GamepadTouchReversed = false;
            fullChange = true;
        }

        if (GamePads[0].GamepadTouchReversed) {
            UpdateTrack(1, x1, y1, fullChange);
            UpdateTrack(0, x2, y2, fullChange);
            return;
        }

        UpdateTrack(0, x1, y1, fullChange);
        UpdateTrack(1, x2, y2, fullChange);
    }

    public void UpdateTrack(int trackNo, int x, int y, boolean fullChange) {
        if (fullChange) {
            for (int i = 0; i < 4; i++) {
                int index = (trackNo * 8) + (i * 2);
                GamePads[0].GamepadTouches[index] = x;
                GamePads[0].GamepadTouches[index + 1] = y;
            }
            return;
        }

        int index2 = (trackNo * 8) + ((GamePads[0].numGamepadTouchSamples % 4) * 2);
        GamePads[0].GamepadTouches[index2] = x;
        GamePads[0].GamepadTouches[index2 + 1] = y;
    }

    @SuppressWarnings("unused")
    public int GetGamepadType(int index) {
        return GamePads[index].GamepadType;
    }

    @SuppressWarnings("unused")
    public int GetGamepadButtons(int index) {
        return GamePads[index].GamepadButtonMask;
    }

    @SuppressWarnings("unused")
    public float GetGamepadAxis(int index, int axisId) {
        return GamePads[index].GamepadAxes[axisId];
    }

    @SuppressWarnings("unused")
    public int GetGamepadTrack(int index, int trackId, int coord) {
        if (GamePads[index].numGamepadTouchSamples < 4) {
            return 0;
        }

        int average = 0;
        for (int i = 0; i < 4; i++) {
            average += GamePads[index].GamepadTouches[(trackId * 8) + (i * 2) + coord];
        }

        return average / 4;
    }

    @SuppressWarnings("UnusedParameters")
    int GetMogaControllerType(int index) {
        int mogaType = mogaController.getState(Controller.STATE_CURRENT_PRODUCT_VERSION);
        if (mogaType == 0) {
            return OSGT_Moga;
        }

        if (mogaType == 1) {
            return OSGT_MogaPro;
        }

        System.out.println("Moga controller type = " + mogaType);
        return OSGT_MogaPro;
    }

    @Override
    public void onKeyEvent(com.bda.controller.KeyEvent event) {
        int deviceIndex = GetDeviceIndex(event.getControllerId(), 16);
        if (deviceIndex != -1) {
            GamePad myGamePad = GamePads[deviceIndex];
            if (!(myGamePad.GamepadType == OSGT_60beat || myGamePad.GamepadType == OSGT_MogaPro)) {
                myGamePad.GamepadType = GetMogaControllerType(deviceIndex);
            }

            int buttonMask = 0;
            switch (event.getKeyCode()) {
                case 19:
                    if (myGamePad.GamepadType == OSGT_MogaPro) {
                        buttonMask = OSX360_DPADUP;
                        break;
                    }
                    break;
                case 20:
                    if (myGamePad.GamepadType == OSGT_MogaPro) {
                        buttonMask = OSX360_DPADDOWN;
                        break;
                    }
                    break;
                case 21:
                    if (myGamePad.GamepadType == OSGT_MogaPro) {
                        buttonMask = OSX360_DPADLEFT;
                        break;
                    }
                    break;
                case 22:
                    if (myGamePad.GamepadType == OSGT_MogaPro) {
                        buttonMask = OSX360_DPADRIGHT;
                        break;
                    }
                    break;
                case 96:
                    buttonMask = OSX360_A;
                    break;
                case 97:
                    buttonMask = OSX360_B;
                    break;
                case 99:
                    buttonMask = OSX360_X;
                    break;
                case 100:
                    buttonMask = OSX360_Y;
                    break;
                case 102:
                    buttonMask = OSX360_L1;
                    break;
                case 103:
                    buttonMask = OSX360_R1;
                    break;
                case 104:
                    myGamePad.GamepadAxes[4] = 0.0f;
                    break;
                case 105:
                    myGamePad.GamepadAxes[5] = 0.0f;
                    break;
                case 106:
                    buttonMask = OSX360_L3;
                    break;
                case 107:
                    buttonMask = OSX360_R3;
                    break;
                case 108:
                    buttonMask = OSX360_START;
                    break;
                case 109:
                    buttonMask = OSX360_BACK;
                    break;
                default:
                    System.out.println("onKeyEvent " + event.getKeyCode());
                    break;
            }

            System.out.println("onKeyEvent " + event.getKeyCode());
            if (buttonMask != 0) {
                if (event.getAction() == com.bda.controller.KeyEvent.ACTION_DOWN) {
                    myGamePad.GamepadButtonMask |= buttonMask;
                } else {
                    myGamePad.GamepadButtonMask &= ~buttonMask;
                }
            }
        }
    }

    float GetWithDeadZone(float x) {
        if (((double) x) > 0.25d || ((double) x) < -0.25d) {
            return x;
        }

        return 0.0f;
    }

    @Override
    public void onMotionEvent(com.bda.controller.MotionEvent event) {
        int deviceIndex = GetDeviceIndex(event.getControllerId(), 16);
        if (deviceIndex != -1) {
            GamePad myGamePad = GamePads[deviceIndex];
            if (!(myGamePad.GamepadType == OSGT_60beat || myGamePad.GamepadType == OSGT_MogaPro)) {
                myGamePad.GamepadType = GetMogaControllerType(deviceIndex);
            }

            myGamePad.mobiX = GetWithDeadZone(event.getAxisValue(com.bda.controller.MotionEvent.AXIS_X));
            myGamePad.mobiY = GetWithDeadZone(event.getAxisValue(com.bda.controller.MotionEvent.AXIS_Y));
            myGamePad.GamepadAxes[0] = myGamePad.mobiX;
            myGamePad.GamepadAxes[1] = myGamePad.mobiY;
            myGamePad.GamepadAxes[2] = GetWithDeadZone(event.getAxisValue(com.bda.controller.MotionEvent.AXIS_Z));
            myGamePad.GamepadAxes[3] = GetWithDeadZone(event.getAxisValue(com.bda.controller.MotionEvent.AXIS_RZ));
        }
    }

    @Override
    public void onStateEvent(StateEvent event) {
        int deviceIndex = GetDeviceIndex(event.getControllerId(), 16);
        System.out.println("onStateEvent " + event + " getState " + event.getState() + " action " + event.getAction());
        if (event.getState() == StateEvent.STATE_CONNECTION) {
            switch (event.getAction()) {
                case 0:
                    if (deviceIndex > -1) {
                        GamePads[deviceIndex].GamepadType = -1;
                        GamePads[deviceIndex].active = false;
                        break;
                    }
                    break;
                case 1:
                    int freeIndex = GetFreeIndex(event.getControllerId(), 16);
                    if (deviceIndex == -1 && freeIndex > -1) {
                        GamePads[freeIndex].GamepadType = GetMogaControllerType(freeIndex);
                        break;
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
