package com.daou.xenmanager.entity;

/**
 * Created by user on 2016-12-07.
 */
public class XenServer {
    public final String hostName;
    public final String userName;
    public final String password;

    public XenServer(String hostName, String userName, String password)
    {
        this.hostName = hostName;
        this.userName = userName;
        this.password = password;
    }
}
