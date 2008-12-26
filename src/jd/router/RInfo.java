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
    /**
     * 
     */
    private static final long serialVersionUID = -2119228262137830055L;

    public String getRouterIP() {
        return RouterIP;
    }

    public void setRouterIP(String routerIP) {
        RouterIP = routerIP;
    }

    public String getRouterHost() {
        return RouterHost;
    }

    public void setRouterHost(String routerHost) {
        RouterHost = routerHost;
    }

    public String getRouterMAC() {
        return RouterMAC;
    }

    public void setRouterMAC(String routerMAC) {
        if (RouterMAC == null) {
            RouterMAC = routerMAC.replaceAll(" ", "0");
            if(RouterMAC.length()>8)
                RouterMAC=RouterMAC.substring(0, 8);
        }
    }

    public String getPageHeader() {
        return PageHeader;
    }

    public void setPageHeader(String pageHeader) {
        PageHeader = SQLRouterData.replaceTimeStamps(pageHeader);
    }

    public String getRouterPage() {
        return RouterPage;
    }

    public void setRouterPage(String routerPage) {
        RouterPage = SQLRouterData.replaceTimeStamps(routerPage);
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
                    RouterMAC = infoupnp[2].replaceAll(" ", "0");
                } catch (Exception e) {
                    // TODO: handle exception
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
            // TODO: handle exception
        }

        setUPnPSCPDs(UPnPSCPDs);
    }

    public String getRouterErrorPage() {
        return RouterErrorPage;
    }

    public void setRouterErrorPage(String routerErrorPage) {
        RouterErrorPage = SQLRouterData.replaceTimeStamps(routerErrorPage);
    }

    public String getReconnectMethode() {
        return ReconnectMethode;
    }

    public void setReconnectMethode(String reconnectMethode) {
        ReconnectMethode = reconnectMethode;
    }

    public String getReconnectMethodeClr() {
        return ReconnectMethodeClr;
    }

    public void setReconnectMethodeClr(String reconnectMethodeClr) {
        ReconnectMethodeClr = reconnectMethodeClr;
    }

    public String getRouterPageLoggedIn() {
        return RouterPageLoggedIn;
    }

    public void setRouterPageLoggedIn(String routerPageLoggedIn) {
        RouterPageLoggedIn = SQLRouterData.replaceTimeStamps(routerPageLoggedIn);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }

    public boolean isHaveUpnpReconnect() {
        return haveUpnpReconnect;
    }

    public void setHaveUpnpReconnect(boolean haveUpnpReconnect) {
        this.haveUpnpReconnect = haveUpnpReconnect;
    }

    public boolean isHaveUpnp() {
        return haveUpnp;
    }

    public void setHaveUpnp(boolean haveUpnp) {
        this.haveUpnp = haveUpnp;
    }

    public int countHtmlTags() {
        if (RouterPage == null) return 0;
        return new Regex(RouterPage, "<[^>]*>").count();
    }

    private int id, integrety;
    public int getIntegrety() {
        return integrety;
    }

    public void setIntegrety(String integrety) {
        this.integrety = Integer.parseInt(integrety);
    }
    public void setIntegrety(int integrety) {
        this.integrety = integrety;
    }

    private boolean haveUpnpReconnect = false;
    private boolean haveUpnp = false;
    public transient boolean setPlaceholder = false;
    private String RouterName = null;
    

    public String getRouterName() {
        return RouterName;
    }

    public void setRouterName(String routerName) {
        RouterName = routerName;
    }

    private String RouterIP, RouterHost, RouterMAC = null, PageHeader, RouterPage, RouterErrorPage, ReconnectMethode, ReconnectMethodeClr, RouterPageLoggedIn;

    public int compare(RInfo rInfo) {
        int ret = 0;
        if (RouterIP != null && !RouterIP.equals(rInfo.RouterIP)) ret += 50;
        if (RouterHost != null && !RouterHost.equals(rInfo.RouterHost)) ret += 100;
        if (RouterMAC != null && !RouterMAC.equals(rInfo.RouterMAC)) ret += 100;
        ret += EditDistance.getLevenshteinDistance(PageHeader, rInfo.PageHeader);
        ret += EditDistance.getLevenshteinDistance(RouterErrorPage, rInfo.RouterErrorPage);
        ret += EditDistance.getLevenshteinDistance(RouterPage, rInfo.RouterPage);
        return ret;
    }
/*
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RInfo) {
            RInfo ob = (RInfo) obj;
            if (RouterIP.equals(ob.RouterIP) && RouterHost.equals(ob.RouterHost) && (ob.RouterMAC == null || RouterMAC == null || RouterMAC.equals(ob.RouterMAC)) && ReconnectMethode.equals(ReconnectMethode) && ReconnectMethodeClr.equals(ReconnectMethodeClr)) {
                if (EditDistance.getLevenshteinDifference(PageHeader, ob.PageHeader) < 5 && EditDistance.getLevenshteinDifference(RouterErrorPage, ob.RouterErrorPage) < 10 && EditDistance.getLevenshteinDifference(RouterPage, ob.RouterPage) < 10 && EditDistance.getLevenshteinDifference(RouterPageLoggedIn, ob.RouterPageLoggedIn) < 10) {
                    if (RouterMAC == null) {
                        RouterMAC = ob.RouterMAC;
                    } else if (ob.RouterMAC == null) {
                        ob.RouterMAC = RouterMAC;
                    }
                    if (RouterName == null) {
                        if (ob.RouterName != null) RouterName = ob.RouterName;
                    } else if (ob.RouterName == null) {
                        ob.RouterName = RouterName;
                    }
                    if (haveUpnp && !ob.haveUpnp) {
                        ob.haveUpnp = true;
                        ob.haveUpnpReconnect = haveUpnpReconnect;
                    } else if (ob.haveUpnp && !haveUpnp) {
                        haveUpnp = true;
                        haveUpnpReconnect = ob.haveUpnpReconnect;
                    }
                    return true;
                }

            }
        }
        return false;
    }
*/
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
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        int c = 0;
                        while (field.getName().equals("RouterName") && StrCont.startsWith("<?xml version")) {
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
                                // TODO: handle exception
                            }

                        }
                        if (StrCont.length() == 0) StrCont = null;
                    }
                    if (StrCont != null) {
                        try {
                            ret.put(field.getName(), URLEncoder.encode(StrCont, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        ret.put("HTMLTagCount", "" + countHtmlTags());
        return ret;

    }
    public void setRouterNames(String string) {
        // TODO Auto-generated method stub

    }
    public void sendToServer() {
        try {
            try {
                if (ReconnectMethode != null && !ReconnectMethode.contains("%%%pass%%%")) {
                    ReconnectMethode = SQLRouterData.setPlaceHolder(ReconnectMethode);
                }
                if (RouterName != null) {
                    int c = 0;
                    while (RouterName.startsWith("<?xml version")) {
                        String[] arRouterNames = ((String[]) JDUtilities.xmlStringToObjekt(RouterName));
                        if (arRouterNames.length == 0) {
                            RouterName = null;
                            break;
                        } else
                            RouterName = arRouterNames[0];
                        if (c++ == 10) {
                            RouterName = null;
                            break;
                        }
                    }

                }
                if (RouterName == null || RouterName.equals("Reconnect Recorder Methode")) {
                    RouterName = new Regex(getRouterPage(), "<title>(.*?)</title>").getMatch(0);
                }

            } catch (Exception e) {
                // TODO: handle exception
            }

            if(ReconnectMethode!=null)
            System.out.println(SQLRouterData.br.postPage("http://localhost/router/setIntegrety2.php", getHashMap()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
