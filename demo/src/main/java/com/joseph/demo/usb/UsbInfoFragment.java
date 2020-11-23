package com.joseph.demo.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.joseph.demo.BaseFragment;
import com.joseph.demo.R;
import com.joseph.transfer_sdk.usb.UsbClient;
import com.joseph.transfer_sdk.usb.UsbReceiver;

import java.util.HashMap;

public class UsbInfoFragment extends BaseFragment {
    private static final String TAG="UsbFragment";
    private Context context;

    private TextView tvDeviceInfo;
    private EditText etVendorId;
    private EditText etProductId;
    private EditText etInterfaceId;
    private TextView btnOpen;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_usb_info, container, false);
        setToolbar();
        context=view.getContext();
        tvDeviceInfo=view.findViewById(R.id.tv_device_info);
        tvDeviceInfo.setText("USB未识别");
        etVendorId= view.findViewById(R.id.et_vendor_id);
        etProductId= view.findViewById(R.id.et_product_id);
        etInterfaceId= view.findViewById(R.id.et_interface_id);
        btnOpen= view.findViewById(R.id.btn_open);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!etVendorId.getText().toString().matches("([1-9]{1,10})")){
                    Toast.makeText(getHoldingActivity(),
                            "Vendor Id 无效",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!etProductId.getText().toString().matches("([1-9]{1,10})")){
                    Toast.makeText(getHoldingActivity(),
                            "Product Id 无效",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!etInterfaceId.getText().toString().matches("([1-9]{1,10})")){
                    Toast.makeText(getHoldingActivity(),
                            "Interface Id 无效",Toast.LENGTH_SHORT).show();
                    return;
                }
                //查找端口
                try {
                    boolean hasPermission=UsbClient.getInstance().initTransferState(context,
                            Integer.parseInt(etVendorId.getText().toString()),
                            Integer.parseInt(etProductId.getText().toString()),
                            Integer.parseInt(etInterfaceId.getText().toString()));
                    if(hasPermission){
                        UsbClient.getInstance().openUsb();
                        //跳转
                        getHoldingActivity().addFragment(new UsbTransferFragment());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getHoldingActivity(),
                            e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        //初始化
        UsbClient.getInstance().init(context,
                new UsbReceiver.UsbBroadcastListener() {
                    @Override
                    public void onRequirePermission(UsbDevice device, boolean isGranted) {
                        try {
                            UsbClient.getInstance().openUsb();
                            //跳转
                            getHoldingActivity().addFragment(new UsbTransferFragment());
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getHoldingActivity(),
                                    e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAttached() {
                        HashMap<String,UsbDevice>devices=UsbClient.getInstance().findDeviceList();
                        String msg=devices.toString();
//                        Log.w(TAG,"onAttached="+msg);
                        tvDeviceInfo.setText(msg);
                        String vendor="";
                        String product="";
                        if(devices.size()==1){
                            for (String key:devices.keySet()){
                                UsbDevice device=devices.get(key);
                                if(device!=null){
                                    vendor=device.getVendorId()+"";
                                    product=device.getProductId()+"";
                                }
                            }
                        }
                        etVendorId.setText(vendor);
                        etProductId.setText(product);
                        Toast.makeText(getHoldingActivity(),
                                "USB已插入",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onDetached() {
                        Toast.makeText(getHoldingActivity(),"USB已拔出",Toast.LENGTH_SHORT).show();
                        tvDeviceInfo.setText("USB未识别");
                        UsbClient.getInstance().resetConnection();
                    }
                });
        return view;
    }

    @Override
    public void onDestroyView() {
        UsbClient.getInstance().clear(context);
        super.onDestroyView();
    }

    @Override
    protected void setToolbar() {
        getHoldingActivity().setToolbar(
                "USB设备",
                R.drawable.ic_back,
                -1,
                null
        );
    }
}
