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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fex.net" }, urls = { "https?://(?:www\\.)?fex\\.net/folder/([a-z0-9]+)/file/(\\d+)" })
public class FexNet extends PluginForHost {
    public FexNet(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Connection stuff */
    private static final boolean free_resume          = false;
    private static final int     free_maxchunks       = 1;
    private static final int     free_maxdownloads    = -1;
    private String               dllink               = null;
    public static final String   PROPERTY_directurl   = "directurl";
    public static final String   PROPERTY_token       = "authtoken";
    private static String        cachedToken          = null;
    private static long          cachedTokenTimestamp = 0;
    private static Object        LOCK                 = new Object();

    @Override
    public String getAGBLink() {
        return "https://fex.net/terms";
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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        /* 2020-01-14: At this moment we're working with static URLs */
        dllink = link.getStringProperty(PROPERTY_directurl);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // br.getPage("https://" + this.getHost() + "/");
        final String thistoken;
        /* First try to use token from API */
        if (link.hasProperty(PROPERTY_token)) {
            thistoken = link.getStringProperty(PROPERTY_token);
        } else {
            synchronized (LOCK) {
                if (cachedToken == null || System.currentTimeMillis() - cachedTokenTimestamp > 5 * 60 * 1000l) {
                    logger.info("Refreshing token");
                    cachedToken = jd.plugins.decrypter.FexNet.getFreshAuthToken(this.br);
                    cachedTokenTimestamp = System.currentTimeMillis();
                }
            }
            thistoken = cachedToken;
        }
        br.setCookie(this.getHost(), "token", thistoken);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(this.dllink);
            if (this.looksLikeDownloadableContent(con)) {
                /* Filename is usually set in crawler */
                if (link.getFinalFileName() == null) {
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                }
                link.setDownloadSize(con.getCompleteContentLength());
                link.setVerifiedFileSize(con.getCompleteContentLength());
            } else if (con.getResponseCode() == 403) {
                /* Auth token refresh needed */
                if (link.hasProperty(PROPERTY_token)) {
                    link.removeProperty(PROPERTY_token);
                } else {
                    cachedToken = null;
                    cachedTokenTimestamp = 0;
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid auth token", 3 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        dl.setFilenameFix(true);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error");
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
