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

public class EgoshareCom extends PluginForHost {
    private static final String CODER           = "Bo0nZ";
    private static final String HOST            = "egoshare.com";
    private static final String PLUGIN_NAME     = HOST;
    private static final String PLUGIN_VERSION  = "1.0.0.0";
    private static final String PLUGIN_ID       = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    private static final Pattern PAT_SUPPORTED  = getSupportPattern("http://[*]egoshare.com/[+]/[+]");
                                                                    
    private String sessionID = "";
    
    /*
     * Suchmasken (z.B. Fehler)
     */
    private static final String ERROR_DOWNLOAD_DELETED  = "Download was deleted";
    private static final String ERROR_DOWNLOAD_NOT_FOUND = "No Download associated with that id";
    private static final String DOWNLOAD_INFO           = "Filename: </td><td><b>°</b></td></tr>°°°<tr><td>Filesize: </td><td><b>° MB</b></td>";
    private static final String SESSION_ID              = "<input type=\"hidden\" value=\"°\" name=\"PHPSESSID\" />";
    
    /*
     * Konstruktor 
     */
    public EgoshareCom() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
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
        this.sessionID = "";
    }
    
    @Override public int getMaxSimultanDownloadNum() {
        return 999; // eigentlich keine Begrenzung paralleler Downloads
    }
    
    @Override public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            RequestInfo requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()));

            String fileName = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 0));
            String fileSize = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), DOWNLOAD_INFO, 4));

            //Wurden DownloadInfos gefunden? --> Datei ist vorhanden/online
            if (fileName != null && fileSize != null) {
                downloadLink.setName(fileName);
            
                try {
                    int length = (int) (Double.parseDouble(fileSize.trim()) * 1024 * 1024);
                    downloadLink.setDownloadMax(length);
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
            URL downloadUrl = new URL(downloadLink.getUrlDownloadDecrypted());
            String finishURL = null;
            
            switch (step.getStep()) {
                case PluginStep.STEP_PAGE:
                    requestInfo = getRequest(downloadUrl);
                    
                    // Datei geloescht?
                    if (requestInfo.getHtmlCode().indexOf(ERROR_DOWNLOAD_DELETED) > 0) {
                        logger.severe("download was deleted");
                        // Abused-Status setzen, da es (noch) keinen Deleted-Status gibt :(
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_ABUSED);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                    // Datei nicht gefunden?
                    if (requestInfo.getHtmlCode().indexOf(ERROR_DOWNLOAD_NOT_FOUND) > 0) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    //SessionId auslesen
                    this.sessionID = JDUtilities.htmlDecode(getSimpleMatch(requestInfo.getHtmlCode(), SESSION_ID, 0));
                    return step;

                case PluginStep.STEP_PENDING:
                    // immer 4 Sekunden vor dem Download warten!
                    step.setParameter(4000l);
                    return step;
                    
                case PluginStep.STEP_DOWNLOAD:
                    try {
                        //Formular abschicken und Weiterleitungs-Adresse auslesen (= DL)
                        requestInfo = postRequest(downloadUrl, null, downloadLink.getUrlDownloadDecrypted(), null, "PHPSESSID="+this.sessionID+"&submitname=DOWNLOAD", false);
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
                        downloadLink.setName(URLDecoder.decode(this.getFileNameFormHeader(urlConnection),"UTF-8"));
                        
                        //Download starten
                        boolean downloadSuccess = download(downloadLink, urlConnection);
                        
                        // Download erfolgreich oder fehlerhaft?
                        if (downloadSuccess == false) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            
                        } else {
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
    
}
