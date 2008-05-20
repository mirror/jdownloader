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
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

/**
 * HostPlugin für gullishare TODO: Erzwungene Wartezeit (gibt es die überhaupt
 * noch?)
 */
public class Gulli extends PluginForHost {
    //http://share.gulli.com/files/819611887
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://share\\.gulli\\.com/files/[\\d]+.*", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAT_CAPTCHA        = Pattern.compile("<img src=\"(/captcha[^\"]*)");

    static private final Pattern PAT_FILE_ID        = Pattern.compile("<input type=\"hidden\" name=\"file\" value=\"([^\"]*)");

    static private final Pattern PAT_DOWNLOAD_URL   = Pattern.compile("<form action=\"/(download[^\"]*)");
    static private final String PAT_DOWNLOAD_SIZE_MB = "div id=\"share_download\">°<h1>° (° MB)</h1>";
    private static final String PAT_DOWNLOAD_SIZE_B = "div id=\"share_download\">°<h1>° (° B)</h1>";

    private static final String PAT_DOWNLOAD_SIZE_KB = "div id=\"share_download\">°<h1>° (° KB)</h1>";

    static private final Pattern PAT_DOWNLOAD_LIMIT = Pattern.compile("timeLeft=([^\"]*)&");

    static private final Pattern PAT_DOWNLOAD_ERROR = Pattern.compile("share.gulli.com/error([^\"]*)");

    static private final String  HOST_URL           = "http://share.gulli.com/";

    static private final String  DOWNLOAD_URL       = "http://share.gulli.com/download";

    static private final String  HOST               = "share.gulli.com";

    static private final String  PLUGIN_NAME        = HOST;

    static private final String  PLUGIN_VERSION     = "0";

    static private final String  PLUGIN_ID          = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final String  CODER              = "JD-Team";



    /**
     * ID des Files bei gulli
     */
    private String               fileId;

    private String               cookie;

    private String               finalDownloadURL;

    private HTTPConnection    finalDownloadConnection;

    //private boolean              serverIPChecked;

