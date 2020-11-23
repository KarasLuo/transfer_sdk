# transfer_sdk #

This is a transfer utils for Bluetooth LE and USB. 

# Install #

- Platform: Android

- Language: java

- Version: 
      [![](https://jitpack.io/v/KarasLuo/transfer_sdk.svg)](https://jitpack.io/#KarasLuo/transfer_sdk)

Add the JitPack repository to your build file.

    allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Add the dependency.

    dependencies {
	        implementation 'com.github.KarasLuo:transfer_sdk:version'
	}


Add declaration in ***AndroidManifest.xml***.

    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH" />

    <uses-feature
        android:name="android.hardware.bluetooth_le"
        android:required="true" />
    <uses-feature android:name="android.hardware.usb.host" />

# BLE #

Require the *ACCESS_FINE_LOCATION* permission *dynamically* on Android level 23+. Accordingly require the *ACCESS_COARSE_LOCATION* permission *dynamically* on Android level 28+. Some libs working for this, such as **RxPermission**. If not, no ble device will be searched. Referece to [https://developer.android.google.cn/guide/topics/connectivity/bluetooth-le#permissions](https://developer.android.google.cn/guide/topics/connectivity/bluetooth-le#permissions)

Initialize BleClient instance(throw *FeatureNotSupportException*).

    try {
            BleClient.getInstance().init(context.getApplicationContext());
    } catch (FeatureNotSupportException e) {
            e.printStackTrace();
    }

Clear BleClient before destroy application.

    BleClient.getInstance().clear();

Overwrite the **onActivityResult** function of Activity. If  *requestCode* equals **BLUETOOTH_REQUEST_CODE** and *resultCode* equals **Activity.RESULT_OK**, the bluetooth switch of andoid device is opened.

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case BLUETOOTH_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    //bluetooth is opened
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

Set GATT Service UUID.
    
    //replace XXXX-XXXX-XXXX by UUID string of BluetoothGattService
    BleClient.getInstance().setUuidService("XXXX-XXXX-XXXX");

Set BluetoothGattCharacteristic UUID for **read**, **write** and **notify** action.
    
    //replace read/write/notify-XXXX-XXXX-XXXX by UUID string of BluetoothGattCharacteristic
    BleClient.getInstance().setUuidCharacteristics(
                "read-XXXX-XXXX-XXXX",
                "write-XXXX-XXXX-XXXX",
                "notify-XXXX-XXXX-XXXX");

Search Bluetooth LE devices(throw *PermissionNotSupportException*).

     try {
            List<String>serviceUUIDs=new ArrayList<>();
            serviceUUIDs.add("XXXX-XXXX-XXXX");//filter: service uuid
            BleClient.getInstance().setSearchCallback(new BleSearchCallback() {
                @Override
                public void onSingle(ScanResult result) {
                   //found device
                   BluetoothDevice device=result.getDevice();
                   int rssi=result.getRssi();
                }

                @Override
                public void onList(List<BluetoothDevice> list) {
                   //found device list
                }

                @Override
                public void onError(int errorCode) {
                   //error
                }
            }).startSearchBluetooth(appCompatActivity,serviceUUIDs);
        } catch (PermissionNotSupportException e) {
            e.printStackTrace();
        }

Stop bluetooth search.

    BleClient.getInstance().stopSearchBluetooth();

Connect bluetooth device.

    BleClient.getInstance()
                    .setConnectCallback(new BleConnectCallback() {
                        @Override
                        public void onConnected(BluetoothDevice device) {
                            //device connected
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            //error
                        }

                        @Override
                        public void onDisconnected(BluetoothDevice device) {
                            //device disconected
                        }
                    })
                    .connectBluetooth(context,bleDevice,15);

Disconnect bluetooth.

    BleClient.getInstance().disconnectBluetooth();

Create transfer task.

        //read task
        BleTask readTask=new BleTask(
                BleTask.TASK_TRANSFER_READ,
                null,
                2000,
                new BleTask.BleTransferCallback() {
                    @Override
                    public void onReply(byte[] bytes) {
                        //read bytes
                    }

                    @Override
                    public void onTimeout(BleTask task) {
                        //timeout
                    }
                }
        );

        //write task
        byte[]buffer=new byte[]{};//write bytes
        BleTask writeTask=new BleTask(
                BleTask.TASK_TRANSFER_WRITE,
                buffer,
                2000,
                new BleTask.BleTransferCallback() {
                    @Override
                    public void onReply(byte[] bytes) {
                        //write bytes
                    }

                    @Override
                    public void onTimeout(BleTask task) {
                        //timeout
                    }
                }
        );

        //notify task
        BleTask notifyTask=new BleTask(
                BleTask.TASK_TRANSFER_NOTIFY,
                null,
                2000,
                new BleTask.BleTransferCallback() {
                    @Override
                    public void onReply(byte[] bytes) {
                        //notify bytes
                        //do something
                    }

                    @Override
                    public void onTimeout(BleTask task) {
                        //timeout
                    }
                }
        );

Add bluetooth transfer task into task queue and excute.
    
        //single task
        BleClient.getInstance().addTransferTask(readTask);
        //task list
        List<BleTask>taskList=new ArrayList<>();
        taskList.add(writeTask);
        taskList.add(notifyTask);
        BleClient.getInstance().addTransferTaskList(taskList);


# USB #

Some device working as host needs open OTG switch on system setting.

Initialize UsbClient instance and define usb broadcast receiver callback.

    UsbClient.getInstance().init(context, new UsbReceiver.UsbBroadcastListener() {
                    @Override
                    public void onRequirePermission(UsbDevice device, boolean isGranted) {
                        //Require UsbDevice Permission and granted
                        //do something
                    }

                    @Override
                    public void onAttached() {
                        //UsbDevice attached
                    }

                    @Override
                    public void onDetached() {
                        //UsbDevice detached
                        UsbClient.getInstance().resetConnection();
                    }
                });

Clear UsbClient before destroy application.

    UsbClient.getInstance().clear(context);

Find UsbDevice by *vendorId* and *productId*, find bulk transfer UsbInterface with *interfaceId*, and initialize endpoints.

            try {
                    boolean hasPermission=UsbClient.getInstance().initTransferState(context,vendorId, productId,interfaceId);
                    if(hasPermission){
                        //initialized and device has permission
                        //do something
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

Connect to UsbDevice interface.

    UsbClient.getInstance().openUsb();

Close Usb connection and reset parameters.

    UsbClient.getInstance().resetConnection();

Do bulk transfer or control transfer. If read only, let *writeBytes* be null; if write only, let *readLength* value be -1. *timeout* is in millisecond.

        // bulk transfer
        BulkTask task=new BulkTask(writeBytes, readLength, timeout, new UsbTransferCallback() {
            @Override
            public void onSuccess(UsbTransferTask task) {
                //success
                BulkTask bulkTask=(BulkTask)task;
                List<byte[]>result=bulkTask.receivedBytes;//read bytes list
            }

            @Override
            public void onError(UsbTransferTask task, Exception e) {
                //error
            }
        });
        UsbClient.getInstance().addTransferTask(task);

        //control transfer -- write buffer
        ControlTask writeTask=new ControlTask(
                requestType,
                request,
                value,
                buffer,
                index,
                buffer.length,
                timeout,
                new UsbTransferCallback() {
                    @Override
                    public void onSuccess(UsbTransferTask task) {

                    }

                    @Override
                    public void onError(UsbTransferTask task, Exception e) {

                    }
                });
        UsbClient.getInstance().addTransferTask(writeTask);

        //control transfer -- read buffer
        ControlTask readTask=new ControlTask(
                requestType,
                request,
                value,
                new byte[length],
                index,
                length,
                timeout,
                new UsbTransferCallback() {
                    @Override
                    public void onSuccess(UsbTransferTask task) {
                        ControlTask controlTask=(ControlTask)task;
                        byte[]result=controlTask.buffer;
                    }

                    @Override
                    public void onError(UsbTransferTask task, Exception e) {

                    }
                });
        UsbClient.getInstance().addTransferTask(readTask);