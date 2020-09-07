package com.joseph.demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.joseph.transfer_sdk.ByteUtils;
import com.joseph.transfer_sdk.ble.BleClient;
import com.joseph.transfer_sdk.ble.BleConnectCallback;
import com.joseph.transfer_sdk.ble.BleSearchCallback;
import com.joseph.transfer_sdk.ble.BleTransfer;
import com.joseph.transfer_sdk.exception.FeatureNotSupportException;
import com.joseph.transfer_sdk.exception.PermissionNotSupportException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

import static com.joseph.transfer_sdk.ByteUtils.byteToInt;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private static final String TAG="MainActivity";

    private BleClient bleClient;
    private PV300R_I dataParser;

    private BluetoothChooseDialog bleDialog;
    public static final int BLUETOOTH_REQUEST_CODE=303;//蓝牙请求码，便于处理反馈
    public static final int PERMISSION_REQUEST_CODE=922;//蓝牙请求码，便于处理反馈

    private TextView tvDevice;
    private TextView tvOutput;
    private EditText etInput;

    @Override
    protected void onDestroy() {
        bleClient.killInstance();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvOutput=findViewById(R.id.tv_output);
        etInput=findViewById(R.id.et_input);
        tvDevice=findViewById(R.id.tv_device_name);
        tvDevice.setText("未连接:点我搜索");
        tvDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //动态申请权限
                String[] perms = {Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION};
                if(!EasyPermissions.hasPermissions(MainActivity.this,perms)){
                    EasyPermissions.requestPermissions(new PermissionRequest.Builder(
                            MainActivity.this,
                            PERMISSION_REQUEST_CODE,perms)
                            .setRationale("麻烦同意一哈权限呐")
                            .build());
                }else {
                    showBleDialog();
                }
            }
        });

        Button btnReadVoltage=findViewById(R.id.btn_ble_read_voltage);
        btnReadVoltage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //读取电池电压
                List<BleTransfer> tasks=new ArrayList<>();
                tasks.add(new BleTransfer(
                        BleTransfer.TASK_TRANSFER_WRITE,
                        PV300R_I.getOrderOfReadVoltage(),
                        100,
                        bleWriteCallback));
                tasks.add(new BleTransfer(
                        BleTransfer.TASK_TRANSFER_NOTIFY,
                        null,
                        100,
                        bleVoltageCallback));
                bleClient.addTransferTaskList(tasks);
                tvOutput.setText("电池电压检测");
            }
        });
        Button btnAutoDetect=findViewById(R.id.btn_ble_auto_detect);
        btnAutoDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //自动检测一次
                List<BleTransfer> tasks=new ArrayList<>();
                tasks.add(new BleTransfer(
                        BleTransfer.TASK_TRANSFER_WRITE,
                        PV300R_I.getOrderOfAutoDetect(),
                        100,
                        bleWriteCallback));
                tasks.add(new BleTransfer(
                        BleTransfer.TASK_TRANSFER_NOTIFY,
                        null,
                        1000,
                        bleAutoDetectCallback));
                for (int i=1;i<50;i++){
                    tasks.add(new BleTransfer(
                            BleTransfer.TASK_TRANSFER_NOTIFY,
                            null,
                            300,
                            bleAutoDetectCallback));
                }
                bleClient.addTransferTaskList(tasks);
                tvOutput.setText("自动检测（单次）");
            }
        });

        //动态申请权限
        String[] perms = {Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if(!EasyPermissions.hasPermissions(this,perms)){
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this,
                    PERMISSION_REQUEST_CODE,perms)
                    .build());
        }

        dataParser=new PV300R_I();
        try {
            bleClient=BleClient.getInstance(this)
                    .setUuidService(dataParser.getServiceUUID())
                    .setUuidCharacteristics(
                            dataParser.getReadUUID(),
                            dataParser.getWriteUUID(),
                            dataParser.getNotifyUUID()
                    )
                    .setSearchCallback(bleSearchCallback)
                    .setConnectCallback(bleConnectCallback);
        } catch (FeatureNotSupportException e) {
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        Log.w(TAG,"onPermissionsGranted requestCode="+requestCode+"\n"+ Arrays.toString(list.toArray()));
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        Log.w(TAG,"onPermissionsDenied requestCode="+requestCode+"\n"+ Arrays.toString(list.toArray()));
//        Toast.makeText(this,"不给权限不干事儿", Toast.LENGTH_SHORT).show();
        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case BLUETOOTH_REQUEST_CODE:
                Log.i(TAG, "resultCode=" + resultCode);
                if (resultCode == Activity.RESULT_OK) {
                    showBleDialog();
                }
                break;
            case AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE:
                Log.w(TAG,"DEFAULT_SETTINGS_REQ_CODE");
                break;
            default:
                break;
        }
    }

    private void showBleDialog(){
        if(bleDialog!=null){
            bleDialog.dismiss();
        }
        bleDialog=new BluetoothChooseDialog(this,
                new BluetoothChooseDialog.BluetoothChooseListener() {
            @Override
            public void connectChosenDevice(BluetoothDevice device) {
                String dn=device.getName()==null?
                        " "+device.getAddress(): ":"+device.getName();
                tvDevice.setText("正在连接"+dn);
                bleDialog.dismiss();
                bleClient.disconnectBluetooth();
                bleClient.connectBluetooth(device,20);
            }

            @Override
            public void startSearchBleDevice() {
                List<String>uuids=new ArrayList<>();
                uuids.add(dataParser.getServiceUUID());
                try {
                    bleClient.startSearchBluetooth(MainActivity.this, uuids);
                } catch (PermissionNotSupportException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,"缺少蓝牙权限",Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void stopSearchBleDevice() {
                bleClient.stopSearchBluetooth();
            }
        });
        bleDialog.show();
    }

    /**
     * 蓝牙搜索的回调
     */
    private BleSearchCallback bleSearchCallback=new BleSearchCallback() {
        @Override
        public void onSingle(ScanResult result) {

        }

        @Override
        public void onList(List<BluetoothDevice> list) {
            bleDialog.update(list);
        }

        @Override
        public void onError(int errorCode) {
            Toast.makeText(MainActivity.this,"蓝牙搜索异常",Toast.LENGTH_SHORT)
                    .show();
        }
    };

    /**
     * 蓝牙连接操作的回调
     */
    private BleConnectCallback bleConnectCallback=new BleConnectCallback() {
        @Override
        public void onConnected(BluetoothDevice device) {
            String dn=device.getName()==null?
                    " "+device.getAddress(): ":"+device.getName();
            tvDevice.setText("已连接"+dn);
        }

        @Override
        public void onError(Throwable throwable) {
            Toast.makeText(MainActivity.this,"蓝牙连接发生错误:"+throwable.getMessage(),
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDisconnected(BluetoothDevice device) {
            String dn=device.getName()==null?
                    " "+device.getAddress(): ":"+device.getName();
            tvDevice.setText("已断开"+dn);
        }
    };

    private BleTransfer.BleTransferCallback bleWriteCallback=new BleTransfer.BleTransferCallback() {

        @Override
        public void onReply(byte[] bytes) {
            Toast.makeText(MainActivity.this,
                    "数据写入完成:"+ ByteUtils.bytesToString(bytes),
                    Toast.LENGTH_SHORT)
                    .show();
        }

        @Override
        public void onTimeout(BleTransfer task) {
            Toast.makeText(MainActivity.this,
                    "数据写入超时:"+ ByteUtils.bytesToString(task.buffer),
                    Toast.LENGTH_SHORT)
                    .show();
        }
    };

    private BleTransfer.BleTransferCallback bleVoltageCallback=new BleTransfer.BleTransferCallback() {
        @Override
        public void onReply(byte[] bytes) {
            if(bytes[0]==(byte)0xaa&&bytes[1]==0x55&&bytes[2]==(byte)0x97){
                int temp=byteToInt(bytes[5],bytes[4]);
                double vol = 3.3/4096*(temp)*1.83;
                NumberFormat nf = new DecimalFormat( "0.0000");
                double voltage= Double.parseDouble(nf.format(vol));

                String str=tvOutput.getText().toString()+"\n电压="+voltage+"V";
                tvOutput.setText(str);
            }
        }

        @Override
        public void onTimeout(BleTransfer task) {
            String str=tvOutput.getText().toString()+"\n获取电压超时";
            tvOutput.setText(str);
        }
    };

    private BleTransfer.BleTransferCallback bleAutoDetectCallback=new BleTransfer.BleTransferCallback() {
        @Override
        public void onReply(byte[] bytes) {
            if(bytes[0]==(byte)0xaa&&bytes[1]==0x55&&bytes[2]==(byte)0x83){
                int[] data_16 = {byteToInt(bytes[9], bytes[8]),
                        byteToInt(bytes[15], bytes[14], bytes[13], bytes[12])};
                int voltage = byteToInt(bytes[11], bytes[10]);
                int temperature = byteToInt(bytes[7], bytes[6]);
                String str=tvOutput.getText().toString();
                str+="\n波长="+data_16[0]+",光强="+(data_16[1]/1020);
                tvOutput.setText(str);
            }

        }

        @Override
        public void onTimeout(BleTransfer task) {
            String str=tvOutput.getText().toString()+"\n自动检测超时";
            tvOutput.setText(str);
        }
    };
}
