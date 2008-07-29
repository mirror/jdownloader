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

import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;

public class Zippysharecom extends PluginForHost {

    private static final String HOST = "zippyshare.com";
    private static final String VERSION = "1.0.0";
    static private final Pattern patternSupported = Pattern.compile("http://www\\d{0,}\\.zippyshare\\.com/v/\\d+/file\\.html", Pattern.CASE_INSENSITIVE);
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

    public Zippysharecom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        //steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

     public void handle(DownloadLink downloadLink) throws Exception{ LinkStatus linkStatus=downloadLink.getLinkStatus();
        try {
          //  switch (step.getStep()) {
            //case PluginStep.STEP_PAGE:
                url = downloadLink.getDownloadURL();
                /* Nochmals das File überprüfen */
                if (!getFileInformation(downloadLink)) {
                    linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
                return;
            //case PluginStep.STEP_DOWNLOAD:
                /* Link holen */                
                String linkurl = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("downloadlink = unescape\\(\\'(.*?)\\'\\);",Pattern.CASE_INSENSITIVE)).getFirstMatch());
                /* Datei herunterladen */
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(linkurl), requestInfo.getCookie(), url.toString(), false);
                HTTPConnection urlConnection = requestInfo.getConnection();
                String filename = getFileNameFormHeader(urlConnection);
                if (urlConnection.getContentLength() == 0) {
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                    //step.setStatus(PluginStep.STATUS_ERROR);
                    return;
                }
                downloadLink.setDownloadMax(urlConnection.getContentLength());
                downloadLink.setName(filename);
                long length = downloadLink.getDownloadMax();
                dl = new RAFDownload(this, downloadLink, urlConnection);
                dl.setFilesize(length);
               dl.startDownload(); \r\n if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                    linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
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
        linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        return;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) { LinkStatus linkStatus=downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();
            for (int i = 1; i < 3; i++) {
                requestInfo = HTTP.getRequest(new URL(url));
                if (!requestInfo.containsHTML("File does not exist")) {
                    downloadLink.setName(JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<strong>Name: </strong>(.*?)</font>",Pattern.CASE_INSENSITIVE)).getFirstMatch()));
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<strong>Size: </strong>(.*?)MB</font>",Pattern.CASE_INSENSITIVE)).getFirstMatch().replaceAll(",", "\\.")) * 1024 * 1024));
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