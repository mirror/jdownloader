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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
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
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "twitter.com", "t.co" }, urls = { "https?://(?:www\\.|mobile\\.)?twitter\\.com/[A-Za-z0-9_\\-]+/status/\\d+|https?://(?:www\\.|mobile\\.)?twitter\\.com/(?!i/)[A-Za-z0-9_\\-]{2,}(?:/(?:media|likes))?(\\?.*)?|https://twitter\\.com/i/cards/tfw/v1/\\d+", "https?://t\\.co/[a-zA-Z0-9]+" })
public class TwitterCom extends PornEmbedParser {
    public TwitterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String            TYPE_CARD                      = "https?://[^/]+/i/cards/tfw/v1/(\\d+)";
    private static final String            TYPE_USER_ALL                  = "https?://[^/]+/([A-Za-z0-9_\\-]+)(?:/(?:media|likes))?(\\?.*)?";
    private static final String            TYPE_USER_POST                 = "https?://[^/]+/([^/]+)/status/(\\d+).*?";
    private static final String            TYPE_REDIRECT                  = "https?://t\\.co/[a-zA-Z0-9]+";
    private ArrayList<DownloadLink>        decryptedLinks                 = new ArrayList<DownloadLink>();
    private static AtomicReference<String> GUEST_TOKEN                    = new AtomicReference<String>();
    private static AtomicLong              GUEST_TOKEN_TS                 = new AtomicLong(-1);
    public static final String             PROPERTY_USERNAME              = "username";
    private static final String            PROPERTY_DATE                  = "date";
    public static final String             PROPERTY_MEDIA_INDEX           = "mediaindex";
    public static final String             PROPERTY_MEDIA_ID              = "mediaid";
    public static final String             PROPERTY_BITRATE               = "bitrate";
    public static final String             PROPERTY_POST_TEXT             = "post_text";
    public static final String             PROPERTY_FILENAME_FROM_CRAWLER = "crawlerfilename";

