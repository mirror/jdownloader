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

public class DepositFiles extends PluginForHost {
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?depositfiles\\.com/[\\w\\/]*?files/[\\d]+", Pattern.CASE_INSENSITIVE);
    static private final String HOST = "depositfiles.com";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.1";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "DwD";
    // /Simplepattern
    //      
    //private Pattern DOWNLOAD_URL = Pattern.compile("<form action=\"(.*?)\" method=\"POST\" id=\"gateway_form\" >", Pattern.CASE_INSENSITIVE);
    private Pattern HIDDENPARAM = Pattern.compile("<input type=\"hidden\" name=\"gateway_result\" value=\"([\\d]+)\">", Pattern.CASE_INSENSITIVE);
    private Pattern FILE_INFO_NAME = Pattern.compile("(?s)Dateiname: <b title=\"(.*?)\">.*?</b>", Pattern.CASE_INSENSITIVE);
    private Pattern FILE_INFO_SIZE = Pattern.compile("(?s)Dateigr&ouml;&szlig;e: <b>([\\d|\\.]+)&nbsp;MB</b>", Pattern.CASE_INSENSITIVE);
    private Pattern CAPTCHA_FLE = Pattern.compile(" <img height=\"35px\" width=\"100px\" src='(http://.*?.depositfiles.com/img/codes/.*?)'>");
    // ich hoffe das wird nicht verbessert weil das bestimmt nicht so schnell
    // auffaellt
    static private final String FILE_NOT_FOUND = "Dieser File existiert nicht";
    private static final String DOWNLOAD_NOTALLOWED = "Entschuldigung aber im Moment koennen Sie nur diesen Downloadmodus anwenden";

    private String captchaAddress;
    private String finalURL;
    private String cookie;

    public DepositFiles() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
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
        RequestInfo requestInfo;
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;
            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME :
                    finalURL=downloadLink.getUrlDownloadDecrypted().replace("depositfiles.com.*?files", "depositfiles.com/de/files");
                    logger.info(finalURL);
                    requestInfo = getRequest(new URL(finalURL));
                    // Datei geloescht?
                    if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    if (requestInfo.containsHTML(DOWNLOAD_NOTALLOWED)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.severe("Download momentan nicht moeglich");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TO_MANY_USERS);
                        step.setParameter(60000l);
                        return step;
                    }
                    
                    requestInfo=postRequest(new URL(finalURL), requestInfo.getCookie(), finalURL, null, "x=15&y=7&gateway_result=" + getFirstMatch(requestInfo.getHtmlCode(),HIDDENPARAM, 1), true);

                    if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                        logger.severe("Unbekannter fehler.. retry in 20 sekunden");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;
                    } else {
                        this.captchaAddress = getFirstMatch(requestInfo.getHtmlCode(), CAPTCHA_FLE, 1);
                        this.cookie = requestInfo.getCookie();
                        // this.postParameter=getInputHiddenFields(requestInfo.getHtmlCode().replace("<input
                        // type=\"hidden\" name=\"gateway_result\"
                        // value=\"1\">", ""));
                        return step;
                    }

                case PluginStep.STEP_GET_CAPTCHA_FILE :
                    File file = this.getLocalCaptchaFile(this);
                    if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        System.out.println("asdf");
                        return step;
                    } else {
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                    }
                    break;
                case PluginStep.STEP_PENDING :
                    step.setParameter(60000l);
                    break;
                case PluginStep.STEP_DOWNLOAD :
                    logger.info("dl " + finalURL);
                    String code = (String) steps.get(2).getParameter();
                    requestInfo = postRequestWithoutHtmlCode(new URL(finalURL), cookie, finalURL, "img_code="+code+"&file_password&gateway_result=1&go=1", true);
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
                    download(downloadLink, (URLConnection) requestInfo.getConnection());
                    step.setStatus(PluginStep.STATUS_DONE);
                    downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    return step;
            }
            return step;
        } catch (IOException e) {
             e.printStackTrace();
            return null;
        }
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
        try {
            requestInfo = getRequestWithoutHtmlCode(new URL(downloadLink.getUrlDownloadDecrypted()), null, null, false);
            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                return false;
            } else {
                if (requestInfo.getConnection().getHeaderField("Location") != null) {
                    requestInfo = getRequest(new URL("http://" + HOST + requestInfo.getConnection().getHeaderField("Location")), null, null, true);
                } else {
                    requestInfo = readFromURL(requestInfo.getConnection());
                }

                // Datei geloescht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                    return false;
                }

                String fileName = JDUtilities.htmlDecode(getFirstMatch(requestInfo.getHtmlCode(), FILE_INFO_NAME, 1));
                String fileSize = JDUtilities.htmlDecode(getFirstMatch(requestInfo.getHtmlCode(), FILE_INFO_SIZE, 1));
                downloadLink.setName(fileName);
                if (fileSize != null) {
                    try {
                        int length = (int) (Double.parseDouble(fileSize) * 1024 * 1024);
                        downloadLink.setDownloadMax(length);
                    } catch (Exception e) {
                    }
                }
            }
            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return false;
    }
    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }
    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }
}
