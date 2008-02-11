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
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class YourFilesBiz extends PluginForHost {
	
    private static final String  CODER                    = "eXecuTe";
    private static final String  HOST                     = "yourfiles.biz";
    private static final String  PLUGIN_NAME              = HOST;
    private static final String  PLUGIN_VERSION           = "0.1.0";
    private static final String  PLUGIN_ID                = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    
    static private final Pattern PAT_SUPPORTED 			  = Pattern.compile("http://.*?yourfiles\\.biz/\\?d\\=[a-zA-Z0-9]+");
    private static final int	 MAX_SIMULTAN_DOWNLOADS   = 999; // unbegrenzt

    private String               downloadURL              = "";
    private URLConnection        urlConnection;
    
    // Suchmasken
    //private static final String  ERROR_DOWNLOAD_NOT_FOUND = "Die angefragte Datei wurde nicht gefunden";
    private static final String  DOWNLOAD_SIZE            = "<td align=left><b>Dateigröße:</b></td>\n       <td align=left>°</td>";
    private static final String  DOWNLOAD_NAME            = "<td align=left width=20%><b>Dateiname:</b></td>\n       <td align=left width=80%>°</td>";
    private static final String  DOWNLOAD_LINK            = "value='http://°'>";
    
    public YourFilesBiz() {
        
    	super();

        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
        
    }

    @Override
    public boolean doBotCheck(File file) {
        return false; // kein BotCheck
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
        urlConnection = null;
        
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS; 
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
    	
        try {
        	
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));
            
            if ( requestInfo.getHtmlCode().equals("") ) {
            	logger.severe("download not found");
				downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
				return false;
			}
            
            String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_NAME, 0));
            Integer length = getFileSize(requestInfo.getHtmlCode());
            
            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {
            	
                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadMax(length);
                } catch (Exception e) { }
                
                return true;
                
            }

        }
        catch (MalformedURLException e) {
             e.printStackTrace();
        }
        catch (IOException e) {
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

                    // serverantwort leer (weiterleitung) -> download nicht verfügbar
                    if (requestInfo.getHtmlCode().equals("")) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                    String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_NAME, 0));
                    downloadLink.setName(fileName);
                    
                    try {
                    	int length = getFileSize(requestInfo.getHtmlCode());
                        downloadLink.setDownloadMax(length);
                    } catch (Exception e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                    // downloadLink auslesen
                    this.downloadURL = "http://"+JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_LINK, 0));
                    return step;
                    
                case PluginStep.STEP_WAIT_TIME:
                	
                    // Download vorbereiten
                    downloadLink.setStatusText("Verbindung aufbauen");
                    urlConnection = new URL(this.downloadURL).openConnection();
                    int length = urlConnection.getContentLength();
                    
                    if ( Math.abs(length - downloadLink.getDownloadMax()) > 1024*1024 ) {
                        logger.warning("Dateigrößenfehler -> Neustart");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                    downloadLink.setDownloadMax(length);
                    return step;

                case PluginStep.STEP_DOWNLOAD:
                	
                    // Download starten
                   if(download(downloadLink, urlConnection)!=DOWNLOAD_SUCCESS) {
                       step.setStatus(PluginStep.STATUS_ERROR);
                       
                   }
                   else {
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
    }

    @Override
    public String getAGBLink() {
        return "http://yourfiles.biz/rules.php";
    }

    private int getFileSize(String source) {
    	
    	String sizeString = JDUtilities.htmlDecode(getSimpleMatch(source, DOWNLOAD_SIZE, 0));
    	int size = 0;
    	
    	if ( sizeString.contains("KB") ) {
    		sizeString = getSimpleMatch(sizeString, "° KB", 0);
    		size = (int) Math.round(Double.parseDouble(sizeString)*1024);
    	} else if ( sizeString.contains("MB") ) {
    		sizeString = getSimpleMatch(sizeString, "° MB", 0);
    		size = (int) Math.round(Double.parseDouble(sizeString)*1024*1024);
    	}
    	
    	return size;
    	
    }

}