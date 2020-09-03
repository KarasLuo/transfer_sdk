package com.joseph.transfer_sdk.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import java.util.List;

public interface BleSearchCallback {

    /**
     * 单个结果
     * @param result 包含设备的一个结果对象
     */
    void onSingle(ScanResult result);

    /**
     * 设备列表
     * @param list 扫描到的不重复的设备列表
     */
    void onList(List<BluetoothDevice> list);

    /**
     * 扫描蓝牙发生异常
     * @param errorCode 异常码
     */
    void onError(int errorCode);
}
