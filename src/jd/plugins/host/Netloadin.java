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
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Browser;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://www.netload.in/datei408a37036e4ceacf1d24857fbc9acbed.htm
// http://netload.in/datei0eabdd9b6897b96bd2970a9b54afc284.htm
//  http://netload.in/mindestens20zeichen
//http://netload.in/datei47f13cf27d3f9104b19553abf57eba8e/Svyatie.iz.bundoka.by.Shevlyakov.part02.rar.htm
public class Netloadin extends PluginForHost {
    static private final String AGB_LINK = "http://netload.in/index.php?id=13";
    // <img src="share/includes/captcha.php?t=1189894445" alt="Sicherheitsbild"
    // />
    static private final String CAPTCHA_URL = "<img src=\"share/includes/captcha.php?t=°\" alt=\"Sicherheitsbild\" />";
    static private final String CAPTCHA_WRONG = "Sicherheitsnummer nicht eingegeben";
    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "1.1.0";
    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    static private final String CODER = "JD-Team";
    static private final String DOWNLOAD_CAPTCHA = "download_captcha.tpl";
    static private final String DOWNLOAD_LIMIT = "download_limit.tpl";
    static private final String DOWNLOAD_START = "download_load.tpl";
    // /Simplepattern
    static private final String DOWNLOAD_URL = "<div class=\"Free_dl\"><a href=\"°\">";
    // static private final String DOWNLOAD_WAIT = "download_wait.tpl";
    static private final Pattern DOWNLOAD_WAIT_TIME = Pattern.compile("countdown\\(([0-9]*),'change", Pattern.CASE_INSENSITIVE);
    /**
     * Muss static bleiben!!!. Das Rapidshare Plugin merkt sich so, dass es
     * gerade wartezeit hat. Überflüssige
     */
    private static long END_OF_DOWNLOAD_LIMIT = 0;
    static private final String FILE_DAMAGED = "Diese Datei liegt auf einem Server mit einem technischen Defekt. Wir konnten diese Datei leider nicht wieder herstellen.";
    static private final String FILE_HARDWARE = "Die Datei wurde Opfer einer defekten Festplatte";
    static private final String FILE_NOT_FOUND = "Die Datei konnte leider nicht gefunden werden";
    static private final String HOST = "netload.in";

    static private long LAST_FILE_STARTED = 0;
    static private final String LIMIT_REACHED = "share/images/download_limit_go_on.gif";
    static private final String NEW_HOST_URL = "<a class=\"Orange_Link\" href=\"°\" >Alternativ klicke hier.</a>";
    // http://netload.in/datei47f13cf27d3f9104b19553abf57eba8e/Svyatie.iz.bundoka.by.Shevlyakov.part02.rar.htm
    static private final Pattern PAT_SUPPORTED = Pattern.compile("(http://[\\w\\.]*?netload\\.in/(?!index\\.php).*|http://.*?netload\\.in/(?!index\\.php).*/.*)", Pattern.CASE_INSENSITIVE);
    // <form method="post" action="index.php?id=10">
    static private final String POST_URL = "<form method=\"post\" action=\"°\">";
    private static final String PROPERTY_TRY_2_SIMULTAN = "TRY_2_SIMULTAN";

    private static String getID(String link) {
        // http://www.netload.in/datei408a37036e4ceacf1d24857fbc9acbed.htm
        // http://netload.in/datei0eabdd9b6897b96bd2970a9b54afc284.htm
        // http://netload.in/mindestens20zeichen
        // http://netload.in/datei47f13cf27d3f9104b19553abf57eba8e/Svyatie.iz.bundoka.by.Shevlyakov.part02.rar.htm

        return new Regex(link, "\\/datei([a-fA-F0-9]{32})").getFirstMatch();

    }

    private String captchaURL;
    private String fileID;
    private String fileStatusText;
    private String finalURL;
    private String postURL;
    private String sessionID;
    private String userCookie;

    private Long waitTime;

