import com.ibm.staf.STAFException;
import com.ibm.staf.STAFHandle;
import com.ibm.staf.STAFResult;
import com.ibm.staf.STAFUtil;
import com.ibm.staf.service.STAFCommandParseResult;
import com.ibm.staf.service.STAFCommandParser;
import com.ibm.staf.service.STAFServiceInterfaceLevel30;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * Created by user on 2016-12-06.
 */
public class DeviceService implements STAFServiceInterfaceLevel30{
    private final String kVersion = "1.1.0";
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

    public DeviceService() {}

    public STAFResult init(InitInfo info)
    {
        int rc = STAFResult.Ok;

        try
        {
            fServiceName = info.name;
            fHandle = new STAFHandle("STAF/SERVICE/" + info.name);
        }
        catch (STAFException e)
        {
            return  new STAFResult(STAFResult.STAFRegistrationError, e.toString());
        }

        // ADD parser
        fAddParser = new STAFCommandParser();

        fAddParser.addOption("ADD", 1,
                STAFCommandParser.VALUENOTALLOWED);

        fAddParser.addOption("PRINTER", 1,
                STAFCommandParser.VALUEREQUIRED);

        fAddParser.addOption("MODEL", 1,
                STAFCommandParser.VALUEREQUIRED);

        fAddParser.addOption("SN", 1,
                STAFCommandParser.VALUEREQUIRED);

        fAddParser.addOption("MODEM", 1,
                STAFCommandParser.VALUEREQUIRED);

        // this means you can have PRINTER or MODEM, but not both
        fAddParser.addOptionGroup("PRINTER MODEM", 0, 1);

        // if you specify ADD, MODEL is required
        fAddParser.addOptionNeed("ADD", "MODEL");

        // if you specify ADD, SN is required
        fAddParser.addOptionNeed("ADD", "SN");

        // if you specify PRINTER or MODEM, ADD is required
        fAddParser.addOptionNeed("PRINTER MODEM", "ADD");

        // if you specify ADD, PRINTER or MODEM is required
        fAddParser.addOptionNeed("ADD", "PRINTER MODEM");

        // LIST parser
        fListParser = new STAFCommandParser();

        fListParser.addOption("LIST", 1,
                STAFCommandParser.VALUENOTALLOWED);

        fListParser.addOption("PRINTERS", 1,
                STAFCommandParser.VALUENOTALLOWED);

        fListParser.addOption("MODEMS", 1,
                STAFCommandParser.VALUENOTALLOWED);

        // QUERY parser
        fQueryParser = new STAFCommandParser();

        fQueryParser.addOption("QUERY", 1,
                STAFCommandParser.VALUENOTALLOWED);

        fQueryParser.addOption("PRINTER", 1,
                STAFCommandParser.VALUEREQUIRED);

        fQueryParser.addOption("MODEM", 1,
                STAFCommandParser.VALUEREQUIRED);

        // this means you can have PRINTER or MODEM, but not both
        fQueryParser.addOptionGroup("PRINTER MODEM", 0, 1);

        // if you specify PRINTER or MODEM, QUERY is required
        fQueryParser.addOptionNeed("PRINTER MODEM", "QUERY");

        // if you specify QUERY, PRINTER or MODEM is required
        fQueryParser.addOptionNeed("QUERY", "PRINTER MODEM");

        // DELETE parser
        fDeleteParser = new STAFCommandParser();

        fDeleteParser.addOption("DELETE", 1,
                STAFCommandParser.VALUENOTALLOWED);

        fDeleteParser.addOption("PRINTER", 1,
                STAFCommandParser.VALUEREQUIRED);

        fDeleteParser.addOption("MODEM", 1,
                STAFCommandParser.VALUEREQUIRED);

        fDeleteParser.addOption("CONFIRM", 1,
                STAFCommandParser.VALUENOTALLOWED);

        // this means you must have PRINTER or MODEM, but not both
        fDeleteParser.addOptionGroup("PRINTER MODEM", 0, 1);

        // if you specify PRINTER or MODEM, DELETE is required
        fDeleteParser.addOptionNeed("PRINTER MODEM", "DELETE");

        // if you specify DELETE, PRINTER or MODEM is required
        fDeleteParser.addOptionNeed("DELETE", "PRINTER MODEM");

        // if you specify DELETE, CONFIRM is required
        fDeleteParser.addOptionNeed("DELETE", "CONFIRM");

        fLineSep = fHandle.submit2("local", "var",
                "resolve {STAF/Config/Sep/Line}").result;

        // Register Help Data

        registerHelpData(
                kDeviceInvalidSerialNumber,
                "Invalid serial number",
                "A non-numeric value was specified for serial number");

        return new STAFResult(STAFResult.Ok);
    }

