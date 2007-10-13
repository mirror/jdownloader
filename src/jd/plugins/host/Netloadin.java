package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * 
 */
public class Netloadin extends PluginForHost {
    // http://www.netload.in/datei408a37036e4ceacf1d24857fbc9acbed.htm
    static private final Pattern PAT_SUPPORTED    = getSupportPattern("http://[*]netload.in/[+]");
    static private final String  HOST             = "netload.in";
    static private final String  PLUGIN_NAME      = HOST;
    static private final String  PLUGIN_VERSION   = "1.0.0";
    static private final String  PLUGIN_ID        = PLUGIN_NAME + "-" + VERSION;
    static private final String  CODER            = "coalado";
    // /Simplepattern
    static private final String  DOWNLOAD_URL     = "<div class=\"Free_dl\"><a href=\"°\">";
    // <img src="share/includes/captcha.php?t=1189894445" alt="Sicherheitsbild"
    // />
    static private final String  CAPTCHA_URL      = "<img src=\"share/includes/captcha.php?t=°\" alt=\"Sicherheitsbild\" />";
    // <form method="post" action="index.php?id=10">
    static private final String  POST_URL         = "<form method=\"post\" action=\"°\">";
    static private final String  LIMIT_REACHED    = "share/images/download_limit_go_on.gif";
    static private final String  CAPTCHA_WRONG    = "Sicherheitsnummer nicht eingegeben";
    static private final String  NEW_HOST_URL     = "<a class=\"Orange_Link\" href=\"°\" >Hier geht es zum Download</a>";
    static private final String  FILE_NOT_FOUND   = "Datei nicht vorhanden";
    static private final String  DOWNLOAD_LIMIT   = "download_limit.tpl";
    static private final String  DOWNLOAD_CAPTCHA = "download_captcha.tpl";
    static private final String  DOWNLOAD_START   = "download_load.tpl";
    static private final String  DOWNLOAD_WAIT    = "download_wait.tpl";
    private String               finalURL;
    private String               captchaURL;
    private String               fileID;
    private String               postURL;
    private String               sessionID;
    public Netloadin() {
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
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
    // @Override
    // public URLConnection getURLConnection() {
    // // XXX: ???
    // return null;
    // }
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;
            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME:
                    if (captchaURL == null) {
                        logger.info(downloadLink.getUrlDownloadDecrypted());
                        requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()), null, null, true);
                        this.sessionID = requestInfo.getCookie();
                        String url = "http://" + HOST + "/" + getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL, 0);
                        url = url.replaceAll("\\&amp\\;", "&");
                        if (!requestInfo.containsHTML(DOWNLOAD_START)) {
                            logger.warning("ERROR: NO " + DOWNLOAD_START);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            return step;
                        }
                        logger.info(url);
                        requestInfo = getRequest(new URL(url), sessionID, null, true);
                        if (!requestInfo.containsHTML(DOWNLOAD_CAPTCHA)) {
                            logger.warning("ERROR: NO " + DOWNLOAD_CAPTCHA);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            return step;
                        }
                        // logger.info(requestInfo.getHtmlCode());
                        this.captchaURL = "http://" + HOST + "/share/includes/captcha.php?t=" + getSimpleMatch(requestInfo.getHtmlCode(), CAPTCHA_URL, 0);
                        this.fileID = this.getInputHiddenFields(requestInfo.getHtmlCode()).get("file_id");
                        this.postURL = "http://" + HOST + "/" + getSimpleMatch(requestInfo.getHtmlCode(), POST_URL, 0);
                        logger.info(captchaURL + " - " + fileID + " - " + postURL);
                        if (captchaURL == null || fileID == null || postURL == null) {
                            if (requestInfo.getHtmlCode().indexOf("download_load.tpl") >= 0) {
                                step.setStatus(PluginStep.STATUS_ERROR);
                                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                                return step;
                            }
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            return step;
                        }
                        else {
                            return step;
                        }
                    }
                    else {
                        requestInfo = postRequest(new URL(postURL), sessionID, requestInfo.getLocation(), null, "file_id=" + fileID + "&captcha_check=" + (String) steps.get(1).getParameter() + "&start=", false);
                        if (requestInfo.getHtmlCode().indexOf(LIMIT_REACHED) >= 0 || requestInfo.containsHTML(DOWNLOAD_LIMIT)) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                            // TODO: Richtige Wartezeit bestimmen
                            step.setParameter(60 * 60 * 1000l);
                            return step;
                        }
                        if (requestInfo.getHtmlCode().indexOf(CAPTCHA_WRONG) >= 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                            return step;
                        }
                        this.finalURL = getSimpleMatch(requestInfo.getHtmlCode(), NEW_HOST_URL, 0);
                        return step;
                    }
                case PluginStep.STEP_PENDING:
                    step.setParameter(20000l);
                    return step;
                case PluginStep.STEP_GET_CAPTCHA_FILE:
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                    }
                    File file = this.getLocalCaptchaFile(this);
                    requestInfo = getRequestWithoutHtmlCode(new URL(captchaURL), this.sessionID, requestInfo.getLocation(), false);
                    if (!JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaURL);
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
                case PluginStep.STEP_DOWNLOAD:
                    logger.info("dl " + finalURL);
                    requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), sessionID, null, false);
                    int length = requestInfo.getConnection().getContentLength();
                    downloadLink.setDownloadMax(length);
                    logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));
                    downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                    if (!download(downloadLink, (URLConnection) requestInfo.getConnection())) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    }
                    else {
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    }
                    return step;
            }
            return step;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.Plugin#doBotCheck(java.io.File)
     */
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#reset()
     */
    @Override
    public void reset() {
        requestInfo = null;
        this.sessionID = null;
        this.captchaURL = null;
        this.fileID = null;
        this.postURL = null;
        this.finalURL = null;
    }
    /*
     * (non-Javadoc)
     * 
     * @see jd.plugins.PluginForHost#checkAvailability(jd.plugins.DownloadLink)
     */
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {
            requestInfo = getRequestWithoutHtmlCode(new URL(downloadLink.getUrlDownloadDecrypted()), null, null, false);
            if (requestInfo.getHtmlCode().indexOf(FILE_NOT_FOUND) > 0) {
                return false;
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
}
