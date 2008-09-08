//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.host;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class UploadServiceinfo extends PluginForHost {

    private String postdata;
    private RequestInfo requestInfo;
    private String url;

    public UploadServiceinfo() {
        super();
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadservice.info/rules.html";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));
            if (!requestInfo.containsHTML("<strong>Die ausgew&auml;hlte Datei existiert nicht!</strong>")) {
                downloadLink.setName(Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<input type=\"text\" value=\"(.*?)\" /></td>", Pattern.CASE_INSENSITIVE)).getMatch(0)));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "<td style=\"font-weight: bold;\">(\\d+) MB</td>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize(new Integer(filesize) * 1024 * 1024);
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
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
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
        url = requestInfo.getForms()[0].action;
        HashMap<String, String> submitvalues = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode());
        postdata = "key=" + Encoding.urlEncode(submitvalues.get("key"));
        postdata = postdata + "&mysubmit=Download";

        // case PluginStep.STEP_PENDING:
        /* Zwangswarten, 10seks, kann man auch weglassen */
        sleep(10000, downloadLink);

        // case PluginStep.STEP_DOWNLOAD:
        /* Datei herunterladen */
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), requestInfo.getCookie(), downloadLink.getDownloadURL(), postdata, false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
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
