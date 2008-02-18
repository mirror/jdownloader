package jd.plugins.host;

import java.io.File;
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
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://filefactory.com/file/b1bf90/
// http://filefactory.com/f/ef45b5179409a229/ 

public class FileFactory extends PluginForHost {
	
    static private final String host = "filefactory.com";
    private String version = "1.3.0";
    static private final Pattern patternSupported = Pattern.compile("http://.*?filefactory\\.com/file/.{6}/?", Pattern.CASE_INSENSITIVE);

    private static final String            PROPERTY_GET_FILEHEADER        = "GET_FILEHEADER";
    
    private static Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");
    private static Pattern patternForCaptcha = Pattern.compile("src=\"(/captcha2/captcha.php\\?[^\"]*)\" alt=");
    private static Pattern baseLink = Pattern.compile("<a href=\"(.*?)\" id=\"basicLink\"", Pattern.CASE_INSENSITIVE);
    private static Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");
    
    private static final String DOWNLOAD_INFO = "<h1 style=\"width:370px;\">°</h1>°<p>°Size:°MB<br />°Description: ";
    private static final String FILENAME = "<h1 style=\"width:.*?px;\">(.*?)</h1>";
    private static final String NOT_AVAILABLE = "this file is no longer available";
    private static final String NO_SLOT = "no free download slots";
    
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
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        
    	DownloadLink downloadLink = null;
        
    	try {
    		
            logger.info("Step: " + step);
            downloadLink = (DownloadLink) parameter;
            
            switch (step.getStep()) {
            
                case PluginStep.STEP_WAIT_TIME :
                	
                    requestInfo = getRequest(new URL(downloadLink.getDownloadURL()), null, null, true);
                    
                    if (requestInfo.getHtmlCode().indexOf(NOT_AVAILABLE) >= 0) {
                    	
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                        return step;
                        
                    }
                    
                    if (requestInfo.containsHTML(NO_SLOT)) {
                    	
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        return step;
                        
                    }
                    
                    String newURL = "http://"+requestInfo.getConnection().getURL().getHost()+getFirstMatch(requestInfo.getHtmlCode(), baseLink, 1);
                    logger.info(newURL);
                    
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
                
                Boolean getFileHeader = this.getProperties().getBooleanProperty(PROPERTY_GET_FILEHEADER, false);
            	
                // Dateiname ist auf der Seite nur gekürzt auslesbar
                // http://www.filefactory.com/file/d0b032/
				if ( getFileHeader && fileName.substring(fileName.length()-3, fileName.length()).equals("...") ) {
					
					logger.info(JDLocale.L("plugins.hoster.filefactory.getFullFilenameInfo", "Dateiname hat Überlänge - Captcha muss für vollen Namen erkannt werden"));
					
					String newURL = "http://" + requestInfo.getConnection().getURL().getHost()
							+ getFirstMatch(requestInfo.getHtmlCode(), baseLink, 1);
					
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
					
					File captchaFile = this.getLocalCaptchaFile(this);
					JDUtilities.download(captchaFile, captchaAddress);
					String captchaText = JDUtilities.getCaptcha(this, "filefactory.com", captchaFile, false);
					requestInfo = postRequest((new URL(actionString)), requestInfo.getCookie(),
							actionString, null, postTarget + "&captcha=" + captchaText, true);
					postTarget = getFirstMatch(requestInfo.getHtmlCode(), patternForDownloadlink, 1);
					
					URLConnection urlConnection = new URL(postTarget).openConnection();
					length = urlConnection.getContentLength();
					fileName = this.getFileNameFormHeader(urlConnection);
					
				} else if ( fileName.substring(fileName.length()-3, fileName.length()).equals("...") ) {
					
					logger.info(JDLocale.L("plugins.hoster.general.filnameShorted", "Dateiname ist gekürzt"));
					
				} else {
	            	
	            	String fileSize = getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 3);
	            	
	            	if (fileSize != null) {
	                    try {
	                        length = (int) (Double.parseDouble(fileSize.trim()) * 1024 * 1024);
	                    } catch (Exception e) { }
	                }
	            	
	            }
				
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
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), PROPERTY_GET_FILEHEADER,
        		JDLocale.L("plugins.hoster.filefactory.getFileHeaderIfFilenameTooLong1",
        				"Wenn gekürzt, vollständigen Dateinamen aus dem Dateiheader lesen")));
        cfg.setDefaultValue(false);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL,
        		JDLocale.L("plugins.hoster.filefactory.getFileHeaderIfFilenameTooLong2",
        				"(benötigt Captchaerkennung und kann einige Sekunden in Anspruch nehmen)")));

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
