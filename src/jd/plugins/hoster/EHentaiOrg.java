//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.http.requests.HeadRequest;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "e-hentai.org" }, urls = { "http://ehentaidecrypted\\.org/\\d+" }, flags = { 0 })
public class EHentaiOrg extends PluginForHost {

    public EHentaiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean free_resume       = true;
    /* Limit chunks to 1 as we only download small files */
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;

    private static final long    minimal_filesize  = 1000;

    private String               DLLINK            = null;
    private final boolean        ENABLE_RANDOM_UA  = true;

    @Override
    public String getAGBLink() {
        return "http://g.e-hentai.org/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final String namepart = downloadLink.getStringProperty("namepart", null);
        DLLINK = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (ENABLE_RANDOM_UA) {
            /*
             * Using a different UA for every download might be a bit obvious but at the moment, this fixed the error-server responses as it
             * tricks it into thinking that we re a lot of users and not only one.
             */
            br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        br.getPage(downloadLink.getStringProperty("individual_link", null));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        DLLINK = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+(:\\d+)?/h/[^<>\"]*?)\"").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("src=\"(http://[^<>\"]*?image\\.php\\?[^<>\"]*?)\"").getMatch(0);
        }
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(namepart + DLLINK.substring(DLLINK.lastIndexOf(".")));
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            try {
                if (isJDStable()) {
                    /* @since JD2 */
                    con = br2.openHeadConnection(DLLINK);
                } else {
                    /* Not supported in old 0.9.581 Stable */
                    con = br2.openGetConnection(DLLINK);
                }
            } catch (final BrowserException ebr) {
                /* Whatever happens - its most likely a server problem for this host! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final long conlength = con.getLongContentLength();
            if (!con.getContentType().contains("html") && conlength > minimal_filesize) {
                downloadLink.setDownloadSize(conlength);
                downloadLink.setProperty("directlink", DLLINK);
                return AvailableStatus.TRUE;
            } else {
                return AvailableStatus.UNCHECKABLE;
            }
        } finally {
            if (con != null) {
                if (con.getRequest() instanceof HeadRequest) {
                    br2.loadConnection(con);
                } else {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadSize() < minimal_filesize) {
            /* E.h. "403 picture" is smaller than 1 KB */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - file is too small", 2 * 60 * 1000l);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, free_resume, free_maxchunks);
        } catch (final BrowserException ebr) {
            /* Whatever happens - its most likely a server problem for this host! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
