package com.joseph.demo;

import com.trello.rxlifecycle4.components.support.RxFragment;

public abstract class BaseFragment extends RxFragment {
    private static final String TAG="BaseFragment";

    abstract protected void setToolbar();

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            setToolbar();
        }
    }

    public ToolActivity getHoldingActivity(){
        if(getActivity()==null){
            throw new NullPointerException("fragment["+this+"].getActivity() is null!");
        }
        if(getActivity()instanceof ToolActivity){
            return (ToolActivity)getActivity();
        }else {
            throw new ClassCastException("activity["+getActivity()+"] could not cast to ToolActivity.class");
        }
    }
}
