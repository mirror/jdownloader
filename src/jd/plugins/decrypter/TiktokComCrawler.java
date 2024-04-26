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
package jd.plugins.decrypter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.config.TiktokConfig;
import org.jdownloader.plugins.components.config.TiktokConfig.ImageFormat;
import org.jdownloader.plugins.components.config.TiktokConfig.MediaCrawlMode;
import org.jdownloader.plugins.components.config.TiktokConfig.ProfileCrawlMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.TiktokCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { TiktokCom.class })
public class TiktokComCrawler extends PluginForDecrypt {
    public TiktokComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        TiktokCom.setRequestLimits();
    }

    public static List<String[]> getPluginDomains() {
        return TiktokCom.getPluginDomains();
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
            ret.add("https?://(?:(?:www|vt|vm)\\.)?" + buildHostsPatternPart(domains) + "/.+");
        }
        return ret.toArray(new String[0]);
    }

    /** 2023-09-04: new: vt.tiktok.com */
    private final String TYPE_REDIRECT       = "(?i)https?://(?:vm|vt)\\.[^/]+/([A-Za-z0-9]+).*";
    private final String TYPE_APP            = "(?i)https?://[^/]+/t/([A-Za-z0-9]+).*";
    private final String TYPE_USER_USERNAME  = "(?i)https?://[^/]+/@([^\\?/]+).*";
    private final String TYPE_USER_USER_ID   = "(?i)https?://[^/]+/share/user/(\\d+).*";
    private final String TYPE_PLAYLIST_TAG   = "(?i)https?://[^/]+/tag/([^/]+)";
    private final String TYPE_PLAYLIST_MUSIC = "(?i)https?://[^/]+/music/([a-z0-9\\-]+)-(\\d+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final TiktokCom hostPlg = (TiktokCom) this.getNewPluginForHostInstance(this.getHost());
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* 2023-01-26: Replace photo -> video is just a cheap workaround for now. */
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://").replace("/photo/", "/video/");
        if (contenturl.matches(TYPE_REDIRECT) || contenturl.matches(TYPE_APP)) {
            /* Single redirect URLs */
            br.setFollowRedirects(false);
            final String initialURL = contenturl;
            String redirect = initialURL;
            int loops = 0;
            do {
                br.getPage(redirect);
                redirect = br.getRedirectLocation();
                if (redirect == null) {
                    break;
                } else if (hostPlg.canHandle(redirect)) {
                    break;
                } else if (loops >= 5) {
                    logger.info("Redirectloop -> URL must be offline");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (this.isAbort()) {
                    /* Aborted by user */
                    throw new InterruptedException();
                } else {
                    loops++;
                }
            } while (true);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (redirect == null) {
                logger.info("Failed to find redirect -> Looks like content is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!hostPlg.canHandle(redirect)) {
                /* E.g. redirect to mainpage */
                logger.info("Redirect did not lead to supported URL -> Looks like content is offline");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            logger.info("Old URL: " + initialURL + " | New URL: " + redirect);
            ret.add(createDownloadlink(redirect));
            return ret;
        }
        if (hostPlg.canHandle(contenturl)) {
            return crawlSingleMedia(hostPlg, param, contenturl);
        } else if (contenturl.matches(TYPE_USER_USERNAME) || contenturl.matches(TYPE_USER_USER_ID)) {
            return crawlProfile(param, contenturl);
        } else if (contenturl.matches(TYPE_PLAYLIST_TAG)) {
            return this.crawlPlaylistTag(param, contenturl);
        } else if (contenturl.matches(TYPE_PLAYLIST_MUSIC)) {
            return this.crawlPlaylistMusic(param, contenturl);
        } else {
            // unsupported url pattern
            logger.warning("Unsupported URL: " + param.getCryptedUrl());
            return new ArrayList<DownloadLink>(0);
        }
    }

    private ArrayList<DownloadLink> crawlSingleMedia(final TiktokCom hostPlg, final CryptedLink param, final String contenturl) throws Exception {
        return crawlSingleMedia(hostPlg, param, contenturl, AccountController.getInstance().getValidAccount(this.getHost()), false);
    }

    public ArrayList<DownloadLink> crawlSingleMedia(final TiktokCom hostPlg, final CryptedLink param, final String contenturl, final Account account, final boolean forceGrabAll) throws Exception {
        final DownloadLink link = param.getDownloadLink();
        final boolean forceAPI = link != null ? link.getBooleanProperty(TiktokCom.PROPERTY_FORCE_API, false) : false;
        if (TiktokCom.getDownloadMode() == MediaCrawlMode.API || forceAPI) {
            try {
                return this.crawlSingleMediaAPI(contenturl, null, forceAPI, forceGrabAll);
            } catch (final JSonMapperException jme) {
                /* Most likely API has answered with empty page. */
                logger.info("Attempting website fallback in API mode");
                return crawlSingleMediaWebsite(hostPlg, contenturl, null, forceGrabAll);
            }
        } else {
            try {
                return crawlSingleMediaWebsite(hostPlg, contenturl, null, forceGrabAll);
            } catch (final PluginException e) {
                logger.info("Attempting API fallback in website mode");
                if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND && br.containsHTML("\"(?:status_msg|message)\"\\s*:\\s*\"Something went wrong\"")) {
                    return this.crawlSingleMediaAPI(contenturl, null, true, forceGrabAll);
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * This can crawl single videos from website. </br>
     * If this tiktok item also contains images or contains only images, this handling will fail!
     */
    public ArrayList<DownloadLink> crawlSingleMediaWebsite(final TiktokCom hostPlg, final String url, final Account account, final boolean forceGrabAll) throws Exception {
        if (account != null) {
            hostPlg.login(account, false);
        }
        /* In website mode we neither know whether or not a video is watermarked nor can we download it without watermark. */
        final String contentID = TiktokCom.getContentID(url);
        if (contentID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        prepBRWebsite(br);
        final TiktokConfig cfg = PluginJsonConfig.get(hostPlg.getConfigInterface());
        final DownloadLink video = new DownloadLink(hostPlg, this.getHost(), url);
        String description = null;
        String username = null;
        Object diggCountO = null;
        Object shareCountO = null;
        Object playCountO = null;
        Object commentCountO = null;
        String dateFormatted = null;
        final boolean useWebsiteEmbed = true;
        /**
         * 2021-04-09: Avoid using the website-way as their bot protection may kick in right away! </br>
         * When using an account and potentially downloading private videos however, we can't use the embed way.
         */
        String videoDllink = null;
        boolean useWebsiteHandling = false;
        boolean offlineIfWebsiteHandlingFails = false;
        if (account != null) {
            useWebsiteHandling = true;
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
                br.getPage(url);
            } else {
                br.getPage("https://www." + this.getHost() + "/oembed?url=" + Encoding.urlEncode("https://www." + this.getHost() + "/video/" + contentID));
            }
            if (br.containsHTML("\"(?:status_msg|message)\"\\s*:\\s*\"Something went wrong\"")) {
                logger.info("Item looks to be offline according to oembed status -> Try Website as fallback");
                useWebsiteHandling = true;
                offlineIfWebsiteHandlingFails = true;
            } else {
                /* Required headers! */
                final Browser brc = this.br.cloneBrowser();
                brc.getHeaders().put("sec-fetch-dest", "iframe");
                brc.getHeaders().put("sec-fetch-mode", "navigate");
                // brc.getHeaders().put("sec-fetch-site", "cross-site");
                // brc.getHeaders().put("upgrade-insecure-requests", "1");
                // brc.getHeaders().put("Referer", link.getPluginPatternMatcher());
                brc.getPage("https://www." + this.getHost() + "/embed/v2/" + contentID);
                brc.followRedirect(); // without this we have different videoJson
                checkErrorsWebsite(brc);
                String videoJson = brc.getRegex("crossorigin=\"anonymous\">\\s*(.*?)\\s*</script>").getMatch(0);
                if (videoJson == null) {
                    videoJson = brc.getRegex("<script\\s*id[^>]*>\\s*(\\{.*?)\\s*</script>").getMatch(0);
                }
                final Map<String, Object> root = restoreFromString(videoJson, TypeRef.MAP);
                Map<String, Object> videoData = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "props/pageProps/videoData");
                if (videoData == null) {
                    // different videoJson when we do not follow the embed/v2 redirect
                    Map<String, Object> data = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "source/data/");
                    if (data != null) {
                        String key = null;
                        for (String keyEntry : data.keySet()) {
                            if (StringUtils.containsIgnoreCase(keyEntry, contentID)) {
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
                final Map<String, Object> musicInfos = (Map<String, Object>) videoData.get("musicInfos");
                // entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "videoData/itemInfos");
                /* In some cases this will be "0". In these cases, the date will be obtained from "last modified" header via website. */
                final String createTime = itemInfos.get("createTime").toString();
                description = (String) itemInfos.get("text");
                videoDllink = (String) JavaScriptEngineFactory.walkJson(itemInfos, "video/urls/{0}");
                /* Always look for username --> Username given inside URL which user added can be wrong! */
                final Object authorInfosO = videoData.get("authorInfos");
                if (authorInfosO != null) {
                    final Map<String, Object> authorInfos = (Map<String, Object>) authorInfosO;
                    username = (String) authorInfos.get("uniqueId");
                }
                /* Set more Packagizer properties */
                diggCountO = itemInfos.get("diggCount");
                playCountO = itemInfos.get("playCount");
                shareCountO = itemInfos.get("shareCount");
                commentCountO = itemInfos.get("commentCount");
                if (!StringUtils.isEmpty(createTime) && !"0".equals(createTime)) {
                    dateFormatted = TiktokCom.convertDateFormat(createTime);
                }
                if (videoDllink == null) {
                    /* Fallback */
                    videoDllink = TiktokCom.generateDownloadurlOld(br, contentID);
                }
                if (cfg.isVideoCrawlerCrawlAudioSeparately() && musicInfos != null) {
                    final List<String> audioURLs = (List<String>) musicInfos.get("playUrl");
                    final String audioDirecturl = audioURLs != null && audioURLs.size() > 0 ? StringUtils.nullOrNonEmpty(audioURLs.get(0)) : null;
                    if (audioDirecturl != null) {
                        final DownloadLink audio = new DownloadLink(hostPlg, this.getHost(), url);
                        audio.setProperty(TiktokCom.PROPERTY_DIRECTURL_WEBSITE, audioDirecturl);
                        audio.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_AUDIO);
                        ret.add(audio);
                    }
                }
                if (!StringUtils.isEmpty(videoDllink)) {
                    video.setProperty(TiktokCom.PROPERTY_DIRECTURL_WEBSITE, videoDllink);
                }
                video.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_VIDEO);
                ret.add(video);
            }
        } else {
            /* Rev. 40928 and earlier */
            /* Super old handling */
            videoDllink = TiktokCom.generateDownloadurlOld(br, contentID);
        }
        if (useWebsiteHandling) {
            try {
                br.getPage(url);
                checkErrorsWebsite(br);
                String videoJson = br.getRegex("crossorigin=\"anonymous\">\\s*(.*?)\\s*</script>").getMatch(0);
                if (videoJson == null) {
                    videoJson = br.getRegex("<script\\s*id[^>]*>\\s*(\\{.*?)\\s*</script>").getMatch(0);
                }
                final Map<String, Object> entries = restoreFromString(videoJson, TypeRef.MAP);
                final Map<String, Object> itemModule = (Map<String, Object>) entries.get("ItemModule");
                /* 2020-10-12: Hmm reliably checking for offline is complicated so let's try this instead ... */
                if (itemModule == null || itemModule.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final Map<String, Object> aweme_detail = (Map<String, Object>) itemModule.entrySet().iterator().next().getValue();
                final ArrayList<DownloadLink> resultsTmp = this.crawlProcessWebsiteMediaMapSingleTiktokItem(hostPlg, aweme_detail, null, true);
                ret.addAll(resultsTmp);
            } catch (final Exception e) {
                if (offlineIfWebsiteHandlingFails) {
                    logger.log(e);
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw e;
                }
            }
        }
        /* Set additional properties and find packagename */
        final String dateFromHtml = TiktokCom.getAndSetDateFromWebsite(this, br, ret.get(0));
        String packagename = null;
        for (final DownloadLink result : ret) {
            if (cfg.isEnableFastLinkcheck()) {
                result.setAvailable(true);
            }
            result.setProperty(TiktokCom.PROPERTY_ALLOW_HEAD_REQUEST, true);
            result.setProperty(TiktokCom.PROPERTY_AWEME_ITEM_ID, contentID);
            TiktokCom.setFilename(result);
            TiktokCom.setDescriptionAndHashtags(result, description);
            if (!StringUtils.isEmpty(username)) {
                result.setProperty(TiktokCom.PROPERTY_USERNAME, username);
            }
            if (diggCountO != null) {
                TiktokCom.setLikeCount(result, (Number) diggCountO);
            }
            if (shareCountO != null) {
                TiktokCom.setShareCount(result, (Number) shareCountO);
            }
            if (playCountO != null) {
                TiktokCom.setPlayCount(result, (Number) playCountO);
            }
            if (commentCountO != null) {
                TiktokCom.setCommentCount(result, (Number) commentCountO);
            }
            if (dateFormatted != null) {
                result.setProperty(TiktokCom.PROPERTY_DATE, dateFormatted);
            }
            result.setProperty(TiktokCom.PROPERTY_ATTEMPTED_TO_OBTAIN_DATE_FROM_WEBSITE, true);
            if (dateFromHtml != null) {
                result.setProperty(TiktokCom.PROPERTY_DATE_FROM_WEBSITE, dateFromHtml);
            }
            if (packagename == null && (result.getStringProperty(TiktokCom.PROPERTY_TYPE).equals(TiktokCom.TYPE_AUDIO) || result.getStringProperty(TiktokCom.PROPERTY_TYPE).equals(TiktokCom.TYPE_VIDEO))) {
                final String filename = result.getName();
                packagename = filename.substring(0, filename.lastIndexOf("."));
            }
        }
        if (packagename != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(packagename);
            fp.setCleanupPackageName(false);
            fp.addLinks(ret);
        }
        return ret;
    }

    private void checkErrorsWebsite(final Browser br) throws PluginException, DecrypterRetryException {
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("pageDescKey\\s*=\\s*'user_verify_page_description';|class=\"verify-wrap\"")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Captcha-blocked");
        } else if (TiktokCom.isBotProtectionActive(br)) {
            final String blockedText = "Premature stop: Blocked by anti bot protection / captcha";
            this.displayBubblenotifyMessage(blockedText, blockedText);
            throw new DecrypterRetryException(RetryReason.CAPTCHA, blockedText, blockedText, null);
        }
    }

    public ArrayList<DownloadLink> crawlSingleMediaAPI(final String url, final Account account, final boolean isForcedAPIUsage, final boolean forceGrabAll) throws Exception {
        final TiktokCom hostPlg = (TiktokCom) this.getNewPluginForHostInstance(this.getHost());
        if (account != null) {
            hostPlg.login(account, false);
        }
        /* In website mode we neither know whether or not a video is watermarked nor can we download it without watermark. */
        final String contentID = TiktokCom.getContentID(url);
        if (contentID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBRAPI(br);
        final UrlQuery query = TiktokCom.getAPIQuery();
        query.add("aweme_id", contentID);
        /* Alternative check for videos not available without feed-context: same request with path == '/feed' */
        // accessAPI(br, "/feed", query);
        TiktokCom.accessAPI(br, "/aweme/detail", query);
        Map<String, Object> entries = null;
        Map<String, Object> aweme_detail = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            aweme_detail = (Map<String, Object>) entries.get("aweme_detail");
        } catch (final JSonMapperException jse) {
            /* Fallback */
            logger.info("Trying API /feed fallback");
            /* Alternative check for videos not available without feed-context: same request with path == '/feed' */
            prepBRAPI(br);
            /* Make sure that the next request will not contain a Referer header otherwise we'll get a blank page! */
            br.setCurrentURL("");
            TiktokCom.accessAPI(br, "/feed", query);
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> aweme_list = (List<Map<String, Object>>) entries.get("aweme_list");
            for (final Map<String, Object> aweme_detailTmp : aweme_list) {
                if (StringUtils.equals(aweme_detailTmp.get("aweme_id").toString(), contentID)) {
                    aweme_detail = aweme_detailTmp;
                    break;
                }
            }
            if (aweme_detail == null) {
                logger.info("Fallback failed -> Video item looks to be offline");
            }
        }
        if (aweme_detail == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return this.processAwemeDetail(hostPlg, aweme_detail, forceGrabAll, isForcedAPIUsage);
    }

    public ArrayList<DownloadLink> crawlProfile(final CryptedLink param, final String contenturl) throws Exception {
        if (PluginJsonConfig.get(TiktokConfig.class).getProfileCrawlerMaxItemsLimit() == 0) {
            logger.info("User has disabled profile crawler --> Returning empty array");
            return new ArrayList<DownloadLink>();
        }
        if (PluginJsonConfig.get(TiktokConfig.class).getProfileCrawlMode() == ProfileCrawlMode.API) {
            /* API mode with website-fallback */
            try {
                return crawlProfileAPI(param, contenturl);
            } catch (final JSonMapperException jme) {
                /* Most likely API has answered with empty page. */
                logger.info("Attempting website fallback in API mode");
                return crawlProfileWebsite(param, contenturl);
            }
        } else {
            /* Website mode (without API fallback) */
            return crawlProfileWebsite(param, contenturl);
        }
    }

    /**
     * Use website to crawl all videos of a user. </br>
     * Pagination hasn't been implemented so this will only find the first batch of items - usually around 30 items!
     */
    public ArrayList<DownloadLink> crawlProfileWebsite(final CryptedLink param, final String contenturl) throws Exception {
        prepBRWebsite(br);
        /* Login whenever possible */
        final TiktokCom hostPlg = (TiktokCom) this.getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            hostPlg.login(account, false);
        }
        br.setFollowRedirects(true);
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Profile does not exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String usernameSlug = new Regex(br.getURL(), TYPE_USER_USERNAME).getMatch(0);
        if (usernameSlug == null) {
            /* Redirect to somewhere else -> Probably profile does not exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (TiktokCom.isBotProtectionActive(this.br)) {
            final UrlQuery query = TiktokCom.getWebsiteQuery();
            query.add("keyword", Encoding.urlEncode(usernameSlug));
            br.getPage("https://www." + this.getHost() + "/api/search/general/preview/?" + query.toString());
            sleep(1000, param);// this somehow bypass the protection, maybe calling api twice sets a cookie?
            br.getPage("https://www." + this.getHost() + "/api/search/general/preview/?" + query.toString());
            br.getPage(contenturl);
        }
        this.checkErrorsWebsite(br);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        FilePackage fp = null;
        Exception websiteFailureException = null;
        try {
            /* First try the "hard" way */
            String json = br.getRegex("window\\['SIGI_STATE'\\]\\s*=\\s*(\\{.*?\\});").getMatch(0);
            if (json == null) {
                json = br.getRegex("<script\\s*id\\s*=\\s*\"SIGI_STATE\"[^>]*>\\s*(\\{.*?\\});?\\s*</script>").getMatch(0);
            }
            // final String preferredImageFileExtension;
            // if (cfg.getPreferredImageFormat() == ImageFormat.JPEG) {
            // preferredImageFileExtension = ".jpeg";
            // } else {
            // preferredImageFileExtension = ".webp";
            // }
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            if (entries == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Map<String, Map<String, Object>> itemModule = (Map<String, Map<String, Object>>) entries.get("ItemModule");
            final Map<String, Object> userPost = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "ItemList/user-post");
            final List<Map<String, Object>> preloadList = (List<Map<String, Object>>) userPost.get("preloadList");
            /* Typically we get up to 30 items per page. In some cases we get only 28 or 29 for some reason. */
            final Collection<Map<String, Object>> medias = itemModule.values();
            int mediaIndex = 0;
            for (final Map<String, Object> media : medias) {
                final Map<String, Object> preloadInfo = preloadList.get(mediaIndex);
                final ArrayList<DownloadLink> tempResults = this.crawlProcessWebsiteMediaMapSingleTiktokItem(hostPlg, media, preloadInfo, false);
                for (final DownloadLink result : tempResults) {
                    if (fp == null) {
                        final String username = result.getStringProperty(TiktokCom.PROPERTY_USERNAME);
                        final String userID = result.getStringProperty(TiktokCom.PROPERTY_USER_ID);
                        fp = this.getProfileFilePackage(username, userID);
                    }
                    result._setFilePackage(fp);
                    distribute(result);
                }
                if ((mediaIndex + 1) == cfg.getProfileCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    return ret;
                }
                mediaIndex++;
            }
            if ((Boolean) userPost.get("hasMore") && cfg.isAddDummyURLProfileCrawlerWebsiteModeMissingPagination()) {
                final String detailedErrorExplanation = "This crawler plugin cannot handle pagination in website mode thus it is currently impossible to crawl more than " + medias.size() + " items of this particular profile.\r\nCheck this forum thread for more info: https://board.jdownloader.org/showthread.php?t=79982";
                this.displayBubblenotifyMessage("Premature stop", detailedErrorExplanation);
                final DownloadLink dummy = createLinkCrawlerRetry(getCurrentLink(), new DecrypterRetryException(RetryReason.FILE_NOT_FOUND));
                dummy.setFinalFileName("CANNOT_CRAWL_MORE_THAN_" + medias.size() + "_ITEMS_OF_PROFILE_" + usernameSlug + "_IN_WEBSITE_PROFILE_CRAWL_MODE");
                dummy.setComment(detailedErrorExplanation);
                if (fp != null) {
                    dummy._setFilePackage(fp);
                }
                distribute(dummy);
                ret.add(dummy);
            }
        } catch (final Exception ignore) {
            logger.log(ignore);
            websiteFailureException = ignore;
        }
        if (ret.isEmpty()) {
            /* Super old code: Last chance fallback */
            logger.warning("Fallback to last resort plain html handling");
            final String[] videoIDs = br.getRegex(usernameSlug + "/video/(\\d+)\"").getColumn(0);
            for (final String videoID : videoIDs) {
                final DownloadLink dl = this.createDownloadlink(getContentURL(usernameSlug, videoID));
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                ret.add(dl);
                if (ret.size() == cfg.getProfileCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    this.displayBubblenotifyMessage("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit(), "Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    return ret;
                }
            }
        }
        if (ret.isEmpty()) {
            if (websiteFailureException != null) {
                throw websiteFailureException;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlProcessWebsiteMediaMapSingleTiktokItem(final PluginForHost hostPlg, final Map<String, Object> media, final Map<String, Object> preloadInfo, final boolean forceGrabAll) {
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        final String preferredImageFileExtension;
        if (cfg.getPreferredImageFormat() == ImageFormat.JPEG) {
            preferredImageFileExtension = ".jpeg";
        } else {
            preferredImageFileExtension = ".webp";
        }
        final Map<String, Object> stats = (Map<String, Object>) media.get("stats");
        final Map<String, Object> streamInfo = (Map<String, Object>) media.get("video");
        final Map<String, Object> imagePost = (Map<String, Object>) media.get("imagePost");
        final Map<String, Object> music = (Map<String, Object>) media.get("music");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String author = media.get("author").toString();
        final String videoID = (String) media.get("id");
        final String createTimeStr = (String) media.get("createTime");
        final String description = (String) media.get("desc");
        final String contentURL = getContentURL(author, videoID);
        final boolean crawlAudio;
        if (imagePost != null) {
            /* Image post */
            final List<Map<String, Object>> images = (List<Map<String, Object>>) imagePost.get("images");
            int imageIndex = 0;
            for (final Map<String, Object> imageMap : images) {
                final Map<String, Object> imageURL = (Map<String, Object>) imageMap.get("imageURL");
                final List<String> urlList = (List<String>) imageURL.get("urlList");
                String preferredImageURL = null;
                for (final String image_url : urlList) {
                    final String thisExt = Plugin.getFileNameExtensionFromURL(image_url);
                    if (StringUtils.equalsIgnoreCase(thisExt, preferredImageFileExtension)) {
                        preferredImageURL = image_url;
                        break;
                    }
                }
                final String chosenImageURL;
                if (StringUtils.isNotEmpty(preferredImageURL)) {
                    chosenImageURL = preferredImageURL;
                } else {
                    /* Fallback */
                    chosenImageURL = urlList.get(0);
                    logger.info("Failed to find preferred image format -> Fallback: " + chosenImageURL);
                }
                final DownloadLink picture = new DownloadLink(hostPlg, this.getHost(), contentURL);
                picture.setProperty(TiktokCom.PROPERTY_ALLOW_HEAD_REQUEST, false);
                picture.setProperty(TiktokCom.PROPERTY_DIRECTURL_WEBSITE, chosenImageURL);
                picture.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_PICTURE);
                picture.setProperty(TiktokCom.PROPERTY_INDEX, imageIndex);
                picture.setProperty(TiktokCom.PROPERTY_INDEX_MAX, images.size());
                ret.add(picture);
                imageIndex++;
            }
            /* Force crawl audio as audio is part of that "image slideshow" on tiktok website. */
            crawlAudio = true;
        } else {
            /* Video post */
            final DownloadLink video = new DownloadLink(hostPlg, this.getHost(), contentURL);
            video.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_VIDEO);
            video.setProperty(TiktokCom.PROPERTY_ALLOW_HEAD_REQUEST, true);
            String videoDirecturl = (String) streamInfo.get("downloadAddr");
            if (StringUtils.isEmpty(videoDirecturl)) {
                videoDirecturl = (String) streamInfo.get("playAddr");
            }
            if (StringUtils.isEmpty(videoDirecturl) && preloadInfo != null) {
                videoDirecturl = preloadInfo.get("url").toString();
            }
            if (!StringUtils.isEmpty(videoDirecturl)) {
                video.setProperty(TiktokCom.PROPERTY_DIRECTURL_WEBSITE, videoDirecturl);
            }
            ret.add(video);
            /* Crawl separate audio only if wished by user. */
            crawlAudio = cfg.isVideoCrawlerCrawlAudioSeparately();
        }
        if ((crawlAudio || forceGrabAll) && music != null) {
            final String musicURL = music.get("playUrl").toString();
            String ext = Plugin.getFileNameExtensionFromURL(musicURL);
            if (ext == null) {
                /* Fallback */
                ext = ".mp3";
            }
            final DownloadLink audio = new DownloadLink(hostPlg, this.getHost(), contentURL);
            audio.setProperty(TiktokCom.PROPERTY_ALLOW_HEAD_REQUEST, true);
            audio.setProperty(TiktokCom.PROPERTY_DIRECTURL_WEBSITE, musicURL);
            audio.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_AUDIO);
            ret.add(audio);
        }
        final String dateFormatted = formatDate(Long.parseLong(createTimeStr));
        for (final DownloadLink result : ret) {
            result.setAvailable(true);
            TiktokCom.setDescriptionAndHashtags(result, description);
            result.setProperty(TiktokCom.PROPERTY_USERNAME, author);
            result.setProperty(TiktokCom.PROPERTY_USER_ID, media.get("authorId"));
            result.setProperty(TiktokCom.PROPERTY_DATE, dateFormatted);
            TiktokCom.setLikeCount(result, (Number) stats.get("diggCount"));
            TiktokCom.setPlayCount(result, (Number) stats.get("playCount"));
            TiktokCom.setShareCount(result, (Number) stats.get("shareCount"));
            TiktokCom.setCommentCount(result, (Number) stats.get("commentCount"));
            TiktokCom.setFilename(result);
        }
        return ret;
    }

    public ArrayList<DownloadLink> crawlProfileAPI(final CryptedLink param, final String contenturl) throws Exception {
        String user_id = null;
        if (contenturl.matches(TYPE_USER_USER_ID)) {
            /* user_id is given inside URL. */
            user_id = new Regex(contenturl, TYPE_USER_USER_ID).getMatch(0);
        } else {
            /* Only username is given and we need to find the user_id. */
            final String usernameSlug = new Regex(contenturl, TYPE_USER_USERNAME).getMatch(0);
            if (usernameSlug == null) {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setFollowRedirects(true);
            /* Find userID via website */
            final Browser websitebrowser = br.cloneBrowser();
            TiktokCom.prepBRWebAPI(websitebrowser);
            final UrlQuery query = TiktokCom.getWebsiteQuery();
            query.add("keyword", Encoding.urlEncode(usernameSlug));
            websitebrowser.getPage("https://www." + this.getHost() + "/api/search/user/preview/?" + query.toString());
            final Map<String, Object> searchResults = restoreFromString(websitebrowser.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> sug_list = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(searchResults, "sug_list/");
            for (Map<String, Object> sug : sug_list) {
                final Map<String, Object> info = (Map<String, Object>) sug.get("extra_info");
                final String sug_uniq_id = info != null ? StringUtils.valueOfOrNull(info.get("sug_uniq_id")) : null;
                if (StringUtils.equals(usernameSlug, sug_uniq_id)) {
                    user_id = info.get("sug_user_id").toString();
                    break;
                }
            }
            if (user_id == null) {
                logger.info("Using fallback method to find userID!");
                websitebrowser.getPage(contenturl);
                user_id = websitebrowser.getRegex("\"authorId\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                if (user_id == null && TiktokCom.isBotProtectionActive(websitebrowser)) {
                    sleep(1000, param);// This used to somehow bypass the protection, maybe calling api twice sets a cookie?
                    websitebrowser.getPage("https://www." + this.getHost() + "/api/search/general/preview/?" + query.toString());
                    websitebrowser.getPage(contenturl);
                    user_id = websitebrowser.getRegex("\"authorId\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                }
            }
            if (user_id == null) {
                this.checkErrorsWebsite(websitebrowser);
                logger.info("Profile doesn't exist or it's a private profile");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        prepBRAPI(this.br);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final int maxItemsPerPage = 21;
        final UrlQuery query = TiktokCom.getAPIQuery();
        query.add("user_id", user_id);
        query.add("count", Integer.toString(maxItemsPerPage));
        query.add("max_cursor", "0");
        query.add("min_cursor", "0");
        query.add("retry_type", "no_retry");
        query.add("device_id", generateDeviceID());
        int page = 1;
        FilePackage fp = null;
        String author = null;
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        int numberofProcessedItems = 0;
        final TiktokCom hosterplugin = (TiktokCom) this.getNewPluginForHostInstance(this.getHost());
        do {
            TiktokCom.accessAPI(br, "/aweme/post", query);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> mediaitems = (List<Map<String, Object>>) entries.get("aweme_list");
            if (mediaitems == null) {
                /* Profile does not exist. */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (mediaitems.isEmpty()) {
                if (ret.isEmpty()) {
                    /* User has no video uploads at all. */
                    throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE);
                } else {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            for (final Map<String, Object> aweme_detail : mediaitems) {
                final ArrayList<DownloadLink> resultlist = this.processAwemeDetail(hosterplugin, aweme_detail, false, false);
                for (final DownloadLink result : resultlist) {
                    if (fp == null) {
                        /*
                         * Collect author name on first round because it is not always given before e.g. not given if user adds URL of type
                         * TYPE_USER_USER_ID.
                         */
                        author = result.getStringProperty(TiktokCom.PROPERTY_USERNAME);
                        fp = getProfileFilePackage(author, user_id);
                    }
                    result._setFilePackage(fp);
                    ret.add(result);
                    distribute(result);
                }
                numberofProcessedItems++;
                if (numberofProcessedItems == cfg.getProfileCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    this.displayBubblenotifyMessage("Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit(), "Stopping because: Reached user defined max items limit: " + cfg.getProfileCrawlerMaxItemsLimit());
                    return ret;
                }
            }
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                break;
            } else if (((Number) entries.get("has_more")).intValue() != 1) {
                logger.info("Stopping because: Reached last page");
                break;
            } else if (mediaitems.size() < maxItemsPerPage) {
                /* Extra fail-safe */
                logger.info("Stopping because: Current page contained less items than " + maxItemsPerPage);
                break;
            }
            query.addAndReplace("max_cursor", entries.get("max_cursor").toString());
            page++;
        } while (true);
        return ret;
    }

    public ArrayList<DownloadLink> crawlPlaylistTag(final CryptedLink param, final String contenturl) throws Exception {
        if (PluginJsonConfig.get(TiktokConfig.class).getTagCrawlerMaxItemsLimit() == 0) {
            logger.info("User has disabled tag crawler --> Returning empty array");
            return new ArrayList<DownloadLink>();
        }
        return crawlPlaylistAPI(param, contenturl);
    }

    public ArrayList<DownloadLink> crawlPlaylistAPI(final CryptedLink param, final String contenturl) throws Exception {
        final String tagName = new Regex(contenturl, TYPE_PLAYLIST_TAG).getMatch(0);
        if (tagName == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        prepBRWebsite(br);
        br.getPage(contenturl);
        checkErrorsWebsite(br);
        final String tagID = br.getRegex("snssdk\\d+://challenge/detail/(\\d+)").getMatch(0);
        if (tagID == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("tag - " + tagName);
        return crawlPlaylistAPI("/challenge/aweme", "ch_id", tagID, fp);
    }

    /** Under development */
    public ArrayList<DownloadLink> crawlPlaylistMusic(final CryptedLink param, final String contenturl) throws Exception {
        // TODO
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_PLAYLIST_MUSIC);
        final String musicPlaylistTitle = urlinfo.getMatch(0);
        final String musicID = urlinfo.getMatch(1);
        if (musicPlaylistTitle == null || musicID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return crawlPlaylistMusicAPI(param, musicPlaylistTitle, musicID);
    }

    /** Under development */
    public ArrayList<DownloadLink> crawlPlaylistMusicAPI(final CryptedLink param, final String musicPlaylistTitle, final String musicID) throws Exception {
        // TODO
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (musicPlaylistTitle == null || musicID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName("music - " + musicPlaylistTitle);
        return crawlPlaylistAPI("/music/aweme", "music_id", musicID, fp);
    }

    /** Generic function to crawl playlist-like stuff. */
    public ArrayList<DownloadLink> crawlPlaylistAPI(final String apiPath, final String playlistKeyName, final String playlistID, final FilePackage fp) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        logger.info("Crawling playlist or playlist-like item with ID: " + playlistID);
        final int maxItemsPerPage = 20;
        final UrlQuery query = TiktokCom.getAPIQuery();
        query.add(playlistKeyName, playlistID);
        query.add("cursor", "0");
        query.add("count", Integer.toString(maxItemsPerPage));
        query.add("type", "5");
        query.add("device_id", generateDeviceID());
        prepBRAPI(this.br);
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        int page = 1;
        int numberofProcessedItems = 0;
        final TiktokCom hosterplugin = (TiktokCom) this.getNewPluginForHostInstance(this.getHost());
        do {
            TiktokCom.accessAPI(br, apiPath, query);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> videos = (List<Map<String, Object>>) entries.get("aweme_list");
            if (videos.isEmpty()) {
                if (ret.isEmpty()) {
                    /* There are no videos with this tag available. */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            for (final Map<String, Object> aweme_detail : videos) {
                final ArrayList<DownloadLink> resultlist = this.processAwemeDetail(hosterplugin, aweme_detail, false, false);
                for (final DownloadLink result : resultlist) {
                    if (fp != null) {
                        result._setFilePackage(fp);
                    }
                    ret.add(result);
                    distribute(result);
                }
                numberofProcessedItems++;
                if (numberofProcessedItems == cfg.getTagCrawlerMaxItemsLimit()) {
                    logger.info("Stopping because: Reached user defined max items limit: " + cfg.getTagCrawlerMaxItemsLimit());
                    return ret;
                }
            }
            logger.info("Crawled page " + page + "Number of items on current page " + videos.size() + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (((Integer) entries.get("has_more")).intValue() != 1) {
                logger.info("Stopping because: Reached end");
                break;
            }
            final String nextCursor = entries.get("cursor").toString();
            if (StringUtils.isEmpty(nextCursor)) {
                /* Additional fail-safe */
                logger.info("Stopping because: Failed to find cursor --> Reached end?");
                break;
            } else {
                query.addAndReplace("cursor", nextCursor);
                page++;
            }
        } while (true);
        return ret;
    }
    // private ArrayList<DownloadLink> processVideoList(final List<Map<String, Object>> videos, final FilePackage fp) throws PluginException
    // {
    // final TiktokCom hosterplugin = (TiktokCom) this.getNewPluginForHostInstance(this.getHost());
    // final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
    // for (final Map<String, Object> aweme_detail : videos) {
    // final ArrayList<DownloadLink> resultlist = this.processAwemeDetail(hosterplugin, aweme_detail, false, false);
    // for (final DownloadLink result : resultlist) {
    // result._setFilePackage(fp);
    // ret.add(result);
    // distribute(result);
    // }
    // }
    // return ret;
    // }

    private String getPreferredImageURL(final List<String> urlList) {
        final List<String> preferredFallbackExtensions = new ArrayList<String>();
        preferredFallbackExtensions.add(".jpeg");
        preferredFallbackExtensions.add(".webp");
        // exts.add(".heic");
        final String preferredImageFileExtension;
        if (PluginJsonConfig.get(TiktokConfig.class).getPreferredImageFormat() == ImageFormat.JPEG) {
            preferredImageFileExtension = ".jpeg";
        } else {
            preferredImageFileExtension = ".webp";
        }
        String preferredImageURL = null;
        String preferredImageURLFallback = null;
        for (final String image_url : urlList) {
            String thisExt = Plugin.getFileNameExtensionFromURL(image_url);
            if (thisExt == null) {
                continue;
            }
            thisExt = thisExt.toLowerCase(Locale.ENGLISH);
            if (thisExt.equals(preferredImageFileExtension)) {
                preferredImageURL = image_url;
                break;
            } else if (preferredFallbackExtensions.contains(thisExt)) {
                preferredImageURLFallback = image_url;
            }
        }
        final String chosenImageURL;
        if (StringUtils.isNotEmpty(preferredImageURL)) {
            chosenImageURL = preferredImageURL;
        } else if (preferredImageURLFallback != null) {
            chosenImageURL = preferredImageURLFallback;
        } else {
            /* Fallback */
            chosenImageURL = urlList.get(0);
            logger.info("Failed to find preferred image format -> Fallback: " + chosenImageURL);
        }
        return chosenImageURL;
    }

    /**
     * Processes 'aweme_detail' map which is delivered as API response and can contain a single video, audio or multiple images with audio.
     */
    private ArrayList<DownloadLink> processAwemeDetail(final TiktokCom hostPlg, final Map<String, Object> aweme_detail, final boolean forceGrabAll, final boolean isForcedAPIUsage) throws PluginException {
        final Map<String, Object> status = (Map<String, Object>) aweme_detail.get("status");
        if ((Boolean) status.get("is_delete")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String contentID = aweme_detail.get("aweme_id").toString();
        final TiktokConfig cfg = PluginJsonConfig.get(TiktokConfig.class);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(new Date(((Number) aweme_detail.get("create_time")).longValue() * 1000));
        final Map<String, Object> statistics = (Map<String, Object>) aweme_detail.get("statistics");
        final Map<String, Object> video = (Map<String, Object>) aweme_detail.get("video");
        final Map<String, Object> music = (Map<String, Object>) aweme_detail.get("music");
        final Map<String, Object> author = (Map<String, Object>) aweme_detail.get("author");
        final Map<String, Object> image_post_info = (Map<String, Object>) aweme_detail.get("image_post_info");
        final String username = author.get("unique_id").toString();
        final String contenturl = this.getContentURL(username, contentID);
        final boolean crawlAudio;
        if (image_post_info != null) {
            /* Image post */
            final List<Map<String, Object>> images = (List<Map<String, Object>>) image_post_info.get("images");
            int index = 0;
            for (final Map<String, Object> image : images) {
                final Map<String, Object> imageMap;
                if (cfg.isImageCrawlerCrawlImagesWithoutWatermark()) {
                    imageMap = (Map<String, Object>) image.get("display_image");
                } else {
                    imageMap = (Map<String, Object>) image.get("user_watermark_image");
                }
                final List<String> url_list = (List<String>) imageMap.get("url_list");
                final String chosenImageURL = this.getPreferredImageURL(url_list);
                final DownloadLink picture = new DownloadLink(hostPlg, this.getHost(), contenturl);
                picture.setProperty(TiktokCom.PROPERTY_DIRECTURL_API, chosenImageURL);
                picture.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_PICTURE);
                picture.setProperty(TiktokCom.PROPERTY_INDEX, index);
                picture.setProperty(TiktokCom.PROPERTY_INDEX_MAX, images.size());
                ret.add(picture);
                index++;
            }
            /* Force crawl audio as audio is part of that "image slideshow" on tiktok website. */
            crawlAudio = true;
        } else {
            /* Video post */
            final DownloadLink video0 = new DownloadLink(hostPlg, this.getHost(), contenturl);
            final Boolean has_watermark = Boolean.TRUE.equals(video.get("has_watermark"));
            Map<String, Object> downloadInfo = (Map<String, Object>) video.get("download_addr");
            if (downloadInfo == null) {
                /* Fallback/old way */
                final String downloadJson = video.get("misc_download_addrs").toString();
                final Map<String, Object> misc_download_addrs = restoreFromString(downloadJson, TypeRef.MAP);
                downloadInfo = (Map<String, Object>) misc_download_addrs.get("suffix_scene");
            }
            final Map<String, Object> play_addr = (Map<String, Object>) video.get("play_addr");
            final boolean tryHDDownload = false;
            // if (cfg.getDownloadMode() == DownloadMode.API_HD) {
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
                video0.setProperty(TiktokCom.PROPERTY_DIRECTURL_API, String.format("https://api.tiktokv.com/aweme/v1/play/?video_id=%s&line=0&watermark=0&source=AWEME_DETAIL&is_play_url=1&ratio=default&improve_bitrate=1", play_addr.get("uri").toString()));
                /*
                 * This way we can't know whether or not the video comes with watermark but usually this version will not contain a
                 * watermark.
                 */
                video0.removeProperty(null);
                /* We can't know the filesize of this video version in beforehand. */
                video0.setVerifiedFileSize(-1);
            } else {
                /* Get non-HD directurl */
                String directurl = null;
                final Number data_size = downloadInfo != null ? (Number) downloadInfo.get("data_size") : null;
                if (has_watermark || (Boolean.TRUE.equals(aweme_detail.get("prevent_download")) && downloadInfo == null)) {
                    /* Get stream downloadurl because it comes WITHOUT watermark anyways */
                    if (has_watermark) {
                        video0.setProperty(TiktokCom.PROPERTY_HAS_WATERMARK, true);
                    } else {
                        video0.setProperty(TiktokCom.PROPERTY_HAS_WATERMARK, null);
                    }
                    directurl = (String) JavaScriptEngineFactory.walkJson(play_addr, "url_list/{0}");
                    video0.setProperty(TiktokCom.PROPERTY_DIRECTURL_API, directurl);
                    if (data_size != null) {
                        /**
                         * Set filesize of download-version because streaming- and download-version are nearly identical. </br>
                         * If a video is watermarked and downloads are prohibited both versions should be identical.
                         */
                        video0.setDownloadSize(data_size.longValue());
                    }
                } else {
                    /* Get official downloadurl. */
                    final Object directURL = JavaScriptEngineFactory.walkJson(downloadInfo, "url_list/{0}");
                    if (directURL != null) {
                        video0.setProperty(TiktokCom.PROPERTY_DIRECTURL_API, StringUtils.valueOfOrNull(directURL));
                        if (data_size != null) {
                            video0.setVerifiedFileSize(data_size.longValue());
                        }
                        video0.removeProperty(TiktokCom.PROPERTY_HAS_WATERMARK);
                    }
                }
            }
            video0.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_VIDEO);
            ret.add(video0);
            /* User decides whether or not he wants to download the audio of this video separately. */
            crawlAudio = cfg.isVideoCrawlerCrawlAudioSeparately();
        }
        if ((crawlAudio || forceGrabAll) && music != null) {
            final String musicURL = JavaScriptEngineFactory.walkJson(music, "play_url/uri").toString();
            if (StringUtils.isNotEmpty(musicURL)) {
                String ext = Plugin.getFileNameExtensionFromURL(musicURL);
                if (ext == null) {
                    /* Fallback */
                    ext = ".mp3";
                }
                final DownloadLink audio = new DownloadLink(hostPlg, this.getHost(), contenturl);
                audio.setProperty(TiktokCom.PROPERTY_DIRECTURL_API, musicURL);
                audio.setProperty(TiktokCom.PROPERTY_TYPE, TiktokCom.TYPE_AUDIO);
                ret.add(audio);
            }
        }
        /* Set additional properties and find package name */
        String packagename = null;
        for (final DownloadLink result : ret) {
            result.setAvailable(true);
            result.setProperty(TiktokCom.PROPERTY_AWEME_ITEM_ID, contentID);
            result.setProperty(TiktokCom.PROPERTY_DATE, dateFormatted);
            result.setProperty(TiktokCom.PROPERTY_USERNAME, username);
            result.setProperty(TiktokCom.PROPERTY_USER_ID, author.get("uid"));
            TiktokCom.setDescriptionAndHashtags(result, aweme_detail.get("desc").toString());
            TiktokCom.setLikeCount(result, (Number) statistics.get("digg_count"));
            TiktokCom.setPlayCount(result, (Number) statistics.get("play_count"));
            TiktokCom.setShareCount(result, (Number) statistics.get("share_count"));
            TiktokCom.setCommentCount(result, (Number) statistics.get("comment_count"));
            result.setProperty(TiktokCom.PROPERTY_ALLOW_HEAD_REQUEST, true);
            result.setProperty(TiktokCom.PROPERTY_FORCE_API, isForcedAPIUsage);
            TiktokCom.setFilename(result);
            if (packagename == null && (result.getStringProperty(TiktokCom.PROPERTY_TYPE).equals(TiktokCom.TYPE_AUDIO) || result.getStringProperty(TiktokCom.PROPERTY_TYPE).equals(TiktokCom.TYPE_VIDEO))) {
                final String filename = result.getName();
                packagename = filename.substring(0, filename.lastIndexOf("."));
            }
        }
        if (packagename != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(packagename);
            fp.setCleanupPackageName(false);
            fp.addLinks(ret);
        }
        return ret;
    }

    private FilePackage getProfileFilePackage(final String name, final String userID) {
        final FilePackage fp = FilePackage.getInstance();
        fp.setCleanupPackageName(false);
        fp.setName(name);
        if (userID != null) {
            fp.setPackageKey("tiktok://profile/" + userID);
        }
        return fp;
    }

    private String getContentURL(final String user, final String videoID) {
        return "https://www." + this.getHost() + "/@" + sanitizeUsername(user) + "/video/" + videoID;
    }

    /** Cleans up given username String. */
    public static String sanitizeUsername(final String user) {
        if (user == null) {
            return null;
        } else if (user.startsWith("@")) {
            return user.substring(1, user.length());
        } else {
            return user;
        }
    }

    /** Returns random 19 digit string. */
    public static String generateDeviceID() {
        return TiktokCom.generateRandomString("1234567890", 19);
    }

    public static String formatDate(final long date) {
        if (date <= 0) {
            return null;
        }
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date * 1000);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = Long.toString(date);
        }
        return formattedDate;
    }

    /** Wrapper */
    private Browser prepBRWebsite(final Browser br) {
        return TiktokCom.prepBRWebsite(br);
    }

    /** Wrapper */
    private Browser prepBRWebAPI(final Browser br) {
        return TiktokCom.prepBRWebAPI(br);
    }

    /** Wrapper */
    private Browser prepBRAPI(final Browser br) {
        return TiktokCom.prepBRAPI(br);
    }

    private void displayBubblenotifyMessage(final String title, final String msg) {
        BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
            @Override
            public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                return new BasicNotify("Tiktok: " + title, msg, new AbstractIcon(IconKey.ICON_INFO, 32));
            }
        });
    }
}
