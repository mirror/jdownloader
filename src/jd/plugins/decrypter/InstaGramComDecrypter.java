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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.config.InstagramConfig;
import org.jdownloader.plugins.components.instagram.Qdb;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.http.Request;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.InstaGramCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 4, names = { "instagram.com" }, urls = { "https?://(?:www\\.)?instagram\\.com/(stories/(?:highlights/\\d+/?|[^/]+)|explore/tags/[^/]+/?|((?:p|tv|reel)/[A-Za-z0-9_-]+|(?!explore)[^/]+(/saved|/tagged/?|/p/[A-Za-z0-9_-]+)?))" })
public class InstaGramComDecrypter extends PluginForDecrypt {
    public InstaGramComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String                  TYPE_PROFILE          = "(?i)https?://[^/]+/([^/]+)(?:/.*)?";
    private static final String                  TYPE_PROFILE_TAGGED   = "(?i)https?://([^/]+)/tagged/?";
    private static final String                  TYPE_GALLERY          = "(?i).+/(?:p|tv|reel)/([A-Za-z0-9_-]+)/?";
    private static final String                  TYPE_STORY            = "(?i)https?://[^/]+/stories/([^/]+).*";
    private static final String                  TYPE_STORY_HIGHLIGHTS = "(?i)https?://[^/]+/stories/highlights/(\\d+)/?";
    private static final String                  TYPE_SAVED_OBJECTS    = "(?i)https?://[^/]+/([^/]+)/saved/?$";
    private static final String                  TYPE_TAGS             = "(?i)https?://[^/]+/explore/tags/([^/]+)/?$";
    /** For links matching pattern {@link #TYPE_TAGS} --> This will be set on created DownloadLink objects as a (packagizer-) property. */
    private String                               hashtag               = null;
    private Boolean                              isPrivate             = false;
    private FilePackage                          fp                    = null;
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

