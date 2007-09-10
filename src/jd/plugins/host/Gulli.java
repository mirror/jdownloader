package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

/**
 * HostPlugin für gullishare TODO: Erzwungene Wartezeit (gibt es die überhaupt
 * noch?)
 */
public class Gulli extends PluginForHost {
    static private final Pattern PAT_SUPPORTED      = Pattern.compile("http://share.gulli.com/.*");

    static private final Pattern PAT_CAPTCHA        = Pattern.compile("<img src=\"(/captcha[^\"]*)");

    static private final Pattern PAT_FILE_ID        = Pattern.compile("<input type=\"hidden\" name=\"file\" value=\"([^\"]*)");

    static private final Pattern PAT_DOWNLOAD_URL   = Pattern.compile("<form action=\"/(download[^\"]*)");

    static private final Pattern PAT_DOWNLOAD_LIMIT = Pattern.compile("timeLeft=([^\"]*)&");

    static private final Pattern PAT_DOWNLOAD_ERROR = Pattern.compile("share.gulli.com/error([^\"]*)");

    static private final String  HOST_URL           = "http://share.gulli.com/";

    static private final String  DOWNLOAD_URL       = "http://share.gulli.com/download";

    static private final String  HOST               = "share.gulli.com";

    static private final String  PLUGIN_NAME        = HOST;

    static private final String  PLUGIN_VERSION     = "0";

    static private final String  PLUGIN_ID          = PLUGIN_NAME + "-" + VERSION;

    static private final String  CODER              = "olimex";

    /**
     * ID des Files bei gulli
     */
    private String               fileId;

    private String               cookie;

    private String               finalDownloadURL;

    private HttpURLConnection    finalDownloadConnection;

    public Gulli() {
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
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
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public URLConnection getURLConnection() {
        // XXX: ???
        return null;
    }

    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        RequestInfo requestInfo;
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;

            switch (step.getStep()) {
                case PluginStep.STEP_GET_CAPTCHA_FILE:
                    // con.setRequestProperty("Cookie",
                    // Plugin.joinMap(cookieMap,"=","; "));
                    requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()));

                    fileId = getFirstMatch(requestInfo.getHtmlCode(), PAT_FILE_ID, 1);
                    String captchaLocalUrl = getFirstMatch(requestInfo.getHtmlCode(), PAT_CAPTCHA, 1);
                    if (captchaLocalUrl == null) {
                        logger.severe("Captcha URL konnte nicht gefunden werden "+downloadLink.getUrlDownload());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_STATIC_WAITTIME);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        step.setParameter(3*60);

                       
                        return step;

                    }
                    else {
                        cookie = requestInfo.getCookie();
                        logger.info(cookie);
                        logger.finest("Captcha Page");
                        String captchaUrl = "http://share.gulli.com" + captchaLocalUrl;
                        
                        File file=this.getLocalCaptchaFile(this);
                        if (!JDUtilities.download(file, captchaUrl) || !file.exists()) {
                            logger.severe("Captcha Download fehlgeschlagen: " + captchaUrl);
                            step.setParameter(null);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                            break;
                        }else{
                            step.setParameter(file);
                            step.setStatus(PluginStep.STATUS_USER_INPUT);
                        }
                     
                        return step;
                    }
                case PluginStep.STEP_WAIT_TIME:
                    String captchaTxt = (String) steps.get(0).getParameter();
                    String dlUrl;
                    logger.info("file=" + fileId + "&" + "captcha=" + captchaTxt);
                    requestInfo = postRequest(new URL(DOWNLOAD_URL), cookie, null, null, "file=" + fileId + "&" + "captcha=" + captchaTxt, true);

                    dlUrl = getFirstMatch(requestInfo.getHtmlCode(), PAT_DOWNLOAD_URL, 1);

                    if (dlUrl == null) {
                        logger.finest("Error Page");
                    }
                    logger.info(dlUrl);
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {

                    }
                    requestInfo = postRequestWithoutHtmlCode(new URL(HOST_URL + dlUrl), cookie, null, "action=download&file=" + fileId, false);
                    String red;
                    String waittime = null;
                    String error = null;
                    String url = HOST_URL + dlUrl;
                    // Redirect folgen und dabei die Cookies weitergeben
                    // share.gulli.com/error
                    while ((red = requestInfo.getConnection().getHeaderField("Location")) != null && (waittime = getFirstMatch(red, PAT_DOWNLOAD_LIMIT, 1)) == null && (error = getFirstMatch(red, PAT_DOWNLOAD_ERROR, 1)) == null) {
                        logger.info("red: " + red + " cookie: " + cookie);
                        url = red;
                        requestInfo = getRequestWithoutHtmlCode(new URL(red), cookie, null, false);

                    }
                    logger.info("abbruch bei :" + red);
                    if (waittime != null) {

                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        step.setParameter(Long.parseLong(waittime) * 60 * 1000);

                        logger.info("Warten " + (Long) step.getParameter() + " - " + waittime);
                        logger.info(step.toString());
                    }
                    else if (error != null) {
                        logger.info("Error: " + error);
                        if (error.indexOf("ticket") > 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            try {
                                Thread.sleep(3000);
                            }
                            catch (InterruptedException e) {
                            }
                        }
                        else {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        }

                    }
                    else {
                        logger.info("URL: " + url);
                        finalDownloadURL = url;
                        finalDownloadConnection = requestInfo.getConnection();
                    }
                    return step;
                case PluginStep.STEP_DOWNLOAD:
                    logger.info("dl " + finalDownloadURL);

                    int length = finalDownloadConnection.getContentLength();
                    downloadLink.setDownloadMax(length);
                    download(downloadLink, (URLConnection) finalDownloadConnection);
                    step.setStatus(PluginStep.STATUS_DONE);
                    downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    // String captchaTxt = (String) steps.get(0).getParameter();
                    // logger.info("code for gulli " + captchaTxt);
                    // HttpURLConnection con =
                    // createPostConnection(DOWNLOAD_URL, "file=" + fileId + "&"
                    // + "captcha=" + captchaTxt);
                    //
                    // processPage(con, downloadLink);
                    return step;
            }
            return step;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Liest Content von Connection und gibt diesen als String zurück TODO:
     * auslagern
     * 
     * @param con Connection
     * @return Content
     * @throws IOException
     */
    public static String contentToString(HttpURLConnection con) throws IOException {
        InputStreamReader in = new InputStreamReader(con.getInputStream());
        StringBuffer sb = new StringBuffer();
        int chr;
        while ((chr = in.read()) != -1) {
            sb.append((char) chr);
        }
        return sb.toString();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        fileId = null;
        cookie = null;
        finalDownloadURL = null;
        finalDownloadConnection = null;

    }


}
