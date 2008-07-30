package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class SpeedySharecom extends PluginForHost {

    private static final String HOST = "speedy-share.com";
   
    private String url;
    private String postdata;
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?speedy\\-share\\.com/[a-zA-Z0-9]+/(.*)", Pattern.CASE_INSENSITIVE);
    RequestInfo requestInfo;

    //
    
    public boolean doBotCheck(File file) {
        return false;
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getPluginName() {
        return HOST;
    }

    
    public String getHost() {
        return HOST;
    }

    
    public String getVersion() {
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    
        
    

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    public SpeedySharecom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    public void handle(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        // switch (step.getStep()) {
        // case PluginStep.STEP_PAGE:
        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /* Link holen */
        HashMap<String, String> submitvalues = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode());
        postdata = "act=" + JDUtilities.urlEncode(submitvalues.get("act"));
        postdata = postdata + "&id=" + JDUtilities.urlEncode(submitvalues.get("id"));
        postdata = postdata + "&fname=" + JDUtilities.urlEncode(submitvalues.get("fname"));
        if (requestInfo.containsHTML("type=\"password\" name=\"password\"")) {
            String password = JDUtilities.getGUI().showUserInputDialog(JDLocale.L("plugins.decrypt.speedysharecom.password", "Enter Password:"));
            if (password != null && password != "") {
                postdata = postdata + "&password=" + JDUtilities.urlEncode(password);
            }
        }

        // case PluginStep.STEP_PENDING:
        /* Zwangswarten, 30seks */
        this.sleep(30000, downloadLink);

        // case PluginStep.STEP_DOWNLOAD:
        /* Datei herunterladen */
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), null, url, postdata, false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        String filename = getFileNameFormHeader(urlConnection);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        if (requestInfo.getHeaders().get("Content-Type").get(0).contains("text")) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            downloadLink.getLinkStatus().setErrorMessage("Wrong Password");
            // step.setStatus(PluginStep.STATUS_ERROR);
            // step.setParameter("Wrong Password");
            return;
        }
        downloadLink.setDownloadMax(urlConnection.getContentLength());
        downloadLink.setName(filename);
        long length = downloadLink.getDownloadMax();
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setFilesize(length);
        dl.startDownload();
    }

    
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));
            if (!requestInfo.containsHTML("File Not Found")) {
                downloadLink.setName(JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<b>File Name\\:</b>(.*?)<br>", Pattern.CASE_INSENSITIVE)).getFirstMatch()));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "<b>File Size\\:</b>(.*)Mb<br>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "<b>File Size\\:</b>(.*)Kb<br>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize)) * 1024);
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

    
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    
    public void reset() {
    }

    
    public void resetPluginGlobals() {
    }

    
    public String getAGBLink() {
        return "http://www.speedy-share.com/tos.html";
    }
}
