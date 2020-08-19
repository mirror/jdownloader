//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "furaffinity.net" }, urls = { "https?://(?:www\\.)?furaffinity\\.net/view/(\\d+)" })
public class FuraffinityNet extends antiDDoSForHost {
    public FuraffinityNet(PluginWrapper wrapper) {
        super(wrapper);
        /* 2020-08-19: Try to avoid 503 errors */
        this.setStartIntervall(1000l);
    }
    /* DEV NOTES */
    // Tags:
    // other:

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;
    private boolean              accountRequired   = false;

    @Override
    public String getAGBLink() {
        return "https://www.furaffinity.net/tos";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* Website is hosting mostly picture content but sometimes also audio snippets. */
        // link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
        if (!link.isNameSet()) {
            link.setName(this.getFID(link));
        }
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 503 });
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 too many requests", 5 * 60 * 1000l);
        } else if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(this.getFID(link)) || br.containsHTML(">\\s*System Error|>\\s*The submission you are trying to find is not in")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*The owner of this page has elected to make it available to registered users only|>\\s*This submission contains Mature or Adult content")) {
            /* Content is online but we can't view/download it! */
            this.accountRequired = true;
            return AvailableStatus.TRUE;
        }
        dllink = br.getRegex("class=\"download fullsize\"><a href=\"([^\"]+)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("data-fullview-src=\"([^\"]+)").getMatch(0);
        }
        String filename = Plugin.getFileNameFromURL(br.getURL(dllink));
        if (filename != null) {
            link.setFinalFileName(filename);
        } else {
            /* Fallback */
            link.setName(this.getFID(link) + ".jpg");
        }
        if (!StringUtils.isEmpty(dllink) && link.getView().getBytesTotal() <= 0) {
            URLConnectionAdapter con = null;
            try {
                con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
                if (con.getContentType().contains("html") || !con.isOK()) {
                    server_issues = true;
                } else {
                    link.setDownloadSize(con.getCompleteContentLength());
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
        requestFileInformation(link);
        if (this.accountRequired) {
            throw new AccountRequiredException();
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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
