package com.joseph.transfer_sdk.exception;

public class TransferException extends Exception {

    public TransferException(){
        super();
    }

    public TransferException(String s){
        super(s);
    }

    public TransferException(String s,Throwable cause){
        super(s,cause);
    }

    public TransferException(Throwable cause){
        super(cause);
    }
}
