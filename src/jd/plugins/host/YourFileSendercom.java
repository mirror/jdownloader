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

public class YourFileSendercom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private String downloadurl;
    private RequestInfo requestInfo;

    public YourFileSendercom() {
        super();
    }

    @Override
    public String getAGBLink() {
        return "http://www.yourfilesender.com/terms.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        downloadurl = downloadLink.getDownloadURL();
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (!requestInfo.containsHTML("alert('File Not Found")) {
                String linkinfo[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<P>You have requested the file <strong>(.*?)</strong> \\(([0-9\\.,]+) (.*?)\\)\\.<br", Pattern.CASE_INSENSITIVE)).getMatches();

                if (linkinfo[0][2].matches("KBytes")) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][1].replaceAll(",", "").replaceAll("\\.0+", "")) * 1024.0));
                }
                downloadLink.setName(linkinfo[0][0]);
                return true;
            }
        } catch (Exception e) {

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
        if (requestInfo.containsHTML("<span>You have got max allowed download sessions from the same IP!</span>")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            // step.setParameter(60 * 60 * 1000L);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }

        String link = Encoding.htmlDecode(new Regex(requestInfo.getHtmlCode(), Pattern.compile("unescape\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        /* 10 Seks warten */
        sleep(10000, downloadLink);
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
        if (requestInfo.getLocation() != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        /* Datei herunterladen */
        HTTPConnection urlConnection = requestInfo.getConnection();
        // String filename = Plugin.getFileNameFormHeader(urlConnection);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setFilesizeCheck(false);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert prüfen */
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
