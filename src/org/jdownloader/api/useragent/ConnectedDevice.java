package org.jdownloader.api.useragent;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpserver.HttpConnection;
import org.jdownloader.api.myjdownloader.MyJDownloaderDirectHttpConnection;

public class ConnectedDevice {

    private RemoteAPIRequest latestRequest;
    private long             lastPing;
    private UserAgentInfo    info;

    public UserAgentInfo getInfo() {
        return info;
    }

    private String id;

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
    }

    public String getDeviceName() {
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
        HttpConnection con = latestRequest.getHttpRequest().getConnection();
        if (con instanceof MyJDownloaderDirectHttpConnection) {
            String ip = latestRequest.getHttpRequest().getRemoteAddress().get(0);
            // try {
            // if (isThisMyIpAddress(InetAddress.getByName(ip))) {
            // return "Direct Local Connection";
            // } else {
            return "Direct Connection from " + ip;
            // }
            // } catch (UnknownHostException e) {
            // return "Direct Connection";
            // }
        } else {
            return "Remote Connection via my.jdownloader";
        }

    }
}
