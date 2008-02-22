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
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class FastLoadNet extends PluginForHost {
	
    private static final String  CODER                    = "eXecuTe";
    private static final String  HOST                     = "fast-load.net";
    private static final String  PLUGIN_NAME              = HOST;
    private static final String  PLUGIN_VERSION           = "0.1.0";
    private static final String  PLUGIN_ID                = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    
    static private final Pattern PAT_SUPPORTED 			  = Pattern.compile("http://.*?fast-load\\.net/index\\.php\\?pid=[a-zA-Z0-9]+");
    private static final int	 MAX_SIMULTAN_DOWNLOADS   = Integer.MAX_VALUE;
    
    private String               downloadURL              = "";
    
    // Suchmasken
    private static final String  DOWNLOAD_SIZE            = "<div id=\"dlpan_size\" style=\".*?\">(.*?) MB</div>";
    private static final String  DOWNLOAD_NAME            = "<div id=\"dlpan_file\" style=\".*?\">(.*?)</div>";
    private static final String  DOWNLOAD_LINK            = "<div id=\"dlpan_btn\" style=\".*?\"><a href=\"(.*?)\">";
    private static final String  NOT_FOUND		          = "Datei existiert nicht";
    
    public FastLoadNet() {
        
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
        return PLUGIN_NAME;
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

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public void reset() {
    	
        this.downloadURL = "";
        
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS; 
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
    	
        try {
        	
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));
            
            if ( requestInfo.getHtmlCode().contains(NOT_FOUND) ) {
            	
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
    	
        try {

            URL downloadUrl = new URL(downloadLink.getDownloadURL());

            switch (step.getStep()) {
            	
                case PluginStep.STEP_PAGE:
                	
                    requestInfo = getRequest(downloadUrl);
                    
                    if ( requestInfo.getHtmlCode().contains(NOT_FOUND) ) {
                    	
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
                    downloadLink.setName(fileName);
                    
                    try {
                    	
                    	int length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim())*1024*1024);
                        downloadLink.setDownloadMax(length);
                        
                    } catch (Exception e) {
                    	
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    // downloadLink auslesen
                    downloadURL = new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_LINK).getFirstMatch();
                    return step;

                case PluginStep.STEP_DOWNLOAD:
                	
                    // Download vorbereiten
                    URLConnection urlConnection = new URL(downloadURL).openConnection();
                    int length = urlConnection.getContentLength();
                    
                    if ( Math.abs(length - downloadLink.getDownloadMax()) > 1024*1024 ) {
                        
                    	logger.warning(JDLocale.L("plugins.host.general.filesizeError", "Dateigrößenfehler -> Neustart"));
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    downloadLink.setDownloadMax(length);
                	
                    // Download starten
                   if ( download(downloadLink, urlConnection) != DOWNLOAD_SUCCESS ) {
                	   
                       step.setStatus(PluginStep.STATUS_ERROR);
                       
                   } else {
                	   
                       step.setStatus(PluginStep.STATUS_DONE);
                       downloadLink.setStatus(DownloadLink.STATUS_DONE);
                
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
    public void resetPluginGlobals() {
    	this.downloadURL = null;
    }

    @Override
    public String getAGBLink() {
        return "http://yourfiles.biz/rules.php";
    }

}