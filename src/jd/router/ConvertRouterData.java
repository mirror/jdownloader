package jd.router;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jd.controlling.interaction.HTTPLiveHeader;
import jd.unrar.Utilities;
import jd.utils.JDUtilities;

public class ConvertRouterData {
    private String ip = "Host: %%%routerip%%%\n";
    private String cookie = null;
    private Vector<String[]> routerData = null;
    private String getType(int type) {
        if (type == 3)
            return "POST ";
        else
            return "GET ";

    }
    private String getPoperties(HashMap<String, String> poperties) {
        if (poperties == null)
            return "Content-Type: application/x-www-form-urlencoded\n";
        String Script = "";
        for (Map.Entry<String, String> entry : poperties.entrySet()) {
            Script += entry.getKey() + ": " + entry.getValue() + "\n";
        }
        return Script;
    }
    private String convertUserPass(String arg) {
        if (arg != null)
            return arg.replaceAll("MD5PasswordL(%PASSWORD%)", "%%%MD5:::pass%%%").replaceAll("\\%USERNAME\\%", "%%%user%%%").replaceAll("\\%PASSWORD\\%", "%%%pass%%%");
        return null;
    }
    private String defaultRequest(int type, String address, String post, HashMap<String, String> poperties) {
        if (address == null || address.matches("[\\s]*")) {
            if (type == 2)
                return "";
            address = "";
            if (post == null || post.matches("[\\s]*"))
                return "";

        }
        String bakip = ip;
        String cookiebak = cookie;
        if (address.matches("http://.*")) {
            cookie = "";
            address = address.replaceFirst("http://", "");
            ip = "Host: http://" + address.replaceFirst("/.*", "") + "\n";
            if (!address.matches(".*/.*"))
                address = "/";
            else
                address = address.replaceFirst(".*/", "/");
        }
        
        post = convertUserPass(post);

        address = convertUserPass(address);
        if(address.charAt(0)!='/')
            address="/"+address;
        String script = "";
        script += "\t[[[STEP]]]\n" + "\t\t[[[REQUEST]]]\n" + getType(type) + address + " HTTP/1.1\n" + "Accept-Language: de, en-gb;q=0.9, en;q=0.8" + "User-Agent: Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)\n" + ip + "Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5\n" + cookie;
        if (type == 3) {
            script += getPoperties(poperties) + ((post != null) ? ("Content-Length: " + post.length() + "\n\n" + post + "\n") : "\n");
        }
        script += "\t\t[[[/REQUEST]]]\n" + "\t[[[/STEP]]]\n";
        ip = bakip;
        cookie = cookiebak;
        return script;
    }
    private String[] toLiveHeader(RouterData routerData) {
        ip = routerData.getRouterIP();
        cookie = "";
        int routerport = routerData.getRouterPort();
        String port = "";
        if (routerport != 80)
            port = ":" + routerport;
        if (ip == null || ip.matches("[\\s]*") || ip.matches("192.168.0.1")) {
            ip = "Host: %%%routerip%%%" + port + "\n";
        } else {
            ip = "Host: " + ip + port + "\n";
        }
        String script = "[[[HSRC]]]\n";
        String login = defaultRequest(routerData.getLoginType(), routerData.getLogin(), routerData.getLoginPostParams(), routerData.getLoginRequestProperties());
        if (!login.equals(""))
            cookie = "Cookie: %%%Set-Cookie%%%\n";
        else
            cookie = "Authorization: Basic %%%basicauth%%%\n";
        script += login;
        String disconnect = defaultRequest(routerData.getDisconnectType(), routerData.getDisconnect(), routerData.getDisconnectPostParams(), routerData.getDisconnectRequestProperties());
        if (disconnect.equals(""))
            return null;
        script += disconnect;
        script += "\t\t[[[STEP]]][[[WAIT seconds=\"1\"/]]][[[/STEP]]]\n";

        script += defaultRequest(routerData.getConnectType(), routerData.getConnect(), routerData.getConnectPostParams(), routerData.getConnectRequestProperties());
        script += defaultRequest(2, routerData.getLogoff(), null, null);
        script += "[[[/HSRC]]]";
        String name = routerData.getRouterName();
        String[] pass = getUserPass(name);
        String regexp = "(?s).*" + name.replaceFirst(" .*", "").toLowerCase() + ".*";
        if(pass[0]!=null && !pass[0].matches("[\\s]*"))
            regexp=pass[0];
        return new String[]{name.replaceFirst(" .*", ""), name, script, regexp, pass[1], pass[2]};

    }
    private RouterData[] readRouterDat(File file) {
        Vector<RouterData> routerData;
        if (file != null) {
            RouterParser parser = new RouterParser();

            routerData = parser.parseXMLFile(file);
            return routerData.toArray(new RouterData[routerData.size()]);
        }
        return null;
    }
    private void saveTolist(Vector<String[]> list, File file) {
        if (file.exists()) {
            list.addAll((Collection<? extends String[]>) Utilities.loadObject(file, true));
            Collections.sort(list, new Comparator<Object>() {
                public int compare(Object a, Object b) {
                    String[] aa = (String[]) a;
                    String[] bb = (String[]) b;

                    if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) > 0) {
                        return 1;
                    } else if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) < 0) {
                        return -1;
                    } else {
                        return 0;
                    }

                }

            });

        }
        Utilities.saveObject(list, file, true);
    }
    private static String replace(String arg)
    {
        return arg.replaceAll("&amp;nbsp;", " ").replaceAll("&#13;", "\n");
    }
    private static void formateNames(File file, File fileout)
    {
        ConvertRouterData conv = new ConvertRouterData();
        Vector<String[]> routers = new Vector<String[]>();
        Vector<String[]> routersv = new Vector<String[]>();
        routers.addAll((Collection<? extends String[]>) Utilities.loadObject(file, true));
        for (int i = 0; i < routers.size(); i++) {
            String[] la = routers.get(i);
            la[1]=replace(la[1]);
            routersv.add(la);
        }
        conv.saveTolist(routersv, fileout);
    }
    private static void ConvertXml(File file, File out)
    {
        ConvertRouterData conv = new ConvertRouterData();
        RouterData[] routers = conv.readRouterDat(file);
        Vector<String[]> routersv = new Vector<String[]>();
        for (int i = 0; i < routers.length; i++) {
            String[] la = conv.toLiveHeader(routers[i]);
            if (la != null) {
                routersv.add(la);
            } else {
                System.out.println(routers[i].getRouterName());
            }
        }
                conv.saveTolist(routersv, out);
    }
    private String[] getUserPass(String routername)
    {
        if(routerData==null)
        routerData = new HTTPLiveHeader().getLHScripts();
        for (int i = 0; i < routerData.size(); i++) {
            String[] router = routerData.get(i);
            if(router[1].equals(routername))
            {
                return new String[] {router[3],router[4],router[5]};
            }
        }
        return new String[] {null,null,null};
    }
    public static void main(String[] args) {
        

   
        File fileRoutersDat = JDUtilities.getResourceFile("jd/Routers.xml");
        
        File fileRoutersout = JDUtilities.getResourceFile("jd/new.xml");
        //formateNames(fileRoutersDat, fileRoutersout);
        ConvertXml(fileRoutersDat, fileRoutersout);
        //RouterParser parser = new RouterParser();
       // parser.routerDatToXML(JDUtilities.getResourceFile("jd/Routers.dat"), JDUtilities.getResourceFile("jd/Routers.xml"));

    }
   

}
