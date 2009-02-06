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

package jd.plugins.host;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.PackageManager;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.TextAreaDialog;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.http.PostRequest;
import jd.http.Request;
import jd.nutils.JDHash;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.update.HTMLEntities;
import jd.update.PackageData;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.SnifferException;
import jd.utils.Sniffy;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Rapidshare extends PluginForHost {

    private static long LAST_FILE_CHECK = 0;

    private static final Pattern PATTERM_MATCHER_ALREADY_LOADING = Pattern.compile("(Warten Sie bitte, bis der Download abgeschlossen ist)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FIND_DOWNLOAD_POST_URL = Pattern.compile("<form name=\"dl[f]?\" action=\"(.*?)\" method=\"post\"");

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?herunterladen:.*?<p>(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_1 = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?<p.*?>(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_2 = Pattern.compile("<!-- E#[\\d]{1,2} -->(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_3 = Pattern.compile("<!-- E#[\\d]{1,2} --><p>(.*?)<\\/p>", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_MIRROR_URL = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");

    private static final Pattern PATTERN_FIND_MIRROR_URLS = Pattern.compile("<input.*?type=\"radio\" name=\"mirror\" onclick=\"document\\.dlf?\\.action=[\\\\]?'(.*?)[\\\\]?';\" /> (.*?)<br />", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FIND_PRESELECTED_SERVER = Pattern.compile("<form name=\"dlf?\" action=\"(.*?)\" method=\"post\">");

    private static final Pattern PATTERN_FIND_TICKET_WAITTIME = Pattern.compile("var c=([\\d]*?);");

    private static final String PROPERTY_INCREASE_TICKET = "INCREASE_TICKET";

    private static final String PROPERTY_SELECTED_SERVER = "SELECTED_SERVER";

    private static final String PROPERTY_SELECTED_SERVER2 = "SELECTED_SERVER#2";

    private static final String PROPERTY_SELECTED_SERVER3 = "SELECTED_SERVER#3";

    private static final String PROPERTY_USE_PRESELECTED = "USE_PRESELECTED";

    private static final String PROPERTY_USE_TELEKOMSERVER = "USE_TELEKOMSERVER";

    private static String[] serverList1;

    private static String[] serverList2;

    private static String[] serverList3;

    private static Integer loginlock = 0;

    private static HashMap<String, String> serverMap = new HashMap<String, String>();

    public static void correctURL(DownloadLink downloadLink) {
        downloadLink.setUrlDownload(Rapidshare.getCorrectedURL(downloadLink.getDownloadURL()));
    }

    /**
     * Korrigiert die URL und befreit von subdomains etc.
     * 
     * @param link
     * @return
     */
    private static String getCorrectedURL(String link) {
        if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
            link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);
        }
        String fileid = new Regex(link, "http://[\\w\\.]*?rapidshare\\.com/files/([\\d]{3,9})/?.*").getMatch(0);
        String filename = new Regex(link, "http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/?(.*)").getMatch(0);
        return "http://rapidshare.com/files/" + fileid + "/" + filename;
    }

    // private static int I = 0;

    public Rapidshare(PluginWrapper wrapper) {
        super(wrapper);
        serverMap.put("Cogent", "cg");
        serverMap.put("Cogent #2", "cg2");
        serverMap.put("Deutsche Telekom", "dt");
        serverMap.put("GlobalCrossing", "gc");
        serverMap.put("GlobalCrossing #2", "gc2");
        serverMap.put("Level(3)", "l3");
        serverMap.put("Level(3) #2", "l32");
        serverMap.put("Level(3) #3", "l33");
        serverMap.put("Level(3) #4", "l34");
        serverMap.put("Tata Com.", "tg");
        serverMap.put("Tata Com. #2", "tg2");
        serverMap.put("Teleglobe", "tg");
        serverMap.put("Teleglobe #2", "tg2");
        serverMap.put("TeliaSonera", "tl");
        serverMap.put("TeliaSonera #2", "tl2");
        serverMap.put("TeliaSonera #3", "tl3");

        serverList1 = new String[] { "cg", "cg2", "dt", "gc", "gc2", "l3", "l32", "l33", "l34", "tg", "tl", "tl2" };
        serverList2 = new String[] { "cg", "dt", "gc", "gc2", "l3", "l32", "tg", "tg2", "tl", "tl2", "tl3" };
        serverList3 = new String[] { "cg", "dt", "gc", "gc2", "l3", "l32", "l33", "l34", "tg", "tg2", "tl", "tl2" };

        setConfigElements();
        enablePremium("http://rapidshare.com/premium.html");
        this.setMaxConnections(30);

    }

    public int getTimegapBetweenConnections() {
        return 100;
    }

    /**
     * Bietet der hoster eine Möglichkeit mehrere links gleichzeitig zu prüfen,
     * kann das über diese Funktion gemacht werden.
     */
    public boolean[] checkLinks(DownloadLink[] urls) {
        try {
            if (urls == null) { return null; }
            boolean[] ret = new boolean[urls.length];
            int c = 0;
            ArrayList<Integer> sjlinks = new ArrayList<Integer>();
            while (true) {
                String post = "";
                int i = 0;
                boolean isRSCom = false;
                for (i = c; i < urls.length; i++) {
                    if (urls[i].getDownloadURL().matches("sjdp://rapidshare\\.com.*")) {
                        sjlinks.add(i);
                        ret[i] = false;
                    } else {
                        isRSCom = true;
                        if (!canHandle(urls[i].getDownloadURL())) { return null; }

                        urls[i].setUrlDownload(getCorrectedURL(urls[i].getDownloadURL()));

                        if ((post + urls[i].getDownloadURL() + "%0a").length() > 10000) {
                            break;
                        }
                        post += urls[i].getDownloadURL() + "%0a";
                    }

                }
                if (!isRSCom) return ret;
                PostRequest r = new PostRequest("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi");
                r.setPostVariable("urls", post);
                post = null;
                r.setPostVariable("toolmode", "1");
                String page = r.load();
                r = null;
                String[] lines = Regex.getLines(page);
                page = null;
                if (lines.length != i - c) {
                    lines = null;
                    System.gc();
                    return null;
                }

                for (String line : lines) {

                    String[] erg = line.split(",");
                    /*
                     * 1: Normal online -1: date nicht gefunden 3: Drect
                     * download
                     */
                    while (sjlinks.contains(c)) {
                        c++;
                    }
                    ret[c] = true;
                    if (erg.length < 6 || !erg[2].equals("1") && !erg[2].equals("3")) {
                        ret[c] = false;
                    } else {
                        urls[c].setDownloadSize(Integer.parseInt(erg[4]));
                        urls[c].setFinalFileName(erg[5].trim());
                        urls[c].setDupecheckAllowed(true);
                        if (urls[c].getDownloadSize() > 8192) {
                            /* Rapidshare html endung workaround */
                            /*
                             * man kann jeden scheiss an die korrekte url hängen
                             * und die api gibt das dann als filename zurück,
                             * doofe api
                             */
                            urls[c].setFinalFileName(erg[5].trim().replaceAll(".html", "").replaceAll(".htm", ""));
                        }
                    }
                    c++;

                }
                if (c >= urls.length) {
                    lines = null;
                    System.gc();
                    return ret;
                }
                Thread.sleep(400);
            }

        } catch (Exception e) {
            System.gc();
            e.printStackTrace();
            return null;
        }

    }

    public void handleFree(DownloadLink downloadLink) throws Exception {

        // JDProxy[] list = new JDProxy[]{new JDProxy("200.110.145.33:3128"),new
        // JDProxy("202.107.231.157:8080"),new
        // JDProxy("125.167.177.233:8080"),new JDProxy("76.107.94.147:9090"),new
        // JDProxy("218.248.33.12:3128"),new JDProxy("24.126.147.186:9090"),new
        // JDProxy("189.77.7.130:3128"),new JDProxy("118.142.8.62:3128"),new
        // JDProxy("60.191.61.162:80"),new JDProxy("207.157.9.166:9090"),new
        // JDProxy("201.54.234.11:8080"),new JDProxy("216.114.194.19:7212"),new
        // JDProxy("87.120.57.8:8080"),new JDProxy("222.239.222.40:8080"),new
        // JDProxy("85.238.102.14:3128"),new JDProxy("24.255.219.247:9090"),new
        // JDProxy("89.16.173.180:80"),new JDProxy("69.65.55.213:80"),new
        // JDProxy("72.219.18.179:9090"),new JDProxy("72.9.72.148:9090"),new
        // JDProxy("216.164.170.134:9090"),new JDProxy("71.236.126.53:9090"),new
        // JDProxy("71.205.59.3:9090"),new JDProxy("68.111.231.178:9090"),new
        // JDProxy("66.168.31.52:9090"),new JDProxy("65.190.207.153:9090"),new
        // JDProxy("62.193.246.10:6654"),new JDProxy("61.238.104.200:808"),new
        // JDProxy("82.16.112.129:9090"),new JDProxy("96.3.152.82:9090"),new
        // JDProxy("68.118.245.35:9090"),new JDProxy("76.236.53.243:9090"),new
        // JDProxy("76.123.145.161:9090"),new JDProxy("74.77.117.65:9090"),new
        // JDProxy("68.11.155.121:9090"),new JDProxy("68.104.28.45:9090"),new
        // JDProxy("24.191.241.154:9090"),new JDProxy("68.63.27.57:9090"),new
        // JDProxy("68.105.2.18:9090"),new JDProxy("67.186.21.32:9090"),new
        // JDProxy("24.20.45.101:9090"),new JDProxy("77.101.55.240:9090"),new
        // JDProxy("71.82.59.206:9090"),new JDProxy("98.150.85.174:9090"),new
        // JDProxy("75.83.57.219:9090"),new JDProxy("75.64.252.237:9090"),new
        // JDProxy("212.247.103.171:80"),new JDProxy("76.167.197.53:9090"),new
        // JDProxy("70.162.244.181:9090"),new JDProxy("69.138.47.42:9090"),new
        // JDProxy("65.190.82.22:9090"),new JDProxy("67.174.68.94:9090"),new
        // JDProxy("24.12.214.237:9090"),new JDProxy("99.253.185.5:9090"),new
        // JDProxy("99.243.108.199:9090"),new JDProxy("96.28.198.244:9090"),new
        // JDProxy("74.72.115.208:9090"),new JDProxy("70.64.250.176:9090"),new
        // JDProxy("69.242.176.42:9090"),new JDProxy("97.86.124.33:9090"),new
        // JDProxy("217.147.235.105:3128"),new
        // JDProxy("123.238.35.104:6588"),new JDProxy("203.89.182.248:8080"),new
        // JDProxy("202.155.10.131:8080"),new JDProxy("195.101.42.25:80"),new
        // JDProxy("220.224.224.66:3128"),new
        // JDProxy("209.137.151.229:8080"),new JDProxy("91.121.13.127:8080"),new
        // JDProxy("221.120.121.122:80"),new JDProxy("69.127.115.255:9090"),new
        // JDProxy("68.114.255.3:9090"),new JDProxy("68.100.221.146:9090"),new
        // JDProxy("76.123.18.157:9090"),new JDProxy("74.53.11.83:80"),new
        // JDProxy("200.138.135.12:6588"),new JDProxy("92.22.97.188:9090"),new
        // JDProxy("86.14.253.190:2301"),new JDProxy("12.205.135.91:9090"),new
        // JDProxy("75.37.214.224:9090"),new JDProxy("202.52.243.206:80"),new
        // JDProxy("85.219.198.217:8080"),new JDProxy("200.21.232.130:8080"),new
        // JDProxy("65.110.62.11:80"),new JDProxy("68.118.147.60:9090"),new
        // JDProxy("194.180.252.35:80"),new JDProxy("203.206.128.19:8080"),new
        // JDProxy("91.194.85.198:6654"),new JDProxy("69.47.165.83:9090"),new
        // JDProxy("69.14.142.185:9090"),new JDProxy("66.165.197.37:9090"),new
        // JDProxy("65.25.149.228:9090"),new JDProxy("65.190.253.101:9090"),new
        // JDProxy("24.9.22.230:9090"),new JDProxy("24.34.60.98:9090"),new
        // JDProxy("211.154.133.210:80"),new JDProxy("80.250.70.111:3128"),new
        // JDProxy("70.67.220.27:9090"),new JDProxy("24.70.56.39:9090"),new
        // JDProxy("198.164.83.28:9090"),new JDProxy("88.80.208.22:80"),new
        // JDProxy("12.183.216.12:80"),new JDProxy("212.108.250.70:8080"),new
        // JDProxy("217.151.231.34:3128"),new JDProxy("212.20.115.100:80"),new
        // JDProxy("202.29.137.145:80"),new JDProxy("203.70.96.9:80"),new
        // JDProxy("195.113.207.43:80"),new JDProxy("202.168.193.131:80"),new
        // JDProxy("89.162.237.22:80"),new JDProxy("66.253.168.169:9090"),new
        // JDProxy("75.139.62.247:9090"),new JDProxy("75.126.232.52:9090"),new
        // JDProxy("195.180.11.231:8080"),new JDProxy("189.19.10.115:3128"),new
        // JDProxy("74.142.142.57:9090"),new JDProxy("72.205.59.135:9090"),new
        // JDProxy("201.210.49.219:3128"),new
        // JDProxy("222.123.215.201:3128"),new
        // JDProxy("220.225.245.229:3128"),new
        // JDProxy("200.110.130.129:3128"),new JDProxy("98.197.249.24:9090"),new
        // JDProxy("69.22.123.154:9090"),new JDProxy("24.230.182.225:9090"),new
        // JDProxy("24.224.230.126:9090"),new JDProxy("68.225.96.18:9090"),new
        // JDProxy("71.199.135.108:9090"),new JDProxy("89.248.239.59:3128"),new
        // JDProxy("89.188.238.98:8080"),new JDProxy("203.109.125.252:6588"),new
        // JDProxy("90.157.37.216:8000"),new JDProxy("202.32.9.157:80"),new
        // JDProxy("190.199.249.254:8080"),new JDProxy("59.120.164.148:80"),new
        // JDProxy("76.170.85.232:9090"),new JDProxy("76.106.234.173:9090"),new
        // JDProxy("69.161.78.160:9090"),new JDProxy("66.31.108.157:9090"),new
        // JDProxy("71.9.6.233:9090"),new JDProxy("24.78.86.89:9090"),new
        // JDProxy("67.182.84.133:9090"),new JDProxy("66.67.106.227:9090"),new
        // JDProxy("201.52.73.13:6588"),new JDProxy("82.22.138.43:9090"),new
        // JDProxy("75.85.136.141:9090"),new JDProxy("69.118.237.19:9090"),new
        // JDProxy("24.25.164.214:9090"),new JDProxy("189.54.169.42:6588"),new
        // JDProxy("24.8.14.155:9090"),new JDProxy("66.196.86.219:80"),new
        // JDProxy("12.130.107.115:80"),new JDProxy("75.132.25.184:9090"),new
        // JDProxy("96.28.160.240:9090"),new JDProxy("66.167.228.62:9090"),new
        // JDProxy("24.34.160.245:9090"),new JDProxy("24.14.112.139:9090"),new
        // JDProxy("24.125.64.236:9090"),new JDProxy("69.15.128.20:80"),new
        // JDProxy("201.229.208.2:80"),new JDProxy("66.145.194.101:80"),new
        // JDProxy("98.192.124.144:9090"),new JDProxy("98.192.114.62:9090"),new
        // JDProxy("62.49.119.178:8080"),new JDProxy("60.217.227.211:808"),new
        // JDProxy("24.129.120.19:8090"),new JDProxy("222.171.28.246:808"),new
        // JDProxy("222.141.136.61:8080"),new JDProxy("201.80.62.254:6588"),new
        // JDProxy("200.163.7.182:8080"),new JDProxy("212.24.237.49:8080"),new
        // JDProxy("96.28.116.40:9090"),new JDProxy("92.23.152.189:9090"),new
        // JDProxy("88.165.169.130:9090"),new JDProxy("84.193.238.116:9090"),new
        // JDProxy("82.234.51.250:9090"),new JDProxy("81.237.233.253:9090"),new
        // JDProxy("77.97.55.155:9090"),new JDProxy("76.182.53.239:9090"),new
        // JDProxy("76.127.163.18:9090"),new JDProxy("76.116.89.184:9090"),new
        // JDProxy("75.39.132.200:9090"),new JDProxy("75.147.125.182:9090"),new
        // JDProxy("71.237.98.13:9090"),new JDProxy("71.194.0.41:9090"),new
        // JDProxy("70.237.142.249:9090"),new JDProxy("69.122.222.90:9090"),new
        // JDProxy("69.119.243.192:9090"),new JDProxy("68.60.189.199:9090"),new
        // JDProxy("68.57.62.200:9090"),new JDProxy("68.52.143.160:9090"),new
        // JDProxy("24.79.173.214:9090"),new JDProxy("24.238.197.103:9090"),new
        // JDProxy("24.117.213.220:9090"),new JDProxy("204.210.116.65:9090"),new
        // JDProxy("85.105.144.28:8088"),new JDProxy("79.133.224.249:80"),new
        // JDProxy("66.61.69.94:9090"),new JDProxy("128.42.197.35:49400"),new
        // JDProxy("74.206.198.14:3128"),new JDProxy("202.102.75.230:80"),new
        // JDProxy("210.245.52.192:8088")};
        //       
        //       
        // I++;
        // if(I>=list.length)I=0;
        //       
        // br.setProxy(list[I]);
        // System.out.println("Proxy: "+list[I]);
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
            if (Sniffy.hasSniffer()) throw new SnifferException();
        }
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // if (ddl)this.doPremium(downloadLink);
        Rapidshare.correctURL(downloadLink);

        // if (getRemainingWaittime() > 0) { return
        // handleDownloadLimit(downloadLink); }
        String freeOrPremiumSelectPostURL = null;

        br.setAcceptLanguage(ACCEPT_LANGUAGE);
        br.setFollowRedirects(false);

        String link = downloadLink.getDownloadURL();

        // RS URL wird aufgerufen
        // req = new GetRequest(link);
        // req.load();
        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            logger.info("Direct Download for Free Users");
            this.handlePremium(downloadLink, new Account("dummy", "dummy"));
            return;
        }
        // posturl für auswahl free7premium wird gesucht
        freeOrPremiumSelectPostURL = new Regex(br, PATTERN_FIND_MIRROR_URL).getMatch(0);
        // Fehlerbehandlung auf der ersten Seite
        if (freeOrPremiumSelectPostURL == null) {
            String error = null;
            if ((error = findError(br + "")) != null) { throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error)); }
            reportUnknownError(br, 1);
            logger.warning("could not get newURL");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }

        // Post um freedownload auszuwählen
        Form[] forms = br.getForms();

        br.submitForm(forms[0]);
        // PostRequest pReq = new PostRequest(freeOrPremiumSelectPostURL);
        // pReq.setPostVariable("dl.start", "free");
        // pReq.load();
        String error = null;

        if ((error = findError(br + "")) != null) {
            if (Regex.matches(error, Pattern.compile("(als 200 Megabyte)"))) throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugin.rapidshare.error.filetolarge", "This file is larger than 200 MB, you need a premium-account to download this file."));
            if (Regex.matches(error, Pattern.compile("(weder einem Premiumaccount)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("(keine freien Slots)"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "All free slots in use", 120000l); }
            if (Regex.matches(error, Pattern.compile("(in 2 Minuten)"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many users are currently downloading this file", 120 * 1000l); }
            if (Regex.matches(error, Pattern.compile("(Die Datei konnte nicht gefunden werden)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            if (Regex.matches(error, Pattern.compile("Der Server .*? ist momentan nicht verf.*"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.LF("plugin.rapidshare.error.serverunavailable", "The Server %s is currently unavailable.", error.substring(11, error.indexOf(" ist"))), 3600 * 1000l); }
            if (Regex.matches(error, PATTERM_MATCHER_ALREADY_LOADING)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Already a download from your ip in progress!", 120 * 1000l); }
            // für java 1.5
            if (new Regex(error, "(kostenlose Nutzung erreicht)|(.*download.{0,3}limit.{1,50}free.{0,3}users.*)").matches()) {

                String waitfor = new Regex(br, "es in ca\\.(.*?)Minuten wieder").getMatch(0);
                if (waitfor == null) {
                    waitfor = new Regex(br, "Or try again in about(.*?)minutes").getMatch(0);

                }
                long waitTime = 60 * 60 * 1000l;
                try {
                    waitTime = new Long(waitfor.trim()) * 60 * 1000l;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
            }
            reportUnknownError(br, 2);
            throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
        }
        // Ticketwartezeit wird gesucht
        String ticketTime = new Regex(br, PATTERN_FIND_TICKET_WAITTIME).getMatch(0);
        if (ticketTime != null && ticketTime.equals("0")) {
            ticketTime = null;
        }

        String ticketCode = br + "";

        String tt = new Regex(ticketCode, "var tt =(.*?)document\\.getElementById\\(\"dl\"\\)\\.innerHTML").getMatch(0);

        String fun = "function f(){ return " + tt + "} f()";
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();

        // Collect the arguments into a single string.

        // Now evaluate the string we've colected.
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);

        // Convert the result to a string and print it.
        String code = Context.toString(result);
        if (tt != null) ticketCode = code;
        Context.exit();
        if (ticketCode.contains("Leider sind derzeit keine freien Slots ")) {
            downloadLink.getLinkStatus().setStatusText("All free slots in use: try to download again after 2 minutes");
            logger.warning("All free slots in use: try to download again after 2 minutes");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "All free slots in use", 120000);
        }
        if (new Regex(ticketCode, ".*download.{0,3}limit.{1,50}free.{0,3}users.*").matches()) {
            String waitfor = new Regex(ticketCode, "Or try again in about(.*?)minutes").getMatch(0);
            long waitTime = 60 * 60 * 1000l;
            try {
                waitTime = new Long(waitfor.trim()) * 60 * 1000l;
            } catch (Exception e) {
                e.printStackTrace();
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);

        }
        long pendingTime = 0;
        if (ticketTime != null) {
            pendingTime = Long.parseLong(ticketTime);

            if (getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) > 0) {
                logger.warning("Waittime increased by JD: " + pendingTime + " --> " + (pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100));
                pendingTime = pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100;
            }
            pendingTime *= 1000;
        }

        waitTicketTime(downloadLink, pendingTime);

        String postTarget = getDownloadTarget(downloadLink, ticketCode);
        System.out.println(postTarget);
        // Falls Serverauswahl fehlerhaft war
        if (linkStatus.isFailed()) return;

        Request request = br.createPostRequest(postTarget, "mirror=on&x=" + Math.random() * 40 + "&y=" + Math.random() * 40);

        // Download
        dl = new RAFDownload(this, downloadLink, request);
        long startTime = System.currentTimeMillis();
        HTTPConnection con = dl.connect();
        if (!con.isContentDisposition() && con.getHeaderField("Cache-Control") != null) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        downloadLink.setProperty("REQUEST_TIME", (System.currentTimeMillis() - startTime));
        dl.startDownload();
        downloadLink.setProperty("DOWNLOAD_TIME", (System.currentTimeMillis() - startTime));
        int dif = (int) ((System.currentTimeMillis() - startTime) / 1000);
        if (dif > 0) downloadLink.setProperty("DOWNLOAD_SPEED", (downloadLink.getDownloadSize() / dif) / 1024);
        if (downloadLink.getStringProperty("USE_SERVER") != null) {
            new File(downloadLink.getFileOutput()).delete();
            downloadLink.getLinkStatus().setStatusText(" | SRV: " + downloadLink.getStringProperty("USE_SERVER") + " Speed: " + downloadLink.getProperty("DOWNLOAD_SPEED") + " kb/s");

            ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            String msg = "";
            for (DownloadLink dLink : downloadLink.getFilePackage().getDownloadLinks()) {
                if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    ret.add(dLink);

                    msg += "Server: " + dLink.getStringProperty("USE_SERVER") + " : Speed: " + dLink.getProperty("DOWNLOAD_SPEED") + " kb/s\r\n";
                } else if (dLink.getLinkStatus().isFailed()) {
                    ret.add(dLink);

                    msg += "Server: " + dLink.getStringProperty("USE_SERVER") + " not available\r\n";
                } else {
                    return;
                }
            }
            TextAreaDialog.showDialog(SimpleGUI.CURRENTGUI.getFrame(), "Speedtest result", "Your speedtest results", msg);
        }
    }

    /**
     * premiumdownload Methode
     * 
     * @param step
     * @param downloadLink
     * @return
     */
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) {
            ((PluginForHost) PluginWrapper.getNewInstance("jd.plugins.host.Serienjunkies")).handleFree(downloadLink);
            return;
        }
        if (downloadLink.getLinkType() == DownloadLink.LINKTYPE_CONTAINER) {
            if (Sniffy.hasSniffer()) throw new SnifferException();
        }
        String freeOrPremiumSelectPostURL = null;
        Request request = null;
        String error = null;
        long startTime = System.currentTimeMillis();
        Rapidshare.correctURL(downloadLink);
        br = login(account, true);
        br.setFollowRedirects(false);
        br.setAcceptLanguage(ACCEPT_LANGUAGE);
        br.getPage(downloadLink.getDownloadURL());
        String directurl = br.getRedirectLocation();
        if (directurl == null) {
            logger.finest("InDirect-Download: Server-Selection available!");
            if (account.getStringProperty("premcookie", null) == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
            if ((error = findError(br.toString())) != null) {
                logger.warning(error);
                if (Regex.matches(error, Pattern.compile("(Ihr Cookie wurde nicht erkannt)"))) {
                    account.setProperty("premcookie", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (Regex.matches(error, Pattern.compile("(weder einem Premiumaccount)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                if (Regex.matches(error, Pattern.compile("(in 2 Minuten)"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many users are currently downloading this file", 120 * 1000l); }
                if (Regex.matches(error, Pattern.compile("(Die Datei konnte nicht gefunden werden)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                if (Regex.matches(error, Pattern.compile("(Betrugserkennung)"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.fraud", "Fraud detected: This Account has been illegally used by several users."), LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
                if (Regex.matches(error, Pattern.compile("(expired|abgelaufen)"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("(You have exceeded the download limit|Sie haben heute das Limit überschritten)"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.limitexeeded", "You have exceeded the download limit."), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("Passwort ist falsch"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("IP"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("Der Server .*? ist momentan nicht verf.*"))) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.LF("plugin.rapidshare.error.serverunavailable", "The Server %s is currently unavailable.", error.substring(11, error.indexOf(" ist"))), 3600 * 1000l);
                } else if (Regex.matches(error, Pattern.compile("(Account wurde nicht gefunden|Your Premium Account has not been found)"))) {
                    account.setProperty("premcookie", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.accountnotfound", "Your Premium Account has not been found."), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    account.setProperty("premcookie", null);
                    throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
                }
            }

            // posturl für auswahl wird gesucht
            freeOrPremiumSelectPostURL = new Regex(br, PATTERN_FIND_MIRROR_URL).getMatch(0);
            // Fehlerbehandlung auf der ersten Seite
            if (freeOrPremiumSelectPostURL == null) {
                if ((error = findError(br + "")) != null) { throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error)); }
                reportUnknownError(br, 1);
                logger.warning("could not get newURL");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            // Post um Premium auszuwählen
            Form[] forms = br.getForms();
            br.submitForm(forms[1]);
            String postTarget = getDownloadTarget(downloadLink, br.toString());
            if (postTarget == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            request = br.createGetRequest(postTarget);
        } else {
            logger.finest("Direct-Download: Server-Selection not available!");
            request = br.createGetRequest(directurl);
        }

        // Download
        dl = new RAFDownload(this, downloadLink, request);
        // Premiumdownloads sind resumefähig
        dl.setResume(true);
        // Premiumdownloads erlauben chunkload
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        HTTPConnection urlConnection;
        try {
            urlConnection = dl.connect(br);
        } catch (Exception e) {
            br.setRequest(request);
            request = br.createGetRequest(null);
            logger.info("Load from " + request.getUrl().toString().substring(0, 35));
            // Download
            dl = new RAFDownload(this, downloadLink, request);
            // Premiumdownloads sind resumefähig
            dl.setResume(true);
            // Premiumdownloads erlauben chunkload
            dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

            urlConnection = dl.connect(br);
        }
        // Download starten
        // prüft ob ein content disposition header geschickt wurde. Falls nicht,
        // ist es eintweder eine Bilddatei oder eine Fehlerseite. BIldfiles
        // haben keinen Cache-Control Header
        if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
            // Lädt die zuletzt aufgebaute vernindung
            br.setRequest(request);
            br.followConnection();

            // Fehlerbehanldung
            /*
             * Achtung! keine Parsing arbeiten an diesem String!!!
             */
            if ((error = findError(br.toString())) != null) {
                logger.warning(error);
                if (Regex.matches(error, Pattern.compile("(Ihr Cookie wurde nicht erkannt)"))) {
                    account.setProperty("premcookie", null);
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
                if (Regex.matches(error, Pattern.compile("(weder einem Premiumaccount)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                if (Regex.matches(error, Pattern.compile("(in 2 Minuten)"))) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Too many users are currently downloading this file", 120 * 1000l); }
                if (Regex.matches(error, Pattern.compile("(Die Datei konnte nicht gefunden werden)"))) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                if (Regex.matches(error, Pattern.compile("(Betrugserkennung)"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.fraud", "Fraud detected: This Account has been illegally used by several users."), LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
                if (Regex.matches(error, Pattern.compile("(expired|abgelaufen)"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("(You have exceeded the download limit|Sie haben heute das Limit überschritten)"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.limitexeeded", "You have exceeded the download limit."), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("Passwort ist falsch"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("IP"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, dynTranslate(error), LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (Regex.matches(error, Pattern.compile("(Account wurde nicht gefunden|Your Premium Account has not been found)"))) {
                    account.setProperty("premcookie", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, JDLocale.L("plugin.rapidshare.error.accountnotfound", "Your Premium Account has not been found."), LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    account.setProperty("premcookie", null);
                    throw new PluginException(LinkStatus.ERROR_FATAL, dynTranslate(error));
                }
            } else {
                reportUnknownError(br.toString(), 6);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }

        }

        downloadLink.setProperty("REQUEST_TIME", (System.currentTimeMillis() - startTime));
        dl.startDownload();
        downloadLink.setProperty("DOWNLOAD_TIME", (System.currentTimeMillis() - startTime));
        int dif = (int) ((System.currentTimeMillis() - startTime) / 1000);
        if (dif > 0) downloadLink.setProperty("DOWNLOAD_SPEED", (downloadLink.getDownloadSize() / dif) / 1024);
        if (downloadLink.getStringProperty("USE_SERVER") != null) {
            new File(downloadLink.getFileOutput()).delete();
            downloadLink.getLinkStatus().setStatusText(" | SRV: " + downloadLink.getStringProperty("USE_SERVER") + " Speed: " + downloadLink.getProperty("DOWNLOAD_SPEED") + " kb/s");

            ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            String msg = "";
            for (DownloadLink dLink : downloadLink.getFilePackage().getDownloadLinks()) {
                if (dLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    ret.add(dLink);

                    msg += "Server: " + dLink.getStringProperty("USE_SERVER") + " : Speed: " + dLink.getProperty("DOWNLOAD_SPEED") + " kb/s\r\n";
                } else if (dLink.getLinkStatus().isFailed()) {
                    ret.add(dLink);

                    msg += "Server: " + dLink.getStringProperty("USE_SERVER") + " not available\r\n";
                } else {
                    return;
                }
            }
            TextAreaDialog.showDialog(SimpleGUI.CURRENTGUI.getFrame(), "Speedtest result", "Your speedtest results", msg);
        }

    }

    private String findError(String string) {
        String error = null;
        error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE).getMatch(0);

        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_3).getMatch(0);
        }
        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_2).getMatch(0);
        }
        if (error == null || error.trim().length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_1).getMatch(0);
        }

        error = Encoding.htmlDecode(error);
        String[] er = Regex.getLines(error);

        if (er == null || er.length == 0) { return null; }
        er[0] = HTMLEntities.unhtmlentities(er[0]);
        if (er[0] == null || er[0].length() == 0) { return null; }
        return er[0];

    }

    private String dynTranslate(String error) {
        String error2 = JDLocale.L("plugins.host.rapidshare.errors." + JDHash.getMD5(error) + "", error);
        if (error.equals(error2)) {
            logger.warning("NO TRANSLATIONKEY FOUND FOR: " + error + "(" + JDHash.getMD5(error) + ")");
        }
        return error2;
    }

    public String getAGBLink() {
        return "http://rapidshare.com/faq.html";
    }

    /**
     * Sucht im ticketcode nach der entgültigen DownloadURL Diese Downlaodurl
     * beinhaltet in ihrer Subdomain den zielserver. Durch Anpassung dieses
     * Zielservers kann also die Serverauswahl vorgenommen werden.
     * 
     * @param step
     * @param downloadLink
     * @param ticketCode
     * @return
     * @throws PluginException
     */
    private String getDownloadTarget(DownloadLink downloadLink, String ticketCode) throws PluginException {

        String postTarget = new Regex(ticketCode, PATTERN_FIND_DOWNLOAD_POST_URL).getMatch(0);

        String server1 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER, "Level(3)");
        String server2 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER2, "TeliaSonera");
        String server3 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER3, "TeliaSonera");
        boolean serverTest = false;
        if (downloadLink.getProperty("USE_SERVER") != null) {
            serverTest = true;
            server1 = server2 = server3 = downloadLink.getStringProperty("USE_SERVER");
            logger.finer("Speedtest detected. use Server: " + server1);

        }

        String serverAbb = serverMap.get(server1);
        String server2Abb = serverMap.get(server2);
        String server3Abb = serverMap.get(server3);
        if (serverAbb == null) {
            serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
            logger.finer("Use Random #1 server " + serverAbb);
        }
        if (server2Abb == null) {
            server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
            logger.finer("Use Random #2 server " + server2Abb);
        }
        if (server3Abb == null) {
            server3Abb = serverList3[(int) (Math.random() * (serverList3.length - 1))];
            logger.finer("Use Random #3 server " + server3Abb);
        }
        // String endServerAbb = "";
        boolean telekom = getPluginConfig().getBooleanProperty(PROPERTY_USE_TELEKOMSERVER, false);
        boolean preselected = getPluginConfig().getBooleanProperty(PROPERTY_USE_PRESELECTED, true);

        if (postTarget == null) {
            logger.severe("postTarget not found:");
            reportUnknownError(ticketCode, 4);
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_RETRY);
            return null;
        }
        String[] serverstrings = new Regex(ticketCode, PATTERN_FIND_MIRROR_URLS).getColumn(0);
        logger.info("wished Mirror #1 Server " + serverAbb);
        logger.info("wished Mirror #2 Server " + server2Abb);
        logger.info("wished Mirror #3 Server " + server3Abb);
        String selected = new Regex(ticketCode, PATTERN_FIND_PRESELECTED_SERVER).getMatch(0);
        logger.info("Preselected Server: " + selected.substring(0, 30));
        if (preselected && !serverTest) {
            logger.info("RS.com Use preselected : " + selected.substring(0, 30));
            postTarget = selected;
        } else if (!serverTest && telekom && ticketCode.indexOf("td.rapidshare.com") >= 0) {
            logger.info("RS.com Use Telekom Server");
            postTarget = getURL(serverstrings, "Deutsche Telekom", postTarget);
        } else if (ticketCode.indexOf(serverAbb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #1 Server: " + getServerName(serverAbb));
            postTarget = getURL(serverstrings, getServerName(serverAbb), postTarget);
        } else if (ticketCode.indexOf(server2Abb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #2 Server: " + getServerName(server2Abb));
            postTarget = getURL(serverstrings, getServerName(server2Abb), postTarget);
        } else if (ticketCode.indexOf(server3Abb + ".rapidshare.com") >= 0) {
            logger.info("RS.com Use Mirror #3 Server: " + getServerName(server3Abb));
            postTarget = getURL(serverstrings, getServerName(server3Abb), postTarget);
        } else if (serverstrings.length > 0) {
            logger.severe("Kein Server gefunden 1");
            if (serverTest) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, " Server not available");
        } else {
            logger.severe("Kein Server gefunden 2");
            if (serverTest) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, " Server not available");
        }

        return postTarget;
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        if (downloadLink.getDownloadURL().matches("sjdp://.*")) return false;

        if (System.currentTimeMillis() - LAST_FILE_CHECK < 250) {
            try {
                Thread.sleep(System.currentTimeMillis() - LAST_FILE_CHECK);
            } catch (InterruptedException e) {
            }
        }
        Rapidshare.correctURL(downloadLink);
        LAST_FILE_CHECK = System.currentTimeMillis();

        String[] erg = br.getPage("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi?urls=" + downloadLink.getDownloadURL() + "&toolmode=1").trim().split(",");
        /*
         * 1: Normal online -1: date nicht gefunden 3: Drect download
         */
        if (erg.length < 6 || !erg[2].equals("1") && !erg[2].equals("3")) { return false; }

        downloadLink.setFinalFileName(erg[5]);
        downloadLink.setDownloadSize(Integer.parseInt(erg[4]));
        downloadLink.setDupecheckAllowed(true);
        if (downloadLink.getDownloadSize() > 8192) {
            /* Rapidshare html endung workaround */
            /*
             * man kann jeden scheiss an die korrekte url hängen und die api
             * gibt das dann als filename zurück, doofe api
             */
            downloadLink.setFinalFileName(erg[5].trim().replaceAll(".html", "").replaceAll(".htm", ""));
        }

        return true;
    }

    private String getServerName(String id) {
        Iterator<Entry<String, String>> it = serverMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> next = it.next();
            if (next.getValue().equalsIgnoreCase(id)) { return next.getKey(); }
        }
        return null;
    }

    private String getURL(String[] serverstrings, String selected, String postTarget) {
        if (!serverMap.containsKey(selected.trim())) {
            logger.severe("Unknown Servername: " + selected);
            return postTarget;
        }
        String abb = serverMap.get(selected.trim());

        for (String url : serverstrings) {
            if (url.contains(abb + ".rapidshare.com")) {
                logger.info("Load from " + selected + "(" + abb + ")");
                return url;
            }
        }

        logger.warning("No Serverstring found for " + abb + "(" + selected + ")");
        return postTarget;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    private void reportUnknownError(Object req, int id) {
        logger.severe("Unknown error(" + id + "). please add this htmlcode to your bugreport:\r\n" + req);
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert nachprüfen */
        return 1;
    }

    public void reset() {
    }

    /**
     * Erzeugt den Configcontainer für die Gui
     */
    private void setConfigElements() {

        Vector<String> m1 = new Vector<String>();
        Vector<String> m2 = new Vector<String>();
        Vector<String> m3 = new Vector<String>();
        for (String element : serverList1) {
            m1.add(getServerName(element));
        }
        for (String element : serverList2) {
            m2.add(getServerName(element));
        }
        for (String element : serverList3) {
            m3.add(getServerName(element));
        }
        m1.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        m2.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        m3.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer", "Bevorzugte Server")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER, m1.toArray(new String[] {}), "#1").setDefaultValue("Level(3)"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER2, m2.toArray(new String[] {}), "#2").setDefaultValue("TeliaSonera"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER3, m3.toArray(new String[] {}), "#3").setDefaultValue("TeliaSonera"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ArrayList<PackageData> all = new PackageManager().getPackageData();
                PackageData dat = all.get((int) (Math.random() * (all.size() - 1)));
                String url = dat.getStringProperty("url");
                String link = JDUtilities.getGUI().showUserInputDialog(JDLocale.L("plugins.host.rapidshare.speedtest.link", "Enter a Rapidshare.com Link"), url);
                if (link == null) return;
                if (!canHandle(link)) {
                    link = url;
                }
                FilePackage fp = new FilePackage();
                fp.setName("RS Speedtest");
                for (Iterator<Entry<String, String>> it = serverMap.entrySet().iterator(); it.hasNext();) {
                    Entry<String, String> n = it.next();
                    DownloadLink dlink = new DownloadLink((PluginForHost) getWrapper().getNewPluginInstance(), link.substring(link.lastIndexOf("/") + 1), getHost(), link, true);
                    dlink.setProperty("USE_SERVER", n.getKey());
                    dlink.setProperty("ALLOW_DUPE", true);
                    dlink.setFinalFileName("Speedtest_svr_" + n.getKey() + ".test");
                    dlink.setFilePackage(fp);
                    dlink.getLinkStatus().setStatusText("Server: " + n.getKey());

                }
                JDUtilities.getController().addPackageAt(fp, 0);
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED, null));

            }

        }, JDLocale.L("plugins.host.rapidshare.speedtest", "SpeedTest")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_TELEKOMSERVER, JDLocale.L("plugins.hoster.rapidshare.com.telekom", "Telekom Server verwenden falls verfügbar")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_PRESELECTED, JDLocale.L("plugins.hoster.rapidshare.com.preSelection", "Vorauswahl übernehmen")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_INCREASE_TICKET, JDLocale.L("plugins.hoster.rapidshare.com.increaseTicketTime", "Ticketwartezeit verlängern (0%-500%)"), 0, 500).setDefaultValue(0).setStep(1));
    }

    public Browser login(Account account, boolean usesavedcookie) throws IOException, PluginException {
        synchronized (loginlock) {
            Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.clearCookies(this.getHost());
            String cookie = account.getStringProperty("premcookie", null);
            if (usesavedcookie && cookie != null) {
                br.setCookie("http://rapidshare.com", "user", cookie);
                logger.finer("Cookie Login");
                return br;
            }
            logger.finer("HTTPS Login");
            br.setAcceptLanguage("en, en-gb;q=0.8");
            br.getPage("https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi?login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            cookie = br.getCookie("http://rapidshare.com", "user");
            account.setProperty("premcookie", cookie);
            return br;
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        br = login(account, false);
        if (account.getStringProperty("premcookie", null) == null || account.getUser().equals("") || account.getPass().equals("") || br.getRegex("(wurde nicht gefunden|Your Premium Account has not been found)").matches() || br.getRegex("but the password is incorrect").matches() || br.getRegex("Fraud detected, Account").matches()) {

            String error = findError("" + br);
            if (error != null) {
                if (error.contains("Fraud")) {
                    ai.setStatus(JDLocale.L("plugin.rapidshare.error.fraud", "Fraud detected: This Account has been illegally used by several users."));
                } else {
                    ai.setStatus(this.dynTranslate(error));
                }
            }
            ai.setValid(false);
            account.setProperty("premcookie", null);
            return ai;
        }

        String validUntil = br.getRegex("<td>(Expiration date|G\\&uuml\\;ltig bis)\\:</td><td style=.*?><b>(.*?)</b></td>").getMatch(1).trim();

        String trafficLeft = br.getRegex("<td>(Traffic left:|Traffic &uuml;brig:)</td><td align=right><b><script>document\\.write\\(setzeTT\\(\"\"\\+Math\\.ceil\\(([\\d]*?)\\/1000\\)\\)\\)\\;<\\/script> MB<\\/b><\\/td>").getMatch(1);
        String files = br.getRegex("<td>(Files:|Dateien:)</td><td.*?><b>(.*?)</b></td>").getMatch(1).trim();
        String rapidPoints = br.getRegex("<td>RapidPoints:</td><td.*?><b>(.*?)</b></td>").getMatch(0).trim();
        String newRapidPoints = br.getRegex(">RapidPoints PU</a>:</td><td.*?><b>(.*?)</b></td>").getMatch(0).trim();
        String usedSpace = br.getRegex("<td>(Used storage:|Belegter Speicher:)</td><td.*?><b>(.*?)</b></td>").getMatch(1).trim();
        String trafficShareLeft = br.getRegex("<td>(TrafficShare left:|TrafficShare &uuml;brig:)</td><td.*?><b>(.*?)</b></td>").getMatch(1).trim();
        ai.setTrafficLeft(Regex.getSize(trafficLeft + " Mb") / 1000);
        ai.setTrafficMax(10 * 1024 * 1024 * 1024l);
        ai.setFilesNum(Integer.parseInt(files));
        ai.setPremiumPoints(Integer.parseInt(rapidPoints));
        ai.setNewPremiumPoints(Integer.parseInt(newRapidPoints));
        ai.setUsedSpace(Regex.getSize(usedSpace));
        ai.setTrafficShareLeft(Regex.getSize(trafficShareLeft));
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd. MMM yyyy", Locale.UK);

        try {
            Date date = dateFormat.parse(validUntil);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            try {
                dateFormat = new SimpleDateFormat("EEE, dd. MMM yyyy");
                Date date = dateFormat.parse(validUntil);
                ai.setValidUntil(date.getTime());
                e.printStackTrace();
            } catch (ParseException e2) {
                return null;
            }

        }

        if (br.containsHTML("expired") && br.containsHTML("if (1)")) {
            ai.setExpired(true);
            account.setProperty("premcookie", null);
        }

        return ai;
    }

    /**
     * Wartet die angegebene Ticketzeit ab
     * 
     * @param step
     * @param downloadLink
     * @param pendingTime
     * @throws InterruptedException
     */
    private void waitTicketTime(DownloadLink downloadLink, long pendingTime) throws InterruptedException {

        while (pendingTime > 0 && !downloadLink.isAborted()) {
            downloadLink.getLinkStatus().setStatusText(String.format(JDLocale.L("plugin.rapidshare.tickettime", "Wait %s for ticket"), JDUtilities.formatSeconds((int) (pendingTime / 1000))));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);
            pendingTime -= 1000;
        }
    }
}
