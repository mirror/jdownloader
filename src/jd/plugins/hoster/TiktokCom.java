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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.config.TiktokConfig;
import org.jdownloader.plugins.components.config.TiktokConfig.MediaCrawlMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.TiktokComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tiktok.com" }, urls = { "https?://(?:www\\.)?tiktok\\.com/((@[^/]+)/video/|embed/)(\\d+)|https?://m\\.tiktok\\.com/v/(\\d+)\\.html" })
public class TiktokCom extends PluginForHost {
    public TiktokCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://tiktok.com/");
    }

    @Override
    public void init() {
        setRequestLimits();
    }

    public static void setRequestLimits() {
        Browser.setRequestIntervalLimitGlobal("tiktok.com", true, 1000);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
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

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final Account account) {
        /* 2019-07-10: More chunks possible but that would not be such a good idea! */
        return 1;
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getContentID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid + "_" + getType(link) + "_" + getIndexNumber(link);
        } else {
            return super.getLinkID(link);
        }
    }

    public static String getContentID(final DownloadLink link) {
        return getContentID(link.getPluginPatternMatcher());
    }

    public static String getContentID(final String url) {
        return new Regex(url, "https?://.*/(?:video|v|embed)/(\\d+)").getMatch(0);
    }

    // private String dllink = null;
    public static final String PROPERTY_DIRECTURL_WEBSITE                     = "directurl";
    public static final String PROPERTY_DIRECTURL_API                         = "directurl_api";
    public static final String PROPERTY_USERNAME                              = "username";
    public static final String PROPERTY_USER_ID                               = "user_id";
    public static final String PROPERTY_VIDEO_ID                              = "videoid";
    public static final String PROPERTY_DATE                                  = "date";
    public static final String PROPERTY_ATTEMPTED_TO_OBTAIN_DATE_FROM_WEBSITE = "attempted_to_obtain_date_from_website";
    public static final String PROPERTY_DATE_FROM_WEBSITE                     = "date_from_website";
    public static final String PROPERTY_DATE_LAST_MODIFIED_HEADER             = "date_last_modified_header";
    public static final String PROPERTY_DESCRIPTION                           = "description";
    public static final String PROPERTY_HASHTAGS                              = "hashtags";
    public static final String PROPERTY_LIKE_COUNT                            = "like_count";
    public static final String PROPERTY_PLAY_COUNT                            = "play_count";
    public static final String PROPERTY_SHARE_COUNT                           = "share_count";
    public static final String PROPERTY_COMMENT_COUNT                         = "comment_count";
    public static final String PROPERTY_HAS_WATERMARK                         = "has_watermark";
    public static final String PROPERTY_FORCE_API                             = "force_api";
    public static final String PROPERTY_LAST_USED_DOWNLOAD_MODE               = "last_used_download_mode";
    public static final String PROPERTY_ALLOW_HEAD_REQUEST                    = "allow_head_request";
    public static final String PROPERTY_TYPE                                  = "type";
    public static final String PROPERTY_INDEX                                 = "index";
    public static final String PROPERTY_INDEX_MAX                             = "index_max";
    public static final String TYPE_AUDIO                                     = "audio";
    public static final String TYPE_VIDEO                                     = "video";
    public static final String TYPE_PICTURE                                   = "picture";
    public static final String PATTERN_VIDEO                                  = "(?i)https?://[^/]+/@([^/]+)/video/(\\d+).*";
    /* API related stuff */
    public static final String API_CLIENT                                     = "trill";
    public static final String API_AID                                        = "1180";
    public static final String API_BASE                                       = "https://api16-normal-c-useast1a.tiktokv.com/aweme/v1";
    public static final String API_VERSION_NAME                               = "25.6.2";
    public static final String API_VERSION_CODE                               = "250602";
    private final String       PROPERTY_ACCOUNT_HAS_SHOWN_DOWNLOAD_MODE_HINT  = "has_shown_download_mode_hint";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false, false);
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

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload, final boolean forceFetchNewDirecturl) throws Exception {
        if (account != null) {
            /* Login whenever possible. */
            this.login(account, false);
        }
        final String fid = getContentID(link);
        if (fid == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(PROPERTY_VIDEO_ID, fid);
        if (!link.isNameSet()) {
            /* Set fallback-filename. Use .mp4 file-extension as most items are expected to be videos. */
            link.setName(fid + ".mp4");
        }
        String dllink = null;
        dllink = getStoredDirecturl(link);
        if (dllink == null || forceFetchNewDirecturl) {
            logger.info("Obtaining fresh directurl");
            final TiktokComCrawler crawler = (TiktokComCrawler) this.getNewPluginForDecryptInstance(this.getHost());
            final ArrayList<DownloadLink> results = crawler.crawlSingleMedia(this, new CryptedLink(link.getPluginPatternMatcher()), account, true);
            final String storedType = link.getStringProperty(PROPERTY_TYPE);
            DownloadLink result = null;
            final String currentFilename = link.getName();
            boolean resultsContainVideo = false;
            DownloadLink audioFallbackCandidate = null;
            for (final DownloadLink thisresult : results) {
                if (StringUtils.equals(this.getLinkID(thisresult), this.getLinkID(link))) {
                    result = thisresult;
                    break;
                } else if (StringUtils.equals(getType(thisresult), TYPE_AUDIO)) {
                    audioFallbackCandidate = thisresult;
                } else if (StringUtils.equals(getType(thisresult), TYPE_VIDEO)) {
                    resultsContainVideo = true;
                }
            }
            if (result == null && storedType == null && !resultsContainVideo && StringUtils.endsWithCaseInsensitive(currentFilename, ".mp4") && audioFallbackCandidate != null) {
                logger.info("Workaround for legacy elements: Download .mp4 file as .mp3 because there is no video available for this item");
                result = audioFallbackCandidate;
            }
            if (result == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = getStoredDirecturl(result);
            link.setProperties(result.getProperties());
            /* Properties might have changed -> Set final filename again. */
            setFilename(link);
        }
        if (!StringUtils.isEmpty(dllink) && !link.isSizeSet() && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(true);
                prepareDownloadHeaders(link, brc);
                if (allowsHeadRequest(link)) {
                    con = brc.openHeadConnection(dllink);
                } else {
                    con = brc.openGetConnection(dllink);
                }
                if (con.getResponseCode() == 405 && con.getRequestMethod() == RequestMethod.HEAD) {
                    logger.info("Fallback: Attempt GET-request");
                    prepareDownloadHeaders(link, brc);
                    con = brc.openGetConnection(dllink);
                    link.setProperty(PROPERTY_ALLOW_HEAD_REQUEST, false);
                }
                if (!this.looksLikeDownloadableContent(con)) {
                    brc.followConnection(true);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken media file?", 10 * 60 * 1000l);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                missingDateFilenameLastResortHandling(link, con);
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    /** Prepare headers for usage of tiktok media direct-URLs. */
    private void prepareDownloadHeaders(final DownloadLink link, final Browser br) {
        br.getHeaders().put("Referer", "https://www." + this.getHost() + "/");
        br.getHeaders().put("Origin", "https://www." + this.getHost());
    }

    private boolean allowsHeadRequest(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_ALLOW_HEAD_REQUEST, false);
    }

    /**
     * Handling that tries to find date via website if needed. Call this before download but not during availablecheck as it would prolong
     * the check!
     */
    private void missingDateFilenamePreDownloadHandling(final DownloadLink link, final Account account) {
        if (!link.hasProperty(PROPERTY_DATE) && !link.hasProperty(PROPERTY_ATTEMPTED_TO_OBTAIN_DATE_FROM_WEBSITE)) {
            logger.info("Trying to find video create date via date workaround");
            try {
                final Browser br3 = br.cloneBrowser();
                br3.getPage(link.getPluginPatternMatcher());
                if (getAndSetDateFromWebsite(this, br3, link) != null) {
                    /*
                     * Filename has already been set before but date was not available --> Set filename again as date information is given
                     * now.
                     */
                    logger.info("Re-generating filename as date was missing and has been found now");
                    setFilename(link);
                }
            } catch (final Exception ignore) {
                logger.log(ignore);
                logger.info("Failed to obtain date via website due to Exception");
            }
        }
    }

    private void missingDateFilenameLastResortHandling(final DownloadLink link, final URLConnectionAdapter con) {
        if (getDateFormatted(link) == null && con != null) {
            final String lastModifiedHeaderValue = con.getRequest().getResponseHeader("Last-Modified");
            if (lastModifiedHeaderValue != null) {
                link.setProperty(PROPERTY_DATE_LAST_MODIFIED_HEADER, lastModifiedHeaderValue);
                /*
                 * Filename has already been set before but date was not available --> Set filename again as date information is given now.
                 */
                logger.info("Re-generating filename as date was missing and has been found now");
                setFilename(link);
            }
        }
    }

    private static boolean useAPI(final DownloadLink link) {
        if (configUseAPI() || link.hasProperty(PROPERTY_FORCE_API)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns true for items that can only be fetched via API. </br>
     * Returns false for items which can also be fetched via website.
     */
    private final boolean needsAPIUsage(final DownloadLink link) {
        if (isVideo(link)) {
            return false;
        } else {
            return true;
        }
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
        filename += "_" + getContentID(link);
        final int index_max = getIndexMaxNumber(link);
        if (index_max > 0) {
            /* Append index to filenames */
            filename += "_" + StringUtils.formatByPadLength(index_max + 1, getIndexNumber(link) + 1);
        }
        final String directurl = getStoredDirecturl(link);
        final String extByURL = Plugin.getFileNameExtensionFromURL(directurl);
        final String type = getType(link);
        if (type.equals(TYPE_VIDEO)) {
            filename += ".mp4";
        } else if (type.equals(TYPE_AUDIO) && extByURL == null) {
            filename += ".mp3";
        } else {
            filename += extByURL;
        }
        /* Only set final filename if ALL information is available! */
        if (dateFormatted != null && !StringUtils.isEmpty(username)) {
            link.setFinalFileName(filename);
        } else {
            link.setName(filename);
        }
    }

    public static MediaCrawlMode getDownloadMode() {
        final MediaCrawlMode mode = PluginJsonConfig.get(TiktokConfig.class).getMediaCrawlMode();
        if (false) {
            // see https://svn.jdownloader.org/issues/90292
            return MediaCrawlMode.WEBSITE;
        } else {
            return mode;
        }
    }

    private static boolean configUseAPI() {
        final MediaCrawlMode mode = getDownloadMode();
        // if (mode == DownloadMode.API || mode == DownloadMode.API_HD) {
        if (mode == MediaCrawlMode.API) {
            return true;
        } else {
            return false;
        }
    }

    public static String getType(final DownloadLink link) {
        final String storedType = link.getStringProperty(PROPERTY_TYPE);
        if (storedType != null) {
            return storedType;
        } else {
            /* Old items added in revisions in which we were only supporting single video items. */
            return TYPE_VIDEO;
        }
    }

    public static boolean isVideo(final DownloadLink link) {
        final String storedType = link.getStringProperty(PROPERTY_TYPE);
        if (storedType == null) {
            /* Older items (only video) -> Those did not have the "TYPE" property set. */
            return true;
        } else if (StringUtils.equals(getType(link), TYPE_VIDEO)) {
            return true;
        } else {
            return false;
        }
    }

    public static int getIndexNumber(final DownloadLink link) {
        return link.getIntegerProperty(PROPERTY_INDEX, 0);
    }

    public static int getIndexMaxNumber(final DownloadLink link) {
        return link.getIntegerProperty(PROPERTY_INDEX_MAX, 0);
    }

    private static String getStoredDirecturl(final DownloadLink link) {
        final Boolean forceAPI = (Boolean) link.getProperty(PROPERTY_FORCE_API);
        String directlink;
        if (configUseAPI()) {
            directlink = link.getStringProperty(PROPERTY_DIRECTURL_API);
        } else if (Boolean.TRUE.equals(forceAPI)) {
            directlink = link.getStringProperty(PROPERTY_DIRECTURL_API);
        } else {
            directlink = link.getStringProperty(PROPERTY_DIRECTURL_WEBSITE);
        }
        if (StringUtils.isEmpty(directlink) && forceAPI == null) {
            /* E.g. items that have been added via crawler in website mode while download mode is set to API. */
            directlink = link.getStringProperty(PROPERTY_DIRECTURL_WEBSITE);
        }
        return directlink;
    }

    @Deprecated
    public void checkAvailablestatusWebsite(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        if (account != null) {
            this.login(account, false);
        }
        /* In website mode we neither know whether or not a video is watermarked nor can we download it without watermark. */
        link.removeProperty(PROPERTY_HAS_WATERMARK);
        final String fid = getContentID(link);
        prepBRWebsite(br);
        boolean accessedContentURL = false;
        if (!link.getPluginPatternMatcher().matches(PATTERN_VIDEO)) {
            /* 2nd + 3rd linktype which does not contain username --> Find username by finding original URL. */
            br.setFollowRedirects(true);
            br.getPage("https://m.tiktok.com/v/" + fid + ".html");
            if (!this.canHandle(br.getURL())) {
                /* Redirect to unsupported URL -> Most likely mainpage -> Offline! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.info("Old URL: " + link.getPluginPatternMatcher() + " | New URL: " + br.getURL());
            /* Set new URL so we do not have to handle that redirect next time. */
            link.setPluginPatternMatcher(br.getURL());
            accessedContentURL = true;
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
            final String description = (String) entries.get("title");
            if (!StringUtils.isEmpty(description)) {
                TiktokCom.setDescriptionAndHashtags(link, description);
            }
        } else {
            String description = null;
            final boolean useWebsiteEmbed = true;
            /**
             * 2021-04-09: Avoid using the website-way as their bot protection may kick in right away! </br>
             * When using an account and potentially downloading private videos however, we can't use the embed way.
             */
            String dllink = null;
            if (account != null) {
                if (!accessedContentURL) {
                    br.getPage(link.getPluginPatternMatcher());
                }
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.containsHTML("pageDescKey\\s*=\\s*'user_verify_page_description';|class=\"verify-wrap\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Captcha-blocked");
                }
                String videoJson = br.getRegex("crossorigin=\"anonymous\">\\s*(.*?)\\s*</script>").getMatch(0);
                if (videoJson == null) {
                    videoJson = br.getRegex("<script\\s*id[^>]*>\\s*(\\{.*?)\\s*</script>").getMatch(0);
                }
                final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(videoJson);
                final Map<String, Object> itemModule = (Map<String, Object>) entries.get("ItemModule");
                /* 2020-10-12: Hmm reliably checking for offline is complicated so let's try this instead ... */
                if (itemModule == null || itemModule.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> videoInfo = (Map<String, Object>) itemModule.entrySet().iterator().next().getValue();
                final String createDateTimestampStr = videoInfo.get("createTime").toString();
                description = (String) entries.get("desc");
                final Map<String, Object> downloadInfo = (Map<String, Object>) videoInfo.get("video");
                dllink = (String) downloadInfo.get("downloadAddr");
                if (StringUtils.isEmpty(dllink)) {
                    dllink = (String) downloadInfo.get("playAddr");
                }
                link.setProperty(PROPERTY_USERNAME, videoInfo.get("author"));
                if (!StringUtils.isEmpty(createDateTimestampStr)) {
                    link.setProperty(PROPERTY_DATE, convertDateFormat(createDateTimestampStr));
                }
                final Map<String, Object> stats = (Map<String, Object>) videoInfo.get("stats");
                final Object diggCountO = stats.get("diggCount");
                if (diggCountO != null) {
                    setLikeCount(link, (Number) diggCountO);
                }
                final Object shareCountO = stats.get("shareCount");
                if (shareCountO != null) {
                    setShareCount(link, (Number) shareCountO);
                }
                final Object playCountO = stats.get("playCount");
                if (playCountO != null) {
                    setPlayCount(link, (Number) playCountO);
                }
                final Object commentCountO = stats.get("commentCount");
                if (commentCountO != null) {
                    setCommentCount(link, (Number) commentCountO);
                }
                getAndSetDateFromWebsite(this, br, link);
                if (dllink == null) {
                    /* Fallback */
                    if (!isDownload) {
                        dllink = generateDownloadurlOld(br, fid);
                    }
                    link.setProperty(PROPERTY_ALLOW_HEAD_REQUEST, true);
                } else {
                    link.setProperty(PROPERTY_ALLOW_HEAD_REQUEST, false);
                }
            } else if (useWebsiteEmbed) {
                /* Old version: https://www.tiktok.com/embed/<videoID> */
                // br.getPage(String.format("https://www.tiktok.com/embed/%s", fid));
                /* Alternative URL: https://www.tiktok.com/node/embed/render/<videoID> */
                /*
                 * 2021-04-09: Without accessing their website before (= fetches important cookies), we won't be able to use our final
                 * downloadurl!!
                 */
                /* 2021-04-09: Both ways will work fine but the oembed one is faster and more elegant. */
                if (account != null) {
                    if (!accessedContentURL) {
                        br.getPage(link.getPluginPatternMatcher());
                    }
                } else {
                    br.getPage("https://www." + this.getHost() + "/oembed?url=" + Encoding.urlEncode("https://www." + this.getHost() + "/video/" + fid));
                }
                if (br.containsHTML("\"(?:status_msg|message)\"\\s*:\\s*\"Something went wrong\"")) {
                    // webmode not possible!? retry with api
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* Required headers! */
                final Browser brc = this.br.cloneBrowser();
                brc.getHeaders().put("sec-fetch-dest", "iframe");
                brc.getHeaders().put("sec-fetch-mode", "navigate");
                // brc.getHeaders().put("sec-fetch-site", "cross-site");
                // brc.getHeaders().put("upgrade-insecure-requests", "1");
                // brc.getHeaders().put("Referer", link.getPluginPatternMatcher());
                brc.getPage("https://www." + this.getHost() + "/embed/v2/" + fid);
                brc.followRedirect(); // without this we have different videoJson
                if (brc.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (brc.containsHTML("pageDescKey\\s*=\\s*'user_verify_page_description';|class=\"verify-wrap\"")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Captcha-blocked");
                }
                String videoJson = brc.getRegex("crossorigin=\"anonymous\">\\s*(.*?)\\s*</script>").getMatch(0);
                if (videoJson == null) {
                    videoJson = brc.getRegex("<script\\s*id[^>]*>\\s*(\\{.*?)\\s*</script>").getMatch(0);
                }
                final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(videoJson);
                Map<String, Object> videoData = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "props/pageProps/videoData");
                if (videoData == null) {
                    // different videoJson when we do not follow the embed/v2 redirect
                    final Map<String, Object> data = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "source/data/");
                    if (data != null) {
                        String key = null;
                        for (String keyEntry : data.keySet()) {
                            if (StringUtils.containsIgnoreCase(keyEntry, fid)) {
                                key = keyEntry;
                                break;
                            }
                        }
                        // key contains / separator, so we must use different walkJson here
                        videoData = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "source", "data", key, "videoData");
                    }
                }
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
                if (!StringUtils.isEmpty(createDate) && !"0".equals(createDate)) {
                    link.setProperty(PROPERTY_DATE, convertDateFormat(createDate));
                }
                if (dllink == null && isDownload) {
                    /* Fallback */
                    dllink = generateDownloadurlOld(br, fid);
                }
                link.setProperty(PROPERTY_ALLOW_HEAD_REQUEST, true);
            } else {
                /* Old deprecated handling of rev. 40928 and earlier */
                dllink = generateDownloadurlOld(br, fid);
                link.setProperty(PROPERTY_ALLOW_HEAD_REQUEST, true);
            }
            setDescriptionAndHashtags(link, description);
            if (!StringUtils.isEmpty(dllink)) {
                link.setProperty(PROPERTY_DIRECTURL_WEBSITE, dllink);
            }
        }
        link.setAvailable(true);
        setFilename(link);
    }

    public static String getAndSetDateFromWebsite(final Plugin plg, final Browser br, final DownloadLink link) {
        link.setProperty(PROPERTY_ATTEMPTED_TO_OBTAIN_DATE_FROM_WEBSITE, true);
        final String createDateStr[] = br.getRegex("(?i)</span><span style=\"margin:0 4px\">[^<]*</span><span>\\s*(\\d{4}-)?(\\d{1,2})-(\\d{1,2})\\s*</span></span></a>").getRow(0);
        if (createDateStr != null) {
            String date = "";
            if (createDateStr[0] == null) {
                final GregorianCalendar calendar = new GregorianCalendar();
                calendar.setTimeInMillis(System.currentTimeMillis());
                date = "" + calendar.get(Calendar.YEAR);
            } else {
                date = createDateStr[0];
            }
            date += "-" + StringUtils.fillPre(createDateStr[1], "0", 2);
            date += "-" + StringUtils.fillPre(createDateStr[2], "0", 2);
            plg.getLogger().info("Successfully found video create date in website html: " + date);
            link.setProperty(PROPERTY_DATE_FROM_WEBSITE, date);
            return date;
        } else {
            plg.getLogger().warning("Failed to find date via date workaround");
            return null;
        }
    }

    @Deprecated
    public void checkAvailablestatusAPI(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        prepBRAPI(br);
        final UrlQuery query = getAPIQuery();
        final String contentID = getContentID(link);
        query.add("aweme_id", contentID);
        accessAPI(br, "/aweme/detail", query);
        Map<String, Object> entries = null;
        Map<String, Object> aweme_detail = null;
        try {
            entries = restoreFromString(br.toString(), TypeRef.MAP);
            aweme_detail = (Map<String, Object>) entries.get("aweme_detail");
        } catch (final JSonMapperException jse) {
            /* Fallback */
            logger.info("Trying API /feed fallback");
            /* Alternative check for videos not available without feed-context: same request with path == '/feed' */
            prepBRAPI(br);
            /* Make sure that the next request will not contain a Referer header otherwise we'll get a blank page! */
            br.setCurrentURL("");
            accessAPI(br, "/feed", query);
            entries = restoreFromString(br.toString(), TypeRef.MAP);
            final List<Map<String, Object>> aweme_list = (List<Map<String, Object>>) entries.get("aweme_list");
            for (final Map<String, Object> aweme_detailTmp : aweme_list) {
                if (StringUtils.equals(aweme_detailTmp.get("aweme_id").toString(), contentID)) {
                    aweme_detail = aweme_detailTmp;
                    break;
                }
            }
        }
        if (aweme_detail == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        parseFileInfoAPI(this, link, aweme_detail);
        if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static void parseFileInfoAPI(final Plugin plugin, final DownloadLink link, final Map<String, Object> aweme_detail) throws PluginException {
        link.setProperty(PROPERTY_VIDEO_ID, aweme_detail.get("aweme_id").toString());
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
        final Boolean has_watermark = Boolean.TRUE.equals(video.get("has_watermark"));
        Map<String, Object> downloadInfo = (Map<String, Object>) video.get("download_addr");
        if (downloadInfo == null) {
            /* Fallback/old way */
            final String downloadJson = video.get("misc_download_addrs").toString();
            final Map<String, Object> misc_download_addrs = plugin.restoreFromString(downloadJson, TypeRef.MAP);
            downloadInfo = (Map<String, Object>) misc_download_addrs.get("suffix_scene");
        }
        final Map<String, Object> play_addr = (Map<String, Object>) video.get("play_addr");
        final boolean tryHDDownload = false;
        // if (PluginJsonConfig.get(TiktokConfig.class).getDownloadMode() == DownloadMode.API_HD) {
        if (tryHDDownload && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* User prefers to download HD version */
            /*
             * 2022-08-17: Look like HD versions have been disabled serverside see e.g.:
             * https://github.com/yt-dlp/yt-dlp/issues/4138#issuecomment-1217380819
             */
            /**
             * This is also possible using "https://api-h2.tiktokv.com/aweme/v1/play/" </br>
             * This is also possible using modified URLs in e.g.: play_addr_bytevc1/uri_list/{last_item} --> Or also any item inside any
             * "uri_list" which contains the "video_id" parameter which also typically matches play_addr/uri
             */
            link.setProperty(PROPERTY_DIRECTURL_API, String.format("https://api.tiktokv.com/aweme/v1/play/?video_id=%s&line=0&watermark=0&source=AWEME_DETAIL&is_play_url=1&ratio=default&improve_bitrate=1", play_addr.get("uri").toString()));
            /*
             * This way we can't know whether or not the video comes with watermark but usually this version will not contain a watermark.
             */
            link.removeProperty(PROPERTY_HAS_WATERMARK);
            /* We can't know the filesize of this video version in beforehand. */
            link.setVerifiedFileSize(-1);
        } else {
            /* Get non-HD directurl */
            String directurl = null;
            final Number data_size = downloadInfo != null ? (Number) downloadInfo.get("data_size") : null;
            if (has_watermark || (Boolean.TRUE.equals(aweme_detail.get("prevent_download")) && downloadInfo == null)) {
                /* Get stream downloadurl because it comes WITHOUT watermark anyways */
                if (has_watermark) {
                    link.setProperty(PROPERTY_HAS_WATERMARK, true);
                } else {
                    link.removeProperty(PROPERTY_HAS_WATERMARK);
                }
                directurl = (String) JavaScriptEngineFactory.walkJson(play_addr, "url_list/{0}");
                link.setProperty(PROPERTY_DIRECTURL_API, directurl);
                if (data_size != null) {
                    /**
                     * Set filesize of download-version because streaming- and download-version are nearly identical. </br>
                     * If a video is watermarked and downloads are prohibited both versions should be identical.
                     */
                    link.setDownloadSize(data_size.longValue());
                }
            } else {
                /* Get official downloadurl. */
                final Object directURL = JavaScriptEngineFactory.walkJson(downloadInfo, "url_list/{0}");
                if (directURL != null) {
                    link.setProperty(PROPERTY_DIRECTURL_API, StringUtils.valueOfOrNull(directURL));
                    if (data_size != null) {
                        link.setVerifiedFileSize(data_size.longValue());
                    }
                    link.removeProperty(PROPERTY_HAS_WATERMARK);
                }
            }
        }
        setLikeCount(link, (Number) statistics.get("digg_count"));
        setPlayCount(link, (Number) statistics.get("play_count"));
        setShareCount(link, (Number) statistics.get("share_count"));
        setCommentCount(link, (Number) statistics.get("comment_count"));
        link.setProperty(PROPERTY_ALLOW_HEAD_REQUEST, true);
        link.setAvailable(true);
        setFilename(link);
    }

    public static void accessAPI(final Browser br, final String path, final UrlQuery query) throws IOException {
        br.getPage(API_BASE + path + "/" + "?" + query.toString());
    }

    public static Browser prepBRWebsite(final Browser br) {
        br.setAllowedResponseCodes(400);
        return br;
    }

    public static Browser prepBRWebAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.67 Safari/537.36");
        return br;
    }

    public static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("User-Agent", String.format("com.ss.android.ugc.%s/%s (Linux; U; Android 10; en_US; Pixel 4; Build/QQ3A.200805.001; Cronet/58.0.2991.0)", API_CLIENT, API_VERSION_CODE));
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
        query.add("app_name", API_CLIENT);
        query.add("app_language", "en");
        query.add("language", "en");
        query.add("timezone_name", Encoding.urlEncode("America/New_York"));
        query.add("timezone_offset", "-14400");
        query.add("channel", "googleplay");
        query.add("ac", "wifi");
        query.add("mcc_mnc", "310260");
        query.add("is_my_cn", "0");
        query.add("aid", API_AID);
        query.add("ssmix", "a");
        query.add("as", "a1qwert123");
        query.add("cp", "cbfhckdckkde1");
        return query;
    }

    public static UrlQuery getWebsiteQuery() {
        final UrlQuery query = new UrlQuery();
        query.add("aid", "1459");
        query.add("app_language", "en");
        query.add("app_name", "tiktok_web");
        query.add("battery_info", "1");
        query.add("browser_language", "en-US");
        query.add("browser_name", "Mozilla");
        query.add("browser_online", "true");
        query.add("browser_platform", "Win32");
        query.add("browser_version", "5.0%20%28Windows%20NT%2010.0%3B%20Win64%3B%20x64%29%20AppleWebKit%2F537.36%20%28KHTML%2C%20like%20Gecko%29%20Chrome%2F102.0.5005.62%20Safari%2F537.36");
        query.add("channel", "tiktok_web");
        query.add("cookie_enabled", "true");
        query.add("device_id", TiktokComCrawler.generateDeviceID());
        query.add("device_platform", "web_pc");
        query.add("focus_state", "true");
        /* Can e.g. be "user" or "fyp". We'll leave that blank for now. */
        query.add("from_page", "");
        query.add("history_len", "4");
        query.add("is_fullscreen", "false");
        query.add("is_page_visible", "true");
        query.add("os", "windows");
        query.add("priority_region", "US");
        query.add("referer", "https%3A%2F%2Fwww.tiktok.com%2F");
        query.add("screen_height", "1080");
        query.add("screen_width", "1920");
        query.add("tz_name", "America%2FMonterrey");
        // query.add("verifyFp", "");
        query.add("webcast_language", "en-US");
        // query.add("msToken", "");
        // query.add("X-Bogus", "");
        // query.add("_signature", "");
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
            /* Stored username value. */
            return TiktokComCrawler.sanitizeUsername(link.getStringProperty(PROPERTY_USERNAME));
        } else {
            final String usernameFromURL = new Regex(link.getPluginPatternMatcher(), PATTERN_VIDEO).getMatch(0);
            if (usernameFromURL != null) {
                return TiktokComCrawler.sanitizeUsername(usernameFromURL);
            } else {
                return null;
            }
        }
    }

    private static String getDateFormatted(final DownloadLink link) {
        final String lastModifiedHeaderValue = link.getStringProperty(PROPERTY_DATE_LAST_MODIFIED_HEADER);
        final String dateFromWebsite = link.getStringProperty(PROPERTY_DATE_FROM_WEBSITE);
        if (link.hasProperty(PROPERTY_DATE)) {
            /* Prefer pre-formatted date obtained via timestamp from API/website-json */
            return link.getStringProperty(PROPERTY_DATE);
        } else if (dateFromWebsite != null) {
            /* This one is already available in the format we want. */
            return dateFromWebsite;
        } else if (lastModifiedHeaderValue != null) {
            /* Use formatted date of "Last-Modified" header of video directurls timestamp as fallback. */
            return convertDateFormat(lastModifiedHeaderValue);
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
        if (br.containsHTML("pageDescKey\\s*=\\s*'user_verify_page_description';|class=\"verify-wrap\"")) {
            return true;
        } else {
            return false;
        }
    }

    public static String generateDownloadurlOld(final Browser br, final String contentID) throws IOException {
        br.getPage("https://www.tiktok.com/node/video/playwm?id=" + contentID);
        return new URL(br.toString()).toString();
    }

    /** Converts given date to format yyyy-MM-dd */
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
            final SimpleDateFormat source_format = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
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
        final MediaCrawlMode mode = getDownloadMode();
        if (!link.hasProperty(PROPERTY_LAST_USED_DOWNLOAD_MODE) || !StringUtils.equals(link.getStringProperty(PROPERTY_LAST_USED_DOWNLOAD_MODE), mode.name())) {
            /* Prevent file corruption */
            logger.info("Resetting progress because user has downloaded using other download mode before");
            link.setChunksProgress(null);
            link.setVerifiedFileSize(-1);
        }
        /* Remember last used download mode */
        link.setProperty(PROPERTY_LAST_USED_DOWNLOAD_MODE, mode.name());
        this.missingDateFilenamePreDownloadHandling(link, account);
        if (this.attemptStoredDownloadurlDownload(link)) {
            logger.info("Using stored directurl for downloading");
        } else {
            requestFileInformation(link, account, true, true);
            final String dllink = getStoredDirecturl(link);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!this.attemptStoredDownloadurlDownload(link)) {
                br.followConnection(true);
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
        final String url = getStoredDirecturl(link);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            this.prepareDownloadHeaders(link, brc);
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, null), this.getMaxChunks(null));
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                missingDateFilenameLastResortHandling(link, dl.getConnection());
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
        return getUserConfiguredMaxSimultaneousDownloads();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        account.setType(AccountType.FREE);
        return ai;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies == null) {
                    showCookieLoginInfo();
                    throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
                }
                br.setCookies(userCookies);
                if (!force) {
                    /* Do not verify cookies */
                    return;
                }
                logger.info("Attempting user cookie login");
                prepBRWebAPI(br);
                br.getPage("https://us." + this.getHost() + "/passport/web/account/info/?" + getWebsiteQuery().toString());
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                final String msg = entries.get("message").toString();
                if (msg.equals("success")) {
                    /* Save new cookie timestamp */
                    logger.info("User Cookie login successful");
                    /*
                     * User can enter whatever he wants into the 'username' field but we want unique usernames --> Grab username from json
                     * response and set it.
                     */
                    final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                    account.setUser(data.get("username").toString());
                    if (configUseAPI() && !account.hasProperty(PROPERTY_ACCOUNT_HAS_SHOWN_DOWNLOAD_MODE_HINT)) {
                        showAccountLoginDownloadModeHint();
                        account.setProperty(PROPERTY_ACCOUNT_HAS_SHOWN_DOWNLOAD_MODE_HINT, true);
                    }
                    return;
                } else {
                    logger.info("User Cookie login failed");
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    protected Thread showAccountLoginDownloadModeHint() {
        final String host = this.getHost();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = host + " - Login";
                        message += "Hallo liebe(r) " + host + " NutzerIn\r\n";
                        message += "Du hast soeben erfolgreich deinen tiktok Account eingetragen.\r\n";
                        message += "Um damit private Videos herunterladen zu können, musst du noch den Download Modus in den tiktok Plugin Einstellungen auf 'Webseite' ändern!\r\n";
                    } else {
                        title = host + " - Login";
                        message += "Hello dear " + host + " user\r\n";
                        message += "You've just successfully added your tiktok account to JDownloader.\r\n";
                        message += "In order to use it to download private videos you need to change the download mode in your tiktok plugin settings to 'Website' first.";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(2 * 60 * 1000);
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return getUserConfiguredMaxSimultaneousDownloads();
    }

    private int getUserConfiguredMaxSimultaneousDownloads() {
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
            if (!needsAPIUsage(link)) {
                link.removeProperty(PROPERTY_FORCE_API);
            }
            link.removeProperty(PROPERTY_HAS_WATERMARK);
        }
    }
}