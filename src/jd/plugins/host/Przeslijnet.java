package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Przeslijnet extends PluginForHost {

    private static final String HOST = "przeslij.net";
    private static final String VERSION = "1.0.0";
    static private final Pattern patternSupported = Pattern.compile("http://www2\\.przeslij\\.net/download.php\\?file=(.*)", Pattern.CASE_INSENSITIVE);
    private String url;
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

    public Przeslijnet() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        //steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle( DownloadLink downloadLink) {
        try {
          //  switch (step.getStep()) {
            case PluginStep.STEP_PAGE:
                url = downloadLink.getDownloadURL();
                /* Nochmals das File überprüfen */
                if (!getFileInformation(downloadLink)) {
                    downloadLink.setStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }                
                return;
            case PluginStep.STEP_PENDING:
                /* Zwangswarten, 15seks */
                //step.setParameter(15000l);
                return;
            case PluginStep.STEP_DOWNLOAD:
                /* Link holen */
                String linkurl = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), "onClick=\"window\\.location=\\\\\'(.*?)\\\\\'").getFirstMatch());
                /* Datei herunterladen */
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(linkurl), requestInfo.getCookie(), url.toString(), false);
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
            if (!requestInfo.containsHTML("Invalid download link")) {
                downloadLink.setName(JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<font color=#000000>(.*?)</font>",Pattern.CASE_INSENSITIVE)).getFirstMatch()));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*)MB</td>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*)KB</td>").getFirstMatch()) != null) {
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
        return "hhttp://www2.przeslij.net/#";
    }
}
