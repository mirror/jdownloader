//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.host;

//requests RMEIUM:
//#1  (Linkinfos holen, serverID etc
//POST /files/36717968/Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip HTTP/1.1
//Host: rs102.rapidshare.com
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.6) Gecko/20070725 Firefox/2.0.0.6
//Accept: application/x-shockwave-flash,text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Referer: http://rapidshare.com/files/36717968/Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip
//Content-Type: application/x-www-form-urlencoded
//Content-Length: 16
//#2  ((Kann man umgehen)  Hier kann man sich den cookie holen, denn amn dirch eine rawURlEncode Funktion aber nachstellen kann
//POST /cgi-bin/premium.cgi HTTP/1.1
//Host: rs102.rapidshare.com
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.6) Gecko/20070725 Firefox/2.0.0.6
//Accept: application/x-shockwave-flash,text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Referer: http://rs102.rapidshare.com/files/36717968/Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip
//Content-Type: application/x-www-form-urlencoded
//Content-Length: 137
//#3 $p ist das passwort 2 mal rawurlencodet. dieser schritt führt zur serverliste
//POST /files/36717968/Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip HTTP/1.1
//Host: rs102.rapidshare.com
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.6) Gecko/20070725 Firefox/2.0.0.6
//Accept: application/x-shockwave-flash,text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Referer: http://rs102.rapidshare.com/cgi-bin/premium.cgi
//Cookie: user=******-**********
//Content-Type: application/x-www-form-urlencoded
//Content-Length: 119
//
//l=********&p=***********&dl.start=Download+Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip
//#4  entgültoger download
//GET /files/36717968/dl/Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip HTTP/1.1
//Host: rs102tl2.rapidshare.com
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.0; de; rv:1.8.1.6) Gecko/20070725 Firefox/2.0.0.6
//Accept: application/x-shockwave-flash,text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
//Accept-Language: de-de,de;q=0.8,en-us;q=0.5,en;q=0.3
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Referer: http://rs102.rapidshare.com/files/36717968/Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip
//Cookie: user=****-*******
//premiumlogin=1&fileid=36717968&filename=Cash_Flow_Bir_Anlik_Hata__Bandrollu__AraFura.zip&serverid=102&accountid=******&password=******
//dl.start=PREMIUM
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.CaptchaMethodLoader;
import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Rapidshare extends PluginForHost {
    static private final String            host                               = "rapidshare.com";

    private String                         version                            = "1.3.0.1";

    // http://(?:[^.]*\.)*rapidshare\.com/files/[0-9]*/[^\s"]+
    private String                         botHash                            = "63d572beae06a841c23b0d824ac1bfe2";                                                                                          // "dab07d2b7f1299f762454cda4c6143e7";

    /**
     * Vereinfachte Patternerstellung: [*] optionaler Platzhalter [+] musthav
     * platzhalter
     */
    // http://rapidshare.com/files/62495619/toca3.lst
    static private final Pattern           patternSupported                   = Pattern.compile("http://.*?rapidshare\\.com/files/[\\d]{3,9}/.*", Pattern.CASE_INSENSITIVE);

    /**
     * Das findet die Ziel URL für den Post
     */
    private Pattern                        patternForNewHost                  = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");

    /**
     * Das findet die Captcha URL <form *name *= *"dl" (?s).*<img *src *=
     * *"([^\n"]*)">
     */
    private Pattern                        patternForCaptcha                  = Pattern.compile("<form *name *= *\"dl\" (?s).*<img *src *= *\"([^\\n\"]*)\">");

    /**
     * <form name="dl".* action="([^\n"]*)"(?s).*?<input type="submit"
     * name="actionstring" value="[^\n"]*"
     */
    // private Pattern patternForFormData = Pattern.compile("<form name=\"dl\".*
    // action=\"([^\\n\"]*)\"(?s).*?<input type=\"submit\" name=\"actionstring\"
    // value=\"([^\\n\"]*)\"");
    // private Pattern patternForFormData =
    // Pattern.compile("document.dl.action=\'([^\\n\"]*)\"(?s).*?\';document.dl.actionstring.value=\'([^\\n\"]*)\'");
    // private String dataPattern=
    // "document.dl.action=\'°\';document.dl.actionstring.value=\'°\'\">°<br></td></tr></table><h3>Kein
    // Premium-User. Bitte<br>'°'<img src=°><br>hier eingeben: <input
    // type=\"text\" name=\"accesscode\" °size=\"5\" maxlength=\"4\"> <input
    // type=\"submit\" name=\"actionstring\" value=\"°\"></h3></form>";
    private String                         dataPatternPost                    = "<form name=\"dl\" ' +°'action=\"°\" method=\"post\">'";                                                                     // "document.dl.action=°document.dl.actionstring.value";

    private String                         dataPatternAction                  = "name=\"actionstring\" value=\"°\"></h3></form>";

    /**
     * Pattern trifft zu wenn die "Ihre Ip läd gerade eine datei " Seite kommt
     */

    private String                         patternForAlreadyDownloading       = "bereits eine Datei runter";

    /**
     * Muss static bleiben!!!. Das Rapidshare Plugin merkt sich so, dass es
     * gerade wartezeit hat. Überflüssige
     */
    private static long                    END_OF_DOWNLOAD_LIMIT              = 0;

    private static final String            captchaWrong                       = "Access code wrong";

    /**
     * s Das DownloadLimit wurde erreicht (?s)Downloadlimit.*Oder warte ([0-9]+)
     */
    private String                         patternErrorDownloadLimitReached   = "Oder warte ° Minute";

    // private Pattern patternErrorCaptchaWrong = Pattern.compile("(zugriffscode
    // falsch|code wrong)", Pattern.CASE_INSENSITIVE);
    private Pattern                        patternErrorFileAbused             = Pattern.compile("(darf nicht verteilt werden|forbidden to be shared)", Pattern.CASE_INSENSITIVE);

    private Pattern                        patternErrorFileNotFound           = Pattern.compile("(datei nicht gefunden|file not found)", Pattern.CASE_INSENSITIVE);

    private String                         patternForSelectedServer           = "<input checked °actionstring.value=°>°<br>";

    private String                         patternForServer                   = "<input° type=\"radio\" name=\"°\" onclick=\"document.dl.action=°http://°/files/°;document.dl.actionstring.value=°\"> °<br>";

    private String                         ticketWaitTimepattern              = "var c=°;";

    private String                         ticketCodePattern                  = "unescape('°')}";

    private String                         hardwareDefektString               = "wegen Hardwaredefekt nicht";

    private String                         deletedByUploaderString            = "Grund: Vom Uploader";

    private String                         toManyUser                         = "Zu viele Benutzer";

    private String                         notUploaded                        = "Diese Datei ist noch nicht vollst";

    private static final String            PATTERN_ACCOUNT_EXPIRED            = "Dieser Account lief am";

    private static final String            PATTERN_ERROR_BOT                  = "Too many wrong codes";

    private int                            waitTime                           = 500;

    private String                         captchaAddress;

    private String                         postTarget;

    private String                         actionString;

    private HashMap<String, String>        postParameter                      = new HashMap<String, String>();

    private String                         finalURL;

    private static HashMap<String, String> serverMap                          = new HashMap<String, String>();

    private static String[]                serverList1;

    private String                         finalCookie;

    private String[]                       serverList2;

    private boolean                        hardewareError                     = false;

    private String                         ticketCode;

    private String                         newURL;

    private String                         captchaCode;

    private File                           captchaFile;

    private Boolean                        noLimitFreeInsteadPremium          = false;

    private static long                    LAST_FILE_CHECK                    = 0;

    private static final String            PROPERTY_BYTES_TO_LOAD             = "BYTES_TO_LOAD";

    private static final String            PROPERTY_SELECTED_SERVER           = "SELECTED_SERVER";

    private static final String            PROPERTY_SELECTED_SERVER2          = "SELECTED_SERVER#2";

    private static final String            PROPERTY_USE_TELEKOMSERVER         = "USE_TELEKOMSERVER";

    private static final String            PROPERTY_USE_PRESELECTED           = "USE_PRESELECTED";

    private static final String            PROPERTY_USE_SSL                   = "USE_SSL";

    private static final String            PROPERTY_WAIT_WHEN_BOT_DETECTED    = "WAIT_WHEN_BOT_DETECTED";

    private static final String            PROPERTY_INCREASE_TICKET           = "INCREASE_TICKET";

    private static final String            PROPERTY_PREMIUM_USER_2            = "PREMIUM_USER_2";

    private static final String            PROPERTY_PREMIUM_PASS_2            = "PREMIUM_PASS_2";

    private static final String            PROPERTY_USE_PREMIUM_2             = "USE_PREMIUM_2";

    private static final String            PROPERTY_PREMIUM_USER_3            = "PREMIUM_USER_3";

    private static final String            PROPERTY_PREMIUM_PASS_3            = "PREMIUM_PASS_3";

    private static final String            PROPERTY_USE_PREMIUM_3             = "USE_PREMIUM_3";

    private static final String            PROPERTY_FREE_IF_LIMIT_NOT_REACHED = "FREE_IF_LIMIT_NOT_REACHED";

    private static final String            PATTERN_DOWNLOAD_ERRORPAGE         = "RapidShare: 1-Click Webhosting";

    private static final String            PATTERN_ACCOUNT_OVERLOAD           = "runtergeladen und damit das Limit";

    private static int                     ERRORS                             = 0;

    @Override
    public String getCoder() {
        return "astaldo/JD-Team";
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
        // Prüfe auf Wartezeit wg downloadlimit
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // Prüfe Ticket
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // Serverauswahl und captchalden
        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // Downloads
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));

        serverMap.put("TeliaSonera #2", "tl2");// <td><input name="mirror"
        // value="tl2"
        // type="radio">TeliaSonera
        // #2</td>
        serverMap.put("TeliaSonera #3", "tl3");// <td><input name="mirror2"
        // value="tl3"
        // type="radio">TeliaSonera
        // #3</td>

        serverMap.put("GlobalCrossing", "gc"); // <td><input name="mirror"
        // value="gc"
        // type="radio">GlobalCrossing
        // #1</td>
        serverMap.put("GlobalCrossing #2", "gc2"); // <td><input name="mirror"
        // value="gc2"
        // type="radio">GlobalCrossing
        // #2</td>
        serverMap.put("Cogent", "cg"); // td><input name="mirror" value="cg"
        // type="radio">Cogent #1</td>
        serverMap.put("Cogent #2", "cg2");// <td><input name="mirror"
        // value="cg2" type="radio">Cogent
        // #2</td>
        serverMap.put("Teleglobe", "tg"); // <td><input name="mirror"
        // value="tg" type="radio">Teleglobe
        // #1</td>
        serverMap.put("Level(3)", "l3");// <td><input name="mirror" value="l3"
        // type="radio">Level(3) #1</td>
        serverMap.put("Level(3) #2", "l32");// <td><input name="mirror"
        // value="l32" type="radio">Level(3)
        // #2</td>
        serverMap.put("Level(3) #3", "l33");// <td><input name="mirror"
        // value="l33" type="radio">Level(3)
        // #3</td>

        serverMap.put("Level(3) #4", "l34");// <td><input name="mirror"
        // value="l34" type="radio">Level(3)
        // #4</td>
        serverMap.put("TeliaSonera", "tl");// <td><input name="mirror"
        // value="tl"
        // type="radio">TeliaSonera #1</td>
        serverMap.put("Deutsche Telekom", "dt");
        serverList1 = new String[] { "tl", "tl2", "gc", "gc2", "cg", "cg2", "tg", "l3", "l32", "l33", "l34", "tl", "dt" };
        serverList2 = new String[] { "tl", "tl2", "tl3", "gc", "gc2", "l32", "tg", "l3", "cg" };
        this.setConfigElements();
    }

    private String getServerFromAbbreviation(String abb) {
        Iterator<String> iter = serverMap.keySet().iterator();
        Object next;
        while (iter.hasNext()) {
            next = iter.next();
            if (serverMap.get((String) next).equals(abb)) return (String) next;
        }
        return null;
    }

    private String getServerName(String id) {
        Iterator<Entry<String, String>> it = serverMap.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> next = it.next();
            if (next.getValue().equalsIgnoreCase(id)) return next.getKey();
        }
        return null;
    }

    private void setConfigElements() {

        Vector<String> m1 = new Vector<String>();
        Vector<String> m2 = new Vector<String>();
        for (int i = 0; i < serverList1.length; i++)
            m1.add(getServerName(serverList1[i]));
        for (int i = 0; i < serverList2.length; i++)
            m2.add(getServerName(serverList2[i]));
        m1.add("zufällig");
        m2.add("zufällig");
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer", "Bevorzugte Server (*1)")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), PROPERTY_SELECTED_SERVER, m1.toArray(new String[] {}), "#1"));
        cfg.setDefaultValue("Level(3)");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), PROPERTY_SELECTED_SERVER2, m2.toArray(new String[] {}), "#2"));
        cfg.setDefaultValue("TeliaSonera");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_TELEKOMSERVER, JDLocale.L("plugins.hoster.rapidshare.com.telekom", "Telekom Server verwenden falls verfügbar*")));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PRESELECTED, JDLocale.L("plugins.hoster.rapidshare.com.preSelection", "Vorauswahl übernehmen (*2)")));
        cfg.setDefaultValue(true);

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "1. " + JDLocale.L("plugins.hoster.rapidshare.com.premiumAccount", "Premium Account")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, JDLocale.L("plugins.hoster.rapidshare.com.premiumUser", "Premium User")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, JDLocale.L("plugins.hoster.rapidshare.com.premiumPass", "Premium Pass")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, JDLocale.L("plugins.hoster.rapidshare.com.usePremium", "Premium Account verwenden")));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "2. " + JDLocale.L("plugins.hoster.rapidshare.com.premiumAccount", "Premium Account")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER_2, JDLocale.L("plugins.hoster.rapidshare.com.premiumUser2", "Premium User(alternativ)")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS_2, JDLocale.L("plugins.hoster.rapidshare.com.premiumPass2", "Premium Pass(alternativ)")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM_2, JDLocale.L("plugins.hoster.rapidshare.com.usePremium2", "2. Premium Account verwenden")));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "3. " + JDLocale.L("plugins.hoster.rapidshare.com.premiumAccount", "Premium Account")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER_3, JDLocale.L("plugins.hoster.rapidshare.com.premiumUser3", "Premium User(alternativ)")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS_3, JDLocale.L("plugins.hoster.rapidshare.com.premiumPass3", "Premium Pass(alternativ)")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM_3, JDLocale.L("plugins.hoster.rapidshare.com.usePremium3", "3. Premium Account verwenden")));
        cfg.setDefaultValue(false);

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_SSL, JDLocale.L("plugins.hoster.rapidshare.com.useSSL", "SSL Downloadlink verwenden")));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_FREE_IF_LIMIT_NOT_REACHED, JDLocale.L("plugins.hoster.rapidshare.com.freeDownloadIfLimitNotReached", "Free Download wenn Downloadlimit noch nicht erreicht wurde")));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), PROPERTY_WAIT_WHEN_BOT_DETECTED, JDLocale.L("plugins.hoster.rapidshare.com.waitTimeOnBotDetection", "Wartezeit [ms] wenn Bot erkannt wird.(-1 für Reconnect)"), -1, 600000).setDefaultValue(-1).setStep(1000));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), PROPERTY_INCREASE_TICKET, JDLocale.L("plugins.hoster.rapidshare.com.increaseTicketTime", "Ticketwartezeit verlängern (0%-500%)"), 0, 500).setDefaultValue(0).setExpertEntry(true).setStep(1));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), PROPERTY_BYTES_TO_LOAD, JDLocale.L("plugins.hoster.rapidshare.com.loadFirstBytes", "Nur die ersten * KiloBytes jeder Datei laden[-1 to disable]"), -1, 100000).setDefaultValue(-1).setStep(500));
        // cfg.setDefaultValue(true);

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.important", "WICHTIG! ")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.concernsPremiumUser", "(*1)Premiumuser müssen die Bevorzugten Server in den Rapidshare-Online-Optionen (rs.com Premiumbereich) einstellen falls sie Direktlinks aktiviert haben!")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.concernsFreeUser", "(*2)Betrifft nur Freeuser")));

    }

    // @Override
    // public URLConnection getURLConnection() {
    // return null;
    // }
    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        // RequestInfo requestInfo;

        if (step == null) {
            logger.info("Plugin Ende erreicht.");
            return null;
        }
        String link = downloadLink.getDownloadURL();
        if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
            link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);

            downloadLink.setUrlDownload(link);
        }

        logger.info("get Next Step " + step);
        // premium
        PluginStep st;
        if ((this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false) || this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_2, false) || this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_3, false)) && !noLimitFreeInsteadPremium) {
            st = this.doPremiumStep(step, downloadLink);
        }
        else {
            st = this.doFreeStep(step, downloadLink);
        }
        if (st != null && st.getStatus() == PluginStep.STATUS_ERROR) {
            ERRORS++;
        }
        else {
            ERRORS--;
            if (ERRORS < 0) ERRORS = 0;
        }
        // if(ERRORS>5){
        // JDUtilities.getGUI().showMessageDialog(JDLocale.L("plugins.hoster.rapidshare.com.offline",
        // "Keine Internetverbindung vermutet. "));
        // System.exit(1);
        // }
        return st;
    }

    private PluginStep doFreeStep(PluginStep step, DownloadLink downloadLink) {
        if (END_OF_DOWNLOAD_LIMIT > System.currentTimeMillis()) {
            long waitTime = END_OF_DOWNLOAD_LIMIT - System.currentTimeMillis();
            logger.severe("wait (intern) " + waitTime + " minutes");
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
            step.setStatus(PluginStep.STATUS_ERROR);
            logger.info(" WARTEZEIT SETZEN IN " + step + " : " + waitTime);
            step.setParameter((long) waitTime);
            return step;
        }
        logger.info("Intern: " + END_OF_DOWNLOAD_LIMIT + " - " + System.currentTimeMillis());

        switch (step.getStep()) {
            case PluginStep.STEP_WAIT_TIME:
                newURL = null;

                try {
                    if (aborted) {
                        // Häufige abbruchstellen sorgen für einen Zügigen
                        // Downloadstop
                        logger.warning("Plugin abgebrochen");
                        downloadLink.setStatus(DownloadLink.STATUS_TODO);
                        step.setStatus(PluginStep.STATUS_TODO);
                        return step;
                    }
                    // Der Download wird bestätigt
                    String link = downloadLink.getDownloadURL();
                    if (this.getProperties().getBooleanProperty(PROPERTY_USE_SSL, false)) link = link.replaceFirst("http://", "http://ssl.");
                    requestInfo = getRequest(new URL(link));

                    if (requestInfo.getHtmlCode().indexOf(hardwareDefektString) > 0) {
                        // hardewaredefeklt bei rs.com
                        step.setStatus(PluginStep.STATUS_ERROR);
                        step.setParameter(60 * 10);
                        logger.severe("Rs.com hardwaredefekt");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        return step;
                    }
                    if (requestInfo.containsHTML(deletedByUploaderString)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Vom Uploader gelöscht");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        return step;
                    }
                    // falls dei meldung auf der Startseite kommt ist der check
                    // hier
                    // richtig
                    if (requestInfo.containsHTML(toManyUser)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        step.setParameter(60l * 2000l);
                        logger.severe("Rs.com zuviele User");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TO_MANY_USERS);
                        return step;
                    }
                    newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
                    // Fehlerbehandlung auf der ersten Seite
                    if (newURL == null) {
                        String strFileAbused = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileAbused, 0);
                        if (strFileAbused != null) {
                            logger.severe("file abused");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        String strFileNotFound = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileNotFound, 0);
                        if (strFileNotFound != null) {
                            logger.severe("file not found");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }

                        if (requestInfo.containsHTML(notUploaded)) {
                            logger.severe("file not full uploaded");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_UPLOADED);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.warning("could not get downloadInfo ");
                        return step;
                    }
                    return step;
                }catch(SocketTimeoutException e1){
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setParameter(JDLocale.L("gui.status.timeoutdetected","Timeout"));
                    step.setStatus(PluginStep.STATUS_ERROR);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                step.setStatus(PluginStep.STATUS_ERROR);
                logger.warning("could not get downloadInfo ");
                return step;
            case PluginStep.STEP_PENDING:
                try {
                    if (aborted) {
                        logger.warning("Plugin abgebrochen");
                        downloadLink.setStatus(DownloadLink.STATUS_TODO);
                        step.setStatus(PluginStep.STATUS_TODO);
                        return step;
                    }
                    // Auswahl ob free oder prem

                    requestInfo = postRequest(new URL(newURL), null, null, null, "dl.start=free", true);

                    // Falls der check erst nach der free auswahl sein muss,
                    // dann
                    // wäre hier der richtige Platz
                    // Fehlerbehandlung nach free/premium auswahl
                    if (requestInfo.containsHTML(toManyUser)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        step.setParameter(60l * 2000l);
                        logger.severe("Rs.com zuviele User");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TO_MANY_USERS);
                        return step;
                    }

                    if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
                        logger.severe("File already is in progress. " + downloadLink.getFileOutput());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    if (new File(downloadLink.getFileOutput()).exists()) {
                        logger.severe("File already exists. " + downloadLink.getFileOutput());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    String strWaitTime = getSimpleMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 0);
                    if (strWaitTime != null) {

                        logger.severe("wait " + strWaitTime + " minutes");
                        waitTime = (int) (Double.parseDouble(strWaitTime) * 60 * 1000);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        END_OF_DOWNLOAD_LIMIT = System.currentTimeMillis() + waitTime;
                        logger.info("Wait until: " + System.currentTimeMillis() + "+ " + waitTime + " = " + END_OF_DOWNLOAD_LIMIT);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.info(" WARTEZEIT SETZEN IN " + step + " : " + waitTime);
                        step.setParameter((long) waitTime);
                        return step;
                    }
                    // String strCaptchaWrong =
                    // getFirstMatch(requestInfo.getHtmlCode(),
                    // patternErrorCaptchaWrong, 0);
                    // if (strCaptchaWrong != null) {
                    // logger.severe("captchaWrong");
                    // downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    // step.setStatus(PluginStep.STATUS_ERROR);
                    // return step;
                    // }
                    if (requestInfo.containsHTML(patternForAlreadyDownloading)) {
                        logger.severe("Already Loading wait " + 180 + " sek. to Retry");

                        waitTime = 180 * 1000;
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_STATIC_WAITTIME);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        END_OF_DOWNLOAD_LIMIT = System.currentTimeMillis() + waitTime;
                        logger.info("Wait until: " + System.currentTimeMillis() + "+ " + waitTime + " = " + END_OF_DOWNLOAD_LIMIT);
                        logger.info(" WARTEZEIT SETZEN IN (already loading)" + step + " : " + waitTime);
                        step.setParameter((long) waitTime);
                        return step;
                    }
                    String wait = getSimpleMatch(requestInfo.getHtmlCode(), ticketWaitTimepattern, 0);
                    ticketCode = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), ticketCodePattern, 0));
                    ticketCode = requestInfo.getHtmlCode() + " " + ticketCode;
                    captchaAddress = getFirstMatch(ticketCode, patternForCaptcha, 1);

                    if (captchaAddress == null) {
                        logger.severe("Captcha Address not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    this.captchaFile = this.getLocalCaptchaFile(this);
                    if (!JDUtilities.download(captchaFile, captchaAddress) || !captchaFile.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                    }
                    long timer = System.currentTimeMillis();

                    if (doBotCheck(captchaFile)) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        step.setParameter(null);
                        break;
                    }
                    this.captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                    timer = System.currentTimeMillis() - timer;
                    logger.info("captcha detection: " + timer + " ms");

                    if (wait != null) {
                        long pendingTime = Long.parseLong(wait);

                        if (getProperties().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) > 0) {
                            logger.warning("Waittime increased by JD: " + waitTime + " --> " + (pendingTime + (getProperties().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime) / 100));
                        }
                        pendingTime = (pendingTime + (getProperties().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime) / 100);

                        logger.info("Ticket: wait " + pendingTime + " seconds");

                        step.setParameter(pendingTime * 1000 - timer);
                        return step;

                    }
                    else {
                        // TODO: Gibt es file sbei denen es kein Ticket gibt?
                        logger.finer("Kein Ticket gefunden. fahre fort");
                        ticketCode = requestInfo.getHtmlCode();

                        step.setParameter(10l);
                        return step;
                    }
                
        }catch(SocketTimeoutException e1){
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            step.setParameter(JDLocale.L("gui.status.timeoutdetected","Timeout"));
            step.setStatus(PluginStep.STATUS_ERROR);
        }
                catch (Exception e) {
                    e.printStackTrace();
                }
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                step.setStatus(PluginStep.STATUS_ERROR);
                logger.warning("could not get downloadInfo 2");
                return step;
            case PluginStep.STEP_PAGE:
                String server1 = this.getProperties().getStringProperty(PROPERTY_SELECTED_SERVER, "Level(3)");
                String server2 = this.getProperties().getStringProperty(PROPERTY_SELECTED_SERVER2, "TeliaSonera");
                String serverAbb = serverMap.get(server1);
                String server2Abb = serverMap.get(server2);
                logger.info("Servermap: " + serverMap);
                logger.info("Servers settings: " + server1 + "-" + server2 + " : " + serverAbb + "-" + server2Abb);
                if (serverAbb == null) {
                    serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
                    logger.finer(" Use Random #1 server " + serverAbb);
                }
                if (server2Abb == null) {
                    server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
                    logger.finer("Use Random #2 server " + server2Abb);
                }
                // String endServerAbb = "";
                Boolean telekom = !(this.getProperties().getProperty(PROPERTY_USE_TELEKOMSERVER) == null || !(Boolean) this.getProperties().getProperty(PROPERTY_USE_TELEKOMSERVER));
                boolean preselected = this.getProperties().getBooleanProperty(PROPERTY_USE_PRESELECTED, true);

                // post daten lesen
                // postTarget = getFirstMatch(ticketCode, patternForFormData,
                // 1);
                // actionString = getFirstMatch(ticketCode, patternForFormData,
                // 2);

                // postTarget=this.getSimpleMatch(ticketCode, dataPattern, 0);
                // actionString=this.getSimpleMatch(ticketCode, dataPattern, 1);
                postTarget = getSimpleMatch(ticketCode, dataPatternPost, 1);
                actionString = getSimpleMatch(ticketCode, dataPatternAction, 0);

                if (postTarget == null) {
                    logger.severe("postTarget not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                // postTarget=postTarget.substring(2, postTarget.length()-3);
                // logger.info(postTarget+" -"+actionString);
                if (actionString == null) {
                    logger.severe("actionString not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                // Vector<String> serverids = getAllSimpleMatches(ticketCode,
                // patternForServer, 3);
                Vector<String> serverstrings = getAllSimpleMatches(ticketCode, patternForServer, 7);
                logger.info(serverstrings + "");

                // logger.info(ticketCode);
                logger.info("wished Mirror #1 Server " + serverAbb);
                logger.info("wished Mirror #2 Server " + server2Abb);
                String selected = getSimpleMatch(ticketCode, patternForSelectedServer, 2);
                logger.info("Preselected Server: " + selected);
                if (preselected) {
                    logger.info("RS.com-free Use preselected : " + selected);
                    actionString = selected;
                }
                else if (telekom && ticketCode.indexOf("td.rapidshare.com") >= 0) {
                    actionString = "Download via Deutsche Telekom.";
                    logger.info("RS.com-free Use Telekom Server");
                }
                else if (ticketCode.indexOf(serverAbb + ".rapidshare.com") >= 0) {
                    logger.info("RS.com-free Use Mirror #1 Server: " + getServerFromAbbreviation(serverAbb));
                    actionString = "Download via " + getServerFromAbbreviation(serverAbb);
                }
                else if (ticketCode.indexOf(server2Abb + ".rapidshare.com") >= 0) {
                    logger.info("RS.com-free Use Mirror #2 Server: " + getServerFromAbbreviation(server2Abb));
                    actionString = "Download via " + getServerFromAbbreviation(server2Abb);
                }
                else if (serverstrings.size() > 0) {
                    actionString = serverstrings.get((int) Math.ceil(Math.random() * serverstrings.size()) - 1);
                    logger.info("RS.com-free Use Errer random Server: " + actionString);
                }
                else {
                    logger.severe("Kein Server gefunden");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return null;
                }
                downloadLink.setStatusText(actionString);

                break;
            case PluginStep.STEP_DOWNLOAD:

                actionString = actionString.replace(' ', '+');
                postParameter.put("mirror", "on");
                postParameter.put("accesscode", this.captchaCode);
                postParameter.put("actionstring", actionString);
                try {

                    HTTPConnection urlConnection = new HTTPConnection(new URL(postTarget).openConnection());
                    urlConnection.setDoOutput(true);
                    // Post Parameter vorbereiten
                    String postParams = createPostParameterFromHashMap(postParameter);

                    postParams = "mirror=on&accesscode=" + captchaCode + "&actionstring=" + actionString;

                    OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                    wr.write(postParams);
                    wr.flush();
                    // content-disposition: Attachment;
                    // filename=a_mc_cs3_g_cd.rsdf

                    String name = getFileNameFormHeader(urlConnection);
                    if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
                    downloadLink.setName(name);
                    int length = urlConnection.getContentLength();
                    downloadLink.setDownloadMax(length);
                    if (!hasEnoughHDSpace(downloadLink)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    Set<Entry<String, String>> entries = serverMap.entrySet();
                    logger.info("link: " + postTarget.substring(0, 30) + " " + actionString);
                    Iterator<Entry<String, String>> it = entries.iterator();
                    while (it.hasNext()) {
                        Entry<String, String> entry = it.next();
                        int i;
                        if ((i = postTarget.indexOf(entry.getValue())) < 20 && i > 0) {
                            logger.info(JDUtilities.htmlDecode(actionString.split("via")[1].trim()).trim());
                            postTarget = postTarget.substring(0, i) + serverMap.get(JDUtilities.htmlDecode(actionString.split("via")[1].trim()).trim()) + postTarget.substring(i + entry.getValue().length());
                            break;
                        }
                    }
                    logger.info("link: " + postTarget.substring(0, 30) + " " + actionString);

                    Download dl = new Download(this, downloadLink, urlConnection);
                    if (getProperties().getIntegerProperty(PROPERTY_BYTES_TO_LOAD, -1) > 0) {
                        dl.setMaxBytesToLoad(1024 * getProperties().getIntegerProperty(PROPERTY_BYTES_TO_LOAD, -1));
                    }
                    ;

                    if (dl.startDownload()) {
                        if (new File(downloadLink.getFileOutput()).length() < 4000 && JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())).indexOf(captchaWrong) > 0) {
                            new File(downloadLink.getFileOutput()).delete();
                            JDUtilities.appendInfoToFilename(this, captchaFile, actionString + "_" + captchaCode, false);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                            logger.info("Error detected. Update captchafile");
                            new CaptchaMethodLoader().interact("rapidshare.com");
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        if (new File(downloadLink.getFileOutput()).length() < 4000 && JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())).indexOf(PATTERN_ERROR_BOT) > 0) {
                            new File(downloadLink.getFileOutput()).delete();

                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_BOT_DETECTED);
                            logger.info("Error detected. Bot detected");

                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        if (new File(downloadLink.getFileOutput()).length() < 4000 && JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())).indexOf(PATTERN_DOWNLOAD_ERRORPAGE) > 0) {

                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                            downloadLink.setStatusText("Download error(>log)");

                            logger.severe("Error detected. " + JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())));
                            new File(downloadLink.getFileOutput()).delete();
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }

                        JDUtilities.appendInfoToFilename(this, captchaFile, actionString + "_" + captchaCode, true);

                        return null;
                    }
                }catch(SocketTimeoutException e1){
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setParameter(JDLocale.L("gui.status.timeoutdetected","Timeout"));
                    step.setStatus(PluginStep.STATUS_ERROR);
                }
              
                catch (IOException e) {
                    logger.severe("URL could not be opened. " + e.toString());
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }

                break;
        }
        return step;
    }

    private PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) {
        String server1 = this.getProperties().getStringProperty(PROPERTY_SELECTED_SERVER, "Level(3)");
        String server2 = this.getProperties().getStringProperty(PROPERTY_SELECTED_SERVER2, "TeliaSonera");
        String serverAbb = serverMap.get(server1);
        String server2Abb = serverMap.get(server2);

        logger.info("Servermap: " + serverMap);
        logger.info("Servers settings: " + server1 + "-" + server2 + " : " + serverAbb + "-" + server2Abb);

        if (serverAbb == null) {
            serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
            logger.finer(" Use Random #1 server " + serverAbb);
        }
        if (server2Abb == null) {
            server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
            logger.finer("Use Random #2 server " + server2Abb);
        }
        // String endServerAbb = "";
        Boolean telekom = !(this.getProperties().getProperty(PROPERTY_USE_TELEKOMSERVER) == null || !(Boolean) this.getProperties().getProperty(PROPERTY_USE_TELEKOMSERVER));

        String user = null;
        String pass = null;
        String premium = null;
        if (this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            premium = PROPERTY_USE_PREMIUM;
            user = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_USER);
            pass = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_PASS);
        }
        else if (this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_2, false)) {
            user = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_USER_2);
            pass = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_PASS_2);
            premium = PROPERTY_USE_PREMIUM_2;
        }
        else if (this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM_3, false)) {
            user = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_USER_3);
            pass = (String) this.getProperties().getProperty(PROPERTY_PREMIUM_PASS_3);
            premium = PROPERTY_USE_PREMIUM_3;
        }
        else {
            return doFreeStep(step, downloadLink);
        }
        // String encodePass = rawUrlEncode(pass);
        switch (step.getStep()) {
            case PluginStep.STEP_WAIT_TIME:
                try {
                    // get Startseite
                    // public static RequestInfo getRequest(URL link, String
                    // cookie, String referrer, boolean redirect) throws
                    // IOException {
                    String link = downloadLink.getDownloadURL();
                    if (this.getProperties().getBooleanProperty(PROPERTY_USE_SSL, true)) link = link.replaceFirst("http://", "http://ssl.");
                    requestInfo = getRequest(new URL(link), null, "", false);
                    if (requestInfo.getHtmlCode().indexOf(hardwareDefektString) > 0) {
                        // hardewaredefeklt bei rs.com
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Rs.com hardwaredefekt");
                        step.setParameter(60 * 10);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        return step;
                    }
                    if (requestInfo.containsHTML(deletedByUploaderString)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Vom Uploader gelöscht");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        return step;
                    }
                    String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
                    if (newURL != null) {
                        if (aborted) {
                            // Häufige abbruchstellen sorgen für einen
                            // Zügigen Downloadstop
                            logger.warning("Plugin abgebrochen");
                            downloadLink.setStatus(DownloadLink.STATUS_TODO);
                            step.setStatus(PluginStep.STATUS_TODO);
                            return step;
                        }
                        if (this.getProperties().getBooleanProperty(PROPERTY_FREE_IF_LIMIT_NOT_REACHED, false)) {
                            requestInfo = postRequest(new URL(newURL), null, null, null, "dl.start=FREE", true);
                            String strWaitTime = getSimpleMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 0);
                            // wait time pattern not found -> free download
                            if (strWaitTime == null && !requestInfo.containsHTML(patternForAlreadyDownloading) && !requestInfo.containsHTML(toManyUser)) {
                                logger.info("Download limit not reached yet -> free download");
                                currentStep = steps.firstElement();
                                noLimitFreeInsteadPremium = true;
                                return doFreeStep(step, downloadLink);
                            }
                            else
                                logger.info("Download limit reached or free download not possible -> premium download");
                        }
                        // Auswahl ob free oder prem
                        requestInfo = postRequest(new URL(newURL), null, null, null, "dl.start=PREMIUM", true);
                        // post daten lesen
                        HashMap<String, String> fields = getInputHiddenFields(requestInfo.getHtmlCode(), "premium.cgi", "submit");
                        // Part sollte mal drin bleiben. Der gehört zum
                        // normalen USer request dazu. lässt sich aber
                        // umgehen
                        String post = joinMap(fields, "=", "&") + "&accountid=" + JDUtilities.urlEncode(user) + "&password=" + JDUtilities.urlEncode(pass);
                        // Login
                        String url;
                        if (fields.get("serverid") == null) {
                            url = "http://rapidshare.com/cgi-bin/premium.cgi";
                        }
                        else {
                            url = "http://rs" + fields.get("serverid") + ".rapidshare.com/cgi-bin/premium.cgi";
                        }

                        if (aborted) {
                            logger.warning("Plugin abgebrochen");
                            downloadLink.setStatus(DownloadLink.STATUS_TODO);
                            step.setStatus(PluginStep.STATUS_TODO);
                            return step;
                        }
                        requestInfo = postRequest(new URL(url), post);

                        String cookie = requestInfo.getCookie();
                        // String cookie = "user=" + calcCookie;
                        HashMap<String, String> fields2 = getInputHiddenFields(requestInfo.getHtmlCode(), "Cookie", "wrapper");
                        if (fields2.get("p") == null || fields2.get("l") == null) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            logger.severe("Premiumfehler Login");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                            step.setParameter("Login Error: " + user);
                            getProperties().setProperty(premium, false);
                            return step;
                        }
                        this.finalCookie = cookie;

                        post = "l=" + fields2.get("l") + "&p=" + fields2.get("p").replaceAll("\\%", "%25") + "&dl.start=Download+" + fields.get("filename").replaceAll(" ", "+");
                        url = "http://rs" + fields.get("serverid") + ".rapidshare.com/files" + "/" + fields.get("fileid") + "/" + fields.get("filename");

                        requestInfo = postRequestWithoutHtmlCode(new URL(url), cookie, url, post, true);
                        HashMap<String, String> fields3 = getInputHiddenFields(requestInfo.getHtmlCode(), "Cookie", "wrapper");
                        post = joinMap(fields3, "=", "&");
                        if (!requestInfo.isOK()) {
                            logger.info("Kein passender Server gefunden");
                            logger.warning("Plugin abgebrochen");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        // Direktlinks sind aktiv.
                        if (requestInfo.getConnection().getHeaderField("Content-Type").equalsIgnoreCase("application/octet-stream")) {
                            logger.info("Direkt Links ist aktiv");
                        }
                        else {

                            logger.info("Direkt Links ist NICHT aktiv");
                            if (aborted) {
                                logger.warning("Plugin abgebrochen");
                                downloadLink.setStatus(DownloadLink.STATUS_TODO);
                                step.setStatus(PluginStep.STATUS_TODO);
                                return step;
                            }
                            try {
                                requestInfo = readFromURL(requestInfo.getConnection());
                            }
                            catch (Exception e) {

                            }
                            if (requestInfo.containsHTML(PATTERN_ACCOUNT_OVERLOAD)) {
                                logger.severe("Premium Account overload");
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                                step.setParameter("Premium overload>25GB");
                                step.setStatus(PluginStep.STATUS_ERROR);
                                getProperties().setProperty(premium, false);
                                return step;
                            }
                            // logger.info(requestInfo.getHeaders() + " - " +
                            // requestInfo.getHtmlCode());
                            if (requestInfo.containsHTML(PATTERN_ACCOUNT_EXPIRED)) {
                                logger.severe("Premium Account abgelaufen");
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                                step.setParameter("Premium Acc. expired");
                                step.setStatus(PluginStep.STATUS_ERROR);
                                getProperties().setProperty(premium, false);
                                return step;

                            }
                            // <a
                            // href="http://rs214cg.rapidshare.com/files/50231143/dl/Discovery.rar">Download
                            // via Cogent</a><br>

                            Vector<String> urlStrings = getAllSimpleMatches(requestInfo.getHtmlCode(), "<a href=\"http://rs°\">Download via °</a><br>", 1);

                            logger.info("wished Mirror #1 Server " + serverAbb);
                            logger.info("wished Mirror #2 Server " + server2Abb);
                            url = null;
                            if (telekom) {
                                for (int i = 0; i < urlStrings.size(); i++) {
                                    if (urlStrings.get(i).indexOf("td.rapidshare.com") > 0) {
                                        url = "http://rs" + urlStrings.get(i);
                                        break;
                                    }
                                }
                            }
                            if (url == null) {
                                for (int i = 0; i < urlStrings.size(); i++) {
                                    if (urlStrings.get(i).indexOf(serverAbb + ".rapidshare.com") > 0) {
                                        url = "http://rs" + urlStrings.get(i);
                                        logger.finer("Found #1 server: " + url.substring(0, 30));
                                        break;
                                    }
                                }
                            }
                            if (url == null) {
                                for (int i = 0; i < urlStrings.size(); i++) {
                                    if (urlStrings.get(i).indexOf(server2Abb + ".rapidshare.com") > 0) {
                                        url = "http://rs" + urlStrings.get(i);
                                        logger.finer("Found #2 server: " + url.substring(0, 30));
                                        break;
                                    }
                                }
                            }
                            if (url == null && urlStrings.size() > 0) {
                                url = "http://rs" + urlStrings.get((int) Math.ceil(Math.random() * urlStrings.size()) - 1);
                                logger.finer("RS.com Use Error random Server: " + url.substring(0, 30));
                            }
                            if (url == null) {
                                logger.severe("Kein Server gefunden");
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                                step.setStatus(PluginStep.STATUS_ERROR);
                                return step;
                            }
                        }
                        // logger.info("final " + url);
                        downloadLink.setStatusText("Server: " + url.substring(8, 14));
                        this.finalURL = url;
                    }
                    else {
                        String strFileAbused = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileAbused, 0);
                        if (strFileAbused != null) {
                            logger.severe("file abused");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        String strFileNotFound = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileNotFound, 0);
                        if (strFileNotFound != null) {
                            logger.severe("file not found");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.warning("could not get downloadInfo ");
                        return step;
                    }
                }catch(SocketTimeoutException e1){
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    step.setParameter(JDLocale.L("gui.status.timeoutdetected","Timeout"));
                    step.setStatus(PluginStep.STATUS_ERROR);
                }catch (Exception e) {
                    e.printStackTrace();
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                }
                break;
            case PluginStep.STEP_PENDING:
                step.setStatus(PluginStep.STATUS_SKIP);
                downloadLink.setStatusText("Premiumdownload");
                step = nextStep(step);
            case PluginStep.STEP_GET_CAPTCHA_FILE:
                // schritt überspringen
                step.setStatus(PluginStep.STATUS_SKIP);
                downloadLink.setStatusText("Premiumdownload");
                step = nextStep(step);
            case PluginStep.STEP_DOWNLOAD:
                try {
                    if (aborted) {
                        // Häufige abbruchstellen sorgen für einen Zügigen
                        // Downloadstop
                        logger.warning("Plugin abgebrochen");
                        downloadLink.setStatus(DownloadLink.STATUS_TODO);
                        step.setStatus(PluginStep.STATUS_TODO);
                        return step;
                    }
                    logger.info("Loading from: " + finalURL.substring(0, 30));
                    HashMap<String, String> ranger = new HashMap<String, String>();
                    HTTPConnection urlConnection;

                    requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), finalCookie, finalURL, ranger, true);
                    urlConnection = requestInfo.getConnection();
                    int length = urlConnection.getContentLength();

                    downloadLink.setDownloadMax(length);
                    String name = getFileNameFormHeader(urlConnection);
                    if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
                    downloadLink.setName(name);
                    Download dl = new Download(this, downloadLink, urlConnection);
                    dl.setChunks(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS,3));
                    dl.startDownload();
                    if (dl.getErrors().size() == 0) { 
                        if (new File(downloadLink.getFileOutput()).length() < 4000 && JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())).indexOf(PATTERN_DOWNLOAD_ERRORPAGE) > 0) {
                            new File(downloadLink.getFileOutput()).delete();

                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                            downloadLink.setStatusText("Download error(>log)");

                            logger.severe("Error detected.  " + JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())));

                            step.setStatus(PluginStep.STATUS_ERROR);
                            return null;
                        }

                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                        return null;
                    }

                    return null;
                }

                catch (IOException e) {
                    logger.severe("URL could not be opened. " + e.toString());
                }
        }
        return step;
    }

    /*
     * private String rawUrlEncode(String str) { try { str =
     * URLDecoder.decode(str, "UTF-8"); String ret = ""; int i; for (i = 0; i <
     * str.length(); i++) { char letter = str.charAt(i); ret += "%" +
     * Integer.toString(letter, 16); } return ret; } catch
     * (UnsupportedEncodingException e) { e.printStackTrace(); } return str; }
     */
    @Override
    public boolean doBotCheck(File file) {
        try {

            return md5sum(file).equals(botHash);
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void reset() {
        waitTime = 500;
        captchaAddress = null;
        postTarget = null;
        actionString = null;
        postParameter = new HashMap<String, String>();
        ticketCode = "";
        noLimitFreeInsteadPremium = false;
    }

    public String getFileInformationString(DownloadLink parameter) {
        if (this.hardewareError) {
            return "<Hardware Fehler> " + super.getFileInformationString(parameter);
        }
        else {
            return super.getFileInformationString(parameter);
        }
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        // Der Download wird bestätigt
        if ((System.currentTimeMillis() - LAST_FILE_CHECK) < 500) {
            try {
                Thread.sleep(System.currentTimeMillis() - LAST_FILE_CHECK);
            }
            catch (InterruptedException e) {
            }
        }
        LAST_FILE_CHECK = System.currentTimeMillis();
        RequestInfo requestInfo;
        try {
            String link = downloadLink.getDownloadURL();
            if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
                link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);
                logger.finer("URL korrigiert: " + link);
                downloadLink.setUrlDownload(link);
            }
            String name = downloadLink.getName();
            if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
            downloadLink.setName(name);
            if (this.getProperties().getBooleanProperty(PROPERTY_USE_SSL, false)) link = link.replaceFirst("http://", "http://ssl.");
            requestInfo = getRequest(new URL(link));

            if (requestInfo.getHtmlCode().indexOf(hardwareDefektString) > 0) {
                this.hardewareError = true;
                this.setStatusText("Hareware Error");
                downloadLink.setStatusText("Hareware Error");
                return false;
            }
            if (requestInfo.getConnection().getHeaderField("Location") != null) {

                return true;
            }

            String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
            String strWaitTime = getSimpleMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 0);
            if (newURL == null && strWaitTime == null) {

                return false;
            }

            requestInfo = postRequest(new URL(newURL), null, null, null, "dl.start=free", true);
            LAST_FILE_CHECK = System.currentTimeMillis();
            String size = getSimpleMatch(requestInfo.getHtmlCode(), "</font> (<b>°</b> °) angefordert.", 0);
            String type = getSimpleMatch(requestInfo.getHtmlCode(), "</font> (<b>°</b> °) angefordert.", 1);

            if (size == null || type == null) {
                return false;
            }
            int bytes;
            if (type.equalsIgnoreCase("kb")) {
                bytes = Integer.parseInt(size) * 1024;

            }
            else if (type.equalsIgnoreCase("mb")) {
                bytes = Integer.parseInt(size) * 1024 * 1024;

            }
            else {
                bytes = Integer.parseInt(size);
            }
            downloadLink.setDownloadMax(bytes);
            return true;
        }
        catch (MalformedURLException e) {
        }
        catch (IOException e) {
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        int ret = 0;
        if ((this.getProperties().getProperty(PROPERTY_USE_PREMIUM) != null && ((Boolean) this.getProperties().getProperty(PROPERTY_USE_PREMIUM))) || (this.getProperties().getProperty(PROPERTY_USE_PREMIUM_2) != null && ((Boolean) this.getProperties().getProperty(PROPERTY_USE_PREMIUM_2))) || (this.getProperties().getProperty(PROPERTY_USE_PREMIUM_3) != null && ((Boolean) this.getProperties().getProperty(PROPERTY_USE_PREMIUM_3)))) {
            ret = 25;
        }
        else {
            ret = 1;
        }

        return ret;
    }

    public long getBotWaittime() {

        return getProperties().getIntegerProperty(PROPERTY_WAIT_WHEN_BOT_DETECTED, -1);
    }

    @Override
    public void resetPluginGlobals() {
        END_OF_DOWNLOAD_LIMIT = 0;

    }

    @Override
    public String getAGBLink() {

        return "http://rapidshare.com/faq.html";
    }
}
