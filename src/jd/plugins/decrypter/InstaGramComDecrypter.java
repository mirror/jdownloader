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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.InstagramConfig;
import org.jdownloader.plugins.components.config.InstagramConfig.ActionOnRateLimitReached;
import org.jdownloader.plugins.components.config.InstagramConfig.FilenameType;
import org.jdownloader.plugins.components.config.InstagramConfig.SinglePostPackagenameSchemeType;
import org.jdownloader.plugins.components.config.InstagramConfig.StoriesHighlightsPackagenameSchemeType;
import org.jdownloader.plugins.components.config.InstagramConfig.StoryPackagenameSchemeType;
import org.jdownloader.plugins.components.instagram.Qdb;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
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
import jd.plugins.hoster.InstaGramCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 4, names = {}, urls = {})
public class InstaGramComDecrypter extends PluginForDecrypt {
    public InstaGramComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "instagram.com" });
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
            String regex = "https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(";
            regex += "stories/(?:[^/]+/\\d+/?|[^/]+)";
            regex += "|explore/tags/[^/]+/?";
            regex += "|(?:p|tv|reel)/[A-Za-z0-9_-]+";
            regex += "|(?!explore)[^/]+(?:/(saved|tagged|reels)/)?";
            regex += "|p/[A-Za-z0-9_-]+";
            regex += ")";
            ret.add(regex);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    @Override
    public void init() {
        setRequestIntervalLimitGlobal();
    }

    public static void setRequestIntervalLimitGlobal() {
        final int limit = PluginJsonConfig.get(InstagramConfig.class).getGlobalRequestIntervalLimitMilliseconds();
        if (limit > 0) {
            Browser.setRequestIntervalLimitGlobal("instagram.com", limit);
            Browser.setRequestIntervalLimitGlobal("cdninstagram.com", limit);
        }
    }

    private static final String                  TYPE_PROFILE          = "(?i)https?://[^/]+/([^/]+)(?:/.*)?";
    private static final String                  TYPE_PROFILE_TAGGED   = "(?i)https?://[^/]+/([^/]+)/tagged/?$";
    private static final String                  TYPE_PROFILE_REELS    = "(?i)https?://[^/]+/([^/]+)/reels/?$";
    private static final String                  TYPE_GALLERY          = "(?i).+/(?:p|tv|reel)/([A-Za-z0-9_-]+)/?";
    private static final Pattern                 PATTERN_STORY         = Pattern.compile("(?i)https?://[^/]+/stories/([^/]+)(/(\\d+)/?)?");
    private static final String                  TYPE_STORY_HIGHLIGHTS = "(?i)https?://[^/]+/stories/highlights/(\\d+)/?";
    private static final String                  TYPE_SAVED_OBJECTS    = "(?i)https?://[^/]+/([^/]+)/saved/?$";
    private static final String                  TYPE_HASHTAG          = "(?i)https?://[^/]+/explore/tags/([^/]+)/?$";
    /**
     * For links matching pattern {@link #TYPE_HASHTAG} --> This will be set on created DownloadLink objects as a (packagizer-) property.
     */
    private static LinkedHashMap<String, String> ID_TO_USERNAME        = new LinkedHashMap<String, String>() {
                                                                           protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                                                                               return size() > 100;
                                                                           };
                                                                       };

    /** Tries different json paths and returns the first result. */
    private Object get(Map<String, Object> entries, final String... paths) {
        for (String path : paths) {
            final Object ret = JavaScriptEngineFactory.walkJson(entries, path);
            if (ret != null) {
                logger.info("Found:" + path);
                return ret;
            } else {
                logger.info("Not found:" + path);
            }
        }
        return null;
    }

    private void getPage(final CryptedLink link, final Browser br, final String url, final String rhxGis, final String variables) throws Exception {
        getPage(link, br, br.createGetRequest(url), rhxGis, variables);
    }

    @SuppressWarnings({ "unused" })
    private void getPage(CryptedLink link, final Browser br, final Request sourceRequest, final String rhxGis, final String variables) throws Exception {
        int retry = 0;
        final int maxtries = 30;
        long totalWaittime = 0;
        Request request = null;
        boolean success = false;
        while (retry < maxtries && !isAbort()) {
            retry++;
            request = sourceRequest.cloneRequest();
            if (rhxGis != null && variables != null) {
                if (false) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final String sig = Hash.getMD5(rhxGis + ":" + variables);
                    request.getHeaders().put("X-Instagram-GIS", sig);
                }
            }
            if (retry > 1) {
                logger.info(String.format("Trying to get around rate limit %d / %d", retry, maxtries));
                /* 2020-01-21: Changing User-Agent or Cookies will not help us to get around this limit earlier! */
                // br.clearCookies(br.getHost());
                // br.getHeaders().put("User-Agent", "iPad");
            }
            br.getPage(request);
            final int responsecode = br.getHttpConnection().getResponseCode();
            if (responsecode == 502) {
                final int waittime = 20000 + 15000 * retry;
                totalWaittime += waittime;
                logger.info(String.format("Waiting %d seconds on error 502 until retry", waittime / 1000));
                sleep(waittime, link);
            } else if (responsecode == 403 || responsecode == 429) {
                if (PluginJsonConfig.get(InstagramConfig.class).getActionOnRateLimitReached() == ActionOnRateLimitReached.ABORT) {
                    logger.info("Rate limit has been reached --> Aborting");
                    throw new DecrypterRetryException(RetryReason.HOST_RATE_LIMIT, "RATE_LIMIT_REACHED_" + br._getURL().getPath(), "Rate limit has been reached and user prefers abort in this case.");
                } else {
                    final int waittime = 20000 + 15000 * retry;
                    totalWaittime += waittime;
                    logger.info(String.format("Waiting %d seconds on error 403/429 until retry", waittime / 1000));
                    sleep(waittime, link);
                }
            } else {
                success = true;
                break;
            }
        }
        if (!success) {
            if (br.getHttpConnection().getResponseCode() == 502) {
                throw br.new BrowserException("ResponseCode: 502", br.getRequest(), null);
            } else if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 429) {
                throw new DecrypterRetryException(RetryReason.CAPTCHA, "RATE_LIMIT_REACHED_EXHAUSTED_RETRY_COUNT_" + br._getURL().getPath(), "Rate limit has been reached and crawler failed to avoid it. Try again later.");
            } else {
                /* Developer mistake */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (retry > 1) {
            logger.info("Total time waited to get around rate limit: " + TimeFormatter.formatMilliSeconds(totalWaittime, 0));
        }
    }

    // hash changes? but the value within is NEVER cleared. if map > resources || resources == null) remove storable
    private static Map<String, Qdb> QUERY_HASH = new HashMap<String, Qdb>();

    // https://www.diggernaut.com/blog/how-to-scrape-pages-infinite-scroll-extracting-data-from-instagram/
    // https://git.kaki87.net/KaKi87/ig-scraper/src/branch/master/index.js#L190
    private Qdb getQueryHash(final Browser br, final Qdb.QUERY query) throws Exception {
        synchronized (QUERY_HASH) {
            final String userQuery = br.getRegex("(/static/bundles/([^/]+/)?ConsumerLibCommons\\.js/[a-f0-9]+.js)").getMatch(0);
            if (userQuery != null && Qdb.QUERY.USER.equals(query)) {
                final String id = query.name() + userQuery;
                Qdb qdb = QUERY_HASH.get(id);
                if (qdb != null) {
                    return qdb;
                }
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept", "*/*");
                brc.getPage(userQuery);
                final String fbAppId = brc.getRegex("e\\.instagramWebDesktopFBAppId\\s*=\\s*'(\\d+)'").getMatch(0);
                final String qHash = brc.getRegex("\\},queryId\\s*:\\s*\"([0-9a-f]{32})\"").getMatch(0);
                if (StringUtils.isAllNotEmpty(qHash, fbAppId)) {
                    logger.info("found:" + query + "|" + qHash + "|" + fbAppId);
                    qdb = new Qdb();
                    qdb.setFbAppId(fbAppId);
                    qdb.setQueryHash(qHash);
                    QUERY_HASH.put(id, qdb);
                    return qdb;
                }
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "failed:" + query);
        }
    }

    /** Do we have to be logged in to crawl this URL? */
    private boolean requiresLogin(final String url) {
        if (url.matches(TYPE_SAVED_OBJECTS) || url.matches(TYPE_STORY_HIGHLIGHTS) || new Regex(url, PATTERN_STORY).patternFind() || url.matches(TYPE_PROFILE_TAGGED)) {
            return true;
        } else {
            return false;
        }
    }

    /** Get username from userID via alternative API. */
    private String getUsernameFromUserIDAltAPI(final Browser br, final String userID) throws PluginException, IOException {
        if (userID == null || !userID.matches("\\d+")) {
            return null;
        }
        final Browser brc = br.cloneBrowser();
        brc.setRequest(null);
        InstaGramCom.prepBRAltAPI(brc);
        InstaGramCom.getPageAltAPI(null, brc, InstaGramCom.ALT_API_BASE + "/users/" + userID + "/info/");
        final Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
        final Map<String, Object> user = (Map<String, Object>) entries.get("user");
        if (user == null) {
            return null;
        }
        final String username = (String) user.get("username");
        /* Cache information for later usage */
        addCachedUserID(userID, username);
        return username;
    }

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        br.addAllowedResponseCodes(new int[] { 502 });
        final AtomicBoolean loggedIN = new AtomicBoolean(false);
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        if (this.requiresLogin(param.getCryptedUrl()) && !loggedIN.get() && account == null) {
            /* E.g. saved users own objects can only be crawled when he's logged in ;) */
            throw new AccountRequiredException();
        }
        InstaGramCom.prepBRWebsite(this.br);
        br.addAllowedResponseCodes(new int[] { 502 });
        try {
            if (param.getCryptedUrl().matches(TYPE_SAVED_OBJECTS)) {
                return this.crawlUserSavedObjects(param, account, loggedIN);
            } else if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
                /* Crawl single images & galleries */
                return crawlGallery(param, account, loggedIN);
            } else if (param.getCryptedUrl().matches(TYPE_HASHTAG)) {
                return crawlHashtag(param, account, loggedIN);
            } else if (param.getCryptedUrl().matches(TYPE_STORY_HIGHLIGHTS)) {
                return this.crawlStoryHighlight(param, account, loggedIN);
            } else if (new Regex(param.getCryptedUrl(), PATTERN_STORY).patternFind()) {
                return this.crawlStory(param, account, loggedIN, true);
            } else if (param.getCryptedUrl().matches(TYPE_PROFILE_REELS)) {
                return this.crawlUserReels(param, account, loggedIN);
            } else if (param.getCryptedUrl().matches(TYPE_PROFILE_TAGGED)) {
                return this.crawlUserTagged(param, account, loggedIN);
            } else {
                return this.crawlUser(param, account, loggedIN);
            }
        } catch (final AccountUnavailableException e) {
            if (account != null) {
                handleAccountException(account, e);
            }
            throw e;
        } catch (final AccountInvalidException e) {
            if (account != null) {
                handleAccountException(account, e);
            }
            throw e;
        }
    }

    /**
     * Returns userID for given username. </br>
     * Uses website to find userID. </br>
     * Throws Exception if it is unable to find userID in HTML code --> Profile is most likely offline then!
     */
    private String findUserID(final CryptedLink param, final Account account, final AtomicBoolean loggedIN, final String username) throws Exception {
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* First check our cache -> saves time */
        String cachedUserID = getCachedUserID(username);
        if (cachedUserID != null) {
            /* Return cached userID */
            return cachedUserID;
        }
        /* Use website to find userID. */
        final boolean useSearchToFindUserID = true;
        String userID = null;
        final String userProfileURL = this.generateURLProfile(username);
        if (useSearchToFindUserID) {
            /* 2022-05-11: New method: Faster and json only */
            Request req = br.createGetRequest("https://www." + this.getHost() + "/web/search/topsearch/?context=blended&query=" + username + "&include_reel=true");
            /* None of these headers are mandatory atm. */
            req.getHeaders().put("Referer", userProfileURL);
            req.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPageAutoLogin(account, loggedIN, req.getUrl(), param, br, req, null, null);
            Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            if (Boolean.TRUE.equals(entries.get("require_login")) || br.getHttpConnection().getResponseCode() == 401) {
                /*
                 * 2022-10-07 E.g.
                 * {"message":"Bitte warte einige Minuten und versuche es dann noch einmal.","require_login":true,"status":"fail"}
                 */
                /* 2023-11-08: Also possible: response 401 with {"message":"Server Error","status":"fail"} */
                if (loggedIN.get()) {
                    /* Already logged in -> This should never happen */
                    logger.warning("Login required but we're already logged in -> Possible problem with account/session");
                    throw new AccountRequiredException();
                } else if (account == null) {
                    /* Account required but not available */
                    throw new AccountRequiredException();
                }
                sleep(2000, param);
                logger.info("Logging in because: " + entries.get("message"));
                /* We're logged in now -> Perform request again. */
                req = req.cloneRequest();
                getPageAutoLogin(account, loggedIN, true, req.getUrl(), param, br, req, null, null);
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            }
            if ("fail".equals(entries.get("status"))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final List<Map<String, Object>> users = (List<Map<String, Object>>) entries.get("users");
            for (Map<String, Object> entry : users) {
                final Map<String, Object> user = (Map<String, Object>) entry.get("user");
                if (StringUtils.equalsIgnoreCase(username, StringUtils.valueOfOrNull(user.get("username")))) {
                    userID = user.get("pk").toString();
                    break;
                }
            }
            if (StringUtils.isEmpty(userID)) {
                /* Most likely that profile doesn't exist */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Profile not found");
            }
        } else {
            getPageAutoLogin(account, loggedIN, userProfileURL, param, br, userProfileURL, null, null);
            final String json = websiteGetJson();
            final Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            userID = (String) get(entries, "entry_data/ProfilePage/{0}/user/id", "entry_data/ProfilePage/{0}/graphql/user/id");
            if (StringUtils.isEmpty(userID)) {
                userID = br.getRegex("\"owner\":\\s*\\{\"id\":\\s*\"(\\d+)\"\\}").getMatch(0);
                if (StringUtils.isEmpty(userID)) {
                    userID = br.getRegex("\"page_id\"\\s*:\\s*\"profilePage_(\\d+)").getMatch(0);
                }
            }
        }
        /*
         * 2022-06-23: Alternative way: https://i.instagram.com/api/v1/users/web_profile_info/?username=<username> --> Requires special
         * User-Agent!
         */
        if (StringUtils.isEmpty(userID)) {
            /* Most likely that profile doesn't exist */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Profile not found");
        }
        /* Add to cache for later usage */
        addCachedUserID(userID, username);
        return userID;
    }

    private void addCachedUserID(final int userID, final String username) {
        addCachedUserID(Integer.toString(userID), username);
    }

    private void addCachedUserID(final String userID, final String username) {
        synchronized (ID_TO_USERNAME) {
            ID_TO_USERNAME.put(userID, username);
        }
    }

    /** Get cached userID by username */
    private String getCachedUserID(final String username) {
        if (username == null) {
            return null;
        }
        synchronized (ID_TO_USERNAME) {
            final Iterator<Entry<String, String>> iterator = ID_TO_USERNAME.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, String> entry = iterator.next();
                if (entry.getValue().equals(username)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private String getCachedUserName(final int userID) {
        return getCachedUserName(Integer.toString(userID));
    }

    private String getCachedUserName(final String userID) {
        if (userID == null) {
            return null;
        }
        synchronized (ID_TO_USERNAME) {
            return ID_TO_USERNAME.get(userID);
        }
    }

    private String websiteGetJson() {
        return websiteGetJson(this.br);
    }

    private String websiteGetJson(final Browser br) {
        return br.getRegex(">\\s*window\\._sharedData\\s*?=\\s*?(\\{.*?);</script>").getMatch(0);
    }

    private String getVarRhxGis(final Browser br) {
        return br.getRegex("\"rhx_gis\"\\s*:\\s*\"([a-f0-9]{32})\"").getMatch(0);
    }

    private void getPageAutoLogin(final Account account, final AtomicBoolean loginState, final String urlCheck, final CryptedLink param, final Browser br, final String requestURL, final String rhxGis, final String variables) throws Exception {
        getPageAutoLogin(account, loginState, urlCheck, param, br, br.createGetRequest(requestURL), rhxGis, variables);
    }

    private void getPageAutoLogin(final Account account, final AtomicBoolean loginState, final String urlCheck, final CryptedLink param, final Browser br, final Request request, final String rhxGis, final String variables) throws Exception {
        getPageAutoLogin(account, loginState, null, urlCheck, param, br, request, rhxGis, variables);
    }

    /**
     * Automatically performs given request and logs in if needed and possible. </br>
     *
     * @param urlCheck
     *            Ensures that we are on this URL after request has been performed.
     */
    private void getPageAutoLogin(final Account account, final AtomicBoolean loginState, Boolean forceLogin, final String urlCheck, final CryptedLink param, final Browser br, final Request request, final String rhxGis, final String variables) throws Exception {
        if (PluginJsonConfig.get(InstagramConfig.class).isEnforceLoginIfAccountIsAvailable() && account != null && !loginState.get()) {
            logger.info("Performing forced login according to user plugin setting");
            this.loginOrFail(account, loginState);
        } else if (Boolean.TRUE.equals(forceLogin) && !loginState.get()) {
            logger.info("Performing forced login");
            this.loginOrFail(account, loginState);
        }
        getPage(param, br, request, null, null);
        AccountRequiredException accountRequired = null;
        try {
            InstaGramCom.checkErrors(this, br);
        } catch (final AccountRequiredException e) {
            if (account == null) {
                // fail fast
                throw e;
            } else if (loginState.get()) {
                // already logged in?
                throw e;
            } else {
                accountRequired = e;
            }
        }
        if (accountRequired != null || (urlCheck != null && !br.getURL().contains(urlCheck))) {
            /*
             * E.g. private gallery and we're not logged in or we're not logged in with an account with the required permissions -> Redirect
             * to main page or URL of the profile which uploaded the gallery.
             */
            if (account == null) {
                // fail fast
                throw new AccountRequiredException();
            } else if (loginState.get()) {
                // already logged in?
                logger.info("Login required but user is already logged in?!");
                throw new AccountRequiredException();
            } else {
                loginOrFail(account, loginState);
                getPage(param, br, request, null, null);
                try {
                    InstaGramCom.checkErrors(this, br);
                    if (urlCheck != null && !br.getURL().contains(urlCheck)) {
                        throw new AccountRequiredException();
                    }
                } catch (final AccountRequiredException e) {
                    logger.exception("Logged in but URL still isn't accessible", e);
                    throw e;
                }
            }
        }
    }

    /**
     * Gallery == post. Can contain single or multiple media items (image/video). </br>
     * If multiple media is present, insividual pictures cannot be linked individually.
     */
    private ArrayList<DownloadLink> crawlGallery(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws Exception {
        final String galleryID = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
        if (galleryID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /**
         * TODO: This is not a TODO, just an information: 2022-06-22: Example get json right away: Add "?__a=1&__d=dis" to URL e.g.: </br>
         * https://www.instagram.com/p/<postID>/?__a=1&__d=dis
         */
        getPageAutoLogin(account, loggedIN, galleryID, param, br, param.getCryptedUrl(), null, null);
        final List<Map<String, Object>> postsFromWebsite = new ArrayList<Map<String, Object>>(0);
        try {
            final String json = websiteGetJson();
            Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
            List<Map<String, Object>> resource_data_list = null;
            if (loggedIN.get()) {
                final String[] bboxJsons = br.getRegex("data-sjs>(\\{\"require\".*?\\})</script>").getColumn(0);
                if (bboxJsons != null && bboxJsons.length > 0) {
                    for (final String bboxJson : bboxJsons) {
                        final Map<String, Object> bboxJsonParsed = restoreFromString(bboxJson, TypeRef.MAP);
                        final Map<String, Object> result = (Map<String, Object>) findPostsListRecursive(bboxJsonParsed);
                        if (result != null) {
                            final List<Map<String, Object>> postitems = (List<Map<String, Object>>) result.get("items");
                            for (final Map<String, Object> postitem : postitems) {
                                postsFromWebsite.add(postitem);
                            }
                            break;
                        }
                    }
                }
            } else {
                resource_data_list = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "entry_data/PostPage");
            }
            if (resource_data_list != null) {
                /**
                 * Old code </br>
                 * TODO: Delete this
                 */
                for (final Map<String, Object> entry : resource_data_list) {
                    final Map<String, Object> mediaSource = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entry, "graphql/shortcode_media");
                    if (mediaSource != null) {
                        postsFromWebsite.add(mediaSource);
                        continue;
                    }
                    final List<Map<String, Object>> items = (List<Map<String, Object>>) entry.get("items");
                    if (items == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    postsFromWebsite.addAll(items);
                }
            }
        } catch (final Throwable e) {
            logger.log(e);
            logger.warning("Website mode failed with exception");
            /* Allow fallback to API down below. */
        }
        if (postsFromWebsite.size() > 0) {
            final InstagramMetadata metadata = new InstagramMetadata();
            if (param.getDownloadLink() != null) {
                /*
                 * Check for previously set properties e.g. user crawls hashtag URL --> Some URLs can go go back into crawler --> We want to
                 * keep the hashtag value in order to use it inside filenames/packagenames and as a packagizer property.
                 */
                final DownloadLink link = param.getDownloadLink();
                final String hashtag = link.getStringProperty(InstaGramCom.PROPERTY_hashtag);
                if (hashtag != null) {
                    metadata.setHashtag(hashtag);
                }
                final String forcedPackagename = link.getStringProperty(InstaGramCom.PROPERTY_forced_packagename);
                if (forcedPackagename != null) {
                    metadata.setPackageName(forcedPackagename);
                }
            }
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            for (final Map<String, Object> post : postsFromWebsite) {
                ret.addAll(crawlPost(param, metadata, post));
            }
            return ret;
        } else {
            /* API mode. Required when we're logged in. */
            logger.info("Auto fallback to API crawler");
            if (account == null) {
                throw new AccountRequiredException();
            }
            String internalMediaID = null;
            do {
                internalMediaID = br.getRegex("property=\"al:ios:url\" content=\"instagram://media\\?id=(\\d+)").getMatch(0);
                if (internalMediaID == null) {
                    internalMediaID = br.getRegex("\"media_id\"\\s*:\\s*\"(\\d+)").getMatch(0);
                }
                if (internalMediaID != null || loggedIN.get()) {
                    break;
                } else {
                    logger.info("Failed to find internalMediaID in logged-out state --> Retry with account");
                    this.loginOrFail(account, loggedIN);
                    br.getPage(param.getCryptedUrl());
                    InstaGramCom.checkErrors(this, br);
                    continue;
                }
            } while (true);
            if (internalMediaID == null) {
                logger.info("Content is either offline or given account is lacking permissions to view it.");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return this.crawlGalleryAltAPI(param, account, loggedIN, internalMediaID);
        }
    }

    /* Finds object containing single Instagram posts inside given parsed website-json object. */
    private Object findPostsListRecursive(final Object o) {
        if (o instanceof Map) {
            final Map<String, Object> entrymap = (Map<String, Object>) o;
            final Object targetO = entrymap.get("xdt_api__v1__media__shortcode__web_info");
            if (targetO != null) {
                return targetO;
            } else {
                for (final Map.Entry<String, Object> entry : entrymap.entrySet()) {
                    // final String key = entry.getKey();
                    final Object value = entry.getValue();
                    if (value instanceof List || value instanceof Map) {
                        final Object ret = findPostsListRecursive(value);
                        if (ret != null) {
                            return ret;
                        }
                    }
                }
                return null;
            }
        } else if (o instanceof List) {
            final List<Object> array = (List) o;
            for (final Object arrayo : array) {
                if (arrayo instanceof List || arrayo instanceof Map) {
                    final Object res = findPostsListRecursive(arrayo);
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

    private ArrayList<DownloadLink> crawlGalleryAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN, final String internalMediaID) throws Exception {
        if (internalMediaID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        loginOrFail(account, loggedIN);
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(account, this.br, InstaGramCom.ALT_API_BASE + "/media/" + internalMediaID + "/info/");
        InstaGramCom.checkErrorsAltAPI(account, br);
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final List<Map<String, Object>> items = (List<Map<String, Object>>) entries.get("items");
        final Map<String, Object> firstItem = items.get(0);
        final Map<String, Object> user = (Map<String, Object>) firstItem.get("user");
        final InstagramMetadata metadata = new InstagramMetadata(user.get("username").toString());
        metadata.setMainContentID(firstItem.get("code").toString());
        metadata.setDate(new Date(((Number) firstItem.get("taken_at")).longValue() * 1000));
        // for (final Map<String, Object> item : items) {
        // }
        return this.crawlPostListAltAPI(param, items, metadata);
    }

    /** Crawls all items of a user or all items where one user was tagged */
    private ArrayList<DownloadLink> crawlUser(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final String username = new Regex(param.getCryptedUrl(), TYPE_PROFILE).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final InstagramConfig cfg = PluginJsonConfig.get(InstagramConfig.class);
        int userSelectedCrawlTypes = 0;
        final boolean crawlProfilePosts = cfg.getProfileCrawlerMaxItemsLimit() != 0;
        final boolean crawlProfilePicture = cfg.isProfileCrawlerCrawlProfilePicture();
        final boolean crawlStory = cfg.isProfileCrawlerCrawlStory();
        final boolean crawlStoryHighlights = cfg.isProfileCrawlerCrawlStoryHighlights();
        if (crawlStory) {
            userSelectedCrawlTypes++;
        }
        if (crawlStoryHighlights) {
            userSelectedCrawlTypes++;
        }
        if (crawlProfilePosts) {
            userSelectedCrawlTypes++;
        }
        if (crawlProfilePicture) {
            userSelectedCrawlTypes++;
        }
        if (userSelectedCrawlTypes == 0) {
            logger.info("User has disabled all profile crawler functionality");
            return decryptedLinks;
        } else {
            logger.info("crawlUser=crawlStory:" + crawlStory + "|crawlStoryHighlights:" + crawlStoryHighlights + "|crawlProfilePosts:" + crawlProfilePosts + "|crawlProfilePicture:" + crawlProfilePicture);
        }
        /* Now do the actual crawling. */
        if (crawlStory) {
            if (userSelectedCrawlTypes == 1) {
                decryptedLinks.addAll(this.crawlStory(param, username, account, loggedIN, true));
            } else {
                decryptedLinks.addAll(this.crawlStory(param, username, account, loggedIN, false));
            }
        }
        if (crawlStoryHighlights) {
            if (userSelectedCrawlTypes == 1) {
                decryptedLinks.addAll(this.crawlAllHighlightStories(username, account, loggedIN, true));
            } else {
                decryptedLinks.addAll(this.crawlAllHighlightStories(username, account, loggedIN, false));
            }
        }
        if (crawlProfilePicture || crawlProfilePosts) {
            if (loggedIN.get() || account != null) {
                final String userID = findUserID(param, account, loggedIN, username);
                decryptedLinks.addAll(this.crawlUserAltAPI(param, account, loggedIN, username, userID, crawlProfilePicture, crawlProfilePosts));
            } else {
                decryptedLinks.addAll(this.crawlUserWebsite(param, username, account, loggedIN, crawlProfilePicture, crawlProfilePosts));
            }
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlUserReels(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String username = new Regex(param.getCryptedUrl(), TYPE_PROFILE_REELS).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Crawling all reels of user: " + username);
        final String userID = this.findUserID(param, account, loggedIN, username);
        final UrlQuery query = new UrlQuery();
        query.add("target_user_id", userID);
        /* 12 = default pagination value of website */
        query.add("page_size", Integer.toString(PluginJsonConfig.get(InstagramConfig.class).getProfileCrawlerReelsPaginationMaxItemsPerPage()));
        query.add("include_feed_video", "true");
        String max_id = null;
        final InstagramMetadata metadata = new InstagramMetadata();
        metadata.setPackageName(username + " - reels");
        InstaGramCom.prepBRAltAPI(this.br);
        int page = 1;
        do {
            InstaGramCom.postPageAltAPI(account, this.br, InstaGramCom.ALT_API_BASE + "/clips/user/", query);
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> paging_info = (Map<String, Object>) entries.get("paging_info");
            final List<Map<String, Object>> mediaItems = (List<Map<String, Object>>) entries.get("items");
            if (mediaItems.size() == 0) {
                ret.add(this.createOfflinelink(param.getCryptedUrl(), "PROFILE_CONTAINS_NO_REELS_" + username, "The following profile doesn't contain any reels: " + username));
                break;
            }
            ret.addAll(this.crawlPostListAltAPI(param, mediaItems, metadata));
            max_id = (String) paging_info.get("max_id");
            logger.info("Crawled page " + page + " | Found items so far: " + ret.size() + " | Next max_id: " + max_id);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (((Boolean) paging_info.get("more_available")).booleanValue() == false) {
                logger.info("Stopping because: more_available == false");
                break;
            } else if (StringUtils.isEmpty(max_id)) {
                /* Double fail-safe */
                logger.info("Stopping because: Reached last page");
                break;
            } else {
                page++;
                query.addAndReplace("max_id", Encoding.urlEncode(max_id));
            }
        } while (true);
        return ret;
    }

    private ArrayList<DownloadLink> crawlUserTagged(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        if (PluginJsonConfig.get(InstagramConfig.class).getProfileTaggedCrawledMaxItemsLimit() == 0) {
            logger.info("User has disabled all profile crawler functionality");
            return new ArrayList<DownloadLink>();
        }
        return this.crawlProfileTaggedAltAPI(param, account, loggedIN);
    }

    /**
     * Crawl all posts of a user. </br>
     * Sometimes Instagram requires user to be logged in to see any or more than X items. <br>
     * Only use this when you're not logged in!
     */
    @Deprecated
    private ArrayList<DownloadLink> crawlUserWebsite(final CryptedLink param, final String username, final Account account, final AtomicBoolean loggedIN, final boolean crawlProfilePicture, final boolean crawlPosts) throws UnsupportedEncodingException, Exception {
        if (username == null) {
            /* Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!crawlProfilePicture && !crawlPosts) {
            /* Developer mistake */
            throw new IllegalArgumentException("crawlProfilePicture and crawlPosts cannot both be false");
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String userID = this.findUserID(param, account, loggedIN, username);
        if (br.getRequest() == null || !br.getURL().contains("/" + username)) {
            /* We obtained userID from cache --> Access website */
            getPageAutoLogin(account, loggedIN, null, param, br, param.getCryptedUrl(), null, null);
            /* Double-check for invalid username / offline profile. */
            if (!this.br.containsHTML("\"username\":\"" + username)) {
                /* Invalid profile */
                ret.add(getDummyDownloadlinkProfileOffline(username));
                return ret;
            }
        }
        Map<String, Object> entries = null;
        Map<String, Object> user = null;
        boolean hasRetriedWithLogin = false;
        long numberofPosts = -1;
        List<Object> resource_data_list = null;
        do {
            final String json = websiteGetJson();
            if (json == null) {
                /*
                 * 2022-08-31: This website handling is mostly obsolete/broken and has been replaced by the "Graphql" handling by Instagram.
                 * Make users add an account so API handling is used instead.
                 */
                if (account == null) {
                    throw new AccountRequiredException();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            entries = restoreFromString(json, TypeRef.MAP);
            user = (Map<String, Object>) get(entries, "entry_data/ProfilePage/{0}/user", "entry_data/ProfilePage/{0}/graphql/user");
            resource_data_list = (List) get(user, "edge_owner_to_timeline_media/edges", "media/nodes");
            numberofPosts = JavaScriptEngineFactory.toLong(get(entries, "entry_data/ProfilePage/{0}/graphql/user/edge_owner_to_timeline_media/count", "entry_data/ProfilePage/{0}/user/media/count"), -1);
            if (((Boolean) user.get("is_private")) && numberofPosts > 0 && (resource_data_list == null || resource_data_list.size() == 0)) {
                if (!loggedIN.get()) {
                    /* Login and check if we can access this profile now. */
                    logger.info("Profile is private and we appear to be missing permissions to access it --> Logging in to confirm this");
                    br.clearCookies(br.getHost());
                    loginOrFail(account, loggedIN);
                    hasRetriedWithLogin = true;
                    continue;
                }
                logger.info("Cannot parse profile as it is private and not even visible when loggedIN");
                ret.add(getDummyDownloadlinkProfilePrivate(username));
                return ret;
            }
            break;
        } while (!hasRetriedWithLogin);
        final InstagramMetadata metadata = new InstagramMetadata(username);
        metadata.setPackageName(username);
        if (crawlProfilePicture) {
            final String profilePictureURL = user.get("profile_pic_url_hd").toString();
            final DownloadLink profilePic = this.createDownloadlink(profilePictureURL);
            final String ext = Plugin.getFileNameExtensionFromString(profilePictureURL);
            profilePic.setFinalFileName(username + ext);
            profilePic.setAvailable(true);
            profilePic._setFilePackage(metadata.getFilePackage());
            ret.add(profilePic);
            distribute(profilePic);
        }
        if (crawlPosts) {
            if (numberofPosts == 0) {
                ret.add(getDummyDownloadlinkProfileEmpty(username));
                return ret;
            }
            final String rhxGis = getVarRhxGis(this.br);
            final Qdb qdb = getQueryHash(br, Qdb.QUERY.USER);
            int page = 1;
            int decryptedLinksLastSize = 0;
            final long maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getProfileCrawlerMaxItemsLimit();
            String nextid = (String) get(user, "edge_owner_to_timeline_media/page_info/end_cursor");
            if (nextid == null) {
                nextid = (String) get(user, "media/page_info/end_cursor");
            }
            int numberofCrawledPosts = 0;
            do {
                if (page > 1) {
                    final Browser br = this.br.cloneBrowser();
                    prepBrAjax(br, qdb);
                    final Map<String, Object> vars = new LinkedHashMap<String, Object>();
                    vars.put("id", userID);
                    vars.put("first", 12);
                    vars.put("after", nextid);
                    if (qdb == null || qdb.getQueryHash() == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String jsonString = JSonStorage.serializeToJson(vars);
                    try {
                        getPageAutoLogin(account, loggedIN, "/graphql/query", param, br, "/graphql/query/?query_hash=" + qdb.getQueryHash() + "&variables=" + URLEncode.encodeURIComponent(jsonString), rhxGis, jsonString);
                    } catch (final AccountRequiredException ar) {
                        logger.log(ar);
                        /* Instagram blocks the amount of items a user can see based on factors we don't know. */
                        if (loggedIN.get()) {
                            /* We're logged in and still can't see the profile --> Something is wrong! */
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, ar);
                        } else {
                            final String errorText = "Account required to crawl more items of user " + username;
                            throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, errorText, errorText, ar);
                        }
                    }
                    InstaGramCom.checkErrors(this, br);
                    final Map<String, Object> response = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                    resource_data_list = (List) get(response, "data/user/edge_owner_to_timeline_media/edges", "data/user/edge_user_to_photos_of_you/edges");
                    nextid = (String) get(response, "data/user/edge_owner_to_timeline_media/page_info/end_cursor");
                    if (nextid == null) {
                        nextid = (String) get(response, "data/user/edge_user_to_photos_of_you/page_info/end_cursor");
                    }
                }
                if (resource_data_list == null || resource_data_list.size() == 0) {
                    logger.info("Stopping because: Found no new links on page " + page);
                    break;
                }
                decryptedLinksLastSize = ret.size();
                for (final Object o : resource_data_list) {
                    final Map<String, Object> result = (Map<String, Object>) o;
                    // pages > 0, have a additional nodes entry
                    if (result.size() == 1 && result.containsKey("node")) {
                        ret.addAll(crawlPost(param, metadata, (Map<String, Object>) result.get("node")));
                    } else {
                        ret.addAll(crawlPost(param, metadata, result));
                    }
                }
                numberofCrawledPosts += resource_data_list.size();
                logger.info("Crawled page: " + page + " | Crawled posts: " + numberofCrawledPosts + "/" + numberofPosts + " | Collected DownloadLink items so far: " + ret.size());
                if (maxItemsLimit > 0 && ret.size() >= maxItemsLimit) {
                    logger.info("Stopping because: User defined max items limit has been reached");
                    break;
                } else if (numberofCrawledPosts >= numberofPosts) {
                    logger.info("Stopping because: Crawled all posts");
                    break;
                } else if (ret.size() == decryptedLinksLastSize) {
                    logger.info("Stopping because: Failed to find any new items on current page");
                    break;
                } else if (StringUtils.isEmpty(nextid)) {
                    logger.info("Stopping because: nextid is missing/empty");
                    break;
                } else if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                }
                page++;
            } while (true);
            if (!isAbort()) {
                if (ret.size() == 0) {
                    logger.warning("WTF found no content at all for profile: " + username);
                } else {
                    logger.info("nextid:" + nextid + "|decryptedLinksCurrentSize:" + ret.size() + "|decryptedLinksLastSize:" + decryptedLinksLastSize + "|itemCount:" + numberofPosts + "|page:" + page);
                }
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlUserAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN, final String username, final String userID, final boolean crawlProfilePicture, final boolean crawlPosts) throws UnsupportedEncodingException, Exception {
        if (username == null || userID == null) {
            /* Most likely developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (!crawlProfilePicture && !crawlPosts) {
            /* Developer mistake */
            throw new IllegalArgumentException("crawlProfilePicture and crawlPosts cannot both be false");
        }
        final InstagramMetadata metadata = new InstagramMetadata(username);
        metadata.setPackageName(username);
        InstaGramCom.prepBRAltAPI(this.br);
        String nextid = null;
        int page = 1;
        int numberofCrawledPosts = 0;
        final int maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getProfileCrawlerMaxItemsLimit();
        final String profilePostsFeedBaseURL = InstaGramCom.ALT_API_BASE + "/feed/user/" + userID + "/";
        boolean hasCrawledProfilePicture = false;
        do {
            if (page == 1) {
                InstaGramCom.getPageAltAPI(account, this.br, profilePostsFeedBaseURL);
            } else {
                // br.getPage(hashtagBaseURL + "?after=" + nextid);
                InstaGramCom.getPageAltAPI(account, this.br, profilePostsFeedBaseURL + "?max_id=" + nextid);
            }
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            if (crawlProfilePicture && !hasCrawledProfilePicture) {
                final Map<String, Object> user = (Map<String, Object>) entries.get("user");
                final String profilePictureURL = user.get("profile_pic_url").toString();
                final DownloadLink profilePic = this.createDownloadlink(profilePictureURL);
                final String ext = Plugin.getFileNameExtensionFromString(profilePictureURL);
                profilePic.setFinalFileName(username + ext);
                profilePic.setAvailable(true);
                profilePic._setFilePackage(metadata.getFilePackage());
                decryptedLinks.add(profilePic);
                distribute(profilePic);
                hasCrawledProfilePicture = true;
            }
            if (!crawlPosts) {
                /* Do not crawl posts */
                logger.info("Stopping because: do not crawl posts");
                break;
            }
            final int numberofItemsOnCurrentPage = (int) JavaScriptEngineFactory.toLong(entries.get("num_results"), 0);
            if (numberofItemsOnCurrentPage == 0) {
                /* Rare case */
                if (page == 1) {
                    /* Looks like profile doesn't contain any posts. */
                    decryptedLinks.add(this.getDummyDownloadlinkProfileEmpty(username));
                } else {
                    logger.info("Stopping because: 0 items available on current page");
                }
                return decryptedLinks;
            }
            nextid = (String) entries.get("next_max_id");
            final boolean more_available = ((Boolean) entries.get("more_available"));
            final List<Map<String, Object>> mediaItems = (List<Map<String, Object>>) entries.get("items");
            if (mediaItems == null || mediaItems.size() == 0) {
                logger.info("Stopping because: Found no new links on page " + page);
                break;
            }
            decryptedLinks.addAll(this.crawlPostListAltAPI(param, mediaItems, metadata));
            numberofCrawledPosts += numberofItemsOnCurrentPage;
            logger.info("Crawled page: " + page + " | Crawled posts so far: " + numberofCrawledPosts);
            if (!more_available) {
                logger.info("Stopping because: more_available == false");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because: no nextid available");
                break;
            } else if (maxItemsLimit > 0 && numberofCrawledPosts >= maxItemsLimit) {
                logger.info("Stopping because: reached user defined max items limit of " + maxItemsLimit);
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }

    private void loginOrFail(final Account account, final AtomicBoolean loggedIN) throws Exception {
        if (account == null) {
            throw new AccountRequiredException();
        }
        if (!loggedIN.get()) {
            final PluginForHost plg = getNewPluginForHostInstance(getHost());
            ((jd.plugins.hoster.InstaGramCom) plg).login(account, false);
            loggedIN.set(true);
        }
    }

    /**
     * Crawls all saved media items of the currently logged in user. </br>
     * Obviously this will only work when logged in.
     */
    private ArrayList<DownloadLink> crawlUserSavedObjectsWebsite(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        getPageAutoLogin(account, loggedIN, null, param, br, param.getCryptedUrl(), null, null);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String json = websiteGetJson();
        Map<String, Object> entries = restoreFromString(json, TypeRef.MAP);
        final String rhxGis = getVarRhxGis(this.br);
        final String usernameURL = new Regex(param.getCryptedUrl(), TYPE_SAVED_OBJECTS).getMatch(0);
        if (usernameURL == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String userID = br.getRegex("profilePage_(\\d+)").getMatch(0);
        if (userID != null) {
            this.addCachedUserID(userID, usernameURL);
        }
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/graphql");
        String nextid = null;
        int savedPostsCount = -1;
        int numberofCrawledPosts = 0;
        int page = 1;
        int decryptedLinksLastSize = 0;
        int decryptedLinksCurrentSize = 0;
        final Qdb qdb = getQueryHash(br, Qdb.QUERY.USER_SAVED);
        final InstagramMetadata metadata = new InstagramMetadata(usernameURL);
        metadata.setPackageName("saved - " + usernameURL);
        do {
            if (page > 1) {
                if (userID == null) {
                    /* This should never happen */
                    logger.warning("Pagination failed because required param 'id_owner' is missing");
                    break;
                }
                final Browser br = this.br.cloneBrowser();
                prepBrAjax(br, qdb);
                final Map<String, Object> vars = new LinkedHashMap<String, Object>();
                vars.put("id", userID);
                vars.put("first", 12);
                vars.put("after", nextid);
                if (qdb == null || qdb.getQueryHash() == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String jsonString = JSonStorage.serializeToJson(vars);
                try {
                    getPageAutoLogin(account, loggedIN, "/graphql/query", param, br, "/graphql/query/?query_hash=" + qdb.getQueryHash() + "&variables=" + URLEncode.encodeURIComponent(jsonString), rhxGis, jsonString);
                } catch (final AccountRequiredException ar) {
                    logger.log(ar);
                    /* Instagram blocks the amount of items a user can see based on */
                    if (loggedIN.get()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, ar);
                    } else {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account required to crawl more items of user " + usernameURL, null, ar);
                    }
                }
                InstaGramCom.checkErrors(this, br);
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                entries = (Map<String, Object>) entries.get("data");
            }
            if (page == 1) {
                savedPostsCount = ((Number) JavaScriptEngineFactory.walkJson(entries, "user/edge_saved_media/count")).intValue();
                if (savedPostsCount == 0) {
                    /* Looks like profile doesn't contain any saved posts. */
                    decryptedLinks.add(this.getDummyDownloadlinkProfileHasNoSavedPosts(usernameURL));
                    return decryptedLinks;
                } else {
                    logger.info("Expected number of saved posts: " + savedPostsCount);
                }
            }
            nextid = (String) JavaScriptEngineFactory.walkJson(entries, "user/edge_saved_media/page_info/end_cursor");
            List<Object> resource_data_list = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "user/edge_saved_media/edges");
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Found no items on page " + page + " --> Stopping decryption");
                break;
            }
            decryptedLinksLastSize = decryptedLinks.size();
            for (final Object o : resource_data_list) {
                final Map<String, Object> result = (Map<String, Object>) o;
                // pages > 0, have a additional nodes entry
                if (result.size() == 1 && result.containsKey("node")) {
                    decryptedLinks.addAll(crawlPost(param, metadata, (Map<String, Object>) result.get("node")));
                } else {
                    decryptedLinks.addAll(crawlPost(param, metadata, result));
                }
            }
            numberofCrawledPosts += resource_data_list.size();
            decryptedLinksCurrentSize = decryptedLinks.size();
            logger.info("Crawled page: " + page + " | Found items: " + decryptedLinksCurrentSize + " | Crawled posts: " + numberofCrawledPosts + "/" + savedPostsCount);
            page++;
        } while (!this.isAbort() && nextid != null && decryptedLinksCurrentSize > decryptedLinksLastSize && decryptedLinksCurrentSize < savedPostsCount);
        return decryptedLinks;
    }

    /**
     * Crawls all items found when looking for a specified items. </br>
     * Max. number of items which this returns can be limited by user setting. </br>
     * Doesn't require the user to be logged in!
     */
    private ArrayList<DownloadLink> crawlHashtagWebsite(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final String hashtag = new Regex(param.getCryptedUrl(), TYPE_HASHTAG).getMatch(0);
        if (hashtag == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        getPageAutoLogin(account, loggedIN, null, param, br, param.getCryptedUrl(), null, null);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Qdb qdb = getQueryHash(br, Qdb.QUERY.USER);
        final Map<String, Object> entries = restoreFromString(websiteGetJson(), TypeRef.MAP);
        final Map<String, Object> data = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "entry_data/TagPage/{0}/data");
        List<Object> resource_data_list = (List<Object>) JavaScriptEngineFactory.walkJson(data, "recent/sections");
        final InstagramMetadata metadata = new InstagramMetadata();
        metadata.setHashtag(hashtag);
        metadata.setPackageName(getPackagenameHashtag(metadata));
        String next_max_id = (String) get(entries, "entry_data/TagPage/{0}/data/recent/next_max_id");
        Boolean more_available = (Boolean) get(entries, "entry_data/TagPage/{0}/data/recent/more_available");
        Number next_page = (Number) get(entries, "entry_data/TagPage/{0}/data/recent/next_page");
        int page = 1;
        int decryptedLinksLastSize = 0;
        final int totalNumberofPosts = ((Number) data.get("media_count")).intValue();
        int crawledNumberofPosts = 0;
        final long maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getHashtagCrawlerMaxItemsLimit();
        do {
            if (more_available && next_page != null && page > 1) {
                final Browser br = this.br.cloneBrowser();
                InstaGramCom.prepBRAltAPI(br);
                final PostRequest postRequest = br.createPostRequest(InstaGramCom.ALT_API_BASE + "/tags/" + hashtag + "/sections/", "include_persistent=0&max_id=" + URLEncode.encodeURIComponent(next_max_id) + "&page=" + next_page.toString() + "&surface=grid&tab=recent");
                prepRequest(br, postRequest, qdb);
                postRequest.getHeaders().put("Origin", "https://www." + this.getHost());
                postRequest.getHeaders().put("Referer", "https://www." + this.getHost());
                br.setCurrentURL("https://www." + this.getHost());
                try {
                    getPageAutoLogin(account, loggedIN, "/api/v1/tags", param, br, postRequest, null, null);
                } catch (final AccountRequiredException ar) {
                    logger.log(ar);
                    /* Instagram blocks the amount of items a user can see based on */
                    if (loggedIN.get()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, ar);
                    } else {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account required to crawl more items of hashtag " + hashtag, null, ar);
                    }
                }
                InstaGramCom.checkErrors(this, br);
                final Map<String, Object> paginationRoot = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                resource_data_list = (List<Object>) JavaScriptEngineFactory.walkJson(paginationRoot, "sections");
                next_max_id = (String) get(paginationRoot, "next_max_id");
                more_available = (Boolean) get(paginationRoot, "more_available");
                next_page = (Number) get(paginationRoot, "next_page");
            }
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Stopping because: Found no new links on current page: " + page);
                break;
            }
            decryptedLinksLastSize = decryptedLinks.size();
            for (final Object o : resource_data_list) {
                final List<Object> medias = (List<Object>) JavaScriptEngineFactory.walkJson(o, "layout_content/medias/");
                if (medias != null) {
                    for (final Object media : medias) {
                        decryptedLinks.addAll(this.crawlPost(param, metadata, (Map<String, Object>) JavaScriptEngineFactory.walkJson(media, "media/")));
                    }
                }
            }
            crawledNumberofPosts += resource_data_list.size();
            logger.info("Crawled page " + page + " | Number of posts crawled so far: " + crawledNumberofPosts + "/" + totalNumberofPosts + " | Items crawled so far: " + decryptedLinks.size());
            if (!Boolean.TRUE.equals(more_available)) {
                logger.info("Stopping because: Reached end");
                break;
            } else if (next_max_id == null) {
                logger.info("Stopping because: Failed to find next_max_id");
                break;
            } else if (next_page == null) {
                logger.info("Stopping because: Failed to find next_page");
                break;
            } else if (decryptedLinks.size() == decryptedLinksLastSize) {
                logger.info("Stopping because: Failed to find any new items this run");
                break;
            } else if (decryptedLinks.size() >= maxItemsLimit) {
                logger.info("Stopping because: Number of items selected in plugin setting has been crawled --> Done");
                break;
            }
            page++;
        } while (!this.isAbort());
        return decryptedLinks;
    }

    private String getPackagenameHashtag(final InstagramMetadata metadata) {
        return "hashtag - " + metadata.getHashtag();
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    /**
     * 2020-11-18: This never worked but we should keep it as there isn't much work left to make this work. </br>
     * Do not update this! Do not delete this! Only modify it if you can make it work!
     */
    private ArrayList<DownloadLink> crawlStoryGraphql(Map<String, Object> entries, final CryptedLink param) throws Exception {
        final boolean functionNotYetDone = true;
        if (functionNotYetDone) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        final String username_url = new Regex(param.getCryptedUrl(), PATTERN_STORY).getMatch(0);
        final String story_user_id = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/StoriesPage/{0}/user/id");
        final Qdb qdb = getQueryHash(br, Qdb.QUERY.STORY);
        if (username_url == null || StringUtils.isEmpty(story_user_id)) {
            /* This should never happen! */
            return ret;
        }
        final Browser br = this.br.cloneBrowser();
        prepBrAjax(br, qdb);
        if (qdb == null || qdb.getQueryHash() == null) {
            logger.warning("Pagination failed because qHash is not given");
            return ret;
        }
        final String url = "/graphql/query/?query_hash=" + qdb.getQueryHash() + "&variables=%7B%22reel_ids%22%3A%5B%22" + story_user_id + "%22%5D%2C%22tag_names%22%3A%5B%5D%2C%22location_ids%22%3A%5B%5D%2C%22highlight_reel_ids%22%3A%5B%5D%2C%22precomposed_overlay%22%3Afalse%2C%22show_story_viewer_list%22%3Atrue%2C%22story_viewer_fetch_count%22%3A50%2C%22story_viewer_cursor%22%3A%22%22%2C%22stories_video_dash_manifest%22%3Afalse%7D";
        getPage(param, br, url, null, null);
        entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "data/reels_media/{0}/items");
        List<Object> qualities;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username_url + " - Story");
        final String subfolderpath = username_url + "/" + "story";
        for (final Object storyO : ressourcelist) {
            entries = (Map<String, Object>) storyO;
            final String story_segment_id = (String) entries.get("id");
            if (StringUtils.isEmpty(story_segment_id)) {
                /* Skip invalid items */
                continue;
            }
            final boolean is_video = ((Boolean) entries.get("is_video")).booleanValue();
            long maxQuality = -1;
            String finallink = null;
            final String filename;
            if (is_video) {
                qualities = (List<Object>) entries.get("video_resources");
                filename = username_url + "_" + story_segment_id + ".mp4";
            } else {
                qualities = (List<Object>) entries.get("display_resources");
                filename = username_url + "_" + story_segment_id + ".jpg";
            }
            for (final Object qualityO : qualities) {
                entries = (Map<String, Object>) qualityO;
                final String finallinkTmp = (String) entries.get("src");
                if (StringUtils.isEmpty(finallinkTmp)) {
                    /* Skip invalid items */
                    continue;
                }
                final long qualityTmp = JavaScriptEngineFactory.toLong(entries.get("config_height"), 0);
                if (qualityTmp > maxQuality) {
                    maxQuality = qualityTmp;
                    finallink = finallinkTmp;
                }
            }
            if (StringUtils.isEmpty(finallink)) {
                /* Skip invalid items */
                continue;
            }
            final DownloadLink dl = this.createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(filename);
            dl.setAvailable(true);
            dl.setRelativeDownloadFolderPath(subfolderpath);
            dl._setFilePackage(fp);
            ret.add(dl);
            distribute(dl);
        }
        return ret;
    }

    private String getTypeName(final int media_type) {
        switch (media_type) {
        case 1:
            return "GraphImage";
        case 2:
            return "GraphVideo";
        case 8:
            return "GraphSidecar";
        default:
            return null;
        }
    }

    @SuppressWarnings({ "unchecked" })
    private ArrayList<DownloadLink> crawlPost(final CryptedLink param, InstagramMetadata metadata, Map<String, Object> post) throws Exception {
        long date = JavaScriptEngineFactory.toLong(post.get("date"), 0);
        if (date == 0) {
            date = JavaScriptEngineFactory.toLong(post.get("taken_at_timestamp"), 0);
            if (date == 0) {
                // api
                date = JavaScriptEngineFactory.toLong(post.get("taken_at"), 0);
            }
        }
        final InstagramConfig cfg = PluginJsonConfig.get(InstagramConfig.class);
        // is this id? // final String linkid_main = (String) entries.get("id");
        String typename = (String) post.get("__typename");
        final Number mediaType = (Number) post.get("media_type");// api
        if (typename == null && mediaType != null) {
            typename = getTypeName(mediaType.intValue());
        }
        if (typename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown media type");
        }
        String mainContentID = (String) post.get("code");
        if (mainContentID == null) {
            /* 2022-03-30: E.g. required when crawling posts in context of user-profile. */
            mainContentID = (String) post.get("shortcode");
        }
        if (StringUtils.isEmpty(mainContentID)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find postID");
        }
        boolean ownerHasPrivateProfile = false;
        if (metadata == null) {
            metadata = new InstagramMetadata();
        }
        metadata.setMainContentID(mainContentID);
        metadata.setDate(new Date(date * 1000));
        /* Find username if it is not pre-given. */
        try {
            Map<String, Object> ownerInfo = (Map<String, Object>) post.get("owner");
            if (ownerInfo == null) {
                // api
                ownerInfo = (Map<String, Object>) post.get("user");
            }
            String userID = StringUtils.valueOfOrNull(ownerInfo.get("id"));
            if (userID == null) {
                // api
                userID = StringUtils.valueOfOrNull(ownerInfo.get("pk"));
            }
            if (userID == null) {
                /* Should always be given! */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Boolean ownerHasPrivateProfileO = (Boolean) ownerInfo.get("is_private");
            if (ownerHasPrivateProfileO != null) {
                ownerHasPrivateProfile = ownerHasPrivateProfileO.booleanValue();
            }
            /* Check if username is in json */
            String username = (String) ownerInfo.get("username");
            if (username != null) {
                /* Cache information for later usage just in case it isn't present in json the next time. */
                addCachedUserID(userID, username);
                metadata.setUsername(username);
            } else {
                /* Find username "the hard way" */
                /* TODO: Not sure if this is still needed */
                /* Check if we got this username cached */
                synchronized (ID_TO_USERNAME) {
                    username = this.getCachedUserName(userID);
                    if (username == null) {
                        /* HTTP request needed to find username! */
                        username = this.getUsernameFromUserIDAltAPI(br, userID);
                        if (username == null) {
                            logger.warning("WTF failed to find username for userID: " + userID);
                        }
                    }
                }
                if (username != null) {
                    metadata.setUsername(username);
                }
            }
        } catch (final Throwable ignore) {
            logger.log(ignore);
            logger.warning("Error happened during username lookup");
        }
        final Object captionO = post.get("caption");
        String description = null;
        if (captionO != null) {
            /* E.g. single post */
            if (captionO instanceof String) {
                description = captionO.toString();
            } else if (captionO instanceof Map) {
                // api
                description = (String) JavaScriptEngineFactory.walkJson(captionO, "text");
            }
        }
        if (description == null) {
            /* E.g. single post in context of user-profile */
            try {
                final Map<String, Object> edge_media_to_caption = ((Map<String, Object>) post.get("edge_media_to_caption"));
                final List<Map<String, Object>> edges = (List<Map<String, Object>>) edge_media_to_caption.get("edges");
                if (edges.size() > 0) {
                    final Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
                    description = (String) node.get("text");
                }
            } catch (final Throwable ignore) {
            }
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<Map<String, Object>> mediaItems = new ArrayList<Map<String, Object>>();
        if (typename.equalsIgnoreCase("GraphImage")) {
            /* Single image */
            mediaItems.add(post);
        } else if (typename.equalsIgnoreCase("GraphVideo")) {
            /* Single video */
            mediaItems.add(post);
        } else if (typename.equalsIgnoreCase("GraphSidecar")) {
            /* Process multiple media items posted in a single IG post. */
            List<Map<String, Object>> medias = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(post, "edge_sidecar_to_children/edges");
            if (medias == null) {
                // api
                medias = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(post, "carousel_media");
            }
            mediaItems.addAll(medias);
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unsupported media type: " + typename);
        }
        String coauthor_producers_comma_separated = "";
        final List<Map<String, Object>> coauthor_producers = (List<Map<String, Object>>) post.get("coauthor_producers");
        if (coauthor_producers != null) {
            for (final Map<String, Object> coauthor_producer : coauthor_producers) {
                if (coauthor_producers_comma_separated.length() > 0) {
                    coauthor_producers_comma_separated += ",";
                }
                coauthor_producers_comma_separated += coauthor_producer.get("username");
            }
        }
        /* Create FilePackage if packagename is available */
        final FilePackage fp = getFilePackageForGallery(metadata);
        final String postURL = this.generateURLPost(mainContentID);
        final int padLength = StringUtils.getPadLength(mediaItems.size());
        int orderID = 1;
        for (Map<String, Object> media : mediaItems) {
            final Object nodeO = media.get("node");
            if (nodeO != null) {
                /* 2022-03-30: E.g. media as part of user profile */
                media = (Map<String, Object>) nodeO;
            }
            final String internalMediaID = media.get("id").toString();
            final String shortcode = (String) media.get("shortcode");
            final Number thisMediaType = (Number) media.get("media_type");// api
            final boolean isVideo = Boolean.TRUE.equals(media.get("is_video")) || (thisMediaType != null && thisMediaType.intValue() == 2);
            String dllink = null;
            if (isVideo) {
                dllink = (String) media.get("video_url");
            } else {
                /* Find best image-quality */
                final List<Object> ressourcelist = (List<Object>) media.get("display_resources");
                if (ressourcelist != null) {
                    long qualityMax = 0;
                    for (final Object qualityO : ressourcelist) {
                        final Map<String, Object> imageQualityInfo = (Map<String, Object>) qualityO;
                        final long widthTmp = JavaScriptEngineFactory.toLong(imageQualityInfo.get("config_width"), 0);
                        if (widthTmp > qualityMax && imageQualityInfo.containsKey("src")) {
                            qualityMax = widthTmp;
                            dllink = (String) imageQualityInfo.get("src");
                        }
                    }
                }
                if (StringUtils.isEmpty(dllink)) {
                    dllink = (String) media.get("display_src");
                    if (StringUtils.isEmpty(dllink)) {
                        dllink = (String) media.get("display_url");
                        if (StringUtils.isEmpty(dllink)) {
                            dllink = (String) media.get("thumbnail_src");
                        }
                    }
                }
            }
            boolean isFromAltAPI = false;
            if (StringUtils.isEmpty(dllink)) {
                try {
                    dllink = InstaGramCom.getBestQualityURLAltAPI(media);
                    isFromAltAPI = true;
                } catch (final Throwable ignore) {
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find directurl");
            }
            /* 2022-03-30: Shouldn't be required anymore as we crawl story elements separately. */
            final boolean isPartOfStory = Boolean.TRUE.equals(media.get("is_reel_media"));
            final DownloadLink dl = this.createDownloadlink(postURL);
            dl.setAvailable(true);
            dl.setProperty(InstaGramCom.PROPERTY_main_content_id, mainContentID);
            dl.setProperty(InstaGramCom.PROPERTY_date, metadata.getDateFormatted());
            if (!StringUtils.isEmpty(shortcode)) {
                dl.setProperty(InstaGramCom.PROPERTY_shortcode, shortcode);
            }
            dl.setProperty(InstaGramCom.PROPERTY_DIRECTURL, dllink);
            if (isFromAltAPI) {
                dl.setProperty(InstaGramCom.PROPERTY_has_tried_to_crawl_original_url, true);
            }
            if (!StringUtils.isEmpty(description)) {
                dl.setComment(description);
                dl.setProperty(InstaGramCom.PROPERTY_description, description);
            }
            dl.setProperty(InstaGramCom.PROPERTY_orderid, String.format(Locale.US, "%0" + padLength + "d", orderID));
            dl.setProperty(InstaGramCom.PROPERTY_orderid_raw, orderID);
            dl.setProperty(InstaGramCom.PROPERTY_orderid_max_raw, mediaItems.size());
            dl.setProperty(InstaGramCom.PROPERTY_internal_media_id, internalMediaID);
            if (!StringUtils.isEmpty(metadata.getUsername())) {
                /* Packagizer Property */
                dl.setProperty(InstaGramCom.PROPERTY_uploader, metadata.getUsername());
            }
            if (!StringUtils.isEmpty(coauthor_producers_comma_separated)) {
                dl.setProperty(InstaGramCom.PROPERTY_coauthor_producers_comma_separated, coauthor_producers_comma_separated);
            }
            if (isPartOfStory) {
                dl.setProperty(InstaGramCom.PROPERTY_is_part_of_story, true);
                /* There are no individual public URLs to directly link elements of a story --> Use URL user had added. */
                dl.setContentUrl(param.getCryptedUrl());
            } else {
                dl.setContentUrl(postURL);
            }
            if (ownerHasPrivateProfile) {
                dl.setProperty(InstaGramCom.PROPERTY_is_private, true);
            }
            if (isVideo) {
                dl.setProperty(InstaGramCom.PROPERTY_type, "video");
            } else {
                dl.setProperty(InstaGramCom.PROPERTY_type, "photo");
            }
            final String filename = getFilename(this, dl);
            dl.setFinalFileName(filename);
            dl._setFilePackage(fp);
            ret.add(dl);
            distribute(dl);
            orderID++;
        }
        if (cfg.isPostCrawlerAddPostDescriptionAsTextfile() && !StringUtils.isEmpty(description)) {
            /* Download picture-description as .txt file */
            ret.add(getTextDownloadlink(ret.get(0)));
        }
        return ret;
    }

    private FilePackage getFilePackageForGallery(final InstagramMetadata metadata) {
        FilePackage fp = metadata.getFilePackage();
        if (fp == null) {
            /* Fallback + handling for single posts */
            final InstagramConfig cfg = PluginJsonConfig.get(InstagramConfig.class);
            fp = FilePackage.getInstance();
            if (cfg.getPostCrawlerPackagenameSchemeType() == SinglePostPackagenameSchemeType.UPLOADER && metadata.getUsername() != null) {
                fp.setName(metadata.getUsername());
            } else if (cfg.getPostCrawlerPackagenameSchemeType() == SinglePostPackagenameSchemeType.UPLOADER_MAIN_CONTENT_ID && metadata.getUsername() != null) {
                fp.setName(metadata.getUsername() + " - " + metadata.getMainContentID());
            } else if (cfg.getPostCrawlerPackagenameSchemeType() == SinglePostPackagenameSchemeType.CUSTOM && !StringUtils.isEmpty(cfg.getPostCrawlerPackagenameScheme())) {
                /* Use User defined package names */
                String customPackageName = cfg.getPostCrawlerPackagenameScheme().replace("*date*", metadata.getDateFormatted());
                final String usernameForReplacer;
                if (metadata.getUsername() != null) {
                    usernameForReplacer = metadata.getUsername();
                } else {
                    usernameForReplacer = "-";
                }
                if (metadata.getUsername() != null) {
                    customPackageName = customPackageName.replace("*uploader*", usernameForReplacer);
                }
                customPackageName = customPackageName.replace("*main_content_id*", metadata.getMainContentID());
                fp.setName(customPackageName);
            } else {
                /* Fallback */
                fp.setName(metadata.getMainContentID());
            }
        }
        return fp;
    }

    /** Crawls json objects of stories/stories-highlights */
    private ArrayList<DownloadLink> crawlPostAltAPI(final CryptedLink param, final InstagramMetadata metadata, Map<String, Object> item) throws Exception {
        String mainMediaID = StringUtils.valueOfOrNull(item.get("id"));
        if (mainMediaID == null) {
            if (item.containsKey("media") && item.size() == 1) {
                item = (Map<String, Object>) item.get("media");
                mainMediaID = StringUtils.valueOfOrNull(item.get("id"));
            }
            if (mainMediaID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Map<String, Object> user = (Map<String, Object>) item.get("user");
        final String username = user.get("username").toString();
        final Object reel_typeO = item.get("reel_type");
        final InstagramConfig cfg = PluginJsonConfig.get(InstagramConfig.class);
        if (reel_typeO != null) {
            /* Story */
            final String reel_type = reel_typeO.toString();
            final List<Map<String, Object>> mediaItems = (List<Map<String, Object>>) item.get("items");
            final long created_at;
            final String packagenameScheme;
            String title = "";
            /* Different packagenames for different types of stories and based on user plugin settings. */
            if (reel_type.equals("user_reel")) {
                /* Story */
                created_at = ((Number) item.get("latest_reel_media")).longValue();
                final StoryPackagenameSchemeType schemeType = cfg.getStoryPackagenameSchemeType();
                final String customPackagenameScheme = cfg.getStoryPackagenameScheme();
                if (schemeType == StoryPackagenameSchemeType.DEFAULT_1 || StringUtils.isEmpty(customPackagenameScheme)) {
                    packagenameScheme = "story - *uploader*";
                } else {
                    packagenameScheme = customPackagenameScheme;
                }
            } else if (reel_type.equals("highlight_reel")) {
                /* Story highlight */
                created_at = ((Number) item.get("created_at")).longValue();
                title = item.get("title").toString();
                final StoriesHighlightsPackagenameSchemeType schemeType = cfg.getStoriesHighlightsPackagenameSchemeType();
                final String customPackagenameScheme = cfg.getStoriesHighlightsPackagenameScheme();
                if (schemeType == StoriesHighlightsPackagenameSchemeType.DEFAULT_1 || StringUtils.isEmpty(customPackagenameScheme)) {
                    packagenameScheme = "story highlights - *uploader* - *title*";
                } else {
                    packagenameScheme = customPackagenameScheme;
                }
            } else {
                /* Unknown/Unsupported reel_type */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Build packagename */
            String packagename = packagenameScheme.replace("*date*", new SimpleDateFormat("yyyy-MM-dd").format(new Date(created_at * 1000)));
            packagename = packagename.replace("*uploader*", username);
            packagename = packagename.replace("*title*", title);
            /* Set packagename */
            final FilePackage fp = getFilePackage();
            fp.setName(packagename);
            final int padLength = StringUtils.getPadLength(mediaItems.size());
            int orderID = 1;
            for (final Map<String, Object> media : mediaItems) {
                final String mediaID = media.get("id").toString();
                final String contentURL;
                if (reel_type.equals("user_reel")) {
                    /* Story: Individual images can be linked individually */
                    contentURL = "https://www." + this.getHost() + "/stories/" + username + "/" + mediaID.split("_")[0] + "/";
                } else if (reel_type.equals("highlight_reel")) {
                    /* Story highlights: Same URL for all items */
                    contentURL = generateStoryHighlightURL(mainMediaID);
                } else {
                    /* Unknown/Unsupported reel_type */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final long date = ((Number) media.get("taken_at")).longValue();
                final String internalMediaID = media.get("id").toString();
                /* Same ID as for public posts but cannot be used e.g. to direct-link this item for the user to open in browser. */
                final String postIDInternal = (String) media.get("code");
                final Number thisMediaType = (Number) media.get("media_type");
                final String directurl = InstaGramCom.getBestQualityURLAltAPI(media);
                final DownloadLink dl = this.createDownloadlink(this.generateURLPost(postIDInternal));
                dl.setContentUrl(contentURL);
                dl.setAvailable(true);
                dl.setProperty(InstaGramCom.PROPERTY_main_content_id, postIDInternal);
                final String dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(new Date(date * 1000));
                dl.setProperty(InstaGramCom.PROPERTY_date, dateFormatted);
                if (!StringUtils.isEmpty(directurl)) {
                    dl.setProperty(InstaGramCom.PROPERTY_DIRECTURL, directurl);
                    dl.setProperty(InstaGramCom.PROPERTY_has_tried_to_crawl_original_url, true);
                }
                dl.setProperty(InstaGramCom.PROPERTY_orderid, String.format(Locale.US, "%0" + padLength + "d", orderID));
                dl.setProperty(InstaGramCom.PROPERTY_orderid_raw, orderID);
                dl.setProperty(InstaGramCom.PROPERTY_orderid_max_raw, mediaItems.size());
                dl.setProperty(InstaGramCom.PROPERTY_internal_media_id, internalMediaID);
                dl.setProperty(InstaGramCom.PROPERTY_uploader, username);
                dl.setProperty(InstaGramCom.PROPERTY_is_part_of_story, true);
                if (thisMediaType.intValue() == 2) {
                    dl.setProperty(InstaGramCom.PROPERTY_type, "video");
                } else {
                    dl.setProperty(InstaGramCom.PROPERTY_type, "photo");
                }
                dl.setFinalFileName(getFilename(this, dl));
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                orderID++;
            }
        } else {
            /* Post */
            /* Same ID as for public posts but cannot be used e.g. to direct-link this item for the user to open in browser. */
            final String postID = (String) item.get("code");
            final Map<String, Object> caption = (Map<String, Object>) item.get("caption");
            final String description = caption != null ? (String) caption.get("text") : null;
            final List<Map<String, Object>> mediaItems = new ArrayList<Map<String, Object>>();
            final long date = ((Number) item.get("taken_at")).longValue();
            final List<Map<String, Object>> carousel_media = (List<Map<String, Object>>) item.get("carousel_media");
            if (carousel_media != null) {
                /* Post contains multiple items */
                mediaItems.addAll(carousel_media);
            } else {
                /* Post contains single item */
                mediaItems.add(item);
            }
            final int padLength = StringUtils.getPadLength(mediaItems.size());
            int orderID = 1;
            final FilePackage fp = this.getFilePackageForGallery(metadata);
            for (final Map<String, Object> media : mediaItems) {
                final Number thisMediaType = (Number) media.get("media_type");
                final String dllink = InstaGramCom.getBestQualityURLAltAPI(media);
                final String contentURL = this.generateURLPost(postID);
                final DownloadLink dl = this.createDownloadlink(contentURL);
                dl.setContentUrl(contentURL);
                dl.setAvailable(true);
                dl.setProperty(InstaGramCom.PROPERTY_main_content_id, postID);
                final String dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(new Date(date * 1000));
                dl.setProperty(InstaGramCom.PROPERTY_date, dateFormatted);
                if (!StringUtils.isEmpty(dllink)) {
                    dl.setProperty(InstaGramCom.PROPERTY_DIRECTURL, dllink);
                    dl.setProperty(InstaGramCom.PROPERTY_has_tried_to_crawl_original_url, true);
                }
                dl.setProperty(InstaGramCom.PROPERTY_orderid, String.format(Locale.US, "%0" + padLength + "d", orderID));
                dl.setProperty(InstaGramCom.PROPERTY_orderid_raw, orderID);
                dl.setProperty(InstaGramCom.PROPERTY_orderid_max_raw, mediaItems.size());
                dl.setProperty(InstaGramCom.PROPERTY_internal_media_id, media.get("id").toString());
                dl.setProperty(InstaGramCom.PROPERTY_uploader, username);
                if (thisMediaType.intValue() == 2) {
                    dl.setProperty(InstaGramCom.PROPERTY_type, "video");
                } else {
                    dl.setProperty(InstaGramCom.PROPERTY_type, "photo");
                }
                if (!StringUtils.isEmpty(description)) {
                    dl.setProperty(InstaGramCom.PROPERTY_description, description);
                }
                dl.setFinalFileName(getFilename(this, dl));
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                decryptedLinks.add(dl);
                distribute(dl);
                orderID++;
            }
            if (cfg.isPostCrawlerAddPostDescriptionAsTextfile() && !StringUtils.isEmpty(description)) {
                /* Download picture-description as .txt file */
                decryptedLinks.add(getTextDownloadlink(decryptedLinks.get(0)));
            }
        }
        return decryptedLinks;
    }

    /**
     * Returns DownloadLink object for text download of Instagram posts. Sets filename based on given list of DownloadLink objects
     * previously crawled for current Instagram post.
     */
    private DownloadLink getTextDownloadlink(final DownloadLink videoItem) throws Exception {
        final DownloadLink text = this.createDownloadlink(videoItem.getPluginPatternMatcher() + "_description");
        /* Add all properties from previously added media. */
        text.setProperties(videoItem.getProperties());
        /* Remove properties that are not relevant or even wrong for our text item. */
        text.removeProperty(InstaGramCom.PROPERTY_orderid);
        text.removeProperty(InstaGramCom.PROPERTY_orderid_raw);
        text.removeProperty(InstaGramCom.PROPERTY_orderid_max_raw);
        text.removeProperty(InstaGramCom.PROPERTY_shortcode);
        text.removeProperty(InstaGramCom.PROPERTY_DIRECTURL);
        text.setProperty(InstaGramCom.PROPERTY_type, "text");
        /* Important!! This will be used in LinkID later! */
        final String mainContentID = videoItem.getStringProperty(InstaGramCom.PROPERTY_main_content_id);
        final String description = videoItem.getStringProperty(InstaGramCom.PROPERTY_description);
        text.setProperty(InstaGramCom.PROPERTY_internal_media_id, mainContentID);
        final String filename = getFilename(this, text);
        text.setFinalFileName(filename);
        try {
            text.setDownloadSize(description.getBytes("UTF-8").length);
        } catch (final UnsupportedEncodingException ignore) {
            ignore.printStackTrace();
        }
        if (videoItem.getFilePackage() != null) {
            /* Put this textfile into the same package the other posts will go into. */
            text._setFilePackage(videoItem.getFilePackage());
        }
        text.setAvailable(true);
        text.setComment(description);
        distribute(text);
        return text;
    }

    /** Returns filename for single media items --> Video/image as part of post/story. */
    public static String getFilename(Plugin plugin, final DownloadLink link) throws Exception {
        /* Check for pre-defined filename */
        if (link.hasProperty(InstaGramCom.PROPERTY_filename_from_crawler)) {
            return link.getStringProperty(InstaGramCom.PROPERTY_filename_from_crawler);
        }
        final String dateFormatted = link.getStringProperty(InstaGramCom.PROPERTY_date);
        final String username = link.getStringProperty(InstaGramCom.PROPERTY_uploader);
        final int orderid = link.getIntegerProperty(InstaGramCom.PROPERTY_orderid_raw);
        final int orderid_max = link.getIntegerProperty(InstaGramCom.PROPERTY_orderid_max_raw);
        final String orderidFormatted = link.getStringProperty(InstaGramCom.PROPERTY_orderid);
        final String mainContentID = link.getStringProperty(InstaGramCom.PROPERTY_main_content_id);
        final String shortcode = link.getStringProperty(InstaGramCom.PROPERTY_shortcode);
        final String server_filename;
        final String directurl = link.getStringProperty(InstaGramCom.PROPERTY_DIRECTURL);
        if (directurl != null) {
            server_filename = InstaGramCom.getFileNameFromURL(plugin, new URL(directurl));
        } else {
            server_filename = null;
        }
        final String ext;
        if (InstaGramCom.isText(link)) {
            ext = ".txt";
        } else if (InstaGramCom.isVideo(link)) {
            ext = ".mp4";
        } else {
            final String serverFileNameExtension = Files.getExtension(server_filename);
            ext = "." + (serverFileNameExtension != null ? serverFileNameExtension : "jpg");
        }
        final InstagramConfig cfg = PluginJsonConfig.get(InstagramConfig.class);
        final FilenameType ft = cfg.getFilenameType();
        final String filenameScheme = cfg.getFilenameScheme();
        if (ft == FilenameType.SERVER && server_filename != null) {
            /* Server filename is preferred by user && available. */
            return server_filename;
        } else if (ft == FilenameType.CUSTOM && !StringUtils.isEmpty(filenameScheme)) {
            /* Build filename according to user defined filename scheme. */
            // *date*_*uploader* - *main_content_id* *orderid*_of_*orderid_max* - *shortcode*.*ext*
            String filename = filenameScheme.replace("*date*", dateFormatted);
            filename = filename.replace("*uploader*", username != null ? username : "-");
            filename = filename.replace("*orderid*", orderidFormatted != null ? orderidFormatted : "-");
            filename = filename.replace("*main_content_id*", mainContentID);
            filename = filename.replace("*orderid_max*", Integer.toString(orderid_max));
            filename = filename.replace("*shortcode*", shortcode != null ? shortcode : "-");
            filename = filename.replace("*ext*", ext);
            return filename;
        } else {
            /* Default/auto filenames */
            String filename;
            if (PluginJsonConfig.get(InstagramConfig.class).isAddDateToFilenames() && dateFormatted != null) {
                filename = dateFormatted + "_";
            } else {
                filename = "";
            }
            /*
             * 2022-01-28: For some items, individual ID is available when user is not logged in but isn't available when user is logged in.
             */
            final boolean individualShortcodeIsAvailable;
            if (!StringUtils.isEmpty(shortcode) && !shortcode.equals(mainContentID)) {
                individualShortcodeIsAvailable = true;
            } else {
                individualShortcodeIsAvailable = false;
            }
            if (link.hasProperty(InstaGramCom.PROPERTY_is_part_of_story)) {
                /* Use slightly different filenames for items that are part of a users' story */
                filename += username;
                if (orderidFormatted != null && PluginJsonConfig.get(InstagramConfig.class).isAddOrderidToFilenames()) {
                    /* By default: Include orderid whenever it is given to prevent duplicate filenames for different files! */
                    filename += " - " + orderidFormatted;
                }
                filename += " - " + mainContentID;
                if (individualShortcodeIsAvailable) {
                    filename += "_" + shortcode;
                }
            } else {
                final String hashtag = link.getStringProperty(InstaGramCom.PROPERTY_hashtag);
                if (!StringUtils.isEmpty(hashtag)) {
                    filename = hashtag + " - ";
                }
                if (!StringUtils.isEmpty(username)) {
                    filename += username + " - ";
                }
                filename += mainContentID;
                final boolean tryToAddOrderIDToFilename;
                if (PluginJsonConfig.get(InstagramConfig.class).isAddOrderidToFilenames()) {
                    tryToAddOrderIDToFilename = true;
                } else if (!individualShortcodeIsAvailable) {
                    /* Enforce this otherwise we might get same filenames for all items! */
                    tryToAddOrderIDToFilename = true;
                } else {
                    tryToAddOrderIDToFilename = false;
                }
                final boolean includeShortcodesInFilename;
                if (tryToAddOrderIDToFilename) {
                    /* Respect user-setting */
                    if (orderidFormatted != null && (orderid > 1 || orderid_max > 1)) {
                        /*
                         * Only add orderID to filename if mediaItem was added as part of multiple items e.g. story/post with more than 1
                         * items.
                         */
                        filename += " - " + orderidFormatted;
                    }
                    includeShortcodesInFilename = PluginJsonConfig.get(InstagramConfig.class).isAddShortcodeToFilenames();
                } else {
                    /* Force-add shortcode to filename in this case otherwise we might not get unique filenames! */
                    includeShortcodesInFilename = true;
                }
                if (individualShortcodeIsAvailable && includeShortcodesInFilename) {
                    filename += "_" + shortcode;
                }
            }
            filename += ext;
            return filename;
        }
    }

    /** Crawls all saved objects of a user. */
    private ArrayList<DownloadLink> crawlUserSavedObjects(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* 2020-11-19: Prefer API as pagination is broken in website method. */
        final boolean preferAPI = true;
        if (preferAPI) {
            return this.crawlUserSavedObjectsFeedAltAPI(param, account, loggedIN);
        } else {
            return this.crawlUserSavedObjectsWebsite(param, account, loggedIN);
        }
    }

    /*************************************************
     * Methods using alternative API below. All of these require the user to be logged in!
     ***************************************************/
    /**
     * Crawls all saved items of currently logged-in account: https://www.instagram.com/username/saved/ </br>
     * Users can save any post: Their own ones or even posts of other users.
     */
    private ArrayList<DownloadLink> crawlUserSavedObjectsFeedAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final String usernameOwnerOfSavedItems = new Regex(param.getCryptedUrl(), TYPE_SAVED_OBJECTS).getMatch(0);
        if (usernameOwnerOfSavedItems == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Login is mandatory to be able to crawl this linktype! */
        loginOrFail(account, loggedIN);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        InstaGramCom.prepBRAltAPI(this.br);
        Map<String, Object> entries;
        String nextid = null;
        int page = 1;
        int numberofCrawledPosts = 0;
        final String savedItemsFeedBaseURL = InstaGramCom.ALT_API_BASE + "/feed/saved/";
        final InstagramMetadata metadata = new InstagramMetadata();
        metadata.setPackageName("saved - " + usernameOwnerOfSavedItems);
        do {
            logger.info("Crawling page: " + page);
            if (page == 1) {
                /**
                 * This may return a varying amount of items e.g. 84 items on the first request, 9 after the next - this is decided
                 * serverside!
                 */
                InstaGramCom.getPageAltAPI(account, this.br, savedItemsFeedBaseURL);
            } else {
                // br.getPage(hashtagBaseURL + "?after=" + nextid);
                InstaGramCom.getPageAltAPI(account, this.br, savedItemsFeedBaseURL + "?max_id=" + nextid);
            }
            entries = restoreFromString(br.toString(), TypeRef.MAP);
            final int numberofitemsOnThisPage = (int) JavaScriptEngineFactory.toLong(entries.get("num_results"), 0);
            if (numberofitemsOnThisPage == 0) {
                if (page == 1) {
                    decryptedLinks.add(getDummyDownloadlinkProfileHasNoSavedPosts(usernameOwnerOfSavedItems));
                    return decryptedLinks;
                } else {
                    logger.info("Stopping because: 0 items available ...");
                    break;
                }
            }
            nextid = (String) entries.get("next_max_id");
            final boolean more_available = ((Boolean) entries.get("more_available"));
            final List<Map<String, Object>> mediaItems = (List<Map<String, Object>>) entries.get("items");
            if (mediaItems.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            decryptedLinks.addAll(this.crawlPostListAltAPI(param, mediaItems, metadata));
            numberofCrawledPosts += numberofitemsOnThisPage;
            logger.info("Crawled page: " + page + " | Crawled posts so far: " + numberofCrawledPosts);
            if (!more_available) {
                logger.info("Stopping because: more_available == false");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because: No nextid available");
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }

    private boolean canBeProcessedByCrawlUserAltAPI(final String url) {
        if (url.matches(TYPE_PROFILE_TAGGED)) {
            return true;
        } else if (url.matches(TYPE_PROFILE)) {
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<DownloadLink> crawlHashtag(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final InstagramConfig cfg = PluginJsonConfig.get(InstagramConfig.class);
        if (cfg.getHashtagCrawlerMaxItemsLimit() == 0) {
            logger.info("Stopping because: User has disabled hashtag crawling");
            return new ArrayList<DownloadLink>();
        }
        if (loggedIN.get() || account != null) {
            return this.crawlHashtagAltAPI(param, account, loggedIN);
        } else {
            return this.crawlHashtagWebsite(param, account, loggedIN);
        }
    }

    private ArrayList<DownloadLink> crawlHashtagAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final String hashtag = new Regex(param.getCryptedUrl(), TYPE_HASHTAG).getMatch(0);
        if (hashtag == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(account, this.br, InstaGramCom.ALT_API_BASE + "/tags/" + hashtag + "/info/");
        Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final long totalNumberofPosts = JavaScriptEngineFactory.toLong(entries.get("media_count"), 0);
        if (totalNumberofPosts == 0) {
            ret.add(this.createOfflinelink(param.getCryptedUrl(), "No items available for this tag: " + hashtag, "No items available for this tag: " + hashtag));
            return ret;
        }
        final InstagramMetadata metadata = new InstagramMetadata();
        metadata.setHashtag(hashtag);
        metadata.setPackageName(this.getPackagenameHashtag(metadata));
        String nextid = null;
        int page = 1;
        int numberofCrawledPostsTotal = 0;
        final String baseURL = InstaGramCom.ALT_API_BASE + "/feed/tag/" + hashtag + "/";
        final long maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getHashtagCrawlerMaxItemsLimit();
        do {
            if (page == 1) {
                /*
                 * Returns a lot of items on first access and then a lot less e.g. 84 on first request, then 8-9 on each subsequent request.
                 */
                InstaGramCom.getPageAltAPI(account, this.br, baseURL);
            } else {
                InstaGramCom.getPageAltAPI(account, this.br, baseURL + "?max_id=" + nextid);
            }
            entries = restoreFromString(br.toString(), TypeRef.MAP);
            final int numberofPostsOnThisPage = (int) JavaScriptEngineFactory.toLong(entries.get("num_results"), 0);
            if (numberofPostsOnThisPage == 0) {
                /* Rare case */
                logger.info("Stopping because: 0 items available ...");
                return ret;
            }
            nextid = (String) entries.get("next_max_id");
            final List<Map<String, Object>> resource_data_list = (List<Map<String, Object>>) entries.get("items");
            if (resource_data_list.size() == 0) {
                /* Should never happen(?) */
                logger.info("Stopping because: Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            for (final Map<String, Object> post : resource_data_list) {
                ret.addAll(this.crawlPostAltAPI(param, metadata, post));
            }
            numberofCrawledPostsTotal += numberofPostsOnThisPage;
            logger.info("Crawled page: " + page + " | Crawled posts so far: " + numberofCrawledPostsTotal + "/" + totalNumberofPosts);
            if (!((Boolean) entries.get("more_available"))) {
                logger.info("Stopping because: More_available == false");
                break;
            } else if (numberofCrawledPostsTotal >= totalNumberofPosts) {
                logger.info("Stopping because: Found number of items is higher or equal to expected number of items");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because: No nextid available");
                break;
            } else if (numberofCrawledPostsTotal >= maxItemsLimit) {
                logger.info("Stopping because: Reached user defined max items limit of " + maxItemsLimit);
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        return ret;
    }

    private ArrayList<DownloadLink> crawlProfileTaggedAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        final String username = new Regex(param.getCryptedUrl(), TYPE_PROFILE_TAGGED).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String userID = this.findUserID(param, account, loggedIN, username);
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        InstaGramCom.prepBRAltAPI(this.br);
        int totalNumberofPosts = -1;
        final InstagramMetadata metadata = new InstagramMetadata();
        metadata.setPackageName(username + " - tagged");
        String nextid = null;
        int page = 1;
        int numberofCrawledPostsTotal = 0;
        final String baseURL = InstaGramCom.ALT_API_BASE + "/usertags/" + userID + "/feed/";
        final UrlQuery query = new UrlQuery();
        query.add("count", "12");
        final int maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getProfileTaggedCrawledMaxItemsLimit();
        do {
            if (page > 1) {
                query.addAndReplace("max_id", Encoding.urlEncode(nextid));
            }
            InstaGramCom.getPageAltAPI(account, this.br, baseURL + "?" + query.toString());
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            final List<Map<String, Object>> resource_data_list = (List<Map<String, Object>>) entries.get("items");
            final int numberofitemsOnThisPage = resource_data_list.size();
            if (page == 1) {
                totalNumberofPosts = ((Number) entries.get("total_count")).intValue();
                /* Do not(!) Check against 'total_count' here as this can be 12 on first page even though 0 items are available! */
                if (numberofitemsOnThisPage == 0) {
                    ret.add(this.createOfflinelink(param.getCryptedUrl(), "USER_IS_NOT_TAGGED_ANYWHERE_" + username, "The following user is not tagged anywhere: " + username));
                    return ret;
                }
            }
            if (numberofitemsOnThisPage == 0) {
                /* Rare case */
                logger.info("Stopping because: 0 items available ...");
                return ret;
            }
            nextid = entries.get("next_max_id").toString();
            final boolean more_available = ((Boolean) entries.get("more_available"));
            if (resource_data_list.size() == 0) {
                /* Should never happen(?) */
                logger.info("Stopping because: Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            ret.addAll(this.crawlPostListAltAPI(param, resource_data_list, metadata));
            numberofCrawledPostsTotal += numberofitemsOnThisPage;
            logger.info("Crawled page: " + page + " | Crawled posts so far: " + numberofCrawledPostsTotal + "/" + totalNumberofPosts);
            if (!more_available) {
                logger.info("Stopping because: More_available == false");
                break;
            } else if (numberofCrawledPostsTotal >= totalNumberofPosts) {
                logger.info("Stopping because: Found number of items is higher or equal to expected number of items");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because: No nextid available");
                break;
            } else if (numberofCrawledPostsTotal >= maxItemsLimit) {
                logger.info("Stopping because: Reached user defined max items limit of " + maxItemsLimit);
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        return ret;
    }

    /** Crawls: https://www.instagram.com/stories/highlights/<numbers>/ */
    private ArrayList<DownloadLink> crawlStoryHighlight(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final String reelID = new Regex(param.getCryptedUrl(), TYPE_STORY_HIGHLIGHTS).getMatch(0);
        if (reelID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Login is required to crawl such elements! */
        this.loginOrFail(account, loggedIN);
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(account, this.br, InstaGramCom.ALT_API_BASE + "/feed/reels_media/?reel_ids=highlight%3A" + reelID);
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final InstagramMetadata metadata = new InstagramMetadata();
        final Map<String, Object> reel = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "reels/highlight:" + reelID);
        return this.crawlPostAltAPI(param, metadata, reel);
    }

    private ArrayList<DownloadLink> crawlStory(final CryptedLink param, final Account account, final AtomicBoolean loggedIN, final boolean addDummyItemOnNoItemsFound) throws UnsupportedEncodingException, Exception {
        final String username = new Regex(param.getCryptedUrl(), PATTERN_STORY).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return crawlStory(param, username, account, loggedIN, addDummyItemOnNoItemsFound);
    }

    /**
     * Crawls story of given username. </br>
     *
     * @param handleErrors
     *            true = throw exception on error e.g. if no account is given and add dummy item if user has no story. </br>
     *            false = return empty array on error
     */
    private ArrayList<DownloadLink> crawlStory(final CryptedLink param, final String username, final Account account, final AtomicBoolean loggedIN, final boolean handleErrors) throws UnsupportedEncodingException, Exception {
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (account == null && !handleErrors) {
            return new ArrayList<DownloadLink>();
        }
        /* We need to be loggedIN to be able to see stories of users! */
        this.loginOrFail(account, loggedIN);
        final InstagramMetadata metadata = new InstagramMetadata(username);
        final String userID = findUserID(param, account, loggedIN, username);
        if (StringUtils.isEmpty(userID)) {
            /* Most likely that profile doesn't exist */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(getDummyDownloadlinkProfileOffline(username));
            return ret;
        }
        InstaGramCom.prepBRAltAPI(this.br);
        /* Alternative endpoint (website): https://i.instagram.com/api/v1/feed/user/<userID>/story/ */
        InstaGramCom.getPageAltAPI(account, this.br, InstaGramCom.ALT_API_BASE + "/feed/user/" + userID + "/reel_media/");
        final Map<String, Object> reel_media = restoreFromString(br.toString(), TypeRef.MAP);
        final Number media_count = (Number) reel_media.get("media_count"); // Can be null when profile is private
        if (media_count == null || media_count.intValue() == 0) {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            if (handleErrors) {
                final Map<String, Object> user = (Map<String, Object>) reel_media.get("user");
                if (media_count == null && (Boolean) user.get("is_private")) {
                    /* We got a private profile and thus cannot even know if this user has a story or not. */
                    ret.add(getDummyDownloadlinkProfilePrivate(username));
                } else {
                    final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "PROFILE_HAS_NO_STORY_" + username, "This profile currently doesn't have a story: " + username);
                    ret.add(dummy);
                }
            }
            return ret;
        } else {
            return this.crawlPostAltAPI(param, metadata, reel_media);
        }
    }

    /**
     * Crawls all highlight stories of given username. </br>
     *
     * @param handleErrors
     *            true = throw exception on error e.g. if no account is given and add dummy item if user has no story. </br>
     *            false = return empty array on error
     */
    private ArrayList<DownloadLink> crawlAllHighlightStories(final String username, final Account account, final AtomicBoolean loggedIN, final boolean handleErrors) throws UnsupportedEncodingException, Exception {
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (account == null && !handleErrors) {
            return new ArrayList<DownloadLink>();
        }
        /* Login is required to crawl such elements! */
        this.loginOrFail(account, loggedIN);
        final String userID = this.findUserID(null, account, loggedIN, username);
        if (userID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(account, this.br, InstaGramCom.ALT_API_BASE + "/highlights/" + userID + "/highlights_tray/");
        final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
        final List<Map<String, Object>> stories = (List<Map<String, Object>>) entries.get("tray");
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (final Map<String, Object> story : stories) {
            final String storyID = story.get("id").toString();
            /* These ones will go back into our crawler to find the individual media elements. */
            final String url = this.generateStoryHighlightURL(storyID);
            ret.add(this.createDownloadlink(url));
        }
        if (ret.isEmpty()) {
            logger.info("User has no highlight stories: " + username);
        }
        if (ret.isEmpty() && handleErrors) {
            final DownloadLink dummy = this.createOfflinelink(generateURLProfile(username), "PROFILE_HAS_NO_STORY_HIGHLIGHTS_" + username, "This profile currently doesn't have any story highlights: " + username);
            ret.add(dummy);
        }
        return ret;
    }

    private DownloadLink getDummyDownloadlinkProfileEmpty(final String username) {
        return this.createOfflinelink(generateURLProfile(username), "PROFILE_EMPTY_" + username, "The following profile doesn't contain any posts: " + username);
    }

    private DownloadLink getDummyDownloadlinkProfileOffline(final String username) {
        return this.createOfflinelink(generateURLProfile(username), "PROFILE_NOT_FOUND_" + username, "The following profile doesn't exist: " + username);
    }

    private DownloadLink getDummyDownloadlinkProfilePrivate(final String username) {
        return this.createOfflinelink(generateURLProfile(username), "PRIVATE_PROFILE_PERMISSIONS_MISSING_" + username, "The following profile is private and you do not have the required permissions to access it: " + username);
    }

    private DownloadLink getDummyDownloadlinkProfileHasNoSavedPosts(final String username) {
        return this.createOfflinelink(generateURLProfileSavedPosts(username), "PROFILE_CONTAINS_NO_SAVED_POSTS_" + username, "The following profile doesn't contain any saved posts: " + username);
    }

    /** Crawls list of post/story items obtained via alt API. */
    private ArrayList<DownloadLink> crawlPostListAltAPI(final CryptedLink param, final List<Map<String, Object>> mediaItems, final InstagramMetadata metadata) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        for (final Map<String, Object> mediaItem : mediaItems) {
            decryptedLinks.addAll(this.crawlPostAltAPI(param, metadata, mediaItem));
        }
        return decryptedLinks;
    }

    private void prepBrAjax(final Browser br, final Qdb qdb) {
        br.getHeaders().put("Accept", "*/*");
        String csrftoken = br.getCookie("instagram.com", "csrftoken");
        if (csrftoken == null) {
            /* 2020-11-05 */
            csrftoken = PluginJSonUtils.getJson(br, "csrf_token");
        }
        if (!StringUtils.isEmpty(csrftoken)) {
            br.getHeaders().put("X-CSRFToken", csrftoken);
        }
        if (qdb != null) {
            br.getHeaders().put("X-IG-App-ID", qdb.getFbAppId());
        }
        br.getHeaders().put("X-IG-WWW-Claim", "0"); // only ever seen this as 0
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    private void prepRequest(final Browser br, final Request request, final Qdb qdb) {
        request.getHeaders().put("Accept", "*/*");
        String csrftoken = br.getCookie("instagram.com", "csrftoken");
        if (csrftoken == null) {
            /* 2020-11-05 */
            csrftoken = PluginJSonUtils.getJson(br, "csrf_token");
        }
        if (!StringUtils.isEmpty(csrftoken)) {
            request.getHeaders().put("X-CSRFToken", csrftoken);
        }
        if (qdb != null) {
            request.getHeaders().put("X-IG-App-ID", qdb.getFbAppId());
        }
        request.getHeaders().put("X-IG-WWW-Claim", "0"); // only ever seen this as 0
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* Faster processing in debug mode */
            return 2;
        } else {
            /* 2020-01-21: Set to 1 to avoid download issues and try not to perform too many requests at the same time. */
            return 1;
        }
    }

    protected class InstagramMetadata {
        private String username      = null;
        private String mainContentID = null;
        private String hashtag       = null;
        private String description   = null;
        private String packageName   = null;
        private Date   date          = null;

        public InstagramMetadata() {
        }

        public InstagramMetadata(final String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getHashtag() {
            return hashtag;
        }

        public void setHashtag(String hashtag) {
            this.hashtag = hashtag;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public FilePackage getFilePackage() {
            if (this.getPackageName() == null) {
                return null;
            }
            final FilePackage fp = InstaGramComDecrypter.getFilePackage();
            fp.setName(this.getPackageName());
            final String cachedUserID = getCachedUserID(getUsername());
            // use setPackageKey because different user may result in same package name after name cleanup
            if (cachedUserID != null) {
                fp.setPackageKey(cachedUserID);
            } else {
                fp.setPackageKey(this.getPackageName());
            }
            return fp;
        }

        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public Date getDate() {
            return date;
        }

        public String getDateFormatted() {
            if (this.date == null) {
                return null;
            }
            return new SimpleDateFormat("yyyy-MM-dd").format(this.date);
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getMainContentID() {
            return mainContentID;
        }

        public void setMainContentID(String mainContentID) {
            this.mainContentID = mainContentID;
        }
    }

    public static FilePackage getFilePackage() {
        final FilePackage fp = FilePackage.getInstance();
        fp.setAllowMerge(true);
        fp.setCleanupPackageName(false);
        return fp;
    }

    private String generateStoryHighlightURL(final String storyID) {
        final String highlightID = storyID.split(":")[1];
        return "https://www." + this.getHost() + "/stories/highlights/" + highlightID + "/";
    }

    private String generateURLPost(final String postID) {
        return "https://www." + this.getHost() + "/p/" + postID + "/";
    }

    private String generateURLProfile(final String username) {
        return "https://www." + this.getHost() + "/" + username + "/";
    }

    private String generateURLProfileSavedPosts(final String username) {
        return "https://www." + this.getHost() + "/" + username + "/saved/";
    }
}
