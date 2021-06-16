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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "twitter.com", "t.co" }, urls = { "https?://(?:www\\.|mobile\\.)?twitter\\.com/[A-Za-z0-9_\\-]+/status/\\d+|https?://(?:www\\.|mobile\\.)?twitter\\.com/(?!i/)[A-Za-z0-9_\\-]{2,}(?:/(?:media|likes))?|https://twitter\\.com/i/cards/tfw/v1/\\d+|https?://(?:www\\.)?twitter\\.com/i/videos/tweet/\\d+", "https?://t\\.co/[a-zA-Z0-9]+" })
public class TwitterCom extends PornEmbedParser {
    public TwitterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     TYPE_CARD         = "https?://[^/]+/i/cards/tfw/v1/(\\d+)";
    private static final String     TYPE_USER_ALL     = "https?://[^/]+/[A-Za-z0-9_\\-]+(?:/(?:media|likes))?";
    private static final String     TYPE_USER_POST    = "https?://(?:www\\.)?twitter\\.com.*?status/(\\d+).*?";
    private static final String     TYPE_REDIRECT     = "https?://t\\.co/[a-zA-Z0-9]+";
    private ArrayList<DownloadLink> decryptedLinks    = new ArrayList<DownloadLink>();
    private static Object           LOCK              = new Object();
    private static String           guest_token       = null;
    private static final String     PROPERTY_USERNAME = "username";

