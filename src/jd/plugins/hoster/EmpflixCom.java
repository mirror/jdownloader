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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class EmpflixCom extends PluginForHost {
    public EmpflixCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_NORMAL = "(?i)https?://[^/]+/([a-z0-9\\-]+)/([a-z0-9\\-]+)/video(\\d+)";
    private static final String TYPE_embed  = "(?i)https?://player\\.[^/]+/video/(\\d+)";

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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[a-z0-9\\-]+/[a-z0-9\\-]+/video\\d+|https?://player\\." + buildHostsPatternPart(domains) + "/video/\\d+");
        }
        return ret.toArray(new String[0]);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "empflix.com" });
        return ret;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String contentID = getVideoID(link.getPluginPatternMatcher());
        if (contentID != null) {
            return link.getHost() + "://" + contentID;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getVideoID(final String url) {
        final Regex embed = new Regex(url, TYPE_embed);
        if (embed.matches()) {
            return embed.getMatch(0);
        } else {
            return new Regex(url, TYPE_NORMAL).getMatch(2);
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.empflix.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getURLName(final DownloadLink link) {
        final Regex urlNormal = new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL);
        if (urlNormal.matches()) {
            return urlNormal.getMatch(1);
        } else {
            return null;
        }
    }

    private String getFallbackFileTitle(final DownloadLink link) {
        final String urlName = getURLName(link);
        if (urlName != null) {
            return urlName;
        } else {
            return this.getVideoID(link.getPluginPatternMatcher());
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final String fallbackTitle = getFallbackFileTitle(link).replace("-", " ");
        if (!link.isNameSet()) {
            link.setName(fallbackTitle + ".mp4");
        }
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileTitleCrippled = br.getRegex("/([a-z0-9\\-]+)/video" + this.getVideoID(link.getPluginPatternMatcher())).getMatch(0);
        if (!StringUtils.isEmpty(fileTitleCrippled)) {
            link.setFinalFileName(fileTitleCrippled.replace("-", " ").trim() + ".mp4");
        } else {
            link.setFinalFileName(fallbackTitle + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String videoid = this.getVideoID(link.getPluginPatternMatcher());
        /*
         * Do not access relative URL here because e.g. their embed URLs are hosted on subdomain player.empflix.com and this would result in
         * failure then.
         */
        br.getPage("https://www." + this.getHost() + "/ajax/video-player/" + videoid);
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final String html = entries.get("html").toString();
        final String[] urls = new Regex(html, "src=\"(https?://[^\"]+)\"").getColumn(0);
        String bestQualityDownloadurl = null;
        int maxHeight = -1;
        for (final String url : urls) {
            final String heightStr = new Regex(url, "(\\d{2,})p").getMatch(0);
            if (heightStr == null) {
                /* Skip e.g. thumbnail URL. */
                continue;
            }
            final int height = Integer.parseInt(heightStr);
            if (height > maxHeight) {
                maxHeight = height;
                bestQualityDownloadurl = url;
            }
        }
        if (bestQualityDownloadurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, bestQualityDownloadurl, true, -2);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            /* 403 error usually means we've tried to download an official downloadurl which may only be available for loggedin users! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
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