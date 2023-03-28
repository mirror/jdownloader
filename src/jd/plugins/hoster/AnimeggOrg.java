//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.LinkedHashMap;

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
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.AnimeggOrgConfig;
import org.jdownloader.plugins.components.config.AnimeggOrgConfig.Quality;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animegg.org" }, urls = { "https?://(www\\.)?animegg\\.org/(?:embed/\\d+|[\\w\\-]+episode-\\d+)" })
public class AnimeggOrg extends antiDDoSForHost {
    // raztoki embed video player template.
    private String dllink = null;

    public AnimeggOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.animegg.org/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        // not yet available. We can only say offline!
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<img src=\"\\.\\./images/animegg-unavailable.jpg\" style=\"width: 100%\">")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!br.getURL().matches(".+/embed/\\d+")) {
            final String embed = br.getRegex("<iframe [^>]*src=(\"|')(.*?/embed/\\d+)\\1").getMatch(1);
            if (embed == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            getPage(embed);
        }
        final String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        // multiple qualities.
        final String vidquals = br.getRegex("videoSources\\s*=\\s*(\\[.*?\\]);").getMatch(0);
        final LinkedHashMap<Integer, String> results = new LinkedHashMap<Integer, String>();
        final String[] quals = PluginJSonUtils.getJsonResultsFromArray(vidquals);
        String bestQualityDownloadurl = null;
        int bestQuality = -1;
        for (final String qual : quals) {
            final String label = PluginJSonUtils.getJsonValue(qual, "label");
            final String labelP = new Regex(label, "(\\d+)p").getMatch(0);
            final Integer p = labelP != null ? Integer.parseInt(labelP) : -1;
            final String url = PluginJSonUtils.getJsonValue(qual, "file");
            if (url != null && labelP != null) {
                results.put(p, url);
            }
            if (p > bestQuality) {
                bestQuality = p;
                bestQualityDownloadurl = url;
            }
        }
        final int userPreferredQuality = getUserPreferredquality();
        final int chosenQuality;
        if (results.containsKey(userPreferredQuality)) {
            dllink = results.get(userPreferredQuality);
            chosenQuality = userPreferredQuality;
        } else {
            dllink = bestQualityDownloadurl;
            chosenQuality = bestQuality;
        }
        if (dllink == null || filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Chosen quality: " + chosenQuality + "p");
        dllink = Encoding.urlDecode(dllink, false);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(dllink);
            // only way to check for made up links... or offline is here
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.looksLikeDownloadableContent(con)) {
                link.setFinalFileName(filename + ".mp4");
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    private int getUserPreferredquality() {
        final Quality quality = PluginJsonConfig.get(AnimeggOrgConfig.class).getPreferredQuality();
        switch (quality) {
        case Q1080:
            return 1080;
        case Q720:
            return 720;
        case Q480:
            return 480;
        case Q360:
            return 360;
        case Q240:
            return 240;
        default:
            /* Should never happen */
            return -1;
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return AnimeggOrgConfig.class;
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