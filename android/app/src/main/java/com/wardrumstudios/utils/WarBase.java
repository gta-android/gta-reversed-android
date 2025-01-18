package com.wardrumstudios.utils;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import androidx.core.app.ActivityCompat;

import com.nvidia.devtech.NvEventQueueActivity;

public class WarBase extends NvEventQueueActivity {
    private static final String TAG = "WarBase";
    public boolean FinalRelease = false;
    protected UsbManager mUsbManager;
    protected BroadcastReceiver mUsbReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        CreateUSBReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        Intent intent = getIntent();
        String action = intent.getAction();
        Log.i(TAG, "OnResume -> Intent: " + intent);
        UsbDevice device = (UsbDevice) intent.getParcelableExtra("device");

        if (action != null) {
            if (action.equals("android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                Log.i(TAG, "OnResume -> ACTION_USB_DEVICE_ATTACHED " + device.toString());
                USBDeviceAttached(device, device.getDeviceName());
            } else if (action.equals("android.hardware.usb.action.USB_DEVICE_DETACHED")) {
                Log.i(TAG, "OnResume -> ACTION_USB_DEVICE_DETACHED " + device.toString());
                USBDeviceDetached(device, device.getDeviceName());
            }
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        DestroyUSBReceiver();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
    }

    void CreateUSBReceiver() {
        Log.i(TAG, "Creating USB intent receiver");
        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                System.out.println("BroadcastReceiver WarMedia Base " + action);
                UsbDevice device = (UsbDevice) intent.getParcelableExtra("device");
                try {
                    switch (action) {
                        case "android.hardware.usb.action.USB_DEVICE_ATTACHED":
                            Log.i(WarBase.TAG, "BroadcastReceiver -> ACTION_USB_DEVICE_ATTACHED " + device.toString());
                            USBDeviceAttached(device, device.getDeviceName());
                            break;
                        case "android.hardware.usb.action.USB_DEVICE_DETACHED":
                            Log.i(WarBase.TAG, "BroadcastReceiver -> ACTION_USB_DEVICE_DETACHED " + device.toString());
                            USBDeviceDetached(device, device.getDeviceName());
                            break;
                        case "android.bluetooth.device.action.ACL_CONNECTED":
                            BluetoothDevice btDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                            if (btDevice == null) {
                                break;
                            }

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                break;
                            }

                            Log.i(WarBase.TAG, "BroadcastReceiver ACTION_ACL_CONNECTED name " + btDevice.getName());
                            if (btDevice.getName() != null && btDevice.getName().equals("GS controller")) {
                                SetGamepad(btDevice.getName());
                            }
                            break;
                        case "android.bluetooth.device.action.ACL_DISCONNECTED":
                            BluetoothDevice btDevice2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                            if (btDevice2 == null) {
                                break;
                            }

                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                break;
                            }

                            Log.i(WarBase.TAG, "BroadcastReceiver ACTION_ACL_DISCONNECTED name " + btDevice2.getName());
                            if (btDevice2.getName().equals("GS controller")) {
                                SetGamepad("");
                            }
                            break;
                        default:
                            Log.i(WarBase.TAG, "BroadcastReceiver -> UNKNOWN ACTION : " + action.toString());
                            break;
                    }
                }
                catch (Exception e) {
                    /* ~ */
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        filter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        filter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        registerReceiver(mUsbReceiver, filter);
        Log.i(TAG, "Receiver set up");
    }

    public void DestroyUSBReceiver() {
        unregisterReceiver(mUsbReceiver);
    }

    public void USBDeviceAttached(UsbDevice device, String name) {
        /* ~ */
    }

    public void USBDeviceDetached(UsbDevice device, String name) {
        /* ~ */
    }

    public void SetGamepad(String gamepadString) {
        /* ~ */
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        /* ~ */
    }
}
