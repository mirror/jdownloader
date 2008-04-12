//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

// http://bluehost.to/dl=sTCn35Di9

public class BluehostTo extends PluginForHost {
	
    private static final String  CODER                	= "jD-Team";
    private static final String  HOST                  	= "bluehost.to";
    private static final String  VERSION           		= "0.1.0";
    private static final String  AGB_LINK          		= "http://bluehost.to/agb.php";
    
    static private final Pattern PAT_SUPPORTED 			= getSupportPattern("http://[*]bluehost\\.to/dl=[a-zA-Z0-9]+");
    private static final int	 MAX_SIMULTAN_DOWNLOADS	= Integer.MAX_VALUE;
    
    private String               session              	= "";
    private String               code1              	= "";
    private String               code2              	= "";
    private String               hash              	 	= "";
    
    // Suchmasken
    private static final String  DOWNLOAD_SIZE         	= "<div class=\"dl_groessefeld\">(.*?)<font style='.*?'>(MB|KB|B)</font></div>";
    private static final String  DOWNLOAD_NAME       	= "<div class=\"dl_filename2\">(.*?)</div>";
    //private static final String  DOWNLOAD_LINK      	  = "<div id=\"dlpan_btn\" style=\".*?\"><a href=\"(.*?)\">";
    private static final String  SESSION	           	= "name=\"UPLOADSCRIPT_LOGSESSION\" value=\"(.*?)\"";
    private static final String  CODE	              	= "<input type=\"hidden\" name=\"dateidownload\" value=\"erlaubt\" />\n<input type=\"hidden\" name=\"(.*?)\" value=\"(.*?)\" />";
    private static final String  HASH	             	= "name=\"downloadhash\" value=\"(.*?)\"";
    private static final String  DOWNLOAD_URL	       	= "http://bluehost.to/dl.php?UPLOADSCRIPT_LOGSESSION=";
    
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
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
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
            
            try {
            	
            	int length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim())*1024*1024);
                downloadLink.setDownloadMax(length);
                
            } catch (Exception e) {

            	logger.severe("Filesize could not be read from HTML code");
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                return false;
                
            }
            
            String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_NAME).getFirstMatch()).trim();
            
            // for cutted filenames
            if ( fileName.length() == 35 ) {
            	
            	logger.info("Filename cutted - get it from header");
            	
            	session = new Regexp(requestInfo.getHtmlCode(), SESSION).getFirstMatch();
                code1 = new Regexp(requestInfo.getHtmlCode(), CODE).getFirstMatch(1);
                code2 = new Regexp(requestInfo.getHtmlCode(), CODE).getFirstMatch(2);
                hash = new Regexp(requestInfo.getHtmlCode(), HASH).getFirstMatch();
                
                if ( session == null ) {
                	
                	logger.severe("Could not read parameters from HTML code");
                	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    return false;
                    
                }
                
                requestInfo = postRequestWithoutHtmlCode(new URL(DOWNLOAD_URL+session),
                		"UPLOADSCRIPT_LOGSESSION="+session+"; fd_upload_bluehost_bl=aus;",
                		null, "dateidownload=erlaubt&"+code1+"="+code2+"&downloadhash="+hash, true);
                downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()).substring(0,
                		getFileNameFormHeader(requestInfo.getConnection()).length()-2));
            	
            } else if ( fileName == null ) {
            	
            	logger.severe("Filename could not be read from HTML code");
            	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                return false;
                
            } else downloadLink.setName(fileName);
            
            return true;

        } catch (MalformedURLException e) {
             e.printStackTrace();
        } catch (IOException e) {
             e.printStackTrace();
        }

        // unknown error
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
                    
                    try {
                    	
                    	int length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_SIZE).getFirstMatch().trim())*1024*1024);
                        downloadLink.setDownloadMax(length);
                        
                    } catch (Exception e) {

                    	logger.severe("Filesize from HTML code incorrect");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                    
                    session = new Regexp(requestInfo.getHtmlCode(), SESSION).getFirstMatch();
                    code1 = new Regexp(requestInfo.getHtmlCode(), CODE).getFirstMatch(1);
                    code2 = new Regexp(requestInfo.getHtmlCode(), CODE).getFirstMatch(2);
                    hash = new Regexp(requestInfo.getHtmlCode(), HASH).getFirstMatch();
                    
                    if ( session == null ) {
                    	
                    	logger.severe("Could not read parameters from HTML code");
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                        
                    }
                    
                	return step;

                case PluginStep.STEP_DOWNLOAD:
                	
                    // prepare download
                	requestInfo = postRequestWithoutHtmlCode(new URL(DOWNLOAD_URL+session),
                			"UPLOADSCRIPT_LOGSESSION="+session+"; fd_upload_bluehost_bl=aus;",
                			null, "dateidownload=erlaubt&"+code1+"="+code2+"&downloadhash="+hash, true);
                	HTTPConnection urlConnection = requestInfo.getConnection();

                    final long length = downloadLink.getDownloadMax();
                    // replaces cutted filenames
                    downloadLink.setName(getFileNameFormHeader(urlConnection).substring(0,
                    		getFileNameFormHeader(urlConnection).length()-2));
                    
                   dl = new RAFDownload(this, downloadLink, urlConnection);

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

}