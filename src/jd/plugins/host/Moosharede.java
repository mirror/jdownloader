package jd.plugins.host;

import java.io.File;
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

public class Moosharede extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "mooshare.de";

    private static final String PLUGIN_NAME = HOST;

    //private static final String new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    //private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?mooshare\\.de/index\\.php\\?pid\\=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String downloadurl;

    public Moosharede() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
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
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    
        
   

    
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    
    public void reset() {
    }

    
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    
    public boolean getFileInformation(DownloadLink downloadLink) { LinkStatus linkStatus=downloadLink.getLinkStatus();
        downloadurl = downloadLink.getDownloadURL();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (requestInfo != null && requestInfo.getLocation() == null) {
                String filename = requestInfo.getRegexp("<center>Dateiname: <b>(.*?)</b>").getFirstMatch();
                if (filename != "") {
                    downloadLink.setName(filename);
                    return true;
                }
            }
        } catch (Exception e) {
            
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

     public void handle(DownloadLink downloadLink) throws Exception{ LinkStatus linkStatus=downloadLink.getLinkStatus();
        
    
            /* Nochmals das File überprüfen */
            if (!getFileInformation(downloadLink)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }

            /* DownloadLink holen */
            downloadurl = requestInfo.getRegexp("popup\\('(.*?)',").getFirstMatch();
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);
            downloadurl = "http://" + requestInfo.getConnection().getURL().getHost() + requestInfo.getLocation();

            /* Datei herunterladen */
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);
            HTTPConnection urlConnection = requestInfo.getConnection();
            String filename = getFileNameFormHeader(urlConnection);
            if (urlConnection.getContentLength() == 0) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                //step.setStatus(PluginStep.STATUS_ERROR);
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
        return "http://www.mooshare.de/infos.php";
    }

}
