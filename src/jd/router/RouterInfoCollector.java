package jd.router;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import jd.gui.skins.simple.SimpleGUI;

import jd.gui.skins.simple.components.CountdownConfirmDialog;

import jd.JDInit;

import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.http.Browser;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RouterInfoCollector {
    public final static String PROPERTY_SHOW_ROUTERINFO_DIALOG = "PROPERTY_SHOW_ROUTERINFO_DIALOG";
    protected RouterCollectorData rcd = new RouterCollectorData();

    public RouterInfoCollector() {
        rcd.setReconnectMethode(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null));
        String lh = JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl");
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, lh).equals(lh) || rcd.getReconnectMethode() == null) {
            rcd.setLiveHeaderReconnect(false);
        }
        String IP = null;
        try {
            IP = getRouterIP();
        } catch (Exception e) {
            // TODO: handle exception
        }
        if (IP == null) { return; }
        try {
            UPnPInfo up = new UPnPInfo(IP);
            if (up.met != null) {
                String Reconnectmethode = "[[[HSRC]]]\r\n" + "[[[STEP]]]\r\n" + "[[[REQUEST]]]\r\n" + "POST /upnp/control/WANIPConn1 HTTP/1.1\r\n" + "Host: %%%routerip%%%:49000\r\n" + "Content-Type: text/xml; charset=\"utf-8\"\r\n" + "SoapAction:urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination\r\n" +

                "<?xml version='1.0' encoding='utf-8'?> <s:Envelope s:encodingStyle='http://schemas.xmlsoap.org/soap/encoding/' xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'> <s:Body> <u:ForceTermination xmlns:u='urn:schemas-upnp-org:service:WANIPConnection:1' /> </s:Body> </s:Envelope>\r\n" + "[[[/REQUEST]]]\r\n" + "[[[/STEP]]]\r\n" + "[[[/HSRC]]]";

                if (up.met.trim().equalsIgnoreCase(Reconnectmethode))
                    rcd.setHaveFritzUpnp(true);
                else
                    rcd.setUPnPReconnectMeth(up.met);
            }
            if (!rcd.isHaveFritzUpnp()) {
                rcd.setUPnPSCPDs(up.SCPDs);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

        Browser br = new Browser();
        try {
            rcd.setRouterSite(br.getPage("http://" + IP));
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
        if (rcd.isLiveHeaderReconnect()) {
            String rn = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, null);
            if (rn != null) {
                rcd.setRouterMethodeNames(new String[] { rn });
            } else if (rcd.getReconnectMethode() != null && !rcd.getReconnectMethode().matches("\\s")) {
                ArrayList<String> rmn = new ArrayList<String>();
                for (String[] script : new HTTPLiveHeader().getLHScripts()) {
                    if (script[2].trim().equals(rcd.getReconnectMethode()) && !rmn.contains(script[1])) rmn.add(script[1]);
                }
                rcd.setRouterMethodeNames(rmn.toArray(new String[rmn.size()]));
            }
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

    public static String getXMLString() {
        RouterInfoCollector ric = new RouterInfoCollector();
        if (!ric.rcd.isLiveHeaderReconnect() || ric.rcd.getRouterMAC() == null || ric.rcd.getRouterMAC().length() < 8) return null;
        try {
            ric.rcd.setRouterMAC(ric.rcd.getRouterMAC().substring(0, 8));
        } catch (Exception e) {
            // TODO: handle exception
        }

        return ric.toXMLString();
    }

    public static void showDialog() {
         if (true) {
//        if (JDUtilities.getConfiguration().getBooleanProperty(PROPERTY_SHOW_ROUTERINFO_DIALOG, true)) {
            new Thread(new Runnable() {

                public void run() {
                    String xml = getXMLString();
                    if (xml != null) {
                        CountdownConfirmDialog ccd = new CountdownConfirmDialog(JDUtilities.getParentFrame(SimpleGUI.CURRENTGUI.getFrame()), JDLocale.L("routerinfocollector.dialog.title", "Helfen sie die Routererkennung zu verbessern"), 30, true, CountdownConfirmDialog.STYLE_YES | CountdownConfirmDialog.STYLE_NO | CountdownConfirmDialog.STYLE_DETAILLABLE, JDLocale.L("routerinfocollector.dialog.msg", "<b>Um die automatische Routererkennung zu verbessern sammeln wir Routerinformationen!</b><br>Wenn sie damit einverstanden sind die Informationen aus den Details an unseren Server zu übermitteln bestätigen sie mit ja!"), xml);

                        if (!ccd.window_Closed) JDUtilities.getConfiguration().setProperty(PROPERTY_SHOW_ROUTERINFO_DIALOG, false);
                        if (ccd.result) sendToServer(xml);
                    }

                }
            }).start();
        }
    }

    @SuppressWarnings("deprecation")
    public static void sendToServer(String xmlString) {
        if (xmlString == null) return;
        xmlString = xmlString.trim();
        String md5 = JDUtilities.getMD5("jdsecred" + JDUtilities.getMD5(xmlString));
        HashMap<String, String> post = new HashMap<String, String>();
        post.put("routerinfo", URLEncoder.encode(xmlString));
        post.put("key", md5);
        Browser br = new Browser();
        try {
            String out = br.postPage("http://jdownloader.org/router/import.php", post);
            // System.out.println(br);
            if(out==null || !out.equals("No htmlCode read"))
            JDUtilities.getLogger().severe(out);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new JDInit().loadConfiguration();
        System.out.println(getXMLString());

        // System.out.println(rc.compare(rc));
        System.exit(0);
    }
}
