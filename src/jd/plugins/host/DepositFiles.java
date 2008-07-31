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
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DepositFiles extends PluginForHost {

    static private final String CODER = "JD-Team";

    private static final String DOWNLOAD_NOTALLOWED = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

    static private final String FILE_NOT_FOUND = "Dieser File existiert nicht";

    // static private final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "0.1.3";

    // static private final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    static private final String HOST = "depositfiles.com";

    // private Pattern HIDDENPARAM = Pattern.compile("<input type=\"hidden\"
    // name=\"gateway_result\" value=\"([\\d]+)\">", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?depositfiles\\.com(/en/|/de/|/ru/|/)files/[0-9]+", Pattern.CASE_INSENSITIVE);

    private static final String PATTERN_PREMIUM_FINALURL = "var dwnsrc = \"(.*?)\";";

    // private Pattern ICID = Pattern.compile("name=\"icid\" value=\"(.*?)\"");

    private static final String PATTERN_PREMIUM_REDIRECT = "window.location.href = '(.*?)';";

    private String cookie;

    private Pattern FILE_INFO_NAME = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern FILE_INFO_SIZE = Pattern.compile("Dateigr&ouml;&szlig;e: <b>(.*?)</b>");

    // private String captchaAddress;

    private String finalURL;

    // Rechtschreibfehler übernommen
    private String PASSWORD_PROTECTED = "<strong>Bitte Password fuer diesem File eingeben</strong>";

    // private String icid;

    public DepositFiles() {

        super();
        // steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        // //steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        setConfigElements();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        LinkStatus linkStatus = parameter.getLinkStatus();

        RequestInfo requestInfo;

        DownloadLink downloadLink = parameter;
        Browser br = new Browser();

        // switch (step.getStep()) {

        // case PluginStep.STEP_WAIT_TIME:
        Browser.clearCookies("depositfiles.com");

        String link = downloadLink.getDownloadURL().replace("com/en/files/", "com/de/files/");
        link = link.replace("com/ru/files/", "com/de/files/");
        link = link.replace("com/files/", "com/de/files/");
        downloadLink.setUrlDownload(link);

        finalURL = link;

        br.getPage(finalURL);
        requestInfo = br.getRequest().getRequestInfo();
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {

            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_LINK_IN_PROGRESS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        if (new File(downloadLink.getFileOutput()).exists()) {

            logger.severe("File already exists. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_ALREADYEXISTS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        // Datei geloescht?
        if (requestInfo.containsHTML(FILE_NOT_FOUND)) {

            logger.severe("Download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        if (requestInfo.containsHTML(DOWNLOAD_NOTALLOWED)) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Download not possible now");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setParameter(60000l);
            return;

        }

        Form[] forms = br.getForms();
        br.submitForm(forms[1]);
        // requestInfo = HTTP.postRequest(new URL(finalURL),
        // requestInfo.getCookie(), finalURL, null,
        // "x=15&y=7&gateway_result=" +
        // SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(),
        // HIDDENPARAM, 1), true);
        requestInfo = br.getRequest().getRequestInfo();
        if (requestInfo.containsHTML(PASSWORD_PROTECTED)) {

            String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
            requestInfo = HTTP.postRequest(new URL(finalURL), requestInfo.getCookie(), finalURL, null, "go=1&gateway_result=1&file_password=" + password, true);

        }

        if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {

            logger.severe("Unknown error. Retry in 20 seconds");
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }
        // else {
        //
        // icid = SimpleMatches.getFirstMatch(requestInfo.getHtmlCode(),
        // ICID, 1);
        //
        // if (icid == null) {
        //
        // logger.severe("Session error");
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        // return;
        //
        // }
        //
        // captchaAddress =
        // "http://depositfiles.com/get_download_img_code.php?icid=" +
        // icid;
        // cookie = requestInfo.getCookie();
        // return;
        //
        // }

        // //case PluginStep.STEP_GET_CAPTCHA_FILE:
        //
        // File file = this.getLocalCaptchaFile(this);
        //
        // if (!JDUtilities.download(file, captchaAddress) ||
        // !file.exists()) {
        //
        // logger.severe("Captcha donwload failed: " + captchaAddress);
        // //this.sleep(nul,downloadLink);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(DownloadLink.
        // STATUS_ERROR_CAPTCHA_IMAGEERROR);
        // return;
        //
        // }
        // else {
        //
        // //step.setParameter(file);
        // //step.setStatus(PluginStep.STATUS_USER_INPUT);
        // return;
        // }
        finalURL = new Regex(br, "var dwnsrc = \"(.*?)\";").getFirstMatch();
        // case PluginStep.STEP_PENDING:
        // step.setStatus(PluginStep.STATUS_SKIP);

        // this.sleep(60000,downloadLink);
        // return;

        // case PluginStep.STEP_DOWNLOAD:

        // String code = this.getCaptchaCode(captchaFile);
        //
        // if (code == null || code.length() != 4) {
        //
        // logger.severe("Captcha donwload failed: " + captchaAddress);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(DownloadLink.
        // STATUS_ERROR_CAPTCHA_IMAGEERROR);
        // return;
        //
        // }
        //
        // if (code.length() != 4) {
        //
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA_WRONG
        // );
        // return;
        //
        // }

        // requestInfo = HTTP.postRequestWithoutHtmlCode(new
        // URL(finalURL + "#"), cookie, finalURL, "img_code=" + code +
        // "&icid=" + icid + "&file_password&gateway_result=1&go=1",
        // true);

        HTTPConnection con = br.openGetConnection(finalURL);
        if (con.getHeaderField("Location") != null && con.getHeaderField("Location").indexOf("error") > 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        
        logger.info("Filename: " + getFileNameFormHeader(con));

        if (getFileNameFormHeader(con) == null || getFileNameFormHeader(con).indexOf("?") >= 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        downloadLink.setName(getFileNameFormHeader(con));

        dl = new RAFDownload(this, downloadLink, con);

        dl.startDownload();

    }

    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        String user = account.getUser();
        String pass = account.getPass();
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        RequestInfo requestInfo;

        // switch (step.getStep()) {

        // case PluginStep.STEP_WAIT_TIME:

        String link = downloadLink.getDownloadURL().replace("com/en/files/", "com/de/files/");
        link = link.replace("com/ru/files/", "com/de/files/");
        link = link.replace("com/files/", "com/de/files/");
        downloadLink.setUrlDownload(link);

        finalURL = link;
        requestInfo = HTTP.getRequest(new URL(finalURL));
        cookie = requestInfo.getCookie();
        if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
            logger.severe("File already is in progress. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_LINK_IN_PROGRESS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;

        }

        if (new File(downloadLink.getFileOutput()).exists()) {
            logger.severe("File already exists. " + downloadLink.getFileOutput());
            linkStatus.addStatus(LinkStatus.ERROR_ALREADYEXISTS);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // Datei geloescht?
        if (requestInfo.containsHTML(FILE_NOT_FOUND)) {
            logger.severe("Download not found");
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        if (requestInfo.containsHTML(DOWNLOAD_NOTALLOWED)) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Download not possible now");
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setParameter(60000l);
            return;

        }

        Form[] forms = requestInfo.getForms();
        Form login = forms[0];
        login.vars.put("login", user);
        login.vars.put("password", pass);
        login.vars.put("x", "30");
        login.vars.put("y", "11");
        requestInfo = login.getRequestInfo();
        cookie += "; " + requestInfo.getCookie();

        finalURL = new Regex(requestInfo.getHtmlCode(), PATTERN_PREMIUM_REDIRECT).getFirstMatch();
        requestInfo = HTTP.getRequest(new URL(finalURL), cookie, finalURL, true);
        if (requestInfo.containsHTML(PASSWORD_PROTECTED)) {

            String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
            requestInfo = HTTP.postRequest(new URL(finalURL), requestInfo.getCookie(), finalURL, null, "go=1&gateway_result=1&file_password=" + password, true);

        } else {
            logger.info(requestInfo.getHtmlCode());
        }
        finalURL = new Regex(requestInfo.getHtmlCode(), PATTERN_PREMIUM_FINALURL).getFirstMatch();

        if (finalURL == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;

        }

        // //case PluginStep.STEP_GET_CAPTCHA_FILE:
        // //step.setStatus(PluginStep.STATUS_SKIP);
        // downloadLink.getLinkStatus().setStatusText("Premiumdownload");
        // step = nextStep(step);

        // case PluginStep.STEP_PENDING:

        // step.setStatus(PluginStep.STATUS_SKIP);

        // case PluginStep.STEP_DOWNLOAD:

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(finalURL), cookie, finalURL, true);

        if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

        if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {

            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            // step.setParameter(20000l);
            return;

        }

        downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));

        dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
        dl.setResume(true);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));

        dl.startDownload();

    }

    @Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        RequestInfo requestInfo;
        String link = downloadLink.getDownloadURL().replace("/en/files/", "/de/files/");
        link = link.replace("/ru/files/", "/de/files/");

        try {

            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), null, null, false);

            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                return false;
            } else {

                if (requestInfo.getConnection().getHeaderField("Location") != null) {
                    requestInfo = HTTP.getRequest(new URL("http://" + HOST + requestInfo.getConnection().getHeaderField("Location")), null, null, true);
                } else {
                    requestInfo = HTTP.readFromURL(requestInfo.getConnection());
                }

                // Datei geloescht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) { return false; }

                String fileName = new Regex(requestInfo.getHtmlCode(), FILE_INFO_NAME).getFirstMatch();
                downloadLink.setName(fileName);
                String fileSizeString = new Regex(requestInfo.getHtmlCode(), FILE_INFO_SIZE).getFirstMatch();
                int length = (int) Regex.getSize(fileSizeString);

                downloadLink.setDownloadMax(length);

            }

            return true;

        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }

        return false;

    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true) && getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return 20;
        } else {
            return 1;
        }
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void reset() {
        finalURL = null;
    }

    @Override
    public void resetPluginGlobals() {
        finalURL = "";
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

    }

}
