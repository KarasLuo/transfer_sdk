package com.joseph.transfer_sdk.ble;

public interface IBleDevice {

    public String getServiceUUID();
    public String getReadUUID();
    public String getWriteUUID();
    public String getNotifyUUID();
}
