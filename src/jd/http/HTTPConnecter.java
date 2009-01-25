package jd.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import jd.config.Configuration;
import jd.utils.JDUtilities;

public class HTTPConnecter {

    public static URLConnection openConnection(URL url) throws IOException {

        URLConnection connection = url.openConnection();
        initConnection(connection);
        return connection;
    }

    private static void initConnection(URLConnection connection) {
        connection.setRequestProperty("Connection", "close");

        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.4) Gecko/2008111317 Ubuntu/8.04 (hardy) Firefox/3.0.4");
        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_PROXY, false)) {
            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS, "");

            connection.setRequestProperty("Proxy-Authorization", "Basic " + Encoding.Base64Encode(user + ":" + pass));

        }

        if (JDUtilities.getSubConfig("DOWNLOAD").getBooleanProperty(Configuration.USE_SOCKS, false)) {

            String user = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_USER_SOCKS, "");
            String pass = JDUtilities.getSubConfig("DOWNLOAD").getStringProperty(Configuration.PROXY_PASS_SOCKS, "");

            connection.setRequestProperty("Proxy-Authorization", "Basic " + Encoding.Base64Encode(user + ":" + pass));

        }

    }

    public static URLConnection openConnection(URL url, JDProxy proxy) throws IOException {
    URLConnection connection = url.openConnection(proxy);
    initConnection(connection);

    if (proxy.getUser()!=null||proxy.getPass()!=null) {

         
            connection.setRequestProperty("Proxy-Authorization", "Basic " + Encoding.Base64Encode(proxy.getUser() + ":" + proxy.getPass()));

        }
        return connection;
    }
}
