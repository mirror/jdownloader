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

import java.io.File;
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

public class Freaksharenet extends PluginForHost {

    private static final String HOST = "freakshare.net";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?freakshare\\.net/files/\\d+/(.*)", Pattern.CASE_INSENSITIVE);
    private String postdata;
    private RequestInfo requestInfo;
    private String url;

    public Freaksharenet() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://freakshare.net/?x=faq";
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

            if (!requestInfo.containsHTML("<span class=\"txtbig\">Fehler</span>")) {
                String[][] filename = new Regex(requestInfo.getHtmlCode(), Pattern.compile("colspan=\"2\" class=\"content_head\">(.*?)<b>(.*?)</b>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
                downloadLink.setName(filename[0][1]);
                String[][] filesize = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<b>Datei(.*?)</b>(.*?)<td width=\"48%\" height=\"10\" align=\"left\" class=\"content_headcontent\">(.*?)(MB|KB)(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatches();
                if (filesize[0][3].contains("MB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize[0][2])) * 1024 * 1024);
                } else if (filesize[0][3].contains("KB")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize[0][2])) * 1024);
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
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        /* Link holen */
        url = requestInfo.getForms()[1].action;
        HashMap<String, String> submitvalues = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode());
        postdata = "code=" + Encoding.urlEncode(submitvalues.get("code"));
        postdata = postdata + "&cid=" + Encoding.urlEncode(submitvalues.get("cid"));
        postdata = postdata + "&userid=" + Encoding.urlEncode(submitvalues.get("userid"));
        postdata = postdata + "&usermd5=" + Encoding.urlEncode(submitvalues.get("usermd5"));
        postdata = postdata + "&wait=Download";

        /* Zwangswarten, 10seks, kann man auch weglassen */
        sleep(10000, downloadLink);

        /* Datei herunterladen */
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), requestInfo.getCookie(), downloadLink.getDownloadURL(), postdata, false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}