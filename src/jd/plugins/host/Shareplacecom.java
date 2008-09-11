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

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class Shareplacecom extends PluginForHost {

    private String postdata;
    private RequestInfo requestInfo;
    private String url;

    public Shareplacecom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://shareplace.com/rules.php";
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
            if (requestInfo.getLocation() == null) {
                downloadLink.setName(Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("File name: </b>(.*?)<b>", Pattern.CASE_INSENSITIVE)).getMatch(0)));
                String filesize = null;
                if ((filesize = new Regex(requestInfo.getHtmlCode(), "File size: </b>(.*)MB<b>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "File size: </b>(.*)KB<b>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024);
                } else if ((filesize = new Regex(requestInfo.getHtmlCode(), "File size: </b>(.*)byte<b>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)));
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

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        /* Link holen */
        url = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("document.location=\"(.*?)\";", Pattern.CASE_INSENSITIVE)).getMatch(0));

        /* Zwangswarten, 20seks */
        sleep(20000, downloadLink);

        /* Datei herunterladen */
        requestInfo = HTTP.postRequestWithoutHtmlCode(new URL(url), requestInfo.getCookie(), downloadLink.getDownloadURL(), postdata, false);
        HTTPConnection urlConnection = requestInfo.getConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setResume(false);
        dl.setChunkNum(1);
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
