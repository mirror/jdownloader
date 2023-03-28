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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hqcollect.net" }, urls = { "https?://(?:www\\.)?hqcollect\\.(?:me|net)/(?:pack|downloads)/(\\d+\\-[a-z0-9\\-]+)\\.html" })
public class HqcollectNet extends PluginForHost {
    public HqcollectNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: https
    // other:
    /* Extension which will be used if no correct extension is found */
    private static final String  default_Extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    /* More chunks possible but often causes problems / disconnects! */
    private static final int     free_maxchunks    = 1;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              limit_reached     = false;

    @Override
    public String getAGBLink() {
        return "https://hqcollect.me/support/";
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
    public String rewriteHost(String host) {
        if (host == null || host.equalsIgnoreCase("hqcollect.me")) {
            /* hqcollect.me -> hqcollect.net */
            return this.getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null) {
            /* 2021-10-04: E.g. hqcollect.me does not exist anymore */
            final String hostUrl = Browser.getHost(link.getPluginPatternMatcher());
            link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst(org.appwork.utils.Regex.escape(hostUrl), this.getHost()));
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        limit_reached = false;
        this.setBrowserExclusive();
        /* Set readable filename in teams of (unexpected) errors. */
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + default_Extension);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2016-10-07: Limit happens after fully watching/downloading one file - linkchecking does NOT make the limit appear earlier! */
        limit_reached = this.br.containsHTML("(?i)Your limit for browsing videos was reached");
        String filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1>([^<>\"]+)</h1>").getMatch(0);
        }
        if (filename != null) {
            filename = this.getFID(link) + "_" + filename;
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = encodeUnicode(filename);
            link.setFinalFileName(filename + ".mp4");
        }
        dllink = br.getRegex("<source src=\"(https?://[^<>\"]+)\" type=\"video/mp4\">").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[^<>\"/]+\\.hqcollect\\.me/watch/[^<>\"/]+\\.mp4)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<img class=\"u-full-width\" src=\"(.*?)\"").getMatch(0); // Picture from picture collection
        }
        if (!limit_reached && this.dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?", 60 * 60 * 1000l);
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
        if (limit_reached) {
            /* 2016-10-07: Exact waittime is not given */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        } else if (dllink == null) {
            /* Probably only available for premium users */
            throw new AccountRequiredException();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?", 60 * 60 * 1000l);
            }
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
