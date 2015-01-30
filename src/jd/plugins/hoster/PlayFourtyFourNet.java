//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "play44.net" }, urls = { "http://(www\\.)?play44\\.net/embed\\.php\\?.+|http://gateway\\d*\\.play44\\.net/(?:at|videos)/.+" }, flags = { 0 })
public class PlayFourtyFourNet extends antiDDoSForHost {

    // raztoki embed video player template.

    private String dllink = null;

    public PlayFourtyFourNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.play44.net";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        // Offline links should also have nice filenames
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "play44\\.net/embed\\.php\\?(.+)").getMatch(0));
        this.setBrowserExclusive();
        final String link = downloadLink.getDownloadURL();
        URLConnectionAdapter con = null;
        if (link.matches(".+://gateway\\d*\\.play44\\.net/.+")) {
            // In case the link are directlinks! current cloudflare implementation will actually open them!
            br.setFollowRedirects(true);
            try {
                if (isNewJD()) {
                    con = br.openHeadConnection(link);
                    if (!con.getContentType().contains("html")) {
                        // is file
                        downloadLink.setFinalFileName(getFileNameFromHeader(con));
                        downloadLink.setDownloadSize(con.getLongContentLength());
                        return AvailableStatus.TRUE;
                    } else {
                        // is html
                        con = br.openGetConnection(link);
                        br.followConnection();
                    }
                } else {
                    con = br.openGetConnection(link);
                    if (!con.getContentType().contains("html")) {
                        // is file
                        downloadLink.setFinalFileName(getFileNameFromHeader(con));
                        downloadLink.setDownloadSize(con.getLongContentLength());
                        return AvailableStatus.TRUE;
                    } else {
                        // is html
                        br.followConnection();
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            // standard links which are like gogoanime type of embed links. though these seem to always return gateway.play44.net so safe to
            // keep here.
            getPage(link);
        }
        // only way to check for made up links... or offline is here
        final int rc = (br.getHttpConnection() != null ? br.getHttpConnection().getResponseCode() : -1);
        if (rc == 403 || rc == 404 || rc == -1) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = br.getRegex("playlist:.*?url: \\'(http[^']+play44\\.net[^']+)\\'").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.urlDecode(dllink, false);
        }
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        con = null;
        try {
            if (isNewJD()) {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    // is file
                    downloadLink.setFinalFileName(getFileNameFromHeader(con));
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    return AvailableStatus.TRUE;
                } else {
                    // is html
                }
            } else {
                con = br.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    // is file
                    downloadLink.setFinalFileName(getFileNameFromHeader(con));
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    return AvailableStatus.TRUE;
                } else {
                    // is html
                }
            }
            // only way to check for made up links... or offline is here
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}