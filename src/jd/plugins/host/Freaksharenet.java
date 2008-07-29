package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.parser.HTMLParser;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Freaksharenet extends PluginForHost {

    private static final String HOST = "freakshare.net";
    private static final String VERSION = "1.0.0";
    private String url;
    private String postdata;
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?freakshare\\.net/files/\\d+/(.*)", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;

    //
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getPluginID() {
        return HOST + "-" + VERSION;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public Freaksharenet() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // //steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle( DownloadLink downloadLink) {
        try {
          //  switch (step.getStep()) {
            case PluginStep.STEP_PAGE:
                /* Nochmals das File überprüfen */
                if (!getFileInformation(downloadLink)) {
                    downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    //step.setStatus(PluginStep.STATUS_ERROR);
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
                return;
            case PluginStep.STEP_PENDING:
                /* Zwangswarten, 10seks, kann man auch weglassen */
                //step.setParameter(10000l);
                return;
            case PluginStep.STEP_DOWNLOAD:
                /* Datei herunterladen */
                requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), requestInfo.getCookie(), downloadLink.getDownloadURL(), postdata, false);
                HTTPConnection urlConnection = requestInfo.getConnection();
                String filename = getFileNameFormHeader(urlConnection);
                if (urlConnection.getContentLength() == 0) {
                    downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
                downloadLink.setDownloadMax(urlConnection.getContentLength());
                downloadLink.setName(filename);
                long length = downloadLink.getDownloadMax();
                dl = new RAFDownload(this, downloadLink, urlConnection);
                dl.setChunkNum(1);
                dl.setResume(false);
                dl.setFilesize(length);
                if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                    downloadLink.setStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
                return;
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(LinkStatus.ERROR_UNKNOWN);
        return;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public String getAGBLink() {
        return "http://freakshare.net/?x=faq";
    }
}