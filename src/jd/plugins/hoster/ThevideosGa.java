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

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "thevideos.ga" }, urls = { "https?://(?:www\\.)?thevideos\\.ga/embed-([a-f0-9]{32})\\.html" })
public class ThevideosGa extends antiDDoSForHost {
    public ThevideosGa(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags: vev.io, UnknownVideohostingCore
    // other: base is vev.io but it is impossible to use the normal vev.io website/API for their URLs. Handles special embed URLs receives
    // mostly from crawler KinoxTo.java

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://thevideos.ga/";
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
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        /* No filename available --> Check if it maybe has been set in crawler before */
        String filename = link.getStringProperty("fallback_filename");
        if (filename == null) {
            /* Last chance fallback */
            filename = this.getFID(link);
        }
        final String ext = ".mp4";
        filename = Encoding.htmlDecode(filename).trim();
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        final String directurlproperty = "directurl";
        dllink = checkDirectLink(link, "directurl");
        if (dllink != null) {
            logger.info("Successfully re-used stored directurl");
        } else {
            /*
             * 2020-05-25: Basically the URLs which the user adds expire after some time but the final downloadurls seem to last longer
             * (multiple hours longer than the URL which the user adds).
             */
            logger.info("Failed to find/re-use old directurl");
            dllink = String.format("https://thevideos.ga/stream%s.mp4", this.getFID(link));
            br.setAllowedResponseCodes(new int[] { 502 });
            br.setFollowRedirects(false);
            br.getPage(dllink);
            /* 2020-05-24: 502 is typical for timed-out URLs here */
            if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 502) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                logger.info("No redirect found --> Content is probably offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            URLConnectionAdapter con = null;
            try {
                con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
                if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (con.getCompleteContentLength() < 1000) {
                    /* 2020-05-24 */
                    logger.info("File is very small --> Must be offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setProperty(directurlproperty, dllink);
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

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    return null;
                }
                link.setDownloadSize(con.getCompleteContentLength());
                return dllink;
            } catch (final Exception e) {
                logger.log(e);
                link.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
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
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
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
