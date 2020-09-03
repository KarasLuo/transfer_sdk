package com.joseph.transfer_sdk.exception;

public class FeatureNotSupportException extends Exception {

    public FeatureNotSupportException(){
        super();
    }

    public FeatureNotSupportException(String s){
        super(s);
    }

    public FeatureNotSupportException(String s,Throwable cause){
        super(s,cause);
    }

    public FeatureNotSupportException(Throwable cause){
        super(cause);
    }
}
