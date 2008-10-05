package jd.router;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import jd.JDInit;

import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.http.Browser;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RouterInfoCollector  {

    protected RouterCollectorData rcd = new RouterCollectorData();
    public RouterInfoCollector() {
        rcd.setReconnectMethode(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null));
        String lh = JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl");
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, lh).equals(lh) || rcd.getReconnectMethode() == null) {
            rcd.setLiveHeaderReconnect(false);
            return;
        }

        String IP = getRouterIP();
        UPnPInfo up = new UPnPInfo(IP);
        if(up.met!=null)
        {
            String Reconnectmethode = "[[[HSRC]]]\r\n" + "[[[STEP]]]\r\n" + "[[[REQUEST]]]\r\n" + "POST /upnp/control/WANIPConn1 HTTP/1.1\r\n" + "Host: %%%routerip%%%:49000\r\n" + "Content-Type: text/xml; charset=\"utf-8\"\r\n" + "SoapAction:urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination\r\n" +

            "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:ForceTermination xmlns:u='urn:schemas-upnp-org:service:WANIPConnection:1' /> </s:Body> </s:Envelope>\r\n" + "[[[/REQUEST]]]\r\n" + "[[[/STEP]]]\r\n" + "[[[/HSRC]]]";
       
            if(up.met.trim().equalsIgnoreCase(Reconnectmethode))
                rcd.setHaveFritzUpnp(true);
            else
                rcd.setUPnPReconnectMeth(up.met);
        }
        if(!rcd.isHaveFritzUpnp())
        {
            rcd.setUPnPSCPDs(up.SCPDs);
        }
        Browser br = new Browser();
        try {
            rcd.setRouterSite(br.getPage("http://" + IP)) ;
        } catch (IOException e) {
        }
        try {
            rcd.setRouterErrorPage(br.getPage("http://" + IP + "/error404"));
        } catch (IOException e) {
        }
        try {
            rcd.setRouterMAC(new GetMacAdress().getMacAddress(IP));
        } catch (Exception e) {
            // TODO: handle exception
        }
        String rn = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, null);
        if (rn != null) {
            rcd.setRouterMethodeNames(new String[] { rn });
        } else if (rcd.getReconnectMethode()!=null && !rcd.getReconnectMethode().matches("\\s")){
            ArrayList<String> rmn = new ArrayList<String>();
            for (String[] script : new HTTPLiveHeader().getLHScripts()) {
                if (script[2].trim().equals(rcd.getReconnectMethode()) && !rmn.contains(script[1])) rmn.add(script[1]);
            }
            rcd.setRouterMethodeNames(rmn.toArray(new String[rmn.size()]));
        }
        rcd.setHaveSip(GetRouterInfo.checkport(IP, 5060));

    }

    @Override
    public String toString() {
        return toXMLString();
    }

    public static String getRouterIP() {
        String routerIp = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        if (routerIp == null || routerIp.matches("\\s*")) routerIp = new GetRouterInfo(null).getAdress();
        return routerIp;
    }
    public String toXMLString() {
        try {
            return JDUtilities.objectToXml(this.rcd);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }
    /**
     * Gibt einen Prozentwert zurück zu welcher Wahrscheinlichkeit es sich um
     * diesen router handelt
     * 
     * @param routerInfo
     * @return
     */
    public int compare(RouterInfoCollector routerInfo) {
        return rcd.compare(routerInfo.rcd);
    }
    public static String getXMLString()
    {
        RouterInfoCollector ric = new RouterInfoCollector();
        try {
            ric.rcd.setRouterMAC(ric.rcd.getRouterMAC().substring(0, 8));
        } catch (Exception e) {
            // TODO: handle exception
        }

        return ric.toXMLString();
    }
    public static void main(String[] args) {
        new JDInit().loadConfiguration();
        System.out.println(RouterInfoCollector.getXMLString());
        // System.out.println(rc.compare(rc));
System.exit(0);
    }
}
