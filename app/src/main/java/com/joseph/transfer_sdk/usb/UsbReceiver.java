package com.joseph.transfer_sdk.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

public class UsbReceiver extends BroadcastReceiver {
    private static final String TAG="UsbReceiver";

    public static final String ACTION_REQUIRE_USB_PERMISSION="com.joseph.USB_PERMISSION";
    private UsbBroadcastListener listener;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.w(TAG,"onReceive context="+context+",intent="+intent.toString());
        if(action==null){
            Log.e(TAG,"broadcast receiver action is null");
            return;
        }
        Bundle bundle=intent.getExtras();
        if(bundle!=null){
            Log.w(TAG,"bundle="+bundle.toString());
        }
        switch (action){
            case ACTION_REQUIRE_USB_PERMISSION:
                UsbDevice device=intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Log.i(TAG,"onReceive device="+device);
                boolean isGranted= intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                Log.e(TAG,"isGranted="+isGranted);
                if(this.listener!=null){
                    this.listener.onRequirePermission(device,isGranted);
                }
                break;
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                if(this.listener!=null){
                    this.listener.onAttached();
                }
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                if(this.listener!=null){
                    this.listener.onDetached();
                }
                break;
            default:
                break;
        }
    }

    public interface UsbBroadcastListener{
        void onRequirePermission(UsbDevice device, boolean isGranted);
        void onAttached();
        void onDetached();
    }

    public void setUsbBroadcastListener(UsbBroadcastListener listener){
        this.listener=listener;
    }
}
