package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class SiloFilescom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "silofiles.com";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?silofiles\\.com/file/\\d+/.*?", Pattern.CASE_INSENSITIVE);

    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    private static final String PLUGIN_NAME = HOST;
    private String downloadurl;
    private RequestInfo requestInfo;

    public SiloFilescom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://silofiles.com/regeln.html";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        downloadurl = downloadLink.getDownloadURL();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (requestInfo != null && requestInfo.getLocation() == null) {
                String filename = requestInfo.getRegexp("Dateiname:<b>(.*?)</b>").getFirstMatch();
                String filesize;
                if ((filesize = requestInfo.getRegexp("Dateigröße:<b>(.*?)MB</b>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024 * 1024));
                } else if ((filesize = requestInfo.getRegexp("Dateigröße:<b>(.*?)KB</b>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize) * 1024));
                }
                downloadLink.setName(filename);
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
        return 1;
    }

    @Override
   */ public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        /* Downloadlimit */
        if (requestInfo.containsHTML("<span>Maximale Parallele")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            sleep(120000, downloadLink);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }

        /* DownloadLink holen */
        downloadurl = requestInfo.getRegexp("document.location=\"(.*?)\"").getFirstMatch();

        /* Datei herunterladen */
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
