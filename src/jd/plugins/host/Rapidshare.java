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
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
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
 * Vereinfachte Patternerstellung: [*] optionaler Platzhalter [+] musthav platzhalter
 */
    private Pattern                        patternSupported                 = getSupportPattern("http://[*]rapidshare.com/files/[+]/[+]");

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
    private String                        patternForAlreadyDownloading     = "bereits eine Datei runter";

    /**
     * Das DownloadLimit wurde erreicht (?s)Downloadlimit.*Oder warte ([0-9]+)
     */
    private Pattern                        patternErrorDownloadLimitReached = Pattern.compile("\\((?:oder warte|or wait) ([0-9]*) (?:minuten|minutes)\\)", Pattern.CASE_INSENSITIVE);

    private Pattern                        patternErrorCaptchaWrong         = Pattern.compile("(zugriffscode falsch|code wrong)", Pattern.CASE_INSENSITIVE);

    private Pattern                        patternErrorFileAbused           = Pattern.compile("(darf nicht verteilt werden|forbidden to be shared)", Pattern.CASE_INSENSITIVE);

    private Pattern                        patternErrorFileNotFound         = Pattern.compile("(datei nicht gefunden|file not found)", Pattern.CASE_INSENSITIVE);

    private String                         hardwareDefektString             = "wegen Hardwaredefekt nicht";

    private int                            waitTime                         = 500;

    private String                         captchaAddress;

    private String                         postTarget;

    private String                         actionString;

    private HashMap<String, String>        postParameter                    = new HashMap<String, String>();

    private String                         finalURL;

    private static HashMap<String, String> serverMap                        = new HashMap<String, String>();

    private static String[]                serverList;

    private String                         finalCookie;

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
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getPluginID() {
        return "RAPIDSHARE.COM-1.0.0.";
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public Rapidshare() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        this.setConfigEelements();
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
        serverList = new String[] { "tl", "tl2", "gc", "gc2", "cg", "cg2", "tg", "l3", "l32", "l33", "l34", "tl", "dt" };

    }

    private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Server Auswahl"));

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_RADIOFIELD, getProperties(), "SELECTED_SERVER", new String[] { "Cognet", "Cognet #2", "GlobalCrossing", "GlobalCrossing #2", "TeliaSonera", "TeliaSonera #2", "Teleglobe", "Level (3)", "Level (3) #2", "Level (3) #3", "Level (3) #4", "zufällig" }, "Serverauswahl"));
        cfg.setDefaultValue("Level (3)");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_TELEKOMSERVER", "Telekom Server verwenden falls verfügbar"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Premium Accounts"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, "Premium User"));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_PASS, "Premium Pass"));
        cfg.setDefaultValue("Passwort");

        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, "Premium Account verwenden"));
        cfg.setDefaultValue(false);

    }

