//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class ShareBaseDe extends PluginForHost {
	
    private static final String CODER           = "JD-Team";
    private static final String HOST            = "sharebase.de";
    private static final String PLUGIN_NAME     = HOST;
    private static final String PLUGIN_VERSION  = "1.0.1";
    private static final String PLUGIN_ID       = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://.*?sharebase\\.de/files/[a-zA-Z0-9]{10}\\.html", Pattern.CASE_INSENSITIVE);
                                                                    
    private String cookies = "";
    
    /*
     * Suchmasken
     */
    private static final String FILENAME = "<title>(.*?)</title>";
    private static final String FILESIZE = "<span class=\"f1\">.*?\\((.*?)\\)</span></td>";
    private static final String DL_LIMIT = "Das Downloaden ohne Downloadlimit ist nur mit einem Premium-Account";
    private static final String SIM_DL = "Das gleichzeitige Downloaden";
    private static final String WAIT = "Du musst noch °:°:° warten!";
    private static final String DOWLOAD_RUNNING ="Von deinem Computer ist noch ein Download aktiv";
    
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
        	
            RequestInfo requestInfo = HTTP.getRequest(new URL(downloadLink.getDownloadURL()));
            String fileName = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILENAME).getFirstMatch());
            String fileSize = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILESIZE).getFirstMatch());
            boolean sim_dl = new Regex(requestInfo.getHtmlCode(), SIM_DL).count() > 0;
            boolean dl_limit = new Regex(requestInfo.getHtmlCode(), DL_LIMIT).count() > 0;
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
                
                return true;
            }
            
            if(sim_dl || dl_limit)
                return true;

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
                	
                    requestInfo = HTTP.getRequest(downloadUrl);
               
                    String fileName = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), FILENAME).getFirstMatch());
                    
                    if(requestInfo.containsHTML(DOWLOAD_RUNNING)){
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        step.setParameter((long)(60 * 1000));
                        
                    }
                    // Download-Limit erreicht
                    if (requestInfo.getHtmlCode().contains(DL_LIMIT)) {
                    	
                        String hours = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), WAIT, 0);
                        String minutes = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), WAIT, 1);
                        String seconds = SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), WAIT, 2);
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
                    this.cookies = requestInfo.getCookie().split("; ")[0];
                    return step;
                    
                case PluginStep.STEP_DOWNLOAD:
                	
                    try {
                        requestInfo = HTTP.postRequest(downloadUrl, this.cookies, downloadLink.getDownloadURL(), null, "doit=Download+starten", false);
                        finishURL = JDUtilities.htmlDecode(requestInfo.getConnection().getHeaderField("Location"));
                        
                        if (finishURL == null) {
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            return step;
                        }
                        
                    } catch (Exception e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                         e.printStackTrace();
                    }
                    
                    try {
                    	
                        //Download vorbereiten
                        HTTPConnection urlConnection = new HTTPConnection(new URL(finishURL).openConnection());
                        int length = urlConnection.getContentLength();
                        downloadLink.setDownloadMax(length);
                        String filename = URLDecoder.decode(this.getFileNameFormHeader(urlConnection),"UTF-8");
                        downloadLink.setName(filename);
                    
                      
                        //Download starten
                       dl = new RAFDownload(this, downloadLink, urlConnection);
                    
                        dl.startDownload();
                      	return step;
                      
                    } catch (IOException e) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        logger.severe("URL could not be opened. " + e.toString());
                    }
                    
                    break;
            }
            
            return step;    
            
        } catch (IOException e) {
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
