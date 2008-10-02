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
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class MySpaceCom extends PluginForHost {
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
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {
        HTTPConnection urlConnection = br.openGetConnection(getDownloadUrl(downloadLink));
        if (!urlConnection.isOK()) {
            urlConnection.disconnect();
            return false;
        }
        downloadLink.setDownloadSize(urlConnection.getContentLength());
        urlConnection.disconnect();
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
        /* Nochmals das File Überprüfen */
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }

        dl = RAFDownload.download(downloadLink, br.createRequest(getDownloadUrl(downloadLink)), true, 0);

        dl.startDownload();
    }
}
