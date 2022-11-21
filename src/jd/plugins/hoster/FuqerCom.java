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

import org.jdownloader.plugins.controller.LazyPlugin;

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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fuqer.com" }, urls = { "https?://(?:www\\.)?fuqer\\.com/(?:videos/[a-z0-9\\-]+\\-\\d+\\.html|nuevo/player/embed\\.php\\?key=\\d+)" })
public class FuqerCom extends PluginForHost {
    public FuqerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private static final String  TYPE_EMBED        = "https?://[^/]+/nuevo/player/embed\\.php\\?key=(\\d+)";
    private static final String  TYPE_NORMAL       = "https?://[^/]+/videos/([a-z0-9\\-]+)\\-(\\d+)\\.html";

    @Override
    public String getAGBLink() {
        return "https://www.fuqer.com/tos";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        }
        if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(1);
        }
    }

    private String getWeakTitle(final String url) {
        if (url.matches(TYPE_EMBED)) {
            return new Regex(url, TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(url, TYPE_NORMAL).getMatch(0);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String urlTitle = getWeakTitle(link.getPluginPatternMatcher());
        link.setFinalFileName(urlTitle.replace("-", " ").trim() + ".mp4");
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().matches(TYPE_EMBED)) {
            /* Title is not given in added URL -> Look for it in HTML code */
            final String fid = this.getFID(link);
            final String weakTitle = br.getRegex("/videos/([a-z0-9\\-]+-)" + fid + "\\.html").getMatch(0);
            if (weakTitle != null) {
                link.setFinalFileName(weakTitle.replace("-", " ").trim() + ".mp4");
            }
        }
        // final String videoid = urlinfo.getMatch(1);
        // this.br.getPage("/nuevo/player/config5.php?key=" + videoid);
        dllink = br.getRegex("<source src=(\"|')(https?://[^<>\"\\']+)\\1[^>]*type=\\1video/mp4").getMatch(1);
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    server_issues = true;
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
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
