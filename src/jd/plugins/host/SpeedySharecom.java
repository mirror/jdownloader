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
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class SpeedySharecom extends PluginForHost {

    private static final String HOST = "speedy-share.com";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?speedy\\-share\\.com/[a-zA-Z0-9]+/(.*)", Pattern.CASE_INSENSITIVE);
    private String postdata;
    RequestInfo requestInfo;
    private String url;

    //

    public SpeedySharecom() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        // steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        // steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.speedy-share.com/tos.html";
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
            if (!requestInfo.containsHTML("File Not Found")) {
                downloadLink.setName(JDUtilities.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<b>File Name\\:</b>(.*?)<br>", Pattern.CASE_INSENSITIVE)).getFirstMatch()));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "<b>File Size\\:</b>(.*)Mb<br>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "<b>File Size\\:</b>(.*)Kb<br>").getFirstMatch()) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024);
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
    /*public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
   */ public String getPluginName() {
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
        sleep(30000, downloadLink);

        // case PluginStep.STEP_DOWNLOAD:
        /* Datei herunterladen */
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), null, url, postdata, false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        String filename = Plugin.getFileNameFormHeader(urlConnection);
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
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
