package jd.plugins.host;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.http.PostRequest;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDLocale;

public class SpeedySharecom extends PluginForHost {

    private String postdata;
    RequestInfo requestInfo;
    private String url;

    public SpeedySharecom(PluginWrapper wrapper) {
        super(wrapper);
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
    public boolean getFileInformation(DownloadLink downloadLink) throws MalformedURLException, IOException {

        String url = downloadLink.getDownloadURL();
        requestInfo = HTTP.getRequest(new URL(url));
        if (!requestInfo.containsHTML("File Not Found")) {
            downloadLink.setName(Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("File Name:</span>(.*?)</span>", Pattern.CASE_INSENSITIVE)).getMatch(0)));
            String filesize = null;
            filesize = new Regex(requestInfo.getHtmlCode(), "File Size:</span>(.*?)</span>").getMatch(0);
            downloadLink.setDownloadSize(Regex.getSize(filesize));
            return true;
        }
        return false;

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        url = downloadLink.getDownloadURL();
        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* Link holen */
        HashMap<String, String> submitvalues = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode());
        postdata = "act=" + Encoding.urlEncode(submitvalues.get("act"));
        postdata = postdata + "&id=" + Encoding.urlEncode(submitvalues.get("id"));
        postdata = postdata + "&fname=" + Encoding.urlEncode(submitvalues.get("fname"));
        if (requestInfo.containsHTML("type=\"password\" name=\"password\"")) {
            String password = Plugin.getUserInput(JDLocale.L("plugins.decrypt.speedysharecom.password", "Enter Password:"), downloadLink);
            if (password != null && password != "") {
                postdata = postdata + "&password=" + Encoding.urlEncode(password);
            }
        }

        /* Zwangswarten, 30seks */
        sleep(30000, downloadLink);

        /* Datei herunterladen */
        // requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), null,
        // url, postdata, false);
        PostRequest request = new PostRequest(url);

        request.setPostDataString(postdata);
        dl = RAFDownload.download(downloadLink, request);

        HTTPConnection urlConnection = dl.connect();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        if (request.getResponseHeader("Content-Type").contains("text")) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            downloadLink.getLinkStatus().setErrorMessage("Wrong Password");
            return;
        }

        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
