package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class HTTPAllgemein extends PluginForHost {

    private static final String HOST = "HTTP Links";

    static private final Pattern patternSupported = Pattern.compile("httpviajd://[\\w\\.]*/.*?\\.(dlc|ccf|rsdf|zip|mp3|mp4|avi|iso|mov|wmv|mpg|rar|mp2|7z|pdf|flv|jpg|exe|3gp|wav|mkv|tar|bz2)", Pattern.CASE_INSENSITIVE);

    private String contentType;

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

    public String getFileInformationString(DownloadLink parameter) {
        return "(" + contentType + ")" + parameter.getName();
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        String linkurl;
        downloadLink.setUrlDownload(linkurl = downloadLink.getDownloadURL().replaceAll("httpviajd://", "http://"));

        if (linkurl != null) {
            br.setFollowRedirects(true);

            HTTPConnection urlConnection;
            try {
                urlConnection = br.openGetConnection(linkurl);

                if (!urlConnection.isOK()) return false;
                downloadLink.setName(Plugin.getFileNameFormHeader(urlConnection));
                downloadLink.setBrowserUrl(linkurl);
                downloadLink.setDownloadSize(urlConnection.getContentLength());
                this.contentType = urlConnection.getContentType();
                return true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

            }
        }

        linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
        return false;

    }

    @Override
    /*
     * public int getMaxSimultanDownloadNum() { return Integer.MAX_VALUE; }
     * 
     * @Override
     */public String getPluginName() {
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
        HTTPConnection urlConnection = br.openGetConnection(downloadLink.getDownloadURL());

        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
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