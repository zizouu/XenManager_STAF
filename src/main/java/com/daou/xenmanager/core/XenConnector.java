package com.daou.xenmanager.core;

import com.daou.xenmanager.entity.XenServer;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;

import java.net.URL;

/**
 * Created by user on 2016-12-07.
 */
public class XenConnector {
    protected static Connection connection;
    private static String connectionName;

    protected static void connect(XenServer target) throws Exception
    {
        /*
         * Old style: Connection constructor performs login_with_password for you. Deprecated.
         *
         * connection = new Connection(target.Hostname, target.Username, target.Password);
         */

        /*
         * New style: we are responsible for Session login/logout.
         */
        connection = new Connection(new URL("https://" + target.hostName));

        System.out.println(String.format("logging in to '%s' as '%s' with password '%s'...", target.hostName, target.userName,
                target.password));

        Session.loginWithPassword(connection, target.userName, target.password, APIVersion.latest().toString());

        System.out.println(String.format("Session API version is %s", connection.getAPIVersion().toString()));

        connectionName = target.hostName;
    }

    protected static void disconnect() throws Exception
    {
        /*logln("disposing connection for " + connectionName);
        Session.logout(connection);*/
    }
}
