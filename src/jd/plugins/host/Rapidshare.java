package jd.plugins.host;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Rapidshare extends PluginForHost {
    private String                  host                             = "rapidshare.com";

    private String                  version                          = "1.0.0.0";

    // http://(?:[^.]*\.)*rapidshare\.com/files/[0-9]*/[^\s"]+
    private String                  botHash                          = "dab07d2b7f1299f762454cda4c6143e7";

    private Pattern                 patternSupported                 = Pattern.compile("http://(?:rs[0-9]*\\.)*rapidshare\\.com/files/[0-9]+/[^\\s\"]+");

    /**
     * Das findet die Ziel URL für den Post
     */
    private Pattern                 patternForNewHost                = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");

    /**
     * Das findet die Captcha URL <form *name *= *"dl" (?s).*<img *src *=
     * *"([^\n"]*)">
     */
    private Pattern                 patternForCaptcha                = Pattern.compile("<form *name *= *\"dl\" (?s).*<img *src *= *\"([^\\n\"]*)\">");

    /**
     * <form name="dl".* action="([^\n"]*)"(?s).*?<input type="submit"
     * name="actionstring" value="[^\n"]*"
     */
    private Pattern                 patternForFormData               = Pattern.compile("<form name=\"dl\".* action=\"([^\\n\"]*)\"(?s).*?<input type=\"submit\" name=\"actionstring\" value=\"([^\\n\"]*)\"");

    /**
     * Pattern trifft zu wenn die "Ihre Ip läd gerade eine datei " Seite kommt
     */
    private Pattern                 patternForAlreadyDownloading    =Pattern.compile("Your IP-address([^\"]*)is already downloading a file");
    /**
     * Das DownloadLimit wurde erreicht (?s)Downloadlimit.*Oder warte ([0-9]+)
     */
    private Pattern                 patternErrorDownloadLimitReached = Pattern.compile("\\((?:oder warte|or wait) ([0-9]*) (?:minuten|minutes)\\)", Pattern.CASE_INSENSITIVE);

    private Pattern                 patternErrorCaptchaWrong         = Pattern.compile("(zugriffscode falsch|code wrong)", Pattern.CASE_INSENSITIVE);

    private Pattern                 patternErrorFileAbused           = Pattern.compile("(darf nicht verteilt werden|forbidden to be shared)", Pattern.CASE_INSENSITIVE);

    private Pattern                 patternErrorFileNotFound         = Pattern.compile("(datei nicht gefunden|file not found)", Pattern.CASE_INSENSITIVE);

    private int                     waitTime                         = 500;

    private String                  captchaAddress;

    private String                  postTarget;

    private String                  actionString;

    private HashMap<String, String> postParameter                    = new HashMap<String, String>();

    @Override
    public String getCoder() {
        return "astaldo";
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
        return "RAPIDSHARE.COM-1.0.0.";
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public Rapidshare() {
        super();
//        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public URLConnection getURLConnection() {
        return null;
    }

 
    public PluginStep doStep(PluginStep step,DownloadLink downloadLink) {
       
        RequestInfo requestInfo;
        if(step==null){
            logger.info("PLugin Ende erreicht.");
            return null;
        }
        
        logger.info("get Next Step " + currentStep);
        
      
        switch (step.getStep()) {
      
            
            case PluginStep.STEP_WAIT_TIME:
                getDownloadInfo(downloadLink);
           
                break;
            case PluginStep.STEP_GET_CAPTCHA_FILE:
                File file=this.getLocalCaptchaFile(this);
                if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                    logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                    step.setParameter(null);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                    break;
                }else{
                    step.setParameter(file);
                    step.setStatus(PluginStep.STATUS_USER_INPUT);
                }
           
                break;
            case PluginStep.STEP_DOWNLOAD:
                if (steps.get(1).getParameter() == null) {
                    // Bot Erkannt }
                 logger.severe("Fehler. Bot erkennung fehlerhaft");
                }
                else {
                    postParameter.put("mirror", "on");
                    postParameter.put("accesscode", (String) steps.get(1).getParameter());
                    postParameter.put("actionString", actionString);
                    boolean success = prepareDownload(downloadLink);
                    if (success) {
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                        return null;
                    }
                    else if (aborted) {
                        logger.warning("Plugin abgebrochen");
                        downloadLink.setStatus(DownloadLink.STATUS_TODO);
                        step.setStatus(PluginStep.STATUS_TODO);

                    }
                    else {
                        logger.severe("captcha wrong");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        step.setStatus(PluginStep.STATUS_ERROR);
                    }
                }
                break;
        }
        return step;
    }

    private void getDownloadInfo(DownloadLink downloadLink) {
        try {
            // Der Download wird bestätigt
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted())); 
            String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
            if (newURL != null) {

                // Auswahl ob free oder prem
                requestInfo = postRequest(new URL(newURL), "dl.start=free");

                // captcha Adresse finden
                captchaAddress = getFirstMatch(requestInfo.getHtmlCode(), patternForCaptcha, 1);

                // post daten lesen
                postTarget = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 1);
                actionString = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 2);
            }
            logger.info(newURL + " - " + captchaAddress + " - " + postTarget + " - " + actionString);
            currentStep = steps.firstElement();
            if (newURL == null || captchaAddress == null || postTarget == null || actionString == null) {
                logger.info("check pattern "+patternErrorDownloadLimitReached);
                String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), patternErrorDownloadLimitReached, 1);
                if (strWaitTime != null) {
                    logger.severe("wait " + strWaitTime + " minutes");
                    waitTime = Integer.parseInt(strWaitTime) * 60 * 1000;
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    logger.info(" WARTEZEIT SETZEN IN " + currentStep + " : " + waitTime);
                    currentStep.setParameter((long) waitTime);
                    return;
                }
                String strFileAbused = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileAbused, 0);
                if (strFileAbused != null) {
                    logger.severe("file abused");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
                String strFileNotFound = getFirstMatch(requestInfo.getHtmlCode(), patternErrorFileNotFound, 0);
                if (strFileNotFound != null) {
                    logger.severe("file not found");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }

                String strCaptchaWrong = getFirstMatch(requestInfo.getHtmlCode(), patternErrorCaptchaWrong, 0);
                if (strCaptchaWrong != null) {
                    logger.severe("captchaWrong");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
                String strAlreadyLoading = getFirstMatch(requestInfo.getHtmlCode(), patternForAlreadyDownloading, 0);
                
                if (strAlreadyLoading != null) {
                    logger.severe("Already Loading wait " + 60 + " sek. to Retry");
                    waitTime =180 * 1000;
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_STATIC_WAITTIME);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    logger.info(" WARTEZEIT SETZEN IN (already loading)" + currentStep + " : " + waitTime);
                    currentStep.setParameter((long) waitTime);
                    return;
                }
                
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                currentStep.setStatus(PluginStep.STATUS_ERROR);
                logger.warning("could not get downloadInfo ");
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e) {
                  }
                return;
            }
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    private boolean prepareDownload(DownloadLink downloadLink) {

        try {
            URLConnection urlConnection = new URL(postTarget).openConnection();
            urlConnection.setDoOutput(true);

            // Post Parameter vorbereiten
            String postParams = createPostParameterFromHashMap(postParameter);
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(postParams);
            wr.flush();

            int length = urlConnection.getContentLength();
            downloadLink.setDownloadMax(length);
            return download(downloadLink, urlConnection);
        }
        catch (IOException e) {
            logger.severe("URL could not be opened. " + e.toString());
        }
        return false;
    }

    @Override
    public boolean doBotCheck(File file) {
        try {
            return this.md5sum(file).equals(botHash);
        }
        catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
        }
        catch (FileNotFoundException e) {

            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void reset() {
        waitTime = 500;
        captchaAddress = null;
        postTarget = null;
        actionString = null;
        postParameter = new HashMap<String, String>();

    }

 
}
