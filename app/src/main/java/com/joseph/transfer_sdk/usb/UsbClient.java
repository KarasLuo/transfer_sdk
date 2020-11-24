package com.joseph.transfer_sdk.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;


import androidx.annotation.NonNull;

import com.joseph.transfer_sdk.exception.PermissionNotSupportException;
import com.joseph.transfer_sdk.exception.TransferException;
import com.joseph.transfer_sdk.usb.task.BulkTask;
import com.joseph.transfer_sdk.usb.task.ControlTask;
import com.joseph.transfer_sdk.usb.task.UsbTransferTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UsbClient {
    private static final String TAG="UsbClient";

    private static class ClientHolder{
        private static final UsbClient INSTANCE=new UsbClient();
    }

    private UsbManager usbManager;
    private HashMap<String, UsbDevice> deviceList=new HashMap<>();
    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbEndpoint usbEpOut;
    private UsbEndpoint usbEpIn;

    private UsbReceiver usbReceiver;

    /**
     * 管理usb读写数据的队列
     */
    private List<UsbTransferTask> transferQueue =new ArrayList<>();
    private Disposable transferDisposable;

    private UsbClient(){
    }

    /**
     * 获取单例 如果没有实例就创建
     * @return
     */
    public static final UsbClient getInstance() {
        UsbClient client= UsbClient.ClientHolder.INSTANCE;
        Log.w(TAG,"usb实例:"+client);
        return client;
    }

    public void init(@NonNull Context context, UsbReceiver.UsbBroadcastListener listener){
        Log.w(TAG,"init usb");
        this.usbManager=(UsbManager)context.getSystemService(Context.USB_SERVICE);
        usbReceiver=new UsbReceiver();
        usbReceiver.setUsbBroadcastListener(listener);
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(UsbReceiver.ACTION_REQUIRE_USB_PERMISSION);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbReceiver,intentFilter);
    }

    public void resetConnection(){
        Log.w(TAG,"resetConnection usb");
        if(usbInterface!=null){
            if(usbDeviceConnection!=null){
                usbDeviceConnection.releaseInterface(usbInterface);
                usbDeviceConnection.close();
            }
            usbInterface=null;
        }
        usbDevice=null;
        usbDeviceConnection=null;
        usbEpOut=null;
        usbEpIn=null;
        deviceList.clear();

        transferQueue.clear();
        if(transferDisposable!=null){
            if(!transferDisposable.isDisposed()){
                transferDisposable.dispose();
            }
            transferDisposable=null;
        }
    }

    public void clear(Context context){
        Log.w(TAG,"killInstance usb");
        resetConnection();
        usbManager=null;
        try{
            context.unregisterReceiver(usbReceiver);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.gc();
    }

    /**
     * 查找设备和端口并发起权限广播
     * @param vendorId
     * @param productId
     * @param interfaceId bulk传输的接口
     * @return hasPermission
     */
    public boolean initTransferState(Context context,
                                  int vendorId,
                                  int productId,
                                  int interfaceId)throws Exception {
        Log.i(TAG,"initTransferState:try find the interface of usb device. info:vendorId="
                +vendorId+",productId="+productId+",interfaceId="+interfaceId);
        if(findDeviceList()==null){
            throw new Exception("usb DeviceList is null");
        }
        if(findUsbDevice(vendorId,productId)==null){
            throw new NullPointerException("UsbDevice[vendorId="+vendorId+
                    ",productId="+productId+"] not found!");
        }
        if(findUsbInterface(interfaceId)==null){
            throw new NullPointerException("UsbInterface[id="+interfaceId+"] not found!");
        }
        if(!findEndpoint(UsbConstants.USB_ENDPOINT_XFER_BULK)){
            throw new Exception("UsbEndpoint[USB_ENDPOINT_XFER_BULK] not found!");
        }
        //申请权限
        if(usbDevice!=null&&usbManager!=null){
            if(!usbManager.hasPermission(usbDevice)){
                Log.i(TAG,"申请usb权限");
                Intent intent=new Intent(UsbReceiver.ACTION_REQUIRE_USB_PERMISSION);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                usbManager.requestPermission(usbDevice,pendingIntent);
            }else {
                Log.i(TAG,"已有usb设备的权限！");
                return true;
            }
        }else {
            throw new NullPointerException("usbDevice==null或usbManager==null");
        }
        return false;
    }

    /**
     * 获取设备列表
     * @return
     */
    public HashMap<String, UsbDevice> findDeviceList(){
        if(usbManager==null){
            Log.e(TAG,"usbManager is null");
            return null;
        }
        deviceList=usbManager.getDeviceList();
        Log.i(TAG,"deviceList="+deviceList.toString());
        return deviceList;
    }

    /**
     * 查找指定usb设备
     * @param vendorId 供应商id
     * @param productId 产品id
     * @return
     */
    private UsbDevice findUsbDevice(int vendorId, int productId){
        Log.i(TAG,"device list size="+deviceList.size());
        for (String key:deviceList.keySet()){
            UsbDevice device=deviceList.get(key);
            if(device==null){
                continue;
            }
            Log.i(TAG,"device="+device.getDeviceName()
                    +",vendorId="+device.getVendorId()
                    +",productId="+device.getProductId());
            if(device.getVendorId()==vendorId&&device.getProductId()==productId){
                usbDevice=device;
                Log.i(TAG,"usb设备="+usbDevice.getDeviceName());
                return device;
            }
        }
        return null;
    }

    /**
     * 查找指定usb接口
     * @param interfaceId 接口ID
     * @return
     */
    private UsbInterface findUsbInterface(int interfaceId){
        if(usbDevice==null){
            Log.e(TAG,"usbDevice is null");
            return null;
        }
        int interfaceCount=usbDevice.getInterfaceCount();
        for (int i=0;i<interfaceCount;i++){
            UsbInterface tempInterface=usbDevice.getInterface(i);
            int id=tempInterface.getId();
            Log.i(TAG,"find interface id="+id);
            if(interfaceId==id){
                usbInterface=tempInterface;
                return tempInterface;
            }
        }
        return null;
    }

    /**
     * 查找端点
     * @param type
     * <ul>
     *  <li>{@link UsbConstants#USB_ENDPOINT_XFER_CONTROL} (endpoint zero) 特殊控制端口
     *  <li>{@link UsbConstants#USB_ENDPOINT_XFER_ISOC} (isochronous endpoint) 不支持
     *  <li>{@link UsbConstants#USB_ENDPOINT_XFER_BULK} (bulk endpoint) 传输大量数据
     *  <li>{@link UsbConstants#USB_ENDPOINT_XFER_INT} (interrupt endpoint) 传输少量数据
     * </ul>
     */
    private boolean findEndpoint(int type){
        if(usbInterface==null){
            Log.e(TAG,"usbInterface is null");
            return false;
        }
        int epCount=usbInterface.getEndpointCount();
        for (int i=0;i<epCount;i++){
            UsbEndpoint ep=usbInterface.getEndpoint(i);
            Log.i(TAG,"find ep="+ep.toString()+",type="+ep.getType()+",dir="+ep.getDirection());
            if(ep.getType()== type){
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    usbEpOut = ep;
                    Log.i(TAG,"find epOut");
                }
                if (ep.getDirection() == UsbConstants.USB_DIR_IN) {
                    usbEpIn = ep;
                    Log.i(TAG,"find epIn");
                }
                if(usbEpOut!=null&&usbEpIn!=null){
                    Log.i(TAG,"find all ep");
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 打开USB
     * @return
     */
    public void openUsb()throws Exception{
        if(usbDevice==null||usbManager==null){
            Log.e(TAG,"usbDevice==null或usbManager==null");
            throw new NullPointerException("usbDevice==null或usbManager==null");
        }
        if(!usbManager.hasPermission(usbDevice)){
            Log.e(TAG,usbDevice+"has no permission");
            throw new PermissionNotSupportException("UsbDevice["+usbDevice.toString()
                    +"] has no permission.");
        }
        UsbDeviceConnection connection=usbManager.openDevice(usbDevice);
        if(connection==null){
            throw new Exception("Failed to open UsbDevice["+usbDevice.toString()+"].");
        }
        usbDeviceConnection=connection;
    }

    public void addTransferTask(UsbTransferTask task){
        Log.w(TAG,"添加usb读写任务");
        transferQueue.add(task);
        doTransfer();
    }

    /**
     * 消化任务队列，串行传输
     */
    private void doTransfer(){
        Log.w(TAG,"doTransfer");
        if(transferQueue.size()<=0){
            Log.e(TAG,"队列长度为0");
            return;
        }
        if(transferDisposable!=null){
            Log.e(TAG,"usb读写尚未结束:"+transferQueue.size());
            return;
        }
        transferDisposable= Observable.just(transferQueue.remove(0))
                .map(new Function<UsbTransferTask, UsbTransferTask>() {
                    @Override
                    public UsbTransferTask apply(UsbTransferTask task) throws Exception {
                        //根据类型决定传输端点
                        if(task instanceof BulkTask){
                            return transfer((BulkTask) task);
                        }
                        if(task instanceof ControlTask){
                            return transfer((ControlTask) task);
                        }
                        return null;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UsbTransferTask>() {
                    @Override
                    public void accept(UsbTransferTask task) throws Exception {
                        if (task.error != null) {
                            task.usbTransferCallback.onError(task,task.error);
                        } else {
                            task.usbTransferCallback.onSuccess(task);
                        }
                        if(transferDisposable!=null){
                            if(!transferDisposable.isDisposed()){
                                transferDisposable.dispose();
                            }
                            transferDisposable=null;
                        }
                        doTransfer();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        if(transferDisposable!=null){
                            if(!transferDisposable.isDisposed()){
                                transferDisposable.dispose();
                            }
                            transferDisposable=null;
                        }
                        doTransfer();
                    }
                });

    }

    /**
     * usb 用户数据读写
     * @param task
     * @return
     * @throws Exception
     */
    private BulkTask transfer(BulkTask task){
        if(usbEpOut==null||usbEpIn==null){
            task.error=new TransferException("UsbEndpoint is null");
            return task;
        }
        if(usbInterface==null){
            task.error=new TransferException("UsbInterface is null");
            return task;
        }
        if(usbDeviceConnection==null){
            task.error=new TransferException("UsbDeviceConnection is null");
            return task;
        }
        try{
            //占用usb
            usbDeviceConnection.claimInterface(usbInterface,true);
            //写入
            if(task.buffer!=null){
                int len=usbDeviceConnection.bulkTransfer(usbEpOut,task.buffer,
                        task.buffer.length,task.timeout);
                if(len<0){
                    usbDeviceConnection.releaseInterface(usbInterface);//解除占用
                    throw new TransferException("Failed to write to usb :len"+len
                            +",params="+task.toString());
                }
                Log.i(TAG,"写入完成");
            }
            //读取
            if(task.readLen>0){
                int reaminder=task.readLen;//剩下的字节数
                Log.w(TAG,"responseLength="+reaminder);
                while (reaminder > 0) {//循环读取
                    int maxSize = usbEpIn.getMaxPacketSize();
                    //当前有效长度
                    int validLen = reaminder - maxSize > 0 ? maxSize : reaminder;
                    //bulk
                    byte[] tempBytes = new byte[validLen];
                    int len=usbDeviceConnection.bulkTransfer(usbEpIn, tempBytes, validLen, task.timeout);
                    if(len<0){
                        usbDeviceConnection.releaseInterface(usbInterface);
                        throw new TransferException("Failed to read from usb :len=" +len
                                +",params="+task.toString());
                    }
                    task.receivedBytes.add(tempBytes);
                    reaminder = reaminder - maxSize;
                }
                //检查是否传输完成
                int reLen=0;
                for (byte[] arr:task.receivedBytes){
                    reLen+=arr.length;
                }
                if(reLen!=task.readLen){
                    throw new TransferException("Read usb data error:"+reLen+"/"
                            +task.readLen+" bytes");
                }
                Log.i(TAG,"读取完成");
            }
            //解除占用
            usbDeviceConnection.releaseInterface(usbInterface);
        }catch (Exception e){
            e.printStackTrace();
            task.error=e;
        }
        return task;
    }

    /**
     *
     * @param task
     * @return
     * @throws Exception
     */
    private ControlTask transfer(ControlTask task){
        //endpoint zero
        if(usbInterface==null){
            task.error=new TransferException("usbInterface is null");
            return task;
        }
        if(usbDeviceConnection==null){
            task.error=new TransferException("usbDeviceConnection is null");
            return task;
        }
        try{
            //占用usb
            usbDeviceConnection.claimInterface(usbInterface,true);
            //control
            int len=usbDeviceConnection.controlTransfer(
                    task.requestType,
                    task.request,
                    task.value,
                    task.index,
                    task.buffer,
                    task.length,
                    task.timeout);
            //解除占用
            Log.w(TAG,"解除usb占用");
            usbDeviceConnection.releaseInterface(usbInterface);
            if(len<0){
                throw new TransferException("transfer error: len="+len);
            }
        }catch (Exception e){
            task.error=e;
        }
        return task;
    }

}
