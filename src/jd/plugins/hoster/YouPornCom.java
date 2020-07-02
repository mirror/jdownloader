//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.config.YoupornConfig;
import org.jdownloader.plugins.components.config.YoupornConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;

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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class YouPornCom extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    String          dllink        = null;
    private boolean server_issues = false;

    public YouPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://youporn.com/terms";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "youporn.com", "youpornru.com", "youporngay.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:[a-z]{2}\\.|www\\.)?" + buildHostsPatternPart(domains) + "/(?:watch|embed)/(\\d+)(/([a-z0-9\\-]+)/?)?");
        }
        return ret.toArray(new String[0]);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = getFID(link);
        final String url_name = getURLTitle(link);
        link.setLinkID(fid);
        final String host_url = Browser.getHost(link.getPluginPatternMatcher());
        final String final_host;
        /*
         * 2020-07-02: youporngay content is esssentially also youporn content but it will always redirect to youporngay.com --> Save some
         * milliseconds by avoiding having to follow this redirect ;)
         */
        if (host_url.equals("youporngay.com")) {
            final_host = "youporngay.com";
        } else {
            final_host = this.getHost();
        }
        if (url_name == null) {
            link.setPluginPatternMatcher("https://www." + final_host + "/watch/" + fid + "/" + System.currentTimeMillis() + "/");
        } else {
            link.setPluginPatternMatcher("https://www." + final_host + "/watch/" + fid + "/" + url_name + "/");
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getFallbackTitle(final DownloadLink link) {
        final String fid = getFID(link);
        final String url_name = getURLTitle(link);
        if (url_name == null) {
            return fid;
        } else {
            return url_name;
        }
    }

    private String getURLTitle(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
    }

    private static final String defaultEXT = ".mp4";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        if (!link.isNameSet()) {
            link.setName(this.getFallbackTitle(link) + defaultEXT);
        }
        this.dllink = null;
        this.server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://youporn.com/", "age_verified", "1");
        br.setCookie("http://youporn.com/", "yp-device", "1");
        br.setCookie("http://youporn.com/", "language", "en");
        br.getPage(link.getPluginPatternMatcher());
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        if (br.containsHTML("<div id=\"video\\-not\\-found\\-related\"|watchRemoved\"|class=\\'video\\-not\\-found\\'")) {
            /* Offline link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("404 \\- Page Not Found<|id=\"title_404\"") || this.br.getHttpConnection().getResponseCode() == 404) {
            /* Invalid link */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("class='geo-blocked-content'")) {
            /* 2020-07-02: New: E.g. if you go to youpornru.com with a german IP and add specific URLs (not all content is GEO-blocked!). */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "GEO-blockd", 3 * 60 * 60 * 1000l);
        } else if (this.br.containsHTML("onload=\"go\\(\\)\"")) {
            /* 2017-07-26: TODO: Maybe follow that js redirect */
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Temporarily blocked because of too many requests", 5 * 60 * 1000l);
        }
        String filename = br.getRegex("<title>(.*?) \\- Free Porn Videos[^<>]+</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?) Video \\- Youporn\\.com</title>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("addthis:title=\"YouPorn \\- (.*?)\"></a>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("\\'video_title\\' : \"([^<>\"]*?)\"").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("videoTitle: \'([^<>\']*?)\'").getMatch(0);
        }
        if (filename == null) {
            filename = this.getFID(link);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim().replaceAll("   ", "-");
        if (br.getURL().contains("/private/") || br.containsHTML("for=\"privateLogin_password\"")) {
            link.getLinkStatus().setStatusText("Password protected links are not yet supported, contact our support!");
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected links are not yet supported, contact our support!");
        }
        /* Find highest quality */
        int qualityMax = 0;
        /* 2020-07-02: Try to obey users' selected quality in this block only */
        String filesize = null;
        final String mediaDefinition = br.getRegex("video\\.mediaDefinition\\s*=\\s*(\\[.*?\\]);").getMatch(0);
        if (mediaDefinition != null) {
            final String userPreferredQuality = getPreferredStreamQuality();
            qualityMax = 0;
            final List<Object> list = JSonStorage.restoreFromString(mediaDefinition, TypeRef.LIST);
            if (list != null) {
                for (Object entry : list) {
                    final Map<String, Object> video = (Map<String, Object>) entry;
                    final Object quality = video.get("quality");
                    if (quality == null) {
                        continue;
                    }
                    final String videoUrl = (String) video.get("videoUrl");
                    if (StringUtils.isEmpty(videoUrl)) {
                        continue;
                    }
                    final String qualityTempStr = (String) quality;
                    if (StringUtils.equals(qualityTempStr, userPreferredQuality)) {
                        logger.info("Found user preferred quality: " + userPreferredQuality);
                        dllink = videoUrl;
                        break;
                    }
                    final int qualityTemp = Integer.parseInt(qualityTempStr);
                    if (qualityTemp > qualityMax) {
                        qualityMax = qualityTemp;
                        dllink = videoUrl;
                    }
                }
            }
        }
        /* Use fallback if needed - don't care about users' selected quality! */
        if (StringUtils.isEmpty(this.dllink)) {
            /* Old handling: Must not be present */
            final String[] htmls = br.getRegex("class='callBox downloadOption[^~]*?downloadVideoLink clearfix'([^~]*?)</span>").getColumn(0);
            for (final String html : htmls) {
                final String quality = new Regex(html, "(\\d+)p_\\d+k").getMatch(0);
                if (quality == null) {
                    continue;
                }
                final int qualityTemp = Integer.parseInt(quality);
                if (qualityTemp > qualityMax) {
                    qualityMax = qualityTemp;
                    this.dllink = new Regex(html, "(https?://[^'\"]+\\d+p[^'\"]+\\.mp4[^\\'\"\\|]+)").getMatch(0);
                    if (this.dllink != null) {
                        /* Only attempt to grab filesize if it corresponds to the current videoquality! */
                        filesize = new Regex(html, "class=\\'downloadsize\\'>\\((\\d+[^<>\"]+)\\)").getMatch(0);
                    }
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[^<>\"\\']+)\">MP4").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://videos\\-\\d+\\.youporn\\.com/[^<>\"\\'/]+/save/scene_h264[^<>\"\\']+)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://cdn[a-z0-9]+\\.public\\.youporn\\.phncdn\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<ul class=\"downloadList\">.*?href=\"(https?://[^\"]+)\">.*?</ul>").getMatch(0);
        }
        if (dllink == null) {
            /**
             * 2020-05-27: Workaround/Fallback for some users who seem to get a completely different pornhub page (???) RE:
             * https://svn.jdownloader.org/issues/88346 </br>
             * This source will be lower quality than their other sources!
             */
            dllink = br.getRegex("meta name=\"twitter:player:stream\" content=\"(http[^<>\"\\']+)\"").getMatch(0);
        }
        if (dllink != null) {
            /* Do NOT htmldecode! */
            dllink = dllink.replace("&amp;", "&");
        }
        link.setFinalFileName(filename + defaultEXT);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
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

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getPreferredStreamQuality() {
        final YoupornConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        default:
            return null;
        case BEST:
            return null;
        case Q2160P:
            return "2160";
        case Q1080P:
            return "1080";
        case Q720P:
            return "720";
        case Q480P:
            return "480";
        case Q360P:
            return "360";
        case Q240P:
            return "240";
        }
    }

    @Override
    public Class<? extends YoupornConfig> getConfigInterface() {
        return YoupornConfig.class;
    }

    public void reset() {
    }

    public void resetDownloadlink(final DownloadLink link) {
    }
}