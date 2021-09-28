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
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RecurbateCom extends antiDDoSForHost {
    public RecurbateCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags: Porn plugin
    // other:

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = -2;
    private static final int     free_maxdownloads = 1;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "recurbate.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/play\\.php\\?video=(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://recurbate.com/terms";
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
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String performer = br.getRegex("/performer/([^/\"<>]+)").getMatch(0);
        if (performer != null) {
            link.setFinalFileName(Encoding.htmlDecode(performer).trim() + "_" + this.getFID(link) + ".mp4");
        } else {
            /* Fallback */
            link.setFinalFileName(this.getFID(link) + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        final String directurlproperty = "directlink";
        if (!this.attemptStoredDownloadurlDownload(link, directurlproperty, free_resume, free_maxchunks)) {
            requestFileInformation(link);
            final String token = br.getRegex("data-token=\"([a-f0-9]{64})\"").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setCookie(br.getHost(), "im18", "true");
            br.setCookie(br.getHost(), "im18_ets", Long.toString(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
            br.setCookie(br.getHost(), "im18_its", Long.toString(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000));
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            brc.getHeaders().put("Accept", "*/*");
            final UrlQuery query = new UrlQuery();
            query.add("video", this.getFID(link));
            query.add("token", token);
            brc.getPage("/api/get.php?" + query.toString());
            if (brc.toString().equalsIgnoreCase("shall_signin")) {
                /* Free users can watch one video per IP per X time. */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            } else if (brc.toString().equalsIgnoreCase("shall_subscribe")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily downloadlimit reached", 60 * 60 * 1000l);
            }
            final String dllink = brc.getRegex("<source src=\"(https?://[^\"]+)\"[^>]*type=\"video/mp4\" />").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
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
            /*
             * 2021-09-01: Save to re-use later. This URL is valid for some minutes only but allows resume + chunkload (up to 3 connections
             * - we allow max. 2.).
             */
            link.setProperty(directurlproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
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