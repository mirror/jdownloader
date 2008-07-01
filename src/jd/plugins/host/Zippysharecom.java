package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Zippysharecom extends PluginForHost {

    private static final String HOST = "zippyshare.com";
    private static final String VERSION = "1.0.0";
    static private final Pattern patternSupported = Pattern.compile("http://www\\d{0,}\\.zippyshare\\.com/v/\\d+/file\\.html", Pattern.CASE_INSENSITIVE);

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

    public Zippysharecom() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {
            String url = downloadLink.getDownloadURL();
            /* Nochmals das File überprüfen */
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            /* Link holen */
            RequestInfo requestInfo = HTTP.getRequest(new URL(url));
            String linkurl = JDUtilities.htmlDecode(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "downloadlink = unescape\\(\\'", "\\'\\);"));
            /* Datei herunterladen */
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(linkurl), requestInfo.getCookie(), url.toString(), false);
            HTTPConnection urlConnection = requestInfo.getConnection();
            String filename = JDUtilities.htmlDecode(new Regex(requestInfo.getHeaders().get("Content-Disposition").get(0), "attachment; filename=(.*)").getFirstMatch());
            downloadLink.setDownloadMax(urlConnection.getContentLength());
            downloadLink.setStaticFileName(filename);
            downloadLink.setName(filename);
            final long length = downloadLink.getDownloadMax();
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                step.setStatus(PluginStep.STATUS_ERROR);
                return step;
            }
            return step;

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return step;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String url = downloadLink.getDownloadURL();
            for (int i = 1; i < 3; i++) {
                requestInfo = HTTP.getRequest(new URL(url));
                if (!requestInfo.containsHTML("File does not exist")) {
                    downloadLink.setName(JDUtilities.htmlDecode(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "<strong>Name: </strong>", "</font>")));
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "<strong>Size: </strong>", "MB</font>").replaceAll(",", "\\.")) * 1024 * 1024));
                    return true;
                }
                Thread.sleep(250);
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
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
        return "http://www.zippyshare.com/terms.html";
    }

}