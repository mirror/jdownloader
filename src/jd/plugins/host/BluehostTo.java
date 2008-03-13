package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.controlling.interaction.CaptchaMethodLoader;
import jd.plugins.Download;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class BluehostTo extends PluginForHost {
	
    private static final String  CODER                    = "jD-Team";
    private static final String  HOST                     = "bluehost.to";
    private static final String  PLUGIN_NAME              = HOST;
    private static final String  PLUGIN_VERSION           = "0.1.0";
    private static final String  PLUGIN_ID                = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    
    static private final Pattern PAT_SUPPORTED 			  = getSupportPattern("http://[*]bluehost\\.to/dl=[a-zA-Z0-9]+");
    private static final int	 MAX_SIMULTAN_DOWNLOADS   = 1;
    
    private String               session              	  = "";
    private String               code1              	  = "";
    private String               code2              	  = "";
    private String               hash              	      = "";
    
    // Suchmasken
    private static final String  DOWNLOAD_SIZE            = "<div class=\"dl_groessefeld\">(.*?)<font style='.*?'>(MB|KB|B)</font></div>";
    private static final String  DOWNLOAD_NAME            = "<div class=\"dl_filename2\">(.*?)</div>";
    //private static final String  DOWNLOAD_LINK            = "<div id=\"dlpan_btn\" style=\".*?\"><a href=\"(.*?)\">";
    private static final String  SESSION	              = "name=\"UPLOADSCRIPT_LOGSESSION\" value=\"(.*?)\"";
    private static final String  CODE	                  = "<input type=\"hidden\" name=\"dateidownload\" value=\"erlaubt\" />\n<input type=\"hidden\" name=\"(.*?)\" value=\"(.*?)\" />";
    private static final String  HASH	                  = "name=\"downloadhash\" value=\"(.*?)\"";
    
    private static final String  DOWNLOAD_URL	          = "http://bluehost.to/dl.php?UPLOADSCRIPT_LOGSESSION=";
    
    public BluehostTo() {
        
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
    	session = "";
        code1 = "";
        code2 = "";
        hash = "";
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS; 
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
    	
        try {
        	
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));
            
            if ( requestInfo.getConnection().getHeaderField("Location") != null
            		&& requestInfo.getConnection().getHeaderField("Location").equals("index.php") ) {
            	
            	downloadLink.setName(downloadLink.getName().substring(3));
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
                } catch (Exception e) {
                	e.printStackTrace();
                }
                
                return true;
                
            } else return false;

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

            switch ( step.getStep() ) {
            	
                case PluginStep.STEP_PAGE:
                	
                    requestInfo = getRequest(downloadUrl);
                    
                    if ( requestInfo.getConnection().getHeaderField("Location") != null
                    		&& requestInfo.getConnection().getHeaderField("Location").equals("index.php") ) {
                    	
                    	downloadLink.setName(downloadLink.getName().substring(3));
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
        				
        			}
                    
                    String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
                    downloadLink.setName(fileName);
                    
                    try {
                    	
                    	int length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim())*1024*1024);
                        downloadLink.setDownloadMax(length);
                        logger.severe(String.valueOf(length));
                    } catch (Exception e) {
                    	
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    
                    session = new Regexp(requestInfo.getHtmlCode(), SESSION).getFirstMatch();
                    logger.severe(session);
                    code1 = new Regexp(requestInfo.getHtmlCode(), CODE).getFirstMatch(1);
                    logger.severe(code1);
                    code2 = new Regexp(requestInfo.getHtmlCode(), CODE).getFirstMatch(2);
                    logger.severe(code2);
                    hash = new Regexp(requestInfo.getHtmlCode(), HASH).getFirstMatch();
                    logger.severe(hash);
                    
                	return step;

                case PluginStep.STEP_DOWNLOAD:
                	
                    // Download vorbereiten
                	requestInfo = postRequestWithoutHtmlCode(new URL(DOWNLOAD_URL+session), null, null, "UPLOADSCRIPT_LOGSESSION="+session+"&dateidownload=erlaubt&"+code1+"="+code2+"&downloadhash="+hash, true);
                	HTTPConnection urlConnection = requestInfo.getConnection();
                    
                    /*if ( Math.abs(length - downloadLink.getDownloadMax()) > 1024*1024 ) {
                    	
                    	logger.warning("Filesize Error");
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    	step.setStatus(PluginStep.STATUS_ERROR);
                    	return step;
                        
                    }*/
                    
                    //downloadLink.setDownloadMax(length);
                    /*int errorid;

                    // Download starten
                    if ( (errorid = download(downloadLink, urlConnection)) == DOWNLOAD_SUCCESS ) {
                    	
                    	step.setStatus(PluginStep.STATUS_DONE);
                    	downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    	return step;
                    	
                    } else if ( errorid == DOWNLOAD_ERROR_OUTPUTFILE_ALREADYEXISTS ) {
                    	
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_ALREADYEXISTS);
                    	step.setStatus(PluginStep.STATUS_ERROR);  
                    	return step;
                   		
                    } else {       
                    	
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    	step.setStatus(PluginStep.STATUS_ERROR);
                    	
                    }*/
                    
                    //Download dl = new Download(this, downloadLink, urlConnection);
                    //dl.setChunks(1);
                    //dl.startDownload();
                    //dl.handleErrors();
                    
                    final long length = downloadLink.getDownloadMax();
                    downloadLink.setName(getFileNameFormHeader(urlConnection).substring(0,getFileNameFormHeader(urlConnection).length()-2));
                    
                    logger.severe("con-len:"+urlConnection.getContentLength());
                    logger.severe("down-max:"+downloadLink.getDownloadMax());
                    
                    Download dl = new Download(this, downloadLink, urlConnection);
                    dl.setFilesize(length);
                    
                    if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
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
        return "http://www.fast-load.net/infos.php";
    }

}