    public Netloadin() {
        setConfigElements();
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");
        
        // switch (step.getStep()) {
        // case PluginStep.STEP_WAIT_TIME:

        LAST_FILE_STARTED = System.currentTimeMillis();

        if (END_OF_DOWNLOAD_LIMIT > System.currentTimeMillis()) {
            long waitTime = END_OF_DOWNLOAD_LIMIT - System.currentTimeMillis();
            logger.severe("wait (intern) " + waitTime + " minutes");
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            // step.setStatus(PluginStep.STATUS_ERROR);

            linkStatus.setValue((int) waitTime);
            return;
        }
        logger.info("Intern: " + END_OF_DOWNLOAD_LIMIT + " - " + System.currentTimeMillis());

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
        sessionID = requestInfo.getCookie();
        String url = "http://" + HOST + "/" + SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL, 0);
        url = url.replaceAll("\\&amp\\;", "&");

        if (requestInfo.containsHTML(FILE_NOT_FOUND)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        if (requestInfo.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        if (requestInfo.containsHTML(FILE_HARDWARE)) {
            logger.warning("File is on a damaged harddisk");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        if (!requestInfo.containsHTML(DOWNLOAD_START)) {
            logger.severe("Download link not found");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        logger.info(url);
        requestInfo = HTTP.getRequest(new URL(url), sessionID, null, true);

        if (requestInfo.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        if (requestInfo.containsHTML(FILE_HARDWARE)) {
            logger.warning("File is on a damaged harddisk");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }

        if (!requestInfo.containsHTML(DOWNLOAD_CAPTCHA)) {
            logger.severe("Captcha not found");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        // logger.info(requestInfo.getHtmlCode());
        captchaURL = "http://" + HOST + "/share/includes/captcha.php?t=" + SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), CAPTCHA_URL, 0);
        fileID = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode()).get("file_id");
        postURL = "http://" + HOST + "/" + SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), POST_URL, 0);
        logger.info(captchaURL + " - " + fileID + " - " + postURL);
        if (captchaURL == null || fileID == null || postURL == null) {
            if (requestInfo.getHtmlCode().indexOf("download_load.tpl") >= 0) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }
        File file = this.getLocalCaptchaFile(this);

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(captchaURL), sessionID, requestInfo.getLocation(), false);
        if (!JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {
            logger.severe("Captcha donwload failed: " + captchaURL);
            // this.sleep(nul,downloadLink);
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
            // ImageIO
            // Error");
            return;
        }

        requestInfo = HTTP.postRequest(new URL(postURL), sessionID, requestInfo.getLocation(), null, "file_id=" + fileID + "&captcha_check=" + this.getCaptchaCode(file, downloadLink) + "&start=", false);
        if (requestInfo.containsHTML(FILE_NOT_FOUND)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        if (requestInfo.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }

        if (requestInfo.containsHTML(FILE_HARDWARE)) {
            logger.warning("File is on a damaged harddisk");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        if (requestInfo.getHtmlCode().indexOf(LIMIT_REACHED) >= 0 || requestInfo.containsHTML(DOWNLOAD_LIMIT)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);

            waitTime = Long.parseLong(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_WAIT_TIME).getFirstMatch());
            waitTime = waitTime * 10L;
            // step.setParameter(waitTime);
            END_OF_DOWNLOAD_LIMIT = System.currentTimeMillis() + waitTime;
            linkStatus.setValue(waitTime.intValue());
            return;
        }
        if (requestInfo.getHtmlCode().indexOf(CAPTCHA_WRONG) >= 0) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        finalURL = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), NEW_HOST_URL, 0);

        // case PluginStep.STEP_PENDING:
        sleep(20000, downloadLink);
        // case PluginStep.STEP_DOWNLOAD:
        
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(finalURL), sessionID, null, false);
        
        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
        dl.startDownload();

    }
    public void handlePremium(DownloadLink downloadLink,Account account) throws Exception{String user=account.getUser();String pass=account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");
        
        // switch (step.getStep()) {
        // case PluginStep.STEP_WAIT_TIME:
        // Login

        // SessionID holen

        requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
        sessionID = requestInfo.getCookie();
        logger.finer("sessionID: " + sessionID);

        if (requestInfo.getHtmlCode().indexOf(FILE_NOT_FOUND) > 0) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        if (requestInfo.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        if (requestInfo.containsHTML(FILE_HARDWARE)) {
            logger.warning("File is on a damaged harddisk");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        if (!requestInfo.containsHTML(DOWNLOAD_START)) {
            logger.severe("Download link not found");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;
        }

        // Login Cookie abholen
        requestInfo = HTTP.postRequest(new URL("http://" + HOST + "/index.php"), sessionID, downloadLink.getDownloadURL(), null, "txtuser=" + user + "&txtpass=" + pass + "&txtcheck=login&txtlogin=", false);
        userCookie = requestInfo.getCookie();

        // Vorbereitungsseite laden
        requestInfo = HTTP.getRequest(new URL("http://" + HOST + "/" + requestInfo.getLocation()), sessionID + " " + userCookie, null, false);

        if (requestInfo.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        if (requestInfo.containsHTML(FILE_HARDWARE)) {
            logger.warning("File is on a damaged harddisk");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }

        if (requestInfo.getLocation() != null) {
            // Direktdownload
            logger.info("Directdownload aktiviert");
            finalURL = requestInfo.getLocation();
        } else {
            finalURL = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), NEW_HOST_URL, 0);
        }
        if (finalURL == null) {
            logger.info(requestInfo + "");
            logger.severe("Could not get final URL");
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // case PluginStep.STEP_PENDING:
        sleep(100, downloadLink);

        // case PluginStep.STEP_GET_CAPTCHA_FILE:
        // step.setStatus(PluginStep.STATUS_SKIP);

        // case PluginStep.STEP_DOWNLOAD:
        // logger.info("Download " + finalURL);

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(finalURL), sessionID, null, false);
        

        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

        dl.setLoadPreBytes(1);
        dl.startDownload();

    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        //

        Browser br = new Browser();
        br.setConnectTimeout(15000);
        String id = Netloadin.getID(downloadLink.getDownloadURL());
        String page = br.getPage("http://netload.in/share/fileinfos2.php?file_id=" + id);
        for (int i = 0; i < 3; i++) {
            if (page != null) {
                break;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
            page = br.getPage("http://netload.in/share/fileinfos2.php?file_id=" + id);

        }

        if (page == null) { return false; }

        if (Regex.matches(page, "unknown file_data")) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return false;
        }

        String[] entries = Regex.getLines(page);

        if (entries.length < 3) { return false; }

        downloadLink.setName(entries[0]);
        fileStatusText = entries[2];
        downloadLink.setDownloadMax((int) Regex.getSize(entries[1]));

        if (entries[2].equalsIgnoreCase("online")) { return true; }
        return false;

    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + fileStatusText + ")";
    }

    @Override
    public String getHost() {
        return HOST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.Plugin#doBotCheck(java.io.File)
     */

    @Override
    public int getMaxSimultanDownloadNum() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return 20;

        } else {
            if (System.currentTimeMillis() - LAST_FILE_STARTED > 1000 || !getProperties().getBooleanProperty("TRY_2_SIMULTAN", false)) {
                // 1 sekunde nach dem 1. downloadstart wird hier versucht erneut
                // eine datei zu laden
                return 1;
            } else {
                return 1;
            }

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#reset()
     */

    @Override
    public String getPluginName() {
        return HOST;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#checkAvailability(jd.plugins.DownloadLink)
     */

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    // 
    // public URLConnection getURLConnection() {
    // // XXX: ???
    // return null;
    // }


    @Override
    public void reset() {
        requestInfo = null;
        sessionID = null;
        captchaURL = null;
        fileID = null;
        postURL = null;
        finalURL = null;
    }

    @Override
    public void resetPluginGlobals() {
        END_OF_DOWNLOAD_LIMIT = 0l;
    }

    private void setConfigElements() {

        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.host.premium.account", "Premium Account")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, JDLocale.L("plugins.host.premium.user", "Benutzer")));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, JDLocale.L("plugins.host.premium.password", "Passwort")));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, JDLocale.L("plugins.host.premium.useAccount", "Premium Account verwenden")));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_TRY_2_SIMULTAN, JDLocale.L("plugins.host.netload.try2SimultanDownloads", "Versuchen 2 Dateien gleichzeitig zu laden")));
        cfg.setDefaultValue(false);

    }
}