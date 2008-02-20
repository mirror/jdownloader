package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
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
    private String version = "1.4.0";
    static private final Pattern patternSupported = Pattern.compile("http://.*?filefactory\\.com/file/.{6}/?", Pattern.CASE_INSENSITIVE);
    
    private static Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");
    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/captcha2/captcha.php\\?[^\"]*)\" alt=");
    private static Pattern baseLink = Pattern.compile("<a href=\"(.*?)\" id=\"basicLink\"", Pattern.CASE_INSENSITIVE);
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");
    
    private static final String NOT_AVAILABLE = "this file is no longer available";
    private static final String NO_SLOT = "no free download slots";
    private static final String FILENAME = "<tr valign='top' style='color:green;'><td>(.*?)</td>";
    private static final String FILESIZE = "<td style=\"text-align:right;\">(.*?) (B|KB|MB)</td>";
    private static final String PREMIUM_LINK = "<p style=\"margin:30px 0 20px\"><a href=\"(http://[a-z0-9]+\\.filefactory\\.com/dlp/[a-z0-9]+/)\"";
    
    private String captchaAddress;
    private String postTarget;
    private String actionString;
    private RequestInfo requestInfo;

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
        setConfigElements();
        
    }

    @Override
    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {

        if (step == null) {
            return null;
        }

        if ( getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false) ) {
        	
            if ( step.getStep() == 1 ) {
            	logger.info(JDLocale.L("plugins.host.general.premium", "Premium"));
            }
            
        	try {
				return this.doPremiumStep(step, downloadLink);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
        } else {
            
            if ( step.getStep() == 1 ) {
            	logger.info(JDLocale.L("plugins.host.general.free", "Free"));
            }
        	
            try {
				return this.doFreeStep(step, downloadLink);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
        }
        
		return null;
		
    }
    
    public PluginStep doFreeStep(PluginStep step, DownloadLink parameter) {
        
    	DownloadLink downloadLink = null;
        
    	try {
    		
            downloadLink = (DownloadLink) parameter;
            
            switch (step.getStep()) {
            
                case PluginStep.STEP_WAIT_TIME :
                	
                    requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
                    
                    if ( requestInfo.getHtmlCode().indexOf(NOT_AVAILABLE) >= 0 ) {
                    	
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                        return step;
                        
                    }
                    
                    if ( requestInfo.containsHTML(NO_SLOT) ) {
                    	
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
                	
                    File file = this.getLocalCaptchaFile(this);

                    if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                    	
                        logger.severe(JDLocale.L("plugins.hoster.general.captchaDownloadError", "Captcha Download fehlgeschlagen") + ": " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                        
                    } else {
                    	
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                        
                    }
                    
                    break;
                    
                case PluginStep.STEP_DOWNLOAD :
                	
                    try {
                    	
                        requestInfo = postRequest((new URL(actionString)), requestInfo.getCookie(), actionString, null, postTarget + "&captcha=" + (String) steps.get(1).getParameter(), true);
                        postTarget = getFirstMatch(requestInfo.getHtmlCode(), patternForDownloadlink, 1);
                        postTarget = postTarget.replaceAll("&amp;", "&");
                        
                    } catch (Exception e) {
                    	
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        e.printStackTrace();
                        
                    }

                    try {
                    	
                        URLConnection urlConnection = new URL(postTarget).openConnection();
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        downloadLink.setName(this.getFileNameFormHeader(urlConnection));

                        if (!hasEnoughHDSpace(downloadLink)) {
                        	
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                            
                        }
                        
                        if (download(downloadLink, urlConnection)!=DOWNLOAD_SUCCESS) {
                        	
                            step.setStatus(PluginStep.STATUS_DONE);
                            downloadLink.setStatus(DownloadLink.STATUS_DONE);
                            return null;
                            
                        } else {       
                        	
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            
                        }
                        
                    } catch (IOException e) {
                    	
                        logger.severe(JDLocale.L("plugins.hoster.general.urlError", "URL konnte nicht geöffnet werden") + e.toString());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    break;
                    
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
            logger.severe(JDLocale.L("plugins.host.premium.noLoginData", "Bitte Logindaten eingeben"));
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
                  	
                  	if ( requestInfo.getHtmlCode().contains("NOT_AVAILABLE") ) {
                        
                      	step.setStatus(PluginStep.STATUS_ERROR);
                       	downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
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
                    	
                        URLConnection urlConnection = requestInfo.getConnection();
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        downloadLink.setName(this.getFileNameFormHeader(urlConnection));

                        if ( !hasEnoughHDSpace(downloadLink) ) {
                        	
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                            
                        }
                        
                        if ( download(downloadLink, urlConnection) == DOWNLOAD_SUCCESS ) {
                        	
                            step.setStatus(PluginStep.STATUS_DONE);
                            downloadLink.setStatus(DownloadLink.STATUS_DONE);
                            return null;
                            
                        } else {       
                        	
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_PREMIUM);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            
                        }
                        
                    } catch (Exception e) {
                    	
                    	logger.severe(JDLocale.L("plugins.hoster.general.urlError", "URL konnte nicht geöffnet werden") + e.toString());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        e.printStackTrace();
                        return step;
                        
                    }
                    
                    break;
                    
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
        
    	try {
        	
            requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
            
            if ( requestInfo.containsHTML(NOT_AVAILABLE) ) {
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

    }

    @Override
    public String getAGBLink() {
        return "http://www.filefactory.com/info/terms.php";
    }
    
}
