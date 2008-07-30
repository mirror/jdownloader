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

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?mooshare\\.de/index\\.php\\?pid\\=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    private static final String PLUGIN_NAME = HOST;
    private String downloadurl;
    private RequestInfo requestInfo;

    public Moosharede() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.mooshare.de/infos.php";
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

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getPluginName() {
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
    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
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

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