    @Override
    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink link = super.createDownloadlink(url);
        if (this.hashtag != null) {
            link.setProperty(InstaGramCom.PROPERTY_hashtag, this.hashtag);
        }
        return link;
    }

    private void getPage(CryptedLink link, final Browser br, final String url, final String rhxGis, final String variables) throws Exception {
        getPage(link, br, br.createGetRequest(url), rhxGis, variables);
    }

    @SuppressWarnings({ "unused" })
    private void getPage(CryptedLink link, final Browser br, final Request sourceRequest, final String rhxGis, final String variables) throws Exception {
        int retry = 0;
        final int maxtries = 30;
        long totalWaittime = 0;
        Request request = null;
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
                if (PluginJsonConfig.get(InstagramConfig.class).isCrawlerAbortOnRateLimitReached()) {
                    logger.info("abort_on_rate_limit_reached setting active --> Rate limit has been reached --> Aborting");
                    break;
                } else {
                    final int waittime = 20000 + 15000 * retry;
                    totalWaittime += waittime;
                    logger.info(String.format("Waiting %d seconds on error 403/429 until retry", waittime / 1000));
                    sleep(waittime, link);
                }
            } else {
                break;
            }
        }
        if (br.getHttpConnection().getResponseCode() == 502) {
            throw br.new BrowserException("ResponseCode: 502", br.getRequest(), null);
        } else if (retry > 1) {
            logger.info("Total time waited to get around rate limit: " + TimeFormatter.formatMilliSeconds(totalWaittime, 0));
        }
    }

    // hash changes? but the value within is NEVER cleared. if map > resources || resources == null) remove storable
    private static Map<String, Qdb> QUERY_HASH = new HashMap<String, Qdb>();

    // https://www.diggernaut.com/blog/how-to-scrape-pages-infinite-scroll-extracting-data-from-instagram/
    // https://git.kaki87.net/KaKi87/ig-scraper/src/branch/master/index.js#L190
    private Qdb getQueryHash(Browser br, Qdb.QUERY query) throws Exception {
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
        return url.matches(TYPE_SAVED_OBJECTS) || url.matches(TYPE_STORY_HIGHLIGHTS) || url.matches(TYPE_STORY);
    }

    /** Get username from userID via alternative API. */
    private String getUsernameFromUserIDAltAPI(final Browser br, final String userID) throws PluginException, IOException {
        if (userID == null || !userID.matches("\\d+")) {
            return null;
        }
        final Browser brc = br.cloneBrowser();
        brc.setRequest(null);
        InstaGramCom.prepBRAltAPI(brc);
        InstaGramCom.getPageAltAPI(brc, InstaGramCom.ALT_API_BASE + "/users/" + userID + "/info/");
        Map<String, Object> entries = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("user");
        String username = null;
        if (entries != null) {
            username = (String) entries.get("username");
        }
        if (StringUtils.isEmpty(username)) {
            return null;
        } else {
            /* Cache information for later usage */
            synchronized (ID_TO_USERNAME) {
                ID_TO_USERNAME.put(userID, username);
            }
            return username;
        }
    }

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        if (param.getDownloadLink() != null) {
            /*
             * E.g. user crawls hashtag URL --> Some URLs go back into crawler --> We want to keep the hashtag in order to use it inside
             * filenames and as a packagizer property.
             */
            this.hashtag = param.getDownloadLink().getStringProperty(InstaGramCom.PROPERTY_hashtag);
        }
        br.addAllowedResponseCodes(new int[] { 502 });
        fp = FilePackage.getInstance();
        fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
        final AtomicBoolean loggedIN = new AtomicBoolean(false);
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        /* Correct URL added by user if necessary */
        String newURL = param.toString().replaceFirst("^http://", "https://").replaceFirst("://in", "://www.in");
        /* TODO: Re-check (and possibly remove) this handling! */
        if (newURL.contains("?private_url=true")) {
            this.isPrivate = Boolean.TRUE;
            /*
             * Remove this from url as it is only required for decrypter. It tells it whether or not we need to be logged_in to grab this
             * content.
             */
            newURL = newURL.replace("?private_url=true", "");
            loginOrFail(account, loggedIN);
        }
        if (!newURL.endsWith("/")) {
            /* Add slash to the end to prevent 302 redirect to speed up the crawl process a tiny bit. */
            newURL = newURL + "/";
        }
        if (!StringUtils.equals(param.getCryptedUrl(), newURL)) {
            logger.info("URL was changed: Old: " + param.getCryptedUrl() + " | New: " + newURL);
            param.setCryptedUrl(newURL);
        }
        if (this.requiresLogin(param.getCryptedUrl()) && !loggedIN.get()) {
            /* Saved users own objects can only be crawled when he's logged in ;) */
            if (account == null) {
                logger.info("Account required to crawl your own saved items");
                throw new AccountRequiredException();
            }
        }
        InstaGramCom.prepBRWebsite(this.br);
        br.addAllowedResponseCodes(new int[] { 502 });
        if (param.getCryptedUrl().matches(TYPE_SAVED_OBJECTS)) {
            return this.crawlUserSavedObjects(param, account, loggedIN);
        } else if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
            /* Crawl single images & galleries */
            return crawlGallery(param, account, loggedIN);
        } else if (param.getCryptedUrl().matches(TYPE_TAGS)) {
            return crawlHashtag(param, account, loggedIN);
        } else if (param.getCryptedUrl().matches(TYPE_STORY_HIGHLIGHTS)) {
            return this.crawlStoryHighlightsAltAPI(param, account, loggedIN);
        } else if (param.getCryptedUrl().matches(TYPE_STORY)) {
            return this.crawlStoryAltAPI(param, account, loggedIN);
        } else {
            return this.crawlUser(param, account, loggedIN);
        }
    }

    /**
     * Returns userID for given username (userID is required for API requests). </br>
     * 2021-05-04: Failed to find any other more elegant way to do this.
     */
    private String findUserID(final CryptedLink param, final Account account, final AtomicBoolean loggedIN, final String username) throws Exception {
        if (username == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* First check our cache -> saves time */
        String userID = getCachedUserID(username);
        if (userID != null) {
            /* Return cached userID */
            return userID;
        } else {
            /* Use website to find userID. */
            loginOrFail(account, loggedIN);
            final String userProfileURL = "https://www." + this.getHost() + "/" + username + "/";
            getPageAutoLogin(account, loggedIN, userProfileURL, param, br, userProfileURL, null, null);
            final String json = websiteGetJson();
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            /* We need to crawl the userID via website first in order to use the other API. */
            userID = (String) get(entries, "entry_data/ProfilePage/{0}/user/id", "entry_data/ProfilePage/{0}/graphql/user/id");
            if (userID == null) {
                userID = br.getRegex("\"owner\": ?\\{\"id\": ?\"(\\d+)\"\\}").getMatch(0);
            }
            if (StringUtils.isEmpty(userID)) {
                /* Most likely that profile doesn't exist */
                logger.info("Profile not found: " + username);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* Add to cache for later usage */
                synchronized (ID_TO_USERNAME) {
                    ID_TO_USERNAME.put(username, userID);
                }
                return userID;
            }
        }
    }

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

    private String getCachedUserName(final String userID) {
        if (userID == null) {
            return null;
        }
        synchronized (ID_TO_USERNAME) {
            return ID_TO_USERNAME.get(userID);
        }
    }

    private String websiteGetJson() {
        return br.getRegex(">window\\._sharedData\\s*?=\\s*?(\\{.*?);</script>").getMatch(0);
    }

    private String getVarRhxGis(final Browser br) {
        return br.getRegex("\"rhx_gis\"\\s*:\\s*\"([a-f0-9]{32})\"").getMatch(0);
    }

    private void getPageAutoLogin(final Account account, final AtomicBoolean loginState, final String urlCheck, final CryptedLink param, final Browser br, final String requestURL, final String rhxGis, final String variables) throws Exception {
        getPageAutoLogin(account, loginState, urlCheck, param, br, br.createGetRequest(requestURL), rhxGis, variables);
    }

    private void getPageAutoLogin(final Account account, final AtomicBoolean loginState, final String urlCheck, final CryptedLink param, final Browser br, final Request request, final String rhxGis, final String variables) throws Exception {
        getPage(param, br, request, null, null);
        AccountRequiredException accountRequired = null;
        try {
            InstaGramCom.checkErrors(br);
        } catch (AccountRequiredException e) {
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
                throw new AccountRequiredException();
            } else {
                loginOrFail(account, loginState);
                getPage(param, br, request, null, null);
                try {
                    InstaGramCom.checkErrors(br);
                    if (urlCheck != null && !br.getURL().contains(urlCheck)) {
                        throw new AccountRequiredException();
                    }
                } catch (AccountRequiredException e) {
                    logger.exception("Logged in but gallery still isn't accessible", e);
                    throw e;
                }
            }
        }
    }

    private ArrayList<DownloadLink> crawlGallery(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws Exception {
        final String urlCheck = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
        getPageAutoLogin(account, loggedIN, urlCheck, param, br, param.getCryptedUrl(), null, null);
        final Request request = br.getRequest();
        final String json = websiteGetJson();
        // if (json == null) {
        // /* E.g. if you add invalid URLs such as: instagram.com/developer */
        // logger.info("Failed to find any downloadable content");
        // decryptedLinks.add(this.createOfflinelink(parameter));
        // return decryptedLinks;
        // }
        Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
        final List<Object> resource_data_list;
        if (loggedIN.get()) {
            final String graphql = br.getRegex(">\\s*window\\.__additionalDataLoaded\\('/(?:(?:[^/]+/)?p|tv|reel)/[^/]+/'\\s*?,\\s*?(\\{.*?)\\);\\s*</script>").getMatch(0);
            if (graphql != null) {
                logger.info("Found expected __additionalDataLoaded json");
                final Object entriesO = JavaScriptEngineFactory.jsonToJavaObject(graphql);
                // entries = (Map<String, Object>) entriesO;
                resource_data_list = new ArrayList<Object>();
                resource_data_list.add(entriesO);
            } else {
                resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, "entry_data/PostPage");
                logger.info("Failed to find expected __additionalDataLoaded json --> Trying fallback:" + (resource_data_list != null));
            }
        } else {
            resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, "entry_data/PostPage");
        }
        final List<Map<String, Object>> todo_items = new ArrayList<Map<String, Object>>(0);
        for (Object entry : resource_data_list) {
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entry, "graphql/shortcode_media");
            if (entries == null) {
                final List<Map<String, Object>> items = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entry, "items");
                if (items != null && items.size() > 0) {
                    todo_items.addAll(items);
                    continue;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                todo_items.add(entries);
            }
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        for (final Map<String, Object> item : todo_items) {
            /** TODO: Check if cached- handling is useful here as well (see crawlHashtag) */
            final String usernameTmp = (String) get(item, "user/username", "owner/username");
            this.isPrivate = ((Boolean) get(item, "user/is_private", "owner/is_private")).booleanValue();
            if (usernameTmp != null) {
                fp.setName(usernameTmp);
            }
            decryptedLinks.addAll(crawlAlbum(param, request, usernameTmp, item));
        }
        return decryptedLinks;
    }

    @Override
    public void init() {
        setRequestIntervalLimitGlobal();
    }

    public static void setRequestIntervalLimitGlobal() {
        Browser.setRequestIntervalLimitGlobal("instagram.com", true, PluginJsonConfig.get(InstagramConfig.class).getGlobalRequestIntervalLimitMilliseconds());
    }

    /** Crawls all items of a user or all items where one user was tagged */
    private ArrayList<DownloadLink> crawlUser(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (PluginJsonConfig.get(InstagramConfig.class).getProfileCrawlerMaxItemsLimit() == 0) {
            logger.info("Stopping because: User has disabled profile crawler");
            return decryptedLinks;
        }
        final boolean useAltAPI = account != null && PluginJsonConfig.get(InstagramConfig.class).isProfileCrawlerPreferAlternativeAPI();
        if (useAltAPI && canBeProcessedByCrawlUserAltAPI(param.getCryptedUrl())) {
            final String username = new Regex(param.getCryptedUrl(), TYPE_PROFILE).getMatch(0);
            final String userID = findUserID(param, account, loggedIN, username);
            if (StringUtils.isEmpty(userID)) {
                /* Most likely that profile doesn't exist */
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "PROFILE_NOT_FOUND_" + username, "This profile doesn't exist: " + username));
                return decryptedLinks;
            }
            return this.crawlUserAltAPI(param, account, loggedIN, userID);
        } else {
            return crawlUserWebsite(param, account, loggedIN);
        }
    }

    /**
     * Crawl all media items of a user. </br>
     * Sometimes required user to be logged in to see all/more than X items. <br>
     */
    private ArrayList<DownloadLink> crawlUserWebsite(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(TYPE_PROFILE_TAGGED)) {
            /**
             * Crawl all items in which user from username inside URL is tagged. </br>
             * Do not assign username_url because this will be used as the owner-name of all found items which would be wrong here as the
             * content could have been posted by anyone who tagged that specific user!
             */
            /* 2021-08-16: Not yet supported */
            /* TODO: Needs update of getQueryHash! */
            logger.warning("Linktype is not yet supported: " + param.getCryptedUrl());
            fp.setName(new Regex(param.getCryptedUrl(), TYPE_PROFILE_TAGGED).getMatch(0) + " - tagged");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String username_url = new Regex(param.getCryptedUrl(), TYPE_PROFILE).getMatch(0);
        if (username_url == null) {
            /* Developer mistake! */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.setName(username_url);
        int counter = 0;
        long itemCount = 0;
        boolean isPrivate = false;
        String id_owner = null;
        String rhxGis = null;
        List<Object> resource_data_list = null;
        Map<String, Object> entries = null;
        Qdb qdb = null;
        do {
            getPageAutoLogin(account, loggedIN, null, param, br, param.getCryptedUrl(), null, null);
            if (!this.br.containsHTML("user\\?username=.+")) {
                final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "PROFILE_NOT_FOUND_" + username_url, "This profile doesn't exist.");
                decryptedLinks.add(dummy);
                return decryptedLinks;
            }
            final String json = websiteGetJson();
            entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            rhxGis = getVarRhxGis(this.br);
            qdb = getQueryHash(br, Qdb.QUERY.USER);
            id_owner = (String) get(entries, "entry_data/ProfilePage/{0}/user/id", "entry_data/ProfilePage/{0}/graphql/user/id");
            if (id_owner == null) {
                id_owner = br.getRegex("\"owner\": ?\\{\"id\": ?\"(\\d+)\"\\}").getMatch(0);
            }
            if (id_owner == null) {
                // this isn't necessarily an error! check https://www.instagram.com/israbox/
                logger.info("Failed to find id_owner -> Profile probably doesn't exist");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            isPrivate = ((Boolean) get(entries, "entry_data/ProfilePage/{0}/user/is_private", "entry_data/ProfilePage/{0}/graphql/user/is_private")).booleanValue();
            resource_data_list = (List) get(entries, "entry_data/ProfilePage/{0}/graphql/user/edge_owner_to_timeline_media/edges", "entry_data/ProfilePage/{0}/user/media/nodes");
            itemCount = JavaScriptEngineFactory.toLong(get(entries, "entry_data/ProfilePage/{0}/graphql/user/edge_owner_to_timeline_media/count", "entry_data/ProfilePage/{0}/user/media/count"), -1);
            if (isPrivate && (resource_data_list == null || resource_data_list.size() == 0)) {
                if (loggedIN.get()) {
                    logger.info("Cannot parse profile as it is private and not even visible when loggedIN");
                    throw new AccountRequiredException();
                } else {
                    br.clearCookies(br.getHost());
                    loginOrFail(account, loggedIN);
                }
                counter++;
            } else {
                break;
            }
        } while (counter <= 1);
        int page = 0;
        int decryptedLinksLastSize = 0;
        int decryptedLinksCurrentSize = 0;
        final long maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getProfileCrawlerMaxItemsLimit();
        String nextid = (String) get(entries, "entry_data/ProfilePage/{0}/graphql/user/edge_owner_to_timeline_media/page_info/end_cursor");
        if (nextid == null) {
            nextid = (String) get(entries, "entry_data/ProfilePage/{0}/user/media/page_info/end_cursor");
        }
        Request request = br.getRequest();
        do {
            if (page > 0) {
                final Browser br = this.br.cloneBrowser();
                prepBrAjax(br, qdb);
                final Map<String, Object> vars = new LinkedHashMap<String, Object>();
                vars.put("id", id_owner);
                vars.put("first", 12);
                vars.put("after", nextid);
                if (qdb == null || qdb.getQueryHash() == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String jsonString = JSonStorage.toString(vars).replaceAll("[\r\n]+", "").replaceAll("\\s+", "");
                try {
                    getPageAutoLogin(account, loggedIN, "/graphql/query", param, br, "/graphql/query/?query_hash=" + qdb.getQueryHash() + "&variables=" + URLEncode.encodeURIComponent(jsonString), rhxGis, jsonString);
                    request = br.getRequest();
                } catch (final AccountRequiredException ar) {
                    logger.log(ar);
                    /* Instagram blocks the amount of items a user can see based on */
                    if (loggedIN.get()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, ar);
                    } else {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account required to crawl more items of user " + username_url, null, ar);
                    }
                }
                /* TODO: Move all of this errorhandling to one place */
                final int responsecode = br.getHttpConnection().getResponseCode();
                if (responsecode == 403 || responsecode == 429) {
                    /* Stop on too many 403s as 403 is not a rate limit issue! */
                    logger.warning("Failed to bypass rate-limit!");
                    final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "FAILED_TO_BYPASS_RATE_LIMIT_" + username_url, "Failed to bypass rate-limit!");
                    decryptedLinks.add(dummy);
                    return decryptedLinks;
                } else if (responsecode == 439) {
                    logger.info("Seems like user is using an unverified account - cannot grab more items");
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                resource_data_list = (List) get(entries, "data/user/edge_owner_to_timeline_media/edges", "data/user/edge_user_to_photos_of_you/edges");
                nextid = (String) get(entries, "data/user/edge_owner_to_timeline_media/page_info/end_cursor");
                if (nextid == null) {
                    nextid = (String) get(entries, "data/user/edge_user_to_photos_of_you/page_info/end_cursor");
                }
            }
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            decryptedLinksLastSize = decryptedLinks.size();
            for (final Object o : resource_data_list) {
                final Map<String, Object> result = (Map<String, Object>) o;
                // pages > 0, have a additional nodes entry
                if (result.size() == 1 && result.containsKey("node")) {
                    decryptedLinks.addAll(crawlAlbum(param, request, username_url, (Map<String, Object>) result.get("node")));
                } else {
                    decryptedLinks.addAll(crawlAlbum(param, request, username_url, result));
                }
            }
            if (maxItemsLimit > 0 && decryptedLinks.size() >= maxItemsLimit) {
                logger.info("Stopping because: User defined max items limit has been reached");
                break;
            }
            decryptedLinksCurrentSize = decryptedLinks.size();
            page++;
        } while (!this.isAbort() && nextid != null && decryptedLinksCurrentSize > decryptedLinksLastSize && decryptedLinksCurrentSize < itemCount);
        if (!isAbort()) {
            if (decryptedLinks.size() == 0) {
                logger.warning("WTF found no content at all");
            } else {
                logger.info("nextid:" + nextid + "|decryptedLinksCurrentSize:" + decryptedLinksCurrentSize + "|decryptedLinksLastSize:" + decryptedLinksLastSize + "|itemCount:" + itemCount + "|page:" + page);
            }
        }
        return decryptedLinks;
    }

    private void loginOrFail(final Account account, final AtomicBoolean loggedIN) throws Exception {
        if (account == null) {
            throw new AccountRequiredException();
        } else if (!loggedIN.get()) {
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
        Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
        final String rhxGis = getVarRhxGis(this.br);
        final String username_url = new Regex(param.getCryptedUrl(), TYPE_SAVED_OBJECTS).getMatch(0);
        fp.setName("saved - " + username_url);
        final String id_owner = br.getRegex("profilePage_(\\d+)").getMatch(0);
        // final String graphql = br.getRegex("window\\._sharedData = (\\{.*?);</script>").getMatch(0);
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/graphql");
        String nextid = null;
        long count = 0;
        int page = 1;
        int decryptedLinksLastSize = 0;
        int decryptedLinksCurrentSize = 0;
        final Qdb qdb = getQueryHash(br, Qdb.QUERY.USER_SAVED);
        Request request = br.getRequest();
        do {
            if (page > 1) {
                if (id_owner == null) {
                    /* This should never happen */
                    logger.warning("Pagination failed because required param 'id_owner' is missing");
                    break;
                }
                final Browser br = this.br.cloneBrowser();
                prepBrAjax(br, qdb);
                final Map<String, Object> vars = new LinkedHashMap<String, Object>();
                vars.put("id", id_owner);
                vars.put("first", 12);
                vars.put("after", nextid);
                if (qdb == null || qdb.getQueryHash() == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String jsonString = JSonStorage.toString(vars).replaceAll("[\r\n]+", "").replaceAll("\\s+", "");
                try {
                    getPageAutoLogin(account, loggedIN, "/graphql/query", param, br, "/graphql/query/?query_hash=" + qdb.getQueryHash() + "&variables=" + URLEncode.encodeURIComponent(jsonString), rhxGis, jsonString);
                    request = br.getRequest();
                } catch (final AccountRequiredException ar) {
                    logger.log(ar);
                    /* Instagram blocks the amount of items a user can see based on */
                    if (loggedIN.get()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, ar);
                    } else {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account required to crawl more items of user " + username_url, null, ar);
                    }
                }
                InstaGramCom.checkErrors(br);
                /*
                 * 2020-11-06: TODO: Fix broken response: edge_owner_to_timeline_media instead of edge_saved_media ... Possibe reasons:
                 * Wrong "end_cursor" String and/or wrong "query_hash".
                 */
                final int responsecode = br.getHttpConnection().getResponseCode();
                if (responsecode == 404) {
                    logger.warning("Error occurred: 404");
                    return decryptedLinks;
                } else if (responsecode == 403 || responsecode == 429) {
                    /* Stop on too many 403s as 403 is not a rate limit issue! */
                    final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "FAILED_TO_BYPASS_RATE_LIMIT_" + username_url, "Failed to bypass rate-limit!");
                    decryptedLinks.add(dummy);
                    return decryptedLinks;
                } else if (responsecode == 439) {
                    logger.info("Seems like user is using an unverified account - cannot grab more items");
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                entries = (Map<String, Object>) entries.get("data");
            }
            if (page == 1) {
                count = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "user/edge_saved_media/count"), 0);
                if (count == 0) {
                    logger.info("User doesn't have any saved objects (?)");
                    final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "USER_DOES_NOT_HAVE_ANY_SAVED_OBJECTS_" + username_url, "The following user does not have any saved objects: " + username_url);
                    decryptedLinks.add(dummy);
                    return decryptedLinks;
                } else {
                    logger.info("Expecting saved objects: " + count);
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
                    decryptedLinks.addAll(crawlAlbum(param, request, username_url, (Map<String, Object>) result.get("node")));
                } else {
                    decryptedLinks.addAll(crawlAlbum(param, request, username_url, result));
                }
            }
            decryptedLinksCurrentSize = decryptedLinks.size();
            logger.info("Crawled page: " + page + " | Found items: " + decryptedLinksCurrentSize + "/" + count);
            page++;
        } while (!this.isAbort() && nextid != null && decryptedLinksCurrentSize > decryptedLinksLastSize && decryptedLinksCurrentSize < count);
        return decryptedLinks;
    }

    /**
     * Crawls all items found when looking for a specified items. </br>
     * Max. number of items which this returns can be limited by user setting. </br>
     * Doesn't require the user to be logged in!
     */
    private ArrayList<DownloadLink> crawlHashtagWebsite(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        getPageAutoLogin(account, loggedIN, null, param, br, param.getCryptedUrl(), null, null);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Qdb qdb = getQueryHash(br, Qdb.QUERY.USER);
        Map<String, Object> entries = JSonStorage.restoreFromString(websiteGetJson(), TypeRef.HASHMAP);
        List<Object> resource_data_list = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "entry_data/TagPage/{0}/data/recent/sections");
        this.hashtag = new Regex(param.getCryptedUrl(), TYPE_TAGS).getMatch(0);
        if (this.hashtag == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.setName("hashtag - " + this.hashtag);
        String next_max_id = (String) get(entries, "entry_data/TagPage/{0}/data/recent/next_max_id");
        Boolean more_available = (Boolean) get(entries, "entry_data/TagPage/{0}/data/recent/more_available");
        Number next_page = (Number) get(entries, "entry_data/TagPage/{0}/data/recent/next_page");
        int page = 0;
        int decryptedLinksLastSize = 0;
        int decryptedLinksCurrentSize = 0;
        final long maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getHashtagCrawlerMaxItemsLimit();
        Request request = br.getRequest();
        do {
            if (Boolean.TRUE.equals(more_available) && next_page != null && next_page.intValue() == page) {
                final Browser br = this.br.cloneBrowser();
                InstaGramCom.prepBRAltAPI(br);
                PostRequest postRequest = br.createPostRequest(InstaGramCom.ALT_API_BASE + "/tags/" + hashtag + "/sections/", "include_persistent=0&max_id=" + URLEncode.encodeURIComponent(next_max_id) + "&page=" + next_page.toString() + "&surface=grid&tab=recent");
                prepRequest(br, postRequest, qdb);
                postRequest.getHeaders().put("Origin", "https://www.instagram.com");
                postRequest.getHeaders().put("Referer", "https://www.instagram.com");
                br.setCurrentURL("https://www.instagram.com");
                try {
                    getPageAutoLogin(account, loggedIN, "/api/v1/tags", param, br, postRequest, null, null);
                    request = br.getRequest();
                } catch (final AccountRequiredException ar) {
                    logger.log(ar);
                    /* Instagram blocks the amount of items a user can see based on */
                    if (loggedIN.get()) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, ar);
                    } else {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account required to crawl more items of hashtag " + this.hashtag, null, ar);
                    }
                }
                /* TODO: Move all of this errorhandling to one place */
                final int responsecode = br.getHttpConnection().getResponseCode();
                if (responsecode == 403 || responsecode == 429) {
                    /* Stop on too many 403s as 403 is not a rate limit issue! */
                    logger.warning("Failed to bypass rate-limit!");
                    final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "FAILED_TO_BYPASS_RATE_LIMIT_" + this.hashtag, "Failed to bypass rate-limit!");
                    decryptedLinks.add(dummy);
                    return decryptedLinks;
                } else if (responsecode == 439) {
                    logger.info("Seems like user is using an unverified account - cannot grab more items");
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                resource_data_list = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "sections");
                next_max_id = (String) get(entries, "next_max_id");
                more_available = (Boolean) get(entries, "more_available");
                next_page = (Number) get(entries, "next_page");
            }
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Found no new links on page->Stopping decryption");
                break;
            }
            decryptedLinksLastSize = decryptedLinks.size();
            for (final Object o : resource_data_list) {
                final List<Object> medias = (List<Object>) JavaScriptEngineFactory.walkJson(o, "layout_content/medias/");
                if (medias != null) {
                    for (final Object media : medias) {
                        decryptedLinks.addAll(crawlAlbum(param, request, null, (Map<String, Object>) JavaScriptEngineFactory.walkJson(media, "media/")));
                    }
                }
            }
            if (!Boolean.TRUE.equals(more_available) || next_max_id == null || next_page == null) {
                logger.info("more_available:" + more_available + "|next_max_id:" + next_max_id + "|next_page:" + next_page);
                break;
            } else if (decryptedLinks.size() >= maxItemsLimit) {
                logger.info("Number of items selected in plugin setting has been crawled --> Done");
                break;
            }
            decryptedLinksCurrentSize = decryptedLinks.size();
            page++;
        } while (!this.isAbort() && decryptedLinksCurrentSize > decryptedLinksLastSize);
        return decryptedLinks;
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    /**
     * 2020-11-18: This never worked but we should keep it as there isn't much work left to make this work. </br>
     * Do not update this! Do not delete this! Only modify it if you can make it work!
     */
    private ArrayList<DownloadLink> crawlStory(Map<String, Object> entries, final CryptedLink param) throws Exception {
        final boolean functionNotYetDone = true;
        if (functionNotYetDone) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        final String username_url = new Regex(param.getCryptedUrl(), TYPE_STORY).getMatch(0);
        final String story_user_id = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/StoriesPage/{0}/user/id");
        final Qdb qdb = getQueryHash(br, Qdb.QUERY.STORY);
        if (username_url == null || StringUtils.isEmpty(story_user_id)) {
            /* This should never happen! */
            return decryptedLinks;
        }
        final Browser br = this.br.cloneBrowser();
        prepBrAjax(br, qdb);
        if (qdb == null || qdb.getQueryHash() == null) {
            logger.warning("Pagination failed because qHash is not given");
            return decryptedLinks;
        }
        final String url = "/graphql/query/?query_hash=" + qdb.getQueryHash() + "&variables=%7B%22reel_ids%22%3A%5B%22" + story_user_id + "%22%5D%2C%22tag_names%22%3A%5B%5D%2C%22location_ids%22%3A%5B%5D%2C%22highlight_reel_ids%22%3A%5B%5D%2C%22precomposed_overlay%22%3Afalse%2C%22show_story_viewer_list%22%3Atrue%2C%22story_viewer_fetch_count%22%3A50%2C%22story_viewer_cursor%22%3A%22%22%2C%22stories_video_dash_manifest%22%3Afalse%7D";
        getPage(param, br, url, null, null);
        entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
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
            dl.setProperty(DownloadLink.RELATIVE_DOWNLOAD_FOLDER_PATH, subfolderpath);
            dl._setFilePackage(fp);
            decryptedLinks.add(dl);
            distribute(dl);
        }
        return decryptedLinks;
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

    private ArrayList<DownloadLink> crawlAlbum(final CryptedLink param, final Request request, final String preGivenUsername, Map<String, Object> entries) throws PluginException {
        return crawlAlbum(param, request, preGivenUsername, entries, -1);
    }

    @SuppressWarnings({ "unchecked" })
    private ArrayList<DownloadLink> crawlAlbum(final CryptedLink param, final Request request, final String preGivenUsername, Map<String, Object> entries, final int preGivenIndex) throws PluginException {
        long date = JavaScriptEngineFactory.toLong(entries.get("date"), 0);
        if (date == 0) {
            date = JavaScriptEngineFactory.toLong(entries.get("taken_at_timestamp"), 0);
            if (date == 0) {
                // api
                date = JavaScriptEngineFactory.toLong(entries.get("taken_at"), 0);
            }
        }
        final InstagramConfig cfg = PluginJsonConfig.get(InstagramConfig.class);
        // is this id? // final String linkid_main = (String) entries.get("id");
        String typename = (String) entries.get("__typename");
        final Number mediaType = (Number) entries.get("media_type");// api
        if (typename == null && mediaType != null) {
            typename = getTypeName(mediaType.intValue());
        }
        if (typename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown media type");
        }
        String contentIDMain = (String) entries.get("code");
        // page > 0, now called 'shortcode'
        if (contentIDMain == null) {
            contentIDMain = (String) entries.get("shortcode");
        }
        String usernameForFilename = null;
        if (preGivenUsername != null) {
            /* E.g. user crawl a complete user profile --> Username is globally given to set on all crawled objects */
            usernameForFilename = preGivenUsername;
        } else {
            /* Finding the username "the hard way" */
            try {
                Map<String, Object> ownerInfo = (Map<String, Object>) entries.get("owner");
                if (ownerInfo == null) {
                    // api
                    ownerInfo = (Map<String, Object>) entries.get("user");
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
                /* Check if username is in json */
                usernameForFilename = (String) ownerInfo.get("username");
                if (usernameForFilename != null) {
                    /* Cache information for later usage just in case it isn't present in json the next time. */
                    synchronized (ID_TO_USERNAME) {
                        ID_TO_USERNAME.put(userID, usernameForFilename);
                    }
                } else if (cfg.isHashtagCrawlerFindUsernames()) {
                    /* Check if we got this username cached */
                    synchronized (ID_TO_USERNAME) {
                        usernameForFilename = ID_TO_USERNAME.get(userID);
                        if (usernameForFilename == null) {
                            /* HTTP request needed to find username! */
                            usernameForFilename = this.getUsernameFromUserIDAltAPI(br, userID);
                            if (usernameForFilename == null) {
                                logger.warning("WTF failed to find username for userID: " + userID);
                            }
                        }
                    }
                } else {
                    logger.info("Username not available for the following ID because this feature has been disabled by the user: " + contentIDMain);
                }
            } catch (final Throwable ignore) {
                logger.log(ignore);
            }
        }
        if (usernameForFilename == null && cfg.isHashtagCrawlerFindUsernames()) {
            /* This should never happen! */
            logger.warning("WTF - failed to find usernameForFilename!");
        }
        Object caption = entries.get("caption");
        String description = null;
        if (caption != null) {
            if (caption instanceof String) {
                description = caption.toString();
            } else if (caption instanceof Map) {
                // api
                description = (String) JavaScriptEngineFactory.walkJson(caption, "text");
            }
        }
        if (description == null) {
            try {
                final Map<String, Object> edge_media_to_caption = ((Map<String, Object>) entries.get("edge_media_to_caption"));
                final List<Map<String, Object>> edges = (List<Map<String, Object>>) edge_media_to_caption.get("edges");
                if (edges.size() > 0) {
                    final Map<String, Object> node = (Map<String, Object>) edges.get(0).get("node");
                    description = (String) node.get("text");
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        List<Map<String, Object>> resource_data_list = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "edge_sidecar_to_children/edges");
        if (resource_data_list == null) {
            // api
            resource_data_list = (List<Map<String, Object>>) JavaScriptEngineFactory.walkJson(entries, "carousel_media");
        }
        String preGivenOrderidFormatted = null;
        if (preGivenIndex != -1) {
            preGivenOrderidFormatted = Integer.toString(preGivenIndex);
        }
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (StringUtils.equalsIgnoreCase("GraphSidecar", typename) && !param.getCryptedUrl().matches(TYPE_GALLERY) && (resource_data_list == null || resource_data_list.size() > 1)) {
            /* loop back into crawler for GraphSidecar handling */
            final DownloadLink dl = this.createDownloadlink(createSinglePosturl(contentIDMain));
            decryptedLinks.add(dl);
            distribute(dl);
        } else if (StringUtils.equalsIgnoreCase("GraphImage", typename) && (resource_data_list == null || resource_data_list.size() == 0)) {
            /* Single image */
            decryptedLinks.add(crawlSingleMediaObject(param, request, entries, contentIDMain, date, description, preGivenIndex, preGivenOrderidFormatted, usernameForFilename));
        } else if (StringUtils.equalsIgnoreCase("GraphVideo", typename) && (resource_data_list == null || resource_data_list.size() == 0)) {
            /* Single video */
            decryptedLinks.add(crawlSingleMediaObject(param, request, entries, contentIDMain, date, description, preGivenIndex, preGivenOrderidFormatted, usernameForFilename));
        } else if (typename != null && typename.matches("Graph[A-Z][a-zA-Z0-9]+") && resource_data_list == null && !param.getCryptedUrl().matches(TYPE_GALLERY)) {
            /*
             * 2017-05-09: User has added a 'User' URL and in this case a single post contains multiple images (=album) but at this stage
             * the json does not contain the other images --> This has to go back into the decrypter and get crawled as a single item.
             */
            final DownloadLink dl = this.createDownloadlink(createSinglePosturl(contentIDMain));
            decryptedLinks.add(dl);
            distribute(dl);
        } else if (resource_data_list != null && resource_data_list.size() > 0) {
            final int padLength = getPadLength(resource_data_list.size());
            int index = 0;
            if (preGivenIndex != -1) {
                /* Continue from pre-given index */
                index = preGivenIndex;
            }
            /* Album */
            for (Map<String, Object> picture : resource_data_list) {
                index++;
                final String orderidFormatted = String.format(Locale.US, "%0" + padLength + "d", index);
                if (picture.containsKey("node")) {
                    picture = (Map<String, Object>) picture.get("node");
                }
                decryptedLinks.add(crawlSingleMediaObject(param, request, picture, contentIDMain, date, description, index, orderidFormatted, usernameForFilename));
            }
        } else {
            /* Single image */
            decryptedLinks.add(crawlSingleMediaObject(param, request, entries, contentIDMain, date, description, -1, null, usernameForFilename));
        }
        if (cfg.isAddPostDescriptionAsTextfile() && !StringUtils.isEmpty(description)) {
            /* Testing: Download picture-description as .txt file */
            final DownloadLink lastAddedDownloadLink = decryptedLinks.get(decryptedLinks.size() - 1);
            final DownloadLink text = this.createDownloadlink(lastAddedDownloadLink.getPluginPatternMatcher() + "_description");
            text.setProperty(InstaGramCom.PROPERTY_type, "text");
            text.setProperty(InstaGramCom.PROPERTY_description, description);
            /**
             * Determine filename based on whether or not we crawled a single media item or multiple ones. </br>
             * If there is only one media item we will use a filename that matches the one of this item otherwise we'll use "<main
             * contentID> + .txt".
             */
            final String filename;
            if (decryptedLinks.size() == 1) {
                filename = lastAddedDownloadLink.getName().substring(0, lastAddedDownloadLink.getName().lastIndexOf(".")) + ".txt";
            } else if (usernameForFilename != null) {
                filename = usernameForFilename + " - " + contentIDMain + ".txt";
            } else {
                filename = contentIDMain + ".txt";
            }
            text.setFinalFileName(filename);
            text.setProperty(InstaGramCom.PROPERTY_filename_from_crawler, filename);
            try {
                text.setDownloadSize(description.getBytes("UTF-8").length);
            } catch (final UnsupportedEncodingException ignore) {
                ignore.printStackTrace();
            }
            text.setAvailable(true);
            if (this.fp != null) {
                text._setFilePackage(this.fp);
            }
            text.setComment(description);
            distribute(text);
            decryptedLinks.add(text);
        }
        return decryptedLinks;
    }

    /**
     * Crawls json objects of type "GraphImage".
     *
     * @throws PluginException
     */
    private DownloadLink crawlSingleMediaObject(final CryptedLink param, final Request request, final Map<String, Object> entries, String mainContentID, final long date, final String description, final int orderid, final String orderidFormatted, final String username) throws PluginException {
        final String itemID = entries.get("id").toString();
        final String shortcode = (String) entries.get("shortcode");
        if (mainContentID == null && shortcode != null) {
            // link uid, with /p/ its shortcode
            mainContentID = shortcode;
        }
        final Number mediaType = (Number) entries.get("media_type");// api
        final boolean isVideo = Boolean.TRUE.equals(entries.get("is_video")) || (mediaType != null && mediaType.intValue() == 2);
        String dllink = null;
        if (isVideo) {
            dllink = (String) entries.get("video_url");
        } else {
            /* Find best image-quality */
            final List<Object> ressourcelist = (List<Object>) entries.get("display_resources");
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
                dllink = (String) entries.get("display_src");
                if (dllink == null || !dllink.startsWith("http")) {
                    dllink = (String) entries.get("display_url");
                    if (dllink == null || !dllink.startsWith("http")) {
                        dllink = (String) entries.get("thumbnail_src");
                    }
                }
            }
        }
        final boolean isPartOfStory = Boolean.TRUE.equals(entries.get("is_reel_media"));
        boolean apiModeURL = false;
        if (dllink == null) {
            // api
            dllink = InstaGramCom.getBestQualityURLAltAPI(entries);
            if (!StringUtils.isEmpty(dllink) && StringUtils.containsIgnoreCase(request.getUrl(), "i.instagram.com/api/v1")) {
                apiModeURL = true;
            }
        }
        String dateFormatted = null;
        if (date > 0) {
            final String targetFormat = "yyyy-MM-dd";
            final Date theDate = new Date(date * 1000);
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            dateFormatted = formatter.format(theDate);
        }
        final String hostplugin_url = "instagrammdecrypted://" + mainContentID;
        final DownloadLink dl = this.createDownloadlink(hostplugin_url);
        String content_url = createSinglePosturl(mainContentID);
        if (isPrivate) {
            /*
             * Without account, private urls look exactly the same as offline urls --> Save private status for better host plugin
             * errorhandling.
             */
            content_url += "?private_url=true";
            dl.setProperty(InstaGramCom.PROPERTY_private_url, true);
        }
        dl.setContentUrl(content_url);
        if (fp != null && !"Various".equals(fp.getName())) {
            fp.add(dl);
        }
        dl.setAvailable(true);
        dl.setProperty(InstaGramCom.PROPERTY_main_content_id, mainContentID);
        if (dateFormatted != null) {
            dl.setProperty(InstaGramCom.PROPERTY_date, dateFormatted);
        }
        if (!StringUtils.isEmpty(shortcode)) {
            dl.setProperty(InstaGramCom.PROPERTY_shortcode, shortcode);
        }
        if (!StringUtils.isEmpty(dllink)) {
            dl.setProperty(InstaGramCom.PROPERTY_DIRECTURL, dllink);
            if (apiModeURL) {
                dl.setProperty(InstaGramCom.PROPERTY_has_tried_to_crawl_original_url, true);
            }
        }
        if (!StringUtils.isEmpty(description)) {
            dl.setComment(description);
            /* For custom Packagizer filenames */
            dl.setProperty(InstaGramCom.PROPERTY_description, description);
        }
        if (!StringUtils.isEmpty(orderidFormatted)) {
            /* For custom Packagizer filenames */
            dl.setProperty(InstaGramCom.PROPERTY_orderid, orderidFormatted);
            dl.setProperty(InstaGramCom.PROPERTY_orderid_raw, orderid);
        }
        dl.setProperty(InstaGramCom.PROPERTY_postid, itemID);
        if (!StringUtils.isEmpty(username)) {
            /* Packagizer Property */
            dl.setProperty(InstaGramCom.PROPERTY_uploader, username);
        }
        if (isPartOfStory) {
            dl.setProperty(InstaGramCom.PROPERTY_is_part_of_story, true);
            /* There are no individual public URLs to directly link elements of a story. */
            dl.setContentUrl(param.getCryptedUrl());
        }
        if (isVideo) {
            dl.setProperty(InstaGramCom.PROPERTY_type, "video");
        } else {
            dl.setProperty(InstaGramCom.PROPERTY_type, "photo");
        }
        final String filename = getFilename(dl);
        dl.setFinalFileName(filename);
        distribute(dl);
        return dl;
    }

    /** TODO: Make use of this for .txt files containing descriptions of photos see: https://svn.jdownloader.org/issues/85026 */
    public static String getFilename(final DownloadLink link) {
        /* Check for pre-defined filename */
        if (link.hasProperty(InstaGramCom.PROPERTY_filename_from_crawler)) {
            return link.getStringProperty(InstaGramCom.PROPERTY_filename_from_crawler);
        }
        final String dateFormatted = link.getStringProperty(InstaGramCom.PROPERTY_date);
        final String username = link.getStringProperty(InstaGramCom.PROPERTY_uploader);
        final String orderidFormatted = link.getStringProperty(InstaGramCom.PROPERTY_orderid);
        final String mainContentID = link.getStringProperty(InstaGramCom.PROPERTY_main_content_id);
        final String shortcode = link.getStringProperty(InstaGramCom.PROPERTY_shortcode);
        final String ext;
        if (InstaGramCom.isVideo(link)) {
            ext = ".mp4";
        } else {
            ext = ".jpg";
        }
        String server_filename = null;
        final String directurl = link.getStringProperty(InstaGramCom.PROPERTY_DIRECTURL);
        if (directurl != null) {
            try {
                server_filename = getFileNameFromURL(new URL(directurl));
            } catch (final Throwable ignore) {
            }
        }
        if (PluginJsonConfig.get(InstagramConfig.class).isPreferServerFilenames() && server_filename != null) {
            server_filename = jd.plugins.hoster.InstaGramCom.fixServerFilename(server_filename, ext);
            return server_filename;
        }
        String filename;
        if (PluginJsonConfig.get(InstagramConfig.class).isAddDateToFilenames() && dateFormatted != null) {
            filename = dateFormatted + "_";
        } else {
            filename = "";
        }
        if (link.hasProperty(InstaGramCom.PROPERTY_is_part_of_story)) {
            /* Use slightly different filenames for items that are part of a users' story */
            if (!StringUtils.isEmpty(username)) {
                filename += username + " - ";
            }
            if (orderidFormatted != null && PluginJsonConfig.get(InstagramConfig.class).isAddOrderidToFilenames()) {
                /* By default: Include orderid whenever it is given to prevent duplicate filenames for different files! */
                filename += orderidFormatted;
            }
            filename += " - " + mainContentID;
            if (!StringUtils.isEmpty(shortcode) && !shortcode.equals(mainContentID)) {
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
            /*
             * 2022-01-28: For some items, individual ID is available when user is not logged in but isn't available when user is logged in.
             */
            final boolean individualShortcodeIsAvailable;
            if (!StringUtils.isEmpty(shortcode) && !shortcode.equals(mainContentID)) {
                individualShortcodeIsAvailable = true;
            } else {
                individualShortcodeIsAvailable = false;
            }
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
                if (orderidFormatted != null) {
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
        fp.setName("saved - " + usernameOwnerOfSavedItems);
        InstaGramCom.prepBRAltAPI(this.br);
        Map<String, Object> entries;
        String nextid = null;
        int page = 1;
        int numberofCrawledItems = 0;
        final String savedItemsFeedBaseURL = InstaGramCom.ALT_API_BASE + "/feed/saved/";
        do {
            logger.info("Crawling page: " + page);
            if (page == 1) {
                /**
                 * This may return a varying amount of items e.g. 84 items on the first request, 9 after the next - this is decided
                 * serverside!
                 */
                InstaGramCom.getPageAltAPI(this.br, savedItemsFeedBaseURL);
            } else {
                // br.getPage(hashtagBaseURL + "?after=" + nextid);
                InstaGramCom.getPageAltAPI(this.br, savedItemsFeedBaseURL + "?max_id=" + nextid);
            }
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final int numberofitemsOnThisPage = (int) JavaScriptEngineFactory.toLong(entries.get("num_results"), 0);
            if (numberofitemsOnThisPage == 0) {
                /* Rare case */
                logger.info("Stopping because: 0 items available ...");
                return decryptedLinks;
            }
            nextid = (String) entries.get("next_max_id");
            final boolean more_available = ((Boolean) entries.get("more_available"));
            List<Object> mediaItems = (List<Object>) entries.get("items");
            if (mediaItems == null || mediaItems.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            decryptedLinks.addAll(this.crawlAlbumList(param, mediaItems, null));
            numberofCrawledItems += numberofitemsOnThisPage;
            logger.info("Crawled page: " + page + " | Found items so far: " + numberofCrawledItems);
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
            return false;
        } else if (url.matches(TYPE_PROFILE)) {
            return true;
        } else {
            return false;
        }
    }

    private ArrayList<DownloadLink> crawlUserAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN, final String userID) throws UnsupportedEncodingException, Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        final String usernameURL = new Regex(param.getCryptedUrl(), TYPE_PROFILE).getMatch(0);
        if (usernameURL == null || userID == null) {
            /* Most likely developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.setName(usernameURL);
        InstaGramCom.prepBRAltAPI(this.br);
        Map<String, Object> entries;
        String nextid = null;
        int page = 1;
        int numberofCrawledItems = 0;
        final int maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getProfileCrawlerMaxItemsLimit();
        final String savedItemsFeedBaseURL = InstaGramCom.ALT_API_BASE + "/feed/user/" + userID + "/";
        do {
            if (page == 1) {
                InstaGramCom.getPageAltAPI(this.br, savedItemsFeedBaseURL);
            } else {
                // br.getPage(hashtagBaseURL + "?after=" + nextid);
                InstaGramCom.getPageAltAPI(this.br, savedItemsFeedBaseURL + "?max_id=" + nextid);
            }
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final int numberofitemsOnThisPage = (int) JavaScriptEngineFactory.toLong(entries.get("num_results"), 0);
            if (numberofitemsOnThisPage == 0) {
                /* Rare case */
                if (page == 1) {
                    final DownloadLink dummy = this.createOfflinelink(param.getCryptedUrl(), "PROFILE_EMPTY_" + usernameURL, "This profile doesn't contain any items: " + usernameURL);
                    decryptedLinks.add(dummy);
                } else {
                    logger.info("Stopping because: 0 items available on current page");
                }
                return decryptedLinks;
            }
            nextid = (String) entries.get("next_max_id");
            final boolean more_available = ((Boolean) entries.get("more_available"));
            List<Object> mediaItems = (List<Object>) entries.get("items");
            if (mediaItems == null || mediaItems.size() == 0) {
                logger.info("Stopping because: Found no new links on page " + page);
                break;
            }
            decryptedLinks.addAll(this.crawlAlbumList(param, mediaItems, usernameURL));
            numberofCrawledItems += numberofitemsOnThisPage;
            logger.info("Crawled page: " + page + " | Found items so far: " + numberofCrawledItems);
            if (!more_available) {
                logger.info("Stopping because: more_available == false");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because: no nextid available");
                break;
            } else if (maxItemsLimit > 0 && numberofCrawledItems >= maxItemsLimit) {
                logger.info("Stopping because: reached user defined max items limit");
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlHashtag(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        if (PluginJsonConfig.get(InstagramConfig.class).getHashtagCrawlerMaxItemsLimit() == 0) {
            logger.info("Stopping because: User has disabled hashtag crawling");
            return new ArrayList<DownloadLink>();
        }
        if (loggedIN.get()) {
            return this.crawlHashtagAltAPI(param, account, loggedIN);
        } else {
            return this.crawlHashtagWebsite(param, account, loggedIN);
        }
    }

    private ArrayList<DownloadLink> crawlHashtagAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* Login is mandatory! */
        loginOrFail(account, loggedIN);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.hashtag = new Regex(param.getCryptedUrl(), TYPE_TAGS).getMatch(0);
        if (this.hashtag == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.setName("hashtag - " + this.hashtag);
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(this.br, InstaGramCom.ALT_API_BASE + "/tags/" + this.hashtag + "/info/");
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final long totalNumberofItems = JavaScriptEngineFactory.toLong(entries.get("media_count"), 0);
        if (totalNumberofItems == 0) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "No items available for this tag: " + this.hashtag, "No items available for this tag: " + this.hashtag));
            return decryptedLinks;
        }
        String nextid = null;
        int page = 1;
        int numberofCrawledItemsTotal = 0;
        final String hashtagBaseURL = InstaGramCom.ALT_API_BASE + "/feed/tag/" + this.hashtag + "/";
        final long maxItemsLimit = PluginJsonConfig.get(InstagramConfig.class).getHashtagCrawlerMaxItemsLimit();
        logger.info("Expecting to find " + totalNumberofItems + " items");
        do {
            if (page == 1) {
                /*
                 * Returns a lot of items on first access and then a lot less e.g. 84 on first request, then 8-9 on each subsequent request.
                 */
                InstaGramCom.getPageAltAPI(this.br, hashtagBaseURL);
            } else {
                // br.getPage(hashtagBaseURL + "?after=" + nextid);
                InstaGramCom.getPageAltAPI(this.br, hashtagBaseURL + "?max_id=" + nextid);
            }
            entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            final int numberofitemsOnThisPage = (int) JavaScriptEngineFactory.toLong(entries.get("num_results"), 0);
            if (numberofitemsOnThisPage == 0) {
                /* Rare case */
                logger.info("Stopping because: 0 items available ...");
                return decryptedLinks;
            }
            nextid = (String) entries.get("next_max_id");
            final boolean more_available = ((Boolean) entries.get("more_available"));
            List<Object> resource_data_list = (List<Object>) entries.get("items");
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Stopping because: Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            decryptedLinks.addAll(this.crawlAlbumList(param, resource_data_list, null));
            numberofCrawledItemsTotal += numberofitemsOnThisPage;
            logger.info("Crawled page: " + page + " | Found items so far: " + numberofCrawledItemsTotal + "/" + totalNumberofItems);
            if (!more_available) {
                logger.info("Stopping because: More_available == false");
                break;
            } else if (numberofCrawledItemsTotal >= totalNumberofItems) {
                logger.info("Stopping because: Found number of items is higher or equal to expected number of items");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because: No nextid available");
                break;
            } else if (numberofCrawledItemsTotal >= maxItemsLimit) {
                logger.info("Stopping because: Reached user defined max items limit");
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        if (decryptedLinks.size() == 0) {
            logger.warning("WTF empty array");
        }
        return decryptedLinks;
    }

    /** Crawls: https://www.instagram.com/stories/highlights/<numbers>/ */
    private ArrayList<DownloadLink> crawlStoryHighlightsAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        if (account == null) {
            throw new AccountRequiredException();
        }
        final String reelID = new Regex(param.getCryptedUrl(), TYPE_STORY_HIGHLIGHTS).getMatch(0);
        if (reelID == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Login is required to crawl such elements! */
        this.loginOrFail(account, loggedIN);
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(this.br, InstaGramCom.ALT_API_BASE + "/feed/reels_media/?reel_ids=highlight%3A" + reelID);
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (entries.containsKey("reels_media")) {
            fp.setName("story highlights - " + reelID);
            final List<Object> allReels = (List<Object>) entries.get("reels_media");
            if (allReels.isEmpty()) {
                logger.info("Invalid reelsID?!");
                decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "No media available for " + reelID, "No media available for " + reelID));
                return decryptedLinks;
            }
            int totalIndex = 0;
            for (final Object reelsO : allReels) {
                entries = (Map<String, Object>) reelsO;
                final List<Object> resource_data_list = (List<Object>) entries.get("items");
                decryptedLinks.addAll(crawlAlbumList(param, resource_data_list, null, totalIndex));
                totalIndex += resource_data_list.size();
            }
        } else {
            final Map<String, Object> highlightsInfo = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "reels/highlight:" + reelID);
            final List<Object> resource_data_list = (List<Object>) highlightsInfo.get("items");
            final Map<String, Object> userInfo = (Map<String, Object>) highlightsInfo.get("user");
            final String username = (String) userInfo.get("username");
            final long userID = ((Number) userInfo.get("pk")).longValue();
            if (!StringUtils.isEmpty(username)) {
                fp.setName("story highlights - " + username);
                /* Cache information for later usage */
                synchronized (ID_TO_USERNAME) {
                    ID_TO_USERNAME.put(Long.toString(userID), username);
                }
            } else {
                fp.setName("story highlights - " + reelID);
            }
            decryptedLinks.addAll(crawlAlbumList(param, resource_data_list, username, 0));
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlStoryAltAPI(final CryptedLink param, final Account account, final AtomicBoolean loggedIN) throws UnsupportedEncodingException, Exception {
        final String user = new Regex(param.getCryptedUrl(), TYPE_STORY).getMatch(0);
        final String userID = findUserID(param, account, loggedIN, user);
        if (StringUtils.isEmpty(userID)) {
            /* Most likely that profile doesn't exist */
            final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "PROFILE_NOT_FOUND_" + user, "This profile doesn't exist: " + user));
            return decryptedLinks;
        }
        if (userID == null || !userID.matches("\\d+")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String usernameURL = new Regex(param.getCryptedUrl(), TYPE_STORY).getMatch(0);
        if (usernameURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* We need to be loggedIN to be able to see stories of users! */
        this.loginOrFail(account, loggedIN);
        fp.setName("story - " + usernameURL);
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(this.br, InstaGramCom.ALT_API_BASE + "/feed/user/" + userID + "/reel_media/");
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final List<Object> resource_data_list = (List<Object>) entries.get("items");
        if (resource_data_list == null || resource_data_list.size() == 0) {
            logger.info("User doesn't have any story items or profile is private and we're missing the rights to access it");
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(this.createOfflinelink(param.getCryptedUrl(), "NO_STORY_FOUND_" + userID, "User does not have any story items: " + userID));
            return ret;
        }
        return crawlAlbumList(param, resource_data_list, usernameURL, 0);
    }

    private ArrayList<DownloadLink> crawlAlbumList(final CryptedLink param, List<Object> mediaItems, final String preGivenUsername) throws PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        for (final Object mediaItemO : mediaItems) {
            Map<String, Object> mediaItem = (Map<String, Object>) mediaItemO;
            if (mediaItem.containsKey("media")) {
                mediaItem = (Map<String, Object>) mediaItem.get("media");
            }
            decryptedLinks.addAll(crawlAlbum(param, br.getRequest(), preGivenUsername, mediaItem));
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlAlbumList(final CryptedLink param, List<Object> mediaItems, final String preGivenUsername, final int startIndex) throws PluginException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        int orderID = startIndex;
        for (final Object mediaItemO : mediaItems) {
            Map<String, Object> mediaItem = (Map<String, Object>) mediaItemO;
            if (mediaItem.containsKey("media")) {
                mediaItem = (Map<String, Object>) mediaItem.get("media");
            }
            orderID += 1;
            decryptedLinks.addAll(crawlAlbum(param, br.getRequest(), preGivenUsername, mediaItem, orderID));
        }
        return decryptedLinks;
    }

    private String createSinglePosturl(final String p_id) {
        return String.format("https://www.instagram.com/p/%s", p_id);
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

    private final int getPadLength(final int size) {
        if (size < 10) {
            return 1;
        } else if (size < 100) {
            return 2;
        } else if (size < 1000) {
            return 3;
        } else if (size < 10000) {
            return 4;
        } else if (size < 100000) {
            return 5;
        } else if (size < 1000000) {
            return 6;
        } else if (size < 10000000) {
            return 7;
        } else {
            return 8;
        }
        /* 2021-07-06: Why not use this instead? return Integer.toString(size).length(); */
    }
}
