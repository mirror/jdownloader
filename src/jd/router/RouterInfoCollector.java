//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import jd.JDInit;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.http.Browser;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RouterInfoCollector {
    public final static String PROPERTY_SHOW_ROUTERINFO_DIALOG = "PROPERTY_SHOW_ROUTERINFO_DIALOG";
    public final static String RECONNECTTYPE_LIVE_HEADER = JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl");
    public final static String RECONNECTTYPE_CLR = JDLocale.L("modules.reconnect.types.clr", "CLR Script");
    protected String reconnectType = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"));
    protected String routerSite = null;
    protected String routerErrorPage = null;
    protected String routerMAC = null;
    protected String[] routerMethodeNames = null;
    protected HashMap<String, String> uPnPSCPDs = null;
    protected String reconnectMethode = null;
    protected String reconnectMethodeClr = null;
    protected String IP = null;
    protected String routerHost = null;
    protected String pageHeader = null;

    public RouterInfoCollector() {
        reconnectMethode = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);
        reconnectMethodeClr = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, null);
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
        if (isLiveheader()) {
            String rn = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, null);
            if (rn != null) {
                routerMethodeNames = new String[] { rn };
            } else if (!reconnectMethode.matches("\\s")) {
                ArrayList<String> rmn = new ArrayList<String>();
                for (String[] script : new HTTPLiveHeader().getLHScripts()) {
                    if (script[2].trim().equals(reconnectMethode) && !rmn.contains(script[1])) rmn.add(script[1]);
                }
                routerMethodeNames = rmn.toArray(new String[rmn.size()]);
            }
        }

    }

    public boolean isLiveheader() {
        return (reconnectType.equals(RECONNECTTYPE_LIVE_HEADER) && reconnectMethode != null);
    }

    public boolean isClr() {
        return (reconnectType.equals(RECONNECTTYPE_CLR) && reconnectMethodeClr != null);
    }

    public boolean isValidReconnect() {
        return (isLiveheader() || isClr());
    }

    @SuppressWarnings("deprecation")
    public HashMap<String, String> getHashMap(boolean urlencode) {
        HashMap<String, String> ret = new HashMap<String, String>();
        if (IP != null) ret.put("RouterIP", IP);
        if (routerHost != null) ret.put("RouterHost", routerHost);
        if (routerMethodeNames != null) {
            try {
                ret.put("Routernames", JDUtilities.objectToXml(routerMethodeNames));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (routerMAC != null) ret.put("RouterMAC", routerMAC.substring(0, 8));
        if (uPnPSCPDs != null) {
            try {
                ret.put("UPnPSCPDs", JDUtilities.objectToXml(uPnPSCPDs));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (pageHeader != null) ret.put("PageHeader", pageHeader);
        if (routerSite != null) ret.put("RouterPage", routerSite);
        if (routerErrorPage != null) ret.put("RouterErrorPage", routerErrorPage);
        if (isLiveheader()) ret.put("ReconnectMethode", reconnectMethode);
        if (isClr()) ret.put("ReconnectMethodeClr", reconnectMethodeClr);
        if (urlencode) {
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
        return ret;
    }

    /*
     * public static String getXMLString() { RouterInfoCollector ric = new
     * RouterInfoCollector(); if (!ric.rcd.isLiveHeaderReconnect()) return null;
     * try { ric.rcd.setRouterMAC(ric.rcd.getRouterMAC().substring(0, 8)); }
     * catch (Exception e) { // handle exception }
     * 
     * return ric.toXMLString(); }
     */
    public static void showDialog() {
        // if (true) {
        if (JDUtilities.getConfiguration().getBooleanProperty(PROPERTY_SHOW_ROUTERINFO_DIALOG, true)) {
            new Thread(new Runnable() {

                public void run() {
                    RouterInfoCollector ric = new RouterInfoCollector();
                    String xml = ric.toString();
                    if (xml != null && ric.isValidReconnect()) {
                        CountdownConfirmDialog ccd = new CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(), JDLocale.L("routerinfocollector.dialog.title", "Helfen sie die Routererkennung zu verbessern"), 30, true, CountdownConfirmDialog.STYLE_YES | CountdownConfirmDialog.STYLE_NO , JDLocale.L("routerinfocollector.dialog.msg", "<b>Um die automatische Routererkennung zu verbessern sammeln wir Routerinformationen!</b><br>Wenn sie damit einverstanden sind die ihre Routerinformationen an unseren Server zu übermitteln bestätigen sie mit ja!"));

                        if (!ccd.window_Closed) 
                            {
                            JDUtilities.getConfiguration().setProperty(PROPERTY_SHOW_ROUTERINFO_DIALOG, false);
                            JDUtilities.getConfiguration().save();
                            }
                        if (ccd.result) ric.sendToServer();
                    }

                }
            }).start();
        }
    }

    public void sendToServer() {
        if (!isValidReconnect()) return;
        // String md5 = JDHash.getMD5("jdsecred" +
        // JDHash.getMD5(xmlString));
        // HashMap<String, String> post = new HashMap<String, String>();
        // post.put("routerinfo", URLEncoder.encode(xmlString));
        // post.put("key", md5);

        Browser br = new Browser();
        try {
//            br.setAuth("http://loaclhost/router/import.php", "jd", "jdroutercollector");
            String out = br.postPage("http://service.jdownloader.net/routerdb/import.php", getHashMap(true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new JDInit().loadConfiguration();
        showDialog();
    }
}
