package com.joseph.transfer_sdk.ble;

import android.bluetooth.BluetoothDevice;

import java.util.List;

public interface BleConnectCallback {

    /**
     * 蓝牙连接完成
     * @param device 连接的蓝牙设备
     */
    void onConnected(BluetoothDevice device);

    /**
     * 蓝牙连接的异常
     * @param throwable 超时或其它异常
     */
    void onError(Throwable throwable);

    /**
     * 蓝牙断开连接
     * @param device 断开的设备
     */
    void onDisconnected(BluetoothDevice device);
}
