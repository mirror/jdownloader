//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class MySpaceCom extends PluginForHost {
    // private static final Pattern PATTERN_SUPPORTET = Pattern.compile(
    // "http://cache\\d+-music\\d+.myspacecdn\\.com/\\d+/std_.+\\.mp3"
    // ,Pattern.CASE_INSENSITIVE);
    private static final String CODER = "ToKaM";

    private static final String AGB_LINK = "http://www.myspace.com/index.cfm?fuseaction=misc.terms";

    public MySpaceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String getDownloadUrl(DownloadLink link) {
        return link.getDownloadURL().replaceAll("myspace://", "");
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        HTTPConnection urlConnection = br.openGetConnection(getDownloadUrl(downloadLink));
        if (!urlConnection.isOK()) return false;
        downloadLink.setDownloadSize(urlConnection.getContentLength());
        return true;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        downloadLink.setUrlDownload(getDownloadUrl(downloadLink));
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File Überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        HTTPConnection urlConnection = br.openGetConnection(getDownloadUrl(downloadLink));

        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }

        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 2));
        dl.setResume(true);
        dl.startDownload();
    }
}
