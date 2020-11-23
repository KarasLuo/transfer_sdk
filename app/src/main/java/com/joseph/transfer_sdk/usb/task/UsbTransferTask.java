package com.joseph.transfer_sdk.usb.task;

import com.joseph.transfer_sdk.usb.UsbTransferCallback;

/**
 * usb传输任务
 */
public abstract class UsbTransferTask {
    public UsbTransferCallback usbTransferCallback;
    public int timeout;
    public Exception error;
    public UsbTransferTask(int timeout,UsbTransferCallback callback){
        this.usbTransferCallback=callback;
        this.timeout=timeout;
        this.error=null;
    }

    public abstract String getDataString();

    @Override
    public String toString() {
        return "UsbTransferTask{" +
                "usbTransferCallback=" + usbTransferCallback +
                ", timeout=" + timeout +
                ", error=" + (error==null?"null":error.getMessage()) +
                "}";
    }
}