    public Gulli() {
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
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

    // @Override
    // public URLConnection getURLConnection() {
    // // XXX: ???
    // return null;
    // }
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        RequestInfo requestInfo = null;
        String dlUrl=null;
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;
            String name = downloadLink.getName();
            if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
            downloadLink.setName(name);
            switch (step.getStep()) {
                case PluginStep.STEP_GET_CAPTCHA_FILE:
                    // con.setRequestProperty("Cookie",
                    // Plugin.joinMap(cookieMap,"=","; "));
                    requestInfo = getRequest(new URL(downloadLink.getDownloadURL()));
                    fileId = getFirstMatch(requestInfo.getHtmlCode(), PAT_FILE_ID, 1);
                    String captchaLocalUrl = getFirstMatch(requestInfo.getHtmlCode(), PAT_CAPTCHA, 1);
                    dlUrl = getFirstMatch(requestInfo.getHtmlCode(), PAT_DOWNLOAD_URL, 1);
                  

                    if (captchaLocalUrl == null) {

                        logger.severe("Download for your Ip Temp. not available");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        step.setParameter(30 * 1000l);
                        return step;
                    }
                    else {
                        cookie = requestInfo.getCookie();
                        logger.info(cookie);
                        logger.finest("Captcha Page");
                        String captchaUrl = "http://share.gulli.com" + captchaLocalUrl;
                        File file = this.getLocalCaptchaFile(this);
                        if (!JDUtilities.download(file, captchaUrl) || !file.exists()) {
                            logger.severe("Captcha Download fehlgeschlagen: " + captchaUrl);
                            step.setParameter(null);
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                            break;
                        }
                        else {
                            step.setParameter(file);
                            step.setStatus(PluginStep.STATUS_USER_INPUT);
                        }
                        return step;

                    }
                case PluginStep.STEP_WAIT_TIME:
                   
                        String captchaTxt = (String) steps.get(0).getParameter();

                        logger.info("file=" + fileId + "&" + "captcha=" + captchaTxt);
                        requestInfo = postRequest(new URL(DOWNLOAD_URL), cookie, null, null, "file=" + fileId + "&" + "captcha=" + captchaTxt, true);
                    
                    dlUrl = getFirstMatch(requestInfo.getHtmlCode(), PAT_DOWNLOAD_URL, 1);
                    
                    if (dlUrl == null) {
                        logger.finest("Error Page");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        return step;
                    }
                    logger.info(dlUrl);
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                    }
                    requestInfo = postRequestWithoutHtmlCode(new URL(HOST_URL + dlUrl), cookie, null, "action=download&file=" + fileId, false);
                    String red;
                    String waittime = null;
                    String error = null;
                    String url = HOST_URL + dlUrl;
                    // Redirect folgen und dabei die Cookies weitergeben
                    // share.gulli.com/error
                    while ((red = requestInfo.getConnection().getHeaderField("Location")) != null && (waittime = getFirstMatch(red, PAT_DOWNLOAD_LIMIT, 1)) == null && (error = getFirstMatch(red, PAT_DOWNLOAD_ERROR, 1)) == null) {
                        logger.info("red: " + red + " cookie: " + cookie);
                        url = red;
                        requestInfo = getRequestWithoutHtmlCode(new URL(red), cookie, null, false);
                    }
                    logger.info("abbruch bei :" + red);
                    if (waittime != null) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        step.setParameter(Long.parseLong(waittime) * 60 * 1000);
                        logger.info("Warten " + (Long) step.getParameter() + " - " + waittime);
                        logger.info(step.toString());
                    }
                    else if (error != null) {
                        logger.info("Error: " + error);
                        if (error.indexOf("ticket") > 0) {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                            try {
                                Thread.sleep(3000);
                            }
                            catch (InterruptedException e) {
                            }
                        }
                        else {
                            step.setStatus(PluginStep.STATUS_ERROR);
                            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        }
                    }
                    else {
                        logger.info("URL: " + url);
                        finalDownloadURL = url;
                        finalDownloadConnection = requestInfo.getConnection();
                    }
                    return step;
                case PluginStep.STEP_DOWNLOAD:
                    logger.info("dl " + finalDownloadURL);
                    int length = finalDownloadConnection.getContentLength();
                    downloadLink.setDownloadMax(length);
                    if (!hasEnoughHDSpace(downloadLink)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    
                   dl = new RAFDownload(this, downloadLink, finalDownloadConnection);
                    dl.startDownload();
                    
               
                        
                
                    return step;
            }
            return step;
        }
        catch (IOException e) {
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
        fileId = null;
        cookie = null;
        finalDownloadURL = null;
        finalDownloadConnection = null;
        //serverIPChecked = false;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        String name = downloadLink.getName();
        if (name.toLowerCase().matches(".*\\..{1,5}\\.html$")) name = name.replaceFirst("\\.html$", "");
        downloadLink.setName(name);
        try {
            requestInfo = getRequestWithoutHtmlCode(new URL(downloadLink.getDownloadURL()), null, null, false);
            if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                return false;
            }
            requestInfo=readFromURL(requestInfo.getConnection());
          
            int filesize=0;
            String size;
            size=getSimpleMatch(requestInfo.getHtmlCode(), PAT_DOWNLOAD_SIZE_B, 2);  
            
            if(size!=null)filesize=(int)(Double.parseDouble(size));
           
            if(size==null){
               
                size=getSimpleMatch(requestInfo.getHtmlCode(), PAT_DOWNLOAD_SIZE_KB, 2); 
               
                if(size!=null)filesize=(int)(Double.parseDouble(size.replaceAll(",", "."))*1024);
            }
            if(size==null){
                size=getSimpleMatch(requestInfo.getHtmlCode(), PAT_DOWNLOAD_SIZE_MB, 2);
            
                if(size!=null)filesize=(int)(Double.parseDouble(size.replaceAll(",", "."))*1024*1024);
              
            }
          if(filesize>0)downloadLink.setDownloadMax(filesize);
               
            return true;
        }
        catch (MalformedURLException e) {
        }
        catch (IOException e) {
        }
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        if(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)&& this.getProperties().getBooleanProperty(PROPERTY_USE_PREMIUM, false)){
            return 20;
        }else{
            return 1;
        }
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://share.gulli.com/faq";
    }
}
