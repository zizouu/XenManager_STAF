package com.daou.xenmanager.exception;

/**
 * Created by user on 2016-12-08.
 */
public class STAFXenApiException extends Exception{
    /**
     * Exception by using xen api
     * @param type      which method
     * @param message   error log
     */
    public STAFXenApiException(String type, String message){
        super("XenAPI " + type + " Exception : " + message);
    }
}
