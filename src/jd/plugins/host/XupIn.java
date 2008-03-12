package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

// http://www.xup.in/dl,43227676/YourFilesBiz.java/

public class XupIn extends PluginForHost {
	
    private static final String  CODER                    = "jD-Team";
    private static final String  HOST                     = "xup.in";
    private static final String  PLUGIN_VERSION           = "0.1.1";
    private static final String  AGB_LINK                 = "http://www.xup.in/terms/";
    
    static private final Pattern PATTERN_SUPPORTED 		  = getSupportPattern("http://[*]xup\\.in/dl,[0-9]+/?[+]?");
    private static final int	 MAX_SIMULTAN_DOWNLOADS   = Integer.MAX_VALUE;
    
    private String               vid              	  	  = "";
    private String               vtime              	  = "";
    private String               vpass              	  = "";
    
    private static final String  DOWNLOAD_SIZE            = "<li class=\"iclist\">File Size: (.*?) Mbyte</li>";
    private static final String  DOWNLOAD_NAME            = "<legend> <b>Download: (.*?)</b> </legend>";
    private static final String  NAME_FROM_URL            = "http://.*?xup\\.in/dl,[0-9]+/(.*?)";
    private static final String  VID	                  = "value=\"(.*?)\" name=\"vid\"";
    private static final String  VTIME	                  = "value=\"([0-9]+)\" name=\"vtime\"";
    private static final String  NOT_FOUND	              = "File does not exist";
    private static final String  PASSWORD_PROTECTED	      = "Bitte Passwort eingeben";
    
    public XupIn() {
        
    	super();
        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + PLUGIN_VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PATTERN_SUPPORTED;
    }

    @Override
    public void reset() {
    	
    	vid = "";
        vtime = "";
        vpass = "";
        
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS; 
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
    	
        try {
        	
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));
            
            if ( requestInfo.containsHTML(NOT_FOUND) ) {
            	
            	if ( new Regexp(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch() != null )
            		downloadLink.setName(new Regexp(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch());
				downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
				return false;
				
			}
            
            String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
            Integer length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim())*1024*1024);
            
            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {
            	
                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadMax(length);
                } catch (Exception e) { }
                
                return true;
                
            }

        } catch (MalformedURLException e) {
             e.printStackTrace();
        } catch (IOException e) {
             e.printStackTrace();
        }

        // unbekannter fehler
        return false;
        
    }
    
    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
    	
    	if (aborted) {
    		
            logger.warning("Plugin aborted");
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            step.setStatus(PluginStep.STATUS_TODO);
            return step;
            
        }
    	
        try {

            URL downloadUrl = new URL(downloadLink.getDownloadURL());

            switch ( step.getStep() ) {
            	
                case PluginStep.STEP_PAGE:
                	
                    requestInfo = getRequest(downloadUrl);
                    
                    if ( requestInfo.containsHTML(NOT_FOUND) ) {
                    	
                    	if ( new Regexp(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch() != null )
                    		downloadLink.setName(new Regexp(requestInfo.getHtmlCode(), NAME_FROM_URL).getFirstMatch());
        				downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
        				step.setStatus(PluginStep.STATUS_ERROR);
        				return step;
        				
        			}
                    
                    if ( requestInfo.containsHTML(PASSWORD_PROTECTED) ) {
                    	
                    	vpass = JDUtilities.getController().getUiInterface().showUserInputDialog(
                    			JDLocale.L("plugins.hoster.general.passwordProtectedInput", "Die Links sind mit einem Passwort gesch\u00fctzt. Bitte geben Sie das Passwort ein:"));
                    	
                    }
                    
                    String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
                    downloadLink.setName(fileName);
                    
                    try {
                    	
                    	int length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim())*1024*1024);
                        downloadLink.setDownloadMax(length);
                        
                    } catch (Exception e) {
                    	
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    if (JDUtilities.getController().isLocalFileInProgress(downloadLink)) {
                		
                        logger.severe("File already is in progress: " + downloadLink.getFileOutput());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_OUTPUTFILE_OWNED_BY_ANOTHER_LINK);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }

                    if (new File(downloadLink.getFileOutput()).exists()) {
                    	
                        logger.severe("File already exists: " + downloadLink.getFileOutput());
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    if ( !hasEnoughHDSpace(downloadLink) ) {
                    	
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    vid = new Regexp(requestInfo.getHtmlCode(), VID).getFirstMatch();
                    vtime = new Regexp(requestInfo.getHtmlCode(), VTIME).getFirstMatch();
                    
                	return step;

                case PluginStep.STEP_DOWNLOAD:
                	
                	if ( vpass != "") {
                		requestInfo = postRequestWithoutHtmlCode(downloadUrl, null, null, "vid="+vid+"&vtime="+vtime+"&vpass="+vpass, true);
                	} else {
                		requestInfo = postRequestWithoutHtmlCode(downloadUrl, null, null, "vid="+vid+"&vtime="+vtime, true);
                	}
                	
                	HTTPConnection urlConnection = requestInfo.getConnection();
                    int length = urlConnection.getContentLength();
                    
                    if ( urlConnection.getContentType().contains("text/html") ) {
                    	
                    	logger.severe("Wrong password");
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                    	step.setStatus(PluginStep.STATUS_ERROR);
                    	return step;
                    	
                    }
                    
                    if ( Math.abs(length - downloadLink.getDownloadMax()) > 1024*1024 ) {
                    	
                    	logger.severe("Filesize Error");
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    	step.setStatus(PluginStep.STATUS_ERROR);
                    	return step;
                        
                    }
                    
                    int errorid;

                    // Download starten
                    Download dl = new Download(this, downloadLink, urlConnection);

                    if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
     
                    	
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    	step.setStatus(PluginStep.STATUS_ERROR);
                    	
                    }
                    
                    return step;
                    
            }
            
            return step;
            
        } catch (IOException e) {
        	
            e.printStackTrace();
            return step;
            
        }
        
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

}