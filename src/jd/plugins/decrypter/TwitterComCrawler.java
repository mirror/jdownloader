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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.config.TwitterConfigInterface;
import org.jdownloader.plugins.components.config.TwitterConfigInterface.FilenameScheme;
import org.jdownloader.plugins.components.config.TwitterConfigInterface.SingleTweetCrawlerMode;
import org.jdownloader.plugins.components.config.TwitterConfigInterface.SingleTweetCrawlerTextCrawlMode;
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
import jd.plugins.hoster.GenericM3u8;
import jd.plugins.hoster.TwitterCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TwitterComCrawler extends PluginForDecrypt {
    private String  resumeURL                                     = null;
    private Number  maxTweetsToCrawl                              = null;
    private String  maxTweetDateStr                               = null;
    private Long    crawlUntilTimestamp                           = null;
    private Integer preGivenNumberOfTotalWalkedThroughTweetsCount = null;
    private Integer preGivenPageNumber                            = null;
    private String  preGivenNextCursor                            = null;

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
    /* Number of media items in the source-tweet. */
    public static final String             PROPERTY_MEDIA_COUNT                                             = "media_count";
    public static final String             PROPERTY_MEDIA_ID                                                = "mediaid";
    public static final String             PROPERTY_BITRATE                                                 = "bitrate";
    public static final String             PROPERTY_TWEET_TEXT                                              = "tweet_text";
    public static final String             PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY = "video_direct_urls_are_available_via_api_extended_entity";
    public static final String             PROPERTY_TYPE                                                    = "type";
    public static final String             TYPE_PHOTO                                                       = "photo";
    public static final String             TYPE_TEXT                                                        = "text";
    public static final String             TYPE_VIDEO                                                       = "video";
    public static final String             PROPERTY_REPLY                                                   = "reply";
    public static final String             PROPERTY_RETWEET                                                 = "retweet";
    public static final String             PROPERTY_TWEET_ID                                                = "tweetid";
    public static final String             PROPERTY_RELATED_ORIGINAL_FILENAME                               = "related_original_filename";
    private final String                   API_BASE_v2                                                      = "https://api.twitter.com/2";
    private final String                   API_BASE_GRAPHQL                                                 = "https://twitter.com/i/api/graphql";
    private static Map<String, String>     graphqlQueryids                                                  = new HashMap<String, String>();
    /**
     * Enabled Juli 2023: https://www.reuters.com/technology/twitter-now-needs-users-sign-view-tweets-2023-06-30/ </br>
     * Disabled August 2023: https://techcrunch.com/2023/07/05/twitter-silently-removes-login-requirement-for-viewing-tweets/
     */
    public static final boolean            ACCOUNT_IS_ALWAYS_REQUIRED                                       = false;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "twitter.com", "x.com" });
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

    private String getContentURL(final CryptedLink param) {
        return param.getCryptedUrl().replaceFirst("(?i)https?://(www\\.|mobile\\.)?twitter\\.com/", "https://" + this.getHost() + "/");
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        this.resumeURL = param.getDownloadLink() != null ? param.getDownloadLink().getStringProperty(PROPERTY_RESUME_URL) : null;
        /* Parse some special URL params which are relevant for profile crawl process later. */
        final UrlQuery query;
        if (this.resumeURL != null) {
            query = UrlQuery.parse(resumeURL);
        } else {
            query = UrlQuery.parse(param.getCryptedUrl());
        }
        try {
            final int maxItemsToCrawlTmp = Integer.parseInt(query.get("maxitems"));
            if (maxItemsToCrawlTmp > 0) {
                maxTweetsToCrawl = maxItemsToCrawlTmp;
            }
        } catch (final Throwable ignore) {
        }
        final String maxTweetDateStrTmp = query.get("max_date");
        if (maxTweetDateStrTmp != null) {
            try {
                crawlUntilTimestamp = TimeFormatter.getMilliSeconds(maxTweetDateStrTmp, "yyyy-MM-dd", Locale.ENGLISH);
                /* Date has been validated --> Put into public var */
                this.maxTweetDateStr = maxTweetDateStrTmp;
            } catch (final Throwable ignore) {
                logger.info("Ignoring user defined 'max_date' parameter because of invalid input format: " + maxTweetDateStrTmp);
            }
        }
        try {
            final int preGivenPageNumberTmp = Integer.parseInt(query.get("page"));
            if (preGivenPageNumberTmp > 0) {
                preGivenPageNumber = preGivenPageNumberTmp;
            }
            preGivenNumberOfTotalWalkedThroughTweetsCount = Integer.parseInt(query.get("totalCrawledTweetsCount"));
            preGivenNextCursor = Encoding.htmlDecode(query.get("nextCursor"));
            logger.info("Resuming from last state: page = " + preGivenPageNumber + " | totalCrawledTweetsCount = " + preGivenNumberOfTotalWalkedThroughTweetsCount + " | nextCursor = " + preGivenNextCursor);
        } catch (final Throwable ignore) {
        }
        br.setAllowedResponseCodes(new int[] { 429 });
        final String newURL = getContentURL(param);
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
            if (ACCOUNT_IS_ALWAYS_REQUIRED) {
                throw new AccountRequiredException();
            }
        }
        if (param.getCryptedUrl().matches(TYPE_CARD)) {
            return this.crawlCard(param, account);
        } else if (param.getCryptedUrl().matches(TwitterCom.TYPE_VIDEO_EMBED)) {
            return this.crawlSingleTweet(account, null, new Regex(param.getCryptedUrl(), TwitterCom.TYPE_VIDEO_EMBED).getMatch(0));
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

    private String getGraphqlQueryID(final String operationName) throws IOException, PluginException {
        synchronized (graphqlQueryids) {
            if (graphqlQueryids.isEmpty() || !graphqlQueryids.containsKey(operationName) || true) {
                final Browser br = new Browser();
                br.getPage("https://abs.twimg.com/responsive-web/client-web/api.b9f7c7ea.js");
                final HashSet<String> operationNamesToCache = new HashSet<String>();
                operationNamesToCache.add("UserByScreenName");
                operationNamesToCache.add("UserTweets");
                operationNamesToCache.add("TweetDetail");
                operationNamesToCache.add("TweetResultByRestId");
                /* Just in case this isn't in here already */
                if (operationNamesToCache.add(operationName)) {
                    logger.info("!Developer! Update list of defaultIDs and add operationName: " + operationName);
                }
                for (final String idToCache : operationNamesToCache) {
                    final String queryID = br.getRegex("queryId\\s*:\\s*\"([^\"]+)\",\\s*operationName\\s*:\\s*\"" + idToCache + "\"").getMatch(0);
                    if (queryID != null) {
                        graphqlQueryids.put(idToCache, queryID);
                    } else {
                        logger.warning("Failed to find ID for operationName: " + operationName);
                    }
                }
            }
            final String ret = graphqlQueryids.get(operationName);
            if (ret == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return ret;
        }
    }

    /**
     * Crawls items of links like this: https://twitter.com/i/broadcasts/<broadcastID> </br>
     * 2023-09-01: Stopped working on this as the streams are DRM protected. Reference: https://board.jdownloader.org/showthread.php?t=94178
     *
     * @throws Exception
     */
    private ArrayList<DownloadLink> crawlBroadcast(final String broadcastID) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage("https://twitter.com/i/api/1.1/broadcasts/show.json?ids=" + broadcastID + "&include_events=true");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> bc = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "broadcasts/" + broadcastID);
        if (Boolean.FALSE.equals(bc.get("available_for_replay")) || !"ENDED".equalsIgnoreCase(bc.get("state").toString())) {
            throw new DecrypterRetryException(RetryReason.UNSUPPORTED_LIVESTREAM);
        }
        // final String created_at_ms = bc.get("created_at_ms").toString();
        final String broadcastTitle = bc.get("status").toString();
        final String tweet_id = bc.get("tweet_id").toString();
        final String thumbnailURL = bc.get("image_url").toString();
        final String media_key = bc.get("media_key").toString();
        getPage("https://twitter.com/i/api/1.1/live_video_stream/status/" + media_key + "?client=web&use_syndication_guest_id=false&cookie_set_host=twitter.com");
        final Map<String, Object> entries2 = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final Map<String, Object> source = (Map<String, Object>) entries2.get("source");
        final String streamType = source.get("streamType").toString();
        if (!streamType.equalsIgnoreCase("HLS")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String hlsMaster = source.get("noRedirectPlaybackUrl").toString();
        final DownloadLink link = this.createDownloadlink(hlsMaster);
        link.setProperty(GenericM3u8.PRESET_NAME_PROPERTY, broadcastTitle);
        ret.add(link);
        return ret;
    }

    @Deprecated
    private ArrayList<DownloadLink> crawlCard(final CryptedLink param, final Account account) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
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
            ret.add(this.createDownloadlink(externID));
            return ret;
        }
        if (ret.isEmpty()) {
            String dllink = br.getRegex("playlist\\&quot;:\\[\\{\\&quot;source\\&quot;:\\&quot;(https[^<>\"]*?\\.(?:webm|mp4))").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("\\", "");
            final String filename = tweetID + "_" + new Regex(dllink, "([^/]+\\.[a-z0-9]+)$").getMatch(0);
            final DownloadLink dl = this.createDownloadlink(dllink);
            dl.setProperty(PROPERTY_TWEET_ID, tweetID);
            dl.setName(filename);
            dl.setAvailable(true);
            ret.add(dl);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlSingleTweet(final CryptedLink param, final Account account) throws Exception {
        final Regex urlinfo = new Regex(param.getCryptedUrl(), TYPE_USER_POST);
        if (!urlinfo.patternFind()) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String username = urlinfo.getMatch(0);
        final String tweetID = urlinfo.getMatch(1);
        return crawlSingleTweet(account, username, tweetID);
    }

    private ArrayList<DownloadLink> crawlSingleTweet(final Account account, final String username, final String tweetID) throws Exception {
        if (StringUtils.isEmpty(tweetID)) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final SingleTweetCrawlerMode mode = cfg.getSingleTweetCrawlerCrawlMode();
        if (mode == SingleTweetCrawlerMode.NEW_API || mode == SingleTweetCrawlerMode.AUTO) {
            return crawlSingleTweetViaGraphqlAPI(account, username, tweetID);
        } else {
            /* Old API/Auto mode */
            return crawlSingleTweetViaOldAPI(account, tweetID);
        }
    }

    private ArrayList<DownloadLink> crawlSingleTweetViaGraphqlAPI(final Account account, final String username, final String tweetID) throws Exception {
        if (tweetID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean preferNewWay20230811 = true;
        if (preferNewWay20230811) {
            // final String queryID = this.getGraphqlQueryID("TweetResultByRestId");
            this.prepareAPI(br, account);
            final String queryID = "0hWvDhmW8YQ-S_ib3azIrw";
            getPage("https://twitter.com/i/api/graphql/" + queryID + "/TweetResultByRestId?variables=%7B%22tweetId%22%3A%22" + tweetID
                    + "%22%2C%22withCommunity%22%3Afalse%2C%22includePromotedContent%22%3Afalse%2C%22withVoice%22%3Afalse%7D&features=%7B%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Afalse%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22responsive_web_media_download_video_enabled%22%3Afalse%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D");
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> tweetResult = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/tweetResult");
            if (tweetResult.isEmpty()) {
                /* Tweet does not exist: {"data":{"tweetResult":{}}} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> tweetMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(tweetResult, "result/legacy");
            if (tweetMap == null) {
                /* 2023-09-21 */
                tweetMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(tweetResult, "result/tweet/legacy");
            }
            if (tweetMap == null) {
                /*
                 * Tweet does exist but is unavailable or does not exist anymore or is only accessible for logged in users (NSFW blocked).
                 */
                /* For example: {"data":{"tweetResult":{"result":{"__typename":"TweetUnavailable","reason":"Suspended"}}}} */
                /* Example 2: {"data":{"tweetResult":{"result":{"__typename":"TweetUnavailable","reason":"NsfwLoggedOut"}}}} */
                /* Example 3: */
                final Map<String, Object> tweetUnavailableMap = recursiveFindTweetUnavailableMap(entries);
                if (tweetUnavailableMap != null) {
                    final String tweetUnavailableReasonInternal = tweetUnavailableMap.get("reason").toString();
                    String tweetUnavailableReasonHumanReadableText = null;
                    try {
                        if (tweetUnavailableReasonInternal.equalsIgnoreCase("NsfwLoggedOut")) {
                            /* Account required or given account has no permission to view NSFW content */
                            if (account != null) {
                                tweetUnavailableReasonHumanReadableText = "Given account has no permission to view NSFW content.";
                            } else {
                                tweetUnavailableReasonHumanReadableText = "Account required to view NSFW content.";
                            }
                            throw new AccountRequiredException();
                        } else {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                    } finally {
                        final String bubbleNotificationTitle = "Tweet unavailable: " + tweetID;
                        if (tweetUnavailableReasonHumanReadableText != null) {
                            displayBubblenotifyMessage(bubbleNotificationTitle, "Tween unavailable because: " + tweetUnavailableReasonHumanReadableText);
                        } else {
                            displayBubblenotifyMessage(bubbleNotificationTitle, "Tween unavailable because: API error: " + tweetUnavailableReasonInternal);
                        }
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            return this.crawlTweetMap(username, tweetMap, null, null);
        } else {
            final String queryID = this.getGraphqlQueryID("TweetDetail");
            final Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("focalTweetId", tweetID);
            variables.put("with_rux_injections", false);
            variables.put("includePromotedContent", true);
            variables.put("withCommunity", true);
            variables.put("withQuickPromoteEligibilityTweetFields", true);
            variables.put("withBirdwatchNotes", true);
            variables.put("withVoice", true);
            variables.put("withV2Timeline", true);
            final UrlQuery query = new UrlQuery();
            query.add("variables", Encoding.urlEncode(JSonStorage.serializeToJson(variables)));
            // query.add("features", Encoding.urlEncode(
            // "{\"blue_business_profile_image_shape_enabled\":true,\"responsive_web_graphql_exclude_directive_enabled\":true,\"verified_phone_label_enabled\":false,\"responsive_web_graphql_timeline_navigation_enabled\":true,\"responsive_web_graphql_skip_user_profile_image_extensions_enabled\":false,\"tweetypie_unmention_optimization_enabled\":true,\"vibe_api_enabled\":true,\"responsive_web_edit_tweet_api_enabled\":true,\"graphql_is_translatable_rweb_tweet_is_translatable_enabled\":true,\"view_counts_everywhere_api_enabled\":true,\"longform_notetweets_consumption_enabled\":true,\"tweet_awards_web_tipping_enabled\":false,\"freedom_of_speech_not_reach_fetch_enabled\":true,\"standardized_nudges_misinfo\":true,\"tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled\":false,\"interactive_text_enabled\":true,\"responsive_web_text_conversations_enabled\":false,\"longform_notetweets_rich_text_read_enabled\":true,\"responsive_web_enhance_cards_enabled\":false}"));
            //
            query.add("features",
                    "%7B%22rweb_lists_timeline_redesign_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Afalse%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22responsive_web_media_download_video_enabled%22%3Afalse%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D");
            this.prepareAPI(br, account);
            br.getHeaders().put("Content-Type", "application/json");
            // getPage(API_BASE_GRAPHQL + "/" + queryID + "/TweetDetail?" + query.toString());
            /** Developer: Important! If the following request returns http responsecode 400, most likely the queryID is wrong! */
            getPage(API_BASE_GRAPHQL + "/" + queryID + "/TweetDetail?variables=%7B%22focalTweetId%22%3A%22" + tweetID
                    + "%22%2C%22with_rux_injections%22%3Afalse%2C%22includePromotedContent%22%3Atrue%2C%22withCommunity%22%3Atrue%2C%22withQuickPromoteEligibilityTweetFields%22%3Atrue%2C%22withBirdwatchNotes%22%3Atrue%2C%22withVoice%22%3Atrue%2C%22withV2Timeline%22%3Atrue%7D&features=%7B%22rweb_lists_timeline_redesign_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Afalse%2C%22interactive_text_enabled%22%3Atrue%2C%22responsive_web_text_conversations_enabled%22%3Afalse%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Afalse%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D");
            final Map<String, Object> entries = this.handleErrorsAPI(br);
            final List<Map<String, Object>> timelineInstructions = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "data/threaded_conversation_with_injections_v2/instructions");
            final ArrayList<DownloadLink> ret = this.crawlUserProfileGraphqlTimelineInstructions(timelineInstructions, null, tweetID, null);
            return ret;
        }
    }

    /**
     * Recursive function to find first map which contains information about unavailable Tweet. </br>
     * Important: If the return value of this != null this doesn't mean that a Tweet is unavailable - only use the result of this if upper
     * code was unable to find any online Tweet!
     */
    private Map<String, Object> recursiveFindTweetUnavailableMap(final Object o) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                // final String key = entry.getKey();
                final Object value = entry.getValue();
                final String __typename = (String) entrymap.get("__typename");
                if (StringUtils.equalsIgnoreCase(__typename, "TweetUnavailable")) {
                    return entrymap;
                } else if (value instanceof List || value instanceof Map) {
                    final Map<String, Object> ret = recursiveFindTweetUnavailableMap(value);
                    if (ret != null) {
                        return ret;
                    }
                }
            }
            return null;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Map<String, Object> res = recursiveFindTweetUnavailableMap(arrayo);
                    if (res != null) {
                        return res;
                    }
                }
            }
            return null;
        } else {
            return null;
        }
    }

    @Deprecated
    /**
     * 2023-07-13: Looks like twitter has disabled this API(?) </br>
     * It e.g. returns: {"errors":[{"code":179,"message":"Sorry, you are not authorized to see this status."}]} ...even though user has the
     * rights to view that tweet.
     */
    private ArrayList<DownloadLink> crawlSingleTweetViaOldAPI(final Account account, final String tweetID) throws Exception {
        logger.info("Crawling Tweet via old API");
        prepareAPI(this.br, account);
        final boolean tryNewMethod = true; /* 2021-06-15 */
        boolean looksLikeOfflineError34 = false;
        if (tryNewMethod) {
            br.getPage("https://api.twitter.com/1.1/statuses/show/" + tweetID + ".json?cards_platform=Web-12&include_reply_count=1&include_cards=1&include_user_entities=0&tweet_mode=extended");
            try {
                handleErrorsAPI(this.br);
                final Map<String, Object> tweet = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                return crawlTweetMap(tweet);
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND && br.containsHTML("\"code\"\\s*:\\s*34")) {
                    logger.log(e);
                    /* Double-check down below. */
                    logger.info("Tweet looks to be offline");
                    looksLikeOfflineError34 = true;
                } else {
                    throw e;
                }
            }
        }
        br.getPage(API_BASE_v2 + "/timeline/conversation/" + tweetID + ".json?include_profile_interstitial_type=1&include_blocking=1&include_blocked_by=1&include_followed_by=1&include_want_retweets=1&include_mute_edge=1&include_can_dm=1&include_can_media_tag=1&skip_status=1&cards_platform=Web-12&include_cards=1&include_composer_source=true&include_ext_alt_text=true&include_reply_count=1&tweet_mode=extended&include_entities=true&include_user_entities=true&include_ext_media_color=true&include_ext_media_availability=true&send_error_codes=true&simple_quoted_tweets=true&count=20&ext=mediaStats%2CcameraMoment");
        handleErrorsAPI(this.br);
        final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        final Map<String, Object> tweet = (Map<String, Object>) JavaScriptEngineFactory.walkJson(root, "globalObjects/tweets/" + tweetID);
        if (tweet == null) {
            if (looksLikeOfflineError34) {
                /**
                 * We're missing the permissions to view this content. </br>
                 * Most likely it is age restricted content and (age verified) account is required.
                 */
                if (account == null) {
                    logger.info("Looks like an account is required to crawl this thread");
                } else {
                    logger.info("Looks like given account is lacking permissions to view this tweet");
                }
                throw new AccountRequiredException();
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } else {
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

    /** Sets headers required to use [GraphQL] API. */
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
                    plugin.getLogger().warning("Found new guest_token: " + guest_token);
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

    /**
     * Wrapper
     *
     * @throws PluginException
     */
    private ArrayList<DownloadLink> crawlTweetMap(final Map<String, Object> tweet) throws MalformedURLException, PluginException {
        return crawlTweetMap(null, tweet, null, null);
    }

    /**
     * Crawls single media objects obtained via API.
     *
     * @throws MalformedURLException
     * @throws PluginException
     */
    private ArrayList<DownloadLink> crawlTweetMap(String username, Map<String, Object> tweet, Map<String, Object> user, FilePackage fp) throws MalformedURLException, PluginException {
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final Map<String, Object> retweeted_status = (Map<String, Object>) tweet.get("retweeted_status");
        boolean isRetweet = false;
        if (retweeted_status != null && !retweeted_status.isEmpty()) {
            /*
             * Content of tweet is in this if whole tweet is a retweet. Also fields of "root map" of tweet can be truncated then e.g. text
             * of tweet in "full_text" is not the full text then.
             */
            tweet = retweeted_status;
            isRetweet = true;
        }
        final String tweetID = tweet.get("id_str").toString();
        final Object userInContextOfTweet = tweet.get("user");
        if (userInContextOfTweet != null) {
            /**
             * Prefer this as our user object. </br>
             * It's only included when adding single tweets.
             */
            user = (Map<String, Object>) userInContextOfTweet;
        }
        if (user != null) {
            username = (String) user.get("screen_name");
        }
        final String created_at = tweet.get("created_at").toString();
        final long timestamp = getTimestampTwitterDate(created_at);
        final String formattedDate = formatTwitterDateFromTimestamp(timestamp);
        final String tweetText = (String) tweet.get("full_text");
        final boolean isReplyToOtherTweet = tweet.get("in_reply_to_status_id_str") != null;
        String replyTextForFilename = "";
        if (isReplyToOtherTweet) {
            /* Mark filenames of tweet-replies if wished by user. */
            if (cfg.isMarkTweetRepliesViaFilename()) {
                replyTextForFilename += "_reply";
            }
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
        final ArrayList<DownloadLink> retMedia = new ArrayList<DownloadLink>();
        final String urlToTweet = createTwitterPostURL(username, tweetID);
        fp.setAllowInheritance(true);
        fp.setAllowMerge(true);
        /*
         * mediasExtended can contasin image items + video items. medias can contain additional image items that are not inside
         * mediasExtended.
         */
        final List<Map<String, Object>> mediasExtended = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(tweet, "extended_entities/media");
        final List<Map<String, Object>> medias = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(tweet, "entities/media");
        final String vmapURL = (String) JavaScriptEngineFactory.walkJson(tweet, "card/binding_values/amplify_url_vmap/string_value");
        final List<List<Map<String, Object>>> mediaLists = new ArrayList<List<Map<String, Object>>>();
        if (mediasExtended != null && mediasExtended.size() > 0) {
            mediaLists.add(mediasExtended);
        }
        if (medias != null && medias.size() > 0) {
            mediaLists.add(medias);
        }
        int mediaIndex = 0;
        int videoIndex = 0;
        if (mediaLists.size() > 0) {
            final List<String> mediaTypesVideo = Arrays.asList(new String[] { "animated_gif", "video" });
            final String mediaTypePhoto = "photo";
            final Map<String, DownloadLink> mediaResultMap = new LinkedHashMap<String, DownloadLink>();
            final Set<String> videoIDs = new HashSet<String>();
            for (final List<Map<String, Object>> mediaList : mediaLists) {
                for (final Map<String, Object> media : mediaList) {
                    final String mediaType = media.get("type").toString();
                    final String mediaIDStr = media.get("id_str").toString();
                    final String keyForMap = mediaType + "_" + mediaIDStr;
                    if (mediaResultMap.containsKey(keyForMap)) {
                        continue;
                    }
                    final DownloadLink dl;
                    if (mediaTypesVideo.contains(mediaType)) {
                        videoIDs.add(mediaIDStr);
                        /* Animated_gif will usually only have one .mp4 version available with bitrate "0". */
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
                        dl = this.createDownloadlink(createVideourlSpecific(username, tweetID, (videoIndex + 1)));
                        dl.setProperty(PROPERTY_TYPE, TYPE_VIDEO);
                        dl.setProperty(PROPERTY_BITRATE, highestBitrate);
                        dl.setProperty(TwitterCom.PROPERTY_DIRECTURL, streamURL);
                        if (!StringUtils.isEmpty(hlsMaster)) {
                            dl.setProperty(TwitterCom.PROPERTY_DIRECTURL_hls_master, hlsMaster);
                        }
                        dl.setProperty(PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY, true);
                        videoIndex++;
                    } else if (mediaType.equals(mediaTypePhoto)) {
                        // if (!cfg.isCrawlVideoThumbnail() && foundVideos.contains(tweetID)) {
                        // // do not grab video thumbnail
                        // continue;
                        // }
                        String photoURL = (String) media.get("media_url"); /* Also available as "media_url_https" */
                        if (StringUtils.isEmpty(photoURL)) {
                            photoURL = (String) media.get("media_url_https");
                        }
                        if (StringUtils.isEmpty(photoURL)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        dl = this.createDownloadlink(photoURL);
                        dl.setProperty(PROPERTY_TYPE, TYPE_PHOTO);
                        dl.setProperty(TwitterCom.PROPERTY_DIRECTURL, photoURL);
                    } else {
                        /* Unknown type -> This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown media type:" + mediaType);
                    }
                    dl.setAvailable(true);
                    dl.setProperty(PROPERTY_MEDIA_INDEX, mediaIndex);
                    dl.setProperty(PROPERTY_MEDIA_COUNT, mediasExtended.size());
                    dl.setProperty(PROPERTY_MEDIA_ID, media.get("id_str"));
                    mediaResultMap.put(keyForMap, dl);
                    mediaIndex += 1;
                }
            }
            if (!cfg.isCrawlVideoThumbnail()) {
                /* Remove video thumbnails from results as user doesn't want video thumbnails. */
                int numberofSkippedVideoThumbnails = 0;
                for (final String videoMediaIDStr : videoIDs) {
                    final String keyForMap = mediaTypePhoto + "_" + videoMediaIDStr;
                    if (mediaResultMap.remove(keyForMap) != null) {
                        numberofSkippedVideoThumbnails++;
                    }
                }
                if (numberofSkippedVideoThumbnails > 0) {
                    logger.info("Skipped thumbnails: " + numberofSkippedVideoThumbnails);
                }
            }
            /* Add results to list to be returned later. */
            retMedia.addAll(mediaResultMap.values());
        }
        /* Check for fallback video source if no video item has been found until now. */
        if (videoIndex == 0 && !StringUtils.isEmpty(vmapURL)) {
            /* Fallback handling for very old (???) content */
            /* Expect such URLs which our host plugin can handle: https://video.twimg.com/amplify_video/vmap/<numbers>.vmap */
            final DownloadLink singleVideo = this.createDownloadlink(vmapURL);
            final String finalFilename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename + ".mp4";
            singleVideo.setFinalFileName(finalFilename);
            singleVideo.setProperty(PROPERTY_VIDEO_DIRECT_URLS_ARE_AVAILABLE_VIA_API_EXTENDED_ENTITY, false);
            singleVideo.setProperty(PROPERTY_MEDIA_INDEX, 0);
            singleVideo.setProperty(PROPERTY_TYPE, TYPE_VIDEO);
            singleVideo.setAvailable(true);
            retMedia.add(singleVideo);
        }
        final ArrayList<DownloadLink> retInternal = new ArrayList<DownloadLink>();
        int itemsSkippedDueToPluginSettings = 0;
        DownloadLink text = null;
        final ArrayList<DownloadLink> retExternal = new ArrayList<DownloadLink>();
        if (!StringUtils.isEmpty(tweetText)) {
            final String[] urlsInPostText = HTMLParser.getHttpLinks(tweetText, br.getURL());
            if (cfg.isCrawlURLsInsideTweetText() && urlsInPostText.length > 0) {
                final ArrayList<String> skippedUrls = new ArrayList<String>();
                final String whitelistRegexStr = cfg.getRegexWhitelistForCrawledUrlsInTweetText();
                Pattern whitelistPattern = null;
                if (!StringUtils.isEmpty(whitelistRegexStr)) {
                    try {
                        whitelistPattern = Pattern.compile(whitelistRegexStr.trim(), Pattern.CASE_INSENSITIVE);
                    } catch (final PatternSyntaxException pse) {
                        logger.info("User entered invalid whitelist regex, ignoring it. Regex: " + whitelistRegexStr);
                    }
                }
                for (final String url : urlsInPostText) {
                    if (whitelistPattern == null || new Regex(url, whitelistPattern).matches()) {
                        retExternal.add(this.createDownloadlink(url));
                    } else {
                        skippedUrls.add(url);
                    }
                }
                if (skippedUrls.size() > 0) {
                    String logtext = "Skipped URLs due du users' whitelist pattern: ";
                    if (skippedUrls.size() == urlsInPostText.length) {
                        logtext += "ALL";
                    } else {
                        logtext += skippedUrls;
                    }
                    logger.info(logtext);
                }
            } else if (urlsInPostText != null) {
                /* All URLs were skipped. */
                itemsSkippedDueToPluginSettings += urlsInPostText.length;
            }
            /* Crawl Tweet as text if wanted by user or if tweet contains only text. */
            final SingleTweetCrawlerTextCrawlMode mode = cfg.getSingleTweetCrawlerTextCrawlMode();
            if (mode == SingleTweetCrawlerTextCrawlMode.AUTO || mode == SingleTweetCrawlerTextCrawlMode.ALWAYS || (mode == SingleTweetCrawlerTextCrawlMode.ONLY_IF_NO_MEDIA_IS_AVAILABLE && retMedia.isEmpty())) {
                /*
                 * Determine last found original filename now/here because after collecting those items we're removing non-thumbnails if not
                 * wanted by user so this is the only place to determine the last used original filename.
                 */
                String lastFoundOriginalFilename = null;
                for (final DownloadLink result : retMedia) {
                    final String directurl = result.getStringProperty(TwitterCom.PROPERTY_DIRECTURL);
                    if (directurl != null) {
                        final String originalFilename = getFilenameFromURL(directurl);
                        if (originalFilename != null) {
                            lastFoundOriginalFilename = originalFilename;
                        }
                    }
                }
                text = this.createDownloadlink(urlToTweet);
                text.setProperty(PROPERTY_RELATED_ORIGINAL_FILENAME, lastFoundOriginalFilename);
                try {
                    text.setDownloadSize(tweetText.getBytes("UTF-8").length);
                } catch (final UnsupportedEncodingException ignore) {
                    ignore.printStackTrace();
                }
                text.setProperty(PROPERTY_MEDIA_INDEX, 0);
                text.setProperty(PROPERTY_TYPE, TYPE_TEXT);
                text.setAvailable(true);
                // text.setEnabled(false);
                retInternal.add(text);
            } else {
                itemsSkippedDueToPluginSettings++;
            }
        }
        retInternal.addAll(retMedia);
        /* Add remaining plugin properties */
        for (final DownloadLink dl : retInternal) {
            /* Add additional properties */
            dl.setContentUrl(urlToTweet);
            dl.setProperty(PROPERTY_USERNAME, username);
            dl.setProperty(PROPERTY_TWEET_ID, tweetID);
            dl.setProperty(PROPERTY_DATE, formattedDate);
            dl.setProperty(PROPERTY_DATE_TIMESTAMP, timestamp);
            if (!StringUtils.isEmpty(tweetText)) {
                dl.setProperty(PROPERTY_TWEET_TEXT, tweetText);
            }
            if (isReplyToOtherTweet) {
                dl.setProperty(PROPERTY_REPLY, true);
            }
            if (isRetweet) {
                dl.setProperty(PROPERTY_RETWEET, true);
            }
            /* Set filename which gets created based on user settings and previously set properties. */
            setFormattedFilename(dl);
        }
        final ArrayList<DownloadLink> retAll = new ArrayList<DownloadLink>();
        retAll.addAll(retInternal);
        retAll.addAll(retExternal);
        fp.addLinks(retAll);
        /* Logger just in case nothing was added. */
        if (retMedia.isEmpty()) {
            if (itemsSkippedDueToPluginSettings == 0) {
                logger.info("Failed to find any crawlable content in tweet: " + tweetID);
            } else {
                logger.info("Failed to find any crawlable content because of user settings. Crawlable but skipped " + itemsSkippedDueToPluginSettings + " item(s) due to users' plugin settings.");
            }
        }
        return retAll;
    }

    private static String getFilenameFromURL(final String url) {
        try {
            return Plugin.getFileNameFromURL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setFormattedFilename(final DownloadLink link) {
        final String legacyCrawlerFilename = link.getStringProperty("crawlerfilename");
        if (legacyCrawlerFilename != null) {
            /* Hardcoded filename has been set in crawler revision 47957 or before -> Keep that one. */
            link.setFinalFileName(legacyCrawlerFilename);
            return;
        }
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final FilenameScheme scheme = cfg.getFilenameScheme();
        final String tweetID = link.getStringProperty(PROPERTY_TWEET_ID);
        // final String type = link.getStringProperty(PROPERTY_TYPE);
        final String formattedDate = link.getStringProperty(PROPERTY_DATE);
        final int mediaIndex = link.getIntegerProperty(PROPERTY_MEDIA_INDEX, -1);
        final int mediaCount = link.getIntegerProperty(PROPERTY_MEDIA_COUNT, -1);
        String replyTextForFilename = "";
        if (link.getBooleanProperty(PROPERTY_REPLY, false) && cfg.isMarkTweetRepliesViaFilename()) {
            /* Mark filenames of tweet-replies if wished by user. */
            replyTextForFilename += "_reply";
        }
        final String username = link.getStringProperty(PROPERTY_USERNAME);
        final String directurl = link.getStringProperty(TwitterCom.PROPERTY_DIRECTURL);
        final String relatedOriginalFilename = link.getStringProperty(PROPERTY_RELATED_ORIGINAL_FILENAME);
        String originalFilename = null;
        if (directurl != null) {
            originalFilename = getFilenameFromURL(directurl);
        }
        String originalFilenameWithoutExt = null;
        if (originalFilename != null || relatedOriginalFilename != null) {
            final String filenameToUse;
            if (relatedOriginalFilename != null) {
                filenameToUse = relatedOriginalFilename;
            } else {
                filenameToUse = originalFilename;
            }
            if (filenameToUse.contains(".")) {
                originalFilenameWithoutExt = filenameToUse.substring(0, filenameToUse.lastIndexOf("."));
            } else {
                originalFilenameWithoutExt = filenameToUse;
            }
        }
        String ext = null;
        if (TwitterCom.isText(link)) {
            ext = ".txt";
        } else if (directurl == null && TwitterCom.isVideo(link)) {
            ext = ".mp4";
        } else if (originalFilename != null) {
            ext = Plugin.getFileNameExtensionFromString(originalFilename);
        }
        String filename;
        if (scheme == FilenameScheme.ORIGINAL && (originalFilename != null || relatedOriginalFilename != null)) {
            if (originalFilename != null) {
                filename = originalFilename;
            } else {
                /* E.g. .txt file filename which is supposed to look like filename of related media file(s). */
                filename = Plugin.getCorrectOrApplyFileNameExtension(relatedOriginalFilename, ext);
            }
        } else if (scheme == FilenameScheme.ORIGINAL_WITH_TWEET_ID) {
            filename = tweetID;
            if (originalFilenameWithoutExt != null) {
                filename += "_" + originalFilenameWithoutExt;
            }
            filename += ext;
        } else if (scheme == FilenameScheme.ORIGINAL_PLUS) {
            filename = formattedDate + "_" + tweetID;
            if (originalFilenameWithoutExt != null) {
                filename += "_" + originalFilenameWithoutExt;
            }
            filename += ext;
        } else if ((scheme == FilenameScheme.ORIGINAL_PLUS_2 || scheme == FilenameScheme.AUTO) && username != null) {
            filename = formattedDate + "_" + username + "_" + tweetID;
            if (originalFilenameWithoutExt != null) {
                filename += "_" + originalFilenameWithoutExt;
            }
            filename += ext;
        } else if ((scheme == FilenameScheme.PLUGIN || scheme == FilenameScheme.AUTO) && username != null && formattedDate != null) {
            filename = formattedDate + "_" + username + "_" + tweetID + replyTextForFilename;
            if (mediaCount > 1) {
                filename += "_" + mediaIndex;
            }
            filename += ext;
        } else {
            /* Fallback */
            filename = tweetID + ext;
        }
        link.setFinalFileName(filename);
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

    /** Returns true if an account is required to process the given URL. */
    private boolean requiresAccount(final String url) {
        if (url.matches(TYPE_USER_LIKES)) {
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param, final Account account) throws Exception {
        if (PluginJsonConfig.get(TwitterConfigInterface.class).isCrawlRetweetsV2()) {
            return this.crawlUserViaGraphqlAPI(param, account);
        } else {
            return crawlUserViaAPI(param, account);
        }
    }

    private final String TWITTER_PROFILE_PACKAGE_KEY_PREFIX       = "twitterprofile://";
    private final String TWITTER_PROFILE_LIKES_PACKAGE_KEY_PREFIX = "twitterprofile_likes://";

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
        final List<String> pinned_tweet_ids_str = (List<String>) user.get("pinned_tweet_ids_str");
        final String userID = user.get("id_str").toString();
        /* = number of tweets */
        final int tweet_count = ((Number) user.get("statuses_count")).intValue();
        /* = number of tweets containing media (can be lower [ar also higher?] than "statuses_count") */
        final int media_count = ((Number) user.get("media_count")).intValue();
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
            fp.setPackageKey(TWITTER_PROFILE_LIKES_PACKAGE_KEY_PREFIX + userID);
        } else if (param.getCryptedUrl().matches(TYPE_USER_MEDIA) || force_grab_media) {
            logger.info("Crawling self posted media only from user: " + username);
            if (media_count == 0) {
                ret.add(getDummyErrorProfileContainsNoMediaItems(username));
                return ret;
            }
            content_type = "media";
            maxCount = media_count;
            fp.setName(username);
            fp.setPackageKey(TWITTER_PROFILE_PACKAGE_KEY_PREFIX + userID);
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
        final String addedURLWithoutParams = URLHelper.getUrlWithoutParams(param.getCryptedUrl());
        final UrlQuery addedURLQuery = UrlQuery.parse(resumeURL);
        int totalCrawledTweetsCount = 0;
        int page = 1;
        int numberofPagesInARowWithoutResult = 0;
        final int maxNumberofPagesInARowWithoutResult = 5;
        String nextCursor = null;
        if (this.preGivenPageNumber != null && this.preGivenNumberOfTotalWalkedThroughTweetsCount != null && this.preGivenNextCursor != null) {
            /* Resume from last state */
            page = this.preGivenPageNumber.intValue();
            totalCrawledTweetsCount = this.preGivenNumberOfTotalWalkedThroughTweetsCount.intValue();
            nextCursor = this.preGivenNextCursor;
        }
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final HashSet<String> cursorDupes = new HashSet<String>();
        final String apiURL = API_BASE_v2 + "/timeline/" + content_type + "/" + userID + ".json";
        tweetTimeline: do {
            final UrlQuery thisquery = query;
            if (!StringUtils.isEmpty(nextCursor)) {
                thisquery.append("cursor", nextCursor, true);
            }
            getPage(apiURL + "?" + thisquery.toString());
            handleErrorsAPI(this.br);
            final Map<String, Object> root = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
            final Map<String, Object> globalObjects = (Map<String, Object>) root.get("globalObjects");
            final Map<String, Object> users = (Map<String, Object>) globalObjects.get("users");
            final List<Object> pagination_info = (List<Object>) JavaScriptEngineFactory.walkJson(root, "timeline/instructions/{0}/addEntries/entries");
            final Map<String, Object> tweetMap = (Map<String, Object>) globalObjects.get("tweets");
            final Iterator<Entry<String, Object>> iterator = tweetMap.entrySet().iterator();
            Long lastCrawledTweetTimestamp = null;
            final List<DownloadLink> allowedResults = new ArrayList<DownloadLink>();
            boolean stopBecauseReachedUserDefinedMaxItemsLimit = false;
            final HashSet<DownloadLink> skippedResultsByMaxDate = new HashSet<DownloadLink>();
            while (iterator.hasNext()) {
                final Map<String, Object> tweet = (Map<String, Object>) iterator.next().getValue();
                final Map<String, Object> userWhoPostedThisTweet = (Map<String, Object>) users.get(tweet.get("user_id_str").toString());
                final List<DownloadLink> thisResults = crawlTweetMap(username, tweet, userWhoPostedThisTweet, fp);
                if (thisResults.isEmpty()) {
                    logger.info("Found nothing for tweet: " + tweet);
                    continue;
                }
                for (final DownloadLink thisTweetResult : thisResults) {
                    /* Find timestamp of last added result. Ignore pinned tweets. */
                    final String tweetID = thisTweetResult.getStringProperty(PROPERTY_TWEET_ID);
                    if (pinned_tweet_ids_str == null || (tweetID != null && !pinned_tweet_ids_str.contains(tweetID))) {
                        lastCrawledTweetTimestamp = thisTweetResult.getLongProperty(PROPERTY_DATE_TIMESTAMP, -1);
                    }
                }
                if (this.maxTweetsToCrawl != null && totalCrawledTweetsCount == this.maxTweetsToCrawl.intValue()) {
                    stopBecauseReachedUserDefinedMaxItemsLimit = true;
                    break;
                } else if (this.crawlUntilTimestamp != null && lastCrawledTweetTimestamp != null && lastCrawledTweetTimestamp < crawlUntilTimestamp) {
                    skippedResultsByMaxDate.addAll(thisResults);
                } else {
                    totalCrawledTweetsCount++;
                    allowedResults.addAll(thisResults);
                }
            }
            logger.info("Crawled page " + page + " | Tweets crawled so far: " + totalCrawledTweetsCount + "/" + maxCount.intValue() + " | last nextCursor = " + nextCursor);
            distribute(allowedResults);
            ret.addAll(allowedResults);
            /* Check abort conditions */
            if (stopBecauseReachedUserDefinedMaxItemsLimit) {
                logger.info("Stopping because: Reached user defined max items count: " + maxTweetsToCrawl);
                break tweetTimeline;
            } else if (skippedResultsByMaxDate.size() > 0) {
                logger.info("Stopping because: Last item age is older than user defined max age " + this.maxTweetDateStr);
                break tweetTimeline;
            } else if (tweetMap.isEmpty()) {
                logger.info("Current page (" + page + ") didn't contain any results --> Probably it contained only explicit content and we're lacking permissions to view that!");
                numberofPagesInARowWithoutResult++;
            } else {
                numberofPagesInARowWithoutResult = 0;
            }
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
                    break tweetTimeline;
                }
                nextCursor = (String) JavaScriptEngineFactory.walkJson(pagination_info_entries, "content/operation/cursor/value");
                if (StringUtils.isEmpty(nextCursor)) {
                    logger.info("Stopping because: Failed to find nextCursor");
                    break tweetTimeline;
                } else if (!cursorDupes.add(nextCursor)) {
                    logger.info("Stopping because: We've already crawled current cursor: " + nextCursor);
                    break tweetTimeline;
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("Stopping because: Failed to get nextCursor (Exception occured)");
                break tweetTimeline;
            }
            if (numberofPagesInARowWithoutResult >= maxNumberofPagesInARowWithoutResult) {
                logger.info("Stopping because: Reached max number of pages without result in a row [probably explicit content]: " + maxNumberofPagesInARowWithoutResult);
                break tweetTimeline;
            }
            /** Store this information in URL so in case crawler fails, it will resume from previous position if user adds that URL. */
            addedURLQuery.addAndReplace("page", Integer.toString(page));
            addedURLQuery.addAndReplace("totalCrawledTweetsCount", Integer.toString(totalCrawledTweetsCount));
            addedURLQuery.addAndReplace("nextCursor", Encoding.urlEncode(nextCursor));
            this.resumeURL = addedURLWithoutParams + "?" + addedURLQuery.toString();
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

    /** Crawls tweets AND re-tweets of profile in given URL. */
    private ArrayList<DownloadLink> crawlUserViaGraphqlAPI(final CryptedLink param, final Account account) throws Exception {
        final String username = new Regex(param.getCryptedUrl(), TYPE_USER_ALL).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String queryID = this.getGraphqlQueryID("UserTweets");
        final Map<String, Object> user = getUserInfo(br, account, username);
        final Number statuses_count = (Number) user.get("statuses_count");
        final String userID = user.get("id_str").toString();
        final FilePackage fp = FilePackage.getInstance();
        fp.setPackageKey(TWITTER_PROFILE_PACKAGE_KEY_PREFIX + userID);
        fp.setName(username);
        final HashSet<String> cursorDupes = new HashSet<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        profileCrawlerTotalCrawledTweetsCount = 0;
        if (this.preGivenPageNumber != null && this.preGivenNumberOfTotalWalkedThroughTweetsCount != null && this.preGivenNextCursor != null) {
            /* Resume from last state */
            page = this.preGivenPageNumber.intValue();
            profileCrawlerTotalCrawledTweetsCount = this.preGivenNumberOfTotalWalkedThroughTweetsCount.intValue();
            profileCrawlerNextCursor = this.preGivenNextCursor;
        }
        int totalFoundTweets = 0;
        do {
            final Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("userId", userID);
            variables.put("count", 20);
            if (profileCrawlerNextCursor != null) {
                variables.put("cursor", profileCrawlerNextCursor);
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
            query.add("features",
                    "%7B%22rweb_lists_timeline_redesign_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Afalse%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22responsive_web_media_download_video_enabled%22%3Afalse%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D");
            getPage(API_BASE_GRAPHQL + "/" + queryID + "/UserTweets?" + query.toString());
            final Map<String, Object> entries = this.handleErrorsAPI(br);
            final List<Map<String, Object>> timelineInstructions = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "data/user/result/timeline_v2/timeline/instructions");
            final List<DownloadLink> allowedResults = this.crawlUserProfileGraphqlTimelineInstructions(timelineInstructions, user, null, fp);
            /* Yees global variables are dangerous but please don't touch this! */
            totalFoundTweets += profileCrawlerFoundTweetsOnCurrentPage;
            ret.addAll(allowedResults);
            distribute(allowedResults);
            logger.info("Crawled page " + page + " | Found Tweets on this page: " + profileCrawlerFoundTweetsOnCurrentPage + " | Tweets crawled so far: " + profileCrawlerTotalCrawledTweetsCount + " | nextCursor = " + profileCrawlerNextCursor);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (StringUtils.isEmpty(profileCrawlerNextCursor)) {
                logger.info("Stopping because: Failed to find nextCursor");
                break;
            } else if (profileCrawlerFoundTweetsOnCurrentPage == 0) {
                logger.info("Stopping because: Failed to find any items on current page " + page);
                break;
            } else if (!cursorDupes.add(profileCrawlerNextCursor)) {
                logger.info("Stopping because: nextCursor value for  next page has already been crawled -> Reached end?");
                break;
            } else if (profileCrawlerStopBecauseReachedUserDefinedMaxItemsLimit) {
                logger.info("Stopping because: Reached user defined max items count: " + maxTweetsToCrawl + " | Actually crawled: " + profileCrawlerTotalCrawledTweetsCount);
                break;
            } else if (profileCrawlerSkippedResultsByMaxDate.size() > 0) {
                logger.info("Stopping because: Last item age is older than user defined max age " + this.maxTweetDateStr);
                break;
            } else {
                page++;
                continue;
            }
        } while (true);
        logger.info("Last nextCursor: " + profileCrawlerNextCursor);
        if (ret.isEmpty()) {
            /* We found nothing -> Check why */
            final String bubbleNotifyTitle = "Twitter profile " + username + " | ID: " + userID;
            final String bubbleNotifyTextEnding = "\r\nTotal number of Tweets in this profile: " + statuses_count;
            if (totalFoundTweets == 0) {
                if (statuses_count != null && statuses_count.intValue() > 0 && account == null) {
                    displayBubblenotifyMessage(bubbleNotifyTitle, "Returning no results because:\r\nAccount required to view Tweets of this profile." + bubbleNotifyTextEnding);
                    throw new AccountRequiredException();
                } else {
                    /* No results and we don't know why. */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                /* No results because of users' settings. */
                if (profileCrawlerSkippedResultsByMaxDate.size() > 0) {
                    displayBubblenotifyMessage(bubbleNotifyTitle, "Returning no results because:\r\nAll existing elements have earlier timestamps than user defined max_date parameter " + maxTweetDateStr + ".\r\nMinimum number of skipped Tweets: " + profileCrawlerSkippedResultsByMaxDate.size() + bubbleNotifyTextEnding);
                } else {
                    displayBubblenotifyMessage(bubbleNotifyTitle, "Returning no results because:\r\nMost likely user has disabled tweet text crawler but this profile only contained text tweets.\r\nMinimum number of skipped possible Tweets: " + totalFoundTweets + bubbleNotifyTextEnding);
                }
            }
        }
        return ret;
    }

    private void displayBubblenotifyMessage(final String title, final String msg) {
        BubbleNotify.getInstance().show(new AbstractNotifyWindowFactory() {
            @Override
            public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                return new BasicNotify("Twitter: " + title, msg, new AbstractIcon(IconKey.ICON_INFO, 32));
            }
        });
    }

    /**
     * 2023-07-21: Time for some ugly codes: Public variables!
     */
    private String                      profileCrawlerNextCursor                                 = null;
    private int                         profileCrawlerTotalCrawledTweetsCount                    = 0;
    private int                         profileCrawlerFoundTweetsOnCurrentPage                   = 0;
    private boolean                     profileCrawlerStopBecauseReachedUserDefinedMaxItemsLimit = false;
    private final HashSet<DownloadLink> profileCrawlerSkippedResultsByMaxDate                    = new HashSet<DownloadLink>();

    private ArrayList<DownloadLink> crawlUserProfileGraphqlTimelineInstructions(final List<Map<String, Object>> timelineInstructions, final Map<String, Object> user, final String singleTweetID, final FilePackage fp) throws Exception {
        /* Nullification (yes public variables are evil!) */
        profileCrawlerNextCursor = null;
        profileCrawlerFoundTweetsOnCurrentPage = 0;
        profileCrawlerStopBecauseReachedUserDefinedMaxItemsLimit = false;
        profileCrawlerSkippedResultsByMaxDate.clear();
        final List<String> pinned_tweet_ids_str = user != null ? (List<String>) user.get("pinned_tweet_ids_str") : null;
        final ArrayList<DownloadLink> allowedResults = new ArrayList<DownloadLink>();
        timelineInstructionsLoop: for (final Map<String, Object> timelineInstruction : timelineInstructions) {
            if (!timelineInstruction.get("type").toString().equalsIgnoreCase("TimelineAddEntries")) {
                continue;
            }
            final List<Map<String, Object>> timelineEntries = (List<Map<String, Object>>) timelineInstruction.get("entries");
            for (final Map<String, Object> timelineEntry : timelineEntries) {
                final Map<String, Object> content = (Map<String, Object>) timelineEntry.get("content");
                final String contentType = (String) content.get("entryType");
                if (contentType.equalsIgnoreCase("TimelineTimelineCursor")) {
                    if (content.get("cursorType").toString().equalsIgnoreCase("Bottom")) {
                        profileCrawlerNextCursor = content.get("value").toString();
                        /* We've reached the end of current page */
                        break;
                    }
                } else {
                    final Map<String, Object> result = (Map<String, Object>) JavaScriptEngineFactory.walkJson(content, "itemContent/tweet_results/result");
                    if (result == null) {
                        continue;
                    }
                    final String typename = (String) result.get("__typename");
                    if (typename.equalsIgnoreCase("Tweet") || typename.equalsIgnoreCase("TweetWithVisibilityResults")) {
                        profileCrawlerFoundTweetsOnCurrentPage++;
                        final Map<String, Object> tweetmap = (Map<String, Object>) result.get("tweet");
                        final Map<String, Object> thisRoot;
                        if (tweetmap != null) {
                            thisRoot = tweetmap;
                        } else {
                            thisRoot = result;
                        }
                        final Map<String, Object> usr = (Map<String, Object>) JavaScriptEngineFactory.walkJson(thisRoot, "core/user_results/result/legacy");
                        final Map<String, Object> tweet = (Map<String, Object>) thisRoot.get("legacy");
                        if (tweet == null) {
                            continue;
                        }
                        final String tweet_id_str = (String) tweet.get("id_str");
                        if (singleTweetID != null && !StringUtils.equals(tweet_id_str, singleTweetID)) {
                            logger.info("Skipping tweetID because it does not match the one we're looking for: " + tweet_id_str);
                            continue;
                        }
                        final ArrayList<DownloadLink> thisTweetResults = crawlTweetMap(null, tweet, usr, fp);
                        Long crawledTweetTimestamp = null;
                        for (final DownloadLink thisTweetResult : thisTweetResults) {
                            /* Find timestamp of last added result. Ignore pinned tweets. */
                            final String tweetID = thisTweetResult.getStringProperty(PROPERTY_TWEET_ID);
                            if (pinned_tweet_ids_str == null || (tweetID != null && !pinned_tweet_ids_str.contains(tweetID))) {
                                crawledTweetTimestamp = thisTweetResult.getLongProperty(PROPERTY_DATE_TIMESTAMP, -1);
                                break;
                            }
                        }
                        if (this.crawlUntilTimestamp != null && crawledTweetTimestamp != null && crawledTweetTimestamp < crawlUntilTimestamp) {
                            profileCrawlerSkippedResultsByMaxDate.addAll(thisTweetResults);
                        } else if (this.maxTweetsToCrawl != null && profileCrawlerTotalCrawledTweetsCount == this.maxTweetsToCrawl.intValue()) {
                            profileCrawlerStopBecauseReachedUserDefinedMaxItemsLimit = true;
                            break timelineInstructionsLoop;
                        } else {
                            allowedResults.addAll(thisTweetResults);
                            profileCrawlerTotalCrawledTweetsCount++;
                        }
                    } else if (typename.equalsIgnoreCase("TweetTombstone")) {
                        profileCrawlerFoundTweetsOnCurrentPage++;
                        /* TODO: Check if this handling is working */
                        /* 18+ content. We can find the ID of that tweet but we can't know the name of the user who posted it. */
                        final String entryId = timelineEntry.get("entryId").toString();
                        final String thisTweetID = new Regex(entryId, "tweet-(\\d+)").getMatch(0);
                        if (thisTweetID == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        final DownloadLink link = this.createDownloadlink("https://" + this.getHost() + "/unknowntwitteruser/status/" + thisTweetID);
                        link._setFilePackage(fp);
                        allowedResults.add(link);
                        profileCrawlerTotalCrawledTweetsCount++;
                        if (this.maxTweetsToCrawl != null && profileCrawlerTotalCrawledTweetsCount == this.maxTweetsToCrawl.intValue()) {
                            profileCrawlerStopBecauseReachedUserDefinedMaxItemsLimit = true;
                            break timelineInstructionsLoop;
                        }
                    } else {
                        logger.info("Skipping unsupported __typename: " + typename);
                        continue;
                    }
                }
            }
        }
        return allowedResults;
    }

    /**
     * Obtains information about given username via old API. </br>
     * The response of this will also expose the users' userID which is often needed to perform further API requests.
     */
    private Map<String, Object> getUserInfo(final Browser br, final Account account, final String username) throws Exception {
        this.prepareAPI(br, account);
        /* 2023-08-11: Old API can only be used when we're logged in. */
        final boolean use_old_api_to_get_userid = true;
        final Map<String, Object> user;
        if (use_old_api_to_get_userid && account != null) {
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
            final Object responseO = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            if (!(responseO instanceof List)) {
                logger.warning("Unknown API error/response");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final List<Map<String, Object>> users = (List<Map<String, Object>>) responseO;
            user = users.get(0);
        } else {
            final String queryID = this.getGraphqlQueryID("UserByScreenName");
            br.getPage("https://api.twitter.com/graphql/" + queryID + "/UserByScreenName?variables=%7B%22screen_name%22%3A%22" + Encoding.urlEncode(username) + "%22%2C%22withSafetyModeUserFields%22%3Atrue%7D&features=%7B%22hidden_profile_likes_enabled%22%3Afalse%2C%22hidden_profile_subscriptions_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22subscriptions_verification_info_is_identity_verified_enabled%22%3Afalse%2C%22subscriptions_verification_info_verified_since_enabled%22%3Atrue%2C%22highlights_tweets_tab_ui_enabled%22%3Atrue%2C%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%7D&fieldToggles=%7B%22withAuxiliaryUserLabels%22%3Afalse%7D");
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
            final Map<String, Object> userNew = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/user/result");
            final String userID = userNew.get("rest_id").toString();
            user = (Map<String, Object>) userNew.get("legacy");
            /* 2023-08-11: Small ugly hack to keep compatibility ob subsequent methods */
            if (!user.containsKey("id_str")) {
                user.put("id_str", userID);
            }
        }
        return user;
    }

    private final String PROPERTY_RESUME_URL = "twitterResumeURL";

    @Override
    protected DownloadLink createLinkCrawlerRetry(final CrawledLink link, final DecrypterRetryException retryException) {
        final DownloadLink ret = super.createLinkCrawlerRetry(link, retryException);
        if (ret != null && resumeURL != null) {
            ret.setProperty(PROPERTY_RESUME_URL, resumeURL);
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
    private Map<String, Object> handleErrorsAPI(final Browser br) throws Exception {
        Map<String, Object> entries = null;
        try {
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.getRequest().getHtmlCode());
        } catch (final Exception e) {
            /* Check for some pure http error-responsecodes. */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 429) {
                throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT);
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
        return entries;
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

    /** Creates URL that will link to the firt video of a tweet containing at least one video item. */
    public static String createVideourl(final String tweetID) {
        return "https://twitter.com/i/videos/tweet/" + tweetID;
    }

    public static String createVideourlSpecific(final String user, final String tweetID, final int videoPosition) {
        return "https://twitter.com/" + user + "/status/" + tweetID + "/video/" + videoPosition;
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
