package com.daou.xenmanager.core;

import com.daou.xenmanager.entity.ServerInfo;
import com.daou.xenmanager.exception.STAFXenApiException;
import com.ibm.staf.STAFException;
import com.xensource.xenapi.*;
import org.apache.xmlrpc.XmlRpcException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by user on 2016-12-07.
 */
public class XenCore {
    public static final int GET_TYPE_VM = 1000;
    public static final int GET_TYPE_SNAP = 1001;
    private Connection connection;

    public void connect(ServerInfo target)throws STAFXenApiException{
        try{
            connection = new Connection(new URL("http://" + target.hostName));
            Session.loginWithPassword(connection, target.userName, target.password, APIVersion.latest().toString());
        }catch (MalformedURLException e){
            throw new STAFXenApiException("URL", e.getMessage());
        }catch (Exception e){
            throw new STAFXenApiException("CONNECTION", e.toString());
        }
    }

    public void disconnect() throws STAFXenApiException{
        try{
            Session.logout(connection);
        }catch (Exception e){
            throw new STAFXenApiException("LOGOUT", e.getMessage());
        }
    }

    public Map<String, String> getVMListByType(int type) throws STAFXenApiException{
        Map<String, String> map = new HashMap<String, String>();
        try{
            Map<VM, VM.Record> recordMap = VM.getAllRecords(connection);
            Set<VM> st = recordMap.keySet();
            for (VM vm : st){
                boolean isSnapshot = vm.getIsASnapshot(connection);
                boolean isTemplate = vm.getIsATemplate(connection);
                boolean isControlDomain = vm.getIsControlDomain(connection);
                VM.Record record = recordMap.get(vm);
                if(type == GET_TYPE_SNAP && isSnapshot){
                    map.put(record.uuid, record.nameLabel);
                }else if(type == GET_TYPE_VM && !isTemplate && !isControlDomain){
                    map.put(record.uuid, record.nameLabel);
                }
            }
        }catch (Exception e){
            throw new STAFXenApiException("LIST", e.toString());
        }
        return map;
    }

    public VM.Record createVMBySnapshot(String snapName, String snapUuid, String vmName) throws STAFXenApiException {
        VM.Record vmRecord = null;
        try {
            VM targetVM;
            VM.Record snapRecord;
            //find snap-shot by name
                Set<VM> vmSet = VM.getByNameLabel(connection, snapName);
                if (vmSet.size() != 0) {
                    for (VM vm : vmSet) {
                        snapRecord = vm.getRecord(connection);
                        if(snapRecord.isASnapshot && snapUuid.equals(snapRecord.uuid)){
                            snapRecord.nameLabel = vmName;
                            snapRecord.isControlDomain = false;
                            snapRecord.isATemplate = false;
                            targetVM = VM.create(connection, snapRecord);
                            vmRecord = targetVM.getRecord(connection);
                            break;
                        }
                    }
                }
        } catch (Exception e) {
            throw new STAFXenApiException("CREATE VM", e.toString());
        }
        return vmRecord;
    }

    public String removeVMByName(String vmName, String vmUuid) throws STAFXenApiException{
        String result = null;
        try{
            VM.Record vmRecord;
            Set<VM> vmSet = VM.getByNameLabel(connection, vmName);
            //does not exist vmname
            if(vmSet == null || vmSet.size() == 0){
                throw new STAFXenApiException("REMOVE VM", "CHECK VMNAME, DOES NOT EXIST NAME");
            }
            //destroy vm but not cascading delete
            for (VM vm : vmSet) {
                vmRecord = vm.getRecord(connection);
                if(!vmRecord.isASnapshot && !vmRecord.isATemplate && !vmRecord.isControlDomain && vmUuid.equals(vmRecord.uuid)){
                    vm.destroy(connection);
                    result = vmRecord.nameLabel;
                    break;
                }
            }
        }catch (Exception e){
            throw new STAFXenApiException("REMOVE VM", e.toString());
        }
        return result;
    }
}
