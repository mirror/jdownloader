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
import java.net.URL;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.TiktokConfig;
import org.jdownloader.plugins.components.config.TiktokConfig.DownloadMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.TiktokComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tiktok.com" }, urls = { "https?://(?:www\\.)?tiktok\\.com/((@[^/]+)/video/|embed/)(\\d+)|https?://m\\.tiktok\\.com/v/(\\d+)\\.html" })
public class TiktokCom extends PluginForHost {
    public TiktokCom(PluginWrapper wrapper) {
        super(wrapper);
        try {
            Browser.setRequestIntervalLimitGlobal("tiktok.com", true, 1000);
        } catch (final Throwable e) {
        }
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "tiktok.com" });
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
            final String hostsPattern = buildHostsPatternPart(domains);
            ret.add("https?://(?:www\\.)?" + hostsPattern + "/((@[^/]+)/video/|embed/)(\\d+)|https?://m\\." + hostsPattern + "/v/(\\d+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.tiktok.com/";
    }

    /* Connection stuff */
    private final boolean RESUME    = true;
    /* 2019-07-10: More chunks possible but that would not be such a good idea! */
    private final int     MAXCHUNKS = 1;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    public static String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "https?://.*/(?:video|v|embed)/(\\d+)").getMatch(0);
    }

    // private String dllink = null;
    public static final String  PROPERTY_DIRECTURL_WEBSITE         = "directurl";
    public static final String  PROPERTY_DIRECTURL_API             = "directurl_api";
    public static final String  PROPERTY_USERNAME                  = "username";
    public static final String  PROPERTY_USER_ID                   = "user_id";
    public static final String  PROPERTY_VIDEO_ID                  = "videoid";
    public static final String  PROPERTY_DATE                      = "date";
    public static final String  PROPERTY_DATE_LAST_MODIFIED_HEADER = "date_last_modified_header";
    public static final String  PROPERTY_DESCRIPTION               = "description";
    public static final String  PROPERTY_HASHTAGS                  = "hashtags";
    public static final String  PROPERTY_LIKE_COUNT                = "like_count";
    public static final String  PROPERTY_PLAY_COUNT                = "play_count";
    public static final String  PROPERTY_SHARE_COUNT               = "share_count";
    public static final String  PROPERTY_COMMENT_COUNT             = "comment_count";
    public static final String  PROPERTY_HAS_WATERMARK             = "has_watermark";
    public static final String  PROPERTY_LAST_USED_DOWNLOAD_MODE   = "last_used_download_mode";
    private static final String TYPE_VIDEO                         = "https?://[^/]+/@([^/]+)/video/(\\d+).*?";
    /* API related stuff */
    public static final String  API_BASE                           = "https://api-h2.tiktokv.com/aweme/v1";
    public static final String  API_VERSION_NAME                   = "20.9.3";
    public static final String  API_VERSION_CODE                   = "293";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public static String toHumanReadableNumber(final Number number) {
        final long num = number.longValue();
        if (num > 1000000) {
            return new DecimalFormat("0.00m").format((1.0f * num) / 1000000);
        } else if (num > 1000) {
            return new DecimalFormat("0.00k").format((1.0f * num) / 1000);
        } else {
            return number.toString();
        }
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        final String fid = getFID(link);
        if (fid == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(PROPERTY_VIDEO_ID, fid);
        if (!link.isNameSet()) {
            /* Fallback-filename */
            link.setName(fid + ".mp4");
        }
        if (PluginJsonConfig.get(this.getConfigInterface()).getDownloadMode() == DownloadMode.API) {
            this.checkAvailablestatusAPI(link, isDownload);
        } else {
            this.checkAvailablestatusWebsite(link, isDownload);
        }
        final String dllink = getStoredDirecturl(link);
        if (!StringUtils.isEmpty(dllink) && !link.isSizeSet() && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                con = brc.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    try {
                        brc.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?", 10 * 60 * 1000l);
                } else {
                    /*
                     * 2020-05-04: Do not use header anymore as it seems like they've modified all files < December 2019 so their
                     * "Header dates" are all wrong now.
                     */
                    // createDate = con.getHeaderField("Last-Modified");
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    final String lastModifiedHeaderValue = brc.getRequest().getResponseHeader("Last-Modified");
                    if (lastModifiedHeaderValue != null) {
                        link.setProperty(PROPERTY_DATE_LAST_MODIFIED_HEADER, lastModifiedHeaderValue);
                    }
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
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        if (super.looksLikeDownloadableContent(urlConnection)) {
            return true;
        } else if (urlConnection.getRequest().getResponseHeader("X-Video-Codec-Type") != null) {
            /* E.g. HEAD-request returning wrong content-type: https://www.tiktok.com/@ameliagething/video/6895294446670318850 */
            return true;
        } else {
            return false;
        }
    }

    public static void setFilename(final DownloadLink link) {
        String filename = "";
        String dateFormatted = getDateFormatted(link);
        if (dateFormatted != null) {
            filename = dateFormatted;
        }
        final String username = getUsername(link);
        if (!StringUtils.isEmpty(username)) {
            filename += "_@" + username;
        }
        filename += "_" + getFID(link) + ".mp4";
        /* Only set final filename if ALL information is available! */
        if (link.hasProperty(PROPERTY_DATE) && !StringUtils.isEmpty(username)) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
    }

    private String getStoredDirecturl(final DownloadLink link) {
        if (PluginJsonConfig.get(this.getConfigInterface()).getDownloadMode() == DownloadMode.API) {
            return link.getStringProperty(PROPERTY_DIRECTURL_API);
        } else {
            return link.getStringProperty(PROPERTY_DIRECTURL_WEBSITE);
        }
    }

    private void setStoredDirecturl(final DownloadLink link, final String directurl) {
        if (PluginJsonConfig.get(this.getConfigInterface()).getDownloadMode() == DownloadMode.API) {
            link.setProperty(PROPERTY_DIRECTURL_API, directurl);
        } else {
            link.setProperty(PROPERTY_DIRECTURL_WEBSITE, directurl);
        }
    }

    public void checkAvailablestatusWebsite(final DownloadLink link, final boolean isDownload) throws Exception {
        /* In website mode we neither know whether or not a video is watermarked nor can we download it without watermark. */
        link.removeProperty(PROPERTY_HAS_WATERMARK);
        final String fid = getFID(link);
        prepBRWebsite(br);
        if (!link.getPluginPatternMatcher().matches(TYPE_VIDEO)) {
            /* 2nd + 3rd linktype which does not contain username --> Find username by finding original URL. */
            br.setFollowRedirects(false);
            br.getPage("https://m.tiktok.com/v/" + fid + ".html");
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                if (!redirect.matches(TYPE_VIDEO)) {
                    /* Redirect to unsupported URL -> Most likely mainpage -> Offline! */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* Set new URL so we do not have to handle that redirect next time. */
                link.setPluginPatternMatcher(redirect);
            }
        }
        if (PluginJsonConfig.get(this.getConfigInterface()).isEnableFastLinkcheck() && !isDownload) {
            br.getPage("https://www." + this.getHost() + "/oembed?url=" + Encoding.urlEncode("https://www." + this.getHost() + "/video/" + fid));
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (isBotProtectionActive(this.br)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Captcha-blocked");
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
            final String status_msg = (String) entries.get("status_msg");
            final String type = (String) entries.get("type");
            if (!"video".equalsIgnoreCase(type)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!StringUtils.isEmpty(status_msg)) {
                /* {"status_msg":"Something went wrong"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String title = (String) entries.get("title");
            if (!StringUtils.isEmpty(title) && StringUtils.isEmpty(link.getComment())) {
                link.setComment(title);
            }
        } else {
            String description = null;
            final boolean useWebsiteEmbed = true;
            /* 2021-04-09: Don't use the website-way as their bot protection kicks in right away! */
            final boolean useWebsite = false;
            String dllink = null;
            if (useWebsite) {
                br.getPage(link.getPluginPatternMatcher());
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.containsHTML("pageDescKey\\s*=\\s*'user_verify_page_description';|class=\"verify-wrap\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Captcha-blocked");
                }
                final String videoJson = br.getRegex("crossorigin=\"anonymous\">(.*?)</script>").getMatch(0);
                Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(videoJson);
                entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/itemInfo/itemStruct");
                /* 2020-10-12: Hmm reliably checking for offline is complicated so let's try this instead ... */
                if (entries == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String createDate = Long.toString(JavaScriptEngineFactory.toLong(entries.get("createTime"), 0));
                description = (String) entries.get("desc");
                final Map<String, Object> videoInfo = (Map<String, Object>) entries.get("itemInfos");
                dllink = (String) videoInfo.get("downloadAddr");
                if (StringUtils.isEmpty(dllink)) {
                    dllink = (String) videoInfo.get("playAddr");
                }
                /* 2020-10-26: Doesn't work anymore, returns 403 */
                if (entries.containsKey("author")) {
                    final Map<String, Object> authorInfos = (Map<String, Object>) entries.get("author");
                    final String username = (String) authorInfos.get("uniqueId");
                    if (!StringUtils.isEmpty(username)) {
                        link.setProperty(PROPERTY_USERNAME, username);
                    }
                }
                if (!StringUtils.isEmpty(createDate)) {
                    link.setProperty(PROPERTY_DATE, convertDateFormat(createDate));
                }
                if (dllink == null && isDownload) {
                    /* Fallback */
                    dllink = generateDownloadurlOld(link);
                }
            } else if (useWebsiteEmbed) {
                /* Old version: https://www.tiktok.com/embed/<videoID> */
                // br.getPage(String.format("https://www.tiktok.com/embed/%s", fid));
                /* Alternative URL: https://www.tiktok.com/node/embed/render/<videoID> */
                /*
                 * 2021-04-09: Without accessing their website before (= fetches important cookies), we won't be able to use our final
                 * downloadurl!!
                 */
                final boolean useOEmbedToGetCookies = true;
                if (useOEmbedToGetCookies) {
                    /* 2021-04-09: Both ways will work fine but this one is faster and more elegant. */
                    br.getPage("https://www." + this.getHost() + "/oembed?url=" + Encoding.urlEncode("https://www." + this.getHost() + "/video/" + fid));
                } else {
                    br.getPage(link.getPluginPatternMatcher());
                }
                /* Required headers! */
                final Browser brc = this.br.cloneBrowser();
                brc.getHeaders().put("sec-fetch-dest", "iframe");
                brc.getHeaders().put("sec-fetch-mode", "navigate");
                // brc.getHeaders().put("sec-fetch-site", "cross-site");
                // brc.getHeaders().put("upgrade-insecure-requests", "1");
                // brc.getHeaders().put("Referer", link.getPluginPatternMatcher());
                brc.getPage("https://www." + this.getHost() + "/embed/v2/" + fid);
                if (brc.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (brc.containsHTML("pageDescKey\\s*=\\s*'user_verify_page_description';|class=\"verify-wrap\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Captcha-blocked");
                }
                final String videoJson = brc.getRegex("crossorigin=\"anonymous\">(.*?)</script>").getMatch(0);
                final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(videoJson);
                final Map<String, Object> videoData = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "props/pageProps/videoData");
                /* 2020-10-12: Hmm reliably checking for offline is complicated so let's try this instead ... */
                if (videoData == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> itemInfos = (Map<String, Object>) videoData.get("itemInfos");
                // entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "videoData/itemInfos");
                final String createDate = Long.toString(JavaScriptEngineFactory.toLong(itemInfos.get("createTime"), 0));
                description = (String) itemInfos.get("text");
                dllink = (String) JavaScriptEngineFactory.walkJson(itemInfos, "video/urls/{0}");
                /* Always look for username --> Username given inside URL which user added can be wrong! */
                final Object authorInfosO = videoData.get("authorInfos");
                if (authorInfosO != null) {
                    final Map<String, Object> authorInfos = (Map<String, Object>) authorInfosO;
                    final String username = (String) authorInfos.get("uniqueId");
                    if (!StringUtils.isEmpty(username)) {
                        link.setProperty(PROPERTY_USERNAME, username);
                    }
                }
                /* Set more Packagizer properties */
                final Object diggCountO = itemInfos.get("diggCount");
                if (diggCountO != null) {
                    setLikeCount(link, (Number) diggCountO);
                }
                final Object playCountO = itemInfos.get("playCount");
                if (playCountO != null) {
                    setPlayCount(link, (Number) playCountO);
                }
                final Object shareCountO = itemInfos.get("shareCount");
                if (shareCountO != null) {
                    setShareCount(link, (Number) shareCountO);
                }
                final Object commentCountO = itemInfos.get("commentCount");
                if (commentCountO != null) {
                    setCommentCount(link, (Number) commentCountO);
                }
                if (!StringUtils.isEmpty(createDate)) {
                    link.setProperty(PROPERTY_DATE, convertDateFormat(createDate));
                }
                if (dllink == null && isDownload) {
                    /* Fallback */
                    dllink = generateDownloadurlOld(link);
                }
            } else {
                /* Rev. 40928 and earlier */
                dllink = generateDownloadurlOld(link);
            }
            setDescriptionAndHashtags(link, description);
            if (!StringUtils.isEmpty(dllink)) {
                this.setStoredDirecturl(link, dllink);
            }
        }
        link.setAvailable(true);
        setFilename(link);
    }

    public void checkAvailablestatusAPI(final DownloadLink link, final boolean isDownload) throws Exception {
        prepBRAPI(br);
        final UrlQuery query = getAPIQuery();
        query.add("aweme_id", getFID(link));
        /* Alternative check for videos not available without feed-context: same request with path == '/feed' */
        accessAPI(br, "/aweme/detail", query);
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> aweme_detail = (Map<String, Object>) entries.get("aweme_detail");
        if (aweme_detail == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        parseFileInfoAPI(link, aweme_detail);
        if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static void parseFileInfoAPI(final DownloadLink link, final Map<String, Object> aweme_detail) throws PluginException {
        final Map<String, Object> status = (Map<String, Object>) aweme_detail.get("status");
        if ((Boolean) status.get("is_delete")) {
            link.setAvailable(false);
            return;
        }
        link.setProperty(PROPERTY_DATE, new SimpleDateFormat("yyyy-MM-dd").format(new Date(((Number) aweme_detail.get("create_time")).longValue() * 1000)));
        final Map<String, Object> statistics = (Map<String, Object>) aweme_detail.get("statistics");
        final Map<String, Object> video = (Map<String, Object>) aweme_detail.get("video");
        final Map<String, Object> author = (Map<String, Object>) aweme_detail.get("author");
        link.setProperty(PROPERTY_USERNAME, author.get("unique_id").toString());
        setDescriptionAndHashtags(link, aweme_detail.get("desc").toString());
        final Boolean has_watermark = (Boolean) video.get("has_watermark");
        Map<String, Object> downloadInfo = (Map<String, Object>) video.get("download_addr");
        if (downloadInfo == null) {
            /* Fallback/old way */
            final String downloadJson = video.get("misc_download_addrs").toString();
            final Map<String, Object> misc_download_addrs = JSonStorage.restoreFromString(downloadJson, TypeRef.HASHMAP);
            downloadInfo = (Map<String, Object>) misc_download_addrs.get("suffix_scene");
        }
        if (has_watermark || ((Boolean) aweme_detail.get("prevent_download") && downloadInfo == null)) {
            /* Get stream downloadurl because it comes without watermark */
            if (has_watermark) {
                link.setProperty(PROPERTY_HAS_WATERMARK, true);
            } else {
                link.removeProperty(PROPERTY_HAS_WATERMARK);
            }
            link.setProperty(PROPERTY_DIRECTURL_API, JavaScriptEngineFactory.walkJson(video, "play_addr/url_list/{0}"));
            if (downloadInfo != null) {
                /**
                 * Set filesize of download-version because streaming- and download-version are nearly identical. </br>
                 * If a video is watermarked and downloads are prohibited both versions should be identical.
                 */
                link.setDownloadSize(((Number) downloadInfo.get("data_size")).longValue());
            }
        } else {
            /* Grab official downloadlink whenever possible because this video doesn't come with a watermark. */
            link.setProperty(PROPERTY_DIRECTURL_API, JavaScriptEngineFactory.walkJson(downloadInfo, "url_list/{0}").toString());
            link.setVerifiedFileSize(((Number) downloadInfo.get("data_size")).longValue());
            link.removeProperty(PROPERTY_HAS_WATERMARK);
        }
        setLikeCount(link, (Number) statistics.get("digg_count"));
        setPlayCount(link, (Number) statistics.get("play_count"));
        setShareCount(link, (Number) statistics.get("share_count"));
        setCommentCount(link, (Number) statistics.get("comment_count"));
        link.setAvailable(true);
        setFilename(link);
    }

    public static void accessAPI(final Browser br, final String path, final UrlQuery query) throws IOException {
        br.getPage(API_BASE + path + "/" + "?" + query.toString());
    }

    public static Browser prepBRWebsite(final Browser br) {
        return br;
    }

    public static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("User-Agent", String.format("com.ss.android.ugc.trill/%s (Linux; U; Android 10; en_US; Pixel 4; Build/QQ3A.200805.001; Cronet/58.0.2991.0)", API_VERSION_CODE));
        // br.getHeaders().put("User-Agent", "okhttp");
        br.getHeaders().put("Accept", "application/json");
        br.setCookie(API_BASE, "odin_tt", generateRandomString("0123456789abcdef", 160));
        return br;
    }

    public static UrlQuery getAPIQuery() {
        final UrlQuery query = new UrlQuery();
        query.add("version_name", API_VERSION_NAME);
        query.add("version_code", API_VERSION_CODE);
        query.add("build_number", API_VERSION_NAME);
        query.add("manifest_version_code", API_VERSION_CODE);
        query.add("update_version_code", API_VERSION_CODE);
        query.add("openudid", generateRandomString("0123456789abcdef", 16));
        query.add("uuid", generateRandomString("0123456789", 16));
        query.add("_rticket", Long.toString(System.currentTimeMillis()));
        query.add("ts", Long.toString(System.currentTimeMillis() / 1000));
        query.add("device_brand", "Google");
        query.add("device_type", Encoding.urlEncode("Pixel 4"));
        query.add("device_platform", "android");
        query.add("resolution", "1080%2A1920");
        query.add("dpi", "420");
        query.add("os_version", "10");
        query.add("os_api", "29");
        query.add("carrier_region", "US");
        query.add("sys_region", "US");
        query.add("region", "US");
        query.add("app_name", "trill");
        query.add("app_language", "en");
        query.add("language", "en");
        query.add("timezone_name", Encoding.urlEncode("America/New_York"));
        query.add("timezone_offset", "-14400");
        query.add("channel", "googleplay");
        query.add("ac", "wifi");
        query.add("mcc_mnc", "310260");
        query.add("is_my_cn", "0");
        query.add("aid", "1180");
        query.add("ssmix", "a");
        query.add("as", "a1qwert123");
        query.add("cp", "cbfhckdckkde1");
        return query;
    }

    public static String generateRandomString(final String chars, final int length) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(new Random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    public static String getUsername(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_USERNAME)) {
            return TiktokComCrawler.sanitizeUsername(link.getStringProperty(PROPERTY_USERNAME));
        } else if (link.getPluginPatternMatcher().matches(TYPE_VIDEO)) {
            return TiktokComCrawler.sanitizeUsername(new Regex(link.getPluginPatternMatcher(), TYPE_VIDEO).getMatch(0));
        } else {
            return null;
        }
    }

    private static String getDateFormatted(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_DATE)) {
            return link.getStringProperty(PROPERTY_DATE);
        } else if (link.hasProperty(PROPERTY_DATE_LAST_MODIFIED_HEADER)) {
            return convertDateFormat(link.getStringProperty(PROPERTY_DATE_LAST_MODIFIED_HEADER));
        } else {
            return null;
        }
    }

    public static final void setLikeCount(final DownloadLink link, final Number number) {
        link.setProperty(PROPERTY_LIKE_COUNT + "_string", toHumanReadableNumber(number));
        link.setProperty(PROPERTY_LIKE_COUNT, number.longValue());
    }

    public static final void setPlayCount(final DownloadLink link, final Number number) {
        link.setProperty(PROPERTY_PLAY_COUNT + "_string", toHumanReadableNumber(number));
        link.setProperty(PROPERTY_PLAY_COUNT, number.longValue());
    }

    public static final void setShareCount(final DownloadLink link, final Number number) {
        link.setProperty(PROPERTY_SHARE_COUNT + "_string", toHumanReadableNumber(number));
        link.setProperty(PROPERTY_SHARE_COUNT, number.longValue());
    }

    public static final void setCommentCount(final DownloadLink link, final Number number) {
        link.setProperty(PROPERTY_COMMENT_COUNT + "_string", toHumanReadableNumber(number));
        link.setProperty(PROPERTY_COMMENT_COUNT, number.longValue());
    }

    public static void setDescriptionAndHashtags(final DownloadLink link, final String description) {
        if (!StringUtils.isEmpty(description)) {
            final String[] hashtags = new Regex(description, "(#[^# ]+)").getColumn(0);
            if (hashtags.length > 0) {
                final StringBuilder sb = new StringBuilder();
                for (final String hashtag : hashtags) {
                    sb.append(hashtag);
                }
                /* Set Packagizer property */
                link.setProperty(PROPERTY_HASHTAGS, sb.toString());
            }
            if (StringUtils.isEmpty(link.getComment())) {
                link.setComment(description);
            }
            /* Set Packagizer property */
            link.setProperty(PROPERTY_DESCRIPTION, description);
        }
    }

    public static boolean isBotProtectionActive(final Browser br) {
        return br.containsHTML("pageDescKey\\s*=\\s*'user_verify_page_description';|class=\"verify-wrap\"");
    }

    private String generateDownloadurlOld(final DownloadLink link) throws IOException {
        this.br.getPage("https://www." + this.getHost() + "/node/video/playwm?id=" + this.getFID(link));
        return new URL(br.toString()).toString();
    }

    public static String convertDateFormat(final String sourceDateString) {
        if (sourceDateString == null) {
            return null;
        }
        String result = null;
        final SimpleDateFormat target_format = new SimpleDateFormat("yyyy-MM-dd");
        if (sourceDateString.matches("\\d+")) {
            /* Timestamp */
            final Date theDate = new Date(Long.parseLong(sourceDateString) * 1000);
            result = target_format.format(theDate);
        } else if (sourceDateString.matches("[A-Za-z]{3}, \\d{1,2} [A-Za-z]{3} \\d{1,2}:\\d{1,2}:\\d{1,2} [A-Z]+")) {
            /* E.g. "Last-Modified" header */
            final SimpleDateFormat source_format = new SimpleDateFormat("DDD, dd MMM, yyyy HH:mm:ss ZZZ", Locale.ENGLISH);
            Date date;
            try {
                date = source_format.parse(sourceDateString);
                result = target_format.format(date);
            } catch (final ParseException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            final String sourceDatePart = new Regex(sourceDateString, "^[A-Za-z]+, (\\d{1,2} \\w+ \\d{4})").getMatch(0);
            if (sourceDatePart == null) {
                return null;
            }
            final SimpleDateFormat source_format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            try {
                try {
                    final Date date = source_format.parse(sourceDatePart);
                    result = target_format.format(date);
                } catch (Throwable e) {
                }
            } catch (final Throwable e) {
                return null;
            }
        }
        return result;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final DownloadMode mode = PluginJsonConfig.get(this.getConfigInterface()).getDownloadMode();
        if (!link.hasProperty(PROPERTY_LAST_USED_DOWNLOAD_MODE) || !StringUtils.equals(link.getStringProperty(PROPERTY_LAST_USED_DOWNLOAD_MODE), mode.name())) {
            /* Prevent file corruption */
            logger.info("Resetting progress because user has downloaded using other download mode before");
            link.setChunksProgress(null);
            link.setVerifiedFileSize(-1);
        }
        /* Remember last used download mode */
        link.setProperty(PROPERTY_LAST_USED_DOWNLOAD_MODE, mode.name());
        if (this.attemptStoredDownloadurlDownload(link)) {
            logger.info("Using stored directurl for downloading");
        } else {
            requestFileInformation(link, true);
            final String dllink = this.getStoredDirecturl(link);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!this.attemptStoredDownloadurlDownload(link)) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
                }
            }
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = this.getStoredDirecturl(link);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Referer", "https://www." + this.getHost() + "/");
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, RESUME, MAXCHUNKS);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                this.setStoredDirecturl(link, null);
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
        return PluginJsonConfig.get(getConfigInterface()).getMaxSimultaneousDownloads();
    }

    @Override
    public Class<TiktokConfig> getConfigInterface() {
        return TiktokConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            link.removeProperty(PROPERTY_HAS_WATERMARK);
        }
    }
}