    protected DownloadLink createDownloadlink(final String link, final String tweetid) {
        final DownloadLink ret = super.createDownloadlink(link);
        ret.setProperty("tweetid", tweetid);
        return ret;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setAllowedResponseCodes(new int[] { 429 });
        final String parameter = param.toString().replaceAll("https?://(www\\.|mobile\\.)?twitter\\.com/", "https://twitter.com/");
        final String urlfilename = getUrlFname(parameter);
        String tweet_id = null;
        if (parameter.matches(TYPE_REDIRECT)) {
            br.setFollowRedirects(false);
            getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;URL=(https?[^<>\"]*?)(#_=_)?\"").getMatch(0);
            }
            if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.getRequest().getHttpConnection().getResponseCode() == 404 || finallink == null) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                offline.setFinalFileName(urlfilename);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        /* Some profiles can only be accessed if they accepted others as followers --> Login if the user has added his twitter account */
        final Account account = getUserLogin(false);
        if (account != null) {
            logger.info("Account available and we're logged in");
        } else {
            logger.info("No account available or login failed");
        }
        if (parameter.matches(TYPE_CARD)) {
            getPage(parameter);
            if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.getRequest().getHttpConnection().getResponseCode() == 404) {
                if (parameter.contains("/cards/")) {
                    return decryptedLinks;
                }
                final DownloadLink offline = this.createOfflinelink(parameter);
                offline.setFinalFileName(urlfilename);
                decryptedLinks.add(offline);
                return decryptedLinks;
            } else if (br.containsHTML("class=\"ProtectedTimeline\"")) {
                logger.info("This tweet timeline is protected (private)");
                final DownloadLink offline = this.createOfflinelink(parameter);
                offline.setFinalFileName("This tweet timeline is protected_" + urlfilename);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            tweet_id = new Regex(parameter, TYPE_CARD).getMatch(0);
            /* First check for external urls */
            decryptedLinks.addAll(this.findEmbedUrls(null));
            String externID = br.getRegex("u\\-linkClean js\\-openLink\" href=\"(https?://t\\.co/[^<>\"]*?)\"").getMatch(0);
            if (externID == null) {
                externID = br.getRegex("\"card_ur(?:i|l)\"[\t\n\r ]*?:[\t\n\r ]*?\"(https?[^<>\"]*?)\"").getMatch(0);
            }
            if (externID != null) {
                decryptedLinks.add(this.createDownloadlink(externID));
                return decryptedLinks;
            }
            if (decryptedLinks.isEmpty()) {
                String dllink = br.getRegex("playlist\\&quot;:\\[\\{\\&quot;source\\&quot;:\\&quot;(https[^<>\"]*?\\.(?:webm|mp4))").getMatch(0);
                if (dllink == null) {
                    logger.info("dllink == null, abend ");
                    return null;
                }
                dllink = dllink.replace("\\", "");
                final String filename = tweet_id + "_" + new Regex(dllink, "([^/]+\\.[a-z0-9]+)$").getMatch(0);
                final DownloadLink dl = this.createDownloadlink(dllink, tweet_id);
                dl.setProperty("decryptedfilename", filename);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else if (parameter.matches(jd.plugins.hoster.TwitterCom.TYPE_VIDEO_EMBED)) {
            final String tweetID = new Regex(param.getCryptedUrl(), jd.plugins.hoster.TwitterCom.TYPE_VIDEO_EMBED).getMatch(0);
            crawlAPITweet(param, tweetID, account);
        } else if (parameter.matches(TYPE_USER_POST)) {
            final boolean prefer_mobile_website = false;
            if (prefer_mobile_website) {
                /* Single Tweet */
                if (switchtoMobile()) {
                    crawlTweetViaMobileWebsite(parameter, null);
                    return decryptedLinks;
                }
                /* Fallback to API/normal website */
            }
            final String tweetID = new Regex(param.getCryptedUrl(), TYPE_USER_POST).getMatch(0);
            crawlAPITweet(param, tweetID, account);
        } else {
            crawlUserViaAPI(param, account);
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Could not find any media, decrypter might be broken");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

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

    private void crawlAPITweet(final CryptedLink param, final String tweetID, final Account account) throws Exception {
        logger.info("Crawling API tweet");
        final FilePackage fp = FilePackage.getInstance();
        prepareAPI(this.br, account);
        final boolean useNewMethod = true; /* 2021-06-15 */
        if (useNewMethod) {
            br.getPage("https://api.twitter.com/1.1/statuses/show/" + tweetID + ".json?cards_platform=Web-12&include_reply_count=1&include_cards=1&include_user_entities=0&tweet_mode=extended");
            if (br.getHttpConnection().getResponseCode() == 429) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Rate-limit reached", 5 * 60 * 1000l);
            }
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> user = (Map<String, Object>) entries.get("user");
            final String username = (String) user.get("screen_name");
            final String formattedDate = formatTwitterDate((String) entries.get("created_at"));
            final String description = (String) entries.get("full_text");
            fp.setName(formattedDate + "_" + username);
            if (!StringUtils.isEmpty(description)) {
                fp.setComment(description);
            }
            final List<Map<String, Object>> medias = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "extended_entities/media");
            for (final Map<String, Object> media : medias) {
                final String type = (String) media.get("type");
                if (type.equals("video")) {
                    /* Find highest video quality */
                    int highestBitrate = -1;
                    final List<Map<String, Object>> videoVariants = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(media, "video_info/variants");
                    String streamURL = null;
                    for (final Map<String, Object> videoVariant : videoVariants) {
                        final String content_type = (String) videoVariant.get("content_type");
                        if (!content_type.equalsIgnoreCase("video/mp4")) {
                            /* Skip all except http videos (e.g. HLS --> application/x-mpegURL) */
                            continue;
                        }
                        final int bitrate = ((Number) videoVariant.get("bitrate")).intValue();
                        if (bitrate > highestBitrate) {
                            highestBitrate = bitrate;
                            streamURL = (String) videoVariant.get("url");
                        }
                    }
                    final DownloadLink dl = this.createDownloadlink(streamURL);
                    dl.setForcedFileName(formattedDate + "_" + username + "_" + tweetID + ".mp4");
                    dl.setAvailable(true);
                    dl.setProperty(PROPERTY_USERNAME, username);
                    dl.setProperty("bitrate", highestBitrate);
                    this.decryptedLinks.add(dl);
                } else if (type.equals("photo")) {
                    final String url = (String) media.get("media_url"); /* Also available as "media_url_https" */
                    final DownloadLink dl = this.createDownloadlink(url);
                    dl.setForcedFileName(formattedDate + "_" + username + "_" + tweetID + Plugin.getFileNameExtensionFromString(url));
                    dl.setAvailable(true);
                    dl.setProperty(PROPERTY_USERNAME, username);
                    this.decryptedLinks.add(dl);
                } else {
                    /* Unknown type -> This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else {
            br.getPage("https://api.twitter.com/2/timeline/conversation/" + tweetID + ".json?include_profile_interstitial_type=1&include_blocking=1&include_blocked_by=1&include_followed_by=1&include_want_retweets=1&include_mute_edge=1&include_can_dm=1&include_can_media_tag=1&skip_status=1&cards_platform=Web-12&include_cards=1&include_composer_source=true&include_ext_alt_text=true&include_reply_count=1&tweet_mode=extended&include_entities=true&include_user_entities=true&include_ext_media_color=true&include_ext_media_availability=true&send_error_codes=true&simple_quoted_tweets=true&count=20&ext=mediaStats%2CcameraMoment");
            Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "globalObjects/tweets/" + tweetID);
            crawlTweetMediaObjectsAPI(fp, null, entries);
        }
    }

    public static Browser prepAPIHeaders(final Browser br) {
        br.getHeaders().put("Authorization", "Bearer AAAAAAAAAAAAAAAAAAAAANRILgAAAAAAnNwIzUejRCOuH5E6I8xnZz4puTs%3D1Zv7ttfk8LF81IUq16cHjhLTvJu4FA33AGWWjCpTnA");
        final String csrftoken = br.getCookie("twitter.com", jd.plugins.hoster.TwitterCom.COOKIE_KEY_LOGINED_CSRFTOKEN, Cookies.NOTDELETEDPATTERN);
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

    private Browser prepareAPI(final Browser br, final Account account) throws DecrypterException, IOException {
        /* 2020-02-03: Static authtoken */
        prepAPIHeaders(br);
        if (account == null) {
            /* Gues token is only needed for anonymous users */
            getAndSetGuestToken(br);
        }
        return br;
    }

    private void getAndSetGuestToken(final Browser br) throws DecrypterException, IOException {
        synchronized (LOCK) {
            if (guest_token == null) {
                logger.info("Generating new guest_token");
                /** TODO: Save guest_token throughout session so we do not generate them so frequently */
                guest_token = generateNewGuestToken(br);
                if (StringUtils.isEmpty(guest_token)) {
                    logger.warning("Failed to find guest_token");
                    throw new DecrypterException("Plugin broken");
                }
            } else {
                logger.info("Re-using existing guest-token");
            }
            br.getHeaders().put("x-guest-token", guest_token);
        }
    }

    public static String generateNewGuestToken(final Browser br) throws IOException, DecrypterException {
        br.postPage("https://api.twitter.com/1.1/guest/activate.json", "");
        /** TODO: Save guest_token throughout session so we do not generate them so frequently */
        return PluginJSonUtils.getJson(br, "guest_token");
    }

    /** Crawls single media objects obtained via API. */
    private void crawlTweetMediaObjectsAPI(final FilePackage fp, final String username, Map<String, Object> entries) {
        final String tweetID = (String) entries.get("id_str");
        final String formattedDate = formatTwitterDate((String) entries.get("created_at"));
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "entities/media");
        if (ressourcelist == null) {
            logger.info("Current tweet does not contain any media objects");
            return;
        } else if (StringUtils.isEmpty(tweetID)) {
            logger.warning("Failed to find tweet_id");
            return;
        }
        logger.info(String.format("Found %d media objects", ressourcelist.size()));
        for (final Object mediaO : ressourcelist) {
            /* TODO: Check what happens when there is more than one video in a single tweet. */
            entries = (Map<String, Object>) mediaO;
            crawlMediaObjectAPI(fp, username, tweetID, formattedDate, entries);
        }
    }

    private static String formatTwitterDate(String created_at) {
        if (created_at == null) {
            return null;
        }
        try {
            created_at = created_at.substring(created_at.indexOf(" ") + 1, created_at.length());
            final String targetFormat = "yyyy-MM-dd";
            final long date = TimeFormatter.getMilliSeconds(created_at, "MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
            if (date == -1) {
                throw new Exception("TimeFormatter failed for:" + created_at);
            }
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            return formatter.format(new Date(date));
        } catch (final Throwable e) {
            /* Fallback */
            return created_at;
        }
    }

    /** Crawls single media objects obtained via API. */
    private void crawlMediaObjectAPI(final FilePackage fp, final String username, final String tweetID, final String formattedDate, final Map<String, Object> entries) {
        String url = (String) entries.get("media_url_https");
        final String expanded_url = (String) entries.get("expanded_url");
        if (StringUtils.isEmpty(url)) {
            /* Ignore invalid items */
            return;
        }
        /* 2020-02-10: Recognize videos by this URL. If it is a thumbnail --< It is a video */
        if (url.contains("/tweet_video_thumb/") || url.contains("/amplify_video_thumb/") || url.contains("/ext_tw_video_thumb/") || StringUtils.contains(expanded_url, "/video/")) {
            /* Video --> Needs to go into crawler again */
            final DownloadLink dl = this.createDownloadlink(this.createTwitterPostURL(username, tweetID));
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            decryptedLinks.add(dl);
            distribute(dl);
        } else {
            /* Photo */
            String filename = null;
            try {
                filename = Plugin.getFileNameFromURL(new URL(url));
                if (filename != null) {
                    filename = tweetID + "_" + filename;
                }
            } catch (final Throwable e) {
            }
            if (!url.contains("?name=")) {
                url += "?name=orig";
            }
            final DownloadLink dl = this.createDownloadlink(url);
            if (filename != null) {
                // dl.setFinalFileName(filename);
                /* 2020-06-08: Let it survive users' reset especially for items which are handled by directhttp plugin. */
                dl.setForcedFileName(formattedDate + "_" + username + "_" + filename);
            }
            /* Set possible Packagizer properties */
            dl.setProperty("date", formattedDate);
            dl.setAvailable(true);
            if (fp != null) {
                fp.add(dl);
            }
            decryptedLinks.add(dl);
            distribute(dl);
        }
    }

    @Deprecated
    private void crawlTweetViaMobileWebsite(final String parameter, final FilePackage fp) throws IOException {
        logger.info("Crawling mobile website tweet");
        final String tweet_id = new Regex(parameter, "/(?:tweet|status)/(\\d+)").getMatch(0);
        if (br.containsHTML("/status/" + tweet_id + "/video/1")) {
            /* Video */
            final DownloadLink dl = createDownloadlink(createVideourl(tweet_id));
            decryptedLinks.add(dl);
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            distribute(dl);
        } else if (br.containsHTML("/tweet_video_thumb/")) {
            /* TODO: Check what happens if there is a video/gif + pictures in one post. */
            /* .gif --> Can be downloaded as .mp4 video */
            final DownloadLink dl = createDownloadlink(createVideourl(tweet_id));
            decryptedLinks.add(dl);
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            distribute(dl);
        } else {
            /* Picture or text */
            /* 2020-01-30: TODO: .gif download might fail: /Lin_Manuel/status/1208019162657906690 */
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
                        dl.setProperty("decryptedfilename", final_filename);
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
                decryptedLinks.add(this.createOfflinelink(parameter));
            }
        }
    }

    private void crawlUserViaAPI(final CryptedLink param, final Account account) throws Exception {
        logger.info("Crawling API user");
        final String username = new Regex(param.getCryptedUrl(), "https?://[^/]+/([^/]+)").getMatch(0);
        this.prepareAPI(br, account);
        final boolean use_old_api_to_get_userid = true;
        Map<String, Object> entries;
        final String user_id;
        /* = numberof tweets */
        String statuses_count = null;
        /* = number of media tweets (NOT total numberof items - this may even be higher! A tweet can e.g. contain multiple photos!) */
        String media_count = null;
        if (use_old_api_to_get_userid) {
            /* https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-users-show */
            /* https://developer.twitter.com/en/docs/twitter-api/rate-limits */
            /* per 15 mins window, 300 per app, 900 per user */
            br.getPage("https://api.twitter.com/1.1/users/lookup.json?screen_name=" + username);
            user_id = PluginJSonUtils.getJson(br, "id_str");
            statuses_count = PluginJSonUtils.getJson(br, "statuses_count");
            media_count = PluginJSonUtils.getJson(br, "media_count");
        } else {
            br.getPage("https://api.twitter.com/graphql/DO_NOT_USE_ATM_2020_02_05/UserByScreenName?variables=%7B%22screen_name%22%3A%22" + username + "%22%2C%22withHighlightedLabel%22%3Afalse%7D");
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/user");
            user_id = (String) entries.get("rest_id");
        }
        if (StringUtils.isEmpty(user_id)) {
            logger.warning("Failed to find user_id");
            throw new DecrypterException("Decrypter broken");
        }
        final boolean setting_force_grab_media = PluginJsonConfig.get(jd.plugins.hoster.TwitterCom.TwitterConfigInterface.class).isForceGrabMediaOnlyEnabled();
        /* Grab only content posted by user or grab everything from his timeline e.g. also re-tweets. */
        final String content_type;
        String max_countStr;
        int index = 0;
        final int expected_items_per_page = 20;
        int numberof_items_on_current_page = 0;
        String nextCursor = null;
        final UrlQuery query = new UrlQuery();
        query.append("include_profile_interstitial_type", "1", false);
        query.append("include_blocking", "1", false);
        query.append("include_blocked_by", "1", false);
        query.append("include_followed_by", "1", false);
        query.append("include_want_retweets", "1", false);
        query.append("include_mute_edge", "1", false);
        query.append("include_can_dm", "1", false);
        query.append("include_can_media_tag", "1", false);
        query.append("skip_status", "1", false);
        query.append("cards_platform", "Web-12", false);
        query.append("include_cards", "1", false);
        /* 2020-08-24: Not required anymore */
        // query.append("include_composer_source", "true", false);
        query.append("include_quote_count", "true", false);
        query.append("include_ext_alt_text", "true", false);
        query.append("include_reply_count", "1", false);
        query.append("tweet_mode", "extended", false);
        query.append("include_entities", "true", false);
        query.append("include_user_entities", "true", false);
        query.append("include_ext_media_color", "true", false);
        query.append("include_ext_media_availability", "true", false);
        query.append("send_error_codes", "true", false);
        query.append("simple_quoted_tweet", "true", false);
        final boolean isGrabLikedTweetsOfUser = param.getCryptedUrl().endsWith("/likes");
        final boolean isGrabMediaFromOriginalPosterOnly = param.getCryptedUrl().endsWith("/media");
        String fpname = username;
        if (isGrabLikedTweetsOfUser) {
            /* 2020-08-24: Most likely an account is required to do this! */
            logger.info("Grabbing all liked items of a user");
            content_type = "favorites";
            max_countStr = PluginJSonUtils.getJson(br, "favourites_count");
            query.append("simple_quoted_tweets", "true", false);
            query.append("sorted_by_time", "true", false);
            fpname += " - likes";
        } else if (isGrabMediaFromOriginalPosterOnly || setting_force_grab_media) {
            logger.info("Grabbing self posted media only");
            content_type = "media";
            max_countStr = media_count;
        } else {
            logger.info("Grabbing ALL media of a user e.g. also retweets");
            content_type = "profile";
            max_countStr = statuses_count;
            query.append("include_tweet_replies", "false", false);
        }
        if (StringUtils.isEmpty(max_countStr)) {
            /* This should never happen */
            max_countStr = "??";
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpname);
        query.append("userId", user_id, false);
        query.append("count", expected_items_per_page + "", false);
        query.append("ext", "mediaStats,cameraMoment", true);
        int crawled_tweet_count = 0;
        do {
            logger.info("Crawling page " + (index + 1));
            numberof_items_on_current_page = 0;
            final UrlQuery thisquery = query;
            if (!StringUtils.isEmpty(nextCursor)) {
                thisquery.append("cursor", nextCursor, true);
            }
            final String url = String.format("https://api.twitter.com/2/timeline/%s/%s.json", content_type, user_id);
            br.getPage(url + "?" + thisquery.toString());
            if (br.containsHTML("Your credentials do not allow access to this resource")) {
                /* 2020-08-24: {"errors":[{"code":220,"message":"Your credentials do not allow access to this resource."}]} */
                throw new AccountRequiredException();
            }
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Object errors = entries.get("errors");
            if (errors != null) {
                logger.info("Twitter error happened - probably offline- or protected content");
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
                return;
            }
            final ArrayList<Object> pagination_info = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "timeline/instructions/{0}/addEntries/entries");
            final Map<String, Object> tweetMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "globalObjects/tweets");
            final Iterator<Entry<String, Object>> iterator = tweetMap.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                entries = (Map<String, Object>) entry.getValue();
                crawlTweetMediaObjectsAPI(fp, username, entries);
                numberof_items_on_current_page++;
                crawled_tweet_count++;
            }
            logger.info(String.format("Numberof tweets on current page: %d of expected max %d", numberof_items_on_current_page, expected_items_per_page));
            logger.info(String.format("Numberof total tweets crawled: %d of expected total %s", crawled_tweet_count, max_countStr));
            if (numberof_items_on_current_page == 0) {
                logger.info("Found 0 tweets on current page --> Stopping");
                break;
            } else if (numberof_items_on_current_page < expected_items_per_page) {
                logger.info(String.format("Warning: Page contains less than %d objects --> Reached the end?", expected_items_per_page));
            }
            /* Done - now try to find string required to access next page */
            try {
                Map<String, Object> pagination_info_entries = (Map<String, Object>) pagination_info.get(pagination_info.size() - 1);
                final String entryId = (String) pagination_info_entries.get("entryId");
                if (entryId.contains("cursor-bottom")) {
                    logger.info("Found correct cursor object --> Trying to get cursor String");
                    final String nextCursorTmp = (String) JavaScriptEngineFactory.walkJson(pagination_info_entries, "content/operation/cursor/value");
                    logger.info("nextCursor = " + nextCursor);
                    if (nextCursor != null && nextCursor.equals(nextCursorTmp)) {
                        /* Extra fallback - this should never be required */
                        logger.info("New nextCursor is the same as last nextCursor --> Reached the end?!");
                        break;
                    }
                    nextCursor = nextCursorTmp;
                } else {
                    logger.info("Found wrong cursor object --> Plugin needs update");
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                logger.info("Failed to get nextCursor");
            }
            index++;
            this.sleep(3000l, param);
        } while (!StringUtils.isEmpty(nextCursor) && !this.isAbort());
        logger.info(String.format("Done after %d pages", index));
    }

    /* 2020-01-30: Mobile website will only show 1 tweet per page */
    @Deprecated
    private void crawlUserViaMobileWebsite(final String parameter) throws IOException {
        logger.info("Crawling mobile website user");
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return;
        } else if (!this.switchtoMobile()) {
            logger.warning("Unable to crawl: Failed to switch to mobile website");
            return;
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
                crawlTweetViaMobileWebsite(tweet_url, fp);
            }
            index++;
            nextURL = br.getRegex("(/[^/]+/media/grid\\?idx=" + index + ")").getMatch(0);
        } while (nextURL != null && !this.isAbort());
        logger.info(String.format("Done after %d pages", index));
    }

