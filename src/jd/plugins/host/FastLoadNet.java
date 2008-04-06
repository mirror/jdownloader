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
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class FastLoadNet extends PluginForHost {

    private static final String  CODER                  = "eXecuTe";

    private static final String  HOST                   = "fast-load.net";

    private static final String  PLUGIN_NAME            = HOST;

    private static final String  PLUGIN_VERSION         = "0.2.0";

    private static final String  PLUGIN_ID              = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final Pattern PAT_SUPPORTED          = Pattern.compile("http://.*?fast-load\\.net(/|//)index\\.php\\?pid=[a-zA-Z0-9]+");

    private static final int     MAX_SIMULTAN_DOWNLOADS = 1;

    private String               cookie            		= "";

    // Suchmasken
    private static final String  DOWNLOAD_INFO          = "<th.*?><b>File</b></th>\\s*?<th.*?><b>Size</b></th>\\s*?</tr>\\s*?<tr>\\s*?<td.*?><font.*?>(.*)</font></td>\\s*?<td.*?><font.*?>(.*) MB</font></td>";

    private static final String  NOT_FOUND              = "Datei existiert nicht";

    public FastLoadNet() {

        super();
        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
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
        this.cookie = "";
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return MAX_SIMULTAN_DOWNLOADS;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        try {

            RequestInfo requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));

            if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                downloadLink.setName(downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=")+4));
                return false;

            }
            
            String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(1)).trim();
            Integer length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(2).trim()) * 1024 * 1024);
            
            // downloadinfos gefunden? -> download verfügbar
            if (fileName != null && length != null) {

                downloadLink.setName(fileName);

                try {
                    downloadLink.setDownloadMax(length);
                }
                catch (Exception e) {
                }

                return true;

            } else {
            	downloadLink.setName(downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=")+4));
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
    	
    	if (aborted) {
    		
            logger.warning("Plugin aborted");
            downloadLink.setStatus(DownloadLink.STATUS_TODO);
            step.setStatus(PluginStep.STATUS_TODO);
            return step;
            
        }
    	
        try {

            URL downloadUrl = new URL(downloadLink.getDownloadURL());

            switch (step.getStep()) {

                case PluginStep.STEP_PAGE:
                	
                    requestInfo = getRequest(downloadUrl);
                    cookie = requestInfo.getCookie();

                    if (requestInfo.getHtmlCode().contains(NOT_FOUND)) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }

                    String fileName = JDUtilities.htmlDecode(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(1)).trim();
                    downloadLink.setName(fileName);

                    try {

                        int length = (int) Math.round(Double.parseDouble(new Regexp(requestInfo.getHtmlCode(), DOWNLOAD_INFO).getFirstMatch(2).trim()) * 1024 * 1024);
                        downloadLink.setDownloadMax(length);

                    }
                    catch (Exception e) {

                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;

                    }

                    return step;

                case PluginStep.STEP_GET_CAPTCHA_FILE:
                	
                	File file = this.getLocalCaptchaFile(this);
                	
                	requestInfo = getRequestWithoutHtmlCode(new URL("http://fast-load.net/includes/captcha.php"),
                			cookie, downloadLink.getDownloadURL(), true);
                	
                	
                    if (!JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {
                    	
                        logger.severe("Captcha download failed: http://fast-load.net/includes/captcha.php");
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                        
                    } else {
                    	
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                        return step;
                        
                    }

                case PluginStep.STEP_DOWNLOAD:
                	
                	String code = (String) steps.get(1).getParameter();
                    String pid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().indexOf("pid=")+4);
                    requestInfo = postRequestWithoutHtmlCode(new URL("http://fast-load.net/download.php"),
                			cookie, downloadLink.getDownloadURL(), "fid="+pid+"&captcha_code="+code, true);

                    // Download vorbereiten
                    HTTPConnection urlConnection = requestInfo.getConnection();
                    int length = urlConnection.getContentLength();
                    
                    if ( urlConnection.getContentType() != null ) {
	                    
	                    if ( urlConnection.getContentType().contains("text/html") ) {
	                    	
	                    	if ( length == 13 ) {
	                    		
	                    		downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
	                        	step.setStatus(PluginStep.STATUS_ERROR);
	                        	return step;
	                    		
	                    	} else if ( length == 184 ) {
	                    		
	                    		logger.info("System overload: Retry in 20 seconds");
	                    		downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
	                            step.setStatus(PluginStep.STATUS_ERROR);
	                            step.setParameter(20000l);
	                            return step;
	                    		
	                    	} else {
	                    		
	                    		logger.severe("Unknown error page - [Length: "+length+"]");
	                    		downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
	                            step.setStatus(PluginStep.STATUS_ERROR);
	                            return step;
	                    		
	                    	}
	                    	
	                    }
	                    
                    } else {
                    	
                    	logger.severe("Couldn't get HTTP connection");
                    	downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    	
                    }

                    downloadLink.setDownloadMax(length);
                    downloadLink.setName(this.getFileNameFormHeader(urlConnection));

                    // Download starten
                    RAFDownload dl = new RAFDownload(this, downloadLink, urlConnection);
                    dl.setResume(true);
                    dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS,3));
                    dl.startDownload();

                    return step;

            }

            return step;

        }
        catch (IOException e) {

            e.printStackTrace();
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);

        }
        return step;

    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return "http://www.fast-load.net/infos.php";
    }

}