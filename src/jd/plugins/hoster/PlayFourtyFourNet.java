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

import java.io.IOException;

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

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "play44.net" }, urls = { "http://(www\\.)?play44\\.net/embed\\.php\\?.+|http://gateway\\d*\\.play44\\.net/(?:at|videos)/.+" }, flags = { 0 })
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

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        // Offline links should also have nice filenames
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "play44\\.net/embed\\.php\\?(.+)").getMatch(0));
        this.setBrowserExclusive();
        dllink = downloadLink.getDownloadURL();
        URLConnectionAdapter con = null;
        if (dllink.matches(".+://gateway\\d*\\.play44\\.net/.+")) {
            // In case the link are directlinks! current cloudflare implementation will actually open them!
            br.setFollowRedirects(true);
            try {
                con = getConnection(br, downloadLink);
                if (!con.getContentType().contains("html")) {
                    // is file
                    downloadLink.setFinalFileName(getFileNameFromHeader(con));
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    return AvailableStatus.TRUE;
                } else {
                    // is html
                    con = br.openGetConnection(dllink);
                    br.followConnection();
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
            getPage(dllink);
        }
        // only way to check for made up links... or offline is here
        final int rc = (br.getHttpConnection() != null ? br.getHttpConnection().getResponseCode() : -1);
        if (rc == 403 || rc == 404 || rc == -1) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("Content has been removed due to copyright or from users")) {
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
        con = null;
        try {
            con = getConnection(br, downloadLink);
            if (!con.getContentType().contains("html")) {
                // is file
                downloadLink.setFinalFileName(getFileNameFromHeader(con));
                downloadLink.setDownloadSize(con.getLongContentLength());
                return AvailableStatus.TRUE;
            } else {
                // is html
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

    private boolean preferHeadRequest = true && isNewJD();

    private URLConnectionAdapter getConnection(final Browser br, final DownloadLink downloadLink) throws IOException {
        URLConnectionAdapter urlConnection = null;
        boolean rangeHeader = false;
        try {
            if (downloadLink.getProperty("streamMod") != null) {
                rangeHeader = true;
                br.getHeaders().put("Range", "bytes=" + 0 + "-");
            }
            if (downloadLink.getStringProperty("post", null) != null) {
                urlConnection = br.openPostConnection(dllink, downloadLink.getStringProperty("post", null));
            } else {
                try {
                    if (!preferHeadRequest || "GET".equals(downloadLink.getStringProperty("requestType", null))) {
                        urlConnection = br.openGetConnection(dllink);
                    } else if (preferHeadRequest || "HEAD".equals(downloadLink.getStringProperty("requestType", null))) {
                        urlConnection = br.openHeadConnection(dllink);
                        if (urlConnection.getResponseCode() == 404 && StringUtils.contains(urlConnection.getHeaderField("Cache-Control"), "must-revalidate") && urlConnection.getHeaderField("Via") != null) {
                            urlConnection.disconnect();
                            urlConnection = br.openGetConnection(dllink);
                        } else if (urlConnection.getResponseCode() != 404 && urlConnection.getResponseCode() >= 300) {
                            // no head support?
                            urlConnection.disconnect();
                            urlConnection = br.openGetConnection(dllink);
                        } else if (urlConnection.getContentType().contains("html")) {
                            urlConnection.disconnect();
                            urlConnection = br.openGetConnection(dllink);
                        }
                    } else {
                        urlConnection = br.openGetConnection(dllink);
                    }
                } catch (final IOException e) {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (preferHeadRequest || "HEAD".equals(downloadLink.getStringProperty("requestType", null))) {
                        /* some servers do not allow head requests */
                        urlConnection = br.openGetConnection(dllink);
                        downloadLink.setProperty("requestType", "GET");
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            if (rangeHeader) {
                br.getHeaders().remove("Range");
            }
        }
        return urlConnection;
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