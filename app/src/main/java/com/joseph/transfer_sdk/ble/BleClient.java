package com.joseph.transfer_sdk.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import com.joseph.transfer_sdk.ByteUtils;
import com.joseph.transfer_sdk.exception.FeatureNotSupportException;
import com.joseph.transfer_sdk.exception.PermissionNotSupportException;
import com.joseph.transfer_sdk.rxbus.BusConstant;
import com.joseph.transfer_sdk.rxbus.BusEvent;
import com.joseph.transfer_sdk.rxbus.RxBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.joseph.transfer_sdk.rxbus.BusConstant.BLE_EVENT_CONNECTED;
import static com.joseph.transfer_sdk.rxbus.BusConstant.BLE_EVENT_CONNECT_ERROR;
import static com.joseph.transfer_sdk.rxbus.BusConstant.BLE_EVENT_DISCONNECTED;
import static com.joseph.transfer_sdk.rxbus.BusConstant.BLE_EVENT_SCAN_CALLBACK_ERROR;
import static com.joseph.transfer_sdk.rxbus.BusConstant.BLE_EVENT_SCAN_CALLBACK_LIST;
import static com.joseph.transfer_sdk.rxbus.BusConstant.BLE_EVENT_SCAN_CALLBACK_SINGLE;
import static com.joseph.transfer_sdk.rxbus.BusConstant.BLE_EVENT_TRANSFER;

/**
 * 蓝牙功能基本实现
 */
@SuppressLint("MissingPermission")
public class BleClient {
    private static final String TAG="BleClient";
    private Context context;
    private static BleClient instance;

    public static final int BLUETOOTH_REQUEST_CODE=1130;//蓝牙请求码，便于处理反馈

    private BluetoothAdapter bleAdapter;//蓝牙适配器
    private BluetoothGatt bleGatt;//GATT实例
    private String uuidService;
    private String uuidWrite;
    private String uuidRead;
    private String uuidNotify;
    private BluetoothGattCharacteristic bgcWrite;
    private BluetoothGattCharacteristic bgcRead;
    private BluetoothGattCharacteristic bgcNotify;

    private BleSearchCallback searchCallback;
    private BleConnectCallback connectCallback;
    private List<BluetoothDevice> searchedBleList =new ArrayList<>();//保存蓝牙设备实例列表
    private Disposable connectTimeoutDisposable;

    private List<BleTransfer>transferQueue=new ArrayList<>();
    private BleTransfer transferWorking;
    private Disposable transferTimeoutDisposable;
    private Disposable rxbusDisposable;

    private BleClient(@NonNull Context context)throws FeatureNotSupportException{
        this.context=context.getApplicationContext();
        checkBleSupport();
        //接收bleClient的事件
        receiveRxbus();
    }

    /**
     * 获取单例 如果没有实例就创建
     * @param context
     * @return
     */
    public static BleClient getInstance(@NonNull Context context) throws FeatureNotSupportException{
        if(instance==null){
            synchronized (BleClient.class){
                if(instance==null){
                    instance=new BleClient(context);
                }
            }
        }
        return instance;
    }

    /**
     * 销毁单例 回收内存
     */
    public void killInstance(){
        if(rxbusDisposable!=null){
            rxbusDisposable.dispose();
            rxbusDisposable=null;
        }
        if(connectTimeoutDisposable!=null){
            connectTimeoutDisposable.dispose();
            connectTimeoutDisposable=null;
        }
        if(transferTimeoutDisposable!=null){
            transferTimeoutDisposable.dispose();
            transferTimeoutDisposable=null;
        }
        disconnectBluetooth();
        instance=null;
        System.gc();
    }

