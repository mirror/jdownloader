//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.HashMap;

import jd.parser.Regex;
import jd.utils.EditDistance;
import jd.utils.JDUtilities;

public class RInfo implements Serializable {

    private static final long serialVersionUID = -2119228262137830055L;

    private boolean haveUpnp = false;
    private boolean haveUpnpReconnect = false;
    private int id;
    private int integrety;
    private String pageHeader;
    private String reconnectMethode;
    private String reconnectMethodeClr;
    private String routerErrorPage;
    private String routerHost;
    private String routerIP;
    private String routerMAC = null;
    private String routerName = null;
    private String routerPage;
    private String routerPageLoggedIn;

    public transient boolean setPlaceholder = false;

    public int compare(RInfo rInfo) {
        int ret = 0;
        if (routerIP != null && !routerIP.equals(rInfo.routerIP)) ret += 50;
        if (routerHost != null && !routerHost.equals(rInfo.routerHost)) ret += 100;
        if (routerMAC != null && !routerMAC.equals(rInfo.routerMAC)) ret += 100;
        ret += EditDistance.getLevenshteinDistance(pageHeader, rInfo.pageHeader);
        ret += EditDistance.getLevenshteinDistance(routerErrorPage, rInfo.routerErrorPage);
        ret += EditDistance.getLevenshteinDistance(routerPage, rInfo.routerPage);
        return ret;
    }

    public int countHtmlTags() {
        if (routerPage == null) return 0;
        return new Regex(routerPage, "<[^>]*>").count();
    }

