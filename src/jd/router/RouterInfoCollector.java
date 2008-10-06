package jd.router;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
    protected boolean isLiveHeaderReconnect = true, haveSip = false;
    protected String routerSite = null;
    protected String routerErrorPage = null;
    protected String routerMAC = null;
    protected String[] routerMethodeNames = null;
    protected HashMap<String, String> uPnPSCPDs = null;
    protected String reconnectMethode = null;

    protected String IP = null;
    protected String routerHost = null;
    protected String pageHeader = null;

    public RouterInfoCollector() {
        reconnectMethode = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);
        String lh = JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl");
        if (!JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, lh).equals(lh) || reconnectMethode == null) {
            isLiveHeaderReconnect = false;
        }
        IP = null;
        try {
            IP = getRouterIP();

        } catch (Exception e) {
        }
        try {
            InetAddress ia = Inet4Address.getByName(IP);
            IP = ia.getHostAddress();
            routerHost = ia.getHostName();
        } catch (Exception e) {
        }
        if (IP == null) { return; }
        try {
            UPnPInfo up = new UPnPInfo(IP);
            uPnPSCPDs = up.SCPDs;
        } catch (Exception e) {
        }

        Browser br = new Browser();
        try {
            routerSite = br.getPage("http://" + IP);
            pageHeader = "";
            Map<String, List<String>> he = br.openGetConnection("http://" + IP).getHeaderFields();
            for (Entry<String, List<String>> b : he.entrySet()) {
                if (b.getKey() != null) pageHeader += b.getKey() + ": ";
                boolean bs = false;
                for (Iterator<String> iterator = b.getValue().iterator(); iterator.hasNext();) {
                    String type = (String) iterator.next();
                    if (bs) {
                        pageHeader += ";";
                    }
                    pageHeader += type;

                    bs = true;
                }
                pageHeader += "\r\n";
            }
            pageHeader.trim();

        } catch (IOException e) {
        }
        try {
            routerErrorPage = br.getPage("http://" + IP + "/error404");
        } catch (IOException e) {
        }
        try {
            routerMAC = new GetMacAdress().getMacAddress(IP);
        } catch (Exception e) {
        }
        if (isLiveHeaderReconnect) {
            String rn = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, null);
            if (rn != null) {
                routerMethodeNames = new String[] { rn };
            } else if (reconnectMethode != null && !reconnectMethode.matches("\\s")) {
                ArrayList<String> rmn = new ArrayList<String>();
                for (String[] script : new HTTPLiveHeader().getLHScripts()) {
                    if (script[2].trim().equals(reconnectMethode) && !rmn.contains(script[1])) rmn.add(script[1]);
                }
                routerMethodeNames = rmn.toArray(new String[rmn.size()]);
            }
        }
        haveSip = GetRouterInfo.checkport(IP, 5060);

    }

    @SuppressWarnings("deprecation")
    public HashMap<String, String> getHashMap(boolean urlencode)
    {
        HashMap<String, String> ret = new HashMap<String, String>();
            if(IP!=null)
        ret.put("RouterIP",IP);
            if(routerHost!=null)
        ret.put("RouterHost",routerHost);
        if(routerMethodeNames!=null)
        {
        try {
            ret.put("Routernames", JDUtilities.objectToXml(routerMethodeNames));
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
        if(routerMAC!=null)
        ret.put("RouterMAC", routerMAC);
        if(uPnPSCPDs!=null)
        {
        try {
            ret.put("UPnPSCPDs", JDUtilities.objectToXml(uPnPSCPDs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
        if(pageHeader!=null)
        ret.put("PageHeader", pageHeader);
        if(routerSite!=null)
        ret.put("RouterPage", routerSite);
        if(routerErrorPage!=null)
        ret.put("RouterErrorPage", routerErrorPage);
        if(reconnectMethode!=null)
        ret.put("ReconnectMethode", reconnectMethode);
        if(haveSip)
        ret.put("HaveSip", "true");
        if(urlencode)
            {
            for (Entry<String, String> ent : ret.entrySet()) {
                ent.setValue(URLEncoder.encode(ent.getValue()));
            }
            }
        return ret;
        
    }

    @Override
    public String toString() {
        try {
            return JDUtilities.objectToXml(getHashMap(false));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getRouterIP() {
        String routerIp = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        if (routerIp == null || routerIp.matches("\\s*")) routerIp = new GetRouterInfo(null).getAdress();
        return routerIp;
    }

    /*
     * public String toXMLString() { try { return
     * JDUtilities.objectToXml(this.rcd); } catch (IOException e) { //
     * Auto-generated catch block e.printStackTrace(); } return null;
     * 
     * }
     */
    /**
     * Gibt einen Prozentwert zurück zu welcher Wahrscheinlichkeit es sich um
     * diesen router handelt
     * 
     * @param routerInfo
     * @return
     */
    public int compare(RouterInfoCollector routerInfo) {
        int ret = 0;
        if (routerMAC.substring(0, 8).equalsIgnoreCase(routerInfo.routerMAC.substring(0, 8))) ret += 40;
        if (routerSite.equalsIgnoreCase(routerInfo.routerSite)) ret += 25;
        if (routerErrorPage.equalsIgnoreCase(routerInfo.routerErrorPage)) ret += 25;
        if (haveSip == routerInfo.haveSip) ret += 10;
        return ret;
    }

    /*
     * public static String getXMLString() { RouterInfoCollector ric = new
     * RouterInfoCollector(); if (!ric.rcd.isLiveHeaderReconnect()) return null;
     * try { ric.rcd.setRouterMAC(ric.rcd.getRouterMAC().substring(0, 8)); }
     * catch (Exception e) { //  handle exception }
     * 
     * return ric.toXMLString(); }
     */
    public static void showDialog() {
//        if (true) {
             if (JDUtilities.getConfiguration().getBooleanProperty(
             PROPERTY_SHOW_ROUTERINFO_DIALOG, true)) {
            new Thread(new Runnable() {

                public void run() {
                    RouterInfoCollector ric = new RouterInfoCollector();
                    String xml = ric.toString();
                    if (xml != null) {
                        CountdownConfirmDialog ccd = new CountdownConfirmDialog(JDUtilities.getParentFrame(SimpleGUI.CURRENTGUI.getFrame()), JDLocale.L("routerinfocollector.dialog.title", "Helfen sie die Routererkennung zu verbessern"), 30, true, CountdownConfirmDialog.STYLE_YES | CountdownConfirmDialog.STYLE_NO | CountdownConfirmDialog.STYLE_DETAILLABLE, JDLocale.L("routerinfocollector.dialog.msg", "<b>Um die automatische Routererkennung zu verbessern sammeln wir Routerinformationen!</b><br>Wenn sie damit einverstanden sind die Informationen aus den Details an unseren Server zu übermitteln bestätigen sie mit ja!"), xml);

                        if (!ccd.window_Closed) JDUtilities.getConfiguration().setProperty(PROPERTY_SHOW_ROUTERINFO_DIALOG, false);
                        if (ccd.result) ric.sendToServer();
                    }

                }
            }).start();
        }
    }

    public void sendToServer() {
        if (!isLiveHeaderReconnect) return;
        // String md5 = JDUtilities.getMD5("jdsecred" +
        // JDUtilities.getMD5(xmlString));
        // HashMap<String, String> post = new HashMap<String, String>();
        // post.put("routerinfo", URLEncoder.encode(xmlString));
        // post.put("key", md5);

        Browser br = new Browser();
        try {
            br.setAuth("http://jdownloader.org/router/import.php","jd","jdroutercollector");
            String out = br.postPage("http://jdownloader.org/router/import.php", getHashMap(true));
            // System.out.println(br);
            if (out == null || !out.equals("No htmlCode read")) JDUtilities.getLogger().severe(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new JDInit().loadConfiguration();
        RouterInfoCollector ric = new RouterInfoCollector();
        System.out.println(ric.IP);
        String xml = ric.toString();
        System.out.println(xml);

        // System.out.println(rc.compare(rc));
        System.exit(0);
    }
}