    public static Map<String, Object> getPlayerData(final Browser br) {
        Map<String, Object> entries = null;
        try {
            String json_source = br.getRegex("<div id=\"playerContainer\"[^<>]*?data\\-config=\"([^<>]+)\" >").getMatch(0);
            json_source = Encoding.htmlDecode(json_source);
            entries = JavaScriptEngineFactory.jsonToJavaMap(json_source);
        } catch (final Throwable e) {
        }
        return entries;
    }

    protected void getPage(final Browser br, final String url) throws Exception {
        super.getPage(br, url);
        if (br.getHttpConnection().getResponseCode() == 429) {
            logger.info("Error 429 too many requests - add less URLs and/or perform a reconnect!");
        }
    }

    protected void getPage(final String url) throws Exception {
        getPage(br, url);
    }

    private String createVideourl(final String stream_id) {
        return String.format("https://twitter.com/i/videos/tweet/%s", stream_id);
    }

    private String createTwitterPostURL(final String user, final String tweetID) {
        return "https://twitter.com/" + user + "/status/" + tweetID;
    }

    private String getUrlFname(final String parameter) {
        String urlfilename;
        if (parameter.matches(TYPE_USER_ALL)) {
            urlfilename = new Regex(parameter, "twitter\\.com/([^/]+)").getMatch(0);
        } else {
            urlfilename = new Regex(parameter, "twitter\\.com/status/(\\d+)").getMatch(0);
        }
        return urlfilename;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings({ "static-access" })
    private Account getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("twitter.com");
        final Account aa = AccountController.getInstance().getValidAccount("twitter.com");
        if (aa == null) {
            return null;
        }
        try {
            ((jd.plugins.hoster.TwitterCom) hostPlugin).login(br, aa, force);
            return aa;
        } catch (final PluginException e) {
            return null;
        }
    }

    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-30: We have to perform a lot of requests --> Set this to 1. */
        return 1;
    }
}
