//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashMap;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Megauploadcom extends PluginForHost {
    // http://www.megaupload.com/de/?d=0XOSKVY9
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?(megaupload|megarotic|sexuploader)\\.com/.*?\\?d\\=.{8}", Pattern.CASE_INSENSITIVE);

    static private final String     HOST                                = "megaupload.com";

    static private final String     PLUGIN_NAME                         = HOST;

    static private final String     PLUGIN_VERSION                      = "0.1";

    static private final String     PLUGIN_ID                           = PLUGIN_NAME + "-" + VERSION;

    static private final String     CODER                               = "JD-Team";

    static private final String     SIMPLEPATTERN_CAPTCHA_URl           = " <img src=\"/capgen.php?°\">";

    static private final String     SIMPLEPATTERN_FILE_NAME             = "<b>Dateiname:</b>°</div>";

    static private final String     SIMPLEPATTERN_FILE_SIZE             = "<b>Dateigr°e:</b>°</div>";

    static private final String     SIMPLEPATTERN_CAPTCHA_POST_URL      = "<form method=\"POST\" action=\"°\" target";

    static private final String     COOKIE                              = "l=de; v=1; ve_view=1";

    static private final String     SIMPLEPATTERN_GEN_DOWNLOADLINK      = "var ° = String.fromCharCode(Math.abs(°));°var ° = '°' + String.fromCharCode(Math.sqrt(°));";

    static private final String     SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK =   "Math.sqrt(°));°document.getElementById(\"°\").innerHTML = '<a href=\"°' ° '°\"°onclick=\"loadingdownload()";
                                                                            

    static private final String     ERROR_TEMP_NOT_AVAILABLE            = "Zugriff auf die Datei ist vor";

    static private final String     ERROR_FILENOTFOUND                  = "Die Datei konnte leider nicht gefunden werden";

    static private final long       PENDING_WAITTIME                    = 45000;

    private static final String PATTERN_PASSWORD_WRONG = "Wrong password! Please try again";

    // /Simplepattern
  //  private String                  finalURL;

    private String                  captchaURL;

    private HashMap<String, String> fields;

    private String                  captchaPost;

    private boolean tempUnavailable=false;

    private String finalurl;

    public Megauploadcom() {
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

       
        
        
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), "COUNTRY_ID", new String[] { "-","en", "de", "fr", "es", "pt", "nl", "it", "cn", "ct", "jp", "kr", "ru", "fi", "se", "dk", "tr", "sa", "vn" , "pl" }, "LänderID").setDefaultValue("-"));
      
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
        
        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)&&getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)) {

            return doPremiumStep(step, parameter);
        }
        
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link=downloadLink.getDownloadURL().replaceAll("/de", "");
   
        String countryID=getProperties().getStringProperty("COUNTRY_ID", "-");
        if(!countryID.equals("-")){
            logger.info("Use Country trick");
          // http://www.megaupload.com/HIER_STEHT_DER_2_STELLIGE_LÄNDERKÜRZEL/?d=EMXRGYTM
        
                link= link.replace(".com/", ".com/"+countryID+"/");
            
                logger.info("New link: "+link);
            
        }
        
        try {
            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME:
                    logger.info("::" + new URL(link));
                    requestInfo = getRequest(new URL(link), COOKIE, null, true);
                    if (requestInfo.containsHTML(ERROR_TEMP_NOT_AVAILABLE)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        this.tempUnavailable=true;
                        step.setParameter(60 * 30l);
                        return step;
                    }
                    if (requestInfo.containsHTML(ERROR_FILENOTFOUND)) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        return step;
                    }
                   
                    this.captchaURL = "http://" +  new URL(link).getHost() + "/capgen.php?" + getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_URl, 0);
                    this.fields = getInputHiddenFields(requestInfo.getHtmlCode(), "checkverificationform", "passwordhtml");
                    this.captchaPost = getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_POST_URL, 0);
                    step.setParameter(captchaURL);
                    if (captchaURL.endsWith("null") || captchaPost == null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    }
                    return step;
                case PluginStep.STEP_GET_CAPTCHA_FILE:
                    File file = this.getLocalCaptchaFile(this);
                    logger.info("Captcha " + captchaURL);
                    requestInfo = getRequestWithoutHtmlCode(new URL(captchaURL), COOKIE, requestInfo.getLocation(), true);
                    if (!requestInfo.isOK() || !JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {
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
                case PluginStep.STEP_PENDING:
                    requestInfo = postRequest(new URL(captchaPost), COOKIE, null, null, joinMap(fields, "=", "&") + "&imagestring=" + steps.get(1).getParameter(), true);
                    if (getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_CAPTCHA_URl, 0) != null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        return step;
                    }
                    
                    String pwdata = this.getFormInputHidden(requestInfo.getHtmlCode(), "passwordbox", "passwordcountdown");
                   if(pwdata!=null&&pwdata.indexOf("passkey")>0){
                       logger.info("Password protected");
                      String pass= JDUtilities.getController().getUiInterface().showUserInputDialog("Password:");
                      if(pass==null){
                          step.setStatus(PluginStep.STATUS_ERROR);
                          downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                          step.setParameter("wrong Password");
                          return step;
                      }
                      if(countryID.equals("-")){
                      
                      requestInfo = postRequest(new URL("http://"+new URL(link).getHost()+"/de/"), COOKIE, null, null, pwdata + "&pass=" + pass, true);
                      }else{
                          requestInfo = postRequest(new URL("http://"+new URL(link).getHost()+"/"+countryID+"/"), COOKIE, null, null, pwdata + "&pass=" + pass, true);
                             
                      }
                      if(requestInfo.containsHTML(PATTERN_PASSWORD_WRONG)){
                          step.setStatus(PluginStep.STATUS_ERROR);
                          downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                          step.setParameter("wrong Password");
                          return step;
                      }
                     
                   }
                   
                 
                    step.setParameter(PENDING_WAITTIME);
                    return step;
                case PluginStep.STEP_DOWNLOAD:
                    
                    Character l = (char) Math.abs(Integer.parseInt(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 1).trim()));
                    String i = getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 4) + (char) Math.sqrt(Integer.parseInt(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 5).trim()));
                    String url = (JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 3) + i + l + getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 5)));
                    logger.info(".." + url);
                    logger.info(requestInfo.getHtmlCode());
                    try {
                        requestInfo = getRequestWithoutHtmlCode(new URL(url), COOKIE, null, true);
                        if (!requestInfo.isOK()) {
                            logger.warning("Download Limit!");
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                            String wait = requestInfo.getConnection().getHeaderField("Retry-After");
                            logger.finer("Warten: "+wait+" minuten");
                            if (wait == null) {
                                step.setParameter(Long.parseLong(wait.trim()) * 60l * 1000l);
                            }
                            else {
                                step.setParameter(120l * 60l * 1000l);
                            }
                            return step;

                        }
                        int length = requestInfo.getConnection().getContentLength();
                        downloadLink.setDownloadMax(length);
                        logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

                        downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                   
                       dl = new RAFDownload(this, downloadLink,  requestInfo.getConnection());
                      
                        dl.startDownload();
                        return step;
                    }
                    catch (MalformedURLException e) {

                         e.printStackTrace();
                    }
                    catch (IOException e) {

                         e.printStackTrace();
                    }

                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    return step;

            }
            return step;
        }
        catch (IOException e) {
             e.printStackTrace();
            return null;
        }
    }

    private PluginStep doPremiumStep(PluginStep step, DownloadLink parameter) {
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link=downloadLink.getDownloadURL().replaceAll("/de", "");
        String user = getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);
        String countryID=getProperties().getStringProperty("COUNTRY_ID", "-");
        logger.info("PREMOIM");
        if(!countryID.equals("-")){
            logger.info("Use Country trick");
          // http://www.megaupload.com/HIER_STEHT_DER_2_STELLIGE_LÄNDERKÜRZEL/?d=EMXRGYTM
        
                link= link.replace(".com/", ".com/"+countryID+"/");
            
                logger.info("New link: "+link);
            
        }
        
        try {
            downloadLink.setStatusText("Login");
            requestInfo=postRequest(new URL(link), "login="+user+"&password="+pass);
          
            if(requestInfo.getCookie().indexOf("user=")<0){
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM);                
            
            }
            String cookie=requestInfo.getCookie();
            
                requestInfo=getRequest(new URL("http://"+requestInfo.getConnection().getURL().getHost()+"/"+requestInfo.getLocation()), cookie, link, false);
                    
                    
                   // this.postRequest(string, cookie, referrer, requestProperties, parameter, redirect)(, "login="+user+"&password="+pass);  
                Character l = (char) Math.abs(Integer.parseInt(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 1).trim()));
                String i = getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 4) + (char) Math.sqrt(Integer.parseInt(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK, 5).trim()));
                finalurl= (JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 3) + i + l + getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_GEN_DOWNLOADLINK_LINK, 5)));
                
                HTTPConnection urlConnection;
