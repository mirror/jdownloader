//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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

        if (proxy.getUser() != null || proxy.getPass() != null) {
            connection.setRequestProperty("Proxy-Authorization", "Basic " + Encoding.Base64Encode(proxy.getUser() + ":" + proxy.getPass()));
        }
        return connection;
    }
}
