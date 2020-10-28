package com.joseph.demo;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HomeFragment extends BaseFragment {
    private static final String TAG="HomeFragment";

    public HomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.fragment_home, container, false);
        setToolbar();

        TextView tvBle=view.findViewById(R.id.feature_ble);
        tvBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getHoldingActivity().addFragment(new BleListFragment());
            }
        });
        return view;
    }

    @Override
    protected void setToolbar() {
        getHoldingActivity().setToolbar(
                "功能选择",
                R.drawable.ic_shutdown,
                -1,
                null
        );
    }
}