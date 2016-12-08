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

    public List<VM.Record> getVMListByType(int type) throws STAFXenApiException{
        List<VM.Record> list = new ArrayList<>();
        try{
            Map<VM, VM.Record> temp = VM.getAllRecords(connection);
            Set<VM> st = temp.keySet();
            for (VM vm : st){
                boolean isSnapshot = vm.getIsASnapshot(connection);
                boolean isTemplate = vm.getIsATemplate(connection);
                boolean isControlDomain = vm.getIsControlDomain(connection);

                if(type == GET_TYPE_SNAP && isSnapshot){
                    list.add(temp.get(vm));
                }else if(type == GET_TYPE_VM && !isTemplate && !isControlDomain){
                    list.add(temp.get(vm));
                }
            }
        }catch (Exception e){
            throw new STAFXenApiException("LIST", e.toString());
        }
        return list;
    }

    public VM.Record createVMBySnapshotName(String snapName, String vmName) throws STAFXenApiException {
        VM.Record vmRecord = null;
        try {
            VM targetVM;
            Set<VM> vmSet = VM.getByNameLabel(connection, snapName);
            if (vmSet.size() != 0) {
                //create vm using first snap-shot only
                for (VM vm : vmSet) {
                    VM.Record snapRecord = vm.getRecord(connection);
                    snapRecord.nameLabel = vmName;
                    targetVM = VM.create(connection, snapRecord);
                    vmRecord = targetVM.getRecord(connection);
                    break;
                }
            }
        } catch (Exception e) {
            throw new STAFXenApiException("CREATE VM", e.toString());
        }
        return vmRecord;
    }
}
