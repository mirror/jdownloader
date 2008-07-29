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

public class Shareplacecom extends PluginForHost {

    private static final String HOST = "shareplace.com";
    private static final String VERSION = "1.0.0";
    private String url;
    private String postdata;
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?shareplace\\.com/\\?[a-zA-Z0-9]+/.*?", Pattern.CASE_INSENSITIVE);
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

    public Shareplacecom() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        ////steps.add(new PluginStep(PluginStep.STEP_PENDING, null));/*geht wohl auch ohne warten*/
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
                url = JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("document.location=\"(.*?)\";",Pattern.CASE_INSENSITIVE)).getFirstMatch());                
                return;
            case PluginStep.STEP_PENDING:
                /* Zwangswarten, 20seks*/
                //step.setParameter(20000l);
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
                dl.setResume(false);
                dl.setChunkNum(1);
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
            if (requestInfo.getLocation()==null) {
                downloadLink.setName(JDUtilities.htmlDecode( new Regex(requestInfo.getHtmlCode(), Pattern.compile("File name: </b>(.*?)<b>",Pattern.CASE_INSENSITIVE)).getFirstMatch()));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "File size: </b>(.*)MB<b>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                }else if ((filesize = new Regex(requestInfo.getHtmlCode(), "File size: </b>(.*)KB<b>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize))*1024);
                }else if ((filesize = new Regex(requestInfo.getHtmlCode(), "File size: </b>(.*)byte<b>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)));
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
        return "http://shareplace.com/rules.php";
    }
}

