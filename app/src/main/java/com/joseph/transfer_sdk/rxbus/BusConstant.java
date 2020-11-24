package com.joseph.transfer_sdk.rxbus;

/**
 * Rxbus常量值不可混用
 */
public final class BusConstant {
    //蓝牙通信事件
    public final static int BLE_EVENT_SCAN_CALLBACK_SINGLE=1001;//搜索的单个蓝牙
    public final static int BLE_EVENT_SCAN_CALLBACK_LIST=1002;//搜索的蓝牙列表
    public final static int BLE_EVENT_SCAN_CALLBACK_ERROR=1003;//搜索的蓝牙异常

    public final static int BLE_EVENT_CONNECT_ERROR=1021;//连接蓝牙出错
    public final static int BLE_EVENT_CONNECTED=1022;//蓝牙已连接
    public final static int BLE_EVENT_DISCONNECTED=1023;//蓝牙断开
    public final static int BLE_EVENT_SERVICE_INFO=1024;//蓝牙gatt信息，包括service、characteristic

    public final static int BLE_EVENT_TRANSFER=1050;
    public final static int BLE_EVENT_TRANSFER_NOTIFY=1051;

}
