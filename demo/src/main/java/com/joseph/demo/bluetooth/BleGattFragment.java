package com.joseph.demo.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.joseph.demo.BaseFragment;
import com.joseph.demo.MyApplication;
import com.joseph.demo.R;
import com.joseph.transfer_sdk.ble.BleClient;
import com.joseph.transfer_sdk.ble.BleConnectCallback;
import com.joseph.transfer_sdk.ble.BleServiceCallback;

import java.util.ArrayList;
import java.util.List;

public class BleGattFragment extends BaseFragment {
    private static final String TAG="GattFragment";

    private static final String FRAGMENT_PARAM1 = "BleDevice";

    private BluetoothDevice bleDevice;

    private ProgressBar progressBar;
    private RecyclerView rvGatt;
    private List<GattResult>gattResults;
    private RVAdapter rvAdapter;

    private class GattResult{
        boolean isService;//true为服务项，false为特征项
        String uuidService;
        BluetoothGattCharacteristic bgc;
        GattResult(String uuid){
            isService=true;
            uuidService=uuid;
        }
        GattResult(BluetoothGattCharacteristic characteristic){
            isService=false;
            bgc =characteristic;
        }
    }

    private BleGattFragment() {

    }

    public static BleGattFragment newInstance(BluetoothDevice ble) {
        BleGattFragment fragment = new BleGattFragment();
        Bundle args = new Bundle();
        args.putParcelable(FRAGMENT_PARAM1, ble);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bleDevice = getArguments().getParcelable(FRAGMENT_PARAM1);
        }
        gattResults=new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v= inflater.inflate(R.layout.fragment_ble_gatt, container, false);
        setToolbar();
        progressBar=v.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        rvGatt=v.findViewById(R.id.rv_gatt);
        rvGatt.setLayoutManager(new LinearLayoutManager(v.getContext()));
        rvAdapter=new RVAdapter(gattResults);
        rvGatt.setAdapter(rvAdapter);
        rvGatt.setVisibility(View.GONE);
        if(bleDevice!=null){
            BleClient.getInstance()
                    .setConnectCallback(new BleConnectCallback() {
                        @Override
                        public void onConnected(BluetoothDevice device) {
                            rvGatt.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Toast.makeText(MyApplication.getAppContext(),
                                    "蓝牙连接出错",Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onDisconnected(BluetoothDevice device) {
                            Toast.makeText(MyApplication.getAppContext(),
                                    "蓝牙已断开",Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setServiceCallback(new BleServiceCallback() {
                        @Override
                        public void onList(List<BluetoothGattService> serviceList) {
                            gattResults.clear();
                            Log.w(TAG,"######### gatt service ##########");
                            for (BluetoothGattService bgs:serviceList){
                                Log.i(TAG,"** service uuid="+bgs.getUuid().toString()
                                        +", type="+bgs.getType());
                                gattResults.add(new GattResult(bgs.getUuid().toString()));
                                List<BluetoothGattCharacteristic>bgcList=bgs.getCharacteristics();
                                for (BluetoothGattCharacteristic bgc:bgcList){
                                    Log.i(TAG,"  *  characteristic uuid="+bgc.getUuid());
                                    Log.i(TAG,"     characteristic properties="+bgc.getProperties());
                                    Log.i(TAG,"     characteristic permission="+bgc.getPermissions());
                                    Log.i(TAG,"     characteristic write type="+bgc.getWriteType());
                                    gattResults.add(new GattResult(bgc));
                                }
                            }
                            Log.w(TAG,"######### gatt service ##########");
                            rvAdapter.notifyDataSetChanged();
                        }
                    })
                    .connectBluetooth(v.getContext(),bleDevice,15);
        }
        return v;
    }

    @Override
    public void onDestroyView() {
        gattResults.clear();
        BleClient.getInstance().disconnectBluetooth();
        super.onDestroyView();
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


    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> {

        private final List<GattResult> mValues;

        public RVAdapter(List<GattResult> items) {
            mValues = items;
        }

        @Override
        public RVAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_gatt, parent, false);
            return new RVAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RVAdapter.ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            if(holder.mItem.isService){
                holder.layoutService.setVisibility(View.VISIBLE);
                holder.layoutCharacteristic.setVisibility(View.GONE);
                holder.tvServiceUuid.setText("Service UUID\n "+holder.mItem.uuidService);
            }else {
                holder.layoutService.setVisibility(View.GONE);
                holder.layoutCharacteristic.setVisibility(View.VISIBLE);
                holder.tvChaUuid.setText("Characteristic UUID:\n\t"+holder.mItem.bgc.getUuid().toString());
                //解析有哪些属性
                int property=holder.mItem.bgc.getProperties();
                StringBuilder bs= new StringBuilder(Integer.toBinaryString(property));
                if(bs.length()<8){
                    int times=8-bs.length();
                    for (int i=0;i<times;i++){
                        bs.insert(0, "0");
                    }
                }
                Log.i(TAG,"binary string="+bs);
                //按位对应
                boolean writeEnable=false;
                boolean readEnable=false;
                StringBuilder resultBuilder=new StringBuilder("Properties:\t\t");
                if(bs.charAt(0)=='1'){
                    resultBuilder.append("EXTENDED_PROPS").append("\t");
                }
                if(bs.charAt(1)=='1'){
                    resultBuilder.append("SIGNED_WRITE").append("\t");
                }
                if(bs.charAt(2)=='1'){
                    resultBuilder.append("INDICATE").append("\t");
                }
                if(bs.charAt(3)=='1'){
                    resultBuilder.append("NOTIFY").append("\t");
                }
                if(bs.charAt(4)=='1'){
                    resultBuilder.append("WRITE").append("\t");
                    writeEnable=true;
                }
                if(bs.charAt(5)=='1'){
                    resultBuilder.append("WRITE_NO_RESPONSE").append("\t");
                    writeEnable=true;
                }
                if(bs.charAt(6)=='1'){
                    resultBuilder.append("READ").append("\t");
                    readEnable=true;
                }
                if(bs.charAt(7)=='1'){
                    resultBuilder.append("BROADCAST").append("\t");
                }
                resultBuilder.append("\nWrite Type:\t\t");
                if(holder.mItem.bgc.getWriteType()==1){
                    resultBuilder.append("NO_RESPONSE").append("\t");
                } else if(holder.mItem.bgc.getWriteType()==2){
                    resultBuilder.append("DEFAULT").append("\t");
                } else if(holder.mItem.bgc.getWriteType()==4){
                    resultBuilder.append("SIGNED").append("\t");
                }
                Log.w(TAG,"\n"+resultBuilder.toString());
                holder.tvChaMsg.setText(resultBuilder.toString());

                final String transferService=holder.mItem.bgc.getService().getUuid().toString();
                final String transferRead=readEnable ?holder.mItem.bgc.getUuid().toString():null;
                final String transferWrite=writeEnable ?holder.mItem.bgc.getUuid().toString():null;
                final ArrayList<String>notifyList=new ArrayList<>();
                BluetoothGattService bgs=holder.mItem.bgc.getService();
                for (BluetoothGattCharacteristic bgc:bgs.getCharacteristics()){
                    notifyList.add(bgc.getUuid().toString());
                }
                holder.tvTransfer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getHoldingActivity().addFragment(BleTransferFragment.newInstance(
                                bleDevice,
                                transferService,
                                transferRead,
                                transferWrite,
                                notifyList
                        ));
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public GattResult mItem;
            public final ConstraintLayout layoutService;
            public final TextView tvServiceUuid;
            public final ConstraintLayout layoutCharacteristic;
            public final TextView tvChaMsg;
            public final TextView tvChaUuid;
            public final TextView tvTransfer;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                layoutService = (ConstraintLayout) view.findViewById(R.id.layout_service);
                tvServiceUuid = (TextView) view.findViewById(R.id.tv_service_uuid);
                layoutCharacteristic = (ConstraintLayout) view.findViewById(R.id.layout_characteristic);
                tvChaMsg = (TextView) view.findViewById(R.id.tv_characteristic_msg);
                tvChaUuid = (TextView) view.findViewById(R.id.tv_characteristic_uuid);
                tvTransfer = (TextView) view.findViewById(R.id.tv_transfer);
            }
        }
    }
}