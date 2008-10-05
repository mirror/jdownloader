package jd.router;

import java.io.Serializable;
import java.util.HashMap;

import jd.config.Configuration;
import jd.utils.JDUtilities;

public class RouterCollectorData implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 5026622395222229574L;

    private String reconnectMethode = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);

    public String getReconnectMethode() {
        return reconnectMethode;
    }
    public void setReconnectMethode(String reconnectMethode) {
        this.reconnectMethode = reconnectMethode;
    }
    public boolean isLiveHeaderReconnect() {
        return isLiveHeaderReconnect;
    }
    public void setLiveHeaderReconnect(boolean isLiveHeaderReconnect) {
        this.isLiveHeaderReconnect = isLiveHeaderReconnect;
    }
    public boolean isHaveSip() {
        return haveSip;
    }
    public void setHaveSip(boolean haveSip) {
        this.haveSip = haveSip;
    }
    public String getRouterIp() {
        return routerIp;
    }
    public void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }
    public String getRouterSite() {
        return routerSite;
    }
    public void setRouterSite(String routerSite) {
        this.routerSite = routerSite;
    }
    public String getRouterErrorPage() {
        return routerErrorPage;
    }
    public void setRouterErrorPage(String routerErrorPage) {
        this.routerErrorPage = routerErrorPage;
    }
    public String getRouterMAC() {
        return routerMAC;
    }
    public void setRouterMAC(String routerMAC) {
        this.routerMAC = routerMAC;
    }
    public String[] getRouterMethodeNames() {
        return routerMethodeNames;
    }
    public void setRouterMethodeNames(String[] routerMethodeNames) {
        this.routerMethodeNames = routerMethodeNames;
    }
    public boolean isHaveFritzUpnp() {
        return haveFritzUpnp;
    }
    public void setHaveFritzUpnp(boolean haveFritzUpnp) {
        this.haveFritzUpnp = haveFritzUpnp;
    }
    public String getUPnPReconnectMeth() {
        return uPnPReconnectMeth;
    }
    public void setUPnPReconnectMeth(String pnPReconnectMeth) {
        uPnPReconnectMeth = pnPReconnectMeth;
    }
    public HashMap<String, String> getUPnPSCPDs() {
        return uPnPSCPDs;
    }
    public void setUPnPSCPDs(HashMap<String, String> pnPSCPDs) {
        uPnPSCPDs = pnPSCPDs;
    }
    private boolean isLiveHeaderReconnect = true, haveSip = false;
    private String routerIp = null;
    private String routerSite = null;
    private String routerErrorPage = null;
    private String routerMAC = null;
    private String[] routerMethodeNames = null;
    private boolean haveFritzUpnp = false;
    private String uPnPReconnectMeth = null;
    private HashMap<String, String> uPnPSCPDs = null;
    /**
     * Gibt einen Prozentwert zurück zu welcher Wahrscheinlichkeit es sich um
     * diesen router handelt
     * 
     * @param routerInfo
     * @return
     */
    public int compare(RouterCollectorData routerCollectorData) {
        int ret = 0;
        if (routerMAC.substring(0, 8).equalsIgnoreCase(routerCollectorData.routerMAC.substring(0, 8))) ret += 40;
        if (routerSite.equalsIgnoreCase(routerCollectorData.routerSite)) ret += 25;
        if (routerErrorPage.equalsIgnoreCase(routerCollectorData.routerErrorPage)) ret += 25;
        if (haveSip == routerCollectorData.haveSip) ret += 10;
        return ret;
    }
}
