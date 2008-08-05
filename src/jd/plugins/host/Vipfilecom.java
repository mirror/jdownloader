package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class Vipfilecom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "vip-file.com";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?vip-file\\.com/download/[a-zA-z0-9]+/(.*?)\\.html", Pattern.CASE_INSENSITIVE);

    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    private static final String PLUGIN_NAME = HOST;
    private String downloadurl;
    private RequestInfo requestInfo;

    public Vipfilecom() {
        super();

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://vip-file.com/tmpl/terms.php";
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
            if (!requestInfo.containsHTML("This file not found")) {
                String linkinfo[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("Size:.*?<b style=\"padding-left:5px;\">([0-9\\.]*) (.*?)</b>", Pattern.CASE_INSENSITIVE)).getMatches();

                if (linkinfo[0][1].matches("Gb")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024 * 1024 * 1024));
                } else if (linkinfo[0][1].matches("Mb")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024 * 1024));
                } else if (linkinfo[0][1].matches("Kb")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024));
                }
                downloadLink.setName(new Regex(downloadurl, PAT_SUPPORTED).getFirstMatch());
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
        /* DownloadLink holen, 2x der Location folgen */
        String link = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"(http://vip-file\\.com/download.*?)\">", Pattern.CASE_INSENSITIVE)).getFirstMatch());
        if (link == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
        if (requestInfo.getLocation() == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(requestInfo.getLocation()), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
        if (requestInfo.getLocation() == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(requestInfo.getLocation()), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
        /* Datei herunterladen */
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
