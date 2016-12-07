package com.daou.xenmanager.core;

import com.daou.xenmanager.entity.XenServer;
import com.xensource.xenapi.*;
import org.apache.xmlrpc.XmlRpcException;

import java.net.URL;
import java.util.*;

/**
 * Created by user on 2016-12-07.
 */
public class XenCore {
    protected static Connection connection;
    private static String connectionName;

    public static Connection connect(XenServer target)throws Exception{
        connection = new Connection(new URL("http://" + target.hostName));

        Session.loginWithPassword(connection, target.userName, target.password, APIVersion.latest().toString());

        return connection;
    }

    public static List<VM.Record> getAllSnapshot(Connection connection){
        List<VM.Record> list = new ArrayList<>();
        try{
            Map<VM, VM.Record> temp = VM.getAllRecords(connection);
            Set<VM> st = temp.keySet();
            for (VM vm : st){
                boolean isSnapshot = vm.getIsASnapshot(connection);
                if(isSnapshot){
                    list.add(temp.get(vm));
                }
            }
        }catch (Types.XenAPIException e){
            e.printStackTrace();
        }catch (XmlRpcException e){
            e.printStackTrace();
        }
        return list;
    }

    public static void disconnect() throws Exception{
        Session.logout(connection);
    }
}
