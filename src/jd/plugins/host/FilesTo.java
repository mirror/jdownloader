package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class FilesTo extends PluginForHost {
	
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://www\\.files\\.to/get/[0-9]+/[a-zA-Z0-9]+");
    static private final String HOST = "files.to";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.1.0";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "eXecuTe";
    
    private Pattern FILE_INFO_NAME = Pattern.compile("<p>Name: (.*?)</p>");
    private Pattern FILE_INFO_SIZE = Pattern.compile("<p>Gr&ouml;&szlig;e: (.*? (KB|MB)<)/p>");
    private Pattern CAPTCHA_FLE = Pattern.compile("<img src=\"(http://www.files\\.to/captcha_[0-9]+\\.jpg)");
    private Pattern DOWNLOAD_URL = Pattern.compile("action\\=\"(http://.*?files\\.to/dl/.*?)\">");
    private Pattern SESSION = Pattern.compile("action\\=\"\\?(PHPSESSID\\=.*?)\"");
    
    static private final String FILE_NOT_FOUND = "Die angeforderte Datei konnte nicht gefunden werden";

    private String captchaAddress;
    private String finalURL;
    private String session;
    private URLConnection        urlConnection;

    public FilesTo() {
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
    
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
    	
        RequestInfo requestInfo;
        
        try {
        	
            String parameterString = parameter.getDownloadURL().toString();
            
            switch (step.getStep()) {
            
                case PluginStep.STEP_WAIT_TIME :
                	
                    requestInfo = getRequest(new URL(parameterString));
                    
                    // Datei gelöscht?
                    if ( requestInfo.getHtmlCode().contains(FILE_NOT_FOUND) ) {
                    	
                        logger.severe("download not found");
                        parameter.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    if ( requestInfo.getHtmlCode() != null ) {
                    	
                    	session = new Regexp(requestInfo.getHtmlCode(), SESSION).getFirstMatch();
                    	captchaAddress = getFirstMatch(requestInfo.getHtmlCode(), CAPTCHA_FLE, 1) + "?" + session;
                        return step;
                        
                    } else {
                    	
                        logger.severe("Unbekannter fehler.. retry in 20 sekunden");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        parameter.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;
                        
                    }
                    
                case PluginStep.STEP_GET_CAPTCHA_FILE :
                	
                    File file = this.getLocalCaptchaFile(this);
                    
                    if (!JDUtilities.download(file, captchaAddress) || !file.exists()) {
                    	
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        parameter.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        System.out.println("asdf");
                        return step;
                        
                    } else {
                    	
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                        
                    }
                    
                    break;
                    
                case PluginStep.STEP_PENDING :
                	
                    step.setParameter(0l);
                    break;
                    
                case PluginStep.STEP_DOWNLOAD :
                	
                    String code = (String) steps.get(2).getParameter();
                    
                    HashMap<String,String> requestHeaders = new HashMap<String,String>();
            		requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
                    
                    requestInfo = postRequest(new URL(parameterString+"?"),
                    			session,
                    			parameterString+"?"+session,
                    			requestHeaders,
                    			"txt_ccode="+code+"&btn_next=",
                    			true);
                    
                    if ( requestInfo.getHtmlCode() == null ) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        parameter.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;
                    }
                    
                    finalURL = new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_URL).getFirstMatch();
                    logger.info(finalURL);
                    
                    // Download vorbereiten
                    parameter.setStatusText("Verbindung aufbauen");
                    urlConnection = new URL(finalURL).openConnection();
                    int fileSize = urlConnection.getContentLength();
                    parameter.setDownloadMax(fileSize);
                    String fileName = JDUtilities.htmlDecode(getFirstMatch(requestInfo.getHtmlCode(), FILE_INFO_NAME, 1));
                    parameter.setName(fileName);
                    
                    if ( !hasEnoughHDSpace(parameter) ) {
                    	parameter.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                    // Download starten
                    boolean downloadSuccess = download(parameter, urlConnection);
                    
                    if (downloadSuccess == false) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        parameter.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    } else {
                        step.setStatus(PluginStep.STATUS_DONE);
                        parameter.setStatus(DownloadLink.STATUS_DONE);
                    }
                    
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
        this.urlConnection = null;
    }
    
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }
    
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
    	
        RequestInfo requestInfo;
        
        try {
        	
        	requestInfo = getRequest(new URL(downloadLink.getDownloadURL().toString()));
        	
            if ( requestInfo.getHtmlCode() == null ) {
                return false;
            } else {

                // Datei gelöscht?
                if ( requestInfo.getHtmlCode().contains(FILE_NOT_FOUND) ) {
                    return false;
                }

                String fileName = JDUtilities.htmlDecode(getFirstMatch(requestInfo.getHtmlCode(), FILE_INFO_NAME, 1));
                int fileSize = getFileSize(JDUtilities.htmlDecode(getFirstMatch(requestInfo.getHtmlCode(), FILE_INFO_SIZE, 1)));
                downloadLink.setName(fileName);
                
                try {
                    
                    downloadLink.setDownloadMax(fileSize);
                    
                } catch (Exception e) { }
                
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
    @Override
    public String getAGBLink() {
        return "http://www.files.to/content/aup";
    }

    private int getFileSize(String source) {
    	
    	int size = 0;
    	
    	if ( source.contains("KB") ) {
    		source = getSimpleMatch(source, "° KB", 0);
    		size = Integer.parseInt(source)*1024;
    	} else if ( source.contains("MB") ) {
    		source = getSimpleMatch(source, "° MB", 0);
    		size = (int) (Integer.parseInt(source)*1024*1024);
    	}
    	
    	return size;
    	
    }
    
}
