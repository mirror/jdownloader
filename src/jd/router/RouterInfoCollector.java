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

package jd.router;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.HTTPLiveHeader;
import jd.controlling.reconnect.ReconnectMethod;
import jd.http.Browser;
import jd.nrouter.RouterUtils;
import jd.nutils.Threader;
import jd.nutils.jobber.JDRunnable;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class RouterInfoCollector {

    protected static String reconnectMethode = null;
    protected static String reconnectMethodeClr = null;
    public final static int RInfo_UPNP = 1 << 1;
    public final static int RInfo_MAC = 1 << 2;
    public final static int RInfo_METHODENAME = 1 << 3;
    public final static int RInfo_ROUTERPAGE = 1 << 4;
    public final static int RInfo_ROUTERERROR = 1 << 5;
    public final static int RInfo_HOSTNAME = 1 << 6;
    public final static int RInfo_ALL = RInfo_UPNP | RInfo_MAC | RInfo_METHODENAME | RInfo_ROUTERPAGE | RInfo_ROUTERERROR | RInfo_HOSTNAME;
    public final static int RInfo_ROUTERSEARCH = RInfo_MAC | RInfo_ROUTERPAGE | RInfo_ROUTERERROR | RInfo_HOSTNAME;

    public static boolean isLiveheader() {
        reconnectMethode = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS, null);
        return ((JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.LIVEHEADER) && reconnectMethode != null);
    }

    public static boolean isClr() {
        reconnectMethodeClr = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, null);
        return ((JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.CLR) && reconnectMethodeClr != null);
    }

    public static InetAddress getRouterIP() {
        String routerIp = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);
        if (routerIp == null || routerIp.matches("\\s*")) return RouterUtils.getAddress(false);
        try {
            return InetAddress.getByName(routerIp);
        } catch (UnknownHostException e) {
            JDLogger.exception(e);
        }
        return null;
    }

    public static RInfo getRInfo(final int infoToCollect) {
        final RInfo info = new RInfo();
        final Browser br = new Browser();
        final InetAddress ia = getRouterIP();
        try {
            info.setRouterIP(getRouterIP().getHostAddress());
        } catch (Exception e) {
        }
        Threader th = new Threader();
        if ((infoToCollect & RInfo_UPNP) != 0) {
            th.add(new JDRunnable() {

                public void go() throws Exception {

                    try {
                        UPnPInfo up = new UPnPInfo(ia);
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
                        info.setRouterErrorPage(br.getPage("http://" + ia.getHostName() + "/error404"));
                    } catch (IOException e) {
                    }
                }
            });
        }
        if ((infoToCollect & RInfo_MAC) != 0) {
            th.add(new JDRunnable() {

                public void go() throws Exception {
                    try {
                        info.setRouterMAC(GetMacAdress.getMacAddress(ia));
                    } catch (Exception e) {
                    }
                }
            });
        }
        th.add(new JDRunnable() {

            @SuppressWarnings("unchecked")
            public void go() throws Exception {
                if ((infoToCollect & RInfo_ROUTERPAGE) != 0) {
                    try {
                        info.setRouterPage(br.getPage("http://" + ia.getHostName()));

                        StringBuilder pageHeader = new StringBuilder();
                        Map<String, List<String>> he = br.getHttpConnection().getHeaderFields();
                        for (Entry<String, List<String>> b : he.entrySet()) {
                            if (b.getKey() != null) {
                                pageHeader.append(b.getKey());
                                pageHeader.append(new char[] { ':', ' ' });
                            }
                            boolean bs = false;
                            for (Iterator<String> iterator = b.getValue().iterator(); iterator.hasNext();) {
                                String type = iterator.next();
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
                                for (String[] script : HTTPLiveHeader.getLHScripts()) {
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
                    if (info.getRouterName() == null && info.getRouterPage() != null) {
                        info.setRouterName(new Regex(info.getRouterPage(), "<title>(.*?)</title>").getMatch(0));
                    }
                }
            }
        });
        th.startAndWait();
        return info;
    }

}
