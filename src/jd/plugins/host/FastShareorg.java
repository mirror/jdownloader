package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class FastShareorg extends PluginForHost {

    private static final String HOST = "fastshare.org";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?fastshare\\.org/download/(.*)", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String url;

    //

    public FastShareorg() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fastshare.org/discl.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
     
        try {
            Browser.clearCookies(HOST);
            br.setFollowRedirects(false);
            String url = downloadLink.getDownloadURL();
           br.getPage(url);
            if (!br.containsHTML("No filename specified or the file has been deleted")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("Wenn sie die Datei \"<b>(.*?)<\\/b>\"").getFirstMatch()));
                String filesize = null;
                if ((filesize = br.getRegex( "<i>\\((.*)MB\\)</i>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = br.getRegex( "<i>\\((.*)KB\\)</i>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024);
                }
                return true;
            }
     
        } catch (Exception e) {

            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    /*public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
   */ public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        url = downloadLink.getDownloadURL();
        
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /* Link holen */
        Form form = br.getForm(0);
        br.submitForm(form);
        if ((url = new Regex(br, "Link: <a href=(.*)><b>").getFirstMatch()) == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        // case PluginStep.STEP_PENDING:
        /* Zwangswarten, 10seks */
        // step.setParameter(10000l);
        sleep(10000, downloadLink);

        // case PluginStep.STEP_DOWNLOAD:
        /* Datei herunterladen */
        // requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url),
        // null, url, postdata, false);
       
        
        HTTPConnection urlConnection = br.openGetConnection(url);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
