package com.joseph.demo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("MissingPermission")
public class BluetoothChooseDialog extends AlertDialog {
    static final private String TAG="BluetoothChooseDialog";
    private Context context;
    //搜索到的蓝牙
    private List<BluetoothDevice> bleList=new ArrayList<>();
    private BluetoothDevice device;
    private RecyclerView rvBleList;
    private BluetoothChooseListener callBack;
    private RecyclerViewAdapter adapter;

    public BluetoothChooseDialog(@NonNull Context context,
                                 @NonNull BluetoothChooseListener callBack) {
        super(context);
        this.context=context;
        this.callBack=callBack;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_bluetooth);
        setCanceledOnTouchOutside(false);
        TextView tvCancel=findViewById(R.id.tv_bluetooth_list_cancel);
        if (tvCancel != null) {
            tvCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });
        }
        //recycler view
        rvBleList=findViewById(R.id.rv_bluetooth_list);
        rvBleList.setLayoutManager(new LinearLayoutManager(context,
                LinearLayoutManager.VERTICAL, false));
        adapter=new RecyclerViewAdapter();
        rvBleList.setAdapter(adapter);

        if(callBack!=null){
            callBack.startSearchBleDevice();
        }
    }

    @Override
    public void dismiss() {
        if(callBack!=null){
            callBack.stopSearchBleDevice();
        }
        super.dismiss();
    }

    //列表刷新
    public void update(@NonNull List<BluetoothDevice> bleList){
        this.bleList = bleList;
        ViewGroup.LayoutParams lp=rvBleList.getLayoutParams();
        if(bleList.size()>7){
            float scale=context.getResources().getDisplayMetrics().density;
            lp.height=(int)(350*scale+0.5f);
            rvBleList.setLayoutParams(lp);
        }
        adapter.notifyDataSetChanged();
    }

    private class RecyclerViewAdapter extends
            RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view= LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.item_bluetooth,viewGroup,false);
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int i) {
            final int index=i;
            if(viewHolder instanceof ItemViewHolder){
                final ItemViewHolder itemViewHolder=(ItemViewHolder)viewHolder;
                String name=bleList.get(i).getName();
                itemViewHolder.tvBleName.setText(name);
                itemViewHolder.llItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(callBack!=null){
                            callBack.connectChosenDevice(bleList.get(index));
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return bleList.size();
        }

        class ItemViewHolder extends RecyclerView.ViewHolder{
            TextView tvBleName;
            LinearLayout llItem;
            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                llItem=itemView.findViewById(R.id.ll_bluetooth_item);
                tvBleName=itemView.findViewById(R.id.tv_bluetooth_name);
            }
        }
    }

    public interface BluetoothChooseListener {
        //连接选中的设备
        void connectChosenDevice(BluetoothDevice device);
        //开始搜索蓝牙设备
        void startSearchBleDevice();
        //停止搜索蓝牙设备
        void stopSearchBleDevice();
    }

}
