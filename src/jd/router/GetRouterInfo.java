package jd.router;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Vector;

import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class GetRouterInfo {
    private String adress = null;
    private String name = null;
    private static String[] routerNames = new String[]{"fritz", "3com", "4mbo", "acer", "all130dsl", "all130dsla", "allied", "allnet", "amit", "arcor", "ARK IS-104", "astranet-modem", "asus", "auerswald", "belkin", "bibac", "billion", "broadband", "buffalo", "cnet", "compex", "compu-shack", "conceptronics", "connectec", "d-link", "dc-201", "devolo", "digitus", "dlink", "draytec", "draytek", "edimax", "elsa", "eumex", "fiberline", "fli4l", "free Control", "i-smart", "iplinx", "kobishi", "lanware", "lcs-883r-dsl-4f", "level", "level-one", "levelone", "LG L", "linkpro", "linksys", "longshine", "mentor", "microsoft", "MSI RG", "neteasy", "netgear", "netopia", "nexland", "pearl", "pheenet", "philips", "q-tec", "siemens", "silvercrest", "sitecom", "smc", "sphairon", "st lab", "surecom", "synergy21", "t-com", "targa", "tei6608s", "teledat", "thomson", "tp-link", "trendnet", "trendware", "trust", "robotics", "vantage", "we.com", "x-micro", "zywall", "zyxel"};
    private boolean checkport80(String host) {
        Socket sock;
        try {
            sock = new Socket(host, 80);
            sock.setSoTimeout(20);
            return true;
        } catch (UnknownHostException e) {
        } catch (IOException e) {
        }
        return false;

    }
    public String getAdress() {
        if (adress != null)
            return adress;
        String[] hosts = new String[]{"192.168.2.1", "192.168.1.1", "192.168.0.1", "fritz.box"};
        for (int i = 0; i < hosts.length; i++) {
            try {
                if (InetAddress.getByName(hosts[i]).isReachable(2)) {
                    if (checkport80(hosts[i]))
                    {
                        adress=hosts[i];
                        return hosts[i];
                    }
                }

            } catch (IOException e) {
            }
        }
        String ip = null;
        try {
            InetAddress myAddr = InetAddress.getLocalHost();
            ip = myAddr.getHostAddress();
        } catch (UnknownHostException exc) {
        }
        if (ip != null) {
            String host = ip.replace("\\.[\\d]+$", ".");
            for (int i = 1; i < 255; i++) {
                try {
                    String lhost = host + i;
                    if (!lhost.equals(ip) && InetAddress.getByName(host).isReachable(2)) {
                        if (checkport80(host))
                        {
                            adress=host;
                            return host;
                        }
                    }

                } catch (IOException e) {
                }
            }
        }
        return null;

    }
    public String getRouterNames() {
        if(name!=null)
            return name;
        getAdress();
        try {
            RequestInfo request = Plugin.getRequest(new URL("http://"+adress));
            String html = request.getHtmlCode().toLowerCase();
            for (int i = 0; i < routerNames.length; i++) {
                if(html.matches("(?s).*"+routerNames[i]+".*"))
                {
                    name=routerNames[i];
                    return routerNames[i];
                }
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;

    }
    public RouterData[] getRouterDatas()
    {
        if(getRouterNames()==null)
            return null;
        File fileRoutersDat;
        Vector<RouterData> routerData;
        fileRoutersDat = JDUtilities.getResourceFile("jd/router/routerdata.xml");
        if (fileRoutersDat != null) {
            RouterParser parser = new RouterParser();

            routerData = parser.parseXMLFile(fileRoutersDat);
            Vector<RouterData> retRouterData = new Vector<RouterData>();
            for (int i = 0; i < routerData.size(); i++) {
                RouterData dat = routerData.get(i);
                if(dat.getRouterName().toLowerCase().matches(".*"+name+".*"))
                retRouterData.add(dat);
            }
            return retRouterData.toArray(new RouterData[retRouterData.size()]);
        }
        return null;
        
    }
    /**
     * @param args
     */
    public static void main(String[] args) {
       GetRouterInfo router = new GetRouterInfo();
       System.out.println(router.getAdress());
       System.out.println(router.getRouterNames());
       RouterData[] retRouterDatas = router.getRouterDatas();
       for (int i = 0; i < retRouterDatas.length; i++) {
        System.out.println(retRouterDatas[i].getRouterName());
       }
    }

}
