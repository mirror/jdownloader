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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bdsmstreak.com" }, urls = { "https?://(?:www\\.)?bdsmstreak\\.com/(?:embed/\\d+|video/\\d+(?:/[a-z0-9\\-]+)?)" })
public class BdsmstreakCom extends PluginForHost {
    public BdsmstreakCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags:
    // other:
    /* Connection stuff */
    private static final int free_maxchunks = 1;
    private String           dllink         = null;
    private final String     PATTERN_NORMAL = "(?i)https?://[^/]+/video/(\\d+)(/([a-z0-9\\-]+))?";
    private final String     PATTERN_EMBED  = "(?i)https?://[^/]+/embed/(\\d+)";

    @Override
    public String getAGBLink() {
        return "http://bdsmstreak.com/terms";
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

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    private String getFID(final DownloadLink link) {
        String fid = new Regex(link.getPluginPatternMatcher(), PATTERN_NORMAL).getMatch(0);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), PATTERN_EMBED).getMatch(0);
        }
        return fid;
    }

    private String getContentURL(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(PATTERN_EMBED)) {
            /* Their embed URLs are broken so we build normal URLs out of them in case the user is adding embed URLs. */
            return "https://" + this.getHost() + "/video/" + this.getFID(link);
        } else {
            return link.getPluginPatternMatcher();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        final String extDefault = ".mp4";
        dllink = null;
        String urlSlug = new Regex(link.getPluginPatternMatcher(), "([a-z0-9\\-]+)/?$").getMatch(0);
        if (!link.isNameSet()) {
            if (urlSlug != null) {
                link.setName(urlSlug.replace("-", " ").trim() + extDefault);
            } else {
                link.setName(this.getFID(link) + extDefault);
            }
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        br.getPage(getContentURL(link));
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)\"Video not found|>\\s*This video doesn't exist")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (urlSlug == null) {
            urlSlug = new Regex(br.getURL(), PATTERN_NORMAL).getMatch(1);
        }
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (title == null) {
            /* Fallback: use title from url */
            title = urlSlug.replace("-", " ").trim();
        }
        dllink = br.getRegex("\"(https?://[^\"]+\\.mp4[^\"]+)\"").getMatch(0);
        if (!StringUtils.isEmpty(dllink)) {
            dllink = br.getURL(dllink).toString();
            if (Encoding.isHtmlEntityCoded(dllink)) {
                dllink = Encoding.htmlDecode(dllink);
            }
        }
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setName(this.applyFilenameExtension(title, extDefault));
        }
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                if (title != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, con));
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
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
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
