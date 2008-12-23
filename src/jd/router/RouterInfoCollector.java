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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.nutils.jobber.JDRunnable;

import jd.nutils.Threader;

import jd.parser.Regex;

import jd.JDInit;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.http.Browser;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class RouterInfoCollector {
    public final static String PROPERTY_SHOW_ROUTERINFO_DIALOG = "PROPERTY_SHOW_ROUTERINFO_DIALOG";
    public final static String RECONNECTTYPE_LIVE_HEADER = JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl");
    public final static String RECONNECTTYPE_CLR = JDLocale.L("modules.reconnect.types.clr", "CLR Script");
    protected static String reconnectType = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_RECONNECT_TYPE, JDLocale.L("modules.reconnect.types.liveheader", "LiveHeader/Curl"));
    protected static String reconnectMethode = null;
    protected static String reconnectMethodeClr = null;
    public final static int RInfo_UPNP = 1 << 1;
    public final static int RInfo_MAC = 1 << 2;
    public final static int RInfo_METHODENAME = 1 << 3;
    public final static int RInfo_ROUTERPAGE = 1 << 4;
    public final static int RInfo_ROUTERERROR = 1 << 5;
    public final static int RInfo_HOSTNAME = 1 << 6;
    public final static int RInfo_ALL = RInfo_UPNP|RInfo_MAC|RInfo_METHODENAME|RInfo_ROUTERPAGE|RInfo_ROUTERERROR|RInfo_HOSTNAME;
    public final static int RInfo_ROUTERSEARCH = RInfo_MAC|RInfo_ROUTERPAGE|RInfo_ROUTERERROR|RInfo_HOSTNAME;
    public static boolean isLiveheader() {
        reconnectMethode = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);
        return (reconnectType.equals(RECONNECTTYPE_LIVE_HEADER) && reconnectMethode != null);
    }

    public static boolean isClr() {
        reconnectMethodeClr = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, null);
        return (reconnectType.equals(RECONNECTTYPE_CLR) && reconnectMethodeClr != null);
    }

    public static boolean isValidReconnect() {
        return (isLiveheader() || isClr());
    }

    public static String getRouterIP() {
        String routerIp = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        if (routerIp == null || routerIp.matches("\\s*")) routerIp = new GetRouterInfo(null).getAdress();
        return routerIp;
    }

    public static void showDialog() {
        /*
         * if (isValidReconnect()) { if
         * (JDUtilities.getConfiguration().getBooleanProperty
         * (PROPERTY_SHOW_ROUTERINFO_DIALOG, true)) { if (rict != null &&
         * rict.isAlive()) return; rict = new Thread(new Runnable() { public
         * void run() { RouterInfoCollector ric = new RouterInfoCollector();
         * String xml = ric.toString(); if (xml != null && isValidReconnect()) {
         * CountdownConfirmDialog ccd = new
         * CountdownConfirmDialog(SimpleGUI.CURRENTGUI.getFrame(),
         * JDLocale.L("routerinfocollector.dialog.title",
         * "Helfen sie die Routererkennung zu verbessern"), 30, true,
         * CountdownConfirmDialog.STYLE_YES | CountdownConfirmDialog.STYLE_NO |
         * CountdownConfirmDialog.STYLE_DETAILLABLE,
         * JDLocale.L("routerinfocollector.dialog.msg",
         * "<b>Um die automatische Routererkennung zu verbessern sammeln wir Routerinformationen!</b><br>Wenn sie damit einverstanden sind die Informationen aus den Details an unseren Server zu übermitteln bestätigen sie mit ja!"
         * ), xml);
         * 
         * if (!ccd.window_Closed) {JDUtilities.getConfiguration().setProperty(
         * PROPERTY_SHOW_ROUTERINFO_DIALOG, false);
         * JDUtilities.getConfiguration().save(); } if (ccd.result) { if
         * (ric.routerMethodeNames == null) { ric.routerMethodeNames =
         * SimpleGUI.CURRENTGUI.showUserInputDialog(JDLocale.L(
         * "routerinfocollector.namedialog.title",
         * "Please enter the following routerinfos: manufacturer, model, firmware. (e.g DLink, 635 , FW1.37)"
         * )); } ric.sendToServer(); } }
         * 
         * } }); rict.start(); } }
         */
    }

    public static RInfo getRInfo(final int infoToCollect) {
        final RInfo info = new RInfo();
        final Browser br = new Browser();
        try {
            info.setRouterIP(getRouterIP());
        } catch (Exception e) {
        }
        Threader th = new Threader();
        if ((infoToCollect & RInfo_UPNP) != 0) {
            th.add(new JDRunnable() {

                public void go() throws Exception {

                    try {
                        UPnPInfo up = new UPnPInfo(info.getRouterIP());
                        info.setUPnPSCPDs(up.SCPDs);
                    } catch (Exception e) {
                    }

                }
            });
        }
        if ((infoToCollect & RInfo_HOSTNAME) != 0) {
        th.add(new JDRunnable() {

            public void go() throws Exception {
                try {
                    InetAddress ia = Inet4Address.getByName(info.getRouterIP());
                    info.setRouterIP(ia.getHostAddress());
                    info.setRouterHost(ia.getHostName());
                } catch (Exception e) {
                }

            }
        });
        }
        if ((infoToCollect & RInfo_ROUTERERROR) != 0) {
        th.add(new JDRunnable() {

            public void go() throws Exception {
                try {
                    info.setRouterErrorPage(br.getPage("http://" + info.getRouterIP() + "/error404"));
                } catch (IOException e) {
                }
            }
        });
        }
        if ((infoToCollect & RInfo_MAC) != 0) {
        th.add(new JDRunnable() {

            public void go() throws Exception {
                try {
                    info.setRouterMAC(new GetMacAdress().getMacAddress(info.getRouterIP()));
                } catch (Exception e) {
                }
            }
        });
        }
        th.add(new JDRunnable() {

            public void go() throws Exception {
                if ((infoToCollect & RInfo_ROUTERPAGE) != 0) {
                try {
                    info.setRouterPage(br.getPage("http://" + info.getRouterIP()));

                    StringBuilder pageHeader = new StringBuilder();
                    Map<String, List<String>> he = br.getHttpConnection().getHeaderFields();
                    for (Entry<String, List<String>> b : he.entrySet()) {
                        if (b.getKey() != null) {
                            pageHeader.append(b.getKey());
                            pageHeader.append(new char[] { ':', ' ' });
                        }
                        boolean bs = false;
                        for (Iterator<String> iterator = b.getValue().iterator(); iterator.hasNext();) {
                            String type = (String) iterator.next();
                            if (bs) {
                                pageHeader.append(';');
                            }
                            pageHeader.append(type);

                            bs = true;
                        }
                        pageHeader.append(new char[] { '\r', '\n' });
                    }
                    info.setPageHeader(pageHeader.toString().trim());

                } catch (IOException e) {
                }
                }
                if ((infoToCollect & RInfo_METHODENAME) != 0) {
                if (isLiveheader()) {
                    String rn = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_ROUTERNAME, null);
                    if (info.getRouterName() == null) {
                        if (rn != null) {
                            {
                                if (!rn.equals("Reconnect Recorder Methode")) info.setRouterName(info.getRouterName());
                            }
                        } else if (!reconnectMethode.matches("\\s")) {

                            for (String[] script : new HTTPLiveHeader().getLHScripts()) {
                                if (script[2].trim().equals(reconnectMethode.trim()) && (info.getRouterName() == null || !info.getRouterName().contains(script[0] + " - " + script[1]))) info.setRouterName(info.getRouterName() == null ? script[0] + " - " + script[1] : info.getRouterName() + " | " + script[0] + " - " + script[1]);
                            }
                        }
                    }
                    if (rn == null || rn.equals("Reconnect Recorder Methode"))
                        info.setReconnectMethode(SQLRouterData.setPlaceHolder(reconnectMethode));
                    else
                        info.setReconnectMethode(reconnectMethode);

                }
                if (isClr()) {
                    if (info.getRouterName() == null) {
                        info.setRouterName(new Regex(info.getReconnectMethodeClr(), "<Router name=\"(.*?)\" />").getMatch(0));
                    }
                    info.setReconnectMethodeClr(reconnectMethodeClr);
                }
                if (info.getRouterName() == null && info.getRouterPage()!=null) {
                    info.setRouterName(new Regex(info.getRouterPage(), "<title>(.*?)</title>").getMatch(0));
                }
                }
            }
        });
        th.startAndWait();
        return info;
    }

    public static void main(String[] args) {
        new JDInit().loadConfiguration();
        showDialog();
    }
}
