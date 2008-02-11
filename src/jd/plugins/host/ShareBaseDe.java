package jd.plugins.host;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class ShareBaseDe extends PluginForHost {
    private static final String CODER           = "Bo0nZ";
    private static final String HOST            = "sharebase.de";
    private static final String PLUGIN_NAME     = HOST;
    private static final String PLUGIN_VERSION  = "1.0.0.0";
    private static final String PLUGIN_ID       = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?sharebase\\.de/files/[a-zA-Z0-9]{10}\\.html", Pattern.CASE_INSENSITIVE);
                                                                    
    private String cookies = "";
    
    /*
     * Suchmasken
     */
    private static final String FILENAME = "Filename:</td>°<td><strong>°</strong>";
    private static final String FILESIZE = "Filesize:</td>°<td>°</td>";
    private static final String DL_LIMIT = "Das Downloaden ohne Downloadlimit ist nur mit einem Premium-Account";
    private static final String WAIT = "Du musst noch °:°:° warten!";
    
    /*
     * Konstruktor 
     */
    public ShareBaseDe() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }
    

    @Override public boolean    doBotCheck(File file) { return false; } // kein BotCheck
    @Override public String     getCoder() { return CODER; }
    @Override public String     getPluginName() { return PLUGIN_NAME; }
    @Override public String     getHost() { return HOST; }
    @Override public String     getVersion() { return PLUGIN_VERSION; }
    @Override public String     getPluginID() { return PLUGIN_ID; }
    @Override public Pattern    getSupportedLinks() { return PAT_SUPPORTED; } 
    
    @Override public void reset() {
        this.cookies = "";
    }
    
    @Override public int getMaxSimultanDownloadNum() {
        return 1;
    }
    
    @Override public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));

            String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), FILENAME, 1));
            String fileSize = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), FILESIZE, 1));

            //Wurden DownloadInfos gefunden? --> Datei ist vorhanden/online
            if (fileName != null && fileSize != null) {
                fileName = fileName.trim();
                fileSize = fileSize.trim();
                downloadLink.setName(fileName);
            
                try {
                	String[] fileSizeData = fileSize.split(" ");

                    double length = Double.parseDouble(fileSizeData[0].trim());

                	if (fileSizeData[1].equals("KB")) {
                		length *= 1024;
                	} else if (fileSizeData[1].equals("MB")) {
                		length *= 1048576;
                	}
                	
                    downloadLink.setDownloadMax((int)length);
                }
                catch (Exception e) { }
                
                //Datei ist noch verfuegbar
                return true;
            }

        }
        catch (MalformedURLException e) {  }
        catch (IOException e) {  }
        
        //Datei scheinbar nicht mehr verfuegbar, Fehler?
        return false;
    }

    
    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo;
            URL downloadUrl = new URL(downloadLink.getDownloadURL());
            String finishURL = null;
            
            switch (step.getStep()) {
                case PluginStep.STEP_PAGE:
                    requestInfo = getRequest(downloadUrl);
                    
                    String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), FILENAME, 1));

                    // Download-Limit erreicht
                    if (requestInfo.getHtmlCode().contains(DL_LIMIT)) {
                        String hours = getSimpleMatch(requestInfo.getHtmlCode(), WAIT, 0);
                        String minutes = getSimpleMatch(requestInfo.getHtmlCode(), WAIT, 1);
                        String seconds = getSimpleMatch(requestInfo.getHtmlCode(), WAIT, 2);
                        
                    	int waittime = 0;
                        if (hours != null && minutes != null && seconds != null) {
                        	try {
                        		waittime += Integer.parseInt(seconds);
                        		waittime += Integer.parseInt(minutes)*60;
                        		waittime += Integer.parseInt(hours)*3600;
                        		
                        	} catch (Exception Exc) {}
                        	
                        }
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        step.setParameter((long)(waittime * 1000));
                        return step;
                    }

                    //DownloadInfos nicht gefunden? --> Datei nicht vorhanden
                    if (fileName == null) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                    fileName = fileName.trim();

                    //SessionId auslesen
                    this.cookies = requestInfo.getCookie();
                    return step;
                    
                case PluginStep.STEP_DOWNLOAD:
                    try {
                        //Formular abschicken und Weiterleitungs-Adresse auslesen (= DL)
                        requestInfo = postRequest(downloadUrl, this.cookies, downloadLink.getDownloadURL(), null, "machma=Download+starten", false);
                        finishURL = JDUtilities.urlEncode(requestInfo.getConnection().getHeaderField("Location"));
                        
                        // finishURL nicht gefunden? --> Fehler
                        if (finishURL == null) {
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                    }
                    catch (Exception e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                         e.printStackTrace();
                    }
                    
                    try {
                        //Download vorbereiten
                        URLConnection urlConnection = new URL(finishURL).openConnection();
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        String filename = URLDecoder.decode(this.getFileNameFormHeader(urlConnection),"UTF-8");
                        downloadLink.setName(filename);
                        if (!hasEnoughHDSpace(downloadLink)) {
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        //Download starten
                      if(download(downloadLink, urlConnection)!=DOWNLOAD_SUCCESS) {
                          step.setStatus(PluginStep.STATUS_ERROR);
                          
                      }
                      else {
                          step.setStatus(PluginStep.STATUS_DONE);
                          downloadLink.setStatus(DownloadLink.STATUS_DONE);
                   
                      }
                        return step;
                    }
                    catch (IOException e) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        logger.severe("URL could not be opened. " + e.toString());
                    }
                    
                    break;
            }
            return step;            
        }
        catch (IOException e) {
             e.printStackTrace();
            return null;
        }
    }


    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }


    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://sharebase.de/pp.html";
    }  
    
}
