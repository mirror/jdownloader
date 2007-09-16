package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;



public class Uploadedto extends PluginForHost {
    static private final Pattern PAT_SUPPORTED      = Pattern.compile("http://uploaded.to/\\?id\\=[^\\s\"]+");



    static private final String  HOST               = "uploaded.to";

    static private final String  PLUGIN_NAME        = HOST;

    static private final String  PLUGIN_VERSION     = "0";

    static private final String  PLUGIN_ID          = PLUGIN_NAME + "-" + VERSION;

    static private final String  CODER              = "coalado";

   ///Simplepattern
    static private final String DOWNLOAD_URL ="<form name=\"download_form\" method=\"post\" action=\"°\">";



    private String finalURL;

    public Uploadedto() {
       
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
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

//    @Override
//    public URLConnection getURLConnection() {
//        // XXX: ???
//        return null;
//    }

    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        RequestInfo requestInfo;
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;

            switch (step.getStep()) {
                
                case PluginStep.STEP_WAIT_TIME:
                logger.info(downloadLink.getUrlDownloadDecrypted());
                    requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()),"lang=de",null,true);

                    String url=getSimpleMatch(requestInfo.getHtmlCode(),DOWNLOAD_URL,0);
                    logger.info(url);
                    requestInfo = postRequest(new URL(url),"lang=de",null,null,null,false);
                    if(requestInfo.getConnection().getHeaderField("Location")!=null&&requestInfo.getConnection().getHeaderField("Location").indexOf("error")>0){
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;
                    }
                    if(requestInfo.getConnection().getHeaderField("Location")!=null){
                       
                       this.finalURL="http://"+ requestInfo.getConnection().getRequestProperty("host")+ requestInfo.getConnection().getHeaderField("Location");
                        
                       return step;
                    }
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN); 
                    return step;
                case PluginStep.STEP_DOWNLOAD:
                    logger.info("dl " + finalURL);
                    requestInfo=getRequestWithoutHtmlCode(new URL(finalURL),"lang=de",null,false);
                    if(requestInfo.getConnection().getHeaderField("Location")!=null&&requestInfo.getConnection().getHeaderField("Location").indexOf("error")>0){
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;
                    }
                    int length = requestInfo.getConnection().getContentLength();
                    downloadLink.setDownloadMax(length);
                    logger.info("Filename: "+getFileNameFormHeader( requestInfo.getConnection()));
                    if(getFileNameFormHeader( requestInfo.getConnection())==null||getFileNameFormHeader( requestInfo.getConnection()).indexOf("?")>=0){
                        
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step; 
                    }
                    downloadLink.setName(getFileNameFormHeader( requestInfo.getConnection()));
                    download(downloadLink, (URLConnection) requestInfo.getConnection());
                    step.setStatus(PluginStep.STATUS_DONE);
                    downloadLink.setStatus(DownloadLink.STATUS_DONE);
                 
                    return step;
            }
            return step;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Liest Content von Connection und gibt diesen als String zurück TODO:
     * auslagern
     * 
     * @param con Connection
     * @return Content
     * @throws IOException
     */
    public static String contentToString(HttpURLConnection con) throws IOException {
        InputStreamReader in = new InputStreamReader(con.getInputStream());
        StringBuffer sb = new StringBuffer();
        int chr;
        while ((chr = in.read()) != -1) {
            sb.append((char) chr);
        }
        return sb.toString();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        this.finalURL=null;

    }

    @Override
    public boolean checkAvailability(DownloadLink downloadLink) {
        // TODO Auto-generated method stub
        RequestInfo requestInfo;
        try {
            requestInfo = getRequestWithoutHtmlCode(new URL(downloadLink.getUrlDownloadDecrypted()), null, null, false);
       
     
       if(requestInfo.getConnection().getHeaderField("Location")!=null&&requestInfo.getConnection().getHeaderField("Location").indexOf("error")>0){
           return false;
       }
        return true;
        }
        catch (MalformedURLException e) {}
        catch (IOException e) {        }
       
        return false;
    }


}
