package com.joseph.transfer_sdk.usb;

import com.joseph.transfer_sdk.usb.task.UsbTransferTask;

public interface UsbTransferCallback {

    /**
     * 传输成功
     * @param task
     */
    void onSuccess(UsbTransferTask task);

    /**
     * 异常
     * @param e 错误信息
     */
    void onError(UsbTransferTask task,Exception e);
}
