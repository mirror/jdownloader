package jd.plugins.host;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
    // http://netload.in/datei0eabdd9b6897b96bd2970a9b54afc284.htm
    //  http://netload.in/mindestens20zeichen
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?netload\\.in/.{20}.*", Pattern.CASE_INSENSITIVE);
    static private final String  HOST             = "netload.in";
    static private final String  PLUGIN_NAME      = HOST;
    static private final String  PLUGIN_VERSION   = "1.0.0";
    static private final String  PLUGIN_ID        = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String  CODER            = "JD-Team";
    // /Simplepattern
    static private final String  DOWNLOAD_URL     = "<div class=\"Free_dl\"><a href=\"°\">";
    // <img src="share/includes/captcha.php?t=1189894445" alt="Sicherheitsbild"
    // />
    static private final String  CAPTCHA_URL      = "<img src=\"share/includes/captcha.php?t=°\" alt=\"Sicherheitsbild\" />";
    // <form method="post" action="index.php?id=10">
    static private final String  POST_URL         = "<form method=\"post\" action=\"°\">";
    static private final String  LIMIT_REACHED    = "share/images/download_limit_go_on.gif";
    static private final String  CAPTCHA_WRONG    = "Sicherheitsnummer nicht eingegeben";
    static private final String  NEW_HOST_URL     = "<a class=\"Orange_Link\" href=\"°\" >Alternativ klicke hier.</a>";
    static private final String  FILE_NOT_FOUND   = "Datei nicht vorhanden";
    static private final String  FILE_DAMAGED	  = "Diese Datei liegt auf einem Server mit einem technischen Defekt. Wir konnten diese Datei leider nicht wieder herstellen.";
    static private final String  DOWNLOAD_LIMIT   = "download_limit.tpl";
    static private final String  DOWNLOAD_CAPTCHA = "download_captcha.tpl";
    static private final String  DOWNLOAD_START   = "download_load.tpl";
    //static private final String  DOWNLOAD_WAIT    = "download_wait.tpl";
    static private final Pattern DOWNLOAD_WAIT_TIME = Pattern.compile("countdown\\(([0-9]*),'change", Pattern.CASE_INSENSITIVE);
    
    private String               finalURL;

    private String               captchaURL;
    private String               fileID;
    private String               postURL;
    private String               sessionID;
    private String 				 userCookie;
    private Long     			 waitTime;
    
    public Netloadin() {
        setConfigElements();
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
    public PluginStep doStep(PluginStep step, DownloadLink parameter) throws MalformedURLException, IOException {
        DownloadLink downloadLink = (DownloadLink) parameter;
     //   RequestInfo requestInfo;
        if (step == null) {
            logger.info("Plugin Ende erreicht.");
            return null;
        }
        logger.info("get Next Step " + step);
        // premium
        if (this.getProperties().getProperty("USE_PREMIUM") != null && this.getProperties().getBooleanProperty("USE_PREMIUM", false)) {
            return this.doPremiumStep(step, downloadLink);
        }
        else {
            return this.doFreeStep(step, downloadLink);
        }
        
        
    }
    @SuppressWarnings("static-access")
	private PluginStep doFreeStep(PluginStep step, DownloadLink downloadLink) {

        try {
            
            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME:
                    if (captchaURL == null) {
                        logger.info(downloadLink.getDownloadURL());
                        requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
                        this.sessionID = requestInfo.getCookie();
                        System.out.println(requestInfo.getHtmlCode());
                        String url = "http://" + HOST + "/" + getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_URL, 0);
                        url = url.replaceAll("\\&amp\\;", "&");
                        if(requestInfo.containsHTML(FILE_DAMAGED)) {
                        	logger.warning("ERROR: File on a damaged server");
                        	step.setStatus(PluginStep.STATUS_ERROR);
                        	downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        	return step;
                        }
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
                            waitTime = Long.parseLong(getFirstMatch(requestInfo.getHtmlCode(), DOWNLOAD_WAIT_TIME, 1));
                            waitTime = waitTime*10L;
                            step.setParameter(waitTime);
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
                    if (!hasEnoughHDSpace(downloadLink)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
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
        } catch (FileNotFoundException e) {
             e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
            return step;
        
        }
        
        catch (Exception e) {
             e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            
            return step;
        }
    }
    private PluginStep doPremiumStep(PluginStep step, DownloadLink downloadLink) throws MalformedURLException, IOException  {
        String user = (String) this.getProperties().getProperty("PREMIUM_USER");
        String pass = (String) this.getProperties().getProperty("PREMIUM_PASS");
        switch (step.getStep()) {
            case PluginStep.STEP_WAIT_TIME:
                //Login
                if(finalURL==null){
                
                 //SessionID holen
                    requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
                    this.sessionID = requestInfo.getCookie();
                    logger.finer("sessionID: "+sessionID);
                    if (requestInfo.getHtmlCode().indexOf(FILE_NOT_FOUND) > 0) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                   //Login Cookie abholen
                    requestInfo= postRequest(new URL("http://" + HOST + "/index.php"),sessionID,downloadLink.getDownloadURL(),null,"txtuser="+user+"&txtpass="+pass+"&txtcheck=login&txtlogin=", false);
                    this.userCookie= requestInfo.getCookie();
                    logger.finer("Usercookie: "+userCookie+" ->"+requestInfo.getLocation());
                    
                    //Vorbereitungsseite laden
                    requestInfo=getRequest(new URL("http://" + HOST + "/"+requestInfo.getLocation()), sessionID+" "+userCookie, null, false);
                    this.finalURL = getSimpleMatch(requestInfo.getHtmlCode(), NEW_HOST_URL, 0);
                    if(finalURL==null){ 
                        logger.severe("Error: could not get final URL");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                 
                    
                  return step;
                }else{
                    step.setStatus(PluginStep.STATUS_SKIP);
                    downloadLink.setStatusText("Premiumdownload");
                    return step;
                }
            case PluginStep.STEP_PENDING:
                step.setParameter(100l);
             
                return step;
            case PluginStep.STEP_GET_CAPTCHA_FILE:
                step.setStatus(PluginStep.STATUS_SKIP);
                downloadLink.setStatusText("Premiumdownload");
                return step;
            case PluginStep.STEP_DOWNLOAD:
                logger.info("dl " + finalURL);
                requestInfo = getRequestWithoutHtmlCode(new URL(finalURL), sessionID, null, false);
                int length = requestInfo.getConnection().getContentLength();
                downloadLink.setDownloadMax(length);
                logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));
                downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
            
                if (!hasEnoughHDSpace(downloadLink)) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
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
            requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, false);
            
            String name = downloadLink.getName();
            if (name.toLowerCase().matches(".*\\..{1,5}\\.htm$")) name = name.replaceFirst("\\.htm$", "");
            downloadLink.setName(name);
            if (requestInfo.getHtmlCode().indexOf(FILE_NOT_FOUND) > 0) {
                this.setStatusText("File Not Found");
                return false;
            }
            if (requestInfo.getHtmlCode().indexOf(FILE_DAMAGED) > 0) {
                this.setStatusText("File on a damaged server");
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
        if (this.getProperties().getBooleanProperty("USE_PREMIUM", false)) {
            return 20;
        }else{
        return 1;
        }
    }
    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Premium Account"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, "Premium User"));
        cfg.setDefaultValue("Kundennummer");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, "Premium Pass"));
        cfg.setDefaultValue("Passwort");
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, "Premium Account verwenden"));
        cfg.setDefaultValue(false);
 
    }
    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://netload.in/index.php?id=13";
    }
}