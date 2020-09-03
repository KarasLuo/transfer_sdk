package com.joseph.demo2;

import com.joseph.transfer_sdk.ble.IBleDevice;

public class PV300R_I implements IBleDevice {
    public static final String TAG="PV300R_I";
    public static final String UUID_SERVICE ="0000b350-d6d8-c7ec-bdf0-eab1bfc6bcbc";
    public static final String UUID_CHARACTERISTIC_NOTIFY ="0000b351-d6d8-c7ec-bdf0-eab1bfc6bcbc";
    public static final String UUID_CHARACTERISTIC_WRITE ="0000b352-d6d8-c7ec-bdf0-eab1bfc6bcbc";


    @Override
    public String getServiceUUID() {
        return UUID_SERVICE;
    }

    @Override
    public String getReadUUID() {
        return null;
    }

    @Override
    public String getWriteUUID() {
        return UUID_CHARACTERISTIC_WRITE;
    }

    @Override
    public String getNotifyUUID() {
        return UUID_CHARACTERISTIC_NOTIFY;
    }

    public static byte[]getOrderOfReadVoltage(){
        byte[] byte0 = new byte[4];
        byte0[0] = (byte) 0xAA;
        byte0[1] = 0x55;
        byte0[2] = 0x17;
        byte0[3] = 0x01;
        return byte0;
    }

    public static byte[]getOrderOfAutoDetect(){
        byte[] byte0 = new byte[5];
        byte0[0] = (byte) 0xAA;
        byte0[1] = 0x55;
        byte0[2] = 0x03;
        byte0[3] = 0x01;
        byte0[4] = 0x00;
        return byte0;
    }
}
