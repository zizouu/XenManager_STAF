package com.daou.xenmanager.service;

import com.daou.xenmanager.entity.ServerInfo;
import com.daou.xenmanager.exception.STAFXenApiException;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.VM;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by user on 2016-12-14.
 */
public interface XenService {
    public void connect(ServerInfo target)throws STAFXenApiException;
    public void disconnect() throws STAFXenApiException;
    public Map<String, String> getVMListByType(int type) throws STAFXenApiException;
    public VM.Record createVMBySnapshot(String snapName, String snapUuid, String vmName) throws STAFXenApiException ;
    public String removeVMByName(String vmName, String vmUuid) throws STAFXenApiException;
}
