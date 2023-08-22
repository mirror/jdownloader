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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.DouyinComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DouyinCom extends PluginForHost {
    public DouyinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: tiktok.com China (chinese tiktok pendant)
    /* Connection stuff */
    private static final boolean free_resume        = true;
    /* 2021-08-13: Chunks possible but disabled in order to prevent a lot of http requests. */
    private static final int     free_maxchunks     = 1;
    private static final int     free_maxdownloads  = -1;
    private String               dllink             = null;
    private static final String  PROPERTY_DIRECTURL = "directurl";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "douyin.com", "iesdouyin.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:share/)?video/(\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.douyin.com/";
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
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            /* Set fallback name */
            link.setName(this.getFID(link) + ".mp4");
        }
        if (!isDownload && this.checkDirectLink(link) != null) {
            logger.info("Availablecheck via directurl succeeded");
            return AvailableStatus.TRUE;
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String dateFormatted;
        String username;
        /*
         * 2021-08-30: API might get around website captchas but according to users, videos via API come with watermark but not via website
         * (I guess website provides multiple formats while API only provides one).
         */
        if (PluginJsonConfig.get(DouyinComConfig.class).isUseAPI()) {
            final Browser brc = br.cloneBrowser();
            /* https://github.com/missuo/DouyinParsing */
            brc.getHeaders().put("Accept", "application/json");
            brc.getPage("https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=" + this.getFID(link));
            // if (brc.getHttpConnection().getResponseCode() == 404) {
            // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // }
            Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
            final List<Object> results = (List<Object>) entries.get("item_list");
            /* List is empty --> Video must be offline */
            if (results.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> videoInfo = (Map<String, Object>) results.get(0);
            {
                final Map<String, Object> shareInfo = (Map<String, Object>) videoInfo.get("share_info");
                final String description = (String) shareInfo.get("share_weibo_desc");
                if (!StringUtils.isEmpty(description) && link.getComment() == null) {
                    link.setComment(description);
                }
            }
            {
                final Map<String, Object> author = (Map<String, Object>) videoInfo.get("author");
                username = (String) author.get("nickname");
            }
            final long createTime = ((Number) videoInfo.get("create_time")).longValue();
            dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(new Date(createTime * 1000));
            final Map<String, Object> video = (Map<String, Object>) videoInfo.get("video");
            this.dllink = (String) JavaScriptEngineFactory.walkJson(video, "play_addr/url_list/{0}");
        } else {
            br.getPage("https://www." + this.getHost() + "/video/" + this.getFID(link));
            if (isBotProtectionActive(this.br)) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Captcha blocked");
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!br.containsHTML("/video/" + this.getFID(link))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!this.canHandle(this.br.getURL())) {
                /* Redirect to somewhere else */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String json = br.getRegex("<script id=\"RENDER_DATA\" type=\"application/json\">(.*?)<").getMatch(0);
            json = Encoding.htmlDecode(json);
            Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            final Map<String, Object> aweme = (Map<String, Object>) findAwemeMap(entries);
            if (aweme == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Object> videoInfo = (Map<String, Object>) aweme.get("detail");
            /*
             * 2021-08-13: This can contain an "official" download-URL but it seems like the quality is really bad and speed is very
             * limited.
             */
            // final Map<String, Object> download = (Map<String, Object>) videoInfo.get("download");
            final Map<String, Object> video = (Map<String, Object>) videoInfo.get("video");
            final Map<String, Object> authorInfo = (Map<String, Object>) videoInfo.get("authorInfo");
            username = (String) authorInfo.get("nickname");
            final long createTime = ((Number) videoInfo.get("createTime")).longValue();
            final String description = (String) videoInfo.get("desc");
            if (!StringUtils.isEmpty(description) && link.getComment() == null) {
                link.setComment(description);
            }
            dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(new Date(createTime * 1000));
            this.dllink = (String) video.get("playApi");
        }
        link.setFinalFileName(dateFormatted + "_" + username + "_" + this.getFID(link) + ".mp4");
        if (!StringUtils.isEmpty(dllink) && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                /* 2021-08-13: Server doesn't accept HEAD-request and final downloadurl is only valid once! */
                con = br2.openGetConnection(this.dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    br2.followConnection(true);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - video broken?");
                } else {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    /* This final (temporary) downloadurl can be checked using HEAD requests! */
                    link.setProperty(getDirecturlProperty(), con.getURL().toString());
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

    private String checkDirectLink(final DownloadLink link) {
        final String property = this.getDirecturlProperty();
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private static boolean isBotProtectionActive(final Browser br) {
        /* 2021-08-30: This may happen for some users. Captcha required --> Trying again with another IP may or may not help. */
        if (br.containsHTML("window\\.TTGCaptcha\\.init")) {
            return true;
        } else if (br.containsHTML("(?i)error magic number")) {
            /* 2022-08-24 */
            return true;
        } else {
            return false;
        }
    }

    /** Recursive function to find photoMap inside json. */
    private Object findAwemeMap(final Object o) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            if (entrymap.containsKey("statusCode") && entrymap.containsKey("detail")) {
                return entrymap;
            } else {
                for (final Map.Entry<String, Object> cookieEntry : entrymap.entrySet()) {
                    // final String key = cookieEntry.getKey();
                    final Object value = cookieEntry.getValue();
                    if (value instanceof List || value instanceof Map) {
                        final Object video = findAwemeMap(value);
                        if (video != null) {
                            return video;
                        }
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object video = findAwemeMap(arrayo);
                    if (video != null) {
                        return video;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        if (!attemptStoredDownloadurlDownload(link)) {
            requestFileInformation(link, true);
            if (StringUtils.isEmpty(dllink)) {
                if (PluginJsonConfig.get(DouyinComConfig.class).isUseAPI()) {
                    /*
                     * In API mode we can be sure that the videourl should be available --> Assume that the video is broken and also not
                     * playable via browser.
                     */
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Broken video?");
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - video broken?");
                }
            }
            link.setProperty(getDirecturlProperty(), dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private String getDirecturlProperty() {
        return PROPERTY_DIRECTURL + "_" + Boolean.toString(PluginJsonConfig.get(DouyinComConfig.class).isUseAPI());
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(getDirecturlProperty());
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, free_resume, free_maxchunks);
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

    @Override
    public Class<? extends DouyinComConfig> getConfigInterface() {
        return DouyinComConfig.class;
    }
}