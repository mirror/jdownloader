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
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://filefactory.com/file/b1bf90/
// http://filefactory.com/f/ef45b5179409a229/ 

public class FileFactory extends PluginForHost {
	
    static private final String host = "filefactory.com";
    private String version = "1.5.6";
    static private final Pattern patternSupported = Pattern.compile("http://.*?filefactory\\.com(/|//)file/.{6}/?", Pattern.CASE_INSENSITIVE);
    
    private static Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");
    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/captcha2/captcha.php\\?[^\"]*)\" alt=");
    private static Pattern baseLink = Pattern.compile("<a href=\"(.*?)\" id=\"basicLink\"", Pattern.CASE_INSENSITIVE);
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");
    
    private static final String NOT_AVAILABLE = "this file is no longer available";
    private static final String SERVER_DOWN = "server hosting the file you are requesting is currently down";
    private static final String NO_SLOT = "no free download slots";
    private static final String FILENAME = "<tr valign='top' style='color:green;'><td>(.*?)</td>";
    private static final String FILESIZE = "<td style=\"text-align:right;\">(.*?) (B|KB|MB)</td>";
    private static final String PREMIUM_LINK = "<p style=\"margin:30px 0 20px\"><a href=\"(http://[a-z0-9]+\\.filefactory\\.com/dlp/[a-z0-9]+/)\"";
    private static final String WAIT_TIME = "wait ([0-9]+) (minutes|seconds)";
    private static final String DOWNLOAD_LIMIT = "exceeded the download limit";
    private static final String CAPTCHA_WRONG = "verification code you entered was incorrect";
    private static final String PATTERN_DOWNLOADING_TOO_MANY_FILES = "downloading too many files";
    
    private String captchaAddress;
    private String postTarget;
    private String actionString;
    private RequestInfo requestInfo;
    private int wait;
    private File captchaFile;
   

