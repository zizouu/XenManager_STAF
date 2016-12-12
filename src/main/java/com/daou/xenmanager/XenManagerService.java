package com.daou.xenmanager;

import com.daou.xenmanager.core.XenCore;
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
public class XenManagerService implements STAFServiceInterfaceLevel30{
    private final String kVersion = "1.0.0";
    private static final int kDeviceInvalidSerialNumber = 4001;
    private String fServiceName;
    private STAFHandle fHandle;
    private STAFCommandParser fListParser;
    private STAFCommandParser fQueryParser;
    private STAFCommandParser fAddParser;
    private STAFCommandParser fDeleteParser;
    private String fLineSep;
    private TreeMap fPrinterMap = new TreeMap();
    private TreeMap fModemMap = new TreeMap();

    private String xenHost = "vm2.terracetech.co.kr";
    private String xenUser = "root";
    private String xenPassword = "qwopZX!@";

    public XenManagerService() {}

    public STAFResult init(InitInfo info){
        int rc = STAFResult.Ok;

        try{
            fServiceName = info.name;
            fHandle = new STAFHandle("STAF/SERVICE/" + info.name);
        }catch (STAFException e){
            return  new STAFResult(STAFResult.STAFRegistrationError, e.toString());
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
        // Register Help Data
        registerHelpData(kDeviceInvalidSerialNumber,"Invalid serial number","A non-numeric value was specified for serial number");

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
            result = new STAFResult(STAFResult.InvalidRequestString,"Unknown XenManagerService Request: " + lowerRequest);
        }

        return result;
    }

    private STAFResult handleList(RequestInfo info){
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 2){
            return new STAFResult(STAFResult.AccessDenied,"Trust level 2 required for LIST request. Requesting machine's trust level: "+info.trustLevel);
        }
        //About xen
        XenCore xenManager = new XenCore();
        List<VM.Record> list;
        //About STAF
        STAFResult resolveResult, result;
        STAFCommandParseResult parsedRequest = fListParser.parse(info.request);
        String resultString = "";
        String listValue = "";

