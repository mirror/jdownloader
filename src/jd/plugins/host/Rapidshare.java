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
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Rapidshare extends PluginForHost {
    static private final String            host                             = "rapidshare.com";

    private String                         version                          = "1.2.0.0";

    // http://(?:[^.]*\.)*rapidshare\.com/files/[0-9]*/[^\s"]+
    private String                         botHash                          = "dab07d2b7f1299f762454cda4c6143e7";

    /**
     * Vereinfachte Patternerstellung: [*] optionaler Platzhalter [+] musthav
     * platzhalter
     */
    // http://rapidshare.com/files/62495619/toca3.lst
    static private final Pattern           patternSupported                 = Pattern.compile("http://.*?rapidshare\\.com/files/[\\d]{7,9}/.*", Pattern.CASE_INSENSITIVE);

    /**
     * Das findet die Ziel URL für den Post
     */
    private Pattern                        patternForNewHost                = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");

    /**
     * Das findet die Captcha URL <form *name *= *"dl" (?s).*<img *src *=
     * *"([^\n"]*)">
     */
    private Pattern                        patternForCaptcha                = Pattern.compile("<form *name *= *\"dl\" (?s).*<img *src *= *\"([^\\n\"]*)\">");

    /**
     * <form name="dl".* action="([^\n"]*)"(?s).*?<input type="submit"
     * name="actionstring" value="[^\n"]*"
     */
    private Pattern                        patternForFormData               = Pattern.compile("<form name=\"dl\".* action=\"([^\\n\"]*)\"(?s).*?<input type=\"submit\" name=\"actionstring\" value=\"([^\\n\"]*)\"");

    /**
     * Pattern trifft zu wenn die "Ihre Ip läd gerade eine datei " Seite kommt
     */

    private String                         patternForAlreadyDownloading     = "bereits eine Datei runter";

    /**
     * Muss static bleiben!!!. Das Rapidshare Plugin merkt sich so, dass es
     * gerade wartezeit hat. Überflüssige
     */
    private static long                    END_OF_DOWNLOAD_LIMIT            = 0;

    /**
     * Das DownloadLimit wurde erreicht (?s)Downloadlimit.*Oder warte ([0-9]+)
     */
    private Pattern                        patternErrorDownloadLimitReached = Pattern.compile("\\((?:oder warte|or wait) ([0-9]*) (?:minuten|minute)\\)", Pattern.CASE_INSENSITIVE);

    // private Pattern patternErrorCaptchaWrong = Pattern.compile("(zugriffscode
    // falsch|code wrong)", Pattern.CASE_INSENSITIVE);
    private Pattern                        patternErrorFileAbused           = Pattern.compile("(darf nicht verteilt werden|forbidden to be shared)", Pattern.CASE_INSENSITIVE);

    private Pattern                        patternErrorFileNotFound         = Pattern.compile("(datei nicht gefunden|file not found)", Pattern.CASE_INSENSITIVE);

    private String                         patternForSelectedServer         = "<input checked type=\"radio\" name=\"°\" onclick=\"document.dl.action='http://°/files/°';document.dl.actionstring.value='°'\"> °<br>";

    private String                         patternForServer                 = "<input°type=\"radio\" name=\"°\" onclick=\"document.dl.action='http://°/files/°';document.dl.actionstring.value='°'\"> °<br>";

    private String                         ticketWaitTimepattern            = "var c=°;";

    private String                         ticketCodePattern                = "unescape('°')}";

    private String                         hardwareDefektString             = "wegen Hardwaredefekt nicht";

    private String                         deletedByUploaderString          = "Grund: Vom Uploader";

    private String                         toManyUser                       = "Zu viele Benutzer";

    private String                         notUploaded                      = "Diese Datei ist noch nicht vollst";

    private int                            waitTime                         = 500;

    private String                         captchaAddress;

    private String                         postTarget;

    private String                         actionString;

    private HashMap<String, String>        postParameter                    = new HashMap<String, String>();

    private String                         finalURL;

    private static HashMap<String, String> serverMap                        = new HashMap<String, String>();

    private static String[]                serverList1;

    private String                         finalCookie;

    private String[]                       serverList2;

    private boolean                        hardewareError                   = false;

    private String                         ticketCode;

    private String                         newURL;

    private static final String            PROPERTY_BYTES_TO_LOAD           = "BYTES_TO_LOAD";

    @Override
    public String getCoder() {
        return "astaldo/coalado";
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
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // Downloads
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        this.setConfigElements();
        serverMap.put("TeliaSonera", "tl");
        serverMap.put("TeliaSonera #2", "tl2");
        serverMap.put("GlobalCrossing", "gc");
        serverMap.put("GlobalCrossing #2", "gc2");
        serverMap.put("Cogent", "cg");
        serverMap.put("Cogent #2", "cg2");
        serverMap.put("Teleglobe", "tg");
        serverMap.put("Level(3)", "l3");
        serverMap.put("Level(3) #2", "l32");
        serverMap.put("Level(3) #3", "l33");
        serverMap.put("Level(3) #4", "l34");
        serverMap.put("TeliaSonera", "tl");
        serverMap.put("Deutsche Telekom", "dt");
        serverList1 = new String[] { "tl", "tl2", "gc", "gc2", "cg", "cg2", "tg", "l3", "l32", "l33", "l34", "tl", "dt" };
        serverList2 = new String[] { "tl", "gc" };
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

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Bevorzugte Server (*1)"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), "SELECTED_SERVER", new String[] { "Cogent", "Cogent #2", "GlobalCrossing", "GlobalCrossing #2", "TeliaSonera", "TeliaSonera #2", "Teleglobe", "Level (3)", "Level (3) #2", "Level (3) #3", "Level (3) #4", "zufällig" }, "#1"));
        cfg.setDefaultValue("Level (3)");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), "SELECTED_SERVER#2", new String[] { "Cogent", "TeliaSonera", "Level (3)", "GlobalCrossing", "zufällig" }, "#2"));
        cfg.setDefaultValue("TeliaSonera");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_TELEKOMSERVER", "Telekom Server verwenden falls verfügbar*"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_PRESELECTED", "Vorauswahl übernehmen (*2)"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Premium Accounts"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, "Premium User"));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_PASS, "Premium Pass"));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, "Premium Account verwenden"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "WICHTIG! "));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "(*1)Premiumuser müssen die Bevorzugten Server in den Rapidshare-Online-Optionen (rs.com Premiumbereich) einstellen falls sie Direktlinks aktiviert haben!"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "(*2)Betrifft nur Freeuser"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), PROPERTY_BYTES_TO_LOAD, "Nur die ersten * KiloBytes jeder Datei laden[-1 to disable]", -1, 100000).setDefaultValue(-1).setStep(500));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_SSL", "SSL Downloadlink verwenden"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), "WAIT_WHEN_BOT_DETECTED", "Wartezeit [ms] wenn Bot erkannt wird.(-1 für Reconnect)", -1, 600000).setDefaultValue(-1).setStep(1000));

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
        String link = downloadLink.getUrlDownloadDecrypted();
        if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
            link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);

            downloadLink.setUrlDownload(link);
        }

        logger.info("get Next Step " + step);
        // premium

        if (this.getProperties().getProperty("USE_PREMIUM") != null && this.getProperties().getBooleanProperty("USE_PREMIUM", false)) {
            return this.doPremiumStep(step, downloadLink);
        }
        else {
            return this.doFreeStep(step, downloadLink);
        }
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
                    String link = downloadLink.getUrlDownloadDecrypted();
                    if (this.getProperties().getBooleanProperty("USE_SSL", false)) link = link.replaceFirst("http://", "http://ssl.");
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
                        step.setParameter(60l * 2l);
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
                        step.setParameter(60l * 2l);
                        logger.severe("Rs.com zuviele User");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TO_MANY_USERS);
                        return step;
                    }
                    String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 1);
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
                        logger.info(requestInfo.getHtmlCode());
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

                    if (wait != null) {
                        long pendingTime = Long.parseLong(wait);
                        logger.info("Ticket: wait " + pendingTime + " seconds");
                        ticketCode = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), ticketCodePattern, 0));
                        step.setParameter(pendingTime * 1000);
                        return step;
                    }
                    else {
                        // TODO: Gibt es file sbei denen es kein Ticket gibt?
                        logger.finer("Kein Ticket gefunden. fahre fort");
                        ticketCode = "";
                        step.setParameter(10l);
                        return step;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                step.setStatus(PluginStep.STATUS_ERROR);
                logger.warning("could not get downloadInfo 2");
                return step;
            case PluginStep.STEP_GET_CAPTCHA_FILE:
                String serverAbb = serverMap.get((String) this.getProperties().getProperty("SELECTED_SERVER"));
                String server2Abb = serverMap.get((String) this.getProperties().getProperty("SELECTED_SERVER#2"));
                logger.finer("Servers ettings: " + this.getProperties().getProperty("SELECTED_SERVER") + " - " + this.getProperties().getProperty("SELECTED_SERVER#2"));
                if (serverAbb == null) {
                    serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
                    logger.finer("Random #1 server " + serverAbb);
                }
                if (server2Abb == null) {
                    server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
                    logger.finer("Random #2 server " + server2Abb);
                }
                // String endServerAbb = "";
                Boolean telekom = !(this.getProperties().getProperty("USE_TELEKOMSERVER") == null || !(Boolean) this.getProperties().getProperty("USE_TELEKOMSERVER"));
                boolean preselected = this.getProperties().getBooleanProperty("USE_PRESELECTED", true);
                ticketCode = requestInfo.getHtmlCode() + " " + ticketCode;

                captchaAddress = getFirstMatch(ticketCode, patternForCaptcha, 1);
                // post daten lesen
                postTarget = getFirstMatch(ticketCode, patternForFormData, 1);
                actionString = getFirstMatch(ticketCode, patternForFormData, 2);
                if (captchaAddress == null) {
                    logger.severe("Captcha Address not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                if (postTarget == null) {
                    logger.severe("postTarget not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                if (actionString == null) {
                    logger.severe("actionString not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                Vector<String> serverids = getAllSimpleMatches(ticketCode, patternForServer, 3);
                Vector<String> serverstrings = getAllSimpleMatches(ticketCode, patternForServer, 5);
                logger.info(serverids + " - ");
                logger.info(serverstrings + " - ");
                logger.info("wished Mirror #1 Server " + serverAbb);
                logger.info("wished Mirror #2 Server " + server2Abb);
                String selected = getSimpleMatch(ticketCode, patternForSelectedServer, 3);
                logger.finer("Preselected Server: " + selected);
                if (preselected) {
                    logger.finer("RS.com-free Use preselected : " + selected);
                    actionString = selected;
                }
                else if (telekom && ticketCode.indexOf("td.rapidshare.com") >= 0) {
                    actionString = "Download via Deutsche Telekom";
                    logger.finer("RS.com-free Use Telekom Server");
                }
                else if (ticketCode.indexOf(serverAbb + ".rapidshare.com") >= 0) {
                    logger.finer("RS.com-free Use Mirror #1 Server: " + getServerFromAbbreviation(serverAbb));
                    actionString = "Download via " + getServerFromAbbreviation(serverAbb);
                }
                else if (ticketCode.indexOf(server2Abb + ".rapidshare.com") >= 0) {
                    logger.finer("RS.com-free Use Mirror #2 Server: " + getServerFromAbbreviation(server2Abb));
                    actionString = "Download via " + getServerFromAbbreviation(server2Abb);
                }
                else if (serverstrings.size() > 0) {
                    actionString = serverstrings.get((int) Math.ceil(Math.random() * serverstrings.size()) - 1);
                    logger.finer("RS.com-free Use Errer random Server: " + actionString);
                }
                else {
                    logger.severe("Kein Server gefunden");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return null;
                }
                downloadLink.setStatusText(actionString);
                File file = this.getLocalCaptchaFile(this);
                if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                    logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                    step.setParameter(null);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                    break;
                }
                else {
                    step.setParameter(file);
                    step.setStatus(PluginStep.STATUS_USER_INPUT);
                }
                break;
            case PluginStep.STEP_DOWNLOAD:
                if (steps.get(2).getParameter() == null) {
                    // Bot Erkannt }
                    logger.severe("Fehler. Bot erkennung fehlerhaft");
                }
                else {
                    String captchaTxt = (String) steps.get(2).getParameter();
                    File captchaFile = downloadLink.getLatestCaptchaFile();
                    actionString = actionString.replace(' ', '+');
                    postParameter.put("mirror", "on");
                    postParameter.put("accesscode", captchaTxt);
                    postParameter.put("actionstring", actionString);
                    try {

                        URLConnection urlConnection = new URL(postTarget).openConnection();
                        urlConnection.setDoOutput(true);
                        // Post Parameter vorbereiten
                        String postParams = createPostParameterFromHashMap(postParameter);
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
                        logger.info("link: " + postTarget);
                        if (download(downloadLink, urlConnection, 1024 * getProperties().getIntegerProperty(PROPERTY_BYTES_TO_LOAD, -1))) {
                            step.setStatus(PluginStep.STATUS_DONE);
                            downloadLink.setStatus(DownloadLink.STATUS_DONE);
                            JDUtilities.appendInfoToFilename(captchaFile, actionString + "_" + captchaTxt, true);
                            return null;
                        }
                        else if (aborted) {
                            logger.warning("Plugin abgebrochen");
                            downloadLink.setStatus(DownloadLink.STATUS_TODO);
                            step.setStatus(PluginStep.STATUS_TODO);
                        }
                        else {
                            logger.severe("captcha wrong");
                            JDUtilities.appendInfoToFilename(captchaFile, actionString + "_" + captchaTxt, false);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                            step.setStatus(PluginStep.STATUS_ERROR);
                        }
                    }
                    catch (IOException e) {
                        logger.severe("URL could not be opened. " + e.toString());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                }
                break;
        }
        return step;
    }

    private PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) {
        String serverAbb = serverMap.get((String) this.getProperties().getProperty("SELECTED_SERVER"));
        String server2Abb = serverMap.get((String) this.getProperties().getProperty("SELECTED_SERVER#2"));
        if (serverAbb == null) {
            serverAbb = serverList1[(int) (Math.random() * (serverList1.length - 1))];
            logger.finer("Random #1 server " + serverAbb);
        }
        if (server2Abb == null) {
            server2Abb = serverList2[(int) (Math.random() * (serverList2.length - 1))];
            logger.finer("Random #2 server " + server2Abb);
        }
        // String endServerAbb = "";
        Boolean telekom = !(this.getProperties().getProperty("USE_TELEKOMSERVER") == null || !(Boolean) this.getProperties().getProperty("USE_TELEKOMSERVER"));
        String user = (String) this.getProperties().getProperty("PREMIUM_USER");
        String pass = (String) this.getProperties().getProperty("PREMIUM_PASS");
        // String encodePass = rawUrlEncode(pass);
        switch (step.getStep()) {
            case PluginStep.STEP_WAIT_TIME:
                try {
                    // get Startseite
                    // public static RequestInfo getRequest(URL link, String
                    // cookie, String referrer, boolean redirect) throws
                    // IOException {
                    String link = downloadLink.getUrlDownloadDecrypted();
                    if (this.getProperties().getBooleanProperty("USE_SSL", true)) link = link.replaceFirst("http://", "http://ssl.");
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
                        // Auswahl ob free oder prem
                        requestInfo = postRequest(new URL(newURL), null, null, null, "dl.start=PREMIUM", true);
                        // post daten lesen
                        HashMap<String, String> fields = getInputHiddenFields(requestInfo.getHtmlCode(), "premium.cgi", "submit");
                        // Part sollte mal drin bleiben. Der gehört zum
                        // normalen USer request dazu. lässt sich aber
                        // umgehen
                        String post = joinMap(fields, "=", "&") + "&accountid=" + user + "&password=" + pass;
                        // Login
                        String url;
                        if (fields.get("serverid") == null) {
                            url = "http://rapidshare.com/cgi-bin/premium.cgi";
                        }
                        else {
                            url = "http://rs" + fields.get("serverid") + ".rapidshare.com/cgi-bin/premium.cgi";
                        }
                        logger.info("loading from: " + url);
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
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM_LOGIN);
                            return step;
                        }
                        this.finalCookie = cookie;

                        post = "l=" + fields2.get("l") + "&p=" + fields2.get("p").replaceAll("\\%", "%25") + "&dl.start=Download+" + fields.get("filename").replaceAll(" ", "+");
                        url = "http://rs" + fields.get("serverid") + ".rapidshare.com/files" + "/" + fields.get("fileid") + "/" + fields.get("filename");
                        requestInfo = postRequestWithoutHtmlCode(new URL(url), cookie, url, post, true);
                        HashMap<String, String> fields3 = getInputHiddenFields(requestInfo.getHtmlCode(), "Cookie", "wrapper");
                        post = joinMap(fields3, "=", "&");
                        if (!requestInfo.isOK()) {
                            logger.info("KEin passender Server gefunden");
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
                            requestInfo = readFromURL(requestInfo.getConnection());
                            // <a
                            // href="http://rs214cg.rapidshare.com/files/50231143/dl/Discovery.rar">Download
                            // via Cogent</a><br>
                            Vector<String> urlStrings = getAllSimpleMatches(requestInfo.getHtmlCode(), "<a href=\"http://rs°\">Download via °</a><br>", 1);
                            logger.info(urlStrings + " - ");
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
                }
                catch (Exception e) {
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
                    URLConnection urlConnection;
                    File fileOutput = new File(downloadLink.getFileOutput() + ".jdd");
                    if (fileOutput.exists()) {
                        ranger.put("Range", "bytes=" + fileOutput.length() + "-");
                        requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), finalCookie, finalURL, ranger, true);
                         urlConnection = requestInfo.getConnection();
                        int length = urlConnection.getContentLength() + (int) fileOutput.length();
                        logger.info(requestInfo.getHeaders() + " - "+length);
                        downloadLink.setDownloadMax(length);

                        if (download(downloadLink, urlConnection, 1024 * getProperties().getIntegerProperty(PROPERTY_BYTES_TO_LOAD, -1),(int)fileOutput.length())) {
                            step.setStatus(PluginStep.STATUS_DONE);
                            downloadLink.setStatus(DownloadLink.STATUS_DONE);
                            return null;
                        }
                        else if (aborted) {
                            logger.warning("Plugin abgebrochen");
                            downloadLink.setStatus(DownloadLink.STATUS_TODO);
                            step.setStatus(PluginStep.STATUS_TODO);
                        }
                        else {
                            logger.severe("unknown error");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setStatus(PluginStep.STATUS_ERROR);
                        }

                    }else {
                        requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), finalCookie, finalURL, ranger, true);
                         urlConnection = requestInfo.getConnection();
                        int length = urlConnection.getContentLength();
                        logger.info(requestInfo.getHeaders() + "");
                        downloadLink.setDownloadMax(length);
                        String name = getFileNameFormHeader(urlConnection);
                        if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
                        downloadLink.setName(name);

                        if (download(downloadLink, urlConnection, 1024 * getProperties().getIntegerProperty(PROPERTY_BYTES_TO_LOAD, -1))) {
                            step.setStatus(PluginStep.STATUS_DONE);
                            downloadLink.setStatus(DownloadLink.STATUS_DONE);
                            return null;
                        }
                        else if (aborted) {
                            logger.warning("Plugin abgebrochen");
                            downloadLink.setStatus(DownloadLink.STATUS_TODO);
                            step.setStatus(PluginStep.STATUS_TODO);
                        }
                        else {
                            logger.severe("unknown error");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setStatus(PluginStep.STATUS_ERROR);
                        }
                    }
                  
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
        RequestInfo requestInfo;
        try {
            String link = downloadLink.getUrlDownloadDecrypted();
            if (link.contains("://ssl.") || !link.startsWith("http://rapidshare.com")) {
                link = "http://rapidshare.com" + link.substring(link.indexOf("rapidshare.com") + 14);
                logger.finer("URL korrigiert: " + link);
                downloadLink.setUrlDownload(link);
            }

            if (this.getProperties().getBooleanProperty("USE_SSL", true)) link = link.replaceFirst("http://", "http://ssl.");
            requestInfo = getRequest(new URL(link));
            if (requestInfo.getHtmlCode().indexOf(hardwareDefektString) > 0) {
                this.hardewareError = true;
                this.setStatusText("Hareware Error");
                return false;
            }
            if (requestInfo.getConnection().getHeaderField("Location") != null) {
                return true;
            }
            String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
            String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 1);
            if (newURL != null || strWaitTime != null) {
                return true;
            }
        }
        catch (MalformedURLException e) {
        }
        catch (IOException e) {
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        if (this.getProperties().getProperty("USE_PREMIUM") != null && ((Boolean) this.getProperties().getProperty("USE_PREMIUM"))) {
            return Integer.MAX_VALUE;
        }
        else {
            return 1;
        }
    }

    public long getBotWaittime() {

        return getProperties().getIntegerProperty("WAIT_WHEN_BOT_DETECTED", -1);
    }

    @Override
    public void resetPluginGlobals() {
        END_OF_DOWNLOAD_LIMIT = 0;

    }
}