    /**
     * 监听总线事件
     */
    private void receiveRxbus(){
        rxbusDisposable=RxBus.getInstance().toObservable(BusEvent.class)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BusEvent>() {
                    @Override
                    public void accept(BusEvent busEvent) throws Exception {
                        int event=busEvent.getEvent();
                        switch (event){
                            /**蓝牙搜索**/
                            case BLE_EVENT_SCAN_CALLBACK_SINGLE:
                                //搜索到单个蓝牙的结果，带信号强度
                                ScanResult result=(ScanResult)busEvent.getMsg();
                                if(searchCallback!=null&&result!=null){
                                    searchCallback.onSingle(result);
                                }
                                break;
                            case BLE_EVENT_SCAN_CALLBACK_LIST:
                                //搜索到的所有蓝牙的集合，不带信号强度等信息
                                List<BluetoothDevice> list=(List<BluetoothDevice>)busEvent.getMsg();
                                if(searchCallback!=null&&list!=null){
                                    searchCallback.onList(list);
                                }
                                break;
                            case BLE_EVENT_SCAN_CALLBACK_ERROR:
                                //搜索蓝牙的时候出现了异常
                                Integer errorCode=(Integer) busEvent.getMsg();
                                if(searchCallback!=null&&errorCode!=null){
                                    searchCallback.onError(errorCode);
                                }
                                break;
                            /**蓝牙连接**/
                            case BLE_EVENT_CONNECTED:
                                //蓝牙连接完成
                                BluetoothDevice connected=(BluetoothDevice) busEvent.getMsg();
                                if(connectCallback!=null&&connected!=null){
                                    connectCallback.onConnected(connected);
                                }
                                break;
                            case BLE_EVENT_CONNECT_ERROR:
                                //蓝牙连接出现异常
                                Throwable throwable=(Throwable) busEvent.getMsg();
                                if(connectCallback!=null&&throwable!=null){
                                    connectCallback.onError(throwable);
                                }
                                break;
                            case BLE_EVENT_DISCONNECTED:
                                //蓝牙断开
                                BluetoothDevice disconnected=(BluetoothDevice) busEvent.getMsg();
                                if(connectCallback!=null&&disconnected!=null){
                                    connectCallback.onDisconnected(disconnected);
                                }
                                break;
                                /**蓝牙传输**/
                            case BLE_EVENT_TRANSFER:
                                byte[]readBytes=(byte[])busEvent.getMsg();
                                if(transferWorking!=null){
                                    transferWorking.callback.onReply(readBytes);
                                    transferWorking=null;
                                    doTransfer();
                                }
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    /**
     * 设置GATT服务的UUID
     * 初次实例化后设置
     * @param uuidService
     * @return
     */
    public BleClient setUuidService(String uuidService) {
        this.uuidService = uuidService;
        return this;
    }

    /**
     * 设置服务下的可用特征
     * 初次实例化后设置
     * @param uuidRead
     * @param uuidWrite
     * @param uuidNotify
     * @return
     */
    public BleClient setUuidCharacteristics(String uuidRead,String uuidWrite,String uuidNotify) {
        this.uuidRead = uuidRead;
        this.uuidWrite = uuidWrite;
        this.uuidNotify = uuidNotify;
        return this;
    }

    /**
     * 设置搜索蓝牙的回调
     * @param searchCallback
     */
    public BleClient setSearchCallback(BleSearchCallback searchCallback) {
        this.searchCallback = searchCallback;
        return this;
    }

    /**
     * 设置连接蓝牙的回调
     * @param connectCallback
     */
    public BleClient setConnectCallback(BleConnectCallback connectCallback) {
        this.connectCallback = connectCallback;
        return this;
    }

    /**
     * 检查是否支持低功耗蓝牙功能
     * @throws FeatureNotSupportException
     */
    private void checkBleSupport() throws FeatureNotSupportException {
        boolean hasBle=context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if(!hasBle){
            throw new FeatureNotSupportException("device doesn't support Bluetooth LE feature");
        }
        BluetoothManager bluetoothManager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            throw new FeatureNotSupportException("failed to obtain BluetoothManager");
        }
        bleAdapter = bluetoothManager.getAdapter();
        if (bleAdapter == null) {
            throw new FeatureNotSupportException("failed to obtain BluetoothAdapter");
        }
    }

    /**
     * 搜索蓝牙
     * @param activity
     * @param filterServiceUUIDs
     * @throws PermissionNotSupportException
     */
    public void startSearchBluetooth(AppCompatActivity activity,List<String> filterServiceUUIDs)
            throws PermissionNotSupportException {
        searchedBleList.clear();
        String blPermission="android.permission.BLUETOOTH";
        int value=context.checkCallingOrSelfPermission(blPermission);
        Log.e(TAG,"permission value="+value);
        if(value!=PackageManager.PERMISSION_GRANTED){
            throw new PermissionNotSupportException("app required permission:"+blPermission);
        }
        if(!bleAdapter.isEnabled()){
            //请求系统打开蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent,BLUETOOTH_REQUEST_CODE);
            return;
        }

        //设置过滤选项和扫描设置
        List<ScanFilter> filters = new ArrayList<>();
        if(filterServiceUUIDs!=null){
            for (String uuid:filterServiceUUIDs){
                ScanFilter scanFilter = new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(uuid)).build();
                filters.add(scanFilter);
            }
        }
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        //开始扫描
        try{
            bleAdapter.getBluetoothLeScanner()
                    .startScan(filters, scanSettings, scanCallback);
            Log.i(TAG,"start scan ble");
        }catch (Exception e){
            Log.e(TAG,"start ble scan:"+e.getMessage());
        }
    }

    /**
     * 停止扫描
     */
    public void stopSearchBluetooth(){
        try{
            bleAdapter.getBluetoothLeScanner()
                    .stopScan(scanCallback);
            Log.i(TAG,"stop scan ble");
        }catch (Exception e){
            Log.e(TAG,"stop ble scan:"+e.getMessage());
        }
    }

    /**
     * 搜索蓝牙的回调
     */
    private ScanCallback scanCallback=new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice bleDevice=result.getDevice();
            if(!searchedBleList.contains(bleDevice)){
                //添加搜索到的蓝牙
                searchedBleList.add(bleDevice);
                Log.i(TAG,"*****************onScanResult*****************");
                Log.i(TAG,"rssi="+result.getRssi());
                Log.i(TAG,"name="+bleDevice.getName());
                Log.i(TAG,"address="+bleDevice.getAddress());
                Log.i(TAG,"*****************END*****************");
                //搜索到蓝牙，走事件总线
                RxBus.getInstance().post(new BusEvent<>(
                        BLE_EVENT_SCAN_CALLBACK_SINGLE,
                        result));
                RxBus.getInstance().post(new BusEvent<>(
                        BLE_EVENT_SCAN_CALLBACK_LIST,
                        searchedBleList));
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG,"*****************onScanFailed*****************");
            Log.e(TAG,"ble scan errorCode="+errorCode);
            Log.e(TAG,"*****************END*****************");
            RxBus.getInstance().post(new BusEvent<>(
                    BLE_EVENT_SCAN_CALLBACK_ERROR,
                    errorCode));
        }
    };