        if (parsedRequest.rc != STAFResult.Ok){
            return new STAFResult(STAFResult.InvalidRequestString, parsedRequest.errorBuffer);
        }

        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("list"), info.handle);

        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }

        listValue = resolveResult.result;

        try{
            Map<String, String> xenResultMap;

            xenManager.connect(new ServerInfo(xenHost, xenUser, xenPassword));

            if("vm".equals(listValue)){
                //request vm
                xenResultMap = xenManager.getVMListByType(XenCore.GET_TYPE_VM);
            }else if("snap-shot".equals(listValue)){
                //request snap-shot
                xenResultMap = xenManager.getVMListByType(XenCore.GET_TYPE_SNAP);
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

        return new STAFResult(STAFResult.Ok, resultString);
    }

    private STAFResult handleAdd(RequestInfo info){
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 3){
            return new STAFResult(STAFResult.AccessDenied,"Trust level 3 required for ADD request. Requesting machine's trust level: " +  info.trustLevel);
        }

        STAFResult resolveResult;
        String resultString, vmName, snapName, snapUuid;
        STAFCommandParseResult parsedRequest = fAddParser.parse(info.request);

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
        XenCore xenManager = new XenCore();
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

    private STAFResult handleDelete(RequestInfo info){
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 4){
            return new STAFResult(STAFResult.AccessDenied,
                    "Trust level 4 required for DELETE request. Requesting " +
                            "machine's trust level: " +  info.trustLevel);
        }

        String resultString = "";
        String vmName, vmUuid;
        STAFResult resolveResult;
        STAFCommandParseResult parsedRequest = fDeleteParser.parse(info.request);

        if (parsedRequest.rc != STAFResult.Ok){
            return new STAFResult(STAFResult.InvalidRequestString, parsedRequest.errorBuffer);
        }
        //check validation vm-name
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("vm-name"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        vmName = resolveResult.result;
        //check validation vm-uuid
        resolveResult = resolveVar(info.machine, parsedRequest.optionValue("vm-uuid"), info.handle);
        if (resolveResult.rc != STAFResult.Ok){
            return resolveResult;
        }
        vmUuid = resolveResult.result;

        XenCore xenManager = new XenCore();
        try{
            xenManager.connect(new ServerInfo(xenHost, xenUser, xenPassword));
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

//************************** SHOULD DEVELOP *******************************//

    private STAFResult handleHelp()
    {
        return new STAFResult(STAFResult.Ok,
                "XenManagerService Service Help" + fLineSep
                        + fLineSep + "ADD     (PRINTER <printerName> | MODEM <modemName>)" +
                        " MODEL <model> SN <serial#>"
                        + fLineSep + "DELETE  PRINTER <printerName> | MODEM <modemName> " +
                        "CONFIRM"
                        + fLineSep + "LIST    [VM] [MODEMS]"
                        + fLineSep + "QUERY   PRINTER <printerName> | MODEM <modemName>"
                        + fLineSep + "VERSION" + fLineSep + "HELP");
    }

    private STAFResult handleVersion()
    {
        return new STAFResult(STAFResult.Ok, kVersion);
    }

    private STAFResult handleQuery(RequestInfo info)
    {
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 2)
        {
            return new STAFResult(STAFResult.AccessDenied,
                    "Trust level 2 required for QUERY request. Requesting " +
                            "machine's trust level: " +  info.trustLevel);
        }

        STAFResult result = new STAFResult(STAFResult.Ok, "");
        String resultString = "";
        STAFResult resolveResult = new STAFResult();
        STAFCommandParseResult parsedRequest = fQueryParser.parse(info.request);
        String printerValue;
        String modemValue;

        if (parsedRequest.rc != STAFResult.Ok)
        {
            return new STAFResult(STAFResult.InvalidRequestString,
                    parsedRequest.errorBuffer);
        }


        resolveResult = resolveVar(info.machine,
                parsedRequest.optionValue("printer"),
                info.handle);

        if (resolveResult.rc != STAFResult.Ok)
        {
            return resolveResult;
        }

        printerValue = resolveResult.result;

        resolveResult = resolveVar(info.machine,
                parsedRequest.optionValue("modem"),
                info.handle);

        if (resolveResult.rc != STAFResult.Ok)
        {
            return resolveResult;
        }

        modemValue = resolveResult.result;

        if (!printerValue.equals(""))
        {
            if (fPrinterMap.containsKey(printerValue))
            {
                String printer = printerValue;
                DeviceData data = (DeviceData)fPrinterMap.get(printer);
                resultString = "Printer : " + printer + "\n";
                resultString += "Model   : " + data.model + "\n";
                resultString += "Serial# : " + data.sn + "\n";
            }
            else
            {
                return new STAFResult(STAFResult.DoesNotExist, printerValue);
            }
        }
        else if (!modemValue.equals(""))
        {
            if (fModemMap.containsKey(modemValue))
            {
                String modem = modemValue;
                DeviceData data = (DeviceData)fModemMap.get(modem);
                resultString = "Modem   : " + modem + "\n";
                resultString += "Model   : " + data.model + "\n";
                resultString += "Serial# : " + data.sn + "\n";
            }
            else
            {
                return new STAFResult(STAFResult.DoesNotExist, printerValue);
            }
        }

        return new STAFResult(STAFResult.Ok, resultString);
    }

    public STAFResult term()
    {
        try
        {
            // Un-register Help Data

            unregisterHelpData(kDeviceInvalidSerialNumber);

            // Un-register the service handle

            fHandle.unRegister();
        }
        catch (STAFException ex)
        {
            return new STAFResult(STAFResult.STAFRegistrationError, ex.toString());
        }

        return new STAFResult(STAFResult.Ok);
    }

    // this method will resolve any STAF variables that
    // are contained within the Option Value
    private STAFResult resolveVar(String machine, String optionValue,
                                  int handle)
    {
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


    // Register error codes for the STAX Service with the HELP service

    private void registerHelpData(int errorNumber, String info,
                                  String description)
    {
        STAFResult res = fHandle.submit2("local", "HELP",
                "REGISTER SERVICE " + fServiceName +
                        " ERROR " + errorNumber +
                        " INFO " + STAFUtil.wrapData(info) +
                        " DESCRIPTION " + STAFUtil.wrapData(description));
    }

    // Un-register error codes for the STAX Service with the HELP service

    private void unregisterHelpData(int errorNumber)
    {
        STAFResult res = fHandle.submit2("local", "HELP",
                "UNREGISTER SERVICE " + fServiceName +
                        " ERROR " + errorNumber);
    }

    public class DeviceData
    {
        public String model = "";
        public String sn = "";

        public DeviceData(String model, String sn)
        {
            this.model = model;
            this.sn = sn;
        }
    }
}