package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.parser.HTMLParser;
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

public class Freaksharenet extends PluginForHost {

    private static final String HOST = "freakshare.net";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?freakshare\\.net/files/\\d+/(.*)", Pattern.CASE_INSENSITIVE);
    private String postdata;
    private RequestInfo requestInfo;
    private String url;

    //

    public Freaksharenet() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // //steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://freakshare.net/?x=faq";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));

            if (!requestInfo.containsHTML("<span class=\"txtbig\">Fehler</span>")) {
                ArrayList<ArrayList<String>> filename = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("colspan=\"2\" class=\"content_head\">(.*?)<b>(.*?)</b>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
                downloadLink.setName(filename.get(0).get(1));
                ArrayList<ArrayList<String>> filesize = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), Pattern.compile("<b>Datei(.*?)</b>(.*?)<td width=\"48%\" height=\"10\" align=\"left\" class=\"content_headcontent\">(.*?)(MB|KB)(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL));
                if (filesize.get(0).get(3).contains("MB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.get(0).get(2))) * 1024 * 1024);
                } else if (filesize.get(0).get(3).contains("KB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.get(0).get(2))) * 1024);
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

        // switch (step.getStep()) {
        // case PluginStep.STEP_PAGE:
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /* Link holen */
        url = requestInfo.getForms()[1].action;
        HashMap<String, String> submitvalues = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode());
        postdata = "code=" + JDUtilities.urlEncode(submitvalues.get("code"));
        postdata = postdata + "&cid=" + JDUtilities.urlEncode(submitvalues.get("cid"));
        postdata = postdata + "&userid=" + JDUtilities.urlEncode(submitvalues.get("userid"));
        postdata = postdata + "&usermd5=" + JDUtilities.urlEncode(submitvalues.get("usermd5"));
        postdata = postdata + "&wait=Download";

        // case PluginStep.STEP_PENDING:
        /* Zwangswarten, 10seks, kann man auch weglassen */
        sleep(10000, downloadLink);

        // case PluginStep.STEP_DOWNLOAD:
        /* Datei herunterladen */
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), requestInfo.getCookie(), downloadLink.getDownloadURL(), postdata, false);
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