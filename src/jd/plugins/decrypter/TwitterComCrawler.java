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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.TwitterConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.TwitterCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TwitterComCrawler extends PluginForDecrypt {
    private String resumeURL = null;

    public TwitterComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String            TYPE_CARD                                                        = "https?://[^/]+/i/cards/tfw/v1/(\\d+)";
    private static final String            TYPE_USER_ALL                                                    = "https?://[^/]+/([\\w\\-]+)(?:/(?:media|likes))?(\\?.*)?";
    private static final String            TYPE_USER_LIKES                                                  = "https?://[^/]+/([\\w\\-]+)/likes.*";
    private static final String            TYPE_USER_MEDIA                                                  = "https?://[^/]+/([\\w\\-]+)/media.*";
    private static final String            TYPE_USER_POST                                                   = "https?://[^/]+/([^/]+)/status/(\\d+).*?";
    // private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private static AtomicReference<String> GUEST_TOKEN                                                      = new AtomicReference<String>();
    private static AtomicLong              GUEST_TOKEN_TS                                                   = new AtomicLong(-1);
    public static final String             PROPERTY_USERNAME                                                = "username";
    private static final String            PROPERTY_DATE                                                    = "date";
    private static final String            PROPERTY_DATE_TIMESTAMP                                          = "date_timestamp";
    public static final String             PROPERTY_MEDIA_INDEX                                             = "mediaindex";
    public static final String             PROPERTY_MEDIA_ID                                                = "mediaid";
    public static final String             PROPERTY_BITRATE                                                 = "bitrate";
    public static final String             PROPERTY_TWEET_TEXT                                              = "tweet_text";
    public static final String             PROPERTY_FILENAME_FROM_CRAWLER                                   = "crawlerfilename";
    public static final String             PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY = "video_direct_urls_are_available_via_api_extended_entity";
    public static final String             PROPERTY_TYPE                                                    = "type";
    public static final String             PROPERTY_REPLY                                                   = "reply";

    protected DownloadLink createDownloadlink(final String link, final String tweetid) {
        final DownloadLink ret = super.createDownloadlink(link);
        ret.setProperty("tweetid", tweetid);
        return ret;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "twitter.com" });
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
            String regex = "https?://(?:(?:www|mobile)\\.)?" + buildHostsPatternPart(domains);
            regex += "/(?:";
            regex += "[A-Za-z0-9_\\-]+/status/\\d+";
            regex += "|i/videos/tweet/\\d+";
            regex += "|[A-Za-z0-9_\\-]{2,}(?:/(?:media|likes))?(\\?.*)?";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        resumeURL = param.getDownloadLink() != null ? param.getDownloadLink().getStringProperty(PROPERTY_RESUME) : null;
        br.setAllowedResponseCodes(new int[] { 429 });
        final String newURL = param.getCryptedUrl().replaceFirst("https?://(www\\.|mobile\\.)?twitter\\.com/", "https://" + this.getHost() + "/");
        if (!newURL.equals(param.getCryptedUrl())) {
            logger.info("Currected URL: Old: " + param.getCryptedUrl() + " | New: " + newURL);
            param.setCryptedUrl(newURL);
        }
        br.setFollowRedirects(true);
        /* Some profiles can only be accessed if they accepted others as followers --> Login if the user has added his twitter account */
        final Account account = getUserLogin(false);
        if (account != null) {
            logger.info("Account available and we're logged in");
        } else {
            logger.info("No account available or login failed");
        }
        if (param.getCryptedUrl().matches(TYPE_CARD)) {
            return this.crawlCard(param, account);
        } else if (param.getCryptedUrl().matches(TwitterCom.TYPE_VIDEO_EMBED)) {
            return this.crawlAPISingleTweet(param, new Regex(param.getCryptedUrl(), TwitterCom.TYPE_VIDEO_EMBED).getMatch(0), account);
        } else if (param.getCryptedUrl().matches(TYPE_USER_POST)) {
            return this.crawlSingleTweet(param, account);
        } else {
            return this.crawlUser(param, account);
        }
    }

    @Override
    public void init() {
        super.init();
        setRequestIntervallLimits();
    }

    public static void setRequestIntervallLimits() {
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        Browser.setRequestIntervalLimitGlobal("twimg.com", true, cfg.getGlobalRequestIntervalLimitTwimgComMilliseconds());
        Browser.setRequestIntervalLimitGlobal("api.twitter.com", true, cfg.getGlobalRequestIntervalLimitApiTwitterComMilliseconds());
    }

    @Deprecated
    private boolean switchtoMobile() throws IOException {
        /*
         * 2020-01-30: They're now using a json web-API for which we cannot easily get the auto parameters --> Try mobile website as
         * fallback ...
         */
        logger.info("Trying to switch to mobile website");
        final Form nojs_form = br.getFormbyActionRegex(".+nojs_router.+");
        if (nojs_form != null) {
            logger.info("Switching to mobile website");
            br.submitForm(nojs_form);
            logger.warning("Successfully switched to to mobile website");
            return true;
        } else {
            logger.warning("Failed to switch to mobile website");
            return false;
        }
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlCard(final CryptedLink param, final Account account) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String tweetID = new Regex(param.getCryptedUrl(), TYPE_CARD).getMatch(0);
        if (tweetID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(param.getCryptedUrl());
        if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"ProtectedTimeline\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String externID = br.getRegex("u\\-linkClean js\\-openLink\" href=\"(https?://t\\.co/[^<>\"]*?)\"").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("\"card_ur(?:i|l)\"\\s*:\\s*\"(https?[^<>\"]*?)\"").getMatch(0);
        }
        if (externID != null) {
            decryptedLinks.add(this.createDownloadlink(externID));
            return decryptedLinks;
        }
        if (decryptedLinks.isEmpty()) {
            String dllink = br.getRegex("playlist\\&quot;:\\[\\{\\&quot;source\\&quot;:\\&quot;(https[^<>\"]*?\\.(?:webm|mp4))").getMatch(0);
            if (dllink == null) {
                logger.info("dllink == null");
                return null;
            }
            dllink = dllink.replace("\\", "");
            final String filename = tweetID + "_" + new Regex(dllink, "([^/]+\\.[a-z0-9]+)$").getMatch(0);
            final DownloadLink dl = this.createDownloadlink(dllink, tweetID);
            dl.setProperty(PROPERTY_FILENAME_FROM_CRAWLER, filename);
            dl.setName(filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlSingleTweet(final CryptedLink param, final Account account) throws Exception {
        final boolean prefer_mobile_website = false;
        if (prefer_mobile_website) {
            /* Single Tweet */
            if (switchtoMobile()) {
                return crawlTweetViaMobileWebsite(param.getCryptedUrl(), null);
            }
            /* Fallback to API/normal website */
        }
        final String tweetID = new Regex(param.getCryptedUrl(), TYPE_USER_POST).getMatch(1);
        return crawlAPISingleTweet(param, tweetID, account);
    }

    private ArrayList<DownloadLink> crawlAPISingleTweet(final CryptedLink param, final String tweetID, final Account account) throws Exception {
        if (tweetID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Crawling API tweet");
        prepareAPI(this.br, account);
        final boolean useNewMethod = true; /* 2021-06-15 */
        if (useNewMethod) {
            br.getPage("https://api.twitter.com/1.1/statuses/show/" + tweetID + ".json?cards_platform=Web-12&include_reply_count=1&include_cards=1&include_user_entities=0&tweet_mode=extended");
            handleErrorsAPI(this.br);
            final Map<String, Object> tweet = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            return crawlTweetMap(tweet);
        } else {
            br.getPage("https://api.twitter.com/2/timeline/conversation/" + tweetID + ".json?include_profile_interstitial_type=1&include_blocking=1&include_blocked_by=1&include_followed_by=1&include_want_retweets=1&include_mute_edge=1&include_can_dm=1&include_can_media_tag=1&skip_status=1&cards_platform=Web-12&include_cards=1&include_composer_source=true&include_ext_alt_text=true&include_reply_count=1&tweet_mode=extended&include_entities=true&include_user_entities=true&include_ext_media_color=true&include_ext_media_availability=true&send_error_codes=true&simple_quoted_tweets=true&count=20&ext=mediaStats%2CcameraMoment");
            final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> tweet = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "globalObjects/tweets/" + tweetID);
            return crawlTweetMap(tweet);
        }
    }

    public static Browser prepAPIHeaders(final Browser br) {
        br.getHeaders().put("Authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA");
        final String csrftoken = br.getCookie("twitter.com", TwitterCom.COOKIE_KEY_LOGINED_CSRFTOKEN, Cookies.NOTDELETEDPATTERN);
        if (csrftoken != null) {
            /* Indicates that the user is loggedin. */
            br.getHeaders().put("x-csrf-token", csrftoken);
        } else {
            br.getHeaders().put("x-csrf-token", "undefined");
        }
        br.getHeaders().put("x-twitter-active-user", "yes");
        br.getHeaders().put("x-twitter-client-language", "de");
        br.getHeaders().put("x-twitter-polling", "true");
        return br;
    }

    private Browser prepareAPI(final Browser br, final Account account) throws PluginException, IOException {
        /* 2020-02-03: Static authtoken */
        prepAPIHeaders(br);
        if (account == null) {
            /* Gues token is only needed for anonymous users */
            getAndSetGuestToken(this, br);
        }
        return br;
    }

    public static boolean resetGuestToken() {
        synchronized (GUEST_TOKEN) {
            final boolean ret = GUEST_TOKEN.getAndSet(null) != null;
            if (ret) {
                GUEST_TOKEN_TS.set(-1);
            }
            return ret;
        }
    }

    public static String getAndSetGuestToken(Plugin plugin, final Browser br) throws PluginException, IOException {
        synchronized (GUEST_TOKEN) {
            String guest_token = GUEST_TOKEN.get();
            final long age = Time.systemIndependentCurrentJVMTimeMillis() - GUEST_TOKEN_TS.get();
            if (guest_token == null || age > (30 * 60 * 1000l)) {
                plugin.getLogger().info("Generating new guest_token:age:" + age);
                guest_token = generateNewGuestToken(br);
                if (StringUtils.isEmpty(guest_token)) {
                    plugin.getLogger().warning("Failed to find guest_token");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    GUEST_TOKEN.set(guest_token);
                    GUEST_TOKEN_TS.set(Time.systemIndependentCurrentJVMTimeMillis());
                    plugin.getLogger().warning("Found new guest_token:" + guest_token);
                }
            } else {
                plugin.getLogger().info("Re-using existing guest-token:" + guest_token + "|age:" + age);
            }
            br.getHeaders().put("x-guest-token", guest_token);
            return guest_token;
        }
    }

    public static String generateNewGuestToken(final Browser br) throws IOException {
        final Browser brc = br.cloneBrowser();
        brc.postPage("https://api.twitter.com/1.1/guest/activate.json", "");
        /** TODO: Save guest_token throughout session so we do not generate them so frequently */
        return PluginJSonUtils.getJson(brc, "guest_token");
    }

    /** Wrapper */
    private ArrayList<DownloadLink> crawlTweetMap(final Map<String, Object> tweet) throws MalformedURLException {
        return crawlTweetMap(tweet, null, null);
    }

    /**
     * Crawls single media objects obtained via API.
     *
     * @throws MalformedURLException
     */
    private ArrayList<DownloadLink> crawlTweetMap(final Map<String, Object> tweet, Map<String, Object> user, FilePackage fp) throws MalformedURLException {
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final String tweetID = tweet.get("id_str").toString();
        final Object userInContextOfTweet = tweet.get("user");
        if (userInContextOfTweet != null) {
            /**
             * Prefer this as our user object. </br>
             * It's only included when adding single tweets.
             */
            user = (Map<String, Object>) userInContextOfTweet;
        }
        final String username = (String) user.get("screen_name");
        final String created_at = tweet.get("created_at").toString();
        final long timestamp = getTimestampTwitterDate(created_at);
        final String formattedDate = formatTwitterDateFromTimestamp(timestamp);
        final String tweetText = (String) tweet.get("full_text");
        final Object in_reply_to_status_id_strO = tweet.get("in_reply_to_status_id_str");
        final boolean isReplyToOtherTweet;
        String replyTextForFilename = "";
        if (in_reply_to_status_id_strO != null) {
            isReplyToOtherTweet = true;
            /* Mark filenames of tweet-replies if wished by user. */
            if (cfg.isMarkTweetRepliesViaFilename()) {
                replyTextForFilename += "_reply";
            }
        } else {
            isReplyToOtherTweet = false;
        }
        if (fp != null) {
            /* Assume that we're crawling a complete profile. */
            final String profileDescription = (String) user.get("description");
            if (StringUtils.isEmpty(fp.getComment()) && !StringUtils.isEmpty(profileDescription)) {
                fp.setComment(profileDescription);
            }
        } else {
            /* Assume that we're crawling a single tweet -> Set date + username as packagename. */
            fp = FilePackage.getInstance();
            fp.setName(formattedDate + "_" + username);
            if (!StringUtils.isEmpty(tweetText)) {
                fp.setComment(tweetText);
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<DownloadLink> retExternal = new ArrayList<DownloadLink>();
        final String contentURL = createTwitterPostURL(username, tweetID);
        fp.setAllowInheritance(true);
        fp.setAllowMerge(true);
        final List<Map<String, Object>> medias = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(tweet, "extended_entities/media");
        final String vmapURL = (String) JavaScriptEngineFactory.walkJson(tweet, "card/binding_values/amplify_url_vmap/string_value");
        String lastFoundOriginalFilename = null;
        if (medias != null && !medias.isEmpty()) {
            int mediaIndex = 0;
            for (final Map<String, Object> media : medias) {
                final String type = (String) media.get("type");
                try {
                    final DownloadLink dl;
                    String filename = null;
                    if (type.equals("video") || type.equals("animated_gif")) {
                        /* Find highest video quality */
                        /* animated_gif will usually only have one .mp4 version available with bitrate "0". */
                        int highestBitrate = -1;
                        final List<Map<String, Object>> videoVariants = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(media, "video_info/variants");
                        String streamURL = null;
                        String hlsMaster = null;
                        for (final Map<String, Object> videoVariant : videoVariants) {
                            final String content_type = (String) videoVariant.get("content_type");
                            if (content_type.equalsIgnoreCase("video/mp4")) {
                                final int bitrate = ((Number) videoVariant.get("bitrate")).intValue();
                                if (bitrate > highestBitrate) {
                                    highestBitrate = bitrate;
                                    streamURL = (String) videoVariant.get("url");
                                }
                            } else if (content_type.equalsIgnoreCase("application/x-mpegURL")) {
                                hlsMaster = (String) videoVariant.get("url");
                            } else {
                                logger.info("Skipping unsupported video content_type: " + content_type);
                            }
                        }
                        if (StringUtils.isEmpty(streamURL)) {
                            /* This should never happen */
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        dl = this.createDownloadlink(createVideourl(tweetID));
                        dl.setProperty(PROPERTY_TYPE, "video");
                        lastFoundOriginalFilename = tweetID + "_" + Plugin.getFileNameFromURL(new URL(streamURL));
                        if (cfg.isUseOriginalFilenames()) {
                            filename = lastFoundOriginalFilename;
                        } else if (medias.size() > 1) {
                            filename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename + "_" + mediaIndex + ".mp4";
                        } else {
                            filename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename + ".mp4";
                        }
                        dl.setProperty(PROPERTY_BITRATE, highestBitrate);
                        dl.setProperty(TwitterCom.PROPERTY_DIRECTURL, streamURL);
                        if (!StringUtils.isEmpty(hlsMaster)) {
                            dl.setProperty(TwitterCom.PROPERTY_DIRECTURL_hls_master, hlsMaster);
                        }
                        dl.setProperty(PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY, true);
                    } else if (type.equals("photo")) {
                        String photoURL = (String) media.get("media_url"); /* Also available as "media_url_https" */
                        if (StringUtils.isEmpty(photoURL)) {
                            photoURL = (String) media.get("media_url_https");
                        }
                        if (StringUtils.isEmpty(photoURL)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        dl = this.createDownloadlink(photoURL);
                        dl.setProperty(PROPERTY_TYPE, "photo");
                        lastFoundOriginalFilename = tweetID + "_" + Plugin.getFileNameFromURL(new URL(photoURL));
                        if (cfg.isUseOriginalFilenames()) {
                            filename = lastFoundOriginalFilename;
                        } else if (medias.size() > 1) {
                            filename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename + "_" + mediaIndex + Plugin.getFileNameExtensionFromURL(photoURL);
                        } else {
                            filename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename + Plugin.getFileNameExtensionFromURL(photoURL);
                        }
                    } else {
                        /* Unknown type -> This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown media type:" + type);
                    }
                    dl.setContentUrl(contentURL);
                    if (filename != null) {
                        dl.setFinalFileName(filename);
                    }
                    dl.setAvailable(true);
                    dl.setProperty(PROPERTY_MEDIA_INDEX, mediaIndex);
                    dl.setProperty(PROPERTY_MEDIA_ID, media.get("id_str").toString());
                    ret.add(dl);
                } catch (PluginException e) {
                    logger.log(e);
                }
                mediaIndex += 1;
            }
        } else if (!StringUtils.isEmpty(vmapURL)) {
            /* Fallback handling for very old (???) content */
            /* Expect such URLs which our host plugin can handle: https://video.twimg.com/amplify_video/vmap/<numbers>.vmap */
            final DownloadLink singleVideo = this.createDownloadlink(vmapURL);
            singleVideo.setContentUrl(contentURL);
            final String finalFilename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename + ".mp4";
            singleVideo.setFinalFileName(finalFilename);
            singleVideo.setProperty(PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY, false);
            singleVideo.setProperty(PROPERTY_MEDIA_INDEX, 0);
            singleVideo.setProperty(PROPERTY_TYPE, "video");
            singleVideo.setAvailable(true);
            ret.add(singleVideo);
        }
        int itemsSkippedDueToPluginSettings = 0;
        if (!StringUtils.isEmpty(tweetText)) {
            final String[] urlsInPostText = HTMLParser.getHttpLinks(tweetText, br.getURL());
            if (cfg.isCrawlURLsInsideTweetText() && urlsInPostText.length > 0) {
                for (final String url : urlsInPostText) {
                    final DownloadLink dl = this.createDownloadlink(url);
                    retExternal.add(dl);
                }
            } else if (urlsInPostText != null) {
                itemsSkippedDueToPluginSettings += urlsInPostText.length;
            }
            if (cfg.isAddTweetTextAsTextfile()) {
                final DownloadLink text = this.createDownloadlink(contentURL);
                final String filename;
                if (cfg.isUseOriginalFilenames() && lastFoundOriginalFilename != null) {
                    /*
                     * In case tweet contains media, we can use the same filename as that media if user prefers original filename. We just
                     * need to modify the extension.
                     */
                    filename = lastFoundOriginalFilename.substring(0, lastFoundOriginalFilename.lastIndexOf(".")) + ".txt";
                } else {
                    filename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename + ".txt";
                }
                text.setFinalFileName(filename);
                try {
                    text.setDownloadSize(tweetText.getBytes("UTF-8").length);
                } catch (final UnsupportedEncodingException ignore) {
                    ignore.printStackTrace();
                }
                text.setProperty(PROPERTY_MEDIA_INDEX, 0);
                text.setProperty(PROPERTY_TYPE, "text");
                text.setAvailable(true);
                ret.add(text);
            } else {
                itemsSkippedDueToPluginSettings++;
            }
        }
        /* Add remaining plugin properties */
        for (final DownloadLink dl : ret) {
            dl.setProperty(PROPERTY_USERNAME, username);
            dl.setProperty(PROPERTY_DATE, formattedDate);
            dl.setProperty(PROPERTY_DATE_TIMESTAMP, timestamp);
            if (!StringUtils.isEmpty(tweetText)) {
                dl.setProperty(PROPERTY_TWEET_TEXT, tweetText);
            }
            if (isReplyToOtherTweet) {
                dl.setProperty(PROPERTY_REPLY, true);
            }
            if (dl.getFinalFileName() != null) {
                dl.setProperty(PROPERTY_FILENAME_FROM_CRAWLER, dl.getFinalFileName());
            }
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            distribute(dl);
        }
        if (fp != null) {
            fp.addLinks(retExternal);
        }
        ret.addAll(retExternal);
        this.distributeAll(retExternal);
        /* Logger just in case nothing was added. */
        if (ret.isEmpty()) {
            if (itemsSkippedDueToPluginSettings == 0) {
                logger.info("Failed to find any crawlable content in tweet: " + tweetID);
            } else {
                logger.info("Failed to find any crawlable content because of user settings. Crawlable but skipped " + itemsSkippedDueToPluginSettings + " item(s) due to users' plugin settings.");
            }
        }
        return ret;
    }

    private static long getTimestampTwitterDate(String created_at) {
        created_at = created_at.substring(created_at.indexOf(" ") + 1, created_at.length());
        return TimeFormatter.getMilliSeconds(created_at, "MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
    }

    private static Date getDateFromTwitterDate(final String created_at) {
        return new Date(getTimestampTwitterDate(created_at));
    }

    private static String formatTwitterDate(String created_at) {
        return formatTwitterDateFromDate(getDateFromTwitterDate(created_at));
    }

    private static String formatTwitterDateFromDate(final Date date) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    private static String formatTwitterDateFromTimestamp(final long timestamp) {
        return formatTwitterDateFromDate(new Date(timestamp));
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlTweetViaMobileWebsite(final String tweetURL, final FilePackage fp) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        logger.info("Crawling mobile website tweet");
        final String tweet_id = new Regex(tweetURL, "/(?:tweet|status)/(\\d+)").getMatch(0);
        if (br.containsHTML("/status/" + tweet_id + "/video/1")) {
            /* Video */
            final DownloadLink dl = createDownloadlink(createVideourl(tweet_id));
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            decryptedLinks.add(dl);
            distribute(dl);
        } else if (br.containsHTML("/tweet_video_thumb/")) {
            /* .gif --> Can be downloaded as .mp4 video */
            final DownloadLink dl = createDownloadlink(createVideourl(tweet_id));
            decryptedLinks.add(dl);
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            distribute(dl);
        } else {
            /* Picture or text */
            final String[] regexes = { "(https?://[^<>\"]+/media/[A-Za-z0-9\\-_]+(\\.(?:jpg|png|gif):[a-z]+))" };
            for (final String regex : regexes) {
                final String[] alllinks = br.getRegex(regex).getColumn(0);
                if (alllinks != null && alllinks.length > 0) {
                    for (String alink : alllinks) {
                        final Regex fin_al = new Regex(alink, "https?://[^<>\"]+/[^/]+/([A-Za-z0-9\\-_]+)\\.([a-z0-9]+)(:[a-z]+)?$");
                        final String servername = fin_al.getMatch(0);
                        final String ending = fin_al.getMatch(1);
                        final String quality = fin_al.getMatch(2);
                        final String final_filename = tweet_id + "_" + servername + "." + ending;
                        alink = Encoding.htmlDecode(alink.trim());
                        /* Always get the best quality. Possible qualities: thumb, small, medium, large, orig */
                        if (!quality.equalsIgnoreCase("large")) {
                            alink = alink.replace(quality, ":large");
                        }
                        final DownloadLink dl = createDownloadlink(alink, tweet_id);
                        dl.setAvailable(true);
                        dl.setProperty(PROPERTY_FILENAME_FROM_CRAWLER, final_filename);
                        dl.setProperty(PROPERTY_TYPE, "photo");
                        dl.setName(final_filename);
                        if (fp != null) {
                            dl._setFilePackage(fp);
                        }
                        decryptedLinks.add(dl);
                        distribute(dl);
                    }
                }
            }
            if (decryptedLinks.isEmpty()) {
                logger.warning("Found nothing - either only text or plugin broken :(");
                decryptedLinks.add(this.createOfflinelink(tweetURL));
            }
        }
        return decryptedLinks;
    }

    /** Returns true if an account is required to process the given URL. */
    private boolean requiresAccount(final String url) {
        if (url.matches(TYPE_USER_LIKES)) {
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param, final Account account) throws Exception {
        if (PluginJsonConfig.get(TwitterConfigInterface.class).isCrawlRetweets()) {
            return this.crawlUserViaWebAPI(param, account);
        } else {
            return crawlUserViaAPI(param, account);
        }
    }

    /** Crawls only tweets that were posted by the profile in given URL, no re-tweets!!! */
    private ArrayList<DownloadLink> crawlUserViaAPI(final CryptedLink param, final Account account) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        logger.info("Crawling user profile via API");
        final String username = new Regex(param.getCryptedUrl(), TYPE_USER_ALL).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (requiresAccount(param.getCryptedUrl()) && account == null) {
            logger.info("Account required to crawl all liked items of a user");
            throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "ACCOUNT_REQUIRED_TO_CRAWL_LIKED_ITEMS_OF_PROFILE_" + username, "Account is required to crawl liked items of profiles.");
        }
        this.prepareAPI(br, account);
        final Map<String, Object> user = this.getUserInfo(br, account, username);
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final String userID = user.get("id_str").toString();
        /* = number of tweets */
        final int tweet_count = ((Number) user.get("statuses_count")).intValue();
        /* = number of tweets containing media (can be lower [ar also higher?] than "statuses_count") */
        final int media_count = ((Number) user.get("media_count")).intValue();
        // final boolean force_grab_media = cfg.isForceGrabMediaOnlyEnabled();
        final boolean force_grab_media = true;
        /* Grab only content posted by user or grab everything from his timeline e.g. also re-tweets. */
        final String content_type;
        Integer maxCount = null;
        final int expected_items_per_page = 20;
        final UrlQuery query = new UrlQuery();
        query.add("include_profile_interstitial_type", "1");
        query.add("include_blocking", "1");
        query.add("include_blocked_by", "1");
        query.add("include_followed_by", "1");
        query.add("include_want_retweets", "1");
        query.add("include_mute_edge", "1");
        query.add("include_can_dm", "1");
        query.add("include_can_media_tag", "1");
        query.add("skip_status", "1");
        query.add("cards_platform", "Web-12");
        query.add("include_cards", "1");
        /* 2020-08-24: Not required anymore */
        // query.add("include_composer_source", "true");
        query.add("include_quote_count", "true");
        query.add("c", "true");
        query.add("include_reply_count", "1");
        query.add("tweet_mode", "extended");
        query.add("include_entities", "true");
        query.add("include_user_entities", "true");
        query.add("include_ext_media_color", "true");
        query.add("include_ext_media_availability", "true");
        query.add("include_ext_sensitive_media_warning", "true");
        query.add("send_error_codes", "true");
        query.add("simple_quoted_tweet", "true");
        final FilePackage fp = FilePackage.getInstance();
        if (param.getCryptedUrl().matches(TYPE_USER_LIKES)) {
            /* Crawl all liked items of a user */
            logger.info("Crawling all liked items of user " + username);
            content_type = "favorites";
            final int favoritesCount = ((Number) user.get("favourites_count")).intValue();
            if (favoritesCount == 0) {
                ret.add(getDummyErrorProfileContainsNoLikedItems(username));
                return ret;
            }
            maxCount = favoritesCount;
            query.append("simple_quoted_tweets", "true", false);
            query.append("sorted_by_time", "true", false);
            fp.setName(username + " - likes");
        } else if (param.getCryptedUrl().matches(TYPE_USER_MEDIA) || force_grab_media) {
            logger.info("Crawling self posted media only from user: " + username);
            if (media_count == 0) {
                ret.add(getDummyErrorProfileContainsNoMediaItems(username));
                return ret;
            }
            content_type = "media";
            maxCount = media_count;
            fp.setName(username);
        } else {
            /*
             * 2022-03-18: Legacy - not used anymore! This endpoint has either been removed by twitter or we're using it wrong. It would
             * always return error 429 rate limit reached!
             */
            /* TODO: Remove this after 08-2022 */
            logger.info("Crawling ALL media of a user e.g. also retweets | user: " + username);
            if (tweet_count == 0) {
                /* Profile contains zero tweets! */
                ret.add(getDummyErrorProfileContainsNoTweets(username));
                return ret;
            }
            content_type = "profile";
            maxCount = tweet_count;
            query.add("include_tweet_replies", "false");
            fp.setName(username);
        }
        query.append("userId", userID, false);
        query.append("count", expected_items_per_page + "", false);
        query.append("ext", "mediaStats,cameraMoment", true);
        final String addedURLWithoutParams = URLHelper.getURL(new URL(param.getCryptedUrl()), false, false, false).toString();
        final UrlQuery addedURLQuery = UrlQuery.parse(resumeURL);
        Number maxTweetsToCrawl = null;
        final String maxTweetDateStr = addedURLQuery.get("max_date");
        long crawlUntilTimestamp = -1;
        try {
            maxTweetsToCrawl = Integer.parseInt(addedURLQuery.get("maxitems"));
        } catch (final Throwable ignore) {
        }
        if (maxTweetDateStr != null) {
            try {
                crawlUntilTimestamp = TimeFormatter.getMilliSeconds(maxTweetDateStr, "yyyy-MM-dd", Locale.ENGLISH);
            } catch (final Throwable ignore) {
                logger.info("Ignoring user defined 'max_date' parameter because of invalid input format: " + maxTweetDateStr);
            }
        }
        int totalWalkedThroughTweetsCount = 0;
        int page = 1;
        String nextCursor = null;
        try {
            page = Integer.parseInt(addedURLQuery.get("page"));
            totalWalkedThroughTweetsCount = Integer.parseInt(addedURLQuery.get("totalCrawledTweetsCount"));
            nextCursor = Encoding.htmlDecode(addedURLQuery.get("nextCursor"));
            logger.info("Resuming from last state: page = " + page + " | totalCrawledTweetsCount = " + totalWalkedThroughTweetsCount + " | nextCursor = " + nextCursor);
        } catch (final Throwable e) {
        }
        final HashSet<String> cursorDupes = new HashSet<String>();
        final String apiURL = "https://api.twitter.com/2/timeline/" + content_type + "/" + userID + ".json";
        tweetTimeline: do {
            final UrlQuery thisquery = query;
            if (!StringUtils.isEmpty(nextCursor)) {
                thisquery.append("cursor", nextCursor, true);
            }
            br.getPage(apiURL + "?" + thisquery.toString());
            handleErrorsAPI(this.br);
            final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Map<String, Object> globalObjects = (Map<String, Object>) root.get("globalObjects");
            final Map<String, Object> users = (Map<String, Object>) globalObjects.get("users");
            final List<Object> pagination_info = (List<Object>) JavaScriptEngineFactory.walkJson(root, "timeline/instructions/{0}/addEntries/entries");
            final Map<String, Object> tweetMap = (Map<String, Object>) globalObjects.get("tweets");
            final Iterator<Entry<String, Object>> iterator = tweetMap.entrySet().iterator();
            String lastCreatedAtDateStr = null;
            while (iterator.hasNext()) {
                final Map<String, Object> tweet = (Map<String, Object>) iterator.next().getValue();
                final Map<String, Object> userWhoPostedThisTweet = (Map<String, Object>) users.get(tweet.get("user_id_str").toString());
                ret.addAll(crawlTweetMap(tweet, userWhoPostedThisTweet, fp));
                totalWalkedThroughTweetsCount++;
                lastCreatedAtDateStr = (String) tweet.get("created_at");
                final long currentTweetTimestamp = TimeFormatter.getMilliSeconds(lastCreatedAtDateStr, "EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
                /* Check some abort conditions */
                if (maxTweetsToCrawl != null && totalWalkedThroughTweetsCount >= maxTweetsToCrawl.intValue()) {
                    logger.info("Stopping because: Reached user defined max items count: " + maxTweetsToCrawl);
                    break tweetTimeline;
                } else if (crawlUntilTimestamp != -1 && currentTweetTimestamp > crawlUntilTimestamp) {
                    logger.info("Stopping because: Reached max desired tweet age of: " + maxTweetDateStr);
                    break tweetTimeline;
                }
            }
            logger.info("Crawled page " + page + " | Tweets crawled so far: " + totalWalkedThroughTweetsCount + "/" + maxCount.intValue() + " | lastCreatedAtDateStr = " + lastCreatedAtDateStr + " | last nextCursor = " + nextCursor);
            if (tweetMap.size() < expected_items_per_page) {
                /**
                 * This can sometimes happen! </br>
                 * We'll ignore this and let it run into our other fail-safe for when a page contains zero items.
                 */
                logger.info(String.format("Current page contained only %d of max. %d expected objects --> Reached the end?", tweetMap.size(), expected_items_per_page));
                // break;
            }
            /* Done - now try to find string required to access next page. */
            try {
                final Map<String, Object> pagination_info_entries = (Map<String, Object>) pagination_info.get(pagination_info.size() - 1);
                final String entryId = (String) pagination_info_entries.get("entryId");
                if (!entryId.contains("cursor-bottom")) {
                    logger.info("Stopping because: Found wrong cursor object --> Plugin probably needs update");
                    break;
                }
                nextCursor = (String) JavaScriptEngineFactory.walkJson(pagination_info_entries, "content/operation/cursor/value");
                if (StringUtils.isEmpty(nextCursor)) {
                    logger.info("Stopping because: Failed to find nextCursor");
                    break;
                } else if (!cursorDupes.add(nextCursor)) {
                    logger.info("Stopping because: We've already crawled current cursor: " + nextCursor);
                    break;
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("Stopping because: Failed to get nextCursor (Exception occured)");
                break;
            }
            /** Store this information in URL so in case crawler fails, it will resume from previous position if user adds that URL. */
            addedURLQuery.addAndReplace("page", Integer.toString(page));
            addedURLQuery.addAndReplace("totalCrawledTweetsCount", Integer.toString(totalWalkedThroughTweetsCount));
            addedURLQuery.addAndReplace("nextCursor", Encoding.urlEncode(nextCursor));
            resumeURL = addedURLWithoutParams + "?" + addedURLQuery.toString();
            page++;
            /* Wait before accessing next page. */
            this.sleep(cfg.getProfileCrawlerWaittimeBetweenPaginationMilliseconds(), param);
        } while (!this.isAbort());
        logger.info("Done after " + page + " pages | last nextCursor = " + nextCursor);
        if (ret.isEmpty()) {
            logger.info("Found nothing --> Either user has no posts containing media or those can only be viewed by certain users or only when logged in (explicit content)");
            if (account == null) {
                throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "PROFILE_CONTAINS_ONLY_EXPLICIT_CONTENT_ACCOUNT_REQUIRED_" + username, "Profile " + username + " contains only explicit content which can only be viewed when logged in --> Add a twitter account to JDownloader and try again!");
            } else {
                ret.add(getDummyErrorProfileContainsNoDownloadableContent(username));
            }
        }
        return ret;
    }
    // private Number getMaxItemsToCrawl(final String url) {
    // try {
    // return Integer.parseInt(UrlQuery.parse(url).get("maxitems"));
    // } catch (final Throwable e) {
    // return null;
    // }
    // }

    /** Crawls tweets AND re-tweets of profile in given URL. */
    private ArrayList<DownloadLink> crawlUserViaWebAPI(final CryptedLink param, final Account account) throws Exception {
        final String username = new Regex(param.getCryptedUrl(), TYPE_USER_ALL).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser brc = br.cloneBrowser();
        /* TODO: Auto-fetch this URL from mainpage as it will change over time. */
        brc.getPage("https://abs.twimg.com/responsive-web/client-web/main.d7efe778.js");
        final String queryID = brc.getRegex("queryId\\s*:\\s*\"([^\"]+)\",\\s*operationName\\s*:\\s*\"UserTweets\"").getMatch(0);
        if (queryID == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String userID = this.getUserID(br, account, username);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        String nextCursor = null;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int totalCrawledTweetsCount = 0;
        int page = 1;
        do {
            final Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("userId", userID);
            variables.put("count", 40);
            if (nextCursor != null) {
                variables.put("cursor", nextCursor);
            }
            variables.put("includePromotedContent", true);
            variables.put("withQuickPromoteEligibilityTweetFields", true);
            variables.put("withSuperFollowsUserFields", true);
            variables.put("withDownvotePerspective", false);
            variables.put("withReactionsMetadata", false);
            variables.put("withReactionsPerspective", false);
            variables.put("withSuperFollowsTweetFields", true);
            variables.put("withVoice", true);
            variables.put("withV2Timeline", true);
            final UrlQuery query = new UrlQuery();
            query.add("variables", Encoding.urlEncode(JSonStorage.serializeToJson(variables)));
            query.add("features", "%7B%22dont_mention_me_view_api_enabled%22%3Atrue%2C%22interactive_text_enabled%22%3Atrue%2C%22responsive_web_uc_gql_enabled%22%3Afalse%2C%22vibe_tweet_context_enabled%22%3Afalse%2C%22responsive_web_edit_tweet_api_enabled%22%3Afalse%2C%22standardized_nudges_misinfo%22%3Afalse%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D");
            br.getPage("https://twitter.com/i/api/graphql/" + queryID + "/UserTweets?" + query.toString());
            int crawledTweetsThisPage = 0;
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final List<Map<String, Object>> timelineInstructions = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "data/user/result/timeline_v2/timeline/instructions");
            for (final Map<String, Object> timelineInstruction : timelineInstructions) {
                if (!timelineInstruction.get("type").toString().equalsIgnoreCase("TimelineAddEntries")) {
                    continue;
                }
                final List<Map<String, Object>> timelineEntries = (List<Map<String, Object>>) timelineInstruction.get("entries");
                for (final Map<String, Object> timelineEntry : timelineEntries) {
                    final Map<String, Object> content = (Map<String, Object>) timelineEntry.get("content");
                    final String contentType = (String) content.get("entryType");
                    if (contentType.equalsIgnoreCase("TimelineTimelineCursor")) {
                        if (content.get("cursorType").toString().equalsIgnoreCase("Bottom")) {
                            nextCursor = content.get("value").toString();
                            /* We've reached the end of current page */
                            break;
                        }
                    } else {
                        final Map<String, Object> result = (Map<String, Object>) JavaScriptEngineFactory.walkJson(content, "itemContent/tweet_results/result");
                        if (!result.get("__typename").toString().equalsIgnoreCase("Tweet")) {
                            continue;
                        }
                        // TODO: Check if this is the correct user object
                        final Map<String, Object> usr = (Map<String, Object>) JavaScriptEngineFactory.walkJson(result, "core/user_results/result/legacy");
                        final Map<String, Object> tweet = (Map<String, Object>) result.get("legacy");
                        if (tweet == null) {
                            continue;
                        }
                        ret.addAll(crawlTweetMap(tweet, usr, fp));
                        totalCrawledTweetsCount++;
                        crawledTweetsThisPage++;
                    }
                }
            }
            logger.info("Crawled page " + page + " | Found tweets on this page: " + crawledTweetsThisPage + " | Total tweets: " + totalCrawledTweetsCount);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (StringUtils.isEmpty(nextCursor)) {
                logger.info("Stopping because: Failed to find nextCursor");
                break;
            } else if (crawledTweetsThisPage == 0) {
                logger.info("Stopping because: Failed to find any new items on current page");
                break;
            } else if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                logger.info("Stopping because: Re-tweet crawler pagination is currently only availble for developers (unfinished feature)");
                break;
            }
        } while (true);
        return ret;
    }

    /**
     * Obtains information about given username via old API. </br>
     * The response of this will also expose the users' userID which is often needed to perform further API requests.
     */
    private Map<String, Object> getUserInfo(final Browser br, final Account account, final String username) throws Exception {
        this.prepareAPI(br, account);
        final boolean use_old_api_to_get_userid = true;
        final Map<String, Object> user;
        if (use_old_api_to_get_userid) {
            /* https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-users-show */
            /* https://developer.twitter.com/en/docs/twitter-api/rate-limits */
            /* per 15 mins window, 300 per app, 900 per user */
            br.getPage("https://api.twitter.com/1.1/users/lookup.json?screen_name=" + username);
            if (br.getHttpConnection().getResponseCode() == 403) {
                /* {"errors":[{"code":22,"message":"Not authorized to view the specified user."}]} */
                throw new AccountRequiredException();
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* {"errors":[{"code":17,"message":"No user matches for specified terms."}]} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Object responseO = JSonStorage.restoreFromString(br.toString(), TypeRef.OBJECT);
            if (!(responseO instanceof List)) {
                logger.warning("Unknown API error/response");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final List<Map<String, Object>> users = (List<Map<String, Object>>) responseO;
            user = users.get(0);
        } else {
            br.getPage("https://api.twitter.com/graphql/DO_NOT_USE_ATM_2020_02_05/UserByScreenName?variables=%7B%22screen_name%22%3A%22" + username + "%22%2C%22withHighlightedLabel%22%3Afalse%7D");
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            user = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/user");
            // userID = (String) user.get("rest_id");
        }
        return user;
    }

    private String getUserID(final Browser br, final Account account, final String username) throws Exception {
        final Map<String, Object> user = getUserInfo(br, account, username);
        return user.get("id_str").toString();
    }

    private final String PROPERTY_RESUME = "twitterResume";

    @Override
    protected DownloadLink createLinkCrawlerRetry(final CrawledLink link, final DecrypterRetryException retryException) {
        final DownloadLink ret = super.createLinkCrawlerRetry(link, retryException);
        if (ret != null && resumeURL != null) {
            ret.setProperty(PROPERTY_RESUME, resumeURL);
        }
        return ret;
    }

    private DownloadLink getDummyErrorProfileContainsNoLikedItems(final String username) {
        final DownloadLink dummy = this.createOfflinelink(createTwitterProfileURLLikes(username), "PROFILE_CONTAINS_NO_LIKES_" + username, "The profile " + username + " does not contain any liked items.");
        return dummy;
    }

    private DownloadLink getDummyErrorProfileContainsNoTweets(final String username) {
        final DownloadLink dummy = this.createOfflinelink(createTwitterProfileURL(username), "PROFILE_CONTAINS_NO_TWEETS_" + username, "The profile " + username + " does not contain any tweets.");
        return dummy;
    }

    private DownloadLink getDummyErrorProfileContainsNoMediaItems(final String username) {
        final DownloadLink dummy = this.createOfflinelink(createTwitterProfileURL(username), "PROFILE_CONTAINS_NO_MEDIA_ITEMS_" + username, "The profile " + username + " does not contain any tweets containing media items.");
        return dummy;
    }

    private DownloadLink getDummyErrorProfileContainsNoDownloadableContent(final String username) {
        final DownloadLink dummy = this.createOfflinelink(createTwitterProfileURL(username), "PROFILE_CONTAINS_NO_DOWNLOADABLE_CONTENT_" + username, "The profile " + username + " does not appear to contain any downloadable content. Check your twitter plugin settings maybe you've turned off some of the crawlable content.");
        return dummy;
    }

    /**
     * https://developer.twitter.com/en/support/twitter-api/error-troubleshooting </br>
     * Scroll down to "Twitter API error codes"
     */
    private void handleErrorsAPI(final Browser br) throws Exception {
        Map<String, Object> entries = null;
        try {
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        } catch (final Exception e) {
            /* Check for some pure http error-responsecodes. */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 429) {
                throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, "Rate-Limit reached");
            } else if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        final Object errorsO = entries.get("errors");
        if (errorsO != null) {
            final List<Map<String, Object>> errors = (List<Map<String, Object>>) errorsO;
            for (final Map<String, Object> error : errors) {
                final int code = ((Number) error.get("code")).intValue();
                switch (code) {
                case 34:
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                case 63:
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                // case 88:
                /* {"errors":[{"message":"Rate limit exceeded","code":88}]} */
                // final String rateLimitResetTimestamp = br.getRequest().getResponseHeader("x-rate-limit-reset");
                // if (rateLimitResetTimestamp != null && rateLimitResetTimestamp.matches("\\d+")) {
                // logger.info("Rate-limit reached | Resets in: " +
                // TimeFormatter.formatMilliSeconds(Long.parseLong(rateLimitResetTimestamp) - System.currentTimeMillis() / 1000, 0));
                // }
                case 109:
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                case 144:
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                case 325:
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                case 421:
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                case 220:
                    /* {"errors":[{"code":220,"message":"Your credentials do not allow access to this resource."}]} */
                    throw new AccountRequiredException();
                default:
                    throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, error.get("message").toString());
                }
            }
        }
    }

    /* 2020-01-30: Mobile website will only show 1 tweet per page */
    @Deprecated
    private ArrayList<DownloadLink> crawlUserViaMobileWebsite(final String parameter) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        logger.info("Crawling mobile website user");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (!this.switchtoMobile()) {
            logger.warning("Unable to crawl: Failed to switch to mobile website");
            return decryptedLinks;
        }
        final String username = new Regex(parameter, "https?://[^/]+/([^/]+)").getMatch(0);
        int index = 0;
        String nextURL = null;
        final boolean crawl_tweets_separately = true;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        do {
            logger.info("Crawling page " + (index + 1));
            if (nextURL != null) {
                br.getPage(nextURL);
            }
            final String current_tweet_id = br.getRegex("name=\"tweet_(\\d+)\"").getMatch(0);
            if (current_tweet_id == null) {
                logger.warning("Failed to find current_tweet_id");
                break;
            }
            final String tweet_url = String.format("https://twitter.com/%s/status/%s", username, current_tweet_id);
            if (crawl_tweets_separately) {
                /* These URLs will go back into the crawler to get crawled separately */
                final DownloadLink dl = this.createDownloadlink(tweet_url);
                decryptedLinks.add(dl);
                distribute(dl);
            } else {
                decryptedLinks.addAll(crawlTweetViaMobileWebsite(tweet_url, fp));
            }
            index++;
            nextURL = br.getRegex("(/[^/]+/media/grid\\?idx=" + index + ")").getMatch(0);
        } while (nextURL != null && !this.isAbort());
        logger.info(String.format("Done after %d pages", index));
        return decryptedLinks;
    }

    protected void getPage(final Browser br, final String url) throws Exception {
        br.getPage(url);
        if (br.getHttpConnection().getResponseCode() == 429) {
            logger.info("Error 429 too many requests - add less URLs and/or perform a reconnect!");
        }
    }

    protected void getPage(final String url) throws Exception {
        getPage(br, url);
    }

    public static String createTwitterProfileURL(final String user) {
        return "https://twitter.com/" + user;
    }

    public static String createTwitterProfileURLLikes(final String user) {
        return "https://twitter.com/" + user + "/likes";
    }

    public static String createTwitterProfileURLMedia(final String user) {
        return "https://twitter.com/" + user + "/media";
    }

    public static String createVideourl(final String tweetID) {
        return "https://twitter.com/i/videos/tweet/" + tweetID;
    }

    public static String createTwitterPostURL(final String user, final String tweetID) {
        return "https://twitter.com/" + user + "/status/" + tweetID;
    }

    /** Log in the account via hostplugin */
    private Account getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = getNewPluginForHostInstance("twitter.com");
        final Account aa = AccountController.getInstance().getValidAccount("twitter.com");
        if (aa == null) {
            return null;
        }
        try {
            ((jd.plugins.hoster.TwitterCom) hostPlugin).login(aa, force);
            return aa;
        } catch (final PluginException e) {
            handleAccountException(hostPlugin, aa, e);
            logger.log(e);
            return null;
        }
    }

    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-30: We have to perform a lot of requests --> Set this to 1. */
        return 1;
    }
}
