package com.joseph.demo.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.joseph.demo.BaseFragment;
import com.joseph.demo.R;
import com.joseph.transfer_sdk.ble.BleClient;
import com.joseph.transfer_sdk.ble.BleGattNotifyCallback;
import com.joseph.transfer_sdk.ble.BleTask;

import java.util.ArrayList;
import java.util.List;

import static com.joseph.transfer_sdk.ByteUtils.byteToInt;
import static com.joseph.transfer_sdk.ByteUtils.bytesToString;
import static com.joseph.transfer_sdk.ByteUtils.stringToBytes;

public class BleTransferFragment extends BaseFragment {
    private static final String TAG="TransferFragment";

    private static final String PARAM_BLE_DEVICE = "BleDevice";
    private static final String PARAM_SERVICE = "service";
    private static final String PARAM_CHARACTERISTIC_READ = "read";
    private static final String PARAM_CHARACTERISTIC_WRITE = "write";
    private static final String PARAM_CHARACTERISTIC_NOTIFY = "notify";

    private BluetoothDevice bleDevice;
    private String serviceUUID=null;
    private String readUUID=null;
    private String writeUUID=null;
    private ArrayList<String> notifyUUIDs;
    private String notifyUUID=null;

    private TextView tvState;
    private TextView tvConsole;
    private EditText etInput;
    private TextView btnRead;
    private TextView btnWrite;
    private TextView btnNotify;

    public BleTransferFragment() {

    }

    public static BleTransferFragment newInstance(BluetoothDevice bleDevice,
                                                  String serviceUUID,
                                                  String readUUID,
                                                  String writeUUID,
                                                  ArrayList<String> notifyUUID) {
        BleTransferFragment fragment = new BleTransferFragment();
        Bundle args = new Bundle();
        args.putParcelable(PARAM_BLE_DEVICE, bleDevice);
        args.putString(PARAM_SERVICE, serviceUUID);
        args.putString(PARAM_CHARACTERISTIC_READ, readUUID);
        args.putString(PARAM_CHARACTERISTIC_WRITE, writeUUID);
        args.putStringArrayList(PARAM_CHARACTERISTIC_NOTIFY, notifyUUID);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bleDevice = getArguments().getParcelable(PARAM_BLE_DEVICE);
            serviceUUID = getArguments().getString(PARAM_SERVICE);
            readUUID = getArguments().getString(PARAM_CHARACTERISTIC_READ);
            writeUUID = getArguments().getString(PARAM_CHARACTERISTIC_WRITE);
            notifyUUIDs = getArguments().getStringArrayList(PARAM_CHARACTERISTIC_NOTIFY);
        }
    }

    @Override
    public void onDestroyView() {
        BleClient.getInstance().resetCharacteristic();
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_ble_transfer, container, false);
        setToolbar();

        tvState=view.findViewById(R.id.tv_state);
        updateState();
        tvConsole=view.findViewById(R.id.tv_console);
        tvConsole.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvConsole.setText("");
        tvConsole.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String msg=tvConsole.getText().toString();
                if(msg.length()>5000){
                    msg=msg.substring(3000);
                    tvConsole.setText(msg);
                }
            }
        });
        etInput=view.findViewById(R.id.et_input);
        etInput.setText("");
        btnRead= view.findViewById(R.id.btn_read);
        //读写操作依赖于选中的characteristic，为空则不可用
        if(readUUID==null){
            btnRead.setEnabled(false);
        }
        btnRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateConsole("",false);
                BleClient.getInstance().addTransferTask(new BleTask(
                        BleTask.TASK_TRANSFER_READ,
                        null,
                        2000,
                        transferCallback
                ));
            }
        });
        btnWrite= view.findViewById(R.id.btn_write);
        if(writeUUID==null){
            btnWrite.setEnabled(false);
        }
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateConsole("",false);
                String input=etInput.getText().toString();
                byte[]inBytes;
                try {
                    inBytes=stringToBytes(input);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(view.getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                    return;
                }
                BleClient.getInstance().addTransferTask(new BleTask(
                        BleTask.TASK_TRANSFER_WRITE,
                        inBytes,
                        2000,
                        transferCallback
                ));
            }
        });
        //选择监听的notify characteristic
        btnNotify= view.findViewById(R.id.btn_notify);
        btnNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 弹框选择notify
                NotifyListDialog dialog=new NotifyListDialog(
                        getHoldingActivity(),
                        notifyUUIDs,
                        new NotifyListDialog.DialogCallback() {
                            @Override
                            public void onItem(String uuid) {
                                Log.w(TAG,"choose notify uuid="+uuid);
                                notifyUUID=uuid;
                                updateState();
                            }
                        }
                );
                dialog.show();
            }
        });
        //单独设置notify的回调
        BleClient.getInstance().setNotifyCallback(new BleGattNotifyCallback() {
            @Override
            public void onNotify(byte[] bytes) {
                Log.i(TAG,"notify:"+bytesToString(bytes));
                updateConsole("notify:"+bytesToString(bytes),true);
            }
        });
        return view;
    }

    @Override
    protected void setToolbar() {
        String title="";
        if(bleDevice!=null){
            title=bleDevice.getName()==null?bleDevice.getAddress():bleDevice.getName();
        }
        getHoldingActivity().setToolbar(
                title,
                R.drawable.ic_back,
                -1,
                null
        );
    }

    /**
     * 更新状态，重设characteristic
     */
    private void updateState(){
        String state="service:"+serviceUUID+"\nread:"+readUUID
                +"\nwrite:"+writeUUID+"\nnotify:"+ notifyUUID;
        try {
            BleClient.getInstance().setCharacteristic(serviceUUID, notifyUUID,readUUID,writeUUID);
        } catch (Exception e) {
            e.printStackTrace();
            state=e.getMessage();
        }
        tvState.setText(state);
    }

    /**
     * 更新输出内容
     * @param msg
     * @param isAppend
     */
    private void updateConsole(String msg,boolean isAppend){
        if(isAppend){
            String oldMsg=tvConsole.getText().toString();
            tvConsole.setText(String.format("%s\n%s", oldMsg, msg));
        }else {
            tvConsole.setText(msg);
        }
        int offset=tvConsole.getLineCount()*tvConsole.getLineHeight();
        if(offset>tvConsole.getHeight()){
            tvConsole.scrollTo(0,offset-tvConsole.getHeight());
        }
        tvConsole.invalidate();
    }

    /**
     * 传输数据的回调
     */
    private BleTask.BleTransferCallback transferCallback=new BleTask.BleTransferCallback() {
        @Override
        public void onReply(byte[] bytes) {
            Log.i(TAG,"transfer:"+bytesToString(bytes));
            updateConsole("transfer:"+bytesToString(bytes),true);
        }

        @Override
        public void onTimeout(BleTask task) {
            Log.i(TAG,"传输超时:"+task.transferType);
            updateConsole("transfer超时:"+task.transferType,true);
        }
    };

}