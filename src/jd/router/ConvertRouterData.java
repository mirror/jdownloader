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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import jd.controlling.interaction.HTTPLiveHeader;
import jd.gui.skins.simple.SimpleGUI;
import jd.utils.JDUtilities;
import jd.utils.io.JDIO;

public class ConvertRouterData {
    private static void ConvertXml(File file, File out) {
        ConvertRouterData conv = new ConvertRouterData();
        RouterData[] routers = conv.readRouterDat(file);
        Vector<String[]> routersv = new Vector<String[]>();
        for (RouterData element : routers) {
            String[] la = conv.toLiveHeader(element);
            if (la != null) {
                routersv.add(la);
            } else {
                System.out.println(element.getRouterName());
            }
        }
        conv.saveTolist(routersv, out);
    }

    public static void main(String[] args) {

        File fileRoutersDat = JDIO.getResourceFile("jd/Routers.xml");

        File fileRoutersout = JDIO.getResourceFile("jd/new.xml");
        // formateNames(fileRoutersDat, fileRoutersout);
        ConvertRouterData.ConvertXml(fileRoutersDat, fileRoutersout);
        // RouterParser parser = new RouterParser();
        // parser.routerDatToXML(JDUtilities.getResourceFile("jd/Routers.dat"),
        // JDUtilities.getResourceFile("jd/Routers.xml"));
    }

    private String cookie = null;
    private String ip = "            Host: %%%routerip%%%\r\n";
    private Vector<String[]> routerData = null;

    private String convertUserPass(String arg) {
        if (arg != null) { return arg.replaceAll("MD5PasswordL(%PASSWORD%)", "%%%MD5:::pass%%%").replaceAll("\\%USERNAME\\%", "%%%user%%%").replaceAll("\\%PASSWORD\\%", "%%%pass%%%"); }
        return null;
    }

    private String defaultRequest(int type, String address, String post, HashMap<String, String> poperties) {
        if (address == null || address.matches("[\\s]*")) {
            if (type == 2) { return ""; }
            address = "";
            if (post == null || post.matches("[\\s]*")) { return ""; }
        }
        String bakip = ip;
        String cookiebak = cookie;
        if (address.matches("http://.*")) {
            cookie = "";
            address = address.replaceFirst("http://", "");
            ip = "            Host: http://" + address.replaceFirst("/.*", "") + "\r\n";
            if (!address.matches(".*/.*")) {
                address = "/";
            } else {
                address = address.replaceFirst(".*/", "/");
            }
        }

        post = convertUserPass(post);

        address = convertUserPass(address);
        if (address.charAt(0) != '/') {
            address = "/" + address;
        }
        String script = "";
        script += "    [[[STEP]]]\r\n" + "        [[[REQUEST]]]\r\n            " + getType(type) + address + " HTTP/1.1\r\n" + ip + cookie;
        if (type == 3) {
            script += getPoperties(poperties) + (post != null ? "\r\n" + post + "\r\n" : "\r\n");
        }
        script += "        [[[/REQUEST]]]\r\n" + "    [[[/STEP]]]\r\n";
        ip = bakip;
        cookie = cookiebak;
        return script;
    }

    private String getPoperties(HashMap<String, String> poperties) {
        if (poperties == null) { return ""; }
        String Script = "";
        for (Map.Entry<String, String> entry : poperties.entrySet()) {
            Script += entry.getKey() + ": " + entry.getValue() + "\n";
        }
        return Script;
    }

    private String getType(int type) {
        if (type == 3) {
            return "POST ";
        } else {
            return "GET ";
        }
    }

    private String[] getUserPass(String routername) {
        if (routerData == null) {
            routerData = new HTTPLiveHeader().getLHScripts();
        }
        for (int i = 0; i < routerData.size(); i++) {
            String[] router = routerData.get(i);
            if (router[1].equals(routername)) { return new String[] { router[3], router[4], router[5] }; }
        }
        return new String[] { null, null, null };
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

    @SuppressWarnings("unchecked")
    private void saveTolist(Vector<String[]> list, File file) {
        if (file.exists()) {
            list.addAll((Collection<? extends String[]>) JDIO.loadObject(((SimpleGUI) JDUtilities.getGUI()).getFrame(), file, true));
            Collections.sort(list, new Comparator<String[]>() {
                public int compare(String[] a, String[] b) {
                    return (a[0] + " " + a[1]).compareToIgnoreCase(b[0] + " " + b[1]);
                }
            });
        }
        JDIO.saveObject(((SimpleGUI) JDUtilities.getGUI()).getFrame(), list, file, null, null, true);
    } /*
         * private static String replace(String arg) { return
         * arg.replaceAll("&amp;nbsp;", " ").replaceAll("&#13;", "\n"); }
         * 
         * private static void formateNames(File file, File fileout) {
         * ConvertRouterData conv = new ConvertRouterData(); Vector<String[]>
         * routers = new Vector<String[]>(); Vector<String[]> routersv = new
         * Vector<String[]>(); routers.addAll((Collection<? extends String[]>)
         * Utilities.loadObject(file, true)); for (int i = 0; i <
         * routers.size(); i++) { String[] la = routers.get(i);
         * la[1]=replace(la[1]); routersv.add(la); } conv.saveTolist(routersv,
         * fileout); }
         */

    private String[] toLiveHeader(RouterData routerData) {
        ip = routerData.getRouterIP();
        cookie = "";
        int routerport = routerData.getRouterPort();
        String port = "";
        if (routerport != 80) {
            port = ":" + routerport;
        }
        if (ip == null || ip.matches("[\\s]*") || ip.matches("192.168.0.1")) {
            ip = "            Host: %%%routerip%%%" + port + "\n";
        } else {
            ip = "            Host: " + ip + port + "\r\n";
        }
        String script = "[[[HSRC]]]\n";
        String login = defaultRequest(routerData.getLoginType(), routerData.getLogin(), routerData.getLoginPostParams(), routerData.getLoginRequestProperties());
        if (!login.equals("")) {
            cookie = "            Cookie: %%%Set-Cookie%%%\r\n";
        } else {
            cookie = "            Authorization: Basic %%%basicauth%%%\r\n";
        }
        script += login;
        String disconnect = defaultRequest(routerData.getDisconnectType(), routerData.getDisconnect(), routerData.getDisconnectPostParams(), routerData.getDisconnectRequestProperties());
        if (disconnect.equals("")) { return null; }
        script += disconnect;
        script += "    [[[STEP]]][[[WAIT seconds=\"3\"/]]][[[/STEP]]]\r\n";

        script += defaultRequest(routerData.getConnectType(), routerData.getConnect(), routerData.getConnectPostParams(), routerData.getConnectRequestProperties());
        script += defaultRequest(2, routerData.getLogoff(), null, null);
        script += "[[[/HSRC]]]";
        String name = routerData.getRouterName();
        String[] pass = getUserPass(name);
        String regexp = "(?s).*" + name.replaceFirst(" .*", "").toLowerCase() + ".*";
        if (pass[0] != null && !pass[0].matches("[\\s]*")) {
            regexp = pass[0];
        }
        return new String[] { name.replaceFirst(" .*", ""), name, script, regexp, pass[1], pass[2] };
    }
}