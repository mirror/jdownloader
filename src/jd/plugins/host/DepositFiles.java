package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.regex.Pattern;

import jd.controlling.interaction.CaptchaMethodLoader;
import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class DepositFiles extends PluginForHost {

    static private final Pattern PAT_SUPPORTED       = Pattern.compile("http://.*?depositfiles\\.com(/en/|/de/|/ru/|/)files/[0-9]+", Pattern.CASE_INSENSITIVE);

    static private final String  HOST                = "depositfiles.com";

    static private final String  PLUGIN_NAME         = HOST;

    static private final String  PLUGIN_VERSION      = "0.1.3";

    static private final String  PLUGIN_ID           = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final String  CODER               = "JD-Team";

    private Pattern              HIDDENPARAM         = Pattern.compile("<input type=\"hidden\" name=\"gateway_result\" value=\"([\\d]+)\">", Pattern.CASE_INSENSITIVE);

    private Pattern              FILE_INFO_NAME      = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);

    private Pattern              FILE_INFO_SIZE      = Pattern.compile("Dateigr&ouml;&szlig;e: <b>(.*?)</b>");

    private Pattern              ICID                = Pattern.compile("name=\"icid\" value=\"(.*?)\"");

    // Rechtschreibfehler übernommen
    private String               PASSWORD_PROTECTED  = "<strong>Bitte Password fuer diesem File eingeben</strong>";

    static private final String  FILE_NOT_FOUND      = "Dieser File existiert nicht";

    private static final String  DOWNLOAD_NOTALLOWED = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

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

    public PluginStep doStep(PluginStep step, DownloadLink parameter) {

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

                    Download dl = new Download(this, downloadLink, requestInfo.getConnection());

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
