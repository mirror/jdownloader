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
import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.FreeViewMoviesComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { jd.plugins.decrypter.FreeViewMoviesComCrawler.class })
public class FreeViewMoviesCom extends PluginForHost {
    private String              dllink      = null;
    public static final String  TYPE_EMBED  = "(?:https?://[^/]+)?/embed/(\\d+)/?";
    private static final String TYPE_NORMAL = "(?:https?://[^/]+)?/videos?/(\\d+)/([a-z0-9\\-]+)";

    public FreeViewMoviesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        return FreeViewMoviesComCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:videos?/\\d+/[a-z0-9\\-]+|embed/\\d+/?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.freeviewmovies.com/page/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_EMBED)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_EMBED).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0);
        }
    }

    private String getWeakFilename(final DownloadLink link) {
        final String urlTitle = getURLTitleCleaned(link.getPluginPatternMatcher());
        if (urlTitle != null) {
            return urlTitle.replace("-", " ").trim() + ".mp4";
        } else {
            return this.getFID(link) + ".mp4";
        }
    }

    private String getURLTitleCleaned(final String url) {
        String title = new Regex(url, TYPE_NORMAL).getMatch(1);
        if (title != null) {
            return title.replace("-", " ").trim();
        } else {
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().matches(TYPE_EMBED)) {
            final String realVideoURL = br.getRegex(TYPE_NORMAL).getMatch(-1);
            if (realVideoURL == null || !realVideoURL.contains(this.getFID(link))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(realVideoURL);
            /* Double-check */
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (this.canHandle(br.getURL())) {
                link.setPluginPatternMatcher(br.getURL());
            }
        }
        final String titleByURL = getURLTitleCleaned(br.getURL());
        if (titleByURL != null) {
            link.setFinalFileName(titleByURL.replace("-", " ").trim() + ".mp4");
        }
        dllink = br.getRegex("<source src=\"(https?://[^\"]+freeviewmovies[^\"]+)\"[^>]*type=\"video/mp4").getMatch(0);
        if (dllink == null) {
            /* 2023-09-15 */
            dllink = br.getRegex("\"contentUrl\"\\s*:\\s*\"(https?://[^\"]+)").getMatch(0);
        }
        if (this.dllink != null) {
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
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

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*404 Error Page")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (this.dllink == null) {
            if (br.containsHTML("id=\"reportabuse\"")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video content");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
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