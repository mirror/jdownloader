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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.http.HeadRequest;
import jd.http.PostRequest;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Rapidshare extends PluginForHost {

    //    
    // '<form name="dlf"
    // action="http://rs235gc2.rapidshare.com/files/123613963/1216288/webinterface8.jdu"
    // method="post">' +
    // '<center><table><tr><td><img
    // src="http://rs235.rapidshare.com/access1216288.gif"></td>' +

    // private static final Pattern PATTERN_FIND_CAPTCHA_IMAGE_URL =
    // Pattern.compile("<center><table><tr><td><img src\\=\"(.*?)\"></td>");

    static private final String host = "rapidshare.com";
    private static long LAST_FILE_CHECK = 0;
    // private static final Pattern PATTERN_MATCHER_CAPTCHA_WRONG =
    // Pattern.compile("(wrong [acces ]*?code|Zugriffscode)");
    private static final Pattern PATTERM_MATCHER_ALREADY_LOADING = Pattern.compile("(Please wait until the download is completed)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FIND_DOWNLOAD_POST_URL = Pattern.compile("<form name=\"dl[f]?\" action=\"(.*?)\" method=\"post\"");

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?download the following file:.*?<p>(.*?)<", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_1 = Pattern.compile("<h1>Fehler</h1>.*?<div class=\"klappbox\">.*?<p>(.*?)<", Pattern.DOTALL);
    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_2 = Pattern.compile("<!-- E#[\\d]{1,2} -->(.*?)<", Pattern.DOTALL);
    private static final Pattern PATTERN_FIND_ERROR_MESSAGE_3 = Pattern.compile("<!-- E#[\\d]{1,2} --><p>(.*?)<\\/p>", Pattern.DOTALL);

    private static final Pattern PATTERN_FIND_MIRROR_URL = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");
    // <center><table><tr><td><img
    // src="http://rs235.rapidshare.com/access1216288.gif"></td>
    private static final Pattern PATTERN_FIND_MIRROR_URLS = Pattern.compile("<input.*?type=\"radio\" name=\"mirror\" onclick=\"document.dlf?.action=\\\\'(.*)\\\\';\" /> (.*?)<br />'");

    // private static final Pattern PATTERN_MATCHER_HAPPY_HOUR =
    // Pattern.compile("(Happy hour)", Pattern.CASE_INSENSITIVE);
    // private static final Pattern PATTERN_FIND_DOWNLOAD_LIMIT_WAITTIME =
    // Pattern.compile("Alternativ k&ouml;nnen Sie ([\\d]{1,4}) Minuten
    // warten.", Pattern.CASE_INSENSITIVE);
    // <form name="dl"
    // action="http://rs363cg.rapidshare.com/files/119944363/814136/NG_-_001_-_TaN.part2.rar"
    // method="post">
    private static final Pattern PATTERN_FIND_PRESELECTED_SERVER = Pattern.compile("<form name=\"dlf?\" action=\"(.*?)\" method=\"post\">");

    private static final Pattern PATTERN_FIND_TICKET_WAITTIME = Pattern.compile("var c=([\\d]*?);");
    // private static final Pattern PATTERN_MATCHER_ACCOUNT_EXPIRED=
    // Pattern.compile("(Dieses Konto ist am)");
    // private static final Pattern PATTERN_MATCHER_BOT = Pattern.compile("(Too
    // many wrong codes)");

    private static final Pattern PATTERN_MATCHER_DOWNLOAD_ERRORPAGE = Pattern.compile("(RapidShare)", Pattern.CASE_INSENSITIVE);
    // private static final Pattern PATTERN_FIND_ERROR_CODES =
    // Pattern.compile("<!-- E#([\\d]{1,3}) -->(.*?)div");
    // private static final Pattern PATTERN_MATCHER_FIND_ERROR =
    // Pattern.compile("(<h1>Fehler</h1>)");

    // private boolean hashFound;

    // private CESClient ces;

    private static final Pattern PATTERN_MATCHER_PREMIUM_EXPIRED = Pattern.compile("expired");

    private static final Pattern PATTERN_MATCHER_PREMIUM_LIMIT_REACHED = Pattern.compile("You have exceeded the download limit");

    private static final Pattern PATTERN_MATCHER_PREMIUM_OVERLAP = Pattern.compile("IP");

    private static final Pattern PATTERN_MATCHER_TOO_MANY_USERS = Pattern.compile("(2 minutes)");

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidshare\\.com/files/[\\d]{3,9}/.*", Pattern.CASE_INSENSITIVE);

    // private static final String PROPERTY_USE_SSL = "USE_SSL";

    private static final String PROPERTY_INCREASE_TICKET = "INCREASE_TICKET";

    private static final String PROPERTY_SELECTED_SERVER = "SELECTED_SERVER";

    private static final String PROPERTY_SELECTED_SERVER2 = "SELECTED_SERVER#2";

    private static final String PROPERTY_USE_PRESELECTED = "USE_PRESELECTED";
    private static final String PROPERTY_USE_TELEKOMSERVER = "USE_TELEKOMSERVER";
    private static final String PROPERTY_WAIT_WHEN_BOT_DETECTED = "WAIT_WHEN_BOT_DETECTED";
    private static String[] serverList1;
    // <!-- E#7 --><p>Der Server 162.rapidshare.com ist momentan nicht
    // verf&uuml;gbar. Wir arbeiten an der Fehlerbehebung.</p>
    // <!-- E#9 -->Sie haben heute <b>10011 MB</b> heruntergeladen und damit das
    // Limit &uuml;berschritten.</p>
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

    // private static final int ACTION_HAPPY_HOURS = 7;

    // private static final String PARAM_WAIT_FOR_HAPPYHOURS =
    // "WAIT_FOR_HAPPYHOURS";

    // private static final int ACTION_HAPPY_HOURS_TOGGLE_WAIT = 8;

    // private static final int ACTION_HAPPY_HOURS_FORCE_FREE = 9;

    // private static final int ERROR_ID_ACCOUNTEXPIRED = 4;

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
        return link;
    }

    // private static final Pattern PATTERN_FIND_CAPTCHA_ID =
    // Pattern.compile("<table><tr><td><img id\\=\"(.*?)\" src\\=\"\">");

    private final String ACCEPT_LANGUAGE = "en-gb, en;q=0.8";

    // private static boolean FORCE_FREE_USER = true;

    private String[] serverList2;

    public Rapidshare() {
        super();

        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
        serverMap.put("Cogent #1", "cg");
        serverMap.put("Cogent #2", "cg2");
        serverMap.put("Deutsche Telekom", "dt");
        serverMap.put("GlobalCrossing #1", "gc");
        serverMap.put("GlobalCrossing #2", "gc2");
        serverMap.put("Level(3) #1", "l3");
        serverMap.put("Level(3) #2", "l32");
        serverMap.put("Level(3) #3", "l33");
        serverMap.put("Level(3) #4", "l34");
        serverMap.put("Tata Com. #1", "tg");
        serverMap.put("Tata Com. #2", "tg2");
        serverMap.put("TeliaSonera #1", "tl");
        serverMap.put("TeliaSonera #2", "tl2");
        serverMap.put("TeliaSonera #3", "tl3");

        serverList1 = new String[] { "cg", "cg2", "dt", "gc", "gc2", "l3", "l32", "l33", "l34", "tg", "tl", "tl2" };
        serverList2 = new String[] { "cg", "dt", "gc", "gc2", "l3", "l32", "tg", "tg2", "tl", "tl2", "tl3" };
        setConfigElements();
        enablePremium();
    }

    /**
     * Prüft vor dem Download ob der Download geschrieben werden darf Es wird
     * z.B. auf "Is local file in progress" oder "fileexists" geprüft.
     * 
     * @param step
     * @param downloadLink
     * @return
     */
    private boolean checkDestFile(DownloadLink downloadLink) {
        if (JDUtilities.getController().getLinkThatBlocks(downloadLink) != null) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_LINK_IN_PROGRESS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return false;
        }

        if (new File(downloadLink.getFileOutput()).exists()) {
            logger.severe("File already exists. " + downloadLink.getFileOutput());
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_ALREADYEXISTS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return false;
        }
        return true;
    }

    /**
     * Bietet der hoster eine Möglichkeit mehrere links gleichzeitig zu prüfen,
     * kann das über diese Funktion gemacht werden. Beir s.com istd as derzeitd
     * eaktiviert, weild er Linkchecker nicht mehr über ssl erreichbar ist.
     */
    public boolean[] checkLinks(DownloadLink[] urls) {
        try {
            if (urls == null) { return null; }
            boolean[] ret = new boolean[urls.length];
            int c = 0;
            while (true) {
                String post = "";
                int i = 0;
                for (i = c; i < urls.length; i++) {

                    if (!canHandle(urls[i].getDownloadURL())) { return null; }

                    if (urls[i].getDownloadURL().contains("://ssl.") || !urls[i].getDownloadURL().startsWith("http://rapidshare.com")) {
                        urls[i].setUrlDownload("http://rapidshare.com" + urls[i].getDownloadURL().substring(urls[i].getDownloadURL().indexOf("rapidshare.com") + 14));

                    }
                    if ((post + urls[i].getDownloadURL() + "%0a").length() > 10000) {
                        break;
                    }
                    post += urls[i].getDownloadURL() + "%0a";

                }

                PostRequest r = new PostRequest("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi");
                r.setPostVariable("urls", post);
                r.setPostVariable("toolmode", "1");
                String page = r.load();

                String[] lines = Regex.getLines(page);
                if (lines.length != i - c) { return null; }

                for (String line : lines) {

                    String[] erg = line.split(",");
                    /*
                     * 1: Normal online -1: date nicht gefunden 3: Drect
                     * download
                     */
                    ret[c] = true;
                    if (erg.length < 6 || !erg[2].equals("1") && !erg[2].equals("3")) {
                        ret[c] = false;
                    } else {
                        urls[c].setDownloadSize(Integer.parseInt(erg[4]));
                        urls[c].setName(erg[5].trim());
                    }
                    c++;

                }
                if (c >= urls.length) { return ret; }
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

    public boolean doBotCheck(File file) {

        String hash = JDUtilities.getLocalHash(file);
        return hash != null && hash.equals(JDUtilities.getLocalHash(JDUtilities.getResourceFile("jd/captcha/methods/rapidshare.com/bot.jpg")));

    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        // if (ddl)this.doPremium(downloadLink);
        Rapidshare.correctURL(downloadLink);
        // if (getRemainingWaittime() > 0) { return
        // handleDownloadLimit(downloadLink); }
        String freeOrPremiumSelectPostURL = null;
        Browser br = new Browser();
        br.setAcceptLanguage(ACCEPT_LANGUAGE);
        br.setFollowRedirects(false);

        if (!checkDestFile(downloadLink)) { return; }
        String link = downloadLink.getDownloadURL();

        // RS URL wird aufgerufen
        // req = new GetRequest(link);
        // req.load();
        br.getPage(link);
        if (br.getRedirectLocation() != null) {
            logger.info("Direct Download");
            this.handlePremium(downloadLink, new Account("dummy", "dummy"));
            return;
        }
        // posturl für auswahl free7premium wird gesucht
        freeOrPremiumSelectPostURL = new Regex(br, PATTERN_FIND_MIRROR_URL).getFirstMatch();
        // Fehlerbehandlung auf der ersten Seite
        if (freeOrPremiumSelectPostURL == null) {
            String error = null;
            if ((error = findError(br + "")) != null) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(error);
                return;
            }
            reportUnknownError(br, 1);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.warning("could not get newURL");
            return;
        }

        // Post um freedownload auszuwählen
        Form[] forms = br.getForms();

        br.submitForm(forms[0]);
        // PostRequest pReq = new PostRequest(freeOrPremiumSelectPostURL);
        // pReq.setPostVariable("dl.start", "free");
        // pReq.load();
        String error = null;

        if ((error = findError(br + "")) != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(error);
            return;
        }

        // Fehlersuche
        if (Regex.matches(br, PATTERN_MATCHER_TOO_MANY_USERS)) {
            logger.warning("Too many users are currently downloading this file. Wait 2 Minutes and try again");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        } else if (new Regex(br, PATTERM_MATCHER_ALREADY_LOADING).matches()) {
            logger.severe("Already downloading. Wait 2 min. or reconnect");

            // waitTime = 120 * 1000;
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            // step.setStatus(PluginStep.STATUS_ERROR);
            // setDownloadLimitTime(waitTime);

            linkStatus.setValue(120 * 1000);
            return;
        } else if ((error = findError(br + "")) != null) {

            reportUnknownError(br, 2);

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(error);
            return;
        }
        // Ticketwartezeit wird gesucht
        String ticketTime = new Regex(br, PATTERN_FIND_TICKET_WAITTIME).getFirstMatch();
        if (ticketTime != null && ticketTime.equals("0")) {
            ticketTime = null;
        }

        String ticketCode = br + "";

        String tt = new Regex(ticketCode, "var tt =(.*?)document\\.getElementById\\(\"dl\"\\)\\.innerHTML").getFirstMatch();

        String fun = "function f(){ return " + tt + "} f()";
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();

        // Collect the arguments into a single string.

        // Now evaluate the string we've colected.
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);

        // Convert the result to a string and print it.
        String code = Context.toString(result);
        if (tt != null) {
            ticketCode = code;
        }
        Context.exit();

        // captchaadresse wird gesucht
        // String cid=new
        // Regex(ticketCode,PATTERN_FIND_CAPTCHA_IMAGE_URL).getFirstMatch();
        // Pattern
        // p=Pattern.compile("getElementById\\(\""+cid+"\"\\)\\.src\\=\"(.*?)\"");
        //            
        /*
         * String captchaAddress = new Regex(ticketCode,
         * PATTERN_FIND_CAPTCHA_IMAGE_URL).getFirstMatch(); // Happy Hour check
         * String captchaCode = null; File captchaFile = null; if
         * (Regex.matches(pReq, PATTERN_MATCHER_HAPPY_HOUR)) {
         * 
         * logger.info("Happy hours active"); // return
         * doHappyHourDownload(step, downloadLink); } else {
         * 
         * if
         * (getProperties().getBooleanProperty(Rapidshare.PARAM_WAIT_FOR_HAPPYHOURS,
         * false)) { // Auf Happy Hour warten waitForHappyHours(step,
         * downloadLink); return; }
         * 
         * if (captchaAddress == null) { logger.severe("Captcha Address not
         * found"); this.reportUnknownError(pReq, 2);
         * linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);//step.setParameter("Captcha
         * ImageIO Error"); //step.setStatus(PluginStep.STATUS_ERROR); return; }
         */

        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        // this.sleep(1000,downloadLink);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // if(true)return;
        long pendingTime = 0;
        if (ticketTime != null) {
            pendingTime = Long.parseLong(ticketTime);

            if (getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) > 0) {
                logger.warning("Waittime increased by JD: " + pendingTime + " --> " + (pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100));
                pendingTime = pendingTime + getPluginConfig().getIntegerProperty(PROPERTY_INCREASE_TICKET, 0) * pendingTime / 100;

            }
            pendingTime *= 1000;
            // pendingTime -= timer;

            // downloadLink.setEndOfWaittime(System.currentTimeMillis()
            // + pendingTime);
        }

        waitTicketTime(downloadLink, pendingTime);
        // captchaFile = this.getLocalCaptchaFile(this);

        // long timer = System.currentTimeMillis();

        // captchaCode = getCaptchaCode(step, downloadLink, captchaFile,
        // captchaAddress);

        // timer = System.currentTimeMillis() - timer;

        // War Captchaerkennung Fehlerhaft?
        // if (linkStatus.isFailed()) { return; }
        /*
         * if (captchaCode == null || captchaCode.trim().length() != 4) {
         * logger.severe("Captcha could not be recognized");
         * JDUtilities.appendInfoToFilename(this, captchaFile, captchaCode,
         * false); linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA_WRONG);
         * 
         * if (ces != null) ces.sendCaptchaWrong();
         * //step.setStatus(PluginStep.STATUS_ERROR); return; }
         */
        // logger.info("captcha detection duration: " +
        // JDUtilities.formatSeconds((int) (timer / 1000)));
        // get Downloadserverurl
        String postTarget = getDownloadTarget(downloadLink, ticketCode);

        // Falls Serverauswahl fehlerhaft war
        if (linkStatus.isFailed()) { return; }
        // pReq = new PostRequest(postTarget);
        // pReq.setPostVariable("mirror", "on");
        // // if (captchaCode == null) captchaCode = "";
        // // pReq.setPostVariable("accesscode", captchaCode);
        // pReq.setPostVariable("x", (int) (Math.random() * 40) + "");
        // pReq.setPostVariable("y", (int) (Math.random() * 40) + "");
        // pReq.connect();

        HTTPConnection urlConnection = br.openPostConnection(postTarget, "mirror=on&x=" + Math.random() * 40 + "&y=" + Math.random() * 40);

        dl = new RAFDownload(this, downloadLink, urlConnection);
        if (dl.startDownload()) {

            if (new File(downloadLink.getFileOutput()).length() < 8000) {
                String page = JDUtilities.getLocalFile(new File(downloadLink.getFileOutput()));
                error = findError(page + "");
                // if (new Regex(page, PATTERN_MATCHER_CAPTCHA_WRONG).matches())
                // {
                //
                // new File(downloadLink.getFileOutput()).delete();
                //
                // linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
                //
                // // if (hashFound) {
                //
                // // new
                // //
                // GetRequest("http://jdservice.ath.cx/rs/hw.php?loader=jd&code="
                // // + captchaCode + "&hash=" +
                // // JDUtilities.getLocalHash(captchaFile)).load();
                //
                // // }
                // // JDUtilities.appendInfoToFilename(this, captchaFile,
                // // captchaCode, false);
                // // if (ces != null) ces.sendCaptchaWrong();
                // // step.setStatus(PluginStep.STATUS_ERROR);
                // return;
                // }
                // if (new Regex(page, PATTERN_MATCHER_BOT).matches()) {
                // new File(downloadLink.getFileOutput()).delete();
                //
                // linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
                // linkStatus.setValue(Math.max((int)getBotWaittime(), 60000));
                // logger.info("Error detected. Bot detected");
                //
                // // step.setStatus(PluginStep.STATUS_ERROR);
                //
                // // new
                // //
                // GetRequest("http://jdservice.ath.cx/rs/hw.php?loader=jd&code=BOT!&hash="
                // // + JDUtilities.getLocalHash(captchaFile)).load();
                //
                // return;
                // }
                if (Regex.matches(page, PATTERN_MATCHER_DOWNLOAD_ERRORPAGE)) {

                    linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                    downloadLink.getLinkStatus().setStatusText("Download error(>log)");
                    linkStatus.setErrorMessage(error);
                    logger.severe("Error detected. " + JDUtilities.getLocalFile(new File(downloadLink.getFileOutput())));
                    new File(downloadLink.getFileOutput()).delete();
                    // step.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
            }

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
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        Rapidshare.correctURL(downloadLink);
        logger.info(downloadLink.getDownloadURL());

        Browser br = new Browser();
        br.setAcceptLanguage(ACCEPT_LANGUAGE);
        br.setFollowRedirects(false);
        setMaxConnections(35);

        user = Encoding.urlEncode(user.trim());
        pass = Encoding.urlEncode(pass.trim());
        // String encodePass = rawUrlEncode(pass);

        long headLength;

        String link = downloadLink.getDownloadURL();
        // if (this.getProperties().getBooleanProperty(PROPERTY_USE_SSL,
        // true)) link = link.replaceFirst("http://", "http://ssl.");
        HeadRequest hReq = new HeadRequest(link);
        hReq.load();

        headLength = hReq.getContentLength();
        if (headLength <= 10000) {
            // requestInfo = HTTP.getRequest(new URL(link), null, "",
            // false);
            String page = br.getPage(link);
            String error = null;

            if ((error = findError(page)) != null) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                logger.severe(error);
                linkStatus.addStatus(LinkStatus.ERROR_FATAL);
                linkStatus.setErrorMessage(error);
                return;

            }
        }

        logger.info("Loading from: " + link.substring(0, 30));
        // HashMap<String, String> ranger = new HashMap<String, String>();
        // ranger.put("Authorization", "Basic " +
        // JDUtilities.Base64Encode(user + ":" +
        // pass));
        HTTPConnection urlConnection;
        // GetRequest req = new GetRequest(link);
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(user + ":" + pass));
        br.setFollowRedirects(true);

        urlConnection = br.openGetConnection(link);
        // urlConnection.getHeaderField("Content-Type").equals("text/html")
        if (Long.parseLong(urlConnection.getHeaderField("Content-Length")) != headLength) {

            String page = br.getRequest().read();
            String error;
            if ((error = findError(page)) != null) {
                new File(downloadLink.getFileOutput()).delete();

                logger.warning(error);
                // step.setStatus(PluginStep.STATUS_ERROR);
                if (Regex.matches(error, PATTERN_MATCHER_PREMIUM_EXPIRED)) {
                    linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                    // step.setParameter(premium);
                    downloadLink.getLinkStatus().setErrorMessage(error);
                    linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);
                } else if (Regex.matches(error, PATTERN_MATCHER_PREMIUM_LIMIT_REACHED)) {
                    linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                    // step.setParameter(premium);
                    downloadLink.getLinkStatus().setErrorMessage(error);
                    linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);

                } else if (Regex.matches(error, PATTERN_MATCHER_PREMIUM_OVERLAP)) {
                    linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                    // step.setParameter(premium);
                    downloadLink.getLinkStatus().setErrorMessage(error);
                    linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                    linkStatus.setValue(LinkStatus.VALUE_ID_PREMIUM_DISABLE);

                    linkStatus.setErrorMessage(error);
                }

                return;
            } else {
                new File(downloadLink.getFileOutput()).delete();

                linkStatus.addStatus(LinkStatus.ERROR_RETRY);

                reportUnknownError(page, 6);

                // step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }

        }

        dl = new RAFDownload(this, downloadLink, urlConnection);

        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        if (dl.startDownload()) {
            // Dieses Konto ist am Mon, 2. Jun 2008 abgelaufen

            if (new File(downloadLink.getFileOutput()).length() < 8000) {
                String page = JDUtilities.getLocalFile(new File(downloadLink.getFileOutput()));
                if (Regex.matches(page, PATTERN_MATCHER_DOWNLOAD_ERRORPAGE)) {
                    new File(downloadLink.getFileOutput()).delete();

                    linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                    String error = findError(page);
                    logger.severe(error);
                    reportUnknownError(page, 5);

                    // step.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
            }

            // step.setStatus(PluginStep.STATUS_DONE);
            linkStatus.addStatus(LinkStatus.FINISHED);

            return;
        }

    }

    private String findError(String string) {
        String error = null;
        error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE).getFirstMatch();

        if (error == null || error.length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_3).getFirstMatch();
        }
        if (error == null || error.length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_2).getFirstMatch();
        }
        if (error == null || error.length() == 0) {
            error = new Regex(string, PATTERN_FIND_ERROR_MESSAGE_1).getFirstMatch();
        }

        error = Encoding.htmlDecode(error);
        String[] er = Regex.getLines(error);

        if (er == null || er.length == 0) { return null; }
        error = JDLocale.L("plugins.host.rapidshare.errors." + JDUtilities.getMD5(er[0]), er[0]);
        if (error.equals(er[0])) {
            logger.warning("NO TRANSLATIONKEY FOUND FOR: " + er[0] + "(" + JDUtilities.getMD5(er[0]) + ")");
        }
        return error;

    }

    public String getAGBLink() {
        return "http://rapidshare.com/faq.html";
    }

    public long getBotWaittime() {

        return getPluginConfig().getIntegerProperty(PROPERTY_WAIT_WHEN_BOT_DETECTED, -1);
    }

    public String getCoder() {
        return "JD-Team";
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
    private String getDownloadTarget(DownloadLink downloadLink, String ticketCode) {

        String postTarget = new Regex(ticketCode, PATTERN_FIND_DOWNLOAD_POST_URL).getFirstMatch();

        String server1 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER, "Level(3)");
        String server2 = getPluginConfig().getStringProperty(PROPERTY_SELECTED_SERVER2, "TeliaSonera");
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
        boolean telekom = getPluginConfig().getBooleanProperty(PROPERTY_USE_TELEKOMSERVER, false);
        boolean preselected = getPluginConfig().getBooleanProperty(PROPERTY_USE_PRESELECTED, true);

        // actionString = getSimpleMatch(ticketCode, dataPatternAction,
        // 0);

        if (postTarget == null) {
            logger.severe("postTarget not found:");
            reportUnknownError(ticketCode, 4);
            downloadLink.getLinkStatus().addStatus(LinkStatus.ERROR_RETRY);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return null;
        }
        // // postTarget=postTarget.substring(2, postTarget.length()-3);
        // // logger.info(postTarget+" -"+actionString);
        // if (actionString == null) {
        // logger.severe("actionString not found");
        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // return;
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
            // linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // //step.setStatus(PluginStep.STATUS_ERROR);
            // return null;
        }

        return postTarget;
    }

    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        if (System.currentTimeMillis() - LAST_FILE_CHECK < 250) {
            try {
                Thread.sleep(System.currentTimeMillis() - LAST_FILE_CHECK);
            } catch (InterruptedException e) {
            }
        }
        Rapidshare.correctURL(downloadLink);
        LAST_FILE_CHECK = System.currentTimeMillis();
        RequestInfo requestInfo;
        try {
            // http://rapidshare.com/files/117366525/dlc.dlc
            requestInfo = HTTP.getRequest(new URL("https://ssl.rapidshare.com/cgi-bin/checkfiles.cgi?urls=" + downloadLink.getDownloadURL() + "&toolmode=1"));

            String[] erg = requestInfo.getHtmlCode().trim().split(",");
            /*
             * 1: Normal online -1: date nicht gefunden 3: Drect download
             */
            if (erg.length < 6 || !erg[2].equals("1") && !erg[2].equals("3")) { return false; }

            downloadLink.setName(erg[5]);
            downloadLink.setDownloadSize(Integer.parseInt(erg[4]));

            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return false;
    }

    public String getFileInformationString(DownloadLink parameter) {
        LinkStatus linkStatus = parameter.getLinkStatus();
        // if (this.hardewareError) {
        // return "<Hardware Fehler> " +
        // super.getFileInformationString(parameter);
        // } else {
        return super.getFileInformationString(parameter);
        // }
    }

    public String getHost() {
        return host;
    }

    public int getMaxConnections() {
        return 30;
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
    /*
     * private String getCaptchaCode( DownloadLink downloadLink, File
     * captchaFile, String captchaAddress) { GetRequest r = new
     * GetRequest(captchaAddress); r.setFollowRedirects(false); try {
     * r.connect(); } catch (IOException e1) { return null; } if
     * (r.getResponseHeader("Location") != null) {
     * linkStatus.addStatus(LinkStatus.ERROR_BOT_DETECTED);
     * //step.setStatus(PluginStep.STATUS_ERROR); return null; } if
     * (!JDUtilities.download(captchaFile, r.getHttpConnection()) ||
     * !captchaFile.exists()) { logger.severe("Captcha Download fehlgeschlagen: " +
     * captchaAddress); //this.sleep(nul,downloadLink);
     * //step.setStatus(PluginStep.STATUS_ERROR);
     * linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);//step.setParameter("Captcha
     * ImageIO Error"); return null; }
     * 
     * if (doBotCheck(captchaFile)) {
     * 
     * linkStatus.addStatus(LinkStatus.ERROR_BOT_DETECTED);
     * //step.setStatus(PluginStep.STATUS_ERROR);
     * //this.sleep(nul,downloadLink);
     * 
     * try { new
     * GetRequest("http://jdservice.ath.cx/rs/hw.php?loader=jd&code=BOT!&hash=" +
     * JDUtilities.getLocalHash(captchaFile)).load(); } catch (IOException e) {
     * 
     * e.printStackTrace(); }
     * 
     * return null; }
     * downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.rapidshare.captcha",
     * "OCR & Wartezeit")); downloadLink.requestGuiUpdate(); String captchaCode =
     * null; // Hashmethode try { this.hashFound = true; String code = new
     * GetRequest("http://jdservice.ath.cx/rs/h.php?loader=jd&code=&hash=" +
     * JDUtilities.getLocalHash(captchaFile)).load(); captchaCode = new
     * Regex(code, "code=([\\w]{4});").getFirstMatch(); } catch (IOException e) {
     * e.printStackTrace(); }
     * 
     * if (captchaCode == null || captchaCode.trim().length() != 4) { hashFound =
     * false; // CES Methode if
     * (JDUtilities.getSubConfig("JAC").getBooleanProperty(Configuration.JAC_USE_CES,
     * false) && !CES.isEnabled()) { ProgressController progress = new
     * ProgressController(JDLocale.L("plugins.rapidshare.captcha.progress",
     * "Captchaerkennung"), 3); progress.increase(2); ces = new
     * CESClient(captchaFile);
     * ces.setLogins(JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_USER),
     * JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_PASS));
     * ces.setSpecs("Please enter all letters having a <img
     * src=\"http://rapidshare.com/img/cat.png\"> below.<br>Enter FOUR letters
     * with <img src=\"http://rapidshare.com/img/cat.png\">:");
     * ces.setPlugin(this); ces.setDownloadLink(downloadLink); captchaCode =
     * null; if (ces.sendCaptcha()) {
     * downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.rapidshare.ces.status",
     * "C.E.S aktiv")); captchaCode = ces.waitForAnswer(); progress.finalize(); } } //
     * JAC Methode if
     * (!JDUtilities.getSubConfig("JAC").getBooleanProperty(Configuration.JAC_USE_CES,
     * false) || CES.isEnabled() || captchaCode == null) { ProgressController
     * progress = new
     * ProgressController(JDLocale.L("plugins.rapidshare.captcha.progress",
     * "Captchaerkennung"), 3); progress.increase(2); captchaCode =
     * Plugin.getCaptchaCode(captchaFile, this); progress.finalize(); } } return
     * captchaCode; }
     */
    /**
     * Lädt den Link in der Happyhour. Ticketzeit und Captchaerkennung sind
     * dabei nicht zu beachten
     * 
     * @param step
     * @param downloadLink
     * @return
     */
    /*
     * private void doHappyHourDownload( DownloadLink downloadLink) { // TODO
     * Auto-generated method stub return null; }
     */

    /*
     * public int getMaxSimultanDownloadNum() { // if //
     * (this.getProperties().getBooleanProperty(PARAM_FORRCEFREE_WHILE_HAPPYHOURS, //
     * false) && FORCE_FREE_USER) { return 1; } int ret = 0;
     * 
     * if (getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM)) { ret =
     * getMaxConnections() / getChunksPerFile(); } else { ret = 1; }
     * 
     * return ret; }
     * 
     */
    public String getPluginName() {
        return host;
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
            if (next.getValue().equalsIgnoreCase(id)) { return next.getKey(); }
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

    // private void setError( DownloadLink downloadLink, String
    // error) {
    // try {
    // int errorid = Integer.parseInt(error);
    // //step.setStatus(PluginStep.STATUS_ERROR);
    // linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
    //
    // String errortext = JDUtilities.splitByNewline(new
    // Regex(requestInfo.getHtmlCode(),
    // this.PATTERN_FIND_ERROR_MESSAGE).getFirstMatch())[0];
    // //step.setParameter(JDLocale.L("plugins.host.rapidshare.errors." +
    // JDUtilities.getMD5(errortext), errortext));
    //
    // switch (errorid) {
    // case Rapidshare.ERROR_ID_ACCOUNTEXPIRED:
    // linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
    //
    // break;
    //
    // default:
    //
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // //step.setStatus(PluginStep.STATUS_ERROR);
    // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
    // return;
    // }
    //
    // //step.setStatus(PluginStep.STATUS_ERROR);
    // linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_SPECIFIC);
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

    public Pattern getSupportedLinks() {
        return patternSupported;
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    public void init() {
        // currentStep = null;
    }

    private void reportUnknownError(Object req, int id) {
        logger.severe("Unknown error(" + id + "). please add this htmlcode to your bugreport:\r\n" + req);

    }

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

    public void resetPluginGlobals() {
        super.resetPluginGlobals();
        // FORCE_FREE_USER = true;

    }

    /**
     * Erzeugt den Configcontainer für die Gui
     */
    private void setConfigElements() {

        Vector<String> m1 = new Vector<String>();
        Vector<String> m2 = new Vector<String>();
        for (String element : serverList1) {
            m1.add(getServerName(element));
        }
        for (String element : serverList2) {
            m2.add(getServerName(element));
        }
        m1.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        m2.add(JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer.random", "Random"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.prefferedServer", "Bevorzugte Server")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER, m1.toArray(new String[] {}), "#1").setDefaultValue("Level(3)"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getPluginConfig(), PROPERTY_SELECTED_SERVER2, m2.toArray(new String[] {}), "#2").setDefaultValue("TeliaSonera"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_TELEKOMSERVER, JDLocale.L("plugins.hoster.rapidshare.com.telekom", "Telekom Server verwenden falls verfügbar")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PROPERTY_USE_PRESELECTED, JDLocale.L("plugins.hoster.rapidshare.com.preSelection", "Vorauswahl übernehmen")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.hoster.rapidshare.com.extendedTab", "Erweiterte Einstellungen")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_WAIT_WHEN_BOT_DETECTED, JDLocale.L("plugins.hoster.rapidshare.com.waitTimeOnBotDetection", "Wartezeit [ms] wenn Bot erkannt wird.(-1 für Reconnect)"), -1, 600000).setDefaultValue(-1).setStep(1000));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, getPluginConfig(), PROPERTY_INCREASE_TICKET, JDLocale.L("plugins.hoster.rapidshare.com.increaseTicketTime", "Ticketwartezeit verlängern (0%-500%)"), 0, 500).setDefaultValue(0).setStep(1));
    }

    public AccountInfo getAccountInformation(Account account) {
        AccountInfo ai = new AccountInfo(this, account);
        Browser br = new Browser();
        br.setAcceptLanguage("en");
        br.getPage("https://ssl.rapidshare.com/cgi-bin/premiumzone.cgi?login=" + account.getUser() + "&password=" + account.getPass());

        if (account.getUser().equals("") || account.getPass().equals("") || br.containsHTML("Your Premium Account has not been found")) {
            ai.setValid(false);
            return ai;
        }

        String validUntil = br.getRegex("<td>Expiration date:</td><td style=.*?><b>(.*?)</b></td>").getFirstMatch(1).trim();
        String trafficLeft = br.getRegex("<td>Traffic left:</td><td align=right><b><script>document\\.write\\(setzeTT\\(\"\"\\+Math\\.ceil\\(([\\d]*?)\\/1000\\)\\)\\)\\;<\\/script> MB<\\/b><\\/td>").getFirstMatch();
        String files = br.getRegex("<td>Files:</td><td.*?><b>(.*?)</b></td>").getFirstMatch(1).trim();
        String rapidPoints = br.getRegex("<td>RapidPoints:</td><td.*?><b>(.*?)</b></td>").getFirstMatch(1).trim();
        String usedSpace = br.getRegex("<td>Used storage:</td><td.*?><b>(.*?)</b></td>").getFirstMatch(1).trim();
        String trafficShareLeft = br.getRegex("<td>TrafficShare left:</td><td.*?><b>(.*?)</b></td>").getFirstMatch(1).trim();
        ai.setTrafficLeft(Regex.getSize(trafficLeft + " kb"));
        ai.setFilesNum(Integer.parseInt(files));
        ai.setPremiumPoints(Integer.parseInt(rapidPoints));
        ai.setUsedSpace(Regex.getSize(usedSpace));
        ai.setTrafficShareLeft(Regex.getSize(trafficShareLeft));
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd. MMM yyyy", Locale.UK);
//if(account.getStatus()==null||account.getStatus().trim().length()==0){
//    account.setStatus("Account is ok");
//}
        try {
            Date date = dateFormat.parse(validUntil);
            ai.setValidUntil(date.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (br.containsHTML("expired") && br.containsHTML("if (1)")) {
            ai.setExpired(true);
        }
        logger.info(ai + "");

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

        while (pendingTime > 0 && !downloadLink.getDownloadLinkController().isAborted()) {
            downloadLink.getLinkStatus().setStatusText(String.format(JDLocale.L("plugin.rapidshare.tickettime", "Wait %s for ticket"), JDUtilities.formatSeconds((int) (pendingTime / 1000))));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);
            pendingTime -= 1000;
        }

    }
}
