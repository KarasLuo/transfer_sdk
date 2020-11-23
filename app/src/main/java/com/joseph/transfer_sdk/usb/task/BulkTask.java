package com.joseph.transfer_sdk.usb.task;

import com.joseph.transfer_sdk.usb.UsbTransferCallback;

import java.util.ArrayList;
import java.util.List;

import static com.joseph.transfer_sdk.ByteUtils.bytesToString;

public class BulkTask extends UsbTransferTask {

    public int readLen;//要读取的字节长度
    //usb读取的数据，可能有多组字节数据
    public List<byte[]> receivedBytes=new ArrayList<>();
    public byte[]buffer;//写入的数据

    public BulkTask(byte[] buffer,int len,int timeout, UsbTransferCallback callback){
        super(timeout,callback);
        this.buffer=buffer;
        this.readLen =len;
    }

    @Override
    public String getDataString() {
        String msg="";
        if(buffer!=null){
            msg+="\n写入的字节长度："+buffer.length;
            msg+="\n写入的字节："+bytesToString(buffer);
        }
        if(readLen>0){
            msg+="\n读取的长度："+readLen;
        }
        if(receivedBytes.size()>0){
            msg+="\n读取的字节：";
            for (byte[]bytes:receivedBytes){
                msg+="\n"+bytesToString(bytes);
            }
        }
        return msg;
    }

    @Override
    public String toString() {
        StringBuilder sss= new StringBuilder();
        if(receivedBytes!=null){
            for (byte[]bytes:receivedBytes){
                sss.append(bytesToString(bytes));
            }
        }else {
            sss = new StringBuilder("null");
        }
        return "BulkTask{" +
                "readLen=" + readLen +
                ", receivedBytes=" + sss +
                ", buffer=" + bytesToString(buffer) +
                ", usbTransferCallback=" + usbTransferCallback +
                ", timeout=" + timeout +
                ", error=" + (error==null?"null":error.getMessage()) +
                '}';
    }
}