downloadLink.setStatusText("Premium");
                requestInfo = getRequestWithoutHtmlCode(new URL(finalurl), cookie, link, false);
                urlConnection = requestInfo.getConnection();        
                String name = getFileNameFormHeader(urlConnection);
                downloadLink.setName(name);
               dl = new RAFDownload(this, downloadLink, urlConnection);
                dl.setResume(true);dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 3));
                dl.setResume(true);
                dl.startDownload();
                step=nextStep(step);
                step=nextStep(step);
                step=nextStep(step);
               return step;
           
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    // Retry-After

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
      //  this.finalURL = null;
        this.captchaPost = null;
        this.captchaURL = null;
        this.fields = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
      
        return (tempUnavailable?"<Temp. unavailable> ":"")+downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
      
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("/de", ""));
        try {
            requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), "l=de; v=1; ve_view=1", null, true);
            if (requestInfo.containsHTML(ERROR_TEMP_NOT_AVAILABLE)) {
                this.setStatusText("Temp. not available");
                logger.info("Temp. unavailable");
                this.tempUnavailable=true;
                return false;
            }
            if ( requestInfo.containsHTML(ERROR_FILENOTFOUND)) {
                this.setStatusText("File Not Found");
                return false;
            }
            String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_FILE_NAME, 0));
            String fileSize = getSimpleMatch(requestInfo.getHtmlCode(), SIMPLEPATTERN_FILE_SIZE, 1);
            if (fileName == null || fileSize == null) {
                return false;
            }
            downloadLink.setName(fileName.trim());
            if (fileSize.indexOf("KB") > 0) {
                downloadLink.setDownloadMax((int) (Double.parseDouble(fileSize.trim().split(" ")[0].trim()) * 1024));
            }
            if (fileSize.indexOf("MB") > 0) {
                downloadLink.setDownloadMax((int) (Double.parseDouble(fileSize.trim().split(" ")[0].trim()) * 1024 * 1024));
            }
        }
        catch (MalformedURLException e) {
            return false;
        }
        catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }
    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://www.megaupload.com/terms/";
    }
}
