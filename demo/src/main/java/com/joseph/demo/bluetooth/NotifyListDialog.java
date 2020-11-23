package com.joseph.demo.bluetooth;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.joseph.demo.R;

import java.util.ArrayList;

public class NotifyListDialog extends AlertDialog {

    private ArrayList<String>dataList;
    private DialogCallback callback;

    protected NotifyListDialog(@NonNull Context context,
                               ArrayList<String>dataList,
                               DialogCallback callback) {
        super(context);
        this.dataList=dataList;
        this.callback=callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_notify_list);

        RecyclerView rvNotify=findViewById(R.id.rv_notify);
        rvNotify.setLayoutManager(new LinearLayoutManager(rvNotify.getContext()));
        rvNotify.setAdapter(new RvAdapter());
        DividerItemDecoration divider=new DividerItemDecoration(rvNotify.getContext(),
                DividerItemDecoration.HORIZONTAL);
        rvNotify.addItemDecoration(divider);
    }

    private class RvAdapter extends RecyclerView.Adapter<RvAdapter.ViewHolder>{

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view= LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notify,parent,false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final String uuid=dataList.get(position);
            holder.tvItem.setText(uuid);
            holder.tvItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(callback!=null){
                        callback.onItem(uuid);
                        dismiss();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            TextView tvItem;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvItem=itemView.findViewById(R.id.item_notify);
            }
        }
    }

    public interface DialogCallback{
        void onItem(String uuid);
    }
}
