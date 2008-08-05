package jd.plugins.host;

import java.io.File;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
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

        try {
            Browser.clearCookies(HOST);
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() == null) {
                String filename = br.getRegex("<center>Dateiname: <b>(.*?)</b>").getFirstMatch();
                if (filename != "") {
                    downloadLink.setName(filename);
                    return true;
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

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

        /* DownloadLink holen */
        String url = br.getRegex("popup\\('(.*?)',").getFirstMatch();
        br.openGetConnection(url);

        /* Datei herunterladen */

        HTTPConnection urlConnection = br.openGetConnection(null);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
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
