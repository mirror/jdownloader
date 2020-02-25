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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "twitter.com", "t.co" }, urls = { "https?://(?:www\\.|mobile\\.)?twitter\\.com/[A-Za-z0-9_\\-]+/status/\\d+|https?://(?:www\\.|mobile\\.)?twitter\\.com/(?!i/)[A-Za-z0-9_\\-]{2,}(?:/media)?|https://twitter\\.com/i/cards/tfw/v1/\\d+|https?://(?:www\\.)?twitter\\.com/i/videos/tweet/\\d+", "https?://t\\.co/[a-zA-Z0-9]+" })
public class TwitterCom extends PornEmbedParser {
    public TwitterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String     TYPE_CARD      = "https?://(?:www\\.)?twitter\\.com/i/cards/tfw/v1/\\d+";
    private static final String     TYPE_USER_ALL  = "https?://(?:www\\.)?twitter\\.com/[A-Za-z0-9_\\-]+(?:/media)?";
    private static final String     TYPE_USER_POST = "https?://(?:www\\.)?twitter\\.com.*?status/\\d+.*?";
    private static final String     TYPE_REDIRECT  = "https?://t\\.co/[a-zA-Z0-9]+";
    private String                  username       = null;
    private FilePackage             fp             = null;
    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private static Object           LOCK           = new Object();
    private static String           guest_token    = null;

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
        username = new Regex(parameter, "https?://[^/]+/([A-Za-z0-9_\\-]+)").getMatch(0);
        final FilePackage fp;
        if ("i".equals(username)) {
            fp = null;
        } else {
            fp = FilePackage.getInstance();
            fp.setName(username);
        }
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
        /* Some profiles can only be accessed if they accepted others as followers --> Log in if the user has added his twitter account */
        if (getUserLogin(false)) {
            logger.info("Account available and we're logged in");
        } else {
            logger.info("No account available or login failed");
        }
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
        if (parameter.matches(TYPE_CARD)) {
            tweet_id = new Regex(parameter, "(\\d+)$").getMatch(0);
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
                if (fp != null) {
                    fp.add(dl);
                }
                dl.setProperty("decryptedfilename", filename);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else if (parameter.matches(jd.plugins.hoster.TwitterCom.TYPE_VIDEO_EMBED)) {
            LinkedHashMap<String, Object> entries = getPlayerData(br);
            String sourcetype = null;
            if (entries != null) {
                sourcetype = (String) entries.get("source_type");
            }
            // if (entries == null) {
            // /* 2018-11-13: Probably offline */
            // // decryptedLinks.add(this.createOfflinelink(parameter));
            // // return decryptedLinks;
            // tweet_id = new Regex(parameter, "/tweet/(\\d+)$").getMatch(0);
            // if (tweet_id == null) {
            // return null;
            // }
            // br.getHeaders().put("Authorization", "Bearer
            // AAAAAAAAAAAAAAAAAAAAAIK1zgAAAAAA2tUWuhGZ2JceoId5GwYWU5GspY4%3DUq7gzFoCZs1QfwGoVdvSac3IniczZEYXIcDyumCauIXpcAPorE");
            // br.getPage("https://api.twitter.com/1.1/videos/tweet/config/" + tweet_id + ".json");
            // if (br.containsHTML("<div id=\"message\">")) {
            // /* E.g. <div id="message">Das Medium konnte nicht abgespielt werden. */
            // decryptedLinks.add(this.createOfflinelink(parameter));
            // return decryptedLinks;
            // }
            // entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            // entries = (LinkedHashMap<String, Object>) entries.get("track");
            // }
            /* TODO: Fix recognization of embedded content */
            // if (sourcetype.equals("consumer") || sourcetype.equals("gif") || sourcetype.equals("amplify")) {
            if (true) {
                /* Video uploaded by user, hosted on Twitter --> Download via Twitter hosterplugin */
                decryptedLinks.add(this.createDownloadlink(parameter));
            } else {
                /* E.g. embedded Vine.co video */
                final String url_extern = (String) entries.get("player_url");
                if (StringUtils.isEmpty(url_extern)) {
                    return null;
                }
                decryptedLinks.add(this.createDownloadlink(url_extern));
            }
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
            crawlAPITweet(parameter, null);
        } else {
            crawlUserViaAPI(parameter);
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

    private void crawlAPITweet(final String parameter, final FilePackage fp) throws Exception {
        logger.info("Crawling API tweet");
        final String tweet_id = new Regex(parameter, "/(?:tweet|status)/(\\d+)").getMatch(0);
        prepareAPI(this.br);
        br.getPage("https://api.twitter.com/2/timeline/conversation/" + tweet_id + ".json?include_profile_interstitial_type=1&include_blocking=1&include_blocked_by=1&include_followed_by=1&include_want_retweets=1&include_mute_edge=1&include_can_dm=1&include_can_media_tag=1&skip_status=1&cards_platform=Web-12&include_cards=1&include_composer_source=true&include_ext_alt_text=true&include_reply_count=1&tweet_mode=extended&include_entities=true&include_user_entities=true&include_ext_media_color=true&include_ext_media_availability=true&send_error_codes=true&simple_quoted_tweets=true&count=20&ext=mediaStats%2CcameraMoment");
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "globalObjects/tweets/" + tweet_id);
        crawlTweetMediaObjectsAPI(entries);
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

    private Browser prepareAPI(final Browser br) throws DecrypterException, IOException {
        /* 2020-02-03: Static authtoken */
        prepAPIHeaders(br);
        getAndSetGuestToken(br);
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
    private void crawlTweetMediaObjectsAPI(LinkedHashMap<String, Object> entries) {
        final String tweet_id = (String) entries.get("id_str");
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "entities/media");
        if (ressourcelist == null) {
            logger.info("Current tweet does not contain any media objects");
            return;
        } else if (StringUtils.isEmpty(tweet_id)) {
            logger.warning("Failed to find tweet_id");
            return;
        }
        logger.info(String.format("Found %d media objects", ressourcelist.size()));
        for (final Object mediaO : ressourcelist) {
            /* TODO: Check what happens when there is more than one video in a single tweet. */
            entries = (LinkedHashMap<String, Object>) mediaO;
            crawlMediaObjectAPI(tweet_id, entries);
        }
    }

    /** Crawls single media objects obtained via API. */
    private void crawlMediaObjectAPI(final String tweet_id, final LinkedHashMap<String, Object> entries) {
        String url = (String) entries.get("media_url_https");
        if (StringUtils.isEmpty(url)) {
            /* Ignore invalid items */
            return;
        }
        String filename = null;
        final DownloadLink dl;
        /* 2020-02-10: Recognize videos by this URL. If it is a thumbnail --< It is a video */
        if (url.contains("/tweet_video_thumb/") || url.contains("/amplify_video_thumb/") || url.contains("/ext_tw_video_thumb/")) {
            /* Video thumbnail --> Download video */
            dl = this.createDownloadlink(this.createVideourl(tweet_id));
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        } else {
            /* Photo */
            try {
                filename = Plugin.getFileNameFromURL(new URL(url));
                if (filename != null) {
                    filename = tweet_id + "_" + filename;
                }
            } catch (final Throwable e) {
            }
            if (!url.contains("?name=")) {
                url += "?name=orig";
            }
            dl = this.createDownloadlink("directhttp://" + url);
        }
        if (filename != null) {
            dl.setFinalFileName(filename);
        }
        dl.setAvailable(true);
        decryptedLinks.add(dl);
        distribute(dl);
    }

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

    private void crawlUserViaAPI(final String parameter) throws Exception {
        logger.info("Crawling API user");
        final String username = new Regex(parameter, "https?://[^/]+/([^/]+)").getMatch(0);
        this.prepareAPI(br);
        final boolean use_old_api_to_get_userid = true;
        LinkedHashMap<String, Object> entries;
        final String user_id;
        if (use_old_api_to_get_userid) {
            /* https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-users-show */
            br.getPage("https://api.twitter.com/1.1/users/lookup.json?screen_name=" + username);
            user_id = PluginJSonUtils.getJson(br, "id_str");
        } else {
            br.getPage("https://api.twitter.com/graphql/DO_NOT_USE_ATM_2020_02_05/UserByScreenName?variables=%7B%22screen_name%22%3A%22" + username + "%22%2C%22withHighlightedLabel%22%3Afalse%7D");
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/user");
            user_id = (String) entries.get("rest_id");
        }
        if (StringUtils.isEmpty(user_id)) {
            logger.warning("Failed to find user_id");
            throw new DecrypterException("Decrypter broken");
        }
        /* Grab only content posted by user or grab everything from his timeline e.g. also re-tweets. */
        final boolean isMediaFromOriginalPosterOnly = parameter.endsWith("/media");
        int index = 0;
        fp = FilePackage.getInstance();
        fp.setName(username);
        final int items_per_page = 20;
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
        query.append("include_composer_source", "true", false);
        query.append("include_ext_alt_text", "true", false);
        query.append("include_reply_count", "1", false);
        query.append("tweet_mode", "extended", false);
        query.append("include_entities", "true", false);
        query.append("include_user_entities", "true", false);
        query.append("include_ext_media_color", "true", false);
        query.append("include_ext_media_availability", "true", false);
        query.append("send_error_codes", "true", false);
        query.append("simple_quoted_tweets", "true", false);
        if (isMediaFromOriginalPosterOnly) {
        } else {
            query.append("include_tweet_replies", "false", false);
        }
        query.append("userId", user_id, false);
        query.append("count", items_per_page + "", false);
        query.append("ext", "mediaStats,cameraMoment", true);
        /* TODO: 2020-02-05: Check for rate-limit and add waittime- and retry for this case! */
        do {
            logger.info("Crawling page " + (index + 1));
            numberof_items_on_current_page = 0;
            final UrlQuery thisquery = query;
            if (!StringUtils.isEmpty(nextCursor)) {
                thisquery.append("cursor", nextCursor, true);
            }
            br.getPage(String.format("https://api.twitter.com/2/timeline/profile/%s.json?", user_id) + thisquery.toString());
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final Object errors = entries.get("errors");
            if (errors != null) {
                logger.info("Twitter error happened - probably offline- or protected content");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return;
            }
            final ArrayList<Object> pagination_info = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "timeline/instructions/{0}/addEntries/entries");
            final LinkedHashMap<String, Object> tweetMap = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "globalObjects/tweets");
            final Iterator<Entry<String, Object>> iterator = tweetMap.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                entries = (LinkedHashMap<String, Object>) entry.getValue();
                crawlTweetMediaObjectsAPI(entries);
                numberof_items_on_current_page++;
            }
            logger.info(String.format("Numberof tweets on current page: %d of expected max %d", numberof_items_on_current_page, items_per_page));
            /* Done - now try to find string required to access next page */
            try {
                LinkedHashMap<String, Object> pagination_info_entries = (LinkedHashMap<String, Object>) pagination_info.get(pagination_info.size() - 1);
                final String entryId = (String) pagination_info_entries.get("entryId");
                if (entryId.contains("cursor-bottom")) {
                    logger.info("Found correct cursor object --> Trying to get cursor String");
                    nextCursor = (String) JavaScriptEngineFactory.walkJson(pagination_info_entries, "content/operation/cursor/value");
                } else {
                    logger.info("Found wrong cursor object --> Plugin needs update");
                }
            } catch (final Throwable e) {
                e.printStackTrace();
                logger.info("Failed to get nextCursor");
            }
            index++;
            this.sleep(3000l, param);
        } while (!StringUtils.isEmpty(nextCursor) && !this.isAbort() && numberof_items_on_current_page >= items_per_page);
        logger.info(String.format("Done after %d pages", index));
    }

    /* 2020-01-30: Mobile website will only show 1 tweet per page */
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

    @Override
    public DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = super.createDownloadlink(url);
        if (this.username != null && !"i".equalsIgnoreCase(this.username)) {
            /* This can e.g. be used via packagizer */
            dl.setProperty("username", this.username);
        }
        if (fp != null) {
            dl._setFilePackage(fp);
        }
        return dl;
    }

    public static LinkedHashMap<String, Object> getPlayerData(final Browser br) {
        LinkedHashMap<String, Object> entries = null;
        try {
            String json_source = br.getRegex("<div id=\"playerContainer\"[^<>]*?data\\-config=\"([^<>]+)\" >").getMatch(0);
            json_source = Encoding.htmlDecode(json_source);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
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
    @SuppressWarnings({ "deprecation", "static-access" })
    private boolean getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("twitter.com");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) {
            return false;
        }
        try {
            ((jd.plugins.hoster.TwitterCom) hostPlugin).login(br, aa, force);
        } catch (final PluginException e) {
            return false;
        }
        return true;
    }

    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-30: We have to perform a lot of requests --> Set this to 1. */
        return 1;
    }
}
