package com.joseph.transfer_sdk.ble;

import android.bluetooth.BluetoothGattService;

import java.util.List;

public interface BleServiceCallback {
    /**
     * 返回可用的gatt服务
     * @param serviceList
     */
    void onList(List<BluetoothGattService>serviceList);
}
