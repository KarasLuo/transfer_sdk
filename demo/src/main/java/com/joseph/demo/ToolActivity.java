package com.joseph.demo;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle3.components.support.RxAppCompatActivity;

import io.reactivex.functions.Consumer;

import static com.joseph.transfer_sdk.ble.BleClient.BLUETOOTH_REQUEST_CODE;

public class ToolActivity extends RxAppCompatActivity {
    private static final String TAG="ToolActivity";

    private Toolbar toolbar;
    private Fragment lastFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool);
        //toolbar
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        requestPermission();
        //初始界面
        addFirstFragment(new HomeFragment());
    }

    /**
     * 获取动态权限
     */
    @SuppressLint("CheckResult")
    public void requestPermission() {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        ).compose(this.<Boolean>bindToLifecycle())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            //已开启所有权限
                            Log.i(TAG, "已开启所有权限");
                        } else {
                            //权限被拒绝
                            Log.e(TAG, "权限被拒绝");
                            Toast.makeText(ToolActivity.this,
                                    "既然你不仁，就休怪我不义！",Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case BLUETOOTH_REQUEST_CODE:
                Log.i(TAG, "resultCode=" + resultCode);
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "已开启蓝牙，请重新搜索", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setToolbar(String title, int navIcon, int menuResId,
                           Toolbar.OnMenuItemClickListener itemClickListener){
        toolbar.setTitle(title);
        toolbar.setNavigationIcon(navIcon);
        toolbar.getMenu().clear();
        if(menuResId!=-1&&itemClickListener!=null){
            toolbar.inflateMenu(menuResId);
            toolbar.setOnMenuItemClickListener(itemClickListener);
        }
    }

    public void setToolbarMenuEnable(int index,boolean enable){
        toolbar.getMenu().setGroupEnabled(index,enable);
    }

    public void addFirstFragment(Fragment fragment){
        if(fragment!=null){
            lastFragment=fragment;
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container,fragment)
                    .addToBackStack(((Object)fragment).getClass().getSimpleName())
                    .commit();
        }
    }

    /**
     * 每打开一个fragment，就像打开了一个新世界
     * @param fragment
     */
    public void addFragment(Fragment fragment){
        if(fragment!=null){
            int size= getSupportFragmentManager().getFragments().size();
            lastFragment=getSupportFragmentManager().getFragments().get(size-1);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container,fragment)
                    .addToBackStack(((Object)fragment).getClass().getSimpleName())
                    .hide(lastFragment)
                    .commit();
        }
    }

    /**
     * 世界迎来终结
     * @return
     */
    public boolean removeFragmentWithAnimations(){
        if(getSupportFragmentManager().getBackStackEntryCount()>1){
            getSupportFragmentManager().popBackStack();
            Log.i(TAG,"there are more than 1 fragments");
            return false;
        }else {
            Log.i(TAG,"there is only 1 fragment");
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        if(removeFragmentWithAnimations()){
            finish();
        }
    }

    /**
     * 隐藏软键盘
     * @param view 控件视图
     * @param event 手势事件
     */
    public static void hideKeyboard(MotionEvent event, View view,
                                    ToolActivity activity) {
        try {
            if (view instanceof EditText) {
                int[] location = { 0, 0 };
                view.getLocationInWindow(location);
                int left = location[0], top = location[1], right = left
                        + view.getWidth(), bottom = top + view.getHeight();
                // 判断焦点位置坐标是否在控件内，如果位置在控件外，则隐藏键盘
                if (event.getRawX() < left || event.getRawX() > right
                        || event.getY() < top || event.getRawY() > bottom) {
                    // 隐藏键盘
                    IBinder token = view.getWindowToken();
                    InputMethodManager inputMethodManager = (InputMethodManager) activity
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(token,
                                InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拦截手势监听
     * @param ev 触摸事件
     * @return 布尔
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                View view = getCurrentFocus();
                //调用方法判断是否需要隐藏键盘
                hideKeyboard(ev, view, ToolActivity.this);
                break;

            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 设置界面字体大小不随系统变化
     * @return 资源
     */
    @Override
    public Resources getResources() {
        Resources res=super.getResources();
        Configuration configuration=res.getConfiguration();
        if(configuration.fontScale!=1.0f){
            configuration.fontScale=1.0f;//app的字体缩放还原为1.0，即不缩放
            res.updateConfiguration(configuration,res.getDisplayMetrics());
        }
        return res;
    }
}
