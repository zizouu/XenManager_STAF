package com.daou.xenmanager;

import com.daou.xenmanager.service.XenService;
import com.daou.xenmanager.service.impl.XenServiceImpl;
import com.daou.xenmanager.entity.ServerInfo;
import com.daou.xenmanager.exception.STAFXenApiException;
import com.ibm.staf.*;
import com.ibm.staf.service.STAFCommandParseResult;
import com.ibm.staf.service.STAFCommandParser;
import com.ibm.staf.service.STAFServiceInterfaceLevel30;
import com.xensource.xenapi.VM;

import java.util.*;

/**
 * Created by user on 2016-12-06.
 */
public class XenManagerCore implements STAFServiceInterfaceLevel30{
    private final String kVersion = "1.0.0";
    private XenService xenManager;
    private STAFHandle fHandle;
    private STAFCommandParser fListParser;
    private STAFCommandParser fAddParser;
    private STAFCommandParser fDeleteParser;
    private String fLineSep;

    private String xenHost = "vm2.terracetech.co.kr";
    private String xenUser = "root";
    private String xenPassword = "qwopZX!@";

    public XenManagerCore() {}

    public void setXenService(XenService service){
        this.xenManager = service;
    }

    public STAFResult init(InitInfo info){
        try{
            fHandle = new STAFHandle("STAF/SERVICE/" + info.name);
        }catch (STAFException e){
            return  new STAFResult(STAFResult.STAFRegistrationError, e.toString());
        }
        // Init xenService
        if(xenManager == null){
            xenManager = new XenServiceImpl();
        }
        // LIST parser
        fListParser = new STAFCommandParser();
        fListParser.addOption("LIST", 1, STAFCommandParser.VALUEREQUIRED);
        fLineSep = fHandle.submit2("local", "var", "resolve {STAF/Config/Sep/Line}").result;
        // ADD parser
        fAddParser = new STAFCommandParser();
        fAddParser.addOption("ADD", 1, STAFCommandParser.VALUENOTALLOWED);
        fAddParser.addOption("VM-NAME", 1, STAFCommandParser.VALUEREQUIRED);
        fAddParser.addOption("SNAP-NAME", 1, STAFCommandParser.VALUEREQUIRED);
        fAddParser.addOption("SNAP-UUID", 1, STAFCommandParser.VALUEREQUIRED);
        fAddParser.addOptionNeed("ADD", "VM-NAME");
        fAddParser.addOptionNeed("ADD", "SNAP-NAME");
        fAddParser.addOptionNeed("ADD", "SNAP-UUID");
        // DELETE parser
        fDeleteParser = new STAFCommandParser();
        fDeleteParser.addOption("DELETE", 1, STAFCommandParser.VALUENOTALLOWED);
        fDeleteParser.addOption("VM-NAME", 1, STAFCommandParser.VALUEREQUIRED);
        fDeleteParser.addOption("VM-UUID", 1, STAFCommandParser.VALUEREQUIRED);

        return new STAFResult(STAFResult.Ok);
    }

    public STAFResult acceptRequest(RequestInfo info){
        String lowerRequest = info.request.toLowerCase();
        StringTokenizer requestTokenizer = new StringTokenizer(lowerRequest);
        String request = requestTokenizer.nextToken();
        STAFResult result;
        // call the appropriate method to handle the command
        if (request.equals("list")){
            result = handleList(info);
        }else if(request.equals("add")){
            result = handleAdd(info);
        }else if(request.equals("delete")){
            result = handleDelete(info);
        }else{
            //invalid request
            result = new STAFResult(STAFResult.InvalidRequestString,"Unknown XenManagerCore Request: " + lowerRequest);
        }
        return result;
    }

    public STAFResult handleList(RequestInfo info){
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 2){
            return new STAFResult(STAFResult.AccessDenied,"Trust level 2 required for LIST request. Requesting machine's trust level: "+info.trustLevel);
        }
        //About xen var
        //About STAF var
        STAFResult resolveResult;
        String resultString = "";
        String listValue = "";
        //verify valid request
        STAFCommandParseResult parsedRequest = fListParser.parse(info.request);
        if (parsedRequest.rc != STAFResult.Ok){
            return new STAFResult(STAFResult.InvalidRequestString, parsedRequest.errorBuffer);
        }
        //verify list type
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("list"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        listValue = resolveResult.result;
        //xen api logic
        try{
            Map<String, String> xenResultMap;
            xenManager.connect(new ServerInfo(xenHost, xenUser, xenPassword));
            if("vm".equals(listValue)){
                //request vm
                xenResultMap = xenManager.getVMListByType(XenServiceImpl.GET_TYPE_VM);
            }else if("snap-shot".equals(listValue)){
                //request snap-shot
                xenResultMap = xenManager.getVMListByType(XenServiceImpl.GET_TYPE_SNAP);
            }else{
                //Invalid request value
                xenManager.disconnect();
                return new STAFResult(STAFResult.InvalidRequestString, "LIST value is required VM | SNAP-SHOT");
            }
            //set result map definition
            STAFMarshallingContext mc = new STAFMarshallingContext();
            STAFMapClassDefinition mapClass = new STAFMapClassDefinition("XEN_MANAGER/LIST/MAP");
            for (String uuid : xenResultMap.keySet()){
                mapClass.addKey(uuid, uuid);
            }
            mc.setMapClassDefinition(mapClass);
            //create result map
            Map resultMap = mapClass.createInstance();
            for(Object uuid : xenResultMap.keySet()){
                resultMap.put(uuid, xenResultMap.get(uuid));
            }
            mc.setRootObject(resultMap);
            resultString = mc.marshall();
        }catch (STAFXenApiException e){
            return new STAFResult(STAFResult.UserDefined, e.toString());
        }
        return new STAFResult(STAFResult.Ok, resultString, true);
    }

