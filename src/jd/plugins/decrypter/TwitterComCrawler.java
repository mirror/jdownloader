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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.notify.BasicNotify;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.config.TwitterConfigInterface;
import org.jdownloader.plugins.components.config.TwitterConfigInterface.FilenameScheme;
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

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(429);
        return br;
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

    private static final Pattern           PATTERN_CARD                                                     = Pattern.compile("(?i)https?://[^/]+/i/cards/tfw/v1/(\\d+)");
    private static final Pattern           TYPE_USER_ALL                                                    = Pattern.compile("(?i)https?://[^/]+/([\\w\\-]+)(?:/(?:media|likes))?(\\?.*)?");
    private static final Pattern           TYPE_USER_LIKES                                                  = Pattern.compile("(?i)https?://[^/]+/([\\w\\-]+)/likes.*");
    private static final Pattern           TYPE_USER_MEDIA                                                  = Pattern.compile("(?i)https?://[^/]+/([\\w\\-]+)/media.*");
    private static final Pattern           PATTERN_SINGLE_TWEET                                             = Pattern.compile("(?i)https?://[^/]+/([^/]+)/status/(\\d+).*?");
    // private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private static AtomicReference<String> GUEST_TOKEN                                                      = new AtomicReference<String>();
    private static AtomicLong              GUEST_TOKEN_TS                                                   = new AtomicLong(-1);
    public static final String             PROPERTY_USERNAME                                                = "username";
    public static final String             PROPERTY_USER_ID                                                 = "user_id";
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
    public static final String             PROPERTY_PINNED_TWEET                                            = "is_pinned_tweet";
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

    public static enum ProfileCrawlMode {
        ALL_ITEMS,
        LIKES,
        MEDIA
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
        final String contenturl = getContentURL(param);
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
        final Regex singletweetVideoEmbed = new Regex(contenturl, TwitterCom.TYPE_VIDEO_EMBED);
        final Regex singletweet = new Regex(contenturl, PATTERN_SINGLE_TWEET);
        if (new Regex(contenturl, PATTERN_CARD).patternFind()) {
            return this.crawlCard(contenturl);
        } else if (singletweetVideoEmbed.patternFind()) {
            final String tweetID = singletweetVideoEmbed.getMatch(0);
            return this.crawlSingleTweet(account, null, tweetID);
        } else if (singletweet.patternFind()) {
            final String username = singletweet.getMatch(0);
            final String tweetID = singletweet.getMatch(1);
            return this.crawlSingleTweet(account, username, tweetID);
        } else {
            return this.crawlUser(param, account, contenturl);
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
        return PluginJSonUtils.getJson(brc, "guest_token");
    }

    private String getGraphqlQueryID(final String operationName) throws IOException, PluginException {
        synchronized (graphqlQueryids) {
            if (graphqlQueryids.isEmpty() || !graphqlQueryids.containsKey(operationName)) {
                final Browser br = this.createNewBrowserInstance();
                /* URL last updated: 2024-02-23 */
                br.getPage("https://abs.twimg.com/responsive-web/client-web/main.a2166cda.js");
                final HashSet<String> operationNamesToCache = new HashSet<String>();
                operationNamesToCache.add("UserByScreenName");
                operationNamesToCache.add("UserMedia");
                operationNamesToCache.add("UserTweets");
                operationNamesToCache.add("Likes");
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

    private ArrayList<DownloadLink> crawlSingleTweet(final Account account, final String username, final String tweetID) throws Exception {
        if (StringUtils.isEmpty(tweetID)) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return crawlSingleTweetViaGraphqlAPI(account, username, tweetID);
    }

    private ArrayList<DownloadLink> crawlSingleTweetViaGraphqlAPI(final Account account, final String username, final String tweetID) throws Exception {
        if (tweetID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.resetUglyGlobalVariables();
        final boolean preferNewWay20230811 = true;
        final ArrayList<DownloadLink> ret;
        final Map<String, Object> entries;
        if (preferNewWay20230811) {
            /* 2023-08-11 */
            // final String queryID = this.getGraphqlQueryID("TweetResultByRestId");
            this.prepareAPI(br, account);
            final String queryID = "0hWvDhmW8YQ-S_ib3azIrw";
            getPage("https://twitter.com/i/api/graphql/" + queryID + "/TweetResultByRestId?variables=%7B%22tweetId%22%3A%22" + tweetID
                    + "%22%2C%22withCommunity%22%3Afalse%2C%22includePromotedContent%22%3Afalse%2C%22withVoice%22%3Afalse%7D&features=%7B%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Afalse%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22responsive_web_media_download_video_enabled%22%3Afalse%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D");
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            ret = crawlTweets(entries, null, tweetID, null, false);
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
            entries = this.handleErrorsAPI(br);
            final List<Map<String, Object>> timelineInstructions = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "data/threaded_conversation_with_injections_v2/instructions");
            ret = this.crawlUserProfileGraphqlTimelineInstructions(timelineInstructions, null, tweetID, null, false);
        }
        if (ret.isEmpty()) {
            /* Nothing was found or at least nothing that is allowed to be returned by users' settings. */
            if (globalTweetIDsDupelist.size() > 0) {
                /* All elements were skipped due to the users' plugin settings. */
                final String bubbleNotificationTitle = "Tweet without results: " + tweetID;
                String bubbleNotificationText = "No results were returned because of your Twitter plugin settings.";
                bubbleNotificationText += "\r\nNumber of skipped items: " + globalTweetIDsDupelist.size();
                displayBubblenotifyMessage(bubbleNotificationTitle, bubbleNotificationText);
            } else {
                /* Nothing was found -> Try to find out why. */
                final String bubbleNotificationTitle = "Tweet unavailable: " + tweetID;
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
                        if (tweetUnavailableReasonHumanReadableText != null) {
                            displayBubblenotifyMessage(bubbleNotificationTitle, "Tweet unavailable because: " + tweetUnavailableReasonHumanReadableText);
                        } else {
                            displayBubblenotifyMessage(bubbleNotificationTitle, "Tweet unavailable because: API error: " + tweetUnavailableReasonInternal);
                        }
                    }
                } else if (br.getRequest().getHtmlCode().length() <= 100 && !br.containsHTML(tweetID)) {
                    /* E.g. {"data":{"tweetResult":{}}} */
                    /* Example URL: https://twitter.com/blabla/status/1760679931984683061 */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* No results and we don't know why */
                    displayBubblenotifyMessage(bubbleNotificationTitle, "Failed to crawl this Tweet for unknown reasons.");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        return ret;
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
        String directurl = link.getStringProperty(TwitterCom.PROPERTY_DIRECTURL);
        if (new Regex(link.getPluginPatternMatcher(), TwitterCom.TYPE_DIRECT).patternFind()) {
            /* Single added direct-URLs do not have the direct-URL set as a property. */
            directurl = link.getPluginPatternMatcher();
        }
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

    private static String formatTwitterDateFromDate(final Date date) {
        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(date);
    }

    private static String formatTwitterDateFromTimestamp(final long timestamp) {
        return formatTwitterDateFromDate(new Date(timestamp));
    }

    private ArrayList<DownloadLink> crawlUser(final CryptedLink param, final Account account, final String contenturl) throws Exception {
        final String username = new Regex(contenturl, TYPE_USER_ALL).getMatch(0);
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException();
        }
        final ProfileCrawlMode mode;
        if (contenturl.contains("/likes")) {
            mode = ProfileCrawlMode.LIKES;
        } else if (contenturl.contains("/media")) {
            mode = ProfileCrawlMode.MEDIA;
        } else {
            mode = ProfileCrawlMode.ALL_ITEMS;
        }
        if (mode == ProfileCrawlMode.LIKES && account == null) {
            logger.info("Account required to crawl all liked items of a user");
            throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "ACCOUNT_REQUIRED_TO_CRAWL_LIKED_ITEMS_OF_PROFILE_" + username, "Account is required to crawl liked items of profiles.");
        } else if (mode == ProfileCrawlMode.MEDIA && account == null) {
            logger.info("Account required to crawl all media items of a user");
            throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "ACCOUNT_REQUIRED_TO_CRAWL_MEDIA_ITEMS_OF_PROFILE_" + username, "Account is required to crawl media items of profiles.");
        }
        final String warningtextNoAccount = "You did not add a Twitter account to JDownloader or you've disabled it.\r\nAdd your twitter.com account under: Settings -> Account Manager -> Add";
        if (account == null) {
            displayBubblenotifyMessage("Profile crawler " + username + " | Warning", "Results may be incomplete!\r\nTwitter is hiding some posts (e.g. NSFW content) or profiles when a user is not logged in\r\n" + warningtextNoAccount);
        }
        final boolean crawlRetweets = PluginJsonConfig.get(TwitterConfigInterface.class).isCrawlRetweetsV2();
        if (account == null && crawlRetweets) {
            displayBubblenotifyMessage("Profile crawler " + username + " | Warning", "Results may be incomplete!\r\nYou've enabled re-tweet crawling in twitter plugin settings.\r\nTwitter is sometimes hiding Re-Tweets when users are not logged in.\r\n" + warningtextNoAccount);
        }
        return this.crawlUserViaGraphqlAPI(param, username, account, mode);
    }

    private void resetUglyGlobalVariables() {
        /* Clear some global variables (Yes I know, global variables are evil but this whole plugin is a mess so idk) */
        globalTweetIDsDupelist.clear();
        globalActuallyCrawledTweetIDs.clear();
        globalProfileCrawlerNextCursor = null;
        globalProfileCrawlerSkippedResultsByMaxDate.clear();
        globalProfileCrawlerSkippedResultsByMaxitems.clear();
        globalProfileCrawlerSkippedResultsByRetweet.clear();
        globalSumberofSkippedDeadTweets = 0;
    }

    /** Crawls all Tweets of a profile via GraphQL Web-API. */
    private ArrayList<DownloadLink> crawlUserViaGraphqlAPI(final CryptedLink param, final String username, final Account account, final ProfileCrawlMode crawlmode) throws Exception {
        if (username == null) {
            /* Developer mistake */
            throw new IllegalArgumentException();
        }
        resetUglyGlobalVariables();
        final Map<String, Object> user = getUserInfo(br, account, username);
        final int statuses_count = ((Number) user.get("statuses_count")).intValue();
        final int media_count = ((Number) user.get("media_count")).intValue();
        final int favorite_count = ((Number) user.get("favourites_count")).intValue();
        final String userID = user.get("id_str").toString();
        if (this.preGivenPageNumber != null && this.preGivenNumberOfTotalWalkedThroughTweetsCount != null && this.preGivenNextCursor != null) {
            // TODO: Review this
            /* Resume from last state */
            // profileCrawlerTotalCrawledTweetsCount = this.preGivenNumberOfTotalWalkedThroughTweetsCount.intValue();
            globalProfileCrawlerNextCursor = this.preGivenNextCursor;
        }
        final String bubbleNotifyTitle = "Twitter profile " + username + " | ID: " + userID;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowInheritance(true);
        fp.setAllowMerge(true);
        fp.setPackageKey("twitteruser://" + crawlmode.name());
        final String profileDescription = (String) user.get("description");
        if (!StringUtils.isEmpty(profileDescription)) {
            fp.setComment(profileDescription);
        }
        if (crawlmode == ProfileCrawlMode.LIKES) {
            if (favorite_count == 0) {
                displayBubblenotifyMessage(bubbleNotifyTitle, "Warning!\nYou are trying to crawl all likes of this profile but it has no liked items.");
                throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE, "PROFILE_HAS_NO_LIKES_" + username, "You are trying to crawl all likes of this profile but it has no liked items.");
            }
            fp.setName(username + " - likes");
            final ArrayList<DownloadLink> results = crawlTweetsViaGraphqlAPI("Likes", param, user, account, crawlmode, fp);
            ret.addAll(results);
        } else if (crawlmode == ProfileCrawlMode.MEDIA) {
            if (media_count == 0) {
                displayBubblenotifyMessage(bubbleNotifyTitle, "Warning!\nYou are trying to crawl all media items of this profile but it has no media items.");
                throw new DecrypterRetryException(RetryReason.EMPTY_PROFILE, "PROFILE_HAS_NO_MEDIA_ITEMS_" + username, "You are trying to crawl all media items of this profile but it has no media items.");
            }
            fp.setName(username + " - media");
            final ArrayList<DownloadLink> results = crawlTweetsViaGraphqlAPI("UserMedia", param, user, account, crawlmode, fp);
            ret.addAll(results);
        } else {
            /* Crawl all items of user + all media items */
            fp.setName(username);
            final ArrayList<DownloadLink> results1 = crawlTweetsViaGraphqlAPI("UserTweets", param, user, account, crawlmode, fp);
            ret.addAll(results1);
            if (globalProfileCrawlerSkippedResultsByMaxitems.size() == 0 && media_count > 0) {
                logger.info("Crawling remaining " + media_count + " media items from profile");
                if (account == null) {
                    /**
                     * 2024-02-23: Account required to access: /<username>/media </br>
                     * Without account, API will return an empty page.
                     */
                    String text = media_count + " media items can't be crawled because the media tab can only be accessed by logged in users!";
                    text += "\nAdd- and enable a Twitter account to JD to be able to crawl such items.";
                    displayBubblenotifyMessage(bubbleNotifyTitle, text);
                } else {
                    /*
                     * Crawl all media items. Reset global variables before since it can happen that the first few pages of the media tab
                     * will contain items which we've already crawled.
                     */
                    resetUglyGlobalVariables();
                    final ArrayList<DownloadLink> results2 = crawlTweetsViaGraphqlAPI("UserMedia", param, user, account, ProfileCrawlMode.MEDIA, fp);
                    ret.addAll(results2);
                }
            }
        }
        final int totalWalkedThroughTweets = globalTweetIDsDupelist.size();
        if (ret.isEmpty()) {
            /* We found nothing -> Check why */
            final String bubbleNotifyTextEnding = "\r\nTotal number of Tweets in this profile: " + statuses_count;
            if (totalWalkedThroughTweets == 0) {
                if (statuses_count > 0 && account == null) {
                    displayBubblenotifyMessage(bubbleNotifyTitle, "Returning no results because:\r\nAccount required to view Tweets of this profile." + bubbleNotifyTextEnding);
                    throw new AccountRequiredException();
                } else {
                    /* No results and we don't know why. */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                /* No results because of users' settings. */
                if (globalProfileCrawlerSkippedResultsByMaxDate.size() > 0) {
                    displayBubblenotifyMessage(bubbleNotifyTitle, "Returning no results because:\r\nAll existing elements have earlier timestamps than user defined max_date parameter " + maxTweetDateStr + ".\r\nMinimum number of skipped Tweets: " + globalProfileCrawlerSkippedResultsByMaxDate.size() + bubbleNotifyTextEnding);
                } else {
                    displayBubblenotifyMessage(bubbleNotifyTitle, "Returning no results because:\r\nMost likely user has disabled tweet text crawler but this profile only contained text tweets.\r\nMinimum number of skipped possible Tweets: " + totalWalkedThroughTweets + bubbleNotifyTextEnding);
                }
            }
        } else if (totalWalkedThroughTweets < statuses_count && globalProfileCrawlerSkippedResultsByRetweet.isEmpty() && globalProfileCrawlerSkippedResultsByMaxDate.isEmpty() && globalProfileCrawlerSkippedResultsByMaxitems.isEmpty()) {
            /* No items were skipped but also not all items were found -> Notify user */
            String text = "Some items may be missing!";
            text += "\nTotal number of status items: " + statuses_count + " Crawled: " + totalWalkedThroughTweets;
            text += "\nLogged in users can sometimes view more items than anonymous users.";
            displayBubblenotifyMessage(bubbleNotifyTitle, text);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlTweetsViaGraphqlAPI(final String queryName, final CryptedLink param, final Map<String, Object> user, final Account account, final ProfileCrawlMode mode, final FilePackage fp) throws Exception {
        final String userID = user.get("id_str").toString();
        final HashSet<String> cursorDupes = new HashSet<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        if (this.preGivenPageNumber != null && this.preGivenNumberOfTotalWalkedThroughTweetsCount != null && this.preGivenNextCursor != null) {
            // TODO: Review this
            /* Resume from last state */
            page = this.preGivenPageNumber.intValue();
            // profileCrawlerTotalCrawledTweetsCount = this.preGivenNumberOfTotalWalkedThroughTweetsCount.intValue();
            globalProfileCrawlerNextCursor = this.preGivenNextCursor;
        }
        final boolean allowSkipRetweets;
        if (mode == ProfileCrawlMode.LIKES) {
            allowSkipRetweets = false;
        } else {
            allowSkipRetweets = true;
        }
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final long waitBetweenPaginationRequestsMillis = cfg.getProfileCrawlerWaittimeBetweenPaginationMilliseconds();
        final String queryID = this.getGraphqlQueryID(queryName);
        logger.info("Crawling query " + queryName + " | queryID: " + queryID);
        do {
            final Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("userId", userID);
            variables.put("count", 20);
            if (globalProfileCrawlerNextCursor != null) {
                variables.put("cursor", globalProfileCrawlerNextCursor);
            }
            variables.put("includePromotedContent", true);
            variables.put("withQuickPromoteEligibilityTweetFields", true);
            variables.put("withSuperFollowsUserFields", true);
            variables.put("withVoice", true);
            variables.put("withV2Timeline", true);
            /* Not important */
            if (mode == ProfileCrawlMode.LIKES) {
                variables.put("withBirdwatchNotes", false);
            }
            final UrlQuery query = new UrlQuery();
            query.addAndReplace("variables", Encoding.urlEncode(JSonStorage.serializeToJson(variables)));
            query.addAndReplace("features",
                    "%7B%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22c9s_tweet_anatomy_moderator_badge_enabled%22%3Atrue%2C%22tweetypie_unmention_optimization_enabled%22%3Atrue%2C%22responsive_web_edit_tweet_api_enabled%22%3Atrue%2C%22graphql_is_translatable_rweb_tweet_is_translatable_enabled%22%3Atrue%2C%22view_counts_everywhere_api_enabled%22%3Atrue%2C%22longform_notetweets_consumption_enabled%22%3Atrue%2C%22responsive_web_twitter_article_tweet_consumption_enabled%22%3Atrue%2C%22tweet_awards_web_tipping_enabled%22%3Afalse%2C%22freedom_of_speech_not_reach_fetch_enabled%22%3Atrue%2C%22standardized_nudges_misinfo%22%3Atrue%2C%22tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled%22%3Atrue%2C%22rweb_video_timestamps_enabled%22%3Atrue%2C%22longform_notetweets_rich_text_read_enabled%22%3Atrue%2C%22longform_notetweets_inline_media_enabled%22%3Atrue%2C%22responsive_web_enhance_cards_enabled%22%3Afalse%7D");
            final String url = API_BASE_GRAPHQL + "/" + queryID + "/" + queryName + "?" + query.toString();
            getPage(url);
            final Map<String, Object> entries = this.handleErrorsAPI(br);
            final List<Map<String, Object>> timelineInstructions = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "data/user/result/timeline_v2/timeline/instructions");
            final int numberofTweetsWalkedThroughBefore = globalTweetIDsDupelist.size();
            final List<DownloadLink> resultsThisPage = this.crawlUserProfileGraphqlTimelineInstructions(timelineInstructions, user, null, fp, allowSkipRetweets);
            final int numberofNewTweetsWalkedThroughThisPage = globalTweetIDsDupelist.size() - numberofTweetsWalkedThroughBefore;
            ret.addAll(resultsThisPage);
            distribute(resultsThisPage);
            logger.info("Crawled page " + page + " | Found new Tweets on this page: " + numberofNewTweetsWalkedThroughThisPage + " | Tweets walked through so far: " + globalTweetIDsDupelist.size() + " | Tweets crawled and added so far: " + globalActuallyCrawledTweetIDs.size() + " | nextCursor = " + globalProfileCrawlerNextCursor + " | Skipped Re-Tweets: " + globalProfileCrawlerSkippedResultsByRetweet.size() + " | Skipped Tweets via user defined max-date: " + globalProfileCrawlerSkippedResultsByMaxDate.size() + " | Skipped dead Tweets so far: " + this.globalSumberofSkippedDeadTweets);
            /* Check abort conditions */
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (StringUtils.isEmpty(globalProfileCrawlerNextCursor)) {
                logger.info("Stopping because: Failed to find nextCursor");
                break;
            } else if (!cursorDupes.add(globalProfileCrawlerNextCursor)) {
                logger.info("Stopping because: nextCursor value for  next page has already been crawled -> Reached end?");
                break;
            } else if (numberofNewTweetsWalkedThroughThisPage == 0) {
                /* Fail-safe */
                logger.info("Stopping because: Failed to find any new Tweets on current page");
                break;
            } else if (globalProfileCrawlerSkippedResultsByMaxitems.size() > 0) {
                logger.info("Stopping because: Reached user defined max items count: " + maxTweetsToCrawl + " | Actually crawled: " + globalTweetIDsDupelist.size());
                break;
            } else if (globalProfileCrawlerSkippedResultsByMaxDate.size() > 0) {
                logger.info("Stopping because: Last item age is older than user defined max age " + this.maxTweetDateStr);
                break;
            } else {
                /* Continue to next page */
                page++;
                /* Wait before accessing next page. */
                logger.info("Waiting " + waitBetweenPaginationRequestsMillis + " milliseconds before accessing next page");
                sleep(waitBetweenPaginationRequestsMillis, param);
                continue;
            }
        } while (true);
        logger.info("Last nextCursor: " + globalProfileCrawlerNextCursor);
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
    private String                      globalProfileCrawlerNextCursor               = null;
    /*
     * Returns Twitter status IDs of all items that have been walked-through. This does not mean that those will actually be returned - they
     * can be filtered by user-settings. This variable mainly exists to prevent infinite loops.
     */
    private HashSet<String>             globalTweetIDsDupelist                       = new HashSet<String>();
    /* List of tweetIDs which have not only been walked-through but were actually added to the list of final items. */
    private HashSet<String>             globalActuallyCrawledTweetIDs                = new HashSet<String>();
    private final HashSet<DownloadLink> globalProfileCrawlerSkippedResultsByMaxDate  = new HashSet<DownloadLink>();
    private final HashSet<DownloadLink> globalProfileCrawlerSkippedResultsByMaxitems = new HashSet<DownloadLink>();
    private final HashSet<DownloadLink> globalProfileCrawlerSkippedResultsByRetweet  = new HashSet<DownloadLink>();
    private long                        globalSumberofSkippedDeadTweets              = 0;                          // Counts TweetTombstone
                                                                                                                   // items

    private ArrayList<DownloadLink> crawlUserProfileGraphqlTimelineInstructions(final List<Map<String, Object>> timelineInstructions, final Map<String, Object> user, final String singleTweetID, final FilePackage fp, final boolean crawlUserLikes) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> timelineInstruction : timelineInstructions) {
            final String timelineInstructionType = timelineInstruction.get("type").toString();
            if (timelineInstructionType.equals("TimelineAddEntries")) {
                final List<Map<String, Object>> timelineEntries = (List<Map<String, Object>>) timelineInstruction.get("entries");
                for (final Map<String, Object> timelineEntry : timelineEntries) {
                    final Map<String, Object> content = (Map<String, Object>) timelineEntry.get("content");
                    final String contentType = (String) content.get("entryType");
                    if (contentType.equalsIgnoreCase("TimelineTimelineCursor")) {
                        if (content.get("cursorType").toString().equalsIgnoreCase("Bottom")) {
                            globalProfileCrawlerNextCursor = content.get("value").toString();
                            /* We've reached the end of current page */
                            break;
                        }
                    }
                }
            }
            final ArrayList<DownloadLink> thisResults = crawlTweets(timelineInstruction, user, singleTweetID, fp, crawlUserLikes);
            ret.addAll(thisResults);
        }
        return ret;
    }

    /** Crawls all Tweets inside _any_ given Map. */
    private ArrayList<DownloadLink> crawlTweets(final Map<String, Object> sourcemap, final Map<String, Object> user, final String singleTweetID, final FilePackage fp, final boolean allowSkipRetweets) throws PluginException, MalformedURLException {
        final String username = user != null ? user.get("screen_name").toString() : null;
        final List<Map<String, Object>> tweetResults = new ArrayList<Map<String, Object>>();
        final ArrayList<DownloadLink> allowedResults = new ArrayList<DownloadLink>();
        recursiveFindTweetMaps(sourcemap, tweetResults);
        for (final Map<String, Object> tweetResult : tweetResults) {
            final String typename = (String) tweetResult.get("__typename");
            if (typename.equalsIgnoreCase("Tweet") || typename.equalsIgnoreCase("TweetWithVisibilityResults")) {
                final Map<String, Object> tweetmap = (Map<String, Object>) tweetResult.get("tweet");
                final Map<String, Object> thisRoot;
                if (tweetmap != null) {
                    thisRoot = tweetmap;
                } else {
                    thisRoot = tweetResult;
                }
                Map<String, Object> quoted_status = (Map<String, Object>) JavaScriptEngineFactory.walkJson(thisRoot, "quoted_status_result/result/tweet");
                if (quoted_status == null) {
                    quoted_status = (Map<String, Object>) JavaScriptEngineFactory.walkJson(thisRoot, "quoted_status_result/result");
                }
                /* If this is null, the quoted Tweet does not exist anymore. */
                final Map<String, Object> quoted_status_tombstone = quoted_status != null ? (Map<String, Object>) quoted_status.get("tombstone") : null;
                Map<String, Object> retweetRootMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(thisRoot, "legacy/retweeted_status");
                if (retweetRootMap == null) {
                    retweetRootMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(thisRoot, "legacy/retweeted_status_result/result/tweet");
                    if (retweetRootMap == null) {
                        retweetRootMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(thisRoot, "legacy/retweeted_status_result/result");
                    }
                }
                final boolean containsQuotedTweet = quoted_status != null && quoted_status_tombstone == null;
                /* Results of this Tweet */
                final ArrayList<DownloadLink> thisTweetResults = crawlTweetMap(null, thisRoot, fp);
                /* Results of Tweet which was quoted by this one */
                ArrayList<DownloadLink> thisQuotedTweetResults = null;
                /* Results of Retweet if this Tweet is a Retweet */
                ArrayList<DownloadLink> thisRetweetResults = null;
                if (containsQuotedTweet) {
                    /*  */
                    /* 2024-02-21: Current Tweet is a reply to a quoted Tweet -> We need to crawl that quoted Tweet separately */
                    thisQuotedTweetResults = crawlTweetMap(null, quoted_status, fp);
                } else if (retweetRootMap != null) {
                    /* Retweet: Crawl Retweet and replace results of original Tweet with Retweet results. */
                    thisRetweetResults = crawlTweetMap(null, retweetRootMap, fp);
                    /* Find a suitable source Tweet item to inherit properties from. */
                    DownloadLink anySourceTweetItem = null;
                    for (final DownloadLink sourceTweetItem : thisTweetResults) {
                        if (sourceTweetItem.hasProperty(PROPERTY_TWEET_ID)) {
                            anySourceTweetItem = sourceTweetItem;
                            break;
                        }
                    }
                    final String[] inheritProperties = new String[] { PROPERTY_USERNAME, PROPERTY_USER_ID, PROPERTY_DATE, PROPERTY_DATE_TIMESTAMP, PROPERTY_MEDIA_COUNT, PROPERTY_TWEET_ID };
                    for (final DownloadLink retweetResult : thisRetweetResults) {
                        final String retweetID = retweetResult.getStringProperty(PROPERTY_TWEET_ID);
                        if (retweetID == null) {
                            /* Skip external items such as crawled http URLs. */
                            continue;
                        }
                        /* Mark Retweets as such. */
                        retweetResult.setProperty(PROPERTY_RETWEET, true);
                        /* Set source Tweet properties on this Retweet results. */
                        for (final String inheritProperty : inheritProperties) {
                            final Object propertyObj = anySourceTweetItem.getProperty(inheritProperty);
                            if (propertyObj != null) {
                                retweetResult.setProperty("source_" + inheritProperty, propertyObj);
                            }
                        }
                    }
                    /* Delete source Tweet results so we will only return the Retweet result here. */
                    thisTweetResults.clear();
                    thisTweetResults.addAll(thisRetweetResults);
                }
                /* Collect all results */
                final ArrayList<DownloadLink> thisAllResults = new ArrayList<DownloadLink>();
                thisAllResults.addAll(thisTweetResults);
                if (thisQuotedTweetResults != null) {
                    thisAllResults.addAll(thisQuotedTweetResults);
                }
                if (thisRetweetResults != null) {
                    thisAllResults.addAll(thisRetweetResults);
                }
                /* Collect some information from results */
                final HashSet<String> thisRetweetIDs = new HashSet<String>();
                final HashSet<String> thisAllTweetIDs = new HashSet<String>();
                for (final DownloadLink tweetresult : thisAllResults) {
                    final String thisTweetID = tweetresult.getStringProperty(PROPERTY_TWEET_ID);
                    if (thisTweetID == null) {
                        continue;
                    }
                    thisAllTweetIDs.add(thisTweetID);
                    /*
                     * Tweets from other users than the one we are crawling now are either Retweets or Tweets which are part of Tweet
                     * comments/replies. Typicall if a user does not want to have Retweets we want to filter such items too.
                     */
                    if (tweetresult.hasProperty(PROPERTY_RETWEET)) {
                        thisRetweetIDs.add(thisTweetID);
                    }
                }
                /* Check some skip conditions */
                if (singleTweetID != null && tweetResults.size() > 1 && !thisAllTweetIDs.contains(singleTweetID)) {
                    /* Fail-safe */
                    logger.info("Single Tweet filter: Skipping the following results because they do not contain the item we are looking for: " + tweetResults);
                    continue;
                } else if (singleTweetID == null && allowSkipRetweets && !PluginJsonConfig.get(TwitterConfigInterface.class).isCrawlRetweetsV2() && thisRetweetResults != null) {
                    /* Re-Tweet crawling is disabled: Skip all results of this Tweet if it is a Re-Tweet. */
                    logger.info("Retweet filter: Skipping Retweet(s): " + thisRetweetIDs.toString());
                    for (final DownloadLink retweetresult : thisRetweetResults) {
                        globalProfileCrawlerSkippedResultsByRetweet.add(retweetresult);
                    }
                    continue;
                }
                /* Determine timestamp of "last Tweet". */
                final ArrayList<DownloadLink> resultsForLastTweetDateFinder = new ArrayList<DownloadLink>();
                resultsForLastTweetDateFinder.addAll(thisTweetResults);
                if (thisRetweetResults != null) {
                    resultsForLastTweetDateFinder.addAll(thisRetweetResults);
                }
                Long crawledTweetTimestamp = null;
                for (final DownloadLink thisTweetResult : resultsForLastTweetDateFinder) {
                    /* Find timestamp of last added result. Ignore pinned Tweets. */
                    final String thisTweetD = thisTweetResult.getStringProperty(PROPERTY_TWEET_ID);
                    final boolean isPinnedTweet = thisTweetResult.getBooleanProperty(PROPERTY_PINNED_TWEET, false);
                    if (thisTweetD == null) {
                        /* Skip items which would not go into Twitter hosterplugin such as crawled external URLs inside post-text. */
                        continue;
                    } else if (isPinnedTweet) {
                        /* Ignore pinned items */
                        continue;
                    } else {
                        /* Find date of last crawled Tweet. */
                        logger.info("Tweet used as source for last crawled Tweet timestamp: " + thisTweetD);
                        /* If we got a Re-Tweet we want to use the date of the source Tweet here. */
                        crawledTweetTimestamp = thisTweetResult.getLongProperty("source_" + PROPERTY_DATE_TIMESTAMP, -1);
                        if (crawledTweetTimestamp == -1) {
                            crawledTweetTimestamp = thisTweetResult.getLongProperty(PROPERTY_DATE_TIMESTAMP, -1);
                        }
                    }
                }
                /* Check if we've reached any user defined limits */
                if (this.crawlUntilTimestamp != null && crawledTweetTimestamp != null && crawledTweetTimestamp < crawlUntilTimestamp) {
                    /* Skip all results */
                    globalProfileCrawlerSkippedResultsByMaxDate.addAll(thisAllResults);
                } else if (this.maxTweetsToCrawl != null && globalActuallyCrawledTweetIDs.size() >= this.maxTweetsToCrawl.intValue()) {
                    /* Skip all results */
                    globalProfileCrawlerSkippedResultsByMaxitems.addAll(thisAllResults);
                } else {
                    /* Add results */
                    allowedResults.addAll(thisAllResults);
                    globalActuallyCrawledTweetIDs.addAll(thisAllTweetIDs);
                }
            } else if (typename.equalsIgnoreCase("TweetTombstone")) {
                /* Dead Tweet without more information -> Count that. */
                this.globalSumberofSkippedDeadTweets += 1;
            } else {
                logger.info("Skipping unsupported tweetResult __typename: " + typename);
                continue;
            }
        }
        return allowedResults;
    }

    /**
     * Crawls single media objects obtained via API.
     *
     * @throws MalformedURLException
     * @throws PluginException
     *
     * @param username:
     *            Pre given username (only needed if we know in beforehand that all Tweet items we will process belong to one user).
     */
    private ArrayList<DownloadLink> crawlTweetMap(String username, final Map<String, Object> thisRoot, FilePackage fp) throws MalformedURLException, PluginException {
        final TwitterConfigInterface cfg = PluginJsonConfig.get(TwitterConfigInterface.class);
        final Map<String, Object> tweet = (Map<String, Object>) thisRoot.get("legacy");
        final Map<String, Object> user = (Map<String, Object>) JavaScriptEngineFactory.walkJson(thisRoot, "core/user_results/result/legacy");
        if (user == null) {
            throw new IllegalArgumentException();
        }
        final String tweetID = tweet.get("id_str").toString();
        globalTweetIDsDupelist.add(tweetID);
        /* Commented out debug code down below */
        // if (tweetID.equals("test")) {
        // logger.info("hit");
        // }
        if (username == null) {
            username = user.get("screen_name").toString();
        }
        final List<String> pinned_tweet_ids_str = (List<String>) user.get("pinned_tweet_ids_str");
        final String created_at = tweet.get("created_at").toString();
        final long timestamp = getTimestampTwitterDate(created_at);
        final String formattedDate = formatTwitterDateFromTimestamp(timestamp);
        /* Some premium accounts can post longer text Tweets. The full text of such Tweets is stored at a different place. */
        final String extraLongTweetText = (String) JavaScriptEngineFactory.walkJson(thisRoot, "note_tweet/note_tweet_results/result/text");
        String tweetText = null;
        if (!StringUtils.isEmpty(extraLongTweetText)) {
            tweetText = extraLongTweetText;
        } else {
            tweetText = (String) tweet.get("full_text");
        }
        if (tweetText != null) {
            tweetText = sanitizeTweetText(tweetText);
        }
        final boolean isReplyToOtherTweet = tweet.get("in_reply_to_status_id_str") != null;
        if (fp == null) {
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
            // TODO: 2024-03-27: Check if this is still needed
            /* Fallback handling for very old (???) content */
            /* Expect such URLs which our host plugin can handle: https://video.twimg.com/amplify_video/vmap/<numbers>.vmap */
            final DownloadLink singleVideo = this.createDownloadlink(vmapURL);
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
                    if (whitelistPattern == null || new Regex(url, whitelistPattern).patternFind()) {
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
            /* Crawl Tweet as text if wanted by user or if Tweet contains only text. */
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
        final boolean isPinnedTweet = pinned_tweet_ids_str != null && pinned_tweet_ids_str.contains(tweetID);
        for (final DownloadLink dl : retInternal) {
            /* Add additional properties */
            dl.setContentUrl(urlToTweet);
            dl.setProperty(PROPERTY_USERNAME, username);
            dl.setProperty(PROPERTY_USER_ID, tweet.get("user_id_str"));
            dl.setProperty(PROPERTY_TWEET_ID, tweetID);
            dl.setProperty(PROPERTY_DATE, formattedDate);
            dl.setProperty(PROPERTY_DATE_TIMESTAMP, timestamp);
            if (!StringUtils.isEmpty(tweetText)) {
                dl.setProperty(PROPERTY_TWEET_TEXT, tweetText);
            }
            if (isReplyToOtherTweet) {
                dl.setProperty(PROPERTY_REPLY, true);
            }
            if (isPinnedTweet) {
                dl.setProperty(PROPERTY_PINNED_TWEET, true);
            }
            /* Set filename which gets created based on user settings and previously set properties. */
            setFormattedFilename(dl);
        }
        final ArrayList<DownloadLink> retAll = new ArrayList<DownloadLink>();
        retAll.addAll(retInternal);
        retAll.addAll(retExternal);
        fp.addLinks(retAll);
        if (retMedia.isEmpty()) {
            if (itemsSkippedDueToPluginSettings == 0) {
                /* Tweet contains only text */
                logger.info("Failed to find any crawlable media content in Tweet: " + tweetID);
            } else {
                logger.info("Failed to find any crawlable content in Tweet " + tweetID + " because of user settings. Crawlable but skipped " + itemsSkippedDueToPluginSettings + " item(s) due to users' plugin settings.");
            }
        }
        return retAll;
    }

    /** Lazy recursive function which will find app tweet-maps in any given map. */
    private void recursiveFindTweetMaps(final Object o, final List<Map<String, Object>> results) {
        if (o instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) o;
            final String __typename = (String) map.get("__typename");
            final Map<String, Object> tweetMap = (Map<String, Object>) map.get("tweet");
            final Map<String, Object> legacyMap = (Map<String, Object>) map.get("legacy");
            // Debug- code down below if we need to debug into an item with a specific ID
            // if (tweetMap != null) {
            // final Map<String, Object> leacy = (Map<String, Object>) tweetMap.get("legacy");
            // final String id = leacy.get("id_str").toString();
            // if (id.equals("test")) {
            // logger.info("hit");
            // }
            // }
            // final String rest_id = (String) map.get("rest_id");
            // if (rest_id != null && rest_id.equals("TEST")) {
            // logger.info("hit");
            // }
            if (__typename != null && (__typename.equals("Tweet") || __typename.equals("TweetWithVisibilityResults")) && (tweetMap != null || legacyMap != null)) {
                results.add(map);
            } else if (__typename != null && __typename.equals("TweetTombstone")) {
                /* Dead/deleted Tweet */
                results.add(map);
            } else {
                for (final Map.Entry<String, Object> entry : map.entrySet()) {
                    // final String key = entry.getKey();
                    final Object value = entry.getValue();
                    if (value instanceof String && value.toString().startsWith("promoted-tweet")) {
                        // Skip ads / "fake-tweets"
                        return;
                    }
                    if (value instanceof List || value instanceof Map) {
                        recursiveFindTweetMaps(value, results);
                    }
                }
            }
            return;
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    recursiveFindTweetMaps(arrayo, results);
                }
            }
            return;
        } else {
            return;
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
            br.getPage("https://api.twitter.com/graphql/" + queryID + "/UserByScreenName?variables=%7B%22screen_name%22%3A%22" + PluginJSonUtils.escape(username)
                    + "%22%2C%22withSafetyModeUserFields%22%3Atrue%7D&features=%7B%22hidden_profile_likes_enabled%22%3Atrue%2C%22hidden_profile_subscriptions_enabled%22%3Atrue%2C%22responsive_web_graphql_exclude_directive_enabled%22%3Atrue%2C%22verified_phone_label_enabled%22%3Afalse%2C%22subscriptions_verification_info_is_identity_verified_enabled%22%3Atrue%2C%22subscriptions_verification_info_verified_since_enabled%22%3Atrue%2C%22highlights_tweets_tab_ui_enabled%22%3Atrue%2C%22responsive_web_twitter_article_notes_tab_enabled%22%3Atrue%2C%22creator_subscriptions_tweet_preview_api_enabled%22%3Atrue%2C%22responsive_web_graphql_skip_user_profile_image_extensions_enabled%22%3Afalse%2C%22responsive_web_graphql_timeline_navigation_enabled%22%3Atrue%7D&fieldToggles=%7B%22withAuxiliaryUserLabels%22%3Afalse%7D");
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

    public static String sanitizeTweetText(final String text) {
        if (text == null) {
            return null;
        } else {
            return Encoding.htmlOnlyDecode(text).trim();
        }
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
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Invalid API response");
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
            final String ratelimitresetStr = br.getRequest().getResponseHeader("x-rate-limit-reset");
            long timeUntilReset = -1;
            if (ratelimitresetStr != null && ratelimitresetStr.matches("\\d+")) {
                timeUntilReset = Long.parseLong(ratelimitresetStr) * 1000 - System.currentTimeMillis();
            }
            final String title = "Rate-Limit reached";
            String text = "Time until rate-limit reset: " + TimeFormatter.formatMilliSeconds(timeUntilReset, 0);
            text += "\nTry again later or change your IP and use a different Twitter account to get around this limit.";
            this.displayBubblenotifyMessage(title, text);
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

    @Deprecated
    private ArrayList<DownloadLink> crawlCard(final String contenturl) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String tweetID = new Regex(contenturl, PATTERN_CARD).getMatch(0);
        if (tweetID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(contenturl);
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

    /**
     * Crawls items of links like this: https://twitter.com/i/broadcasts/<broadcastID> </br>
     * 2023-09-01: Stopped working on this as the streams are DRM protected. Reference: https://board.jdownloader.org/showthread.php?t=94178
     *
     * @throws Exception
     */
    @SuppressWarnings("unused")
    @Deprecated
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

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-30: We have to perform a lot of requests --> Set this to 1. */
        return 1;
    }
}
