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
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
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

    static private final String CAPTCHA_WRONG = "Sicherheitsnummer nicht eingegeben";
    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "1.1.0";
    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    static private final String CODER = "JD-Team";
    static private final String DOWNLOAD_CAPTCHA = "download_captcha.tpl";
    static private final String DOWNLOAD_LIMIT = "download_limit.tpl";
    static private final String DOWNLOAD_START = "download_load.tpl";
    static private String LINK_PASS = null;

    // static private final String DOWNLOAD_WAIT = "download_wait.tpl";
    static private final Pattern DOWNLOAD_WAIT_TIME = Pattern.compile("countdown\\(([0-9]*),'change", Pattern.CASE_INSENSITIVE);
    /**
     * Muss static bleiben!!!. Das Rapidshare Plugin merkt sich so, dass es
     * gerade wartezeit hat. Überflüssige
     */

    static private final String FILE_DAMAGED = "(Die Datei wurde Opfer einer defekten Festplatte|Diese Datei liegt auf einem Server mit einem technischen Defekt. Wir konnten diese Datei leider nicht wieder herstellen)";

    static private final String FILE_NOT_FOUND = "Die Datei konnte leider nicht gefunden werden";
    static private final String HOST = "netload.in";

    static private long LAST_FILE_STARTED = 0;
    static private final String LIMIT_REACHED = "share/images/download_limit_go_on.gif";
    static private final String NEW_HOST_URL = "<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier\\.<\\/a>";
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

    private String fileStatusText;

    public Netloadin() {
        setConfigElements();
        this.enablePremium();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");

        // switch (step.getStep()) {
        // case PluginStep.STEP_WAIT_TIME:
        Browser.clearCookies(HOST);
        LAST_FILE_STARTED = System.currentTimeMillis();

        br.getPage(downloadLink.getDownloadURL());
        checkPassword(downloadLink,linkStatus);
        if(linkStatus.isFailed())return;
        String url = br.getRegex("<div class=\"Free_dl\"><a href=\"(.*?)\">").getFirstMatch();
        url = url.replaceAll("\\&amp\\;", "&");

        if (br.containsHTML(FILE_NOT_FOUND)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        if (br.containsHTML(FILE_DAMAGED)) {
            linkStatus.setErrorMessage("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }

        if (!br.containsHTML(DOWNLOAD_START)) {
            linkStatus.setErrorMessage("Download link not found");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }

        br.getPage(url);
        if (br.containsHTML(FILE_DAMAGED)) {
            linkStatus.setErrorMessage("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }

        if (!br.containsHTML(DOWNLOAD_CAPTCHA)) {
            linkStatus.setErrorMessage("Captcha not found");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }
        // logger.info(requestInfo.getHtmlCode());
        String captchaURL = br.getRegex("<img style=\".*?\" src=\"(.*?)\" alt=\"Sicherheitsbild\" \\/>").getFirstMatch();
        Form[] forms = br.getForms();
        Form captchaPost = forms[0];

        // String fileID =
        // HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode()).get("file_id");
        // String postURL = "http://" + HOST + "/" +
        // SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), POST_URL, 0);
        // logger.info(captchaURL + " - " + fileID + " - " + postURL);
        if (captchaURL == null) {
            if (requestInfo.getHtmlCode().indexOf("download_load.tpl") >= 0) {
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_RETRY);
                return;
            }
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
            return;
        }
        File file = this.getLocalCaptchaFile(this);

        if (!JDUtilities.download(file, br.openGetConnection(captchaURL)) || !file.exists()) {
            logger.severe("Captcha donwload failed: " + captchaURL);
            // this.sleep(nul,downloadLink);
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);// step.setParameter("Captcha
            // ImageIO
            // Error");
            return;
        }
        captchaPost.vars.put("captcha_check", this.getCaptchaCode(file, downloadLink));
        br.submitForm(captchaPost);
        if (br.containsHTML(FILE_NOT_FOUND)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        if (br.containsHTML(FILE_DAMAGED)) {
            logger.warning("File is on a damaged server");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }

        if (br.containsHTML(LIMIT_REACHED) || br.containsHTML(DOWNLOAD_LIMIT)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);

            long waitTime = Long.parseLong(new Regex(requestInfo.getHtmlCode(), DOWNLOAD_WAIT_TIME).getFirstMatch());
            waitTime = waitTime * 10L;

            linkStatus.setValue(waitTime);
            return;
        }
        if (br.containsHTML(CAPTCHA_WRONG)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }
        String finalURL = br.getRegex(NEW_HOST_URL).getFirstMatch();

        // case PluginStep.STEP_PENDING:
        sleep(20000, downloadLink);
        // case PluginStep.STEP_DOWNLOAD:

        dl = new RAFDownload(this, downloadLink, br.openGetConnection(finalURL));
        dl.startDownload();

    }

    private void checkPassword(DownloadLink downloadLink, LinkStatus linkStatus) {
        if(!br.containsHTML("download_password"))return;
        String pass = downloadLink.getStringProperty("LINK_PASSWORD", LINK_PASS);

        // falls das pw schon gesetzt und gespeichert wurde.. versucht er es
        // damit
        if (pass != null && br.containsHTML("download_password")) {
            Form[] forms = br.getForms();
            Form pw = forms[forms.length-1];
            pw.vars.put("password", pass);
            br.submitForm(pw);
        }
        // ansonsten 3 abfrageversuche
        int maxretries = 3;
        while (br.containsHTML("download_password") && maxretries-- >= 0) {
            Form[] forms = br.getForms();
            Form pw = forms[forms.length-1];
            pw.vars.put("password", pass = JDUtilities.getGUI().showUserInputDialog(String.format(JDLocale.L("plugins.netload.downloadPassword_question", "Password protected. Enter Password for %s"), downloadLink.getName())));
            br.submitForm(pw);
        }
        // falls falsch abbruch
        if (br.containsHTML("download_password")) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setErrorMessage(JDLocale.L("plugins.netload.downloadPassword_wrong", "Linkpassword is wrong"));
            return;
        }
        // richtiges pw... wird gesoeichert
        if (pass != null) {
            downloadLink.setProperty("LINK_PASSWORD", pass);
            LINK_PASS = pass;
        }
        
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadLink.setUrlDownload("http://netload.in/datei" + Netloadin.getID(downloadLink.getDownloadURL()) + ".htm");

        // switch (step.getStep()) {
        // case PluginStep.STEP_WAIT_TIME:
        // Login

        // SessionID holen
        br.setFollowRedirects(false);
        br.getPage("http://" + HOST);
        br.postPage("http://" + HOST + "/index.php", "txtuser=" + user + "&txtpass=" + pass + "&txtcheck=login&txtlogin=");
        if (br.getRedirectLocation() == null) {
            linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);

            return;
        }

        br.getPage(downloadLink);
        HTTPConnection con;
        if (br.getRedirectLocation() == null) {

            
            checkPassword(downloadLink,linkStatus);
            if(linkStatus.isFailed())return;
            if (br.containsHTML(FILE_NOT_FOUND)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                // step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }

            if (br.containsHTML(FILE_DAMAGED)) {
                linkStatus.setErrorMessage("File is on a damaged server");
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                return;
            }

        
            String url=br.getRedirectLocation();
            if(url==null) url=br.getRegex("<a class=\"Orange_Link\" href=\"(.*?)\" >Alternativ klicke hier.<\\/a>").getFirstMatch();
            if (url==null) {
                logger.severe("Download link not found");
                // step.setStatus(PluginStep.STATUS_ERROR);
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                return;
            }
            
            con = br.openGetConnection(url); 
        }else{
            con = br.openGetConnection(null); 
        }

        // case PluginStep.STEP_PENDING:
        sleep(100, downloadLink);

        // case PluginStep.STEP_GET_CAPTCHA_FILE:
        // step.setStatus(PluginStep.STATUS_SKIP);

        // case PluginStep.STEP_DOWNLOAD:
        // logger.info("Download " + finalURL);

       

        dl = new RAFDownload(this, downloadLink, con);
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
        Browser.clearCookies(HOST);

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

    }

    @Override
    public void resetPluginGlobals() {

    }

    private void setConfigElements() {

    }
}