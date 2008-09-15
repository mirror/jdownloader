package jd.router;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import jd.controlling.interaction.HTTPLiveHeader;

import jd.JDInit;

import jd.http.Browser;

import jd.utils.JDLocale;

import jd.config.Configuration;
import jd.utils.JDUtilities;

public class RouterInfoCollector implements Serializable {

    private static final long serialVersionUID = 1L;
    private String reconnectMethode = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);
    protected boolean isLiveHeaderReconnect = true;
    protected String routerIp = null;
    protected String routerSite = null;
    protected String routerErrorPage = null;
    protected String routerMAC = null;
    protected String[] routerMethodeNames = null;

    public RouterInfoCollector() {
        String lh = JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl");
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, lh).equals(lh) || reconnectMethode == null) {
            isLiveHeaderReconnect = false;
            return;
        }
        routerIp = getRouterIP();
        Browser br = new Browser();
        try {
            routerSite = br.getPage("http://" + routerIp);
        } catch (IOException e) {
        }
        try {
            routerErrorPage = br.getPage("http://" + routerIp + "/error404");
        } catch (IOException e) {
        }
        try {
            routerMAC = new GetMacAdress().getMacAddress(routerIp);
        } catch (SocketException e) {
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        String rn = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, null);
        if (rn != null) {
            routerMethodeNames = new String[] { rn };
        } else {
            ArrayList<String> rmn = new ArrayList<String>();
            for (String[] script : new HTTPLiveHeader().getLHScripts()) {
                if (script[2].trim().equals(reconnectMethode) && !rmn.contains(script[1])) rmn.add(script[1]);
            }
            routerMethodeNames = rmn.toArray(new String[rmn.size()]);
        }
    }

    @Override
    public String toString() {
        String sep = System.getProperty("line.separator");
        String ret = "";
        if (routerMethodeNames.length == 1) {
            ret = "RouterMethodeName:" + routerMethodeNames[0] + sep;
        } else {
            for (int i = 0; i < routerMethodeNames.length; i++) {
                ret += "RouterMethodeName[" + i + "]:" + routerMethodeNames[i] + sep;
            }
        }
        ret += "ReconnectMethode:" + reconnectMethode + sep + "isLiveHeaderReconnect:" + isLiveHeaderReconnect + sep + "RouterIp:" + routerIp + sep + "RouterMAC:" + routerMAC + sep + "RouterSite:" + routerSite + sep + "RouterErrorPage:" + routerErrorPage;
        return ret;
    }

    public static String getRouterIP() {
        String routerIp = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        if (routerIp == null || routerIp.matches("\\s*")) routerIp = new GetRouterInfo(null).getAdress();
        return routerIp;
    }

    public static void main(String[] args) {
        new JDInit().loadConfiguration();
        System.out.println(new RouterInfoCollector().toString());
    }
}
