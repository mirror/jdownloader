//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "arte.tv" }, urls = { "" })
public class ArteTv extends PluginForHost {
    @SuppressWarnings("deprecation")
    public ArteTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.arte.tv/sites/corporate/de/allgemeine-nutzungsbedingungen/";
    }
    // public static GetRequest requestAPIURL(final Browser br, final String apiurl) throws PluginException, IOException {
    // if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "API v1 no longer available!?");
    // }
    // GetRequest apiRequest = new GetRequest(br.getURL(apiurl)) {
    // @Override
    // protected boolean isKeepAlivePermitted(URLConnectionAdapter con) {
    // return con != null && con.getResponseCode() != 500;
    // }
    // };
    // // this server responds with 500 internal server error
    // // apiRequest.setCustomInetAddress(InetAddress.getByName("104.121.133.101"));
    // br.getPage(apiRequest);
    // if (br.getHttpConnection().getResponseCode() == 500) {
    // apiRequest.resetConnection();
    // // disable keep-alive to allow customInetAddress
    // // apiRequest.getHeaders().put(HTTPConstants.HEADER_REQUEST_CONNECTION, "close");
    // apiRequest.setCustomInetAddress(InetAddress.getByName("23.54.96.216"));// alternative 23.203.94.140
    // br.getPage(apiRequest);
    // if (br.getHttpConnection().getResponseCode() == 500) {
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // }
    // return apiRequest;
    // }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        br.setFollowRedirects(true);
        final String directurl = this.getDirectURL(link);
        if (!StringUtils.isEmpty(directurl) && !isDownload && !this.isHLS(link)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(directurl);
                connectionErrorhandling(con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private void connectionErrorhandling(final URLConnectionAdapter con) throws PluginException {
        if (!this.looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Video broken?");
            }
        }
    }

    private String getDirectURL(final DownloadLink link) {
        /* TODO: Remove this backward compatibility in 01-2023 */
        final String legacy_directURL = link.getStringProperty("directURL");
        if (legacy_directURL != null) {
            return legacy_directURL;
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    private boolean isHLS(final DownloadLink link) {
        /* TODO: Remove this backward compatibility in 01-2023 */
        final String legacy_quality_intern = link.getStringProperty("quality_intern");
        if (StringUtils.contains(legacy_quality_intern, "hls_")) {
            return true;
        } else if (getDirectURL(link).contains(".m3u8")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String directurl = this.getDirectURL(link);
        if (directurl == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (this.isHLS(link)) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, directurl);
            dl.startDownload();
        } else {
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, true, 0);
            connectionErrorhandling(dl.getConnection());
            dl.startDownload();
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

    @Override
    public String getDescription() {
        return "JDownloader's ARTE Plugin helps downloading videoclips from arte.tv. Arte provides different video qualities.";
    }
}