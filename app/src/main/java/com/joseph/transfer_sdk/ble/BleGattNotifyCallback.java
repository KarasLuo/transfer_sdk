package com.joseph.transfer_sdk.ble;

public interface BleGattNotifyCallback {
    void onNotify(byte[]bytes);
}
