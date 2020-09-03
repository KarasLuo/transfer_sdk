package com.joseph.transfer_sdk.rxbus;

public final class BusConstant {
    //通告信息事件
    public final static int MSG_NOTIFY_EVENT_SUCCESS=101;//正常通告信息
    public final static int MSG_NOTIFY_EVENT_FAILED=102;//错误通告信息
    //蓝牙通信事件
    public final static int BLE_EVENT_SCAN_CALLBACK_SINGLE=201;//搜索的单个蓝牙
    public final static int BLE_EVENT_SCAN_CALLBACK_LIST=202;//搜索的蓝牙列表
    public final static int BLE_EVENT_SCAN_CALLBACK_ERROR=203;//搜索的蓝牙异常

//    public final static int BLE_EVENT_CONNECT_TIMEOUT=204;//连接蓝牙超时
    public final static int BLE_EVENT_CONNECT_ERROR=205;//连接蓝牙出错
    public final static int BLE_EVENT_CONNECTED=206;//蓝牙已连接
    public final static int BLE_EVENT_DISCONNECTED=207;//蓝牙断开

    public final static int BLE_EVENT_TRANSFER=208;

}
