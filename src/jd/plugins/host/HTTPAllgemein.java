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

public class HTTPAllgemein extends PluginForHost {

    private static final String HOST = "HTTP Links";

    static private final Pattern patternSupported = Pattern.compile("httpviajd://[\\w\\.]*/.*?(zip|mp3|mp4|avi|iso|mov|wmv|mpg|rar|mp2|7z|pdf|flv|jpg|exe|3gp|wav|mkv|tar|bz2)", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String linkurl;
    private LinkStatus linkStatus;

    public HTTPAllgemein() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        linkStatus = downloadLink.getLinkStatus();
        linkurl = downloadLink.getDownloadURL().replaceAll("httpviajd://", "http://");
        try {
            if (linkurl != null) {
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(linkurl), null, null, true);
                HTTPConnection urlConnection = requestInfo.getConnection();
                downloadLink.setName(getFileNameFormHeader(urlConnection));
                downloadLink.setBrowserUrl(linkurl);
                downloadLink.setDownloadMax(urlConnection.getContentLength());
                return true;
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
        return false;

    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2070 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        // switch (step.getStep()) {
        // case PluginStep.STEP_PAGE:

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(linkurl), null, null, true);
        HTTPConnection urlConnection = requestInfo.getConnection();        
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

    @Override
    public String getHost() {
        // TODO Auto-generated method stub
        return HOST;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        // TODO Auto-generated method stub

    }

}