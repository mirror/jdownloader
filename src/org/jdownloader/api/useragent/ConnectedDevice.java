package org.jdownloader.api.useragent;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.jdownloader.api.myjdownloader.MyJDownloaderDirectHttpConnection;
import org.jdownloader.api.myjdownloader.MyJDownloaderHttpConnection;

public class ConnectedDevice {

    private RemoteAPIRequest latestRequest;
    private String           token;

    public String getConnectToken() {
        return token;
    }

    public void setConnectToken(String token) {
        this.token = token;
    }

    private long          lastPing;
    private UserAgentInfo info;

    public UserAgentInfo getInfo() {
        return info;
    }

    private String  id;
    private boolean jdanywhere = false;

    public String getId() {
        return id;
    }

    public long getLastPing() {
        return lastPing;
    }

    public ConnectedDevice(String nuaID) {
        this.id = nuaID;
    }

    public long getTimeout() {
        return 5 * 60 * 1000l;
    }

    public void setLatestRequest(RemoteAPIRequest request) {
        latestRequest = request;
        this.lastPing = System.currentTimeMillis();
        if (latestRequest.getRequestedPath().contains("anywhere")) {

            // workaround. jdanywhere does not use a user agent
            this.jdanywhere = true;
        }
    }

    public String getDeviceName() {
        if (jdanywhere) {
            return "IPhone/IPad";
        }
        if (StringUtils.equals(getUserAgentString(), "JDownloader Android App")) {
            return "Android";
        }
        if (info != null) {
            return info.getName() + "@" + info.getOs();
        } else {
            return getUserAgentString();
        }

    }

    public String getUserAgentString() {
        return latestRequest.getRequestHeaders().getValue("User-Agent");
    }

    public void setInfo(UserAgentInfo info) {
        this.info = info;
    }

    public String getFrontendName() {
        if (jdanywhere) {
            return "JDAnywhere";
        }
        String origin = latestRequest.getRequestHeaders().getValue("Origin");

        if (StringUtils.equals(origin, "http://my.jdownloader.org") || StringUtils.equals(origin, "https://my.jdownloader.org")) {
            return "Webinterface http://my.jdownloader.org";
        } else if (origin != null && origin.startsWith("chrome-extension://")) {
            return "Chrome Extension";
        }
        if (StringUtils.equals(getUserAgentString(), "JDownloader Android App")) {
            return "My.JDownloader App";
        }
        if (origin != null) {
            return origin;
        }
        return "Unknown";

    }

    public static boolean isThisMyIpAddress(InetAddress addr) {
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            return true;
        }

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    public String getConnectionString() {
        final List<MyJDownloaderHttpConnection> list = MyJDownloaderHttpConnection.getConnectionsByToken(getConnectToken());
        final int num;
        if (list == null) {
            num = 0;
        } else {
            num = list.size();
        }
        HttpConnection con = latestRequest.getHttpRequest().getConnection();
        if (con instanceof MyJDownloaderDirectHttpConnection) {
            if (num == 0) {
                return "0 Direct Connections via my.jdownloader";
            } else {
                final String ip = latestRequest.getHttpRequest().getRemoteAddress().get(0);
                if (num > 1) {
                    return Integer.toString(num).concat(" Direct Connections from " + ip);
                } else {
                    return Integer.toString(num).concat(" Direct Connection from " + ip);
                }
            }
        } else {
            if (num > 1 || num == 0) {
                return Integer.toString(num).concat(" Remote Connections via my.jdownloader");
            } else {
                return Integer.toString(num).concat(" Remote Connection via my.jdownloader");
            }

        }

    }

    public RemoteAPIRequest getLatestRequest() {
        return latestRequest;
    }
}