//    @Override
//    public URLConnection getURLConnection() {
//        return null;
//    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        RequestInfo requestInfo;
        if (step == null) {
            logger.info("PLugin Ende erreicht.");
            return null;
        }
        String server = (String) this.getProperties().getProperty("SELECTED_SERVER");
        logger.info("get Next Step " + currentStep);
        // premium
        if (this.getProperties().getProperty("USE_PREMIUM") != null && ((Boolean) this.getProperties().getProperty("USE_PREMIUM"))) {
            String user = (String) this.getProperties().getProperty("PREMIUM_USER");
            String pass = (String) this.getProperties().getProperty("PREMIUM_PASS");

            Object telekom = this.getProperties().getProperty("USE_TELEKOMSERVER");

            String encodePass = rawUrlEncode(pass);

            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME:
                    try {
                        // get Startseite
                        // public static RequestInfo getRequest(URL link, String cookie, String referrer, boolean redirect) throws IOException {
                        requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()),"nocookie","",false);
                        if (requestInfo.getHtmlCode().indexOf(hardwareDefektString) > 0) {
                            // hardewaredefeklt bei rs.com
                            step.setStatus(PluginStep.STATUS_ERROR);
                            logger.severe("Rs.com hardwaredefekt");
                            currentStep.setParameter(60 * 10);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                            return step;
                        }
                        if(requestInfo.getConnection().getHeaderField("Location")!=null){
                            requestInfo = getRequest(new URL("http://rapidshare.com/cgi-bin/premium.cgi?logout=1"),"","",false);
                            logger.severe("unknown error");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            step.setStatus(PluginStep.STATUS_ERROR);
//                            this.finalCookie=null;
//                            this.finalURL = requestInfo.getConnection().getHeaderField("Location");
                            return step;
                            
                        }
                        String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
                        if (newURL != null) {

                            // Auswahl ob free oder prem
                            requestInfo = postRequest(new URL(newURL), "dl.start=PREMIUM");

                            // post daten lesen
                            HashMap<String, String> fields = getInputHiddenFields(requestInfo.getHtmlCode(), "premium.cgi", "submit");
                            // Part sollte mal drin bleiben. Der gehört zum
                            // normalen USer request dazu. lässt sich aber
                            // umgehen
                            String post = joinMap(fields, "=", "&") + "&accountid=" + user + "&password=" + pass;
                            // Login
                            String url = "http://rs" + fields.get("serverid") + ".rapidshare.com/cgi-bin/premium.cgi";
                            logger.info(url + " - " + post);

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
                            // Wieder ein umgebarer part
                            post = "l=" + fields2.get("l") + "&p=" + fields2.get("p").replaceAll("\\%", "%25") + "&dl.start=Download+" + fields.get("filename").replaceAll(" ", "+");
                            url = "http://rs" + fields.get("serverid") + ".rapidshare.com/files" + "/" + fields.get("fileid") + "/" + fields.get("filename");
                            logger.info(url + " - " + post);

                            requestInfo = postRequestWithoutHtmlCode(new URL(url), cookie, url, post, false);
                            if (requestInfo.getConnection().getHeaderField("Content-Type").equalsIgnoreCase("application/octet-stream")) {
                                // Direktlinks sind aktiv
                                this.finalURL = "http://rs" + fields.get("serverid") + serverMap.get(server) + ".rapidshare.com/files" + "/" + fields.get("fileid") + "/" + fields.get("filename");
                                requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), cookie, null, true);
                                int x = 0;
                                while (!requestInfo.isOK()) {
                                    x++;
                                    this.finalURL = "http://rs" + fields.get("serverid") + serverList[(int) (Math.random() * (serverList.length - 1))] + ".rapidshare.com/files" + "/" + fields.get("fileid") + "/" + fields.get("filename");
                                    requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), cookie, null, true);
                                    // hardewaredefeklt bei rs.com
                                    if (x > 10) {
                                        step.setStatus(PluginStep.STATUS_ERROR);
                                        logger.severe("File down? Direktlink download. Serverfehler");
                                        currentStep.setParameter(60 * 10);
                                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                                        return step;
                                    }
                                }

                                break;
                            }
                            else {
                                requestInfo = readFromURL(requestInfo.getConnection());
                            }
                            HashMap<String, String> fields3 = getInputHiddenFields(requestInfo.getHtmlCode(), "Cookie", "wrapper");
                            post = joinMap(fields3, "=", "&");

                            if (telekom != null && (Boolean) telekom && requestInfo.getHtmlCode().indexOf("rs" + fields.get("serverid") + "dt.") > 0) {
                                // Telekom server
                                url = "http://rs" + fields.get("serverid") + "dt.rapidshare.com/files" + "/" + fields.get("fileid") + "/dl/" + fields.get("filename");

                            }
                            else if (server != null && requestInfo.getHtmlCode().indexOf("rs" + fields.get("serverid") + serverMap.get(server) + ".") > 0) {
                                // Normaler Wunschserver
                                url = "http://rs" + fields.get("serverid") + serverMap.get(server) + ".rapidshare.com/files" + "/" + fields.get("fileid") + "/dl/" + fields.get("filename");

                            }
                            else {
                                // ZUfall
                                Vector<Vector<String>> serverMatches = getAllSimpleMatches(requestInfo.getHtmlCode(), "</p><p><table><tr><td><a href=\"http://rs°\">Download via °</a><br>");
                                int index = (int) (Math.random() * serverMatches.size());
                                if (serverMatches.size() == 0) {
                                    step.setStatus(PluginStep.STATUS_ERROR);
                                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                                    break;
                                }
                                else {
                                    url = "http://rs" + serverMatches.elementAt(index).elementAt(0);
                                }

                            }
                            logger.info("final " + url);
                            this.finalURL = url;

                        }
                        else {
                            if (newURL == null || captchaAddress == null || postTarget == null || actionString == null) {
                                logger.info("check pattern " + patternErrorDownloadLimitReached);
                                String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 1);
                                if (strWaitTime != null) {
                                    logger.severe("wait " + strWaitTime + " minutes");
                                    waitTime = Integer.parseInt(strWaitTime) * 60 * 1000;
                                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                                    logger.info(" WARTEZEIT SETZEN IN " + currentStep + " : " + waitTime);
                                    currentStep.setParameter((long) waitTime);
                                    return currentStep;
                                }
                                String strFileAbused = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileAbused, 0);
                                if (strFileAbused != null) {
                                    logger.severe("file abused");
                                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                                    return currentStep;
                                }
                                String strFileNotFound = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileNotFound, 0);
                                if (strFileNotFound != null) {
                                    logger.severe("file not found");
                                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                                    return currentStep;
                                }

                                String strCaptchaWrong = getFirstMatch(requestInfo.getHtmlCode(), patternErrorCaptchaWrong, 0);
                                if (strCaptchaWrong != null) {
                                    logger.severe("captchaWrong");
                                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                                    return currentStep;
                                }
                               

                                if (requestInfo.containsHTML(patternForAlreadyDownloading)) {
                                    logger.severe("Already Loading wait " + 60 + " sek. to Retry");
                                    waitTime = 180 * 1000;
                                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_STATIC_WAITTIME);
                                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                                    logger.info(" WARTEZEIT SETZEN IN (already loading)" + currentStep + " : " + waitTime);
                                    currentStep.setParameter((long) waitTime);
                                    return currentStep;
                                }

                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                                currentStep.setStatus(PluginStep.STATUS_ERROR);
                                logger.warning("could not get downloadInfo ");
                                try {
                                    Thread.sleep(5000);
                                }
                                catch (InterruptedException e) {
                                }
                                return currentStep;
                            }
                        }
                        logger.info(newURL + " - " + captchaAddress + " - " + postTarget + " - " + actionString);

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    }
                    break;
                case PluginStep.STEP_GET_CAPTCHA_FILE:

                    // schritt überspringen
                    step.setStatus(PluginStep.STATUS_SKIP);
                    step = nextStep(step);
                case PluginStep.STEP_DOWNLOAD:

                    try {

                        requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), finalCookie, finalURL, true);

                        URLConnection urlConnection = requestInfo.getConnection();
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        downloadLink.setName(getFileNameFormHeader(urlConnection));
                        if (download(downloadLink, urlConnection)) {
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
                    catch (IOException e) {
                        logger.severe("URL could not be opened. " + e.toString());
                    }
            }
        }
        else {
            switch (step.getStep()) {

                case PluginStep.STEP_WAIT_TIME:
                    getDownloadInfo(downloadLink);

                    break;
                case PluginStep.STEP_GET_CAPTCHA_FILE:
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
                    if (steps.get(1).getParameter() == null) {
                        // Bot Erkannt }
                        logger.severe("Fehler. Bot erkennung fehlerhaft");
                    }
                    else {
                        postParameter.put("mirror", "on");
                        postParameter.put("accesscode", (String) steps.get(1).getParameter());
                        postParameter.put("actionString", actionString);
                        boolean success = prepareDownload(downloadLink);
                        if (success) {
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
                            logger.severe("captcha wrong");
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                            step.setStatus(PluginStep.STATUS_ERROR);
                        }
                    }
                    break;
            }

        }
        return step;
    }

    private String rawUrlEncode(String str) {
        try {
            str = URLDecoder.decode(str, "UTF-8");

            String ret = "";
            int i;
            for (i = 0; i < str.length(); i++) {
                char letter = str.charAt(i);

                ret += "%" + Integer.toString(letter, 16);

            }

            return ret;
        }
        catch (UnsupportedEncodingException e) {

            e.printStackTrace();
        }
        return str;
    }

    private void getDownloadInfo(DownloadLink downloadLink) {
        String server = (String) this.getProperties().getProperty("SELECTED_SERVER");
        try {
            // Der Download wird bestätigt
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()));
            if (requestInfo.getHtmlCode().indexOf(hardwareDefektString) > 0) {
                // hardewaredefeklt bei rs.com
                currentStep.setStatus(PluginStep.STATUS_ERROR);
                currentStep.setParameter(60 * 10);
                logger.severe("Rs.com hardwaredefekt");
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                return;
            }
            if(requestInfo.getConnection().getHeaderField("Location")!=null){
                requestInfo = getRequest(new URL("http://rapidshare.com/cgi-bin/premium.cgi?logout=1"),"","",false);
                logger.severe("unknown error");
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                currentStep.setStatus(PluginStep.STATUS_ERROR);
//                this.finalCookie=null;
//                this.finalURL = requestInfo.getConnection().getHeaderField("Location");
                return;
            }
            String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
            if (newURL != null) {

                // Auswahl ob free oder prem
                requestInfo = postRequest(new URL(newURL), "dl.start=free");

                // captcha Adresse finden
                captchaAddress = getFirstMatch(requestInfo.getHtmlCode(), patternForCaptcha, 1);

                // post daten lesen
                postTarget = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 1);
                actionString = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 2);
                if (server != null) {
                    actionString = "Download via " + server;
                }

            }
            logger.info(newURL + " - " + captchaAddress + " - " + postTarget + " - " + actionString);
            currentStep = steps.firstElement();
            if (newURL == null || captchaAddress == null || postTarget == null || actionString == null) {
                logger.info("check pattern " + patternErrorDownloadLimitReached);
                String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 1);
                if (strWaitTime != null) {
                    logger.severe("wait " + strWaitTime + " minutes");
                    waitTime = Integer.parseInt(strWaitTime) * 60 * 1000;
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    logger.info(" WARTEZEIT SETZEN IN " + currentStep + " : " + waitTime);
                    currentStep.setParameter((long) waitTime);
                    return;
                }
                String strFileAbused = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileAbused, 0);
                if (strFileAbused != null) {
                    logger.severe("file abused");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
                String strFileNotFound = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileNotFound, 0);
                if (strFileNotFound != null) {
                    logger.severe("file not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }

                String strCaptchaWrong = getFirstMatch(requestInfo.getHtmlCode(), patternErrorCaptchaWrong, 0);
                if (strCaptchaWrong != null) {
                    logger.severe("captchaWrong");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
     
                if (requestInfo.containsHTML(patternForAlreadyDownloading)) {
                    logger.severe("Already Loading wait " + 60 + " sek. to Retry");
                    waitTime = 180 * 1000;
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_STATIC_WAITTIME);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    logger.info(" WARTEZEIT SETZEN IN (already loading)" + currentStep + " : " + waitTime);
                    currentStep.setParameter((long) waitTime);
                    return;
                }

                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                currentStep.setStatus(PluginStep.STATUS_ERROR);
                logger.warning("could not get downloadInfo ");
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                }
                return;
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean prepareDownload(DownloadLink downloadLink) {

        try {
            URLConnection urlConnection = new URL(postTarget).openConnection();
            urlConnection.setDoOutput(true);

            // Post Parameter vorbereiten
            String postParams = createPostParameterFromHashMap(postParameter);
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(postParams);
            wr.flush();
            // content-disposition: Attachment; filename=a_mc_cs3_g_cd.rsdf
            downloadLink.setName(getFileNameFormHeader(urlConnection));
            int length = urlConnection.getContentLength();
            downloadLink.setDownloadMax(length);
            return download(downloadLink, urlConnection);
        }
        catch (IOException e) {
            logger.severe("URL could not be opened. " + e.toString());
        }
        return false;
    }

    @Override
    public boolean doBotCheck(File file) {
        try {
            return this.md5sum(file).equals(botHash);
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

    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        
            // Der Download wird bestätigt
            RequestInfo requestInfo;
            try {
                requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()));
         
            if (requestInfo.getHtmlCode().indexOf(hardwareDefektString) > 0) {              
                return false;
            }
            if(requestInfo.getConnection().getHeaderField("Location")!=null){               
                return true;
            }
            String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
            String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 1);
            if (newURL != null||strWaitTime!=null) {
              return true;
            }
            
            }
            catch (MalformedURLException e) { }
            catch (IOException e) { }
        return false;
    }


    @Override
    public int getMaxSimultanDownloadNum() {
        if (this.getProperties().getProperty("USE_PREMIUM") != null && ((Boolean) this.getProperties().getProperty("USE_PREMIUM"))) {
        return Integer.MAX_VALUE;
        }else{
     
        return 1;
        }
    }
}
