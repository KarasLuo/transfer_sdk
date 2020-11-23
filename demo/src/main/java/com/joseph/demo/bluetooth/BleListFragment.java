package com.joseph.demo.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.joseph.demo.BaseFragment;
import com.joseph.demo.R;
import com.joseph.transfer_sdk.ble.BleClient;
import com.joseph.transfer_sdk.ble.BleSearchCallback;
import com.joseph.transfer_sdk.exception.FeatureNotSupportException;
import com.joseph.transfer_sdk.exception.PermissionNotSupportException;
import com.joseph.transfer_sdk.usb.UsbClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;


public class BleListFragment extends BaseFragment {
    private static final String TAG="BleListFragment";

    private List<ScanResult>bleList;
    private RVAdapter rvAdapter;

    public BleListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void setToolbar() {
        getHoldingActivity().setToolbar(
                "蓝牙列表",
                R.drawable.ic_back,
                R.menu.menu_blelist,
                new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.search:
                                searchDevice();
//                                Toast.makeText(getContext(),"刷新",Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ble_list, container, false);
        Context context;
        context = view.getContext();
        setToolbar();
        //rv list
        RecyclerView rvBleList = (RecyclerView) view;
        rvBleList.setLayoutManager(new LinearLayoutManager(context));
        bleList=new ArrayList<>();
        rvAdapter=new RVAdapter(bleList);
        rvBleList.setAdapter(rvAdapter);

        //初始化
        try {
            BleClient.getInstance().init(context.getApplicationContext());
        } catch (FeatureNotSupportException e) {
            e.printStackTrace();
            Toast.makeText(getHoldingActivity(),e.getMessage(),Toast.LENGTH_LONG).show();
        }
        searchDevice();
        return view;
    }

    @Override
    public void onDestroyView() {
        BleClient.getInstance().clear();
        super.onDestroyView();
    }

    @SuppressLint("CheckResult")
    private void searchDevice(){
        getHoldingActivity().setToolbarMenuEnable(0,false);
        bleList.clear();
        rvAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(),"正在搜索蓝牙，请稍侯",Toast.LENGTH_SHORT).show();
        try {
            BleClient.getInstance().setSearchCallback(new BleSearchCallback() {
                @Override
                public void onSingle(ScanResult result) {
                    bleList.add(result);
                    rvAdapter.notifyDataSetChanged();
                }

                @Override
                public void onList(List<BluetoothDevice> list) {

                }

                @Override
                public void onError(int errorCode) {
                    Toast.makeText(getContext(),"错误代码:"+errorCode,Toast.LENGTH_SHORT).show();
                }
            }).startSearchBluetooth(getHoldingActivity(),null);
        } catch (PermissionNotSupportException e) {
            e.printStackTrace();
            Toast.makeText(getContext(),"没有权限还玩个毛!",Toast.LENGTH_SHORT).show();
        }
        Observable.just("")
                .delay(10, TimeUnit.SECONDS)
                .compose(this.<String>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        getHoldingActivity().setToolbarMenuEnable(0,true);
                        BleClient.getInstance().stopSearchBluetooth();
                    }
                });
    }

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> {

        private final List<ScanResult> mValues;

        public RVAdapter(List<ScanResult> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ble, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mItem = mValues.get(position);
            final ScanResult item=mValues.get(position);
            String name=item.getDevice().getName();
            String address=item.getDevice().getAddress();
            int rssi=item.getRssi();
            Drawable signal;
            if(rssi>-60){
                signal= ContextCompat.getDrawable(
                        holder.mRssi.getContext(), R.drawable.ic_signal_5);
            }else if(rssi>-70){
                signal= ContextCompat.getDrawable(
                        holder.mRssi.getContext(), R.drawable.ic_signal_4);
            }else if (rssi>-80){
                signal= ContextCompat.getDrawable(
                        holder.mRssi.getContext(), R.drawable.ic_signal_3);
            }else if(rssi>-90){
                signal= ContextCompat.getDrawable(
                        holder.mRssi.getContext(), R.drawable.ic_signal_2);
            } else{
                signal= ContextCompat.getDrawable(
                        holder.mRssi.getContext(), R.drawable.ic_signal_1);
            }
            if(signal!=null){
                signal.setBounds(0,0,signal.getMinimumWidth(),signal.getMinimumHeight());
                holder.mRssi.setCompoundDrawables(null,signal,null,null);
            }
            holder.mRssi.setText(rssi+" dBm");
            holder.mName.setText(name==null?"unknown":name);
            holder.mName.setTextColor(name==null?Color.RED: Color.BLUE);
            holder.mAddress.setText(address);
            holder.mBtnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getHoldingActivity().addFragment(BleGattFragment.newInstance(item.getDevice()));
                }
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mName;
            public final TextView mAddress;
            public final TextView mRssi;
            public final TextView mBtnConnect;
            public ScanResult mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mName = (TextView) view.findViewById(R.id.tv_name);
                mAddress = (TextView) view.findViewById(R.id.tv_address);
                mRssi = (TextView) view.findViewById(R.id.tv_rssi);
                mBtnConnect = (TextView) view.findViewById(R.id.tv_connect);
            }
        }
    }
}