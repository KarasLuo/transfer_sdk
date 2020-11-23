package com.joseph.transfer_sdk.usb.task;


import com.joseph.transfer_sdk.usb.UsbTransferCallback;
import static com.joseph.transfer_sdk.ByteUtils.bytesToString;

public class ControlTask extends UsbTransferTask {

    public int requestType;
    public int request;
    public int value;
    public int index;
    public int length;
    public byte[]buffer;

    public ControlTask(int requestType, int request, int value,
                       byte[]buffer, int index, int length,
                       int timeout, UsbTransferCallback callback){
        super(timeout,callback);
        this.buffer=buffer;
        this.requestType=requestType;
        this.request=request;
        this.value=value;
        this.index=index;
        this.length=length;
    }

    @Override
    public String getDataString() {
        return "\nrequestType:"+requestType+"\nrequest:"+request
                +"\nvalue:"+value+"\nindex:"+index
                +"\nlength:"+length+"\nbuffer:\n"+bytesToString(buffer);
    }

    @Override
    public String toString() {
        return "ControlTask{" +
                "requestType=" + requestType +
                ", request=" + request +
                ", value=" + value +
                ", index=" + index +
                ", length=" + length +
                ", buffer=" + bytesToString(buffer) +
                ", usbTransferCallback=" + usbTransferCallback +
                ", timeout=" + timeout +
                ", error=" + (error==null?"null":error.getMessage()) +
                '}';
    }
}