    /**
     * 连接蓝牙设备
     * @param device
     * @param timeout s
     */
    public void connectBluetooth(BluetoothDevice device,int timeout){
        if(connectTimeoutDisposable!=null){
            connectTimeoutDisposable.dispose();
            connectTimeoutDisposable=null;
        }
        connectTimeoutDisposable= Observable.just(device)
                .delay(100,TimeUnit.MILLISECONDS)
                .map(new Function<BluetoothDevice, BluetoothGatt>() {
                    @Override
                    public BluetoothGatt apply(BluetoothDevice bd) throws Exception {
                        Log.i(TAG,"connect device="+bd.getName()+"("+bd.getAddress()+")");
                        bleGatt=bd.connectGatt(context,false,bleGattCallback);
                        return bleGatt;
                    }
                }).delay(timeout, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BluetoothGatt>() {
                    @Override
                    public void accept(BluetoothGatt bluetoothGatt) throws Exception {
                        if (bleGatt==null) {
                            disconnectBluetooth();
                            RxBus.getInstance().post(new BusEvent<>(
                                    BLE_EVENT_CONNECT_ERROR,
                                    new TimeoutException("bluetooth connect timeout!")));
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        disconnectBluetooth();
                        RxBus.getInstance().post(new BusEvent<>(
                                BLE_EVENT_CONNECT_ERROR,
                                throwable));
                    }
                });
    }

    /**
     * 释放GATT连接
     */
    public void disconnectBluetooth(){
        Log.i(TAG,"^^^^^disconnectBluetooth^^^^^^^^^");
        if (bleGatt!=null){
            bleGatt.disconnect();
            bleGatt=null;
        }
    }

    /**
     * 设置Characteristic的通知可用
     * @param bgc
     */
    private void setNotifyEnable(BluetoothGattCharacteristic bgc){
        if(bgc==null){
            Log.e(TAG,"setNotifyEnable bgc is null!");
            return;
        }
        List<BluetoothGattDescriptor>descriptors=bgc.getDescriptors();
        if(descriptors==null){
            Log.e(TAG,"setNotifyEnable descriptors is null!");
            return;
        }
        Log.i(TAG,"descriptors size="+descriptors.size());
        for (BluetoothGattDescriptor d:descriptors){
            d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean isWrite=bleGatt.writeDescriptor(d);
            Log.i(TAG,"descriptor isWrite="+isWrite);
        }
    }

    /**
     * 蓝牙GATT回调
     */
    private BluetoothGattCallback bleGattCallback =new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int s, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "连接成功 ：" + BluetoothProfile.STATE_CONNECTED);
                    boolean b=bleGatt.discoverServices();
                    Log.i(TAG, "启动服务发现:"+ b);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.w(TAG, "断开连接 :" + BluetoothProfile.STATE_DISCONNECTED);
                    RxBus.getInstance().post(new BusEvent<>(
                            BLE_EVENT_DISCONNECTED,
                            gatt.getDevice()));
                    gatt.close();
                    break;
                default:
                    Log.w(TAG, "其它状态：" +newState);
                    RxBus.getInstance().post(new BusEvent<>(
                            BLE_EVENT_DISCONNECTED,
                            gatt.getDevice()));
                    gatt.close();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int s) {
            //获取service
            BluetoothGattService service=gatt.getService(UUID.fromString(uuidService));
            if(service==null){
                RxBus.getInstance().post(new BusEvent<>(
                        BLE_EVENT_CONNECT_ERROR,
                        new Exception("bluetooth service("+uuidService+") not found!")));
                disconnectBluetooth();
                return;
            }
            //获取characteristic
            List<String>notFoundChars=new ArrayList<>();
            if(uuidNotify!=null){
                bgcNotify=service.getCharacteristic(UUID.fromString(uuidNotify));
                setNotifyEnable(bgcNotify);
                if(bgcNotify==null){
                    notFoundChars.add(uuidNotify);
                }
            }
            if(uuidRead!=null){
                bgcRead=service.getCharacteristic(UUID.fromString(uuidRead));
                if(bgcRead==null){
                    notFoundChars.add(uuidRead);
                }
            }
            if(uuidWrite!=null){
                bgcWrite=service.getCharacteristic(UUID.fromString(uuidWrite));
                if(bgcWrite==null){
                    notFoundChars.add(uuidWrite);
                }
            }
            //检查
            if(notFoundChars.size()>0){
                Log.e(TAG,"获取characteristic不全："+notFoundChars.size());
                RxBus.getInstance().post(new BusEvent<>(
                        BLE_EVENT_CONNECT_ERROR,
                        new Exception("bluetooth characteristic not found:"
                                + Arrays.toString(notFoundChars.toArray()))));
            }else {
                RxBus.getInstance().post(new BusEvent<>(
                        BLE_EVENT_CONNECTED,
                        gatt.getDevice()));
                Log.w(TAG,"连接完成："+gatt.getDevice().getName());
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int s) {
            if (s == BluetoothGatt.GATT_SUCCESS) {
                byte[]bytes=characteristic.getValue();
                Log.e(TAG,"onCharacteristicRead:"+ ByteUtils.bytesToString(bytes));
                RxBus.getInstance().post(new BusEvent<>(BLE_EVENT_TRANSFER, bytes));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int s) {
            if (s == BluetoothGatt.GATT_SUCCESS) {
                byte[]bytes=characteristic.getValue();
                Log.e(TAG,"onCharacteristicWrite:"+ ByteUtils.bytesToString(bytes));
                RxBus.getInstance().post(new BusEvent<>(BLE_EVENT_TRANSFER, bytes));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[]bytes=characteristic.getValue();
            Log.e(TAG,"onCharacteristicChanged:"+ ByteUtils.bytesToString(bytes));
            RxBus.getInstance().post(new BusEvent<>(BLE_EVENT_TRANSFER, bytes));
        }
    };

    /**
     * 写入字节数据 字节数组限制长度为20
     * @param value
     * @throws Exception
     */
    private void writeCharacteristic(byte[]value)throws Exception{
        if(value.length>20){
            throw new IndexOutOfBoundsException("Bluetooth LE transfer limited in 20 bytes");
        }
        if(bgcWrite==null){
            throw new NullPointerException("the BluetoothGattCharacteristic for write is null!");
        }
        if(bgcNotify!=null){
            bleGatt.setCharacteristicNotification(bgcNotify, true);
        }
        //开始写数据
        bgcWrite.setValue(value);
        bleGatt.writeCharacteristic(bgcWrite);
    }

    /**
     * 检查notify特征是否为空
     * @throws Exception
     */
    private void checkNotifyCharacteristic()throws Exception{
        if(bgcNotify==null){
            throw new NullPointerException("the BluetoothGattCharacteristic for notify is null!");
        }
    }

    /**
     * 读取字节（主动）
     * @throws Exception
     */
    private void readCharacteristic()throws Exception{
        if(bgcRead==null){
            throw new NullPointerException("the BluetoothGattCharacteristic for read is null!");
        }
        bleGatt.readCharacteristic(bgcRead);
    }

    /**
     * 添加单个的蓝牙传输任务
     * @param task
     */
    public void addTransferTask(BleTransfer task){
        transferQueue.add(task);
        doTransfer();
    }

    /**
     * 添加一组传输任务
     * @param tasks
     */
    public void addTransferTaskList(List<BleTransfer> tasks){
        transferQueue.addAll(tasks);
        doTransfer();
    }

    /**
     * 执行传输任务
     */
    @UiThread
    private void doTransfer(){
        Log.e(TAG,"doTransfer");
        if(transferQueue.size()<=0){
            Log.w(TAG,"蓝牙传输队列无任务");
            return;
        }
        if(transferWorking!=null){
            Log.w(TAG,"蓝牙数据包尚未传输完毕,无法执行新的任务");
            return;
        }
        transferWorking=transferQueue.remove(0);
        transferWorking.timestamp=System.currentTimeMillis();
        Log.w(TAG,"设置超时时间");
        if(transferTimeoutDisposable !=null){
            transferTimeoutDisposable.dispose();
            transferTimeoutDisposable =null;
        }
        transferTimeoutDisposable = Observable.just(transferWorking)
                .delay(transferWorking.timeout, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BleTransfer>() {
                    @Override
                    public void accept(BleTransfer bleTransfer) throws Exception {
                        if(bleTransfer.equals(transferWorking)){
                            Log.w(TAG,"蓝牙传输任务超时");
                            transferWorking.callback.onTimeout(bleTransfer);
                            transferWorking=null;
                            doTransfer();
                        }
                    }
                });
        try{
            Log.w(TAG,"执行任务。。。");
            switch (transferWorking.transferType){
                case BleTransfer.TASK_TRANSFER_WRITE:
                    writeCharacteristic(transferWorking.buffer);
                    break;
                case BleTransfer.TASK_TRANSFER_READ:
                    readCharacteristic();
                    break;
                case BleTransfer.TASK_TRANSFER_NOTIFY:
                    checkNotifyCharacteristic();
                    break;
                default:
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