    public STAFResult handleAdd(RequestInfo info){
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 3){
            return new STAFResult(STAFResult.AccessDenied,"Trust level 3 required for ADD request. Requesting machine's trust level: " +  info.trustLevel);
        }
        // About STAF var
        STAFResult resolveResult;
        String resultString, vmName, snapName, snapUuid;
        STAFCommandParseResult parsedRequest = fAddParser.parse(info.request);
        //check validate request
        if (parsedRequest.rc != STAFResult.Ok){
            return new STAFResult(STAFResult.InvalidRequestString,
                    parsedRequest.errorBuffer);
        }
        //check validation vm-name
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("vm-name"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        vmName = resolveResult.result;
        //check validation snap-name
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("snap-name"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        snapName = resolveResult.result;
        //check validation snap-uuid
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("snap-uuid"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        snapUuid = resolveResult.result;
        //xen logic
        try{
            xenManager.connect(new ServerInfo(xenHost, xenUser, xenPassword));
            //find snap-shot logic
            VM.Record record = xenManager.createVMBySnapshot(snapName, snapUuid, vmName);
            //if snap-shot is not exist
            if(record == null){
                return new STAFResult(STAFResult.InvalidRequestString, "CHECK YOUR SNAP-SHOT NAME OR UUID");
            }
            //create vm success
            else{
                resultString = "SUCCESS ADD VM named of " + record.nameLabel + "";
            }
        }catch (STAFXenApiException e){
            return new STAFResult(STAFResult.UserDefined, e.toString());
        }
        return new STAFResult(STAFResult.Ok, resultString);
    }

    public STAFResult handleDelete(RequestInfo info){
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 4){
            return new STAFResult(STAFResult.AccessDenied,
                    "Trust level 4 required for DELETE request. Requesting " +
                            "machine's trust level: " +  info.trustLevel);
        }
        //About STAF var
        String resultString = "";
        String vmName, vmUuid;
        STAFResult resolveResult;
        //check validation of request
        STAFCommandParseResult parsedRequest = fDeleteParser.parse(info.request);
        if (parsedRequest.rc != STAFResult.Ok){
            return new STAFResult(STAFResult.InvalidRequestString, parsedRequest.errorBuffer);
        }
        //check validation of vm-name
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("vm-name"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        vmName = resolveResult.result;
        //check validation of vm-uuid
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("vm-uuid"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        vmUuid = resolveResult.result;
        //xen api logic
        try{
            xenManager.connect(new ServerInfo(xenHost, xenUser, xenPassword));
            //check is exist vm
            String name = xenManager.removeVMByName(vmName, vmUuid);
            if(name == null){
                return new STAFResult(STAFResult.InvalidRequestString, "CHECK YOUR VM NAME OR UUID");
            }else{
                resultString = "SUCCESS DELETE VM named of " + name + "";
            }
        }catch (STAFXenApiException e){
            return new STAFResult(STAFResult.UserDefined, e.toString());
        }
        return new STAFResult(STAFResult.Ok, resultString);
    }

    public STAFResult handleHelp(){
        return new STAFResult(STAFResult.Ok,
                "XenManagerCore Service Help" + fLineSep
                        + fLineSep + "ADD VM-NAME <vm-name>" +
                        " SNAP-NAME <snap-name> SNAP-UUID <snap-uuid>"
                        + fLineSep + "DELETE  VM-NAME <vm-name> VM-UUID <vm-uuid>"
                        + fLineSep + "LIST <vm> | <snap-shot>");
    }

    public STAFResult handleVersion()
    {
        return new STAFResult(STAFResult.Ok, kVersion);
    }

    public STAFResult term(){
        try{
            // Un-register the service handle

            fHandle.unRegister();
        }catch (STAFException ex){
            return new STAFResult(STAFResult.STAFRegistrationError, ex.toString());
        }
        return new STAFResult(STAFResult.Ok);
    }

    // this method will resolve any STAF variables that
    // are contained within the Option Value
    public STAFResult resolveVar(String machine, String optionValue,
                                  int handle){
        String value = "";
        STAFResult resolvedResult = null;

        if (optionValue.indexOf("{") != -1)
        {
            resolvedResult =
                    fHandle.submit2(machine, "var", "handle " + handle +
                            " resolve " + optionValue);

            if (resolvedResult.rc != 0)
            {
                return resolvedResult;
            }

            value = resolvedResult.result;
        }
        else
        {
            value = optionValue;
        }

        return new STAFResult(STAFResult.Ok, value);
    }
}