    public STAFResult acceptRequest(RequestInfo info)
    {
        String lowerRequest = info.request.toLowerCase();
        StringTokenizer requestTokenizer = new StringTokenizer(lowerRequest);
        String request = requestTokenizer.nextToken();

        // call the appropriate method to handle the command
        if (request.equals("list"))
        {
            return handleList(info);
        }
        else if (request.equals("query"))
        {
            return handleQuery(info);
        }
        else if (request.equals("add"))
        {
            return handleAdd(info);
        }
        else if (request.equals("delete"))
        {
            return handleDelete(info);
        }
        else if (request.equals("help"))
        {
            return handleHelp();
        }
        else if (request.equals("version"))
        {
            return handleVersion();
        }
        else
        {
            return new STAFResult(STAFResult.InvalidRequestString,
                    "Unknown DeviceService Request: " +
                            lowerRequest);
        }
    }

    private STAFResult handleHelp()
    {
        return new STAFResult(STAFResult.Ok,
                "DeviceService Service Help" + fLineSep
                        + fLineSep + "ADD     (PRINTER <printerName> | MODEM <modemName>)" +
                        " MODEL <model> SN <serial#>"
                        + fLineSep + "DELETE  PRINTER <printerName> | MODEM <modemName> " +
                        "CONFIRM"
                        + fLineSep + "LIST    [PRINTERS] [MODEMS]"
                        + fLineSep + "QUERY   PRINTER <printerName> | MODEM <modemName>"
                        + fLineSep + "VERSION" + fLineSep + "HELP");
    }

    private STAFResult handleVersion()
    {
        return new STAFResult(STAFResult.Ok, kVersion);
    }

    private STAFResult handleAdd(RequestInfo info)
    {
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 3)
        {
            return new STAFResult(STAFResult.AccessDenied,
                    "Trust level 3 required for ADD request. Requesting " +
                            "machine's trust level: " +  info.trustLevel);
        }

        STAFResult result = new STAFResult();
        STAFResult resolveResult = new STAFResult();
        String resultString = "";
        STAFCommandParseResult parsedRequest = fAddParser.parse(info.request);
        String printerValue = "";
        String modemValue = "";
        String modelValue = "";
        String snValue = "";

        if (parsedRequest.rc != STAFResult.Ok)
        {
            return new STAFResult(STAFResult.InvalidRequestString,
                    parsedRequest.errorBuffer);
        }
        resolveResult = resolveVar(info.machine,
                parsedRequest.optionValue("printer"), info.handle);

        if (resolveResult.rc != STAFResult.Ok)
        {
            return resolveResult;
        }

        printerValue = resolveResult.result;
        resolveResult = resolveVar(info.machine,
                parsedRequest.optionValue("modem"), info.handle);

        if (resolveResult.rc != STAFResult.Ok)
        {
            return resolveResult;
        }

        modemValue = resolveResult.result;

        resolveResult = resolveVar(info.machine,
                parsedRequest.optionValue("model"), info.handle);

        if (resolveResult.rc != STAFResult.Ok)
        {
            return resolveResult;
        }

