package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import jd.parser.Form;
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

public class FastShareorg extends PluginForHost {

    private static final String HOST = "fastshare.org";
    private static final String VERSION = "1.0.0";
    private String url;
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?fastshare\\.org/download/(.*)", Pattern.CASE_INSENSITIVE);
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

    public FastShareorg() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {
            switch (step.getStep()) {
            case PluginStep.STEP_PAGE:
                url = downloadLink.getDownloadURL();
                /* Nochmals das File überprüfen */
                if (!getFileInformation(downloadLink)) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                /* Link holen */
                requestInfo = HTTP.getRequest(new URL(url));
                Form form = requestInfo.getForms()[0];
                requestInfo = form.getRequestInfo();
                if ((url = new Regex(requestInfo.getHtmlCode(), "Link: <a href=(.*)><b>").getFirstMatch()) == null) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                return step;
            case PluginStep.STEP_PENDING:
                /* Zwangswarten, 10seks */
                step.setParameter(10000l);
                return step;
            case PluginStep.STEP_DOWNLOAD:
                /* Datei herunterladen */
                // requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url),
                // null, url, postdata, false);
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(url), null, url, false);
                HTTPConnection urlConnection = requestInfo.getConnection();
                String filename = getFileNameFormHeader(urlConnection);
                if (urlConnection.getContentLength() == 0) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    step.setStatus(PluginStep.STATUS_RETRY);
                    return step;
                }
                downloadLink.setDownloadMax(urlConnection.getContentLength());
                downloadLink.setName(filename);
                long length = downloadLink.getDownloadMax();
                dl = new RAFDownload(this, downloadLink, urlConnection);
                dl.setFilesize(length);
                if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                    step.setStatus(PluginStep.STATUS_ERROR);
                    return step;
                }
                return step;
            }
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
            requestInfo = HTTP.getRequest(new URL(url));
            if (!requestInfo.containsHTML("No filename specified or the file has been deleted")) {
                downloadLink.setName(JDUtilities.htmlDecode(SimpleMatches.getBetween(requestInfo.getHtmlCode(), "Wenn sie die Datei \"<b>", "</b>\"")));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "<i>\\((.*)MB\\)</i>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "<i>\\((.*)KB\\)</i>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024);
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
        return "http://www.fastshare.org/discl.php";
    }
}
