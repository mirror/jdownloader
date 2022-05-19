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
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.TiktokConfig;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tiktok.com" }, urls = { "https?://(?:www\\.)?tiktok\\.com/((@[^/]+)/video/|embed/)(\\d+)|https?://m\\.tiktok\\.com/v/(\\d+)\\.html" })
public class TiktokCom extends PluginForHost {
    public TiktokCom(PluginWrapper wrapper) {
        super(wrapper);
        try {
            Browser.setRequestIntervalLimitGlobal("tiktok.com", true, 1000);
        } catch (final Throwable e) {
        }
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

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "/(?:video|v|embed)/(\\d+)").getMatch(0);
    }

    private String              dllink                             = null;
    public static final String  PROPERTY_DIRECTURL                 = "directurl";
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
    private static final String TYPE_VIDEO                         = "https?://[^/]+/(@[^/]+)/video/(\\d+).*?";

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
                this.dllink = (String) videoInfo.get("downloadAddr");
                if (StringUtils.isEmpty(this.dllink)) {
                    this.dllink = (String) videoInfo.get("playAddr");
                }
                /* 2020-10-26: Doesn't work anymore, returns 403 */
                if (entries.containsKey("author")) {
                    final Map<String, Object> authorInfos = (Map<String, Object>) entries.get("author");
                    String username = (String) authorInfos.get("uniqueId");
                    if (!StringUtils.isEmpty(username) && !username.startsWith("@")) {
                        username = "@" + username;
                    }
                    if (!StringUtils.isEmpty(username)) {
                        link.setProperty(PROPERTY_USERNAME, username);
                    }
                }
                if (!StringUtils.isEmpty(createDate)) {
                    link.setProperty(PROPERTY_DATE, convertDateFormat(createDate));
                }
                if (this.dllink == null && isDownload) {
                    /* Fallback */
                    this.dllink = generateDownloadurlOld(link);
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
                    String username = (String) authorInfos.get("uniqueId");
                    if (!StringUtils.isEmpty(username)) {
                        if (!username.startsWith("@")) {
                            username = "@" + username;
                        }
                        link.setProperty(PROPERTY_USERNAME, username);
                    }
                }
                /* Set more Packagizer properties */
                if (itemInfos.containsKey("diggCount")) {
                    final Number number = (Number) itemInfos.get("diggCount");
                    setLikeCount(link, number);
                }
                if (itemInfos.containsKey("playCount")) {
                    final Number number = (Number) itemInfos.get("playCount");
                    setPlayCount(link, number);
                }
                if (itemInfos.containsKey("shareCount")) {
                    final Number number = (Number) itemInfos.get("shareCount");
                    setShareCount(link, number);
                }
                if (itemInfos.containsKey("commentCount")) {
                    final Number number = (Number) itemInfos.get("commentCount");
                    setCommentCount(link, number);
                }
                if (!StringUtils.isEmpty(createDate)) {
                    link.setProperty(PROPERTY_DATE, convertDateFormat(createDate));
                }
                if (this.dllink == null && isDownload) {
                    /* Fallback */
                    this.dllink = generateDownloadurlOld(link);
                }
            } else {
                /* Rev. 40928 and earlier */
                this.dllink = generateDownloadurlOld(link);
            }
            setDescriptionAndHashtags(link, description);
            br.setFollowRedirects(true);
            if (!StringUtils.isEmpty(dllink) && !isDownload) {
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
                        /* Save it for later usage */
                        link.setProperty(PROPERTY_DIRECTURL, this.dllink);
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
        }
        String filename = "";
        String dateFormatted = getDateFormatted(link);
        if (dateFormatted != null) {
            filename = dateFormatted;
        }
        final String username = getUsername(link);
        if (!StringUtils.isEmpty(username)) {
            filename += "_" + username;
        }
        filename += "_" + fid + ".mp4";
        /* Only set final filename if ALL information is available! */
        if (link.hasProperty(PROPERTY_DATE) && !StringUtils.isEmpty(username)) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    private String getUsername(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_USERNAME)) {
            return link.getStringProperty(PROPERTY_USERNAME);
        } else if (link.getPluginPatternMatcher().matches(TYPE_VIDEO)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_VIDEO).getMatch(0);
        } else {
            return null;
        }
    }

    private String getDateFormatted(final DownloadLink link) {
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

    private String convertDateFormat(final String sourceDateString) {
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
        doFree(link);
    }

    private void doFree(final DownloadLink link) throws Exception, PluginException {
        if (this.attemptStoredDownloadurlDownload(link)) {
            logger.info("Using stored directurl for downloading");
        } else {
            requestFileInformation(link, true);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Referer", "https://www." + this.getHost() + "/");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, RESUME, MAXCHUNKS);
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
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
                }
            }
            /* Save it for later usage */
            link.setProperty(PROPERTY_DIRECTURL, this.dllink);
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
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
                link.removeProperty(PROPERTY_DIRECTURL);
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
    public void resetDownloadlink(DownloadLink link) {
    }
}