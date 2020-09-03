package com.joseph.transfer_sdk.ble;

public class BleTransfer {
    public final static int TASK_TRANSFER_WRITE=1;
    public final static int TASK_TRANSFER_READ=2;
    public final static int TASK_TRANSFER_NOTIFY=3;

    public long timestamp;
    public int transferType;
    public byte[]buffer;
    public int timeout;
    public BleTransferCallback callback;

    /**
     * 创建蓝牙传输任务
     * @param transferType 传输的类型 可能的值有
     *                     TASK_TRANSFER_WRITE
     *                     TASK_TRANSFER_READ
     *                     TASK_TRANSFER_NOTIFY
     * @param buffer 要写入的字节 长度不超过20
     * @param timeout 超时时长 毫秒
     * @param callback 读写通知操作的回调 线程不安全
     */
    public BleTransfer(int transferType, byte[]buffer,int timeout,BleTransferCallback callback){
        this.transferType=transferType;
        this.buffer=buffer;
        this.callback=callback;
        this.timeout=timeout;
    }

    public interface BleTransferCallback {
        void onReply(byte[]bytes);
        void onTimeout(BleTransfer task);
    }
}
