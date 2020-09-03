package com.joseph.transfer_sdk.exception;

public class PermissionNotSupportException extends Exception{

    public PermissionNotSupportException(){
        super();
    }

    public PermissionNotSupportException(String s){
        super(s);
    }

    public PermissionNotSupportException(String s,Throwable cause){
        super(s,cause);
    }

    public PermissionNotSupportException(Throwable cause){
        super(cause);
    }
}
