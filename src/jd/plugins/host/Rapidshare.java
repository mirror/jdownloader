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

package jd.plugins.host;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import jd.captcha.CES;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.controlling.ProgressController;
import jd.http.GetRequest;
import jd.http.HeadRequest;
import jd.http.PostRequest;
import jd.http.Request;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.CESClient;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Rapidshare extends PluginForHost {
    static private final String host = "rapidshare.com";

    private String version = "1.3.0.1";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_FIND_MIRROR_URL = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");
    // <center><table><tr><td><img
    // src="http://rs235.rapidshare.com/access1216288.gif"></td>

    //    
    // '<form name="dlf"
    // action="http://rs235gc2.rapidshare.com/files/123613963/1216288/webinterface8.jdu"
    // method="post">' +
    // '<center><table><tr><td><img
    // src="http://rs235.rapidshare.com/access1216288.gif"></td>' +

    private static final Pattern PATTERN_FIND_CAPTCHA_IMAGE_URL = Pattern.compile("<center><table><tr><td><img src\\=\"(.*?)\"></td>");

    private static final Pattern PATTERN_FIND_DOWNLOAD_POST_URL = Pattern.compile("<form name=\"dl[f]?\" action=\"(.*?)\" method=\"post\"");

    private static final Pattern PATTERN_MATCHER_CAPTCHA_WRONG = Pattern.compile("(wrong [acces ]*?code|Zugriffscode)");
    private static final Pattern PATTERM_MATCHER_ALREADY_LOADING = Pattern.compile("(bereits eine Datei runter)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_MATCHER_HAPPY_HOUR = Pattern.compile("(Happy hour)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FIND_DOWNLOAD_LIMIT_WAITTIME = Pattern.compile("Alternativ k&ouml;nnen Sie ([\\d]{1,4}) Minuten warten.", Pattern.CASE_INSENSITIVE);
    // <form name="dl"
    // action="http://rs363cg.rapidshare.com/files/119944363/814136/NG_-_001_-_TaN.part2.rar"
    // method="post">
    private static final Pattern PATTERN_FIND_PRESELECTED_SERVER = Pattern.compile("<form name=\"dlf?\" action=\"(.*?)\" method=\"post\">");
    private static final Pattern PATTERN_FIND_MIRROR_URLS = Pattern.compile("<input.*?type=\"radio\" name=\"mirror\" onclick=\"document.dlf?.action=\\\\'(.*)\\\\';\" /> (.*?)<br />'");
    private static final Pattern PATTERN_FIND_TICKET_WAITTIME = Pattern.compile("var c=([\\d]*?);");
    // private static final Pattern PATTERN_MATCHER_ACCOUNT_EXPIRED=
    // Pattern.compile("(Dieses Konto ist am)");
    private static final Pattern PATTERN_MATCHER_BOT = Pattern.compile("(Too many wrong codes)");

    private static final Pattern PATTERN_MATCHER_DOWNLOAD_ERRORPAGE = Pattern.compile("(RapidShare)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FIND_ERROR_CODES = Pattern.compile("<!-- E#([\\d]{1,3}) -->(.*?)div");
    private static final Pattern PATTERN_MATCHER_FIND_ERROR = Pattern.compile("(<h1>Fehler</h1>)");

    private static final Pattern PATTERN_MATCHER_TOO_MANY_USERS = Pattern.compile("(Bitte versuchen Sie es in 2 Minuten)");
    private static final Pattern PATTERN_FIND_ERROR_MESSAGE = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?folgende Datei herunterladen:.*?<p>(.*?)<", Pattern.DOTALL);
    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_2 = Pattern.compile("<!-- E#[\\d]{1,2} -->(.*?)<", Pattern.DOTALL);
    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_3 = Pattern.compile("<!-- E#[\\d]{1,2} --><p>(.*?)<\\/p>", Pattern.DOTALL);
    // <!-- E#7 --><p>Der Server 162.rapidshare.com ist momentan nicht
    // verf&uuml;gbar. Wir arbeiten an der Fehlerbehebung.</p>

    /**
     * s Das DownloadLimit wurde erreicht (?s)Downloadlimit.*Oder warte ([0-9]+)
     */

    // private Pattern patternErrorCaptchaWrong = Pattern.compile("(zugriffscode
    // falsch|code wrong)", Pattern.CASE_INSENSITIVE);
    // private Pattern patternErrorFileAbused = Pattern.compile("(darf nicht
    // verteilt werden|forbidden to be shared)", Pattern.CASE_INSENSITIVE);
    //
    // private Pattern patternErrorFileNotFound = Pattern.compile("(datei nicht
    // gefunden|file not found)", Pattern.CASE_INSENSITIVE);
    // private String patternForSelectedServer = "<input checked
    // °actionstring.value=°>°<br>";
    // private String patternForServer = "<input° type=\"radio\" name=\"°\"
    // onclick=\"document.dl.action=°http://°/files/°;document.dl.actionstring.value=°\">
    // °<br>";
    // private String ticketWaitTimepattern = "var c=°;";
    // private String ticketCodePattern = "unescape('°')}";
    // <!-- E#8 --><p>Dieses Konto ist am Mon, 2. Jun 2008 abgelaufen.
    // Verl&auml;ngern Sie jetzt Ihren Account und nutzen profitieren Sie
    // weiterhin von den Vorteilen der Premium-Mitgliedschaft.</p></p>
    // private static final String PATTERN_ERROR_BOT = "Too many wrong codes";
    // private int waitTime = 500;
    // private boolean happyhourboolean = false;
    private static HashMap<String, String> serverMap = new HashMap<String, String>();

    private static String[] serverList1;

    private String[] serverList2;

    private boolean hashFound;

    private CESClient ces;

    private static long LAST_FILE_CHECK = 0;

    private static final String PROPERTY_SELECTED_SERVER = "SELECTED_SERVER";

    private static final String PROPERTY_SELECTED_SERVER2 = "SELECTED_SERVER#2";

    private static final String PROPERTY_USE_TELEKOMSERVER = "USE_TELEKOMSERVER";

    private static final String PROPERTY_USE_PRESELECTED = "USE_PRESELECTED";

    // private static final String PROPERTY_USE_SSL = "USE_SSL";

    private static final String PROPERTY_WAIT_WHEN_BOT_DETECTED = "WAIT_WHEN_BOT_DETECTED";

    private static final String PROPERTY_INCREASE_TICKET = "INCREASE_TICKET";

    private static final String PROPERTY_PREMIUM_USER_2 = "PREMIUM_USER_2";

    private static final String PROPERTY_PREMIUM_PASS_2 = "PREMIUM_PASS_2";

    private static final String PROPERTY_USE_PREMIUM_2 = "USE_PREMIUM_2";

    private static final String PROPERTY_PREMIUM_USER_3 = "PREMIUM_USER_3";

    private static final String PROPERTY_PREMIUM_PASS_3 = "PREMIUM_PASS_3";

    private static final String PROPERTY_USE_PREMIUM_3 = "USE_PREMIUM_3";

    private static final String PROPERTY_FREE_IF_LIMIT_NOT_REACHED = "FREE_IF_LIMIT_NOT_REACHED";
    private static final String PARAM_FORRCEFREE_WHILE_HAPPYHOURS = "FORRCEFREE_WHILE_HAPPYHOURS";

    private static final int ACTION_TOGGLE_PREMIUM_1 = 1;

    private static final int ACTION_TOGGLE_PREMIUM_2 = 2;
    private static final int ACTION_TOGGLE_PREMIUM_3 = 3;
    private static final int ACTION_INFO_PREMIUM_1 = 4;
    private static final int ACTION_INFO_PREMIUM_2 = 5;
    private static final int ACTION_INFO_PREMIUM_3 = 6;

    private static final int ACTION_HAPPY_HOURS = 7;

    private static final String PARAM_WAIT_FOR_HAPPYHOURS = "WAIT_FOR_HAPPYHOURS";

    private static final int ACTION_HAPPY_HOURS_TOGGLE_WAIT = 8;

    private static final int ACTION_HAPPY_HOURS_FORCE_FREE = 9;

    private static final int ERROR_ID_ACCOUNTEXPIRED = 4;

    private static final Pattern PATTERN_MATCHER_PREMIUM_EXPIRED = Pattern.compile("Dieses Konto ist am .*? abgelaufen");

    private static final Pattern PATTERN_MATCHER_PREMIUM_LIMIT_REACHED = Pattern.compile("haben Sie <b>50 GB</b> heruntergeladen");

    private static final Pattern PATTERN_FIND_CAPTCHA_ID = Pattern.compile("<table><tr><td><img id\\=\"(.*?)\" src\\=\"\">");

    private static boolean FORCE_FREE_USER = true;

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return host + " - " + version;
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public Rapidshare() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        // serverMap.put("Cogent", "cg");
        // serverMap.put("Cogent #2", "cg2");
        serverMap.put("Deutsche Telekom", "dt");
        serverMap.put("GlobalCrossing #1", "gc");
        serverMap.put("GlobalCrossing #2", "gc2");
        serverMap.put("Level(3) #1", "l3");
        serverMap.put("Level(3) #2", "l32");
        serverMap.put("Level(3) #3", "l33");
        serverMap.put("Level(3) #4", "l34");
        serverMap.put("Tata Com. #1", "tg");
        serverMap.put("Tata Com. #2", "tg2");
        serverMap.put("TeliaSonera", "tl");
        serverMap.put("TeliaSonera #2", "tl2");
        serverMap.put("TeliaSonera #3", "tl3");

        serverList1 = new String[] { "gc", "gc2", "dt", "l3", "l32", "l33", "l34", "tg", "tl", "tl2" };
        serverList2 = new String[] { "dt", "gc", "gc2", "l3", "l32", "tg", "tg2", "tl", "tl2", "tl3" };
        this.setConfigElements();
    }

    /**
     * Gibt den Servernamen zum zugehörigen Serverkürzel zurück tl
     * -->teliaSonera
     * 
     * @param abb
     * @return
     */
    // private String getServerFromAbbreviation(String abb) {
    // Iterator<String> iter = serverMap.keySet().iterator();
    // Object next;
    // while (iter.hasNext()) {
    // next = iter.next();
    // if (serverMap.get((String) next).equals(abb)) return (String) next;
    // }
    // return null;
    // }
    private String getServerName(String id) {
        Iterator<Entry<String, String>> it = serverMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> next = it.next();
            if (next.getValue().equalsIgnoreCase(id)) return next.getKey();
        }
        return null;
    }

    /**
     * Wird von der gui aufgerufen um ein menü aufzubauen.
     */
    public ArrayList<MenuItem> createMenuitems() {

        ArrayList<MenuItem> menuList = new ArrayList<MenuItem>();
        MenuItem premium = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.rapidshare.menu.premium", "Premiumaccounts"), 0);
        MenuItem hh = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.rapidshare.menu.happyHour", "Happy Hours"), 0);

        MenuItem account;
        MenuItem m;

        m = new MenuItem(JDLocale.L("plugins.rapidshare.menu.happyHours", "Happy Hours Abfrage"), ACTION_HAPPY_HOURS);
        m.setActionListener(this);
        hh.addMenuItem(m);

        m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.forcefreewhilehh", "Free Download während Happy Hour erzwingen"), ACTION_HAPPY_HOURS_FORCE_FREE);
        m.setActionListener(this);
        m.setSelected(this.getProperties().getBooleanProperty(PARAM_FORRCEFREE_WHILE_HAPPYHOURS, false));
        hh.addMenuItem(m);

        m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.happyHourswait", "Auf Happy Hours warten"), ACTION_HAPPY_HOURS_TOGGLE_WAIT);
        m.setActionListener(this);
        m.setSelected(this.getProperties().getBooleanProperty(PARAM_WAIT_FOR_HAPPYHOURS, false));
        hh.addMenuItem(m);
        menuList.add(hh);
        menuList.add(premium);
        // account1
        account = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.rapidshare.menu.premium1", "1. Account (") + this.getProperties().getProperty(PROPERTY_PREMIUM_USER) + ")", 0);

        if (!this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.enable_premium", "Aktivieren"), ACTION_TOGGLE_PREMIUM_1);
            m.setSelected(false);

        } else {
            m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.disable_premium", "Deaktivieren"), ACTION_TOGGLE_PREMIUM_1);
            m.setSelected(true);

        }
        m.setActionListener(this);

        account.addMenuItem(m);
        m = new MenuItem(JDLocale.L("plugins.rapidshare.menu.premiumInfo", "Accountinformationen abrufen"), ACTION_INFO_PREMIUM_1);
        m.setActionListener(this);

        account.addMenuItem(m);
        premium.addMenuItem(account);

        // Account 2
        account = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.rapidshare.menu.premium2", "2. Account (") + this.getProperties().getProperty(PROPERTY_PREMIUM_USER_2) + ")", 0);

        if (!this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_2, false)) {
            m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.enable_premium", "Aktivieren"), ACTION_TOGGLE_PREMIUM_2);
            m.setSelected(false);

        } else {
            m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.disable_premium", "Deaktivieren"), ACTION_TOGGLE_PREMIUM_2);
            m.setSelected(true);

        }
        m.setActionListener(this);

        account.addMenuItem(m);
        m = new MenuItem(JDLocale.L("plugins.rapidshare.menu.premiumInfo", "Accountinformationen abrufen"), ACTION_INFO_PREMIUM_2);
        m.setActionListener(this);

        account.addMenuItem(m);
        premium.addMenuItem(account);
        // Account 3
        account = new MenuItem(MenuItem.CONTAINER, JDLocale.L("plugins.rapidshare.menu.premium3", "3. Account (") + this.getProperties().getProperty(PROPERTY_PREMIUM_USER_3) + ")", 0);

        if (!this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_3, false)) {
            m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.enable_premium", "Aktivieren"), ACTION_TOGGLE_PREMIUM_3);
            m.setSelected(false);

        } else {
            m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.rapidshare.menu.disable_premium", "Deaktivieren"), ACTION_TOGGLE_PREMIUM_3);
            m.setSelected(true);
            logger.info("TRUE");

        }
        m.setActionListener(this);
        m.setProperty("id", 3);
        account.addMenuItem(m);
        m = new MenuItem(JDLocale.L("plugins.rapidshare.menu.premiumInfo", "Accountinformationen abrufen"), ACTION_INFO_PREMIUM_3);
        m.setActionListener(this);

        account.addMenuItem(m);
        premium.addMenuItem(account);

        return menuList;
    }

    public void actionPerformed(ActionEvent e) {
        MenuItem mi = (MenuItem) e.getSource();

        switch (mi.getActionID()) {
        case Rapidshare.ACTION_TOGGLE_PREMIUM_1:
            getProperties().setProperty(PROPERTY_USE_PREMIUM, !getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false));
            getProperties().save();
            break;
        case Rapidshare.ACTION_TOGGLE_PREMIUM_2:
            getProperties().setProperty(PROPERTY_USE_PREMIUM_2, !getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_2, false));
            getProperties().save();
            break;
        case Rapidshare.ACTION_TOGGLE_PREMIUM_3:
            getProperties().setProperty(PROPERTY_USE_PREMIUM_3, !getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_3, false));
            getProperties().save();
            break;
        case Rapidshare.ACTION_INFO_PREMIUM_1:
            showInfo(1);
            break;
        case Rapidshare.ACTION_INFO_PREMIUM_2:
            showInfo(2);
            break;
        case Rapidshare.ACTION_INFO_PREMIUM_3:
            showInfo(3);
            break;
        case Rapidshare.ACTION_HAPPY_HOURS_TOGGLE_WAIT:
            getProperties().setProperty(PARAM_WAIT_FOR_HAPPYHOURS, !getProperties().getBooleanProperty(Rapidshare.PARAM_WAIT_FOR_HAPPYHOURS, false));
            getProperties().save();
            break;

        case Rapidshare.ACTION_HAPPY_HOURS_FORCE_FREE:
            getProperties().setProperty(PARAM_FORRCEFREE_WHILE_HAPPYHOURS, !getProperties().getBooleanProperty(Rapidshare.PARAM_FORRCEFREE_WHILE_HAPPYHOURS, false));
            getProperties().save();
            break;
        case Rapidshare.ACTION_HAPPY_HOURS:

            new Thread() {
                public void run() {
                    ProgressController progress = new ProgressController(JDLocale.L("plugins.hoster.rapidshare.com.happyHours", "Happy Hour Check"), 3);

                    try {
                        progress.increase(1);
                        RequestInfo ri = HTTP.getRequest(new URL("http://jdownloader.org/hh.php?txt=1"));
                        progress.increase(1);
                        int sec = 300 - JDUtilities.filterInt(SimpleMatches.getLines(ri.getHtmlCode())[3]);

                        int lastStart = JDUtilities.filterInt(SimpleMatches.getLines(ri.getHtmlCode())[4]);
                        int lastEnd = JDUtilities.filterInt(SimpleMatches.getLines(ri.getHtmlCode())[5]);
                        Date lastStartDate = new Date(lastStart * 1000L);
                        lastStartDate.setTime(lastStart * 1000L);

                        Date lastEndDate = new Date(lastEnd * 1000L);
                        lastEndDate.setTime(lastEnd * 1000L);
                        if (ri.containsHTML("Hour")) {
                            int activ = JDUtilities.filterInt(SimpleMatches.getLines(ri.getHtmlCode())[1]);
                            Date d = new Date(activ * 1000L);
                            d.setTime(activ * 1000L);

                            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                            String html = String.format(JDLocale.L("plugins.hoster.rapidshare.com.hhactive.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><body><div><p style='text-align:center'><img src='http://jdownloader.org/img/hh.jpg' /><br>Aktiv seit %s<br>Zuletzt überprüft vor %s<br>Letzte Happy Hour von %s bis %s</p></div></body>"), df.format(d), JDUtilities.formatSeconds(sec), df.format(lastStartDate), df.format(lastEndDate));
                            JDUtilities.getGUI().showHTMLDialog(JDLocale.L("plugins.hoster.rapidshare.com.happyHours", "Happy Hour Check"), html);
                        } else {
                            int activ = JDUtilities.filterInt(SimpleMatches.getLines(ri.getHtmlCode())[1]);
                            Date d = new Date(activ * 1000L);
                            d.setTime(activ * 1000L);

                            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                            String html = String.format(JDLocale.L("plugins.hoster.rapidshare.com.hhinactive.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><body><div><p style='text-align:center'><img src='http://jdownloader.org/img/nhh.jpg' /><br>Die letzte Happy Hour Phase endete am %s<br>Zuletzt überprüft vor %s<br>Letzte Happy Hour von %s bis %s</p></div></body>"), df.format(d), JDUtilities.formatSeconds(sec), df.format(lastStartDate), df.format(lastEndDate));
                            JDUtilities.getGUI().showHTMLDialog(JDLocale.L("plugins.hoster.rapidshare.com.happyHours", "Happy Hour Check"), html);
                        }
                    } catch (Exception e) {
                    }

                    progress.finalize();
                }
            }.start();
            break;
        }
        return;
    }

    /**
     * Zeigt premiuminfos zum Account i
     * 
     * @param i
     */
    private void showInfo(final int i) {
        new Thread() {
            public void run() {

                String user = null;
                String pass = null;
                switch (i) {
                case 1:
                    user = (String) getProperties().getProperty(PROPERTY_PREMIUM_USER);
                    pass = (String) getProperties().getProperty(PROPERTY_PREMIUM_PASS);
                    break;
                case 2:
                    user = (String) getProperties().getProperty(PROPERTY_PREMIUM_USER_2);
                    pass = (String) getProperties().getProperty(PROPERTY_PREMIUM_PASS_2);
                    break;
                case 3:
                    user = (String) getProperties().getProperty(PROPERTY_PREMIUM_USER_3);
                    pass = (String) getProperties().getProperty(PROPERTY_PREMIUM_PASS_3);
                    break;
                }
                user = JDUtilities.urlEncode(user.trim());
                pass = JDUtilities.urlEncode(pass.trim());
                String url = "https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi?login=" + user + "&password=" + pass;
                ProgressController progress = new ProgressController(JDLocale.L("plugins.hoster.rapidshare.com.loadinfo", "Lade Rs.com Account Informationen: ") + user, 5);

                try {
                    progress.increase(1);
                    RequestInfo ri = HTTP.getRequest(new URL(url));
                    progress.increase(1);
                    // logger.info(ri.getHtmlCode());
                    String html = null;

                    if (ri.containsHTML("Ein Fehler ist aufgetreten")) {
                        html = JDLocale.L("plugins.hoster.rapidshare.com.info.error", "<font color='red'><p>Account nicht gefunden</p></font>");
                    } else {
                        String validuntil = SimpleMatches.getSimpleMatch(ri, "<td>G&uuml;ltig bis:</td><td style=\"padding-right:20px;\"><b>°</b></td>", 0);
                        String files = SimpleMatches.getSimpleMatch(ri, " <td>Dateien:</td><td><b>°</b></td> ", 0);
                        String days5traffic = SimpleMatches.getSimpleMatch(ri, "<td>5 Tage Traffic:</td><td align=right style=\"padding-right:20px;\"><b>°</b></td>", 0);
                        String rapidPoints = SimpleMatches.getSimpleMatch(ri, " <td>RapidPoints:</td><td style=\"padding-right:20px;\"><b>°</b>", 0);

                        String trafficshare = SimpleMatches.getSimpleMatch(ri, "<td>TrafficShare &uuml;brig:</td><td><b>°</b>", 0);
                        String usedHD = SimpleMatches.getSimpleMatch(ri, "<td>Belegter Speicher:</td><td align=right style=\"padding-right:20px;\"><b>°</b>", 0);

                        html = String.format(JDLocale.L("plugins.hoster.rapidshare.com.info.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><div style='width:534px;height;200px'><h2>Accountinformation</h2><table width='100%%' ><tr><th >Valid until</th><td>%s</td></tr><tr><th >Premiumpoints</th><td>%s</td></tr><tr><th >Current traffic</th><td>%s</td></tr><tr><th >Used space</th><td>%s</td></tr><tr><th >Files</th><td>%s</td></tr></table></div>"), validuntil, rapidPoints, days5traffic, usedHD + "/" + trafficshare, files);
                    }

                    JDUtilities.getGUI().showHTMLDialog(String.format(JDLocale.L("plugins.hoster.rapidshare.com.info.title", "Accountinfo für %s"), user), html);
                } catch (MalformedURLException e) {
                } catch (IOException e) {
                }

                progress.finalize();
            }
        }.start();
        ;
    }

    /**
     * Erzeugtd en Configcontainer für die Gui
     */
    private void setConfigElements() {
        ConfigEntry conditionEntry;
        Vector<String> m1 = new Vector<String>();
        Vector<String> m2 = new Vector<String>();
        for (int i = 0; i < serverList1.length; i++)
            m1.add(getServerName(serverList1[i]));
        for (int i = 0; i < serverList2.length; i++)
            m2.add(getServerName(serverList2[i]));
        m1.add("zufällig");
        m2.add("zufällig");
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer", "Bevorzugte Server")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), PROPERTY_SELECTED_SERVER, m1.toArray(new String[] {}), "#1"));
        cfg.setDefaultValue("Level(3)");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), PROPERTY_SELECTED_SERVER2, m2.toArray(new String[] {}), "#2"));
        cfg.setDefaultValue("TeliaSonera");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_TELEKOMSERVER, JDLocale.L("plugins.hoster.rapidshare.com.telekom", "Telekom Server verwenden falls verfügbar")));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PRESELECTED, JDLocale.L("plugins.hoster.rapidshare.com.preSelection", "Vorauswahl übernehmen")));
        cfg.setDefaultValue(true);

        ConfigContainer premiumConfig = new ConfigContainer(this, JDLocale.L("plugins.hoster.rapidshare.com.premiumtab", "Premium Einstellungen"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, premiumConfig));
        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "1. " + JDLocale.L("plugins.hoster.rapidshare.com.premiumAccount", "Premium Account")));
        conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, JDLocale.L("plugins.hoster.rapidshare.com.usePremium", "Premium Account verwenden"));

        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, JDLocale.L("plugins.hoster.rapidshare.com.premiumUser", "Premium User")));
        cfg.setDefaultValue(JDLocale.L("plugins.rapidshare.userid", "Kundennummer"));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);
        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, JDLocale.L("plugins.hoster.rapidshare.com.premiumPass", "Premium Pass")));
        cfg.setDefaultValue(JDLocale.L("plugins.rapidshare.pass", "Passwort"));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);
        premiumConfig.addEntry(conditionEntry);
        conditionEntry.setDefaultValue(false);

        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "2. " + JDLocale.L("plugins.hoster.rapidshare.com.premiumAccount", "Premium Account")));

        conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM_2, JDLocale.L("plugins.hoster.rapidshare.com.usePremium2", "2. Premium Account verwenden"));

        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER_2, JDLocale.L("plugins.hoster.rapidshare.com.premiumUser2", "Premium User(alternativ)")));
        cfg.setDefaultValue(JDLocale.L("plugins.rapidshare.userid", "Kundennummer"));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);
        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS_2, JDLocale.L("plugins.hoster.rapidshare.com.premiumPass2", "Premium Pass(alternativ)")));
        cfg.setDefaultValue(JDLocale.L("plugins.rapidshare.pass", "Passwort"));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);
        premiumConfig.addEntry(conditionEntry);
        conditionEntry.setDefaultValue(false);
        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "3. " + JDLocale.L("plugins.hoster.rapidshare.com.premiumAccount", "Premium Account")));
        conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM_3, JDLocale.L("plugins.hoster.rapidshare.com.usePremium3", "3. Premium Account verwenden"));
        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER_3, JDLocale.L("plugins.hoster.rapidshare.com.premiumUser3", "Premium User(alternativ)")));
        cfg.setDefaultValue(JDLocale.L("plugins.rapidshare.userid", "Kundennummer"));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);
        premiumConfig.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS_3, JDLocale.L("plugins.hoster.rapidshare.com.premiumPass3", "Premium Pass(alternativ)")));
        cfg.setDefaultValue(JDLocale.L("plugins.rapidshare.pass", "Passwort"));
        cfg.setEnabledCondidtion(conditionEntry, "==", true);
        premiumConfig.addEntry(conditionEntry);
        conditionEntry.setDefaultValue(false);

        ConfigContainer extended = new ConfigContainer(this, JDLocale.L("plugins.hoster.rapidshare.com.extendedTab", "Erweiterte Einstellungen"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CONTAINER, extended));

        // extended.addEntry(cfg = new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // getProperties(), PROPERTY_USE_SSL,
        // JDLocale.L("plugins.hoster.rapidshare.com.useSSL", "SSL Downloadlink
        // verwenden")));
        // cfg.setDefaultValue(false);
        //
        // extended.addEntry(cfg = new
        // ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(),
        // PROPERTY_FREE_IF_LIMIT_NOT_REACHED,
        // JDLocale.L("plugins.hoster.rapidshare.com.freeDownloadIfLimitNotReached",
        // "Premium: Free Download wenn Limit noch nicht erreicht wurde")));
        // cfg.setDefaultValue(false);

        extended.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), PROPERTY_WAIT_WHEN_BOT_DETECTED, JDLocale.L("plugins.hoster.rapidshare.com.waitTimeOnBotDetection", "Wartezeit [ms] wenn Bot erkannt wird.(-1 für Reconnect)"), -1, 600000).setDefaultValue(-1).setStep(1000));
        extended.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), PROPERTY_INCREASE_TICKET, JDLocale.L("plugins.hoster.rapidshare.com.increaseTicketTime", "Ticketwartezeit verlängern (0%-500%)"), 0, 500).setDefaultValue(0).setExpertEntry(true).setStep(1));
        // cfg.setDefaultValue(true);

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
        return link;
    }

    public static void correctURL(DownloadLink downloadLink) {
        downloadLink.setUrlDownload(getCorrectedURL(downloadLink.getDownloadURL()));
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        // RequestInfo requestInfo;

        if (step == null) {
            logger.info("Plugin Ende erreicht.");
            return null;
        }
        correctURL(downloadLink);

        // premium
        PluginStep st;
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && ((this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false) || this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_2, false) || this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_3, false)))) {

            st = this.doPremiumStep(step, downloadLink);
        } else {
            st = this.doFreeStep(step, downloadLink);
        }
        logger.finer("got step: " + st + " Linkstatus: " + downloadLink.getStatus());

        return st;
    }

    private PluginStep doFreeStep(PluginStep step, DownloadLink downloadLink) {
        // FREE_DOWNLOADS_INPROGRESS++;
        // if (step == this.steps.firstElement()) this.downloadType = FREE;
        PluginStep ret = doFreeStep0(step, downloadLink);
        // FREE_DOWNLOADS_INPROGRESS--;
        return ret;
    }

    private PluginStep doFreeStep0(PluginStep step, DownloadLink downloadLink) {
        // if (ddl) return this.doPremiumStep(step, downloadLink);
        try {
            if (getRemainingWaittime() > 0) { return handleDownloadLimit(step, downloadLink); }
            String freeOrPremiumSelectPostURL = null;
            Request req;
            if (!checkDestFile(step, downloadLink)) { return step; }
            String link = downloadLink.getDownloadURL();

            // RS URL wird aufgerufen
            req = new GetRequest(link);
            req.load();
            if (req.getLocation() != null) {
                logger.info("Direct Download");
                return doDirectDownload(step, downloadLink, req.getLocation());
            }
            // posturl für auswahl free7premium wird gesucht
            freeOrPremiumSelectPostURL = new Regex(req, PATTERN_FIND_MIRROR_URL).getFirstMatch();
            // Fehlerbehandlung auf der ersten Seite
            if (freeOrPremiumSelectPostURL == null) {
                String error = null;
                if ((error = findError(req + "")) != null) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setParameter(error);
                    return step;
                }
                reportUnknownError(req, 1);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                step.setStatus(PluginStep.STATUS_ERROR);
                logger.warning("could not get newURL");
                return step;
            }

            // Post um freedownload auszuwählen
            PostRequest pReq = new PostRequest(freeOrPremiumSelectPostURL);
            pReq.setPostVariable("dl.start", "free");
            pReq.load();
            String error = null;

            if ((error = findError(req + "")) != null) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                step.setParameter(error);
                return step;
            }

            // Wartezeit (Downloadlimit) wird gesucht
            String strWaitTime = new Regex(pReq, PATTERN_FIND_DOWNLOAD_LIMIT_WAITTIME).getFirstMatch();
            int waitTime;
            if (strWaitTime != null) {
                waitTime = (int) (Double.parseDouble(strWaitTime) * 60 * 1000);
                logger.info("DownloadLimit reached. Wait " + JDUtilities.formatSeconds(waitTime / 1000) + " or reconnect");
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                setDownloadLimitTime(waitTime);
                step.setStatus(PluginStep.STATUS_ERROR);
                step.setParameter((long) waitTime);

                return step;
            }

            // Fehlersuche
            if (Regex.matches(pReq, PATTERN_MATCHER_TOO_MANY_USERS)) {
                logger.warning("Too many users are currently downloading this file. Wait 2 Minutes and try again");
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TO_MANY_USERS);
                return step;
            } else if (new Regex(pReq, PATTERM_MATCHER_ALREADY_LOADING).matches()) {
                logger.severe("Already downloading. Wait 2 min. or reconnect");

                waitTime = 120 * 1000;
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_STATIC_WAITTIME);
                step.setStatus(PluginStep.STATUS_ERROR);
                // setDownloadLimitTime(waitTime);

                step.setParameter((long) waitTime);
                return step;
            } else if ((error = findError(pReq + "")) != null) {

                reportUnknownError(pReq, 2);

                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                step.setParameter(error);
                return step;
            }
            // Ticketwartezeit wird gesucht
            String ticketTime = new Regex(pReq, PATTERN_FIND_TICKET_WAITTIME).getFirstMatch();
            if (ticketTime != null && ticketTime.equals("0")) ticketTime = null;

            String ticketCode = pReq.getHtmlCode();
    
            String tt=new Regex(ticketCode,"var tt =(.*?)document\\.getElementById\\(\"dl\"\\)\\.innerHTML").getFirstMatch();
            
            String fun="function f(){ return "+tt+"} f()";
            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects();

            // Collect the arguments into a single string.
          
          

            // Now evaluate the string we've colected.
            Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);

            // Convert the result to a string and print it.
            ticketCode=Context.toString(result);
            Context.exit();
            
            // captchaadresse wird gesucht
