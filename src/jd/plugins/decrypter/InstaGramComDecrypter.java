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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.Hash;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.instagram.Qdb;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.http.requests.GetRequest;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "instagram.com" }, urls = { "https?://(?:www\\.)?instagram\\.com/(stories/[^/]+|explore/tags/[^/]+/?|((?:p|tv)/[A-Za-z0-9_-]+|(?!explore)[^/]+(/saved|/p/[A-Za-z0-9_-]+)?))" })
public class InstaGramComDecrypter extends PluginForDecrypt {
    public InstaGramComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String                  TYPE_GALLERY                      = ".+/(?:p|tv)/([A-Za-z0-9_-]+)/?";
    private static final String                  TYPE_STORY                        = "https?://[^/]+/stories/([^/]+).*";
    private static final String                  TYPE_SAVED_OBJECTS                = "https?://[^/]+/([^/]+)/saved/?$";
    private static final String                  TYPE_TAGS                         = "https?://[^/]+/explore/tags/([^/]+)/?$";
    private String                               username_url                      = null;
    /** For links matching pattern {@link #TYPE_TAGS} --> This will be set on created DownloadLink objects as a (packagizer-) property. */
    private String                               hashtag                           = null;
    private final ArrayList<DownloadLink>        decryptedLinks                    = new ArrayList<DownloadLink>();
    private boolean                              prefer_server_filename            = jd.plugins.hoster.InstaGramCom.defaultPREFER_SERVER_FILENAMES;
    private boolean                              findUsernameDuringHashtagCrawling = jd.plugins.hoster.InstaGramCom.defaultHASHTAG_CRAWLER_FIND_USERNAMES;
    private Boolean                              isPrivate                         = false;
    private FilePackage                          fp                                = null;
    private String                               parameter                         = null;
    private static LinkedHashMap<String, String> ID_TO_USERNAME                    = new LinkedHashMap<String, String>() {
                                                                                       protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                                                                                           return size() > 100;
                                                                                       };
                                                                                   };

    /** Tries different json paths and returns the first result. */
    private Object get(Map<String, Object> entries, final String... paths) {
        for (String path : paths) {
            final Object ret = JavaScriptEngineFactory.walkJson(entries, path);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    @Override
    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink link = super.createDownloadlink(url);
        if (this.hashtag != null) {
            link.setProperty("hashtag", this.hashtag);
        }
        return link;
    }

    @SuppressWarnings({ "deprecation", "unused" })
    private void getPage(CryptedLink link, final Browser br, String url, final String rhxGis, final String variables) throws Exception {
        int retry = 0;
        final int maxtries = 30;
        long totalWaittime = 0;
        while (retry < maxtries && !isAbort()) {
            retry++;
            final GetRequest get = br.createGetRequest(url);
            if (rhxGis != null && variables != null) {
                if (false) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final String sig = Hash.getMD5(rhxGis + ":" + variables);
                    get.getHeaders().put("X-Instagram-GIS", sig);
                }
            }
            if (retry > 1) {
                logger.info(String.format("Trying to get around rate limit %d / %d", retry, maxtries));
                /* 2020-01-21: Changing User-Agent or Cookies will not help us to get around this limit earlier! */
                // br.clearCookies(br.getHost());
                // br.getHeaders().put("User-Agent", "iPad");
            }
            br.getPage(get);
            final int responsecode = br.getHttpConnection().getResponseCode();
            if (responsecode == 502) {
                final int waittime = 20000 + 15000 * retry;
                totalWaittime += waittime;
                logger.info(String.format("Waiting %d seconds on error 502 until retry", waittime / 1000));
                sleep(waittime, link);
            } else if (responsecode == 403 || responsecode == 429) {
                if (SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.QUIT_ON_RATE_LIMIT_REACHED, jd.plugins.hoster.InstaGramCom.defaultQUIT_ON_RATE_LIMIT_REACHED)) {
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

    private String                  fbAppId    = null;
    private String                  qHash      = null;
    // hash changes? but the value within is NEVER cleared. if map > resources || resources == null) remove storable
    private static Map<String, Qdb> QUERY_HASH = new HashMap<String, Qdb>();

    // https://www.diggernaut.com/blog/how-to-scrape-pages-infinite-scroll-extracting-data-from-instagram/
    private void getByUserIDQueryHash(Browser br) throws Exception {
        synchronized (QUERY_HASH) {
            // they keep changing the filename. was ProfilePageContainer[x], ..., ..., and now Consumer[3rd ref].
            final String profilePageContainer = br.getRegex("(/static/bundles/([^/]+/)?Consumer\\.js/[a-f0-9]+.js)").getMatch(0);
            if (profilePageContainer != null) {
                {
                    final Qdb qdb = QUERY_HASH.get(profilePageContainer);
                    if (qdb != null) {
                        fbAppId = qdb.getFbAppId();
                        qHash = qdb.getQueryHash();
                        return;
                    }
                }
                Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept", "*/*");
                brc.getPage(profilePageContainer);
                qHash = brc.getRegex("\\},queryId\\s*:\\s*\"([0-9a-f]{32})\"").getMatch(0);
                {
                    final String clc = br.getRegex("(/static/bundles/([^/]+/)?ConsumerLibCommons\\.js/[a-f0-9]+.js)").getMatch(0);
                    if (clc != null) {
                        brc = br.cloneBrowser();
                        brc.getHeaders().put("Accept", "*/*");
                        brc.getPage(clc);
                        fbAppId = brc.getRegex("e\\.instagramWebDesktopFBAppId\\s*=\\s*'(\\d+)'").getMatch(0);
                        if (StringUtils.isEmpty(fbAppId)) {
                            logger.info("no fbAppId found!?:" + profilePageContainer);
                        }
                    }
                }
                if (StringUtils.isNotEmpty(qHash)) {
                    final Qdb qdb = new Qdb();
                    if (StringUtils.isNotEmpty(fbAppId)) {
                        qdb.setFbAppId(fbAppId);
                    }
                    qdb.setQueryHash(qHash);
                    QUERY_HASH.put(profilePageContainer, qdb);
                } else {
                    logger.info("no queryHash found!?:" + profilePageContainer);
                }
            }
        }
    }

    /** Do we have to be logged in to crawl this URL? */
    private boolean requiresLogin(final String url) {
        return url.matches(TYPE_SAVED_OBJECTS);
    }

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
        return entries != null ? (String) entries.get("username") : null;
    }

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.clearAll();
        br.setFollowRedirects(true);
        fbAppId = null;
        qHash = null;
        if (param.getDownloadLink() != null) {
            /*
             * E.g. user crawls hashtag URL --> Some URLs go back into crawler --> We want to keep the hashtag in order to use it inside
             * filenames and as a packagizer property.
             */
            this.hashtag = param.getDownloadLink().getStringProperty("hashtag");
        }
        br.addAllowedResponseCodes(new int[] { 502 });
        prefer_server_filename = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.PREFER_SERVER_FILENAMES, jd.plugins.hoster.InstaGramCom.defaultPREFER_SERVER_FILENAMES);
        this.findUsernameDuringHashtagCrawling = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.HASHTAG_CRAWLER_FIND_USERNAMES, jd.plugins.hoster.InstaGramCom.defaultHASHTAG_CRAWLER_FIND_USERNAMES);
        fp = FilePackage.getInstance();
        fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
        final PluginForHost hostplugin = getNewPluginForHostInstance(getHost());
        boolean loggedIN = false;
        final Account account = AccountController.getInstance().getValidAccount(hostplugin);
        /* https and www. is required! */
        parameter = param.toString().replaceFirst("^http://", "https://").replaceFirst("://in", "://www.in");
        /* TODO: Re-check this handling! */
        if (parameter.contains("?private_url=true")) {
            isPrivate = Boolean.TRUE;
            /*
             * Remove this from url as it is only required for decrypter. It tells it whether or not we need to be logged_in to grab this
             * content.
             */
            parameter = parameter.replace("?private_url=true", "");
            if (account == null) {
                throw new AccountRequiredException();
            }
            ((jd.plugins.hoster.InstaGramCom) hostplugin).login(account, false);
            loggedIN = true;
        }
        if (!parameter.endsWith("/")) {
            /* Add slash to the end to prevent 302 redirect to speed up the crawl process a tiny bit. */
            parameter += "/";
        }
        if (this.requiresLogin(this.parameter) && !loggedIN) {
            /* Saved users own objects can only be crawled when he's logged in ;) */
            if (account == null) {
                logger.info("Account required to crawl your own saved items");
                throw new AccountRequiredException();
            }
        }
        InstaGramCom.prepBRWebsite(this.br);
        br.addAllowedResponseCodes(new int[] { 502 });
        if (parameter.matches(TYPE_SAVED_OBJECTS)) {
            /* 2020-11-19: Prefer API as pagination is broken in website method. */
            final boolean preferAPI = true;
            if (preferAPI) {
                this.crawlUserSavedObjectsAltAPI(param, account, loggedIN);
            } else {
                this.crawlUserSavedObjectsWebsite(param, account, loggedIN);
            }
        } else if (parameter.matches(TYPE_GALLERY)) {
            /* Crawl single images & galleries */
            crawlGallery(param, account, loggedIN);
        } else if (parameter.matches(TYPE_TAGS)) {
            if (loggedIN) {
                this.crawlHashtagAltAPI(param, account, loggedIN);
            } else {
                this.crawlHashtag(param, account, loggedIN);
            }
        } else if (parameter.matches(TYPE_STORY)) {
            if (account == null) {
                logger.info("Account required to crawl stories");
                throw new AccountRequiredException();
            } else if (!loggedIN) {
                final PluginForHost plg = getNewPluginForHostInstance(getHost());
                plg.setBrowser(this.br);
                ((jd.plugins.hoster.InstaGramCom) plg).login(account, false);
            }
            final String user = new Regex(this.parameter, TYPE_STORY).getMatch(0);
            getPage(param, br, "https://www." + this.getHost() + "/" + user + "/", null, null);
            InstaGramCom.checkErrors(this.br);
            final String json = websiteGetJson();
            final Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            /* We need to crawl the userID via website first in order to use the other API. */
            String id_owner = (String) get(entries, "entry_data/ProfilePage/{0}/user/id", "entry_data/ProfilePage/{0}/graphql/user/id");
            if (id_owner == null) {
                id_owner = br.getRegex("\"owner\": ?\\{\"id\": ?\"(\\d+)\"\\}").getMatch(0);
            }
            if (StringUtils.isEmpty(id_owner)) {
                /* Most likely that profile doesn't exist */
                decryptedLinks.add(this.createOfflinelink(parameter, "This profile doesn't exist", "This profile doesn't exist"));
                return decryptedLinks;
            }
            this.crawlStoryAltAPI(param, id_owner);
        } else {
            /* Crawl all items of a user */
            crawlUser(param, account, loggedIN);
        }
        return decryptedLinks;
    }

    private String websiteGetJson() {
        return br.getRegex(">window\\._sharedData\\s*?=\\s*?(\\{.*?);</script>").getMatch(0);
    }

    private String getVarRhxGis(final Browser br) {
        return br.getRegex("\"rhx_gis\"\\s*:\\s*\"([a-f0-9]{32})\"").getMatch(0);
    }

    private void crawlGallery(final CryptedLink param, final Account account, boolean loggedIN) throws Exception {
        final String galleryID = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
        getPage(param, br, parameter, null, null);
        InstaGramCom.checkErrors(this.br);
        if (!br.getURL().contains(galleryID)) {
            /*
             * E.g. private gallery and we're not logged in or we're not logged in with an account with the required permissions -> Redirect
             * to main page or URL of the profile which uploaded the gallery.
             */
            if (account == null) {
                throw new AccountRequiredException();
            } else {
                final PluginForHost hostplugin = getNewPluginForHostInstance(getHost());
                hostplugin.setBrowser(this.br);
                ((jd.plugins.hoster.InstaGramCom) hostplugin).login(account, false);
                loggedIN = true;
                getPage(param, br, parameter, null, null);
                InstaGramCom.checkErrors(this.br);
                if (!br.getURL().contains(galleryID)) {
                    logger.info("Logged in but gallery still isn't accessible");
                    throw new AccountRequiredException();
                }
            }
        }
        final String json = websiteGetJson();
        // if (json == null) {
        // /* E.g. if you add invalid URLs such as: instagram.com/developer */
        // logger.info("Failed to find any downloadable content");
        // decryptedLinks.add(this.createOfflinelink(parameter));
        // return decryptedLinks;
        // }
        Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
        final List<Object> resource_data_list;
        if (loggedIN) {
            final String graphql = br.getRegex(">window\\.__additionalDataLoaded\\('/p/[^/]+/'\\s*?,\\s*?(\\{.*?)\\);</script>").getMatch(0);
            if (graphql != null) {
                logger.info("Found expected loggedin json");
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(graphql);
                resource_data_list = new ArrayList<Object>();
                resource_data_list.add(JavaScriptEngineFactory.walkJson(entries, "/"));
            } else {
                /* 2020-12-01: Rare case/fallback */
                logger.info("Failed to find expected loggedin json --> Trying fallback");
                resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, "entry_data/PostPage");
            }
        } else {
            resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, "entry_data/PostPage");
        }
        for (final Object entry : resource_data_list) {
            entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entry, "graphql/shortcode_media");
            if (entries == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /** TODO: Check if cached- handling is useful here as well (see crawlHashtag) */
            final String usernameTmp = (String) JavaScriptEngineFactory.walkJson(entries, "owner/username");
            this.isPrivate = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "owner/is_private")).booleanValue();
            if (usernameTmp != null) {
                fp.setName(usernameTmp);
            }
            crawlAlbum(entries);
        }
    }

    /**
     * Crawl all media items of a user. </br>
     * Sometimes required user to be logged in to see all/more than X items. <br>
     * Alternatively possible via: /api/v1/feed/user/{userID}.
     */
    private void crawlUser(final CryptedLink param, final Account account, boolean loggedIN) throws UnsupportedEncodingException, Exception {
        this.username_url = new Regex(parameter, "instagram\\.com/([^/]+)").getMatch(0);
        fp.setName(username_url);
        int counter = 0;
        long itemCount = 0;
        boolean isPrivate = false;
        String id_owner = null;
        String rhxGis = null;
        ArrayList<Object> resource_data_list = null;
        Map<String, Object> entries = null;
        do {
            getPage(param, br, parameter, null, null);
            InstaGramCom.checkErrors(this.br);
            final String json = websiteGetJson();
            entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            if (!this.br.containsHTML("user\\?username=.+")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return;
            }
            rhxGis = getVarRhxGis(this.br);
            getByUserIDQueryHash(br);
            id_owner = (String) get(entries, "entry_data/ProfilePage/{0}/user/id", "entry_data/ProfilePage/{0}/graphql/user/id");
            if (id_owner == null) {
                id_owner = br.getRegex("\"owner\": ?\\{\"id\": ?\"(\\d+)\"\\}").getMatch(0);
            }
            if (id_owner == null) {
                // this isn't a error persay! check https://www.instagram.com/israbox/
                logger.info("Failed to find id_owner");
                return;
            }
            isPrivate = ((Boolean) get(entries, "entry_data/ProfilePage/{0}/user/is_private", "entry_data/ProfilePage/{0}/graphql/user/is_private")).booleanValue();
            resource_data_list = (ArrayList) get(entries, "entry_data/ProfilePage/{0}/user/media/nodes", "entry_data/ProfilePage/{0}/graphql/user/edge_owner_to_timeline_media/edges");
            itemCount = JavaScriptEngineFactory.toLong(get(entries, "entry_data/ProfilePage/{0}/user/media/count", "entry_data/ProfilePage/{0}/graphql/user/edge_owner_to_timeline_media/count"), -1);
            if (isPrivate && (resource_data_list == null || resource_data_list.size() == 0)) {
                if (account == null) {
                    logger.info("Cannot parse profile as it is private");
                    throw new AccountRequiredException();
                } else if (loggedIN) {
                    logger.info("Cannot parse profile as it is private and not even visible when loggedIN");
                    throw new AccountRequiredException();
                } else {
                    logger.info("Looks like a private profile --> Logging in and trying again");
                    br.clearCookies(br.getHost());
                    final PluginForHost hostplugin = getNewPluginForHostInstance(getHost());
                    hostplugin.setBrowser(this.br);
                    ((jd.plugins.hoster.InstaGramCom) hostplugin).login(account, false);
                    loggedIN = true;
                }
                counter++;
            } else {
                break;
            }
        } while (counter <= 1);
        int page = 0;
        int decryptedLinksLastSize = 0;
        int decryptedLinksCurrentSize = 0;
        final long maX_items = SubConfiguration.getConfig(this.getHost()).getLongProperty(jd.plugins.hoster.InstaGramCom.ONLY_GRAB_X_ITEMS_NUMBER, jd.plugins.hoster.InstaGramCom.defaultONLY_GRAB_X_ITEMS_NUMBER);
        final boolean only_grab_x_items = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.ONLY_GRAB_X_ITEMS, jd.plugins.hoster.InstaGramCom.defaultONLY_GRAB_X_ITEMS);
        String nextid = (String) get(entries, "entry_data/ProfilePage/{0}/user/media/page_info/end_cursor", "entry_data/ProfilePage/{0}/graphql/user/edge_owner_to_timeline_media/page_info/end_cursor");
        do {
            if (page > 0) {
                final Browser br = this.br.cloneBrowser();
                prepBrAjax(br);
                final Map<String, Object> vars = new LinkedHashMap<String, Object>();
                vars.put("id", id_owner);
                vars.put("first", 12);
                vars.put("after", nextid);
                if (qHash == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String jsonString = JSonStorage.toString(vars).replaceAll("[\r\n]+", "").replaceAll("\\s+", "");
                getPage(param, br, "/graphql/query/?query_hash=" + qHash + "&variables=" + URLEncoder.encode(jsonString, "UTF-8"), rhxGis, jsonString);
                try {
                    InstaGramCom.checkErrors(br);
                } catch (final AccountRequiredException ar) {
                    /* Instagram blocks the amount of items a user can see based on */
                    if (!loggedIN) {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account required to crawl more items of user " + this.username_url, null, ar);
                    } else {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account session refresh required to crawl more items of user " + this.username_url, null, ar);
                    }
                }
                /* TODO: Move all of this errorhandling to one place */
                final int responsecode = br.getHttpConnection().getResponseCode();
                if (responsecode == 403 || responsecode == 429) {
                    /* Stop on too many 403s as 403 is not a rate limit issue! */
                    logger.warning("Failed to bypass rate-limit!");
                    return;
                } else if (responsecode == 439) {
                    logger.info("Seems like user is using an unverified account - cannot grab more items");
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "data/user/edge_owner_to_timeline_media/edges");
                nextid = (String) JavaScriptEngineFactory.walkJson(entries, "data/user/edge_owner_to_timeline_media/page_info/end_cursor");
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
                    crawlAlbum((Map<String, Object>) result.get("node"));
                } else {
                    crawlAlbum(result);
                }
            }
            if (only_grab_x_items && decryptedLinks.size() >= maX_items) {
                logger.info("Number of items selected in plugin setting has been crawled --> Done");
                break;
            }
            decryptedLinksCurrentSize = decryptedLinks.size();
            page++;
        } while (!this.isAbort() && nextid != null && decryptedLinksCurrentSize > decryptedLinksLastSize && decryptedLinksCurrentSize < itemCount);
        if (decryptedLinks.size() == 0) {
            logger.warning("WTF found no content at all");
        }
    }

    /**
     * Crawls all saved media items of the currently logged in user. </br>
     * Obviously this will only work when logged in.
     */
    private void crawlUserSavedObjectsWebsite(final CryptedLink param, final Account account, final boolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* Login is mandatory! */
        if (account == null) {
            throw new AccountRequiredException();
        } else if (!loggedIN) {
            final PluginForHost plg = getNewPluginForHostInstance(getHost());
            plg.setBrowser(this.br);
            ((jd.plugins.hoster.InstaGramCom) plg).login(account, false);
        }
        getPage(param, br, parameter, null, null);
        InstaGramCom.checkErrors(this.br);
        final String json = websiteGetJson();
        Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
        final String rhxGis = getVarRhxGis(this.br);
        this.username_url = new Regex(parameter, TYPE_SAVED_OBJECTS).getMatch(0);
        fp.setName("saved - " + this.username_url);
        final String id_owner = br.getRegex("profilePage_(\\d+)").getMatch(0);
        // final String graphql = br.getRegex("window\\._sharedData = (\\{.*?);</script>").getMatch(0);
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/graphql");
        String nextid = null;
        long count = 0;
        int page = 0;
        int decryptedLinksLastSize = 0;
        int decryptedLinksCurrentSize = 0;
        do {
            if (page > 0) {
                if (id_owner == null) {
                    /* This should never happen */
                    logger.warning("Pagination failed because required param 'id_owner' is missing");
                    break;
                }
                getByUserIDQueryHash(this.br);
                final Browser br2 = this.br.cloneBrowser();
                prepBrAjax(br2);
                final Map<String, Object> vars = new LinkedHashMap<String, Object>();
                vars.put("id", id_owner);
                vars.put("first", 12);
                vars.put("after", nextid);
                if (qHash == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String jsonString = JSonStorage.toString(vars).replaceAll("[\r\n]+", "").replaceAll("\\s+", "");
                getPage(param, br2, "/graphql/query/?query_hash=" + qHash + "&variables=" + URLEncoder.encode(jsonString, "UTF-8"), rhxGis, jsonString);
                InstaGramCom.checkErrors(br);
                /*
                 * 2020-11-06: TODO: Fix broken response: edge_owner_to_timeline_media instead of edge_saved_media ... Possibe reasons:
                 * Wrong "end_cursor" String and/or wrong "query_hash".
                 */
                final int responsecode = br2.getHttpConnection().getResponseCode();
                if (responsecode == 404) {
                    logger.warning("Error occurred: 404");
                    return;
                } else if (responsecode == 403 || responsecode == 429) {
                    /* Stop on too many 403s as 403 is not a rate limit issue! */
                    logger.warning("Failed to bypass rate-limit!");
                    return;
                } else if (responsecode == 439) {
                    logger.info("Seems like user is using an unverified account - cannot grab more items");
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br2.toString());
                entries = (Map<String, Object>) entries.get("data");
            }
            if (page == 0) {
                count = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "user/edge_saved_media/count"), 0);
                if (count == 0) {
                    logger.info("User doesn't have any saved objects (?)");
                    return;
                } else {
                    logger.info("Expecting saved objects: " + count);
                }
            }
            nextid = (String) JavaScriptEngineFactory.walkJson(entries, "user/edge_saved_media/page_info/end_cursor");
            ArrayList<Object> resource_data_list = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "user/edge_saved_media/edges");
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            decryptedLinksLastSize = decryptedLinks.size();
            for (final Object o : resource_data_list) {
                final Map<String, Object> result = (Map<String, Object>) o;
                // pages > 0, have a additional nodes entry
                if (result.size() == 1 && result.containsKey("node")) {
                    crawlAlbum((Map<String, Object>) result.get("node"));
                } else {
                    crawlAlbum(result);
                }
            }
            decryptedLinksCurrentSize = decryptedLinks.size();
            page++;
        } while (!this.isAbort() && nextid != null && decryptedLinksCurrentSize > decryptedLinksLastSize && decryptedLinksCurrentSize < count);
    }

    /**
     * Crawls all items found when looking for a specified items. </br>
     * Max. number of items which this returns can be limited by user setting. </br>
     * Doesn't require the user to be logged in!
     */
    private void crawlHashtag(final CryptedLink param, final Account account, final boolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* 2020-12-14: Never login - login is not required to crawl hashtag items! */
        // if (!loggedIN) {
        // final PluginForHost plg = getNewPluginForHostInstance(getHost());
        // plg.setBrowser(this.br);
        // ((jd.plugins.hoster.InstaGramCom) plg).login(account, false);
        // }
        getPage(param, br, parameter, null, null);
        InstaGramCom.checkErrors(this.br);
        getByUserIDQueryHash(br);
        final String json = websiteGetJson();
        Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
        entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "entry_data/TagPage/{0}/graphql/hashtag");
        this.hashtag = new Regex(param.getCryptedUrl(), TYPE_TAGS).getMatch(0);
        if (this.hashtag == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.setName("hashtag - " + this.hashtag);
        final String rhxGis = this.getVarRhxGis(this.br);
        String nextid = null;
        int count = 0;
        int page = 0;
        int decryptedLinksLastSize = 0;
        int decryptedLinksCurrentSize = 0;
        final long maX_items = SubConfiguration.getConfig(this.getHost()).getLongProperty(jd.plugins.hoster.InstaGramCom.ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER, jd.plugins.hoster.InstaGramCom.defaultONLY_GRAB_X_ITEMS_NUMBER);
        do {
            if (page > 0) {
                /* 2020-11-05: TODO: Fix pagination handling - returns error 400 */
                if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                    /* https://svn.jdownloader.org/issues/88960 */
                    logger.info("Pagination is broken in anonymous mode - add account and try again");
                    break;
                }
                final Browser br = this.br.cloneBrowser();
                prepBrAjax(br);
                final Map<String, Object> vars = new LinkedHashMap<String, Object>();
                vars.put("tag_name", this.hashtag);
                vars.put("first", 1);
                vars.put("after", nextid);
                if (qHash == null) {
                    logger.warning("Pagination failed because qHash is not given");
                    break;
                }
                final String jsonString = JSonStorage.toString(vars).replaceAll("[\r\n]+", "").replaceAll("\\s+", "");
                getPage(param, br, "/graphql/query/?query_hash=" + qHash + "&variables=" + URLEncoder.encode(jsonString, "UTF-8"), rhxGis, jsonString);
                try {
                    InstaGramCom.checkErrors(br);
                } catch (final AccountRequiredException ar) {
                    /* Instagram blocks the amount of items a user can see based on */
                    if (loggedIN) {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, "Account required to crawl more items of hashtag " + this.hashtag, null, ar);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, ar);
                    }
                }
                /* TODO: Move all of this errorhandling to one place */
                final int responsecode = br.getHttpConnection().getResponseCode();
                if (responsecode == 403 || responsecode == 429) {
                    /* Stop on too many 403s as 403 is not a rate limit issue! */
                    logger.warning("Failed to bypass rate-limit!");
                    return;
                } else if (responsecode == 439) {
                    logger.info("Seems like user is using an unverified account - cannot grab more items");
                    break;
                }
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                entries = (Map<String, Object>) JavaScriptEngineFactory.walkJson(entries, "data/hashtag");
            }
            entries = (Map<String, Object>) entries.get("edge_hashtag_to_media");
            nextid = (String) JavaScriptEngineFactory.walkJson(entries, "page_info/end_cursor");
            if (page == 0) {
                count = (int) JavaScriptEngineFactory.toLong(entries.get("count"), 0);
                if (count == 0) {
                    /* Rare case */
                    logger.info("Stopping, 0 items available ...");
                    return;
                } else {
                    logger.info("Crawling items: " + count);
                }
            }
            List<Object> resource_data_list = (List<Object>) entries.get("edges");
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            decryptedLinksLastSize = decryptedLinks.size();
            for (final Object o : resource_data_list) {
                final Map<String, Object> result = (Map<String, Object>) o;
                crawlAlbum((Map<String, Object>) result.get("node"));
            }
            if (decryptedLinks.size() >= maX_items) {
                logger.info("Number of items selected in plugin setting has been crawled --> Done");
                break;
            }
            decryptedLinksCurrentSize = decryptedLinks.size();
            page++;
        } while (!this.isAbort() && nextid != null && decryptedLinksCurrentSize > decryptedLinksLastSize && decryptedLinksCurrentSize < count);
        if (decryptedLinks.size() == 0) {
            logger.warning("WTF");
        }
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    /**
     * 2020-11-18: This never worked but we should keep it as there isn't much work left to make this work. </br>
     * Do not update this! Do not delete this! Only modify it if you can make it work!
     */
    private void crawlStory(Map<String, Object> entries, final CryptedLink param) throws Exception {
        final boolean pluginNotYetDone = true;
        if (pluginNotYetDone) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "*/*");
        username_url = new Regex(param.getCryptedUrl(), TYPE_STORY).getMatch(0);
        final String story_user_id = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/StoriesPage/{0}/user/id");
        getByUserIDQueryHash(br);
        if (username_url == null || StringUtils.isEmpty(story_user_id) || StringUtils.isEmpty(qHash)) {
            /* This should never happen! */
            return;
        }
        final String url = "/graphql/query/?query_hash=" + qHash + "&variables=%7B%22reel_ids%22%3A%5B%22" + story_user_id + "%22%5D%2C%22tag_names%22%3A%5B%5D%2C%22location_ids%22%3A%5B%5D%2C%22highlight_reel_ids%22%3A%5B%5D%2C%22precomposed_overlay%22%3Afalse%2C%22show_story_viewer_list%22%3Atrue%2C%22story_viewer_fetch_count%22%3A50%2C%22story_viewer_cursor%22%3A%22%22%2C%22stories_video_dash_manifest%22%3Afalse%7D";
        getPage(param, br, url, null, null);
        entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Object> ressourcelist = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "data/reels_media/{0}/items");
        List<Object> qualities;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username_url + " - Story");
        final String subfolderpath = this.username_url + "/" + "story";
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
            this.decryptedLinks.add(dl);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void crawlAlbum(Map<String, Object> entries) throws PluginException {
        long date = JavaScriptEngineFactory.toLong(entries.get("date"), 0);
        if (date == 0) {
            date = JavaScriptEngineFactory.toLong(entries.get("taken_at_timestamp"), 0);
        }
        // is this id? // final String linkid_main = (String) entries.get("id");
        final String typename = (String) entries.get("__typename");
        String linkid_main = (String) entries.get("code");
        // page > 0, now called 'shortcode'
        if (linkid_main == null) {
            linkid_main = (String) entries.get("shortcode");
        }
        String usernameForFilename = null;
        if (this.username_url != null) {
            /* E.g. user crawl a complete user profile --> Username is globally given to set on all crawled objects */
            usernameForFilename = this.username_url;
        } else {
            /* Finding the username "the hard way" */
            try {
                final Map<String, Object> ownerInfo = (Map<String, Object>) entries.get("owner");
                final String userID = (String) ownerInfo.get("id");
                if (userID == null) {
                    /* Should always be given! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* Check if username is in json */
                usernameForFilename = (String) ownerInfo.get("username");
                if (usernameForFilename != null) {
                    /* Cache information for later usage just in case it isn't present in json the next time. */
                    ID_TO_USERNAME.put(userID, usernameForFilename);
                } else if (this.findUsernameDuringHashtagCrawling) {
                    /* Check if we got this username cached */
                    synchronized (ID_TO_USERNAME) {
                        usernameForFilename = ID_TO_USERNAME.get(userID);
                        if (usernameForFilename == null) {
                            /* HTTP request needed to find username! */
                            usernameForFilename = this.getUsernameFromUserIDAltAPI(br, userID);
                            if (usernameForFilename != null) {
                                /* Cache information for later usage */
                                ID_TO_USERNAME.put(userID, usernameForFilename);
                            } else {
                                logger.warning("WTF failed to find username for userID: " + userID);
                            }
                        } else {
                            logger.info("Found cached username: " + usernameForFilename);
                        }
                    }
                } else {
                    logger.info("Username not available for the following ID because this feature has been disabled by the user: " + linkid_main);
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        if (usernameForFilename == null && this.findUsernameDuringHashtagCrawling) {
            /* This should never happen! */
            logger.warning("WTF - no username given!");
        }
        String description = (String) entries.get("caption");
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
        final List<Object> resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, "edge_sidecar_to_children/edges");
        if (StringUtils.equalsIgnoreCase("GraphSidecar", typename) && !this.parameter.matches(TYPE_GALLERY) && (resource_data_list == null || resource_data_list.size() > 1)) {
            final DownloadLink dl = this.createDownloadlink(createSinglePosturl(linkid_main));
            this.decryptedLinks.add(dl);
            distribute(dl);
        } else if (StringUtils.equalsIgnoreCase("GraphImage", typename) && (resource_data_list == null || resource_data_list.size() == 0)) {
            /* Single image */
            crawlSingleMediaObject(entries, linkid_main, date, description, null, usernameForFilename);
        } else if (StringUtils.equalsIgnoreCase("GraphVideo", typename) && (resource_data_list == null || resource_data_list.size() == 0)) {
            /* Single video */
            crawlSingleMediaObject(entries, linkid_main, date, description, null, usernameForFilename);
        } else if (typename != null && typename.matches("Graph[A-Z][a-zA-Z0-9]+") && resource_data_list == null && !this.parameter.matches(TYPE_GALLERY)) {
            /*
             * 2017-05-09: User has added a 'User' URL and in this case a single post contains multiple images (=album) but at this stage
             * the json does not contain the other images --> This has to go back into the decrypter and get crawled as a single item.
             */
            final DownloadLink dl = this.createDownloadlink(createSinglePosturl(linkid_main));
            this.decryptedLinks.add(dl);
            distribute(dl);
        } else if (resource_data_list != null && resource_data_list.size() > 0) {
            final int padLength = getPadLength(resource_data_list.size());
            int counter = 0;
            /* Album */
            for (final Object pictureo : resource_data_list) {
                counter++;
                final String orderid_formatted = String.format(Locale.US, "%0" + padLength + "d", counter);
                entries = (Map<String, Object>) pictureo;
                entries = (Map<String, Object>) entries.get("node");
                crawlSingleMediaObject(entries, linkid_main, date, description, orderid_formatted, usernameForFilename);
            }
        } else {
            /* Single image */
            crawlSingleMediaObject(entries, linkid_main, date, description, null, usernameForFilename);
        }
    }

    /**
     * Crawls json objects of type "GraphImage".
     *
     * @throws PluginException
     */
    private void crawlSingleMediaObject(final Map<String, Object> entries, String linkid_main, final long date, final String description, final String orderid, final String username) throws PluginException {
        final boolean addOrderIDToFilename = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(InstaGramCom.ADD_ORDERID_TO_FILENAMES, InstaGramCom.defaultADD_ORDERID_TO_FILENAMES);
        final String itemID = (String) entries.get("id");
        if (StringUtils.isEmpty(itemID)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final long taken_at_timestamp = JavaScriptEngineFactory.toLong(entries.get("taken_at_timestamp"), 0);
        String server_filename = null;
        final String shortcode = (String) entries.get("shortcode");
        if (linkid_main == null && shortcode != null) {
            // link uid, with /p/ its shortcode
            linkid_main = shortcode;
        }
        final boolean isVideo = ((Boolean) entries.get("is_video")).booleanValue();
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
        if (!StringUtils.isEmpty(dllink)) {
            try {
                server_filename = getFileNameFromURL(new URL(dllink));
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        String filename;
        final String ext;
        if (isVideo) {
            ext = ".mp4";
        } else {
            ext = ".jpg";
        }
        if (prefer_server_filename && server_filename != null) {
            server_filename = jd.plugins.hoster.InstaGramCom.fixServerFilename(server_filename, ext);
            filename = server_filename;
        } else {
            filename = "";
            if (!StringUtils.isEmpty(this.hashtag)) {
                filename = this.hashtag + " - ";
            }
            if (!StringUtils.isEmpty(username)) {
                filename += username + " - ";
            }
            filename += linkid_main;
            if (!StringUtils.isEmpty(shortcode) && !shortcode.equals(linkid_main)) {
                filename += "_" + shortcode;
            }
            if (orderid != null && addOrderIDToFilename) {
                /* By default: Include orderid whenever it is given to prevent duplicate filenames for different files! */
                filename += "_" + orderid;
            }
            filename += ext;
        }
        String hostplugin_url = "instagrammdecrypted://" + linkid_main;
        if (!StringUtils.isEmpty(shortcode)) {
            // hostplugin_url += "/" + shortcode; // Refresh directurl will fail
        }
        final DownloadLink dl = this.createDownloadlink(hostplugin_url);
        String content_url = createSinglePosturl(linkid_main);
        if (isPrivate) {
            /*
             * Without account, private urls look exactly the same as offline urls --> Save private status for better host plugin
             * errorhandling.
             */
            content_url += "?private_url=true";
            dl.setProperty(InstaGramCom.PROPERTY_private_url, true);
        }
        dl.setContentUrl(content_url);
        dl.setLinkID(this.getHost() + "://" + itemID);
        if (fp != null && !"Various".equals(fp.getName())) {
            fp.add(dl);
        }
        dl.setAvailable(true);
        dl.setProperty("decypter_filename", filename);
        dl.setFinalFileName(filename);
        if (date > 0) {
            jd.plugins.hoster.InstaGramCom.setReleaseDate(dl, date);
        }
        if (!StringUtils.isEmpty(shortcode)) {
            dl.setProperty("shortcode", shortcode);
        }
        if (!StringUtils.isEmpty(dllink)) {
            dl.setProperty(InstaGramCom.PROPERTY_DIRECTURL, dllink);
        }
        if (!StringUtils.isEmpty(description)) {
            dl.setComment(description);
            /* For custom packagizer filenames */
            dl.setProperty("description", orderid);
        }
        if (!StringUtils.isEmpty(orderid)) {
            /* For custom packagizer filenames */
            dl.setProperty("orderid", orderid);
        }
        dl.setProperty("postid", itemID);
        if (!StringUtils.isEmpty(this.username_url)) {
            /* Packagizer Property */
            dl.setProperty("uploader", this.username_url);
        }
        dl.setProperty("isvideo", isVideo);
        if (taken_at_timestamp > 0) {
            jd.plugins.hoster.InstaGramCom.setReleaseDate(dl, taken_at_timestamp);
        }
        decryptedLinks.add(dl);
        distribute(dl);
    }

    /*************************************************
     * Methods using alternative API below. All of these require the user to be logged in!
     ***************************************************/
    /** Crawls all saved data of currently logged-in account. */
    private void crawlUserSavedObjectsAltAPI(final CryptedLink param, final Account account, final boolean loggedIN) throws UnsupportedEncodingException, Exception {
        /* Login is mandatory! */
        if (account == null) {
            throw new AccountRequiredException();
        } else if (!loggedIN) {
            final PluginForHost plg = getNewPluginForHostInstance(getHost());
            plg.setBrowser(this.br);
            ((jd.plugins.hoster.InstaGramCom) plg).login(account, false);
        }
        this.hashtag = new Regex(param.getCryptedUrl(), TYPE_TAGS).getMatch(0);
        this.username_url = new Regex(parameter, TYPE_SAVED_OBJECTS).getMatch(0);
        if (this.username_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.setName("saved - " + this.username_url);
        InstaGramCom.prepBRAltAPI(this.br);
        Map<String, Object> entries;
        String nextid = null;
        int page = 0;
        int numberofCrawledItems = 0;
        final String savedItemsFeedBaseURL = InstaGramCom.ALT_API_BASE + "/feed/saved/";
        do {
            logger.info("Crawling page: " + page);
            if (page == 0) {
                /*
                 * TODO: Why does this return 84 items on the first request and then only 9? Check if there is a way to allow this to return
                 * more items!
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
                logger.info("Stopping, 0 items available ...");
                return;
            }
            logger.info("Crawling items: " + numberofitemsOnThisPage);
            nextid = (String) entries.get("next_max_id");
            final boolean more_available = ((Boolean) entries.get("more_available"));
            List<Object> mediaItems = (List<Object>) entries.get("items");
            if (mediaItems == null || mediaItems.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            for (final Object mediaItemO : mediaItems) {
                final Map<String, Object> mediaItem = (Map<String, Object>) mediaItemO;
                crawlAlbumAltAPI((Map<String, Object>) mediaItem.get("media"));
            }
            numberofCrawledItems += numberofitemsOnThisPage;
            logger.info("Total number of items crawled: " + numberofCrawledItems + " of ??");
            if (!more_available) {
                logger.info("Stopping because more_available == false");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because no nextid available");
                break;
            }
            page++;
        } while (!this.isAbort());
        if (decryptedLinks.size() == 0) {
            logger.warning("WTF");
        }
    }

    private void crawlHashtagAltAPI(final CryptedLink param, final Account account, final boolean loggedIN) throws UnsupportedEncodingException, Exception {
        if (account == null) {
            throw new AccountRequiredException();
        } else if (!loggedIN) {
            final PluginForHost plg = getNewPluginForHostInstance(getHost());
            plg.setBrowser(this.br);
            ((jd.plugins.hoster.InstaGramCom) plg).login(account, false);
        }
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
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "No items available for this tag", "No items available for this tag"));
            return;
        }
        String nextid = null;
        int page = 0;
        int numberofCrawledItemsTotal = 0;
        final String hashtagBaseURL = InstaGramCom.ALT_API_BASE + "/feed/tag/" + this.hashtag + "/";
        final long maX_itemsUserSetting = SubConfiguration.getConfig(this.getHost()).getLongProperty(jd.plugins.hoster.InstaGramCom.ONLY_GRAB_X_ITEMS_HASHTAG_CRAWLER_NUMBER, jd.plugins.hoster.InstaGramCom.defaultONLY_GRAB_X_ITEMS_NUMBER);
        logger.info("Expecting to find " + totalNumberofItems + " items");
        do {
            logger.info("Crawling page: " + page);
            if (page == 0) {
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
                logger.info("Stopping, 0 items available ...");
                return;
            }
            logger.info("Crawling items: " + numberofitemsOnThisPage);
            nextid = (String) entries.get("next_max_id");
            final boolean more_available = ((Boolean) entries.get("more_available"));
            List<Object> resource_data_list = (List<Object>) entries.get("items");
            if (resource_data_list == null || resource_data_list.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            for (final Object o : resource_data_list) {
                crawlAlbumAltAPI((Map<String, Object>) o);
            }
            numberofCrawledItemsTotal += numberofitemsOnThisPage;
            logger.info("Total number of items crawled: " + numberofCrawledItemsTotal + " of " + totalNumberofItems);
            if (!more_available) {
                logger.info("Stopping because more_available == false");
                break;
            } else if (numberofCrawledItemsTotal >= totalNumberofItems) {
                logger.info("Stopping because found number of items is higher or equal to expected number of items");
                break;
            } else if (StringUtils.isEmpty(nextid)) {
                logger.info("Stopping because no nextid available");
                break;
            } else if (numberofCrawledItemsTotal >= maX_itemsUserSetting) {
                logger.info("Number of items selected in plugin setting has been crawled --> Done");
                break;
            }
            page++;
        } while (!this.isAbort());
        if (decryptedLinks.size() == 0) {
            logger.warning("WTF");
        }
    }

    private void crawlStoryAltAPI(final CryptedLink param, final String userID) throws UnsupportedEncodingException, Exception {
        if (userID == null || !userID.matches("\\d+")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.username_url = new Regex(param.getCryptedUrl(), TYPE_STORY).getMatch(0);
        if (this.username_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fp.setName("story - " + this.username_url);
        InstaGramCom.prepBRAltAPI(this.br);
        InstaGramCom.getPageAltAPI(this.br, InstaGramCom.ALT_API_BASE + "/feed/user/" + userID + "/reel_media/");
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        List<Object> resource_data_list = (List<Object>) entries.get("items");
        if (resource_data_list == null || resource_data_list.size() == 0) {
            logger.info("User doesn't have any story items");
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl(), "User does not have any story items", "User does not have any story items"));
            return;
        }
        for (final Object o : resource_data_list) {
            /* Every story item should only lead to one media item but this handling should work fine too! */
            crawlAlbumAltAPI((Map<String, Object>) o);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void crawlAlbumAltAPI(Map<String, Object> entries) throws PluginException {
        if (entries == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // final long date = JavaScriptEngineFactory.toLong(entries.get("taken_at"), 0);
        /* 1 = single photo, 2 = single video, 8 = multiple images */
        final long media_type = JavaScriptEngineFactory.toLong(entries.get("media_type"), 1);
        final String linkid_main = (String) entries.get("code");
        if (StringUtils.isEmpty(linkid_main)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String usernameForFilename = (String) JavaScriptEngineFactory.walkJson(entries, "user/username");
        if (usernameForFilename == null) {
            /* This should never happen! */
            logger.warning("WTF - no username given!");
        }
        final String description = (String) JavaScriptEngineFactory.walkJson(entries, "caption/text");
        if (media_type == 1 || media_type == 2) {
            /* Single image */
            crawlSingleMediaObjectAltAPI(entries, linkid_main, description, null, usernameForFilename);
        } else if (media_type == 8) {
            /* Multiple images */
            final List<Object> resource_data_list = (List) JavaScriptEngineFactory.walkJson(entries, "carousel_media");
            final int padLength = getPadLength(resource_data_list.size());
            int counter = 0;
            /* Album */
            for (final Object pictureo : resource_data_list) {
                counter++;
                final String orderid_formatted = String.format(Locale.US, "%0" + padLength + "d", counter);
                entries = (Map<String, Object>) pictureo;
                crawlSingleMediaObjectAltAPI(entries, linkid_main, description, orderid_formatted, usernameForFilename);
            }
        } else {
            /* This should never happen */
            logger.info("WTF unknown media_type");
        }
    }

    private void crawlSingleMediaObjectAltAPI(final Map<String, Object> entries, String linkid_main, final String description, final String orderid, final String username) throws PluginException {
        if (entries == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String itemID = (String) entries.get("id");
        final boolean is_reel_media = entries.containsKey("is_reel_media") ? ((Boolean) entries.get("is_reel_media")).booleanValue() : false;
        final long taken_at_timestamp = JavaScriptEngineFactory.toLong(entries.get("taken_at"), 0);
        String server_filename = null;
        final String shortcode = (String) entries.get("code");
        if (linkid_main == null && shortcode != null) {
            // link uid, with /p/ its shortcode
            linkid_main = shortcode;
        }
        final Object videoO = entries.get("video_versions");
        final boolean isVideo = videoO != null;
        final String dllink = InstaGramCom.getBestQualityURLAltAPI(entries);
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("WTF failed to find direct-url");
        }
        if (!StringUtils.isEmpty(dllink)) {
            try {
                server_filename = getFileNameFromURL(new URL(dllink));
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        String filename;
        final String ext;
        if (isVideo) {
            ext = ".mp4";
        } else {
            ext = ".jpg";
        }
        if (prefer_server_filename && server_filename != null) {
            /* User prefers server-filename */
            server_filename = jd.plugins.hoster.InstaGramCom.fixServerFilename(server_filename, ext);
            filename = server_filename;
        } else {
            filename = "";
            if (!StringUtils.isEmpty(this.hashtag)) {
                filename = this.hashtag + " - ";
            }
            if (!StringUtils.isEmpty(username)) {
                filename += username + " - ";
            }
            filename += linkid_main;
            if (!StringUtils.isEmpty(shortcode) && !shortcode.equals(linkid_main)) {
                filename += "_" + shortcode;
            }
            if (orderid != null) {
                /* Include orderid whenever it is given to prevent duplicate filenames for different files! */
                filename += "_" + orderid;
            }
            filename += ext;
        }
        String hostplugin_url = "instagrammdecrypted://" + linkid_main;
        if (!StringUtils.isEmpty(shortcode)) {
            // hostplugin_url += "/" + shortcode; // Refresh directurl will fail
        }
        final DownloadLink dl = this.createDownloadlink(hostplugin_url);
        String content_url = createSinglePosturl(linkid_main);
        if (this.isPrivate) {
            /*
             * Without account, private urls look exactly the same as offline urls --> Save private status for better host plugin
             * errorhandling.
             */
            content_url += "?private_url=true";
            dl.setProperty(InstaGramCom.PROPERTY_private_url, true);
        }
        dl.setContentUrl(content_url);
        dl.setLinkID(this.getHost() + "://" + itemID);
        if (fp != null && !"Various".equals(fp.getName())) {
            fp.add(dl);
        }
        dl.setAvailable(true);
        /* Filename should usually be given! */
        if (filename != null) {
            dl.setProperty("decypter_filename", filename);
            dl.setFinalFileName(filename);
        }
        if (taken_at_timestamp > 0) {
            jd.plugins.hoster.InstaGramCom.setReleaseDate(dl, taken_at_timestamp);
        }
        if (!StringUtils.isEmpty(shortcode)) {
            dl.setProperty("shortcode", shortcode);
        }
        if (!StringUtils.isEmpty(dllink)) {
            dl.setProperty(InstaGramCom.PROPERTY_DIRECTURL, dllink);
        }
        if (!StringUtils.isEmpty(description)) {
            dl.setComment(description);
            /* For custom packagizer filenames */
            dl.setProperty("description", orderid);
        }
        if (!StringUtils.isEmpty(orderid)) {
            /* For custom packagizer filenames */
            dl.setProperty("orderid", orderid);
        }
        if (!StringUtils.isEmpty(itemID)) {
            dl.setProperty("postid", itemID);
        }
        if (!StringUtils.isEmpty(this.username_url)) {
            /* Packagizer Property */
            dl.setProperty("uploader", this.username_url);
        }
        dl.setProperty("isvideo", isVideo);
        if (taken_at_timestamp > 0) {
            jd.plugins.hoster.InstaGramCom.setReleaseDate(dl, taken_at_timestamp);
        }
        /* Very important in case directurl has to be refreshed! */
        if (is_reel_media) {
            dl.setProperty(InstaGramCom.PROPERTY_is_part_of_story, true);
        }
        /* We always get the better quality images via this API! */
        dl.setProperty(InstaGramCom.PROPERTY_has_tried_to_crawl_original_url, true);
        decryptedLinks.add(dl);
        distribute(dl);
    }

    private String createSinglePosturl(final String p_id) {
        return String.format("https://www.instagram.com/p/%s", p_id);
    }

    private void prepBrAjax(final Browser br) {
        br.getHeaders().put("Accept", "*/*");
        String csrftoken = br.getCookie("instagram.com", "csrftoken");
        if (csrftoken == null) {
            /* 2020-11-05 */
            csrftoken = PluginJSonUtils.getJson(br, "csrf_token");
        }
        if (!StringUtils.isEmpty(csrftoken)) {
            br.getHeaders().put("X-CSRFToken", csrftoken);
        }
        if (fbAppId != null) {
            br.getHeaders().put("X-IG-App-ID", fbAppId);
        }
        br.getHeaders().put("X-IG-WWW-Claim", "0"); // only ever seen this as 0
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-01-21: Set to 1 to avoid download issues and try not to perform too many requests at the same time. */
        return 1;
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
    }
}
