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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BrighteonCom extends antiDDoSForHost {
    public BrighteonCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // other: Only HLS + DASH

    /* Connection stuff */
    private static final int    free_maxdownloads    = -1;
    private String              hlsMaster            = null;
    private static final String PROPERTY_PREMIUMONLY = "premiumonly";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "brighteon.com" });
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
            /* Only numbers after slash = old format --> URLs are still valid (checked 2021-12-07) */
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:embed/)?([a-f0-9\\-]{32,}|[0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://legal.brighteon.com/TermsofService.html";
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
        hlsMaster = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(500);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 500) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>THE VIDEO YOU&#x27;VE SELECTED IS NOT CURRENTLY AVAILABLE|>This can be caused by the video creator deactivating the video")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String json = br.getRegex("id=\"__NEXT_DATA__\" type=\"application/json\">(\\{.*?\\})</script>").getMatch(0);
        if (json == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(json);
        final Map<String, Object> video = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/initialProps/pageProps/video");
        final Map<String, Object> channel = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/initialProps/pageProps/channel");
        final String channelName = (String) channel.get("shortUrl");
        final String title = (String) video.get("name");
        final String description = (String) video.get("description");
        final String date = (String) video.get("createdAt");
        final String dateFormatted = new Regex(date, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        if (StringUtils.isEmpty(link.getComment()) && !StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        if (!StringUtils.isEmpty(title)) {
            link.setFinalFileName(dateFormatted + "_" + channelName + "_" + title + ".mp4");
        }
        /* Find HLS master URL. */
        final List<Map<String, Object>> sources = (List<Map<String, Object>>) video.get("source");
        for (final Map<String, Object> source : sources) {
            final String type = (String) source.get("type");
            final String src = (String) source.get("src");
            if (type.equalsIgnoreCase("application/x-mpegURL")) {
                this.hlsMaster = src;
                break;
            }
        }
        if ((Boolean) video.get("fullVideoIsForSale") == Boolean.TRUE) {
            link.setProperty(PROPERTY_PREMIUMONLY, true);
        } else {
            link.removeProperty(PROPERTY_PREMIUMONLY);
        }
        return AvailableStatus.TRUE;
    }

    private boolean isPremiumonly(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_PREMIUMONLY)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (isPremiumonly(link)) {
            throw new AccountRequiredException();
        } else if (StringUtils.isEmpty(hlsMaster)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        brc.getPage(hlsMaster);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(brc));
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
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