    @Override
    public String getCoder() {
        return "JD-Team";
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
    public String getPluginID() {
        return host + " - " + version;
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
        
        logger.info("Steps: "+steps);
        setConfigElements();
        
    }

    @Override
    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        if (step == null) {
            return null;
        }
        
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
    	downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));

        if ( getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false) ) {
        	
            if ( step.getStep() == 1 ) {
            	logger.info("Premium");
            }
            
        
				return this.doPremiumStep(step, downloadLink);			
			
        } else {
            
            if ( step.getStep() == 1 ) {
            	logger.info("Free");            }
        	
           
				return this.doFreeStep(step, downloadLink);
			
			
        }
        
	
		
    }
    
    public PluginStep doFreeStep(PluginStep step, DownloadLink parameter) {
        
    	DownloadLink downloadLink = null;
        
    	try {
    		
            downloadLink = (DownloadLink) parameter;
            logger.info(downloadLink.getDownloadURL());
            switch (step.getStep()) {
            	
            
                case PluginStep.STEP_WAIT_TIME :
                	
                    requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
                    
                    if ( requestInfo.containsHTML(NOT_AVAILABLE) ) {
                    	
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                        return step;
                        
                    } else if ( requestInfo.containsHTML(SERVER_DOWN) ) {
                        
                      	step.setStatus(PluginStep.STATUS_ERROR);
                       	downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    	return step;
                        
                    } else if ( requestInfo.containsHTML(NO_SLOT) ) {
                    	
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        return step;
                        
                    }
                    
                    String newURL = "http://"+requestInfo.getConnection().getURL().getHost()+getFirstMatch(requestInfo.getHtmlCode(), baseLink, 1);
                    logger.info(newURL);
                    
                    if ( newURL != null ) {
                    	
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
                    step.setStatus(PluginStep.STATUS_DONE);
                    return step;
                    
              
                case PluginStep.STEP_GET_CAPTCHA_FILE :
                    captchaFile = this.getLocalCaptchaFile(this);

                    if (!JDUtilities.download(captchaFile, captchaAddress) || !captchaFile.exists()) {
                        
                        logger.severe("Captcha Download failed: " + captchaAddress);
                       
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                        
                    } 
                    step.setParameter(captchaFile);
                    //wird in diesem step null zurückgegeben findet keine captchaerkennung statt. der captcha wird im nächsten schritt erkannt
                    return step;
                case PluginStep.STEP_DOWNLOAD :
                  
                    String captchaCode=(String) steps.get(1).getParameter();
                    try {
                    	logger.info(postTarget + "&captcha=" + captchaCode);
                        requestInfo = postRequest((new URL(actionString)), requestInfo.getCookie(), actionString, null, postTarget + "&captcha=" + captchaCode, true);
                        
                        if ( requestInfo.getHtmlCode().contains(CAPTCHA_WRONG) ) {
                        	
                        	step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                           
                            return step;
                        	
                        }
                        
                        postTarget = getFirstMatch(requestInfo.getHtmlCode(), patternForDownloadlink, 1);
                        postTarget = postTarget.replaceAll("&amp;", "&");
                        
                    } catch (Exception e) {
                    	
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        e.printStackTrace();
                        
                    }
                    
                    try {

                    	requestInfo = postRequestWithoutHtmlCode((new URL(postTarget)), requestInfo.getCookie(), actionString, "", false);
                    	HTTPConnection urlConnection = requestInfo.getConnection();
                        
                    	// downloadlimit reached
                    	if ( urlConnection.getHeaderField("Location") != null ) {
                    		
                    		//filefactory.com/info/premium.php/w/
                    		requestInfo = getRequest(new URL(urlConnection.getHeaderField("Location")), null, null, true);
                    		
                    		if ( requestInfo.getHtmlCode().contains(DOWNLOAD_LIMIT) ) {

                    			logger.severe("Download limit reached as free user");
                    			
                    			String waitTime = new Regexp(requestInfo.getHtmlCode(), WAIT_TIME).getFirstMatch(1);
                    			String unit = new Regexp(requestInfo.getHtmlCode(), WAIT_TIME).getFirstMatch(2);
                    			wait = 0;
                    			
                    			if ( unit.equals("minutes") ) {
                    				wait = Integer.parseInt(waitTime);
                    				logger.info("wait" + " " + String.valueOf(wait+1)
                    						+ " minutes" );
                    				wait = wait * 60000 + 60000;
                    			} else if ( unit.equals("seconds") ) {
                    				wait = Integer.parseInt(waitTime);
                    				logger.info("wait" + " " + String.valueOf(wait+5)
                    						+ " seconds" );
                    				wait = wait * 1000 + 5000;
                    			}
                    			
                    			 downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                                 step.setStatus(PluginStep.STATUS_ERROR);
                                 logger.info("Traffic Limit reached....");
                                 step.setParameter((long) wait);
                                 return step;
                    		              
                    			
                    		} else {
                    			
                    			requestInfo = postRequestWithoutHtmlCode((new URL(postTarget)), requestInfo.getCookie(), actionString, "", false);
                            	urlConnection = requestInfo.getConnection();
                    			
                    		}
                    		
                    	}
                    	
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        downloadLink.setName(this.getFileNameFormHeader(urlConnection));
                        
                
                       if(requestInfo.getConnection().getHeaderField("Location")!=null){
                           requestInfo=getRequest(new URL(requestInfo.getConnection().getHeaderField("Location")));
                         
                           if(requestInfo.containsHTML(PATTERN_DOWNLOADING_TOO_MANY_FILES)){
                           
                               logger.info("You are downloading too many files at the same time. Wait 10 seconds(or reconnect) an retry afterwards");
                               
                               downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                               step.setStatus(PluginStep.STATUS_ERROR);
                         
                               step.setParameter(10*60000l);
                              
                               return step;
                           }
                           logger.info(requestInfo.getHtmlCode());
                           downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                           step.setStatus(PluginStep.STATUS_ERROR);
                           return step;
                       }
                        Download dl = new Download(this, downloadLink, requestInfo.getConnection());

                        if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
      
                        	
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                            
                        }
                        
                    } catch (IOException e) {
                    	
                        logger.severe("URL could not be opened" + e.toString());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
            }
            
        } catch (Exception e) {
        	
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            e.printStackTrace();
            return step;
            
        }
        
        return step;
        
    }
    
    // by eXecuTe
    public PluginStep doPremiumStep(PluginStep step, DownloadLink parameter) {
        
    	DownloadLink downloadLink = null;
    	String user = getProperties().getStringProperty(PROPERTY_PREMIUM_USER);
        String pass = getProperties().getStringProperty(PROPERTY_PREMIUM_PASS);

        if ( user == null || pass == null ) {

            step.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("Please enter premium data");
            parameter.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            step.setParameter(JDLocale.L("plugins.host.premium.loginError", "Loginfehler"));
            getProperties().setProperty(PROPERTY_USE_PREMIUM, false);
            return step;

        }
        
    	try {
    		
            downloadLink = (DownloadLink) parameter;
            
            switch (step.getStep()) {
            
                case PluginStep.STEP_WAIT_TIME :
                	
                    requestInfo = postRequest(new URL(downloadLink.getDownloadURL()), null, null, null,
                    		"email="+JDUtilities.urlEncode(user)+"&password="+JDUtilities.urlEncode(pass), true);
                    
                    String premCookie = requestInfo.getCookie();
                  	
                  	if ( requestInfo.containsHTML(NOT_AVAILABLE) ) {
                        
                      	step.setStatus(PluginStep.STATUS_ERROR);
                       	downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                    	return step;
                        
                    } else if ( requestInfo.containsHTML(SERVER_DOWN) ) {
                        
                      	step.setStatus(PluginStep.STATUS_ERROR);
                       	downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    	return step;
                        
                    } else {
                        
                        String link = new Regexp(requestInfo.getHtmlCode(), PREMIUM_LINK).getFirstMatch();
                        requestInfo = postRequestWithoutHtmlCode(new URL(link), premCookie, null, "", true);
                        
                    }
                    
                    step.setStatus(PluginStep.STATUS_DONE);
                    return step;
                    
                case PluginStep.STEP_GET_CAPTCHA_FILE :
                	
                	step.setStatus(PluginStep.STATUS_SKIP);
                    step = nextStep(step);
                    return step;
                    
                case PluginStep.STEP_DOWNLOAD :
                	
                    try {
                    	
                        HTTPConnection urlConnection = requestInfo.getConnection();
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        downloadLink.setName(this.getFileNameFormHeader(urlConnection));

                        if ( !hasEnoughHDSpace(downloadLink) ) {
                        	
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                            
                        }
                        
                        Download dl = new Download(this, downloadLink, urlConnection);
                        dl.setChunks(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS,3));
                        if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR&& step.getStatus() != PluginStep.STATUS_TODO) {
                        	
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                            
                        }
                        
                    } catch (Exception e) {
                    	
                    	logger.severe(JDLocale.L("URL could not be opened") + e.toString());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        e.printStackTrace();
                        return step;
                        
                    }
                    
            }
            
        } catch (Exception e) {
        	
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            e.printStackTrace();
            return step;
            
        }
        
        return step;
        
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
        wait = 0;
  
        
        steps.removeAllElements();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }
    
    // codierung ist nicht standardkonform
    // http%3A%2F%2Fwww.filefactory.com%2Ffile%2Fd0b032%2F
    private static String fileFactoryUrlEncode(String str) {
    	
        String allowed = "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.\\&=;";
        String ret = "";
        int i;
        
        for (i = 0; i < str.length(); i++) {
        	
            char letter = str.charAt(i);
            
            if (allowed.indexOf(letter) >= 0) {
                ret += letter;
            } else {
                ret += "%" + Integer.toString(letter, 16).toUpperCase();
            }
            
        }
        
        return ret;

    }
    
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        
    	downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(".com//", ".com/"));
    	downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("http://filefactory", "http://www.filefactory"));
    	
    	try {
        	
            requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
            
            if ( requestInfo.containsHTML(NOT_AVAILABLE) ) {
                return false;
            } else if ( requestInfo.containsHTML(SERVER_DOWN) ) {
                return false;
            } else {
            	
            	String fileName = JDUtilities.htmlDecode(new Regexp(
            			requestInfo.getHtmlCode().replaceAll("\\&\\#8203\\;", ""), FILENAME).getFirstMatch());
            	int length = 0;
            	
                // Dateiname ist auf der Seite nur gekürzt auslesbar -> linkchecker
                // http://www.filefactory.com/file/d0b032/
					
				requestInfo = postRequest(new URL("http://www.filefactory.com/tools/link_checker.php"), null,
						null, null, "link_text="+fileFactoryUrlEncode(downloadLink.getDownloadURL()), true);
				fileName = new Regexp(requestInfo.getHtmlCode(), FILENAME).getFirstMatch();
				if(fileName==null)return false;
				fileName = fileName.replaceAll(" <br/>", "").trim();
				
				Double fileSize = Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), FILESIZE).getFirstMatch(1));
				String unit = new Regexp(requestInfo.getHtmlCode(), FILESIZE).getFirstMatch(2);
				
				if ( unit.equals("B") )  length = (int) Math.round(fileSize);
				if ( unit.equals("KB") ) length = (int) Math.round(fileSize*1024);
				if ( unit.equals("MB") ) length = (int) Math.round(fileSize*1024*1024);
				
				downloadLink.setName(fileName);
				downloadLink.setDownloadMax(length);           
                
            }
            
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        
        return true;
    }
    
    private void setConfigElements() {
    	
    	ConfigEntry cfg;
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDLocale.L("plugins.host.premium.account", "Premium Account")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PROPERTY_PREMIUM_USER, JDLocale.L("plugins.host.premium.user", "Benutzer")));
        cfg.setDefaultValue(JDLocale.L("plugins.host.premium.email", "Email"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, getProperties(), PROPERTY_PREMIUM_PASS, JDLocale.L("plugins.host.premium.password", "Passwort")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_USE_PREMIUM, JDLocale.L("plugins.host.premium.useAccount", "Premium Account verwenden")));
        cfg.setDefaultValue(false);
        
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public void resetPluginGlobals() {
    	
    	captchaAddress = null;
    	postTarget = null;
    	actionString = null;
    	requestInfo = null;
        wait = 0;

        
   

    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }
    
}
