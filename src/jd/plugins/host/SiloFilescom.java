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

import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class SiloFilescom extends PluginForHost {
    private static final String CODER = "JD-Team";

    private String downloadurl;
    private RequestInfo requestInfo;

    public SiloFilescom(String cfgName) {
        super(cfgName);
    }

    @Override
    public String getAGBLink() {
        return "http://silofiles.com/regeln.html";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws MalformedURLException, IOException {
        downloadurl = downloadLink.getDownloadURL();

        requestInfo = HTTP.getRequest(new URL(downloadurl));
        if (requestInfo != null && requestInfo.getLocation() == null) {
            String filename = requestInfo.getRegexp("Dateiname:<b>(.*?)</b>").getMatch(0).trim();
            String filesize;
            filesize = requestInfo.getRegexp("Dateigr.*?e:<b>(.*?)</b></tr>").getMatch(0);

            downloadLink.setDownloadSize(Regex.getSize(filesize));

            downloadLink.setName(filename);
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

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        /* Downloadlimit */
        if (requestInfo.containsHTML("<span>Maximale Parallele")) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            sleep(120000, downloadLink);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }

        /* DownloadLink holen */
        downloadurl = requestInfo.getRegexp("document.location=\"(.*?)\"").getMatch(0);

        /* Datei herunterladen */
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);
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
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
