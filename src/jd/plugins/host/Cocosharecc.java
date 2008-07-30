package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Cocosharecc extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "cocoshare.cc";

    private static final String PLUGIN_NAME = HOST;

    //private static final String new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    //private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?cocoshare\\.cc/\\d+/(.*)", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String downloadurl;

    public Cocosharecc() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }

    
    public String getCoder() {
        return CODER;
    }

    
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    
    public String getHost() {
        return HOST;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    
    
        
   

    
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    
    public void reset() {
    }

    
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            downloadurl = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (requestInfo.containsHTML("Download startet automatisch")) {
                String filename = requestInfo.getRegexp("<h1>(.*?)</h1>").getFirstMatch();
                String filesize;
                if ((filesize = requestInfo.getRegexp("Dateigr&ouml;sse:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(.*?)Bytes<br").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax(new Integer(filesize.trim().replaceAll("\\.", "")));
                }
                downloadLink.setName(filename);
                return true;
            }
        } catch (MalformedURLException e) {
            
            e.printStackTrace();
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    public void handle(DownloadLink downloadLink) throws Exception {

        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        /* Warten */
        String waittime = requestInfo.getRegexp("var num_timeout = (\\d+);").getFirstMatch();
        if (waittime != null) {
            this.sleep(new Integer(waittime.trim()) * 1000, downloadLink);
        }

        /* DownloadLink holen */
        downloadurl = "http://www.cocoshare.cc" + requestInfo.getRegexp("<meta http-equiv=\"refresh\" content=\"\\d+; URL=(.*?)\"").getFirstMatch();
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);
        downloadurl = requestInfo.getLocation();
        if (downloadurl == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        downloadurl = "http://www.cocoshare.cc" + downloadurl;
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);

        /* DownloadLimit? */
        if (requestInfo.getLocation() != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            // step.setParameter(120000L);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }

        /* Datei herunterladen */
        HTTPConnection urlConnection = requestInfo.getConnection();
        String filename = getFileNameFormHeader(urlConnection);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        downloadLink.setDownloadMax(urlConnection.getContentLength());
        downloadLink.setName(filename);
        long length = downloadLink.getDownloadMax();
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setFilesize(length);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        dl.startDownload();
    }

    
    public void resetPluginGlobals() {
       
    }

    
    public String getAGBLink() {
        return "http://www.cocoshare.cc/imprint";
    }
}
