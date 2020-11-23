package com.joseph.demo.usb;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.joseph.demo.BaseFragment;
import com.joseph.demo.R;
import com.joseph.transfer_sdk.usb.UsbClient;
import com.joseph.transfer_sdk.usb.UsbTransferCallback;
import com.joseph.transfer_sdk.usb.task.BulkTask;
import com.joseph.transfer_sdk.usb.task.ControlTask;
import com.joseph.transfer_sdk.usb.task.UsbTransferTask;

import java.util.List;

import static com.joseph.transfer_sdk.ByteUtils.byteToInt;
import static com.joseph.transfer_sdk.ByteUtils.stringToBytes;

public class UsbTransferFragment extends BaseFragment {
    private static final String TAG="UsbTransferFragment";

    private TextView tvConsole;
    private SwitchCompat scTransferType;
    private LinearLayout panelBulk;
    private LinearLayout panelControl;

    private EditText etBulkBuffer;
    private EditText etBulkLen;

    private EditText etControlRequestType;
    private EditText etControlRequest;
    private EditText etControlValue;
    private EditText etControlIndex;
    private EditText etControlBuffer;

    @Override
    public void onDestroy() {
        UsbClient.getInstance().resetConnection();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_usb_transfer, container, false);
        setToolbar();
        tvConsole= view.findViewById(R.id.tv_console);
        panelBulk= view.findViewById(R.id.panel_bulk);
        panelControl= view.findViewById(R.id.panel_control);
        scTransferType= view.findViewById(R.id.sc_transfer_type);
        scTransferType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    scTransferType.setText("control");
                    panelControl.setVisibility(View.VISIBLE);
                    panelBulk.setVisibility(View.GONE);
                }else {
                    scTransferType.setText("buffer");
                    panelControl.setVisibility(View.GONE);
                    panelBulk.setVisibility(View.VISIBLE);
                }
            }
        });
        scTransferType.setChecked(false);
        //bulk
        etBulkBuffer= view.findViewById(R.id.et_bulk_buffer);
        etBulkLen= view.findViewById(R.id.et_bulk_len);
        Button btnBulkTransfer= view.findViewById(R.id.btn_bulk_transfer);
        btnBulkTransfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String buffer=etBulkBuffer.getText().toString();
                Log.i(TAG,"bulk buffer:"+buffer+",length="+buffer.length());
                byte[]bufferBytes;
                if(buffer.replace("\\s","").equals("")){//无内容
                    bufferBytes=null;
                }else if(buffer.matches("([0-9a-fA-F\\s])*")){//字节内容
                    try {
                        bufferBytes=stringToBytes(buffer);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getHoldingActivity(),e.getMessage(),Toast.LENGTH_SHORT).show();
                        return;
                    }
                }else {//非法内容
                    Toast.makeText(getHoldingActivity(),
                            "写入数据非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                String len=etBulkLen.getText().toString();
                Log.i(TAG,"len="+len);
                int receiveLen;
                if(len.equals("")){
                    receiveLen=-1;
                }else if(len.matches("([0-9])*")){
                    receiveLen=Integer.parseInt(len);
                }else {
                    Toast.makeText(getHoldingActivity(),
                            "读取字节长度非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                // bulk transfer
                BulkTask task=new BulkTask(bufferBytes,receiveLen,2000, transferCallback);
                UsbClient.getInstance().addTransferTask(task);
            }
        });
        //control
        etControlBuffer= view.findViewById(R.id.et_control_buffer);
        etControlRequestType= view.findViewById(R.id.et_control_requesttype);
        etControlRequest= view.findViewById(R.id.et_control_request);
        etControlIndex= view.findViewById(R.id.et_control_index);
        etControlValue= view.findViewById(R.id.et_control_value);
        Button btnControlRead= view.findViewById(R.id.btn_control_read);
        btnControlRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String typeStr=etControlRequestType.getText().toString();
                String requestStr=etControlRequest.getText().toString();
                String indexStr=etControlIndex.getText().toString();
                String valueStr=etControlValue.getText().toString();
                String bufferStr=etControlBuffer.getText().toString();
                if(!typeStr.matches("([0-9a-fA-F])*")){
                    Toast.makeText(getHoldingActivity(),
                            "RequestType非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!requestStr.matches("([0-9a-fA-F])*")){
                    Toast.makeText(getHoldingActivity(),
                            "Request非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!indexStr.matches("([0-9a-fA-F\\s])*")){
                    Toast.makeText(getHoldingActivity(),
                            "Index非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!valueStr.matches("([0-9a-fA-F\\s])*")){
                    Toast.makeText(getHoldingActivity(),
                            "Value非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!bufferStr.matches("([0-9])*")){
                    Toast.makeText(getHoldingActivity(),
                            "buffer非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int type=stringToBytes(typeStr)[0];
                    int request=stringToBytes(requestStr)[0];
                    byte[]indexBytes=stringToBytes(indexStr);
                    int index=byteToInt(indexBytes[1],indexBytes[0]);
                    byte[]valueBytes=stringToBytes(valueStr);
                    int value=byteToInt(valueBytes[1],valueBytes[0]);
                    int len=Integer.parseInt(bufferStr);

                    ControlTask task=new ControlTask(type, request, value, new byte[len],
                            index, len, 2000,transferCallback);
                    UsbClient.getInstance().addTransferTask(task);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getHoldingActivity(),e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        Button btnControlWrite= view.findViewById(R.id.btn_control_write);
        btnControlWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String typeStr=etControlRequestType.getText().toString();
                String requestStr=etControlRequest.getText().toString();
                String indexStr=etControlIndex.getText().toString();
                String valueStr=etControlValue.getText().toString();
                String bufferStr=etControlBuffer.getText().toString();
                if(!typeStr.matches("([0-9a-fA-F])*")){
                    Toast.makeText(getHoldingActivity(),
                            "RequestType非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!requestStr.matches("([0-9a-fA-F])*")){
                    Toast.makeText(getHoldingActivity(),
                            "Request非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!indexStr.matches("([0-9a-fA-F\\s])*")){
                    Toast.makeText(getHoldingActivity(),
                            "Index非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!valueStr.matches("([0-9a-fA-F\\s])*")){
                    Toast.makeText(getHoldingActivity(),
                            "Value非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!bufferStr.matches("([0-9a-fA-F\\s])*")){
                    Toast.makeText(getHoldingActivity(),
                            "buffer非法",Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    int type=stringToBytes(typeStr)[0];
                    int request=stringToBytes(requestStr)[0];
                    byte[]indexBytes=stringToBytes(indexStr);
                    int index=byteToInt(indexBytes[1],indexBytes[0]);
                    byte[]valueBytes=stringToBytes(valueStr);
                    int value=byteToInt(valueBytes[1],valueBytes[0]);
                    byte[]buffer=stringToBytes(bufferStr);

                    ControlTask task=new ControlTask(type,request,value,buffer,
                            index,buffer.length,2000,transferCallback);
                    UsbClient.getInstance().addTransferTask(task);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getHoldingActivity(),e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }

    @Override
    protected void setToolbar() {
        getHoldingActivity().setToolbar(
                "数据读写",
                R.drawable.ic_back,
                -1,
                null
        );
    }

    private UsbTransferCallback transferCallback =new UsbTransferCallback() {
        @Override
        public void onSuccess(UsbTransferTask task) {
            tvConsole.setText(task.getDataString());
        }

        @Override
        public void onError(UsbTransferTask task, Exception e) {
            Toast.makeText(getHoldingActivity(),e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    };
}