//            String cid=new Regex(ticketCode,PATTERN_FIND_CAPTCHA_IMAGE_URL).getFirstMatch();
//            Pattern p=Pattern.compile("getElementById\\(\""+cid+"\"\\)\\.src\\=\"(.*?)\"");
//            
            String captchaAddress = new Regex(ticketCode, PATTERN_FIND_CAPTCHA_IMAGE_URL).getFirstMatch();

            // Happy Hour check
            String captchaCode = null;
            File captchaFile = null;
            if (Regex.matches(pReq, PATTERN_MATCHER_HAPPY_HOUR)) {

                logger.info("Happy hours active");
                // return doHappyHourDownload(step, downloadLink);

            } else {

                if (getProperties().getBooleanProperty(Rapidshare.PARAM_WAIT_FOR_HAPPYHOURS, false)) {

                    // Auf Happy Hour warten
                    waitForHappyHours(step, downloadLink);
                    return step;

                }

                if (captchaAddress == null) {
                    logger.severe("Captcha Address not found");
                    this.reportUnknownError(pReq, 2);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }

                
                // downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                // step.setParameter(1000l);
                // step.setStatus(PluginStep.STATUS_ERROR);

                // if(true)return step;

                long pendingTime = 0;
                if (ticketTime != null) {
                    pendingTime = Long.parseLong(ticketTime);

                    if (getProperties().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) > 0) {
                        logger.warning("Waittime increased by JD: " + pendingTime + " --> " + (pendingTime + (getProperties().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime) / 100));
                        pendingTime = (pendingTime + (getProperties().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime) / 100);

                    }
                    pendingTime *= 1000;
                   // pendingTime -= timer;

                    // downloadLink.setEndOfWaittime(System.currentTimeMillis()
                    // + pendingTime);
                }

                waitTicketTime(step, downloadLink, pendingTime);
                captchaFile = this.getLocalCaptchaFile(this);

                long timer = System.currentTimeMillis();

                captchaCode = getCaptchaCode(step, downloadLink, captchaFile, captchaAddress);

                if (downloadLink.getStatus() == DownloadLink.STATUS_ERROR_BOT_DETECTED) { return step; }
                timer = System.currentTimeMillis() - timer;

                // War Captchaerkennung Fehlerhaft?
                if (step.getStatus() == PluginStep.STATUS_ERROR) { return step; }

                if (captchaCode == null || captchaCode.trim().length() != 4) {
                    logger.severe("Captcha could not be recognized");
                    JDUtilities.appendInfoToFilename(this, captchaFile, captchaCode, false);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);

                    if (ces != null) ces.sendCaptchaWrong();
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }

                logger.info("captcha detection duration: " + JDUtilities.formatSeconds((int) (timer / 1000)));

                
            }
            // get Downloadserverurl
            String postTarget = getDownloadTarget(step, downloadLink, ticketCode);

            // Falls Serverauswahl fehlerhaft war
            if (step.getStatus() == PluginStep.STATUS_ERROR) { return step; }

            pReq = new PostRequest(postTarget);
            pReq.setPostVariable("mirror", "on");
            if (captchaCode == null) captchaCode = "";
            pReq.setPostVariable("accesscode", captchaCode);
            pReq.setPostVariable("x", (int) (Math.random() * 40) + "");
            pReq.setPostVariable("y", (int) (Math.random() * 40) + "");
            pReq.connect();

            HTTPConnection urlConnection = pReq.getHttpConnection();
            String name = getFileNameFormHeader(urlConnection);
            if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
            downloadLink.setName(name);
            int length = urlConnection.getContentLength();
            downloadLink.setDownloadMax(length);

            logger.info("link: " + postTarget.substring(0, 30) + " ");

            dl = new RAFDownload(this, downloadLink, urlConnection);

            if (dl.startDownload()) {

                if (new File(downloadLink.getFileOutput()).length() < 6000) {
                    String page = JDUtilities.getLocalFile(new File(downloadLink.getFileOutput()));
                    error = findError(page + "");
                    if (new Regex(page, PATTERN_MATCHER_CAPTCHA_WRONG).matches()) {

                        new File(downloadLink.getFileOutput()).delete();

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);

                        if (hashFound) {

                            new GetRequest("http://jdservice.ath.cx/rs/hw.php?loader=jd&code=" + captchaCode + "&hash=" + JDUtilities.getLocalHash(captchaFile)).load();

                        }
                        JDUtilities.appendInfoToFilename(this, captchaFile, captchaCode, false);
                        if (ces != null) ces.sendCaptchaWrong();
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    if (new Regex(page, PATTERN_MATCHER_BOT).matches()) {
                        new File(downloadLink.getFileOutput()).delete();

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
                        logger.info("Error detected. Bot detected");

                        step.setStatus(PluginStep.STATUS_ERROR);

                        new GetRequest("http://jdservice.ath.cx/rs/hw.php?loader=jd&code=BOT!&hash=" + JDUtilities.getLocalHash(captchaFile)).load();

                        return step;
                    }
                    if (Regex.matches(page, PATTERN_MATCHER_DOWNLOAD_ERRORPAGE)) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                        downloadLink.setStatusText("Download error(>log)");
                        step.setParameter(error);
                        logger.severe("Error detected. " + JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())));
                        new File(downloadLink.getFileOutput()).delete();
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                }
                return null;
            }

        } catch (SocketTimeoutException e1) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            step.setParameter(JDLocale.L("gui.status.timeoutdetected", "Timeout"));
            step.setStatus(PluginStep.STATUS_ERROR);

        } catch (IOException e) {
            logger.severe("URL could not be opened. " + e.toString());
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            step.setParameter(JDUtilities.convertExceptionReadable(e));
            downloadLink.setStatusText(JDUtilities.convertExceptionReadable(e));
            step.setStatus(PluginStep.STATUS_ERROR);
            return step;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return step;

    }

    private String findError(String string) {
        String error = null;
        error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE).getFirstMatch();

        if (error == null || error.length() == 0) error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_3).getFirstMatch();
        if (error == null || error.length() == 0) error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_2).getFirstMatch();

        error = JDUtilities.htmlDecode(error);
        String[] er = SimpleMatches.getLines(error);

        if (er == null || er.length == 0) return null;
        error = JDLocale.L("plugins.host.rapidshare.errors." + JDUtilities.getMD5(er[0]), er[0]);
        if (error.equals(er[0])) {
            logger.warning("NO TRANSLATIONKEY FOUND FOR: " + er[0] + "(" + JDUtilities.getMD5(er[0]) + ")");
        }
        return error;

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
     */
    private String getDownloadTarget(PluginStep step, DownloadLink downloadLink, String ticketCode) {

        String postTarget = new Regex(ticketCode, PATTERN_FIND_DOWNLOAD_POST_URL).getFirstMatch();

        String server1 = this.getProperties().getStringProperty(PROPERTY_SELECTED_SERVER, "Level(3)");
        String server2 = this.getProperties().getStringProperty(PROPERTY_SELECTED_SERVER2, "TeliaSonera");
        String serverAbb = serverMap.get(server1);
        String server2Abb = serverMap.get(server2);

        logger.info("Servers settings: " + server1 + "-" + server2 + " : " + serverAbb + "-" + server2Abb);
        if (serverAbb == null) {
            serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
            logger.finer("Use Random #1 server " + serverAbb);
        }
        if (server2Abb == null) {
            server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
            logger.finer("Use Random #2 server " + server2Abb);
        }
        // String endServerAbb = "";
        boolean telekom = this.getProperties().getBooleanProperty(PROPERTY_USE_TELEKOMSERVER, false);
        boolean preselected = this.getProperties().getBooleanProperty(PROPERTY_USE_PRESELECTED, true);

        // actionString = getSimpleMatch(ticketCode, dataPatternAction,
        // 0);

        if (postTarget == null) {
            logger.severe("postTarget not found:");
            this.reportUnknownError(ticketCode, 4);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            return null;
        }
        // // postTarget=postTarget.substring(2, postTarget.length()-3);
        // // logger.info(postTarget+" -"+actionString);
        // if (actionString == null) {
        // logger.severe("actionString not found");
        // downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        // step.setStatus(PluginStep.STATUS_ERROR);
        // return step;
        // }
        // Vector<String> serverids = getAllSimpleMatches(ticketCode,
        // patternForServer, 3);
        String[] serverstrings = new Regex(ticketCode, PATTERN_FIND_MIRROR_URLS).getMatches(1);

        // logger.info(ticketCode);
        logger.info("wished Mirror #1 Server " + serverAbb);
        logger.info("wished Mirror #2 Server " + server2Abb);
        String selected = new Regex(ticketCode, PATTERN_FIND_PRESELECTED_SERVER).getFirstMatch();
        logger.info("Preselected Server: " + selected.substring(0, 30));
        if (preselected) {
            logger.info("RS.com-free Use preselected : " + selected.substring(0, 30));
            postTarget = selected;
        } else if (telekom && ticketCode.indexOf("td.rapidshare.com") >= 0) {
            logger.info("RS.com-free Use Telekom Server");
            postTarget = getURL(serverstrings, "Deutsche Telekom", postTarget);
        } else if (ticketCode.indexOf(serverAbb + ".rapidshare.com") >= 0) {
            logger.info("RS.com-free Use Mirror #1 Server: " + getServerName(serverAbb));

            postTarget = getURL(serverstrings, getServerName(serverAbb), postTarget);
        } else if (ticketCode.indexOf(server2Abb + ".rapidshare.com") >= 0) {
            logger.info("RS.com-free Use Mirror #2 Server: " + getServerName(server2Abb));
            postTarget = getURL(serverstrings, getServerName(server2Abb), postTarget);
        } else if (serverstrings.length > 0) {
            logger.severe("Kein Server gefunden 1");
        } else {
            logger.severe("Kein Server gefunden 2");
            // downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            // return null;
        }

        return postTarget;
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

    /**
     * Wartet die angegebene Ticketzeit ab
     * 
     * @param step
     * @param downloadLink
     * @param pendingTime
     * @throws InterruptedException
     */
    private void waitTicketTime(PluginStep step, DownloadLink downloadLink, long pendingTime) throws InterruptedException {

        while (pendingTime > 0 && !downloadLink.getDownloadLinkController().isAborted()) {
            downloadLink.setStatusText(String.format(JDLocale.L("plugin.rapidshare.tickettime", "Wait %s for ticket"), JDUtilities.formatSeconds((int) (pendingTime / 1000))));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);
            pendingTime -= 1000;
        }

    }

    /**
     * Versucht den Captchacode zu ermitteln. 1. über die Hashdatenbank 2. Über
     * ces 3. Über JAC
     * 
     * @param step
     * @param downloadLink
     * @param captchaFile
     * @param captchaAddress
     * @return
     */
    private String getCaptchaCode(PluginStep step, DownloadLink downloadLink, File captchaFile, String captchaAddress) {
        GetRequest r = new GetRequest(captchaAddress);
        r.setFollowRedirects(false);
        try {
            r.connect();
        } catch (IOException e1) {
            return null;
        }
        if (r.getResponseHeader("Location") != null) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
            step.setStatus(PluginStep.STATUS_ERROR);
            return null;
        }
        if (!JDUtilities.download(captchaFile, r.getHttpConnection()) || !captchaFile.exists()) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
            step.setParameter(null);
            step.setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
            return null;
        }

        if (doBotCheck(captchaFile)) {

            downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
            step.setStatus(PluginStep.STATUS_ERROR);
            step.setParameter(null);

            try {
                new GetRequest("http://jdservice.ath.cx/rs/hw.php?loader=jd&code=BOT!&hash=" + JDUtilities.getLocalHash(captchaFile)).load();
            } catch (IOException e) {

                e.printStackTrace();
            }

            return null;
        }
        downloadLink.setStatusText(JDLocale.L("plugins.rapidshare.captcha", "OCR & Wartezeit"));
        downloadLink.requestGuiUpdate();
        String captchaCode = null;

        // Hashmethode
        try {
            this.hashFound = true;
            String code = new GetRequest("http://jdservice.ath.cx/rs/h.php?loader=jd&code=&hash=" + JDUtilities.getLocalHash(captchaFile)).load();
            captchaCode = new Regex(code, "code=([\\w]{4});").getFirstMatch();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (captchaCode == null || captchaCode.trim().length() != 4) {
            hashFound = false;
            // CES Methode
            if (JDUtilities.getSubConfig("JAC").getBooleanProperty(Configuration.JAC_USE_CES, false) && !CES.isEnabled()) {
                ProgressController progress = new ProgressController(JDLocale.L("plugins.rapidshare.captcha.progress", "Captchaerkennung"), 3);
                progress.increase(2);
                ces = new CESClient(captchaFile);
                ces.setLogins(JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_USER), JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_PASS));
                ces.setSpecs("Please enter all letters having a <img src=\"http://rapidshare.com/img/cat.png\"> below.<br>Enter FOUR letters with <img src=\"http://rapidshare.com/img/cat.png\">:");
                ces.setPlugin(this);
                ces.setDownloadLink(downloadLink);
                captchaCode = null;
                if (ces.sendCaptcha()) {
                    downloadLink.setStatusText(JDLocale.L("plugins.rapidshare.ces.status", "C.E.S aktiv"));
                    captchaCode = ces.waitForAnswer();
                    progress.finalize();
                }

            }

            // JAC Methode
            if (!JDUtilities.getSubConfig("JAC").getBooleanProperty(Configuration.JAC_USE_CES, false) || CES.isEnabled() || captchaCode == null) {
                ProgressController progress = new ProgressController(JDLocale.L("plugins.rapidshare.captcha.progress", "Captchaerkennung"), 3);
                progress.increase(2);
                captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                progress.finalize();
            }

        }
        return captchaCode;
    }

    /**
     * Lädt den Link in der Happyhour. Ticketzeit und Captchaerkennung sind
     * dabei nicht zu beachten
     * 
     * @param step
     * @param downloadLink
     * @return
     */
    private PluginStep doHappyHourDownload(PluginStep step, DownloadLink downloadLink) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Wartet 5 minuten und startet den Downlaod anschließend neu.
     * 
     * @param step
     * @param downloadLink
     * @throws InterruptedException
     */
    private void waitForHappyHours(PluginStep step, DownloadLink downloadLink) throws InterruptedException {
        // 5 Minuten Warten und dann einenj Neuversuch starten

        int happyWaittime = 5 * 60 * 1000;
        ProgressController p = new ProgressController(String.format(JDLocale.L("plugins.rapidshare.waitForHappyHour.progressbartext", "Warte auf HappyHour. Nächster Versuch in %s min"), JDUtilities.formatSeconds(happyWaittime / 1000)), happyWaittime);
        p.setStatus(happyWaittime);
        downloadLink.setStatusText(JDLocale.L("plugins.rapidshare.waitForHappyHour", "Warte auf HappyHour"));
        downloadLink.requestGuiUpdate();
        while (happyWaittime > 0) {
            if (downloadLink.getDownloadLinkController().isAborted()) {
                p.finalize();
                return;
            }
            Thread.sleep(1000);
            happyWaittime -= 1000;
            p.setStatus(happyWaittime);
            p.setStatusText(String.format(JDLocale.L("plugins.rapidshare.waitForHappyHour.progressbartext", "Warte auf HappyHour. Nächster Versuch in %s min"), JDUtilities.formatSeconds(happyWaittime / 1000)));
        }

        step.setStatus(PluginStep.STATUS_ERROR);
        step.setParameter(0L);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);

    }

    /**
     * Prüft vor dem Download ob der Download geschrieben werden darf Es wird
     * z.B. auf "Is local file in progress" oder "fileexists" geprüft.
     * 
     * @param step
     * @param downloadLink
     * @return
     */
    private boolean checkDestFile(PluginStep step, DownloadLink downloadLink) {
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
            step.setStatus(PluginStep.STATUS_ERROR);
            return false;
        }

        if (new File(downloadLink.getFileOutput()).exists()) {
            logger.severe("File already exists. " + downloadLink.getFileOutput());
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
            step.setStatus(PluginStep.STATUS_ERROR);
            return false;
        }
        return true;
    }

    /**
     * Wird ein Premium DirectLink angeboten, kann der link von freusern wie ein
     * Premiumuser egladen werden, ohne downloadbeschränkung
     * 
     * @param step
     * @param downloadLink
     * @param location
     * @return
     */
    private PluginStep doDirectDownload(PluginStep step, DownloadLink downloadLink, String location) {
        logger.info("Direct Download from: " + location.substring(0, 30));
        try {
            // HashMap<String, String> ranger = new HashMap<String, String>();
            // ranger.put("Authorization", "Basic " +
            // JDUtilities.Base64Encode(user + ":" +
            // pass));
            HTTPConnection urlConnection;
            GetRequest req = new GetRequest(location);

            req.connect();
            urlConnection = req.getHttpConnection();
            if (urlConnection.getHeaderField("content-disposition") == null) {

                String page = req.read();
                String error;
                if ((error = this.findError(page)) != null) {
                    new File(downloadLink.getFileOutput()).delete();

                    logger.warning(error);
                    step.setStatus(PluginStep.STATUS_ERROR);

                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    downloadLink.setStatusText(error);
                    step.setParameter(error);

                    return step;
                } else {
                    new File(downloadLink.getFileOutput()).delete();

                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                    this.reportUnknownError(page, 6);

                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }

            }

            int length = urlConnection.getContentLength();

            downloadLink.setDownloadMax(length);
            String name = getFileNameFormHeader(urlConnection);
            if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
            downloadLink.setName(name);
            dl = new RAFDownload(this, downloadLink, urlConnection);

            dl.setResume(true);
            dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 3));
            if (dl.startDownload()) {

                if (new File(downloadLink.getFileOutput()).length() < 6000 && Regex.matches(JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())), PATTERN_MATCHER_DOWNLOAD_ERRORPAGE)) {
                    new File(downloadLink.getFileOutput()).delete();

                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    downloadLink.setStatusText("Download error(>log)");
                    step.setParameter("Download error(>log)");
                    logger.severe("Error detected.  " + JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())));

                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }

                step.setStatus(PluginStep.STATUS_DONE);
                downloadLink.setStatus(DownloadLink.STATUS_DONE);

                return step;
            }
        } catch (Exception e) {
            e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("DDL Error");
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            return step;
        }
        return step;
    }

    /**
     * premiumdownload Methode
     * 
     * @param step
     * @param downloadLink
     * @return
     */

    private PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) {
        try {

            if (this.getProperties().getBooleanProperty(PARAM_FORRCEFREE_WHILE_HAPPYHOURS, false)) {

                // RS URL wird aufgerufen

                GetRequest req = new GetRequest(downloadLink.getDownloadURL());
                req.load();
                if (req.getLocation() != null) {
                    logger.info("Direct Download");
                    return doDirectDownload(step, downloadLink, req.getLocation());
                }
                // posturl für auswahl free7premium wird gesucht
                String freeOrPremiumSelectPostURL = new Regex(req, PATTERN_FIND_MIRROR_URL).getFirstMatch();
                // Fehlerbehandlung auf der ersten Seite
                if (freeOrPremiumSelectPostURL == null) {
                    String error = null;
                    if ((error = findError(req + "")) != null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                        step.setParameter(error);
                        return step;
                    }
                    reportUnknownError(req, 1);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    logger.warning("could not get newURL");
                    return step;
                }

                // Post um freedownload auszuwählen
                PostRequest pReq = new PostRequest(freeOrPremiumSelectPostURL);
                pReq.setPostVariable("dl.start", "free");
                pReq.load();
                String error = null;
                // Fehlersuche
                // if ((error = new Regex(pReq,
                // PATTERN_FIND_ERROR_CODES).getFirstMatch()) != null) {
                // setError(step, downloadLink, error);
                // return step;
                // }
                // if (Regex.matches(pReq, PATTERN_MATCHER_FIND_ERROR)) {
                // error = new Regex(pReq,
                // PATTERN_FIND_ERROR_MESSAGE).getFirstMatch();
                // reportUnknownError(pReq, 2);
                //
                // step.setStatus(PluginStep.STATUS_ERROR);
                // downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                // step.setParameter(error);
                // return step;
                // }
                //               

                FORCE_FREE_USER = false;
                // Wartezeit (Downloadlimit) wird gesucht
                String strWaitTime = new Regex(pReq, PATTERN_FIND_DOWNLOAD_LIMIT_WAITTIME).getFirstMatch();
                int waitTime;
                if (strWaitTime != null && !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT, false)) {
                    logger.info("Waittime detected. check happy hour via jdownloader.org");
                    String happyCHeck = new GetRequest("http://jdownloader.org/hh.php?txt=1").load();

                    if (happyCHeck != null && happyCHeck.contains("happy")) {
                        FORCE_FREE_USER = true;
                        logger.info("jdownloader.org detected Happy Hour. Reconnect now");
                        logger.severe("wait " + strWaitTime + " minutes");
                        waitTime = (int) (Double.parseDouble(strWaitTime) * 60 * 1000);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        setDownloadLimitTime(waitTime);
                        step.setStatus(PluginStep.STATUS_ERROR);

                        step.setParameter((long) waitTime);
                        return step;
                    }
                    logger.info("jdownloader.org detected NO Happy Hour, ...continue with Premium");

                } else if (strWaitTime != null) {

                    logger.info("jdownloader.org detected Happy Hour, but Reconnect is  disabled..continue with Premium");
                } else {
                    if (new Regex(pReq, PATTERN_MATCHER_HAPPY_HOUR).matches()) {
                        logger.info("happy Hour is active. Disable Force Free Download inhappy hour to avoid this");
                        FORCE_FREE_USER = true;
                        return this.doFreeStep(step, downloadLink);
                    } else {
                        logger.info("Not happy hour..continue with premium");

                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //   
        //     
        // String firstPage=new
        // GetRequest(downloadLink.getDownloadURL()).load();
        //        
        // String newURL =
        // SimpleMatches.getFirstMatch(ri.getHtmlCode(),PATTERN_FIND_MIRROR_URL,
        // 1);
        // ri = HTTP.postRequest(new URL(newURL), null, null, null,
        // "dl.start=FREE",
        // true);
        // String strWaitTime = new Regex(requestInfo,
        // PATTERN_MATCHER_DOWNLOAD_LIMIT_WAITTIME).getFirstMatch();
        //        
        // if (strWaitTime != null &&
        // !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_DISABLE_RECONNECT,
        // false)) {
        // logger.info("Waittime detected. check happy hour via
        // jdownloader.org");
        // RequestInfo ch = HTTP.getRequest(new
        // URL("http://jdownloader.org/hh.php?txt=1"));
        // if (ch.containsHTML("happy")) {
        // logger.info("jdownloader.org detected Happy Hour. Reconnect now");
        // logger.severe("wait " + strWaitTime + " minutes");
        // waitTime = (int) (Double.parseDouble(strWaitTime) * 60 * 1000);
        // downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
        // setDownloadLimitTime(waitTime);
        // step.setStatus(PluginStep.STATUS_ERROR);
        // logger.info(" WARTEZEIT SETZEN IN " + step + " : " + waitTime);
        // step.setParameter((long) waitTime);
        // return step;
        // } else {
        // logger.info("jdownloader.org detected NO Happy Hour, ...continue with
        // Premium");
        // HAPPYHOUR_IS_SUPPOSED = false;
        //        
        // }
        // } else if (strWaitTime != null) {
        // HAPPYHOUR_IS_SUPPOSED = false;
        // logger.info("jdownloader.org detected Happy Hour, but Reconnect is
        // disabled..continue with Premium");
        // } else {
        // if (new Regex(ri, PATTERN_MATCHER_HAPPY_HOUR).matches()) {
        // logger.info("happy Hour is active. Disable Force Free Download in
        // happy hour
        // to avoid this");
        // HAPPYHOUR_IS_SUPPOSED = true;
        // this.resetSteps();
        // return this.doFreeStep(step, downloadLink);
        // } else {
        // logger.info("Not happy hour..continue with premium");
        // HAPPYHOUR_IS_SUPPOSED = false;
        // }
        //        
        // }
        // } catch (MalformedURLException e) {
        // // e.printStackTrace();
        // } catch (IOException e) {
        // // e.printStackTrace();
        // }
        //        
        // }
        try {
            // if (step == this.steps.firstElement()) this.downloadType =
            // PREMIUM;
            this.setMaxConnections(35);

            String user = null;
            String pass = null;
            String premium = null;
            if (this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
                premium = PROPERTY_USE_PREMIUM;
                user = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_USER);
                pass = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_PASS);
            } else if (this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_2, false)) {
                user = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_USER_2);
                pass = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_PASS_2);
                premium = PROPERTY_USE_PREMIUM_2;
            } else if (this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_3, false)) {
                user = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_USER_3);
                pass = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_PASS_3);
                premium = PROPERTY_USE_PREMIUM_3;
            } else {
                return doFreeStep(step, downloadLink);
            }
            user = JDUtilities.urlEncode(user.trim());
            pass = JDUtilities.urlEncode(pass.trim());
            // String encodePass = rawUrlEncode(pass);

            long headLength;

            String link = downloadLink.getDownloadURL();
            // if (this.getProperties().getBooleanProperty(PROPERTY_USE_SSL,
            // true)) link = link.replaceFirst("http://", "http://ssl.");
            HeadRequest hReq = new HeadRequest(link);
            hReq.load();

            headLength = hReq.getContentLength();
            // if (headLength <= 0 ||
            // hReq.getHttpConnection().getHeaderField("Content-Disposition") ==
            // null) {
            // // requestInfo = HTTP.getRequest(new URL(link), null, "",
            // // false);
            // String page = new GetRequest(link).load();
            // String error = null;
            //
            // if ((error = new Regex(page,
            // PATTERN_FIND_ERROR_CODES).getFirstMatch()) != null) {
            // setError(step, downloadLink, error);
            // return step;
            // }
            // step.setStatus(PluginStep.STATUS_ERROR);
            //
            // downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            // return step;
            //
            // }

            // if
            // (this.getProperties().getBooleanProperty(PROPERTY_FREE_IF_LIMIT_NOT_REACHED,
            // false)) {
            //
            // // get shure that dllink.isInProgress() reacted already on
            // // dl start
            // if (freeInsteadOfPremiumStarttime + 2000 >
            // System.currentTimeMillis()) {
            // logger.info("A download started before -> wait 2 seconds (prevent
            // this by
            // deactivating 'Free download if limit not reached')");
            // Thread.sleep(2000);
            // }
            //
            // if (freeInsteadOfPremiumDownloadlink == null ||
            // !freeInsteadOfPremiumDownloadlink.isInProgress()) {
            //
            // freeInsteadOfPremiumDownloadlink = null;
            // // has to be set here because getRequest takes to much
            // // time
            // freeInsteadOfPremiumStarttime = System.currentTimeMillis();
            //
            // RequestInfo ri = HTTP.getRequest(new URL(link), null, "", false);
            // String error = null;
            //
            // if ((error = new
            // Regex(requestInfo,PATTERN_FIND_ERROR_CODES).getFirstMatch())!=null)
            // {
            // setError(step, downloadLink, error);
            // return step;
            // }
            // String newURL = SimpleMatches.getFirstMatch(ri.getHtmlCode(),
            // PATTERN_FIND_MIRROR_URL, 1);
            // ri = HTTP.postRequest(new URL(newURL), null, null, null,
            // "dl.start=FREE",
            // true);
            // String strWaitTime = new Regex(requestInfo,
            // PATTERN_MATCHER_DOWNLOAD_LIMIT_WAITTIME).getFirstMatch();
            //
            // if
            // (getProperties().getBooleanProperty(PROPERTY_FREE_IF_LIMIT_NOT_REACHED,
            // false) && strWaitTime == null && !new Regex(ri,
            // PATTERM_MATCHER_ALREADY_LOADING).matches()) {
            // // wait time pattern not found -> free download
            // logger.info("Download limit not reached yet -> free download (see
            // RS.com
            // options)");
            // currentStep = steps.firstElement();
            // noLimitFreeInsteadPremium = true;
            //
            // freeInsteadOfPremiumDownloadlink = downloadLink;
            //
            // return doFreeStep(step, downloadLink);
            //
            // } else {
            // logger.info("Download limit reached or free download not possible
            // -> premium
            // download");
            // }
            //
            // } else {
            // logger.info("There is already a running free download -> premium
            // download");
            // }
            //
            // }

            // if
            // (getProperties().getBooleanProperty(Rapidshare.PARAM_FORRCEFREE_WHILE_HAPPYHOURS,
            // false)) {
            // requestInfo = HTTP.getRequest(new URL(link), null, "", false);
            // String newURL =
            // SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(),
            // PATTERN_FIND_MIRROR_URL, 1);
            // requestInfo = HTTP.postRequest(new URL(newURL), null, null, null,
            // "dl.start=FREE", true);
            // if (new Regex(requestInfo,
            // PATTERM_MATCHER_ALREADY_LOADING).matches()) {
            // return doFreeStep(step, downloadLink);
            //
            // }
            //
            // }

            logger.info("Loading from: " + link.substring(0, 30));
            // HashMap<String, String> ranger = new HashMap<String, String>();
            // ranger.put("Authorization", "Basic " +
            // JDUtilities.Base64Encode(user + ":" +
            // pass));
            HTTPConnection urlConnection;
            GetRequest req = new GetRequest(link);

            req.getHeaders().put("Authorization", "Basic " + JDUtilities.Base64Encode(user + ":" + pass));
            req.setFollowRedirects(true);
            // requestInfo = HTTP.getRequestWithoutHtmlCode(new
            // URL(finalURL), finalCookie, finalURL, ranger, true);
            req.connect();
            urlConnection = req.getHttpConnection();
            if (urlConnection.getHeaderField("content-disposition") == null || (Long.parseLong(urlConnection.getHeaderField("Content-Length")) != headLength)) {

                String page = req.read();
                String error;
                if ((error = this.findError(page)) != null) {
                    new File(downloadLink.getFileOutput()).delete();

                    logger.warning(error);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    if (Regex.matches(error, PATTERN_MATCHER_PREMIUM_EXPIRED)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM);
                        step.setParameter(premium);
                        downloadLink.setStatusText(error);
                    } else if (Regex.matches(error, PATTERN_MATCHER_PREMIUM_LIMIT_REACHED)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM);
                        step.setParameter(premium);
                        downloadLink.setStatusText(error);
                    } else {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                        downloadLink.setStatusText(error);
                        step.setParameter(error);
                    }

                    return step;
                } else {
                    new File(downloadLink.getFileOutput()).delete();

                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                    this.reportUnknownError(page, 6);

                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }

            }

            int length = urlConnection.getContentLength();

            downloadLink.setDownloadMax(length);
            String name = getFileNameFormHeader(urlConnection);
            if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
            downloadLink.setName(name);
            dl = new RAFDownload(this, downloadLink, urlConnection);

            dl.setResume(true);
            dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 3));
            if (dl.startDownload()) {
                // Dieses Konto ist am Mon, 2. Jun 2008 abgelaufen

                if (new File(downloadLink.getFileOutput()).length() < 6000) {
                    String page = JDUtilities.getLocalFile(new File(downloadLink.getFileOutput()));
                    if (Regex.matches(page, PATTERN_MATCHER_DOWNLOAD_ERRORPAGE)) {
                        new File(downloadLink.getFileOutput()).delete();

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        String error = findError(page);
                        logger.severe(error);
                        this.reportUnknownError(page, 5);

                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                }

                step.setStatus(PluginStep.STATUS_DONE);
                downloadLink.setStatus(DownloadLink.STATUS_DONE);

                return step;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    // private String getErrorMessage(String page) {
    // String[][] m = new Regex(page, PATTERN_FIND_ERROR_MESSAGE).getMatches();
    // String[] errortext = JDUtilities.splitByNewline(new Regex(page,
    // PATTERN_FIND_ERROR_MESSAGE).getFirstMatch());
    // if (errortext == null || errortext.length <= 0) return "Unknown Error";
    //
    // return JDLocale.L("plugins.host.rapidshare.errors." +
    // JDUtilities.getMD5(errortext[0]), errortext[0]);
    // }

    // private void setError(PluginStep step, DownloadLink downloadLink, String
    // error) {
    // try {
    // int errorid = Integer.parseInt(error);
    // step.setStatus(PluginStep.STATUS_ERROR);
    // downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
    //
    // String errortext = JDUtilities.splitByNewline(new
    // Regex(requestInfo.getHtmlCode(),
    // this.PATTERN_FIND_ERROR_MESSAGE).getFirstMatch())[0];
    // step.setParameter(JDLocale.L("plugins.host.rapidshare.errors." +
    // JDUtilities.getMD5(errortext), errortext));
    //
    // switch (errorid) {
    // case Rapidshare.ERROR_ID_ACCOUNTEXPIRED:
    // downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM);
    //
    // break;
    //
    // default:
    //
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // step.setStatus(PluginStep.STATUS_ERROR);
    // downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
    // return;
    // }
    //
    // step.setStatus(PluginStep.STATUS_ERROR);
    // downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
    //
    // logger.severe("Fehler: " + JDUtilities.splitByNewline(new
    // Regex(requestInfo.getHtmlCode(),
    // this.PATTERN_FIND_ERROR_MESSAGE).getFirstMatch())[0]);
    //
    // }

    /*
     * private String rawUrlEncode(String str) { try { str =
     * URLDecoder.decode(str, "UTF-8"); String ret = ""; int i; for (i = 0; i <
     * str.length(); i++) { char letter = str.charAt(i); ret += "%" +
     * Integer.toString(letter, 16); } return ret; } catch
     * (UnsupportedEncodingException e) { e.printStackTrace(); } return str; }
     */
    @Override
    public boolean doBotCheck(File file) {

        String hash = JDUtilities.getLocalHash(file);
        return hash != null && hash.equals(JDUtilities.getLocalHash(JDUtilities.getResourceFile("jd/captcha/methods/rapidshare.com/bot.jpg")));

    }

    @Override
    public void reset() {
        // waitTime = 500;
        // captchaAddress = null;
        // postTarget = null;
        //
        // postParameter = new HashMap<String, String>();
        // ticketCode = "";
        // noLimitFreeInsteadPremium = false;
        // downloadType = -1;
        // ddl = false;
    }

    public String getFileInformationString(DownloadLink parameter) {
        // if (this.hardewareError) {
        // return "<Hardware Fehler> " +
        // super.getFileInformationString(parameter);
        // } else {
        return super.getFileInformationString(parameter);
        // }
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        if ((System.currentTimeMillis() - LAST_FILE_CHECK) < 250) {
            try {
                Thread.sleep(System.currentTimeMillis() - LAST_FILE_CHECK);
            } catch (InterruptedException e) {
            }
        }
        correctURL(downloadLink);
        LAST_FILE_CHECK = System.currentTimeMillis();
        RequestInfo requestInfo;
        try {
            // http://rapidshare.com/files/117366525/dlc.dlc
            requestInfo = HTTP.getRequest(new URL("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi?urls=" + downloadLink.getDownloadURL() + "&toolmode=1"));

            String[] erg = requestInfo.getHtmlCode().trim().split(",");
            /*
             * 1: Normal online -1: date nicht gefunden 3: Drect download
             */
            if (erg.length < 6 || (!erg[2].equals("1") && !erg[2].equals("3"))) return false;

            downloadLink.setName(erg[5]);
            downloadLink.setDownloadMax(Integer.parseInt(erg[4]));

            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        if (this.getProperties().getBooleanProperty(PARAM_FORRCEFREE_WHILE_HAPPYHOURS, false) && FORCE_FREE_USER) { return 1; }
        int ret = 0;

        if ((((this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM))) || ((this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_2))) || ((this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_3)))) && (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true))) {
            ret = getMaxConnections() / getChunksPerFile();
        } else {
            ret = 1;
        }

        return ret;
    }

    public int getMaxConnections() {
        return 30;
    }

    /**
     * Bietet der hoster eine Möglichkeit mehrere links gleichzeitig zu prüfen,
     * kann das über diese Funktion gemacht werden. Beir s.com istd as derzeitd
     * eaktiviert, weild er Linkchecker nicht mehr über ssl erreichbar ist.
     */
    public boolean[] checkLinks(DownloadLink[] urls) {
        try {
            if (urls == null) return null;
            boolean[] ret = new boolean[urls.length];
            int c = 0;
            while (true) {
                String post = "";
                int i=0;
                for (i = c; i < urls.length; i++) {

                    if (!this.canHandle(urls[i].getDownloadURL())) return null;

                    if (urls[i].getDownloadURL().contains("://ssl.") || !urls[i].getDownloadURL().startsWith("http://rapidshare.com")) {
                        urls[i].setUrlDownload("http://rapidshare.com" + urls[i].getDownloadURL().substring(urls[i].getDownloadURL().indexOf("rapidshare.com") + 14));

                    }
                    if((post+urls[i].getDownloadURL().trim() + "%0a").length()>10000)break;
                    post += urls[i].getDownloadURL().trim() + "%0a";
                    
                }

                PostRequest r = new PostRequest("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi");
                r.setPostVariable("urls", post);
                r.setPostVariable("toolmode", "1");
                String page = r.load();

                String[] lines = SimpleMatches.getLines(page);
                if (lines.length != i-c) return null;
               
                for (String line : lines) {

                    String[] erg = line.split(",");
                    /*
                     * 1: Normal online -1: date nicht gefunden 3: Drect
                     * download
                     */
                    ret[c] = true;
                    if (erg.length < 6 || (!erg[2].equals("1") && !erg[2].equals("3"))) {
                        ret[c] = false;
                    } else {
                        urls[c].setDownloadMax(Integer.parseInt(erg[4]));
                        urls[c].setName(erg[5].trim());
                    }
                    c++;

                }
                if(c>=urls.length){
                    return ret;
                }
                Thread.sleep(400);
            }
          
        } catch (MalformedURLException e) {

            e.printStackTrace();
            return null;
        } catch (Exception e) {

            e.printStackTrace();
            return null;
        }

    }

    public long getBotWaittime() {

        return getProperties().getIntegerProperty(PROPERTY_WAIT_WHEN_BOT_DETECTED, -1);
    }

    public void resetPluginGlobals() {
        super.resetPluginGlobals();
        FORCE_FREE_USER = true;

    }

    @Override
    public String getAGBLink() {
        return "http://rapidshare.com/faq.html";
    }

    private void reportUnknownError(Object req, int id) {
        logger.severe("Unknown error(" + id + "). please add this htmlcode to your bugreport:\r\n" + req);

    }
}
