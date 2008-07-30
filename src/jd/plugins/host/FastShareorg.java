package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class FastShareorg extends PluginForHost {

    private static final String HOST = "fastshare.org";
   
    private String url;
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?fastshare\\.org/download/(.*)", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;

    //
    
    public boolean doBotCheck(File file) {
        return false;
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getPluginName() {
        return HOST;
    }

    
    public String getHost() {
        return HOST;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    
    
        
   

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public FastShareorg() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /* Link holen */
        Form form = requestInfo.getForms()[0];
        requestInfo = form.getRequestInfo();
        if ((url = new Regex(requestInfo.getHtmlCode(), "Link: <a href=(.*)><b>").getFirstMatch()) == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // case PluginStep.STEP_PENDING:
        /* Zwangswarten, 10seks */
        // step.setParameter(10000l);
        this.sleep(10000, downloadLink);

        // case PluginStep.STEP_DOWNLOAD:
        /* Datei herunterladen */
        // requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url),
        // null, url, postdata, false);
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(url), null, url, false);
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
        dl.startDownload();

    }

    
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));
            if (!requestInfo.containsHTML("No filename specified or the file has been deleted")) {
                downloadLink.setName(JDUtilities.htmlDecode(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "Wenn sie die Datei \"<b>", "</b>\"")));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "<i>\\((.*)MB\\)</i>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "<i>\\((.*)KB\\)</i>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024);
                }
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

    
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    
    public void reset() {
    }

    
    public void resetPluginGlobals() {
    }

    
    public String getAGBLink() {
        return "http://www.fastshare.org/discl.php";
    }
}