        modelValue = resolveResult.result;

        resolveResult = resolveVar(info.machine,
                parsedRequest.optionValue("sn"), info.handle);

        if (resolveResult.rc != STAFResult.Ok)
        {
            return resolveResult;
        }

        snValue = resolveResult.result;

        // Verify that the serial number is numeric
        try
        {
            new Integer(snValue);
        }
        catch (NumberFormatException e)
        {
            // Note that instead of creating a new error code specific for
            // this service, could use STAFResult.InvalidValue instead.
            return new STAFResult(kDeviceInvalidSerialNumber, snValue);
        }

        if (!printerValue.equals(""))
        {
            synchronized (fPrinterMap)
            {
                fPrinterMap.put(printerValue,
                        new DeviceData(modelValue, snValue));
            }
        }
        else if (!modemValue.equals(""))
        {
            synchronized (fModemMap)
            {
                fModemMap.put(modemValue,
                        new DeviceData(modelValue, snValue));
            }
        }

        return new STAFResult(STAFResult.Ok, resultString);
    }

    private STAFResult handleList(RequestInfo info)
    {
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 2)
        {
            return new STAFResult(STAFResult.AccessDenied,
                    "Trust level 2 required for LIST request. Requesting " +
                            "machine's trust level: " +  info.trustLevel);
        }

        STAFResult result = new STAFResult(STAFResult.Ok, "");
        String resultString = "";
        STAFCommandParseResult parsedRequest = fListParser.parse(info.request);

        int printersOption;
        int modemsOption;

        if (parsedRequest.rc != STAFResult.Ok)
        {
            return new STAFResult(STAFResult.InvalidRequestString,
                    parsedRequest.errorBuffer);
        }

        printersOption = parsedRequest.optionTimes("printers");

        modemsOption = parsedRequest.optionTimes("modems");

        boolean defaultList = false;

        if (printersOption == 0 && modemsOption == 0)
        {
            defaultList = true;
        }

        if (defaultList || printersOption > 0)
        {
            Iterator iter = fPrinterMap.keySet().iterator();

            while (iter.hasNext())
            {
                String key = (String)iter.next();

                DeviceData data = (DeviceData)fPrinterMap.get(key);

                resultString += key + ";" + data.model + ";" + data.sn + "\n";
            }
        }

        if (defaultList || modemsOption > 0)
        {
            Iterator iter = fModemMap.keySet().iterator();

            while (iter.hasNext())
            {
                String key = (String)iter.next();

                DeviceData data = (DeviceData)fModemMap.get(key);

                resultString += key + ";" + data.model + ";" + data.sn + "\n";
            }
        }

        return new STAFResult(STAFResult.Ok, resultString);
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

    private STAFResult handleDelete(RequestInfo info)
    {
        // Check whether Trust level is sufficient for this command.
        if (info.trustLevel < 4)
        {
            return new STAFResult(STAFResult.AccessDenied,
                    "Trust level 4 required for DELETE request. Requesting " +
                            "machine's trust level: " +  info.trustLevel);
        }

        STAFResult result = new STAFResult(STAFResult.Ok, "");
        String resultString = "";
        STAFResult resolveResult = new STAFResult();
        STAFCommandParseResult parsedRequest = fDeleteParser.parse(info.request);
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
            synchronized (fPrinterMap)
            {
                if (fPrinterMap.containsKey(printerValue))
                {
                    fPrinterMap.remove(printerValue);
                }
                else
                {
                    return new STAFResult(STAFResult.DoesNotExist, printerValue);
                }
            }
        }
        else if (!modemValue.equals(""))
        {
            synchronized (fModemMap)
            {
                if (fModemMap.containsKey(modemValue))
                {
                    fModemMap.remove(modemValue);
                }
                else
                {
                    return new STAFResult(STAFResult.DoesNotExist, modemValue);
                }
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