    protected DownloadLink createDownloadlink(final String link, final String tweetid) {
        final DownloadLink ret = super.createDownloadlink(link);
        ret.setProperty("tweetid", tweetid);
        return ret;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setAllowedResponseCodes(new int[] { 429 });
        final String parameter = param.toString().replaceAll("https?://(www\\.|mobile\\.)?twitter\\.com/", "https://twitter.com/");
        String tweet_id = null;
        if (parameter.matches(TYPE_REDIRECT)) {
            br.setFollowRedirects(false);
            getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;URL=(https?[^<>\"]*?)(#_=_)?\"").getMatch(0);
            }
            if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.getRequest().getHttpConnection().getResponseCode() == 404 || finallink == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("class=\"ProtectedTimeline\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
                dl.setProperty(PROPERTY_FILENAME_FROM_CRAWLER, filename);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else if (parameter.matches(jd.plugins.hoster.TwitterCom.TYPE_VIDEO_EMBED)) {
            /* 2021-06-22: Support for those has been removed in crawler but is still required in host plugin so let's leave it here! */
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
            final String tweetID = new Regex(param.getCryptedUrl(), TYPE_USER_POST).getMatch(1);
            crawlAPITweet(param, tweetID, account);
        } else {
            crawlUserViaAPI(param, account);
        }
        if (decryptedLinks.size() == 0) {
            logger.info("Could not find any media, crawler might be broken");
            return decryptedLinks;
        }
        return decryptedLinks;
    }

    @Override
    public void init() {
        super.init();
        Browser.setRequestIntervalLimitGlobal("twimg.com", true, 500);
        Browser.setRequestIntervalLimitGlobal("api.twitter.com", true, 500);
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

    private void crawlAPITweet(final CryptedLink param, final String tweetID, final Account account) throws Exception {
        logger.info("Crawling API tweet");
        final FilePackage fp = FilePackage.getInstance();
        prepareAPI(this.br, account);
        final boolean useNewMethod = true; /* 2021-06-15 */
        if (useNewMethod) {
            br.getPage("https://api.twitter.com/1.1/statuses/show/" + tweetID + ".json?cards_platform=Web-12&include_reply_count=1&include_cards=1&include_user_entities=0&tweet_mode=extended");
            handleErrorsAPI(this.br);
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final Map<String, Object> user = (Map<String, Object>) entries.get("user");
            final String username = (String) user.get("screen_name");
            final String formattedDate = formatTwitterDate((String) entries.get("created_at"));
            final String postText = (String) entries.get("full_text");
            fp.setName(formattedDate + "_" + username);
            if (!StringUtils.isEmpty(postText)) {
                fp.setComment(postText);
            }
            final boolean useOriginalFilenames = PluginJsonConfig.get(jd.plugins.hoster.TwitterCom.TwitterConfigInterface.class).isUseOriginalFilenames();
            final List<Map<String, Object>> medias = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "extended_entities/media");
            int mediaIndex = 0;
            for (final Map<String, Object> media : medias) {
                final String type = (String) media.get("type");
                try {
                    final DownloadLink dl;
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
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        dl = this.createDownloadlink(createVideourl(tweetID));
                        if (useOriginalFilenames) {
                            dl.setFinalFileName(tweetID + "_" + Plugin.getFileNameFromURL(new URL(streamURL)));
                        } else {
                            dl.setFinalFileName(formattedDate + "_" + username + "_" + tweetID + "_" + mediaIndex + ".mp4");
                        }
                        dl.setProperty(PROPERTY_BITRATE, highestBitrate);
                        dl.setProperty(jd.plugins.hoster.TwitterCom.PROPERTY_DIRECTURL, streamURL);
                        if (!StringUtils.isEmpty(hlsMaster)) {
                            dl.setProperty(jd.plugins.hoster.TwitterCom.PROPERTY_DIRECTURL_hls_master, hlsMaster);
                        }
                    } else if (type.equals("photo")) {
                        final String url = (String) media.get("media_url"); /* Also available as "media_url_https" */
                        if (url == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        dl = this.createDownloadlink(url);
                        if (useOriginalFilenames) {
                            dl.setFinalFileName(tweetID + "_" + Plugin.getFileNameFromURL(new URL(url)));
                        } else {
                            final String filename = formattedDate + "_" + username + "_" + tweetID + "_" + mediaIndex + Plugin.getFileNameExtensionFromURL(url);
                            dl.setFinalFileName(filename);
                        }
                    } else {
                        /* Unknown type -> This should never happen! */
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown type:" + type);
                    }
                    dl.setAvailable(true);
                    dl.setProperty(PROPERTY_USERNAME, username);
                    dl.setProperty(PROPERTY_DATE, formattedDate);
                    dl.setProperty(PROPERTY_MEDIA_INDEX, mediaIndex);
                    dl.setProperty(PROPERTY_MEDIA_ID, media.get("id_str").toString());
                    if (!StringUtils.isEmpty(postText)) {
                        dl.setProperty(PROPERTY_POST_TEXT, postText);
                    }
                    if (dl.getFinalFileName() != null) {
                        dl.setProperty(PROPERTY_FILENAME_FROM_CRAWLER, dl.getFinalFileName());
                    }
                    if (fp != null) {
                        fp.add(dl);
                    }
                    this.decryptedLinks.add(dl);
                    distribute(dl);
                } catch (PluginException e) {
                    logger.log(e);
                }
                mediaIndex += 1;
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

    /**
     * Crawls single media objects obtained via API.
     *
     * @throws MalformedURLException
     */
    private void crawlTweetMediaObjectsAPI(final FilePackage fp, final String username, Map<String, Object> entries) throws MalformedURLException {
        final boolean useOriginalFilenames = PluginJsonConfig.get(jd.plugins.hoster.TwitterCom.TwitterConfigInterface.class).isUseOriginalFilenames();
        final String tweetID = (String) entries.get("id_str");
        final String formattedDate = formatTwitterDate((String) entries.get("created_at"));
        final List<Map<String, Object>> ressourcelist = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "entities/media");
        if (ressourcelist == null) {
            logger.info("This tweet does not contain any media objects: " + tweetID);
            return;
        }
        int mediaIndex = 0;
        for (final Map<String, Object> media : ressourcelist) {
            String url = (String) media.get("media_url_https");
            final String expanded_url = (String) media.get("expanded_url");
            if (StringUtils.isEmpty(url)) {
                /* Ignore invalid items */
                return;
            }
            final DownloadLink dl;
            /* 2020-02-10: Recognize videos by this URL. If it is a thumbnail --< It is a video */
            if (url.contains("/tweet_video_thumb/") || url.contains("/amplify_video_thumb/") || url.contains("/ext_tw_video_thumb/") || StringUtils.contains(expanded_url, "/video/")) {
                /* Video --> Needs to go into crawler again */
                dl = this.createDownloadlink(createVideourl(tweetID));
                /* This filename may be changed later by our hostplugin */
                String tempFilename;
                if (useOriginalFilenames) {
                    tempFilename = tweetID + "_" + Plugin.getFileNameFromURL(new URL(url));
                    /* Fix extension as the URL we got may point to a thumbnail image but we want a video extension. */
                    tempFilename = correctOrApplyFileNameExtension(tempFilename, ".mp4");
                } else {
                    tempFilename = formattedDate + "_" + username + "_" + tweetID + "_" + mediaIndex + ".mp4";
                }
                dl.setName(tempFilename);
            } else {
                /* Photo */
                if (!url.contains("?name=")) {
                    /* Gets highest quality */
                    url += "?name=orig";
                }
                dl = this.createDownloadlink(url);
                /* 2020-06-08: Let it survive users' reset especially for items which are handled by directhttp plugin. */
                if (useOriginalFilenames) {
                    dl.setFinalFileName(tweetID + "_" + Plugin.getFileNameFromURL(new URL(url)));
                } else {
                    dl.setFinalFileName(formattedDate + "_" + username + "_" + tweetID + "_" + mediaIndex + Plugin.getFileNameExtensionFromURL(url));
                }
            }
            dl.setAvailable(true);
            /* Set possible Packagizer properties */
            dl.setProperty(PROPERTY_USERNAME, username);
            dl.setProperty(PROPERTY_DATE, formattedDate);
            dl.setProperty(PROPERTY_MEDIA_INDEX, mediaIndex);
            dl.setProperty(PROPERTY_MEDIA_ID, media.get("id_str").toString());
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            decryptedLinks.add(dl);
            distribute(dl);
            mediaIndex += 1;
        }
    }

    private static String formatTwitterDate(String created_at) {
        if (created_at == null) {
            return null;
        }
        try {
            created_at = created_at.substring(created_at.indexOf(" ") + 1, created_at.length());
            final String targetFormat = "yyyy-MM-dd";
            final long timestamp = TimeFormatter.getMilliSeconds(created_at, "MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
            if (timestamp == -1) {
                throw new Exception("TimeFormatter failed for:" + created_at);
            }
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            return formatter.format(new Date(timestamp));
        } catch (final Throwable e) {
            /* Fallback */
            return created_at;
        }
    }

    @Deprecated
    private void crawlTweetViaMobileWebsite(final String parameter, final FilePackage fp) throws IOException {
        logger.info("Crawling mobile website tweet");
        final String tweet_id = new Regex(parameter, "/(?:tweet|status)/(\\d+)").getMatch(0);
        if (br.containsHTML("/status/" + tweet_id + "/video/1")) {
            /* Video */
            final DownloadLink dl = createDownloadlink(createVideourl(tweet_id));
            if (fp != null) {
                dl._setFilePackage(fp);
            }
            decryptedLinks.add(dl);
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
                        dl.setProperty(PROPERTY_FILENAME_FROM_CRAWLER, final_filename);
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
        final String username = new Regex(param.getCryptedUrl(), TYPE_USER_ALL).getMatch(0);
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
            if (br.getHttpConnection().getResponseCode() == 403) {
                /* {"errors":[{"code":22,"message":"Not authorized to view the specified user."}]} */
                throw new AccountRequiredException();
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* {"errors":[{"code":17,"message":"No user matches for specified terms."}]} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
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
        query.append("include_ext_sensitive_media_warning", "true", false);
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
            /* 2022-02-23: TODO: Remove this and always grab all "media". */
            logger.info("Grabbing ALL media of a user e.g. also retweets");
            content_type = "profile";
            max_countStr = statuses_count;
            query.add("include_tweet_replies", "false");
        }
        if (StringUtils.isEmpty(max_countStr)) {
            /* This should never happen */
            max_countStr = "??";
        }
        final UrlQuery addedURLQuery = UrlQuery.parse(param.getCryptedUrl());
        Number maxTweetsToCrawl = null;
        final String maxTweetsToCrawlStr = addedURLQuery.get("maxitems");
        final String maxTweetDateStr = addedURLQuery.get("max_date");
        long crawlUntilTimestamp = -1;
        if (maxTweetsToCrawlStr != null && maxTweetsToCrawlStr.matches("\\d+")) {
            maxTweetsToCrawl = Integer.parseInt(maxTweetsToCrawlStr);
        }
        if (maxTweetDateStr != null) {
            try {
                crawlUntilTimestamp = TimeFormatter.getMilliSeconds(maxTweetDateStr, "yyyy-MM-dd", Locale.ENGLISH);
            } catch (final Throwable ignore) {
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        /* we want all links from this user to go into the same package */
        fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
        fp.setName(fpname);
        query.append("userId", user_id, false);
        query.append("count", expected_items_per_page + "", false);
        query.append("ext", "mediaStats,cameraMoment", true);
        int crawled_tweet_count = 0;
        tweetTimeline: do {
            logger.info("Crawling page " + (index + 1));
            final UrlQuery thisquery = query;
            if (!StringUtils.isEmpty(nextCursor)) {
                thisquery.append("cursor", nextCursor, true);
            }
            final String url = String.format("https://api.twitter.com/2/timeline/%s/%s.json", content_type, user_id);
            br.getPage(url + "?" + thisquery.toString());
            handleErrorsAPI(this.br);
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            final List<Object> pagination_info = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "timeline/instructions/{0}/addEntries/entries");
            final Map<String, Object> tweetMap = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "globalObjects/tweets");
            if (tweetMap == null || tweetMap.isEmpty()) {
                logger.info("Found 0 tweets on current page --> Stopping");
                break;
            }
            final Iterator<Entry<String, Object>> iterator = tweetMap.entrySet().iterator();
            String lastCreatedAtDateStr = null;
            while (iterator.hasNext()) {
                final Map<String, Object> tweet = (Map<String, Object>) iterator.next().getValue();
                crawlTweetMediaObjectsAPI(fp, username, tweet);
                crawled_tweet_count++;
                lastCreatedAtDateStr = (String) tweet.get("created_at");
                final long currentTweetTimestamp = TimeFormatter.getMilliSeconds(lastCreatedAtDateStr, "EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
                /* Check some abort conditions */
                if (maxTweetsToCrawl != null && crawled_tweet_count >= maxTweetsToCrawl.intValue()) {
                    logger.info("Stopping because: Reached user defined max items count: " + maxTweetsToCrawl);
                    break tweetTimeline;
                } else if (crawlUntilTimestamp != -1 && currentTweetTimestamp > crawlUntilTimestamp) {
                    logger.info("Stopping because: Reached max desired tweet age of: " + maxTweetDateStr);
                    break tweetTimeline;
                }
            }
            logger.info(String.format("Tweets current page: %d|Tweets crawled so far: %d of expected total %s", tweetMap.size(), crawled_tweet_count, max_countStr));
            logger.info("Last created_at date of current page: " + lastCreatedAtDateStr);
            if (tweetMap.size() < expected_items_per_page) {
                logger.info(String.format("Warning: Page contains less than %d objects --> Reached the end?", expected_items_per_page));
            }
            /* Done - now try to find string required to access next page */
            try {
                Map<String, Object> pagination_info_entries = (Map<String, Object>) pagination_info.get(pagination_info.size() - 1);
                final String entryId = (String) pagination_info_entries.get("entryId");
                if (entryId.contains("cursor-bottom")) {
                    logger.info("Expecting next page to be available...");
                    final String nextCursorTmp = (String) JavaScriptEngineFactory.walkJson(pagination_info_entries, "content/operation/cursor/value");
                    if (StringUtils.isEmpty(nextCursorTmp)) {
                        logger.info("Stopping because: Failed to find nextCursor");
                        break;
                    }
                    logger.info("nextCursor = " + nextCursor);
                    if (nextCursor != null && nextCursor.equals(nextCursorTmp)) {
                        /* Extra fallback - this should never be required */
                        logger.info("Stopping because: New nextCursor is the same as last nextCursor --> Reached the end?!");
                        break;
                    } else {
                        nextCursor = nextCursorTmp;
                    }
                } else {
                    logger.info("Stopping because: Found wrong cursor object --> Plugin needs update");
                    break;
                }
            } catch (final Throwable e) {
                logger.log(e);
                logger.info("Stopping because: Failed to get nextCursor (Exception occured)");
                break;
            }
            index++;
            this.sleep(3000l, param);
        } while (!this.isAbort());
        logger.info(String.format("Done after %d pages", index));
        if (decryptedLinks.isEmpty()) {
            logger.info("Found nothing --> Either user has posts containing media or those can only be viewed by certain users or only when logged in (explicit content)");
        }
    }

    /**
     * https://developer.twitter.com/en/support/twitter-api/error-troubleshooting </br> Scroll down to "Twitter API error codes"
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

    protected void getPage(final Browser br, final String url) throws Exception {
        super.getPage(br, url);
        if (br.getHttpConnection().getResponseCode() == 429) {
            logger.info("Error 429 too many requests - add less URLs and/or perform a reconnect!");
        }
    }

    protected void getPage(final String url) throws Exception {
        getPage(br, url);
    }

    private String createVideourl(final String tweetID) {
        return String.format("https://twitter.com/i/videos/tweet/%s", tweetID);
    }

    public static String createTwitterPostURL(final String user, final String tweetID) {
        return "https://twitter.com/" + user + "/status/" + tweetID;
    }

    /** Log in the account of the hostplugin */
    @SuppressWarnings({ "static-access" })
    private Account getUserLogin(final boolean force) throws Exception {
        final PluginForHost hostPlugin = getNewPluginForHostInstance("twitter.com");
        final Account aa = AccountController.getInstance().getValidAccount("twitter.com");
        if (aa == null) {
            return null;
        }
        try {
            ((jd.plugins.hoster.TwitterCom) hostPlugin).login(this, br, aa, force);
            return aa;
        } catch (final PluginException e) {
            logger.log(e);
            return null;
        }
    }

    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-30: We have to perform a lot of requests --> Set this to 1. */
        return 1;
    }
}
