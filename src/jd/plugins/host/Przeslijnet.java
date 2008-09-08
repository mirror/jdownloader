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
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class Przeslijnet extends PluginForHost {

    private RequestInfo requestInfo;
    private String url;

    public Przeslijnet() {
        super();
    }

    @Override
    public String getAGBLink() {
        return "hhttp://www2.przeslij.net/#";
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
            if (!requestInfo.containsHTML("Invalid download link")) {
                downloadLink.setName(Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("<font color=#000000>(.*?)</font>", Pattern.CASE_INSENSITIVE)).getMatch(0)));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*)MB</td>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "File Size:</td><td bgcolor=\\#EEF4FB background=\"img\\/button03.gif\"><font color=#000080>(.*)KB</td>").getMatch(0)) != null) {
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

        /* Zwangswarten, 15seks */
        sleep(15000, downloadLink);

        /* Link holen */
        String linkurl = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), "onClick=\"window\\.location=\\\\\'(.*?)\\\\\'").getMatch(0));
        /* Datei herunterladen */
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(linkurl), requestInfo.getCookie(), url.toString(), false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
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
