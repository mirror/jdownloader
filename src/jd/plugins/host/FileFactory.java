package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class FileFactory extends PluginForHost {

    static private final String host                     = "filefactory.com";

    private String              version                  = "1.1.0.0";

    private Pattern             patternSupported         = Pattern.compile("http://(?:www\\.)*filefactory.com/file/[^\\s\"]+/?");

    /**
     * Das findet die Captcha URL
     * 
     */
    private Pattern             frameForCaptcha          = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");

    private Pattern             patternForCaptcha        = Pattern.compile("src=\"(/captcha2/captcha.php\\?[^\"]*)\" alt=");

    /**
     * 
     * <a target="_top"
     * href="http://archive01.filefactory.com/dl/f/cd66d1//b/3/h/4bb297a8a6f12168/"><img
     * src
     */
    private Pattern             patternForDownloadlink   = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");

    // TODO CaptchaWrong
    private Pattern             patternErrorCaptchaWrong = Pattern.compile("(Sorry, the verification code you entered was incorrect)", Pattern.CASE_INSENSITIVE);

    private String              captchaAddress;

    private String              postTarget;

    private String              actionString;

    private RequestInfo         requestInfo;

    @Override
    public String getCoder() {
        return "DwD/Coalado";
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
        return "FILEFACTORY.COM-1.0.0.";
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public FileFactory() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

//    @Override
//    public URLConnection getURLConnection() {
//        return null;
//    }

    @Override
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {

        DownloadLink downloadLink = null;
        try {
            logger.info("Step: " + step);
            downloadLink = (DownloadLink) parameter;

            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME:
                    requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()),null,null,true);
                    String script = getBetween(requestInfo.getHtmlCode(), "var link", "document");
                    if (requestInfo.getHtmlCode().indexOf("this file is no longer available") >= 0) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                        return step;
                    }
                    Vector<Vector<String>> matches = getAllSimpleMatches(script, "'°'");
                    String url = null;
                    // URL wird hier aus js zusamengebastelt. Wäre ich ff, dann
                    // würde ich die linkzerstückelung gelegentlich ändern. Also
                    // mach ich das ma lieber gleich für beliebige stücke
                    for (int i = 0; i < matches.size(); i++) {
                        if(url==null)url="http://" + host;
                        url += matches.elementAt(i).elementAt(0);
                    }

                    String newURL = url;
logger.info(url);
                    if (newURL != null) {
                        newURL = newURL.replaceAll("' \\+ '", "");
                        requestInfo = getRequest((new URL(newURL)), null, downloadLink.getName(), true);

                        actionString = "http://www.filefactory.com/" + getFirstMatch(requestInfo.getHtmlCode(), frameForCaptcha, 1);

                        actionString = actionString.replaceAll("&amp;", "&");
                        requestInfo = getRequest((new URL(actionString)), "viewad11=yes", newURL, true);
                        // captcha Adresse finden

                        captchaAddress = "http://www.filefactory.com" + getFirstMatch(requestInfo.getHtmlCode(), patternForCaptcha, 1);
                        captchaAddress = captchaAddress.replaceAll("&amp;", "&");
                        // post daten lesen
                        postTarget = getFormInputHidden(requestInfo.getHtmlCode());

                    }
                    logger.info(captchaAddress + " : " + postTarget);
                    if (captchaAddress == null || postTarget == null) {

                       
                    }
                    step.setStatus(PluginStep.STATUS_DONE);
                    return step;

                case PluginStep.STEP_GET_CAPTCHA_FILE:

                    File file = this.getLocalCaptchaFile(this);
                    if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                    }
                    else {
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                    }

                    break;
                case PluginStep.STEP_DOWNLOAD:
                    try {
                        requestInfo = postRequest((new URL(actionString)), requestInfo.getCookie(), actionString, null, postTarget + "&captcha=" + (String) steps.get(1).getParameter(), true);
                        postTarget = getFirstMatch(requestInfo.getHtmlCode(), patternForDownloadlink, 1);
                        postTarget = postTarget.replaceAll("&amp;", "&");

                    }
                    catch (Exception e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        e.printStackTrace();
                    }

                    boolean success = prepareDownload(downloadLink);
                    if (success) {
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                        return null;
                    }
                    else {
                        logger.severe("captcha wrong");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        step.setStatus(PluginStep.STATUS_ERROR);
                    }

                    break;

            }
        }
        catch (Exception e) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            e.printStackTrace();
            return step;
        }
        return step;

    }

    private boolean prepareDownload(DownloadLink downloadLink) {
        try {
            URLConnection urlConnection = new URL(postTarget).openConnection();

            int length = urlConnection.getContentLength();
            downloadLink.setDownloadMax(length);
            downloadLink.setName(this.getFileNameFormHeader(urlConnection));
            return download(downloadLink, urlConnection);
        }
        catch (IOException e) {
            logger.severe("URL could not be opened. " + e.toString());
        }
        return false;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        captchaAddress = null;
        postTarget = null;
        actionString = null;
        requestInfo = null;

    }

    @Override
    public boolean checkAvailability(DownloadLink downloadLink) {
        // TODO Auto-generated method stub
        try {
            requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()),null,null,true);

            if (requestInfo.getHtmlCode().indexOf("this file is no longer available") >= 0) {
                return false;
            }
        }
        catch (MalformedURLException e) {
            return false;
        }
        catch (IOException e) {

            return false;
        }
        //
        return true;
    }

}
