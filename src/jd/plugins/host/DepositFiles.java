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
import jd.controlling.interaction.CaptchaMethodLoader;
import jd.plugins.DownloadLink;
import jd.plugins.Form;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DepositFiles extends PluginForHost {

    static private final Pattern PAT_SUPPORTED            = Pattern.compile("http://.*?depositfiles\\.com(/en/|/de/|/ru/|/)files/[0-9]+", Pattern.CASE_INSENSITIVE);

    static private final String  HOST                     = "depositfiles.com";

    static private final String  PLUGIN_NAME              = HOST;

    static private final String  PLUGIN_VERSION           = "0.1.3";

    static private final String  PLUGIN_ID                = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final String  CODER                    = "JD-Team";

    private Pattern              HIDDENPARAM              = Pattern.compile("<input type=\"hidden\" name=\"gateway_result\" value=\"([\\d]+)\">", Pattern.CASE_INSENSITIVE);

    private Pattern              FILE_INFO_NAME           = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern              FILE_INFO_SIZE           = Pattern.compile("Dateigr&ouml;&szlig;e: <b>(.*?)</b>");

    private Pattern              ICID                     = Pattern.compile("name=\"icid\" value=\"(.*?)\"");

    // Rechtschreibfehler übernommen
    private String               PASSWORD_PROTECTED       = "<strong>Bitte Password fuer diesem File eingeben</strong>";

    static private final String  FILE_NOT_FOUND           = "Dieser File existiert nicht";

    private static final String  DOWNLOAD_NOTALLOWED      = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

    private static final String  PATTERN_PREMIUM_REDIRECT = "window.location.href = '°';";

    private static final String  PATTERN_PREMIUM_FINALURL = "var dwnsrc = \"°\";";

    private String               captchaAddress;

    private String               finalURL;

    private String               cookie;

    private String               icid;

    public DepositFiles() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        setConfigElements();
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

    @Override
    public String getCoder() {
        return CODER;
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
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    public PluginStep doStep(PluginStep step, DownloadLink parameter) throws MalformedURLException, IOException {
        DownloadLink downloadLink = (DownloadLink) parameter;

        if (step == null) {
            logger.info("Plugin Ende erreicht.");
            return null;
        }
        if (aborted) {

            logger.warning("Plugin aborted");
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            step.setStatus(PluginStep.STATUS_TODO);
            return step;
        }
        logger.info("get Next Step " + step);
        // premium
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)&& this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {
            return this.doPremiumStep(step, downloadLink);
        }
        else {
            return this.doFreeStep(step, downloadLink);
        }

    }

    private PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) {

        RequestInfo requestInfo;
        String user = (String) this.getProperties().getProperty("PREMIUM_USER");
        String pass = (String) this.getProperties().getProperty("PREMIUM_PASS");
        try {

            switch (step.getStep()) {

                case PluginStep.STEP_WAIT_TIME:

                    String link = downloadLink.getDownloadURL().replace("com/en/files/", "com/de/files/");
                    link = link.replace("com/ru/files/", "com/de/files/");
                    link = link.replace("com/files/", "com/de/files/");
                    downloadLink.setUrlDownload(link);

                    finalURL = link;
                    requestInfo = getRequest(new URL(finalURL));
                    cookie = requestInfo.getCookie();
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

                    // Datei geloescht?
                    if (requestInfo.containsHTML(FILE_NOT_FOUND)) {
                        logger.severe("Download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    if (requestInfo.containsHTML(DOWNLOAD_NOTALLOWED)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Download not possible now");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TO_MANY_USERS);
                        step.setParameter(60000l);
                        return step;

                    }
               
                    Form[] forms = requestInfo.getForms();
                    Form login = forms[0];
                    login.vars.put("login", user);
                    login.vars.put("password", pass);
                    login.vars.put("x", "30");
                    login.vars.put("y", "11");
                    requestInfo = login.getRequestInfo();
                    cookie += "; " + requestInfo.getCookie();
                  
                 
                    finalURL = getSimpleMatch(requestInfo.getHtmlCode(), PATTERN_PREMIUM_REDIRECT, 0);
                    requestInfo = getRequest(new URL(finalURL), cookie, finalURL, true);
                    if (requestInfo.containsHTML(PASSWORD_PROTECTED)) {

                        String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
                        requestInfo = postRequest(new URL(finalURL), requestInfo.getCookie(), finalURL, null, "go=1&gateway_result=1&file_password=" + password, true);

                    }else{
                        logger.info(requestInfo.getHtmlCode());
                    }
                    finalURL = getSimpleMatch(requestInfo.getHtmlCode(), PATTERN_PREMIUM_FINALURL, 0);
                   
                    if (finalURL == null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        return step;

                    }
                    return step;
                case PluginStep.STEP_GET_CAPTCHA_FILE:
                    step.setStatus(PluginStep.STATUS_SKIP);
                    downloadLink.setStatusText("Premiumdownload");
                    step = nextStep(step);

                case PluginStep.STEP_PENDING:

                    step.setStatus(PluginStep.STATUS_SKIP);
                    downloadLink.setStatusText("Connecting");
                    step = nextStep(step);

                case PluginStep.STEP_DOWNLOAD:

                    requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), cookie, finalURL, true);

                    if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {

                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;

                    }

                    logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

                    if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {

                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;

                    }

                    downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));

                   dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
                    dl.setResume(true);dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS,3));
                    
                    if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);

                    }
                    return step;

            }
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            return step;

        }
        catch (IOException e) {

            e.printStackTrace();

        }

        step.setStatus(PluginStep.STATUS_ERROR);
        return step;
    }

    public PluginStep doFreeStep(PluginStep step, DownloadLink parameter) {

        RequestInfo requestInfo;

        try {

            DownloadLink downloadLink = (DownloadLink) parameter;

            switch (step.getStep()) {

                case PluginStep.STEP_WAIT_TIME:

                    String link = downloadLink.getDownloadURL().replace("com/en/files/", "com/de/files/");
                    link = link.replace("com/ru/files/", "com/de/files/");
                    link = link.replace("com/files/", "com/de/files/");
                    downloadLink.setUrlDownload(link);

                    finalURL = link;
                    requestInfo = getRequest(new URL(finalURL));

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

                    // Datei geloescht?
                    if (requestInfo.containsHTML(FILE_NOT_FOUND)) {

                        logger.severe("Download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }

                    if (requestInfo.containsHTML(DOWNLOAD_NOTALLOWED)) {

                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Download not possible now");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TO_MANY_USERS);
                        step.setParameter(60000l);
                        return step;

                    }

                    requestInfo = postRequest(new URL(finalURL), requestInfo.getCookie(), finalURL, null, "x=15&y=7&gateway_result=" + getFirstMatch(requestInfo.getHtmlCode(), HIDDENPARAM, 1), true);

                    if (requestInfo.containsHTML(PASSWORD_PROTECTED)) {

                        String password = JDUtilities.getController().getUiInterface().showUserInputDialog(JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
                        requestInfo = postRequest(new URL(finalURL), requestInfo.getCookie(), finalURL, null, "go=1&gateway_result=1&file_password=" + password, true);

                    }

                    if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {

                        logger.severe("Unknown error. Retry in 20 seconds");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;

                    }
                    else {

                        icid = getFirstMatch(requestInfo.getHtmlCode(), ICID, 1);

                        if (icid == null) {

                            logger.severe("Session error");
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            return step;

                        }

                        captchaAddress = "http://depositfiles.com/get_download_img_code.php?icid=" + icid;
                        cookie = requestInfo.getCookie();
                        return step;

                    }

                case PluginStep.STEP_GET_CAPTCHA_FILE:

                    File file = this.getLocalCaptchaFile(this);

                    if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {

                        logger.severe("Captcha donwload failed: " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;

                    }
                    else {

                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                        return step;
                    }

                case PluginStep.STEP_PENDING:

                    step.setParameter(60000l);
                    return step;

                case PluginStep.STEP_DOWNLOAD:

                    String code = (String) steps.get(1).getParameter();

                    if (code == null || code.length() != 4) {

                        logger.severe("Captcha donwload failed: " + captchaAddress);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;

                    }

                    if (code.length() != 4) {

                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        return step;

                    }

                    requestInfo = postRequestWithoutHtmlCode(new URL(finalURL + "#"), cookie, finalURL, "img_code=" + code + "&icid=" + icid + "&file_password&gateway_result=1&go=1", true);

                    if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {

                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;

                    }

                    int length = requestInfo.getConnection().getContentLength();
                    downloadLink.setDownloadMax(length);
                    logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

                    if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {

                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;

                    }

                    downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));

                    if (!hasEnoughHDSpace(downloadLink)) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }

                   dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());

                    if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);

                    }
                    return step;

            }
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            return step;

        }
        catch (IOException e) {

            e.printStackTrace();

        }

        step.setStatus(PluginStep.STATUS_ERROR);
        return step;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        this.finalURL = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        RequestInfo requestInfo;
        String link = downloadLink.getDownloadURL().replace("/en/files/", "/de/files/");
        link = link.replace("/ru/files/", "/de/files/");

        try {

            requestInfo = getRequestWithoutHtmlCode(new URL(link), null, null, false);

            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                return false;
            }
            else {

                if (requestInfo.getConnection().getHeaderField("Location") != null) {
                    requestInfo = getRequest(new URL("http://" + HOST + requestInfo.getConnection().getHeaderField("Location")), null, null, true);
                }
                else {
                    requestInfo = readFromURL(requestInfo.getConnection());
                }

                // Datei geloescht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                    return false;
                }

                String fileName = getFirstMatch(requestInfo.getHtmlCode(), FILE_INFO_NAME, 1);
                downloadLink.setName(fileName);
                String fileSizeString = getFirstMatch(requestInfo.getHtmlCode(), FILE_INFO_SIZE, 1);
                int indexOfSpace = fileSizeString.length();
                if (fileSizeString.indexOf("&nbsp;") != -1) indexOfSpace = fileSizeString.indexOf("&nbsp;");
                double fileSize = Double.parseDouble(fileSizeString.substring(0, indexOfSpace));

                if (fileSizeString != null) {

                    int length = 0;

                    if (fileSizeString.contains("MB")) {
                        length = (int) Math.round(fileSize * 1024 * 1024);
                    }
                    else if (fileSizeString.contains("KB")) {
                        length = (int) Math.round(fileSize * 1024);
                    }
                    else {
                        length = (int) Math.round(fileSize);
                    }

                    downloadLink.setDownloadMax(length);

                }

            }

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
        return 1;
    }

    @Override
    public void resetPluginGlobals() {
        this.finalURL = "";
    }

    @Override
    public String getAGBLink() {
        return "http://depositfiles.com/en/agreem.html";
    }

}