    public HashMap<String, String> getHashMap() {
        Class<? extends RInfo> infoc = getClass();
        HashMap<String, String> ret = new HashMap<String, String>();
        for (Field field : infoc.getDeclaredFields()) {
            if (!field.getName().equals("setPlaceholder") && !field.getName().equals("serialVersionUID") && !field.getName().equals("id")) {
                try {
                    Object content = field.get(this);
                    String StrCont = null;
                    if (content != null) {
                        if (content instanceof String)
                            StrCont = (String) content;
                        else if (content instanceof Boolean)
                            StrCont = ((Boolean) content) ? "1" : "0";
                        else {
                            try {
                                StrCont = JDUtilities.objectToXml(content);
                            } catch (IOException e) {
                                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occurred",e);
                            }
                        }
                        int c = 0;
                        while (field.getName().equals("routerName") && StrCont.startsWith("<?xml version")) {
                            if (c++ == 10) {
                                StrCont = "";
                                break;
                            }
                            try {
                                String[] t = ((String[]) JDUtilities.xmlStringToObjekt(StrCont));
                                if (StrCont.length() == 0)
                                    StrCont = "";
                                else
                                    StrCont = t[0];
                            } catch (Exception e) {
                            }

                        }
                        if (StrCont.length() == 0) StrCont = null;
                    }
                    if (StrCont != null) {
                        try {
                            ret.put(field.getName(), URLEncoder.encode(StrCont, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occurred",e);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occurred",e);
                } catch (IllegalAccessException e) {
                    jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occurred",e);
                }
            }
        }
        ret.put("HTMLTagCount", "" + countHtmlTags());
        return ret;

    }

    public int getId() {
        return id;
    }

    public int getIntegrety() {
        return integrety;
    }

    public String getPageHeader() {
        return pageHeader;
    }

    public String getReconnectMethode() {
        return reconnectMethode;
    }

    public String getReconnectMethodeClr() {
        return reconnectMethodeClr;
    }

    public String getRouterErrorPage() {
        return routerErrorPage;
    }

    public String getRouterHost() {
        return routerHost;
    }

    public String getRouterIP() {
        return routerIP;
    }

    public String getRouterMAC() {
        return routerMAC;
    }

    public String getRouterName() {
        return routerName;
    }

    public String getRouterPage() {
        return routerPage;
    }

    public String getRouterPageLoggedIn() {
        return routerPageLoggedIn;
    }

    public boolean isHaveUpnp() {
        return haveUpnp;
    }

    public boolean isHaveUpnpReconnect() {
        return haveUpnpReconnect;
    }

    public void sendToServer() {
        try {
            try {
                if (reconnectMethode != null && !reconnectMethode.contains("%%%pass%%%")) {
                    reconnectMethode = SQLRouterData.setPlaceHolder(reconnectMethode);
                }
                if (routerName != null) {
                    int c = 0;
                    while (routerName.startsWith("<?xml version")) {
                        String[] arRouterNames = ((String[]) JDUtilities.xmlStringToObjekt(routerName));
                        if (arRouterNames.length == 0) {
                            routerName = null;
                            break;
                        } else
                            routerName = arRouterNames[0];
                        if (c++ == 10) {
                            routerName = null;
                            break;
                        }
                    }

                }
                if (routerName == null || routerName.equals("Reconnect Recorder Methode")) {
                    routerName = new Regex(getRouterPage(), "<title>(.*?)</title>").getMatch(0);
                }

            } catch (Exception e) {
            }

            if (reconnectMethode != null) System.out.println(SQLRouterData.br.postPage("http://localhost/router/setIntegrety2.php", getHashMap()));

        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occurred",e);
        }
    }

    public void setHaveUpnp(boolean haveUpnp) {
        this.haveUpnp = haveUpnp;
    }

    public void setHaveUpnpReconnect(boolean haveUpnpReconnect) {
        this.haveUpnpReconnect = haveUpnpReconnect;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    public void setIntegrety(int integrety) {
        this.integrety = integrety;
    }

    public void setIntegrety(String integrety) {
        this.integrety = Integer.parseInt(integrety);
    }

    public void setPageHeader(String pageHeader) {
        this.pageHeader = SQLRouterData.replaceTimeStamps(pageHeader);
    }

    public void setReconnectMethode(String reconnectMethode) {
        this.reconnectMethode = reconnectMethode;
    }

    public void setReconnectMethodeClr(String reconnectMethodeClr) {
        this.reconnectMethodeClr = reconnectMethodeClr;
    }

    public void setRouterErrorPage(String routerErrorPage) {
        this.routerErrorPage = SQLRouterData.replaceTimeStamps(routerErrorPage);
    }

    public void setRouterHost(String routerHost) {
        this.routerHost = routerHost;
    }

    public void setRouterIP(String routerIP) {
        this.routerIP = routerIP;
    }

    public void setRouterMAC(String routerMAC) {
        if (this.routerMAC == null) {
            this.routerMAC = routerMAC.replaceAll(" ", "0");
            if (this.routerMAC.length() > 8) this.routerMAC = this.routerMAC.substring(0, 8);
        }
    }

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    public void setRouterPage(String routerPage) {
        this.routerPage = SQLRouterData.replaceTimeStamps(routerPage);
    }

    public void setRouterPageLoggedIn(String routerPageLoggedIn) {
        this.routerPageLoggedIn = SQLRouterData.replaceTimeStamps(routerPageLoggedIn);
    }

    public void setUPnPSCPDs(HashMap<String, String> UPnPSCPDs) {

        if (UPnPSCPDs != null) {
            haveUpnp = true;
            haveUpnpReconnect = SQLRouterData.haveUpnpReconnect(UPnPSCPDs);
            String[] infoupnp = SQLRouterData.getNameFormUPnPSCPDs(UPnPSCPDs);
            String name = null;
            if (infoupnp != null) {
                name = infoupnp[0];
                if (infoupnp[1] != null) name += " " + infoupnp[1];
            }
            if (name != null) setRouterName(name);
            if (getRouterMAC() == null || getRouterMAC().length() == 0) {
                try {
                    routerMAC = infoupnp[2].replaceAll(" ", "0");
                } catch (Exception e) {
                }

            }
        }
    }

    @SuppressWarnings("unchecked")
    public void setUPnPSCPDs(String pnPSCPDs) {
        if (pnPSCPDs == null) { return; }
        HashMap<String, String> UPnPSCPDs = null;
        try {
            if (pnPSCPDs != null && pnPSCPDs.length() > 0) UPnPSCPDs = (HashMap<String, String>) JDUtilities.xmlStringToObjekt(pnPSCPDs);
        } catch (Exception e) {
        }

        setUPnPSCPDs(UPnPSCPDs);
    }
}
