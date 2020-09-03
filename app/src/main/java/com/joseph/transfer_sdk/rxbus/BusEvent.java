package com.joseph.transfer_sdk.rxbus;

public class BusEvent<T>{
    private int event;
    private T body;

    public BusEvent(int event,T body) {
        this.event=event;
        this.body = body;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public T getMsg() {
        return body;
    }

    public void setMsg(T body) {
        this.body = body;
    }
}
