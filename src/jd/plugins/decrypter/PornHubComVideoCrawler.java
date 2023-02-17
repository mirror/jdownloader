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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.DirectHTTP;
import jd.plugins.hoster.PornHubCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornHubComVideoCrawler extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public PornHubComVideoCrawler(PluginWrapper wrapper) {
        super(wrapper);
        /* Set request-limits for all supported domains */
        for (final String pluginDomains[] : getPluginDomains()) {
            for (final String pluginDomain : pluginDomains) {
                Browser.setRequestIntervalLimitGlobal(pluginDomain, 333);
            }
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pornhub.com", "pornhub.org", "pornhubpremium.com", "pornhubpremium.org", "modelhub.com" });
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
            String pattern = "https?://(?:www\\.|[a-z]{2}\\.)?" + buildHostsPatternPart(domains) + "/(?:";
            /* Single video */
            pattern += ".*\\?viewkey=[a-z0-9]+|";
            /* Single video embeded */
            pattern += "embed/[a-z0-9]+|";
            pattern += "embed_player\\.php\\?id=\\d+|";
            /* Single video modelhub.com 2021-01-06 */
            pattern += "video/ph[a-f0-9]+|";
            /* All photo albums of a user modelhub.com */
            pattern += "[^/]+/photos$|";
            /* All videos of a pornstar/model */
            pattern += "(pornstar|model)/[^/]+(/gifs(/video|/public)?|/public|/videos(/premium|/paid|/upload|/public)?|/from_videos|/photos)?|";
            /* All videos of a channel */
            pattern += "channels/[A-Za-z0-9\\-_]+(?:/videos)?|";
            /* All videos of a user */
            pattern += "users/[^/]+(?:/gifs(/public|/video|/from_videos)?|/videos(/public)?)?|";
            /* Video playlist */
            pattern += "playlist/\\d+";
            pattern += ")";
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_PORNSTAR_VIDEOS_UPLOAD        = "(?i)https?://[^/]+/pornstar/([^/]+)/videos/upload$";
    private static final String TYPE_PORNSTAR_VIDEOS               = "(?i)https?://[^/]+/pornstar/([^/]+)/videos/?$";
    private static final String TYPE_MODEL_VIDEOS                  = "(?i)https?://[^/]+/model/([^/]+)/videos/?$";
    private static final String TYPE_USER_FAVORITES                = "(?i)https?://[^/]+/users/([^/]+)/videos(/?|/favorites/?)$";
    private static final String TYPE_USER_VIDEOS_PUBLIC            = "(?i)https?://[^/]+/users/([^/]+)/videos/public$";
    private static final String TYPE_CHANNEL_VIDEOS                = "(?i)https?://[^/]+/channels/([^/]+)(/?|/videos/?)$";
    public static final String  PATTERN_MODELHUB_USER_PHOTO_ALBUMS = "https?://[^/]+/([^/]+)/photos";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String preferredSubdomain = PornHubCom.getPreferredSubdomain(param.getCryptedUrl());
        param.setCryptedUrl(param.getCryptedUrl().replaceFirst("^https?://[^/]+/", "https://" + preferredSubdomain + PornHubCom.getConfiguredDomainURL(this.getHost(), Browser.getHost(param.getCryptedUrl())) + "/"));
        br.setFollowRedirects(true);
        PornHubCom.prepBr(br);
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        final PornHubCom hosterPlugin = (PornHubCom) this.getNewPluginForHostInstance(this.getHost());
        if (account != null) {
            hosterPlugin.login(account, false);
        }
        if (PornHubCom.requiresPremiumAccount(param.getCryptedUrl()) && (account == null || account.getType() != AccountType.PREMIUM)) {
            throw new AccountRequiredException();
        }
        if (param.getCryptedUrl().matches("(?i).*/playlist/.*")) {
            PornHubCom.getFirstPageWithAccount(hosterPlugin, account, param.getCryptedUrl());
            handleErrorsAndCaptcha(this.br, account);
            return crawlAllVideosOfAPlaylist(account);
        } else if (param.getCryptedUrl().matches("(?i).*/gifs.*")) {
            PornHubCom.getFirstPageWithAccount(hosterPlugin, account, param.getCryptedUrl());
            handleErrorsAndCaptcha(this.br, account);
            return crawlAllGifsOfAUser(param, account);
        } else if (param.getCryptedUrl().matches(PATTERN_MODELHUB_USER_PHOTO_ALBUMS)) {
            return this.crawlAllPhotoAlbumsOfAUserModelhub(param.getCryptedUrl(), account);
        } else if (param.getCryptedUrl().matches("(?i).*/photos$")) {
            return this.crawlAllPhotoAlbumsOfAUser(param, account);
        } else if (param.getCryptedUrl().matches("(?i).*/model/.*")) {
            return crawlModel(br, param, account);
        } else if (param.getCryptedUrl().matches("(?i).*/pornstar/.*")) {
            return this.crawlPornstar(br, param, account);
        } else if (param.getCryptedUrl().matches("(?i).*/(?:users|channels).*")) {
            if (new Regex(br.getURL(), "/(model|pornstar)/").matches()) { // Handle /users/ that has been switched to model|pornstar
                logger.info("Users->Model|pornstar");
                PornHubCom.getFirstPageWithAccount(hosterPlugin, account, param.getCryptedUrl());
                handleErrorsAndCaptcha(this.br, account);
                return crawlAllVideosOf(br, account, new HashSet<String>());
            } else {
                logger.info("Users/Channels");
                return crawlAllVideosOfAUser(param, hosterPlugin, account);
            }
        } else {
            return crawlSingleVideo(this.br, param, account);
        }
    }

    private void handleErrorsAndCaptcha(final Browser br, final Account account) throws Exception {
        if (StringUtils.containsIgnoreCase(br.getURL(), "/premium/login")) {
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)<h2>Upgrade now<")) {
            throw new AccountRequiredException();
        }
        if (AbstractRecaptchaV2.containsRecaptchaV2Class(br) && br.containsHTML("/captcha/validate\\?token=")) {
            final Form form = br.getFormByInputFieldKeyValue("captchaType", "1");
            logger.info("Detected captcha method \"reCaptchaV2\" for this host");
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(form);
        }
        if (br.containsHTML("(?i)>\\s*Sorry, but this video is private") && br.containsHTML("href\\s*=\\s*\"/login\"")) {
            /* Either we're not nogged in or current account does not have permission to view this content. */
            throw new AccountRequiredException();
        }
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser ret = super.createNewBrowserInstance();
        PornHubCom.setSSLSocketStreamOptions(ret);
        return ret;
    }

    @Override
    public String getUserInput(String title, String message, CryptedLink link) throws DecrypterException {
        try {
            return super.getUserInput(title, message, link);
        } catch (DecrypterException e) {
            return null;
        }
    }

    private ArrayList<DownloadLink> crawlModel(final Browser br, final CryptedLink param, final Account account) throws Exception {
        final PornHubCom hosterPlugin = (PornHubCom) this.getNewPluginForHostInstance(this.getHost());
        PornHubCom.getFirstPageWithAccount(hosterPlugin, account, param.getCryptedUrl());
        handleErrorsAndCaptcha(this.br, account);
        final String model = new Regex(param.getCryptedUrl(), "/model/([^/]+)").getMatch(0);
        final String mode = new Regex(param.getCryptedUrl(), "/model/[^/]+/(.+)").getMatch(0);
        /* Main profile URL --> Assume user wants to have all videos of that profile */
        logger.info("Model:" + model + " | Mode:" + mode);
        if (StringUtils.isEmpty(mode)) {
            /* Collect all available categories and let them go through this crawler again. */
            final String pages[] = br.getRegex("(/model/" + Pattern.quote(model) + "/(?:videos|gifs))").getColumn(0);
            if (pages.length > 0) {
                /* Let crawler crawl each page one by one. */
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                for (final String page : new HashSet<String>(Arrays.asList(pages))) {
                    ret.add(createDownloadlink(br.getURL(page).toString()));
                }
                return ret;
            } else {
                return crawlAllVideosOf(br, account, new HashSet<String>());
            }
        } else {
            return crawlAllVideosOf(br, account, new HashSet<String>());
        }
    }

    private ArrayList<DownloadLink> crawlPornstar(final Browser br, final CryptedLink param, final Account account) throws Exception {
        final PornHubCom hosterPlugin = (PornHubCom) this.getNewPluginForHostInstance(this.getHost());
        PornHubCom.getFirstPageWithAccount(hosterPlugin, account, param.getCryptedUrl());
        handleErrorsAndCaptcha(this.br, account);
        final String pornstar = new Regex(param.getCryptedUrl(), "/pornstar/([^/]+)").getMatch(0);
        final String mode = new Regex(param.getCryptedUrl(), "/pornstar/[^/]+/(.+)").getMatch(0);
        /* Main profile URL --> Assume user wants to have all videos of that profile */
        /* Main profile URL --> Assume user wants to have all videos of that profile */
        logger.info("Pornstar:" + pornstar + "|Mode:" + mode);
        if (StringUtils.isEmpty(mode)) {
            final String pages[] = br.getRegex("(/pornstar/" + Pattern.quote(pornstar) + "/(?:videos|gifs))").getColumn(0);
            if (pages.length > 0) {
                /* Let crawler crawl each page one by one. */
                final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
                for (final String page : new HashSet<String>(Arrays.asList(pages))) {
                    ret.add(createDownloadlink(br.getURL(page).toString()));
                }
                return ret;
            } else {
                return crawlAllVideosOf(br, account, new HashSet<String>());
            }
        } else {
            return crawlAllVideosOf(br, account, new HashSet<String>());
        }
    }

    /** Handles pornhub.com/bla/(model|pornstar)/bla */
    private ArrayList<DownloadLink> crawlAllVideosOf(final Browser br, final Account account, final Set<String> dupes) throws Exception {
        if (isOfflineGeneral(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Set<String> pages = new HashSet<String>();
        int page = 0;
        int maxPage = -1;
        int addedItems = 0;
        int foundItems = 0;
        String ajaxPaginationURL = null;
        final String containerURL = br.getURL();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        do {
            int numberofActuallyAddedItems = 0;
            int numberOfDupeItems = 0;
            page++;
            logger.info(String.format("Crawling page %s| %d / %d", br.getURL(), page, maxPage));
            final Set<String> viewKeys = new HashSet<String>();
            if (account != null && page == 1) {
                // layout has changed, we only want to deep dive into other pages on first page
                final String paidVideosSection = findVideoSection(br, "paidVideosSection");// /videos/paid
                if (paidVideosSection != null) {
                    // /videos/premium contains paid/available premium videos while
                    // /videos/paid contains all un-paid videos
                    if (br.containsHTML(Pattern.quote(new URL(br.getURL() + "/paid").getPath()))) {
                        final Browser brc = br.cloneBrowser();
                        PornHubCom.getPage(brc, brc.createGetRequest(br.getURL() + "/paid"));
                        ret.addAll(crawlAllVideosOf(brc, account, dupes));
                    }
                    if (br.containsHTML(Pattern.quote(new URL(br.getURL() + "/premium").getPath()))) {
                        final Browser brc = br.cloneBrowser();
                        PornHubCom.getPage(brc, brc.createGetRequest(br.getURL() + "/premium"));
                        ret.addAll(crawlAllVideosOf(brc, account, dupes));
                    }
                }
                final String fanVideosSection = findVideoSection(br, "fanVideosSection");// /videos/fanonly
                if (false && fanVideosSection != null) {
                    final Browser brc = br.cloneBrowser();
                    PornHubCom.getPage(brc, brc.createGetRequest(br.getURL() + "/fanonly"));
                    ret.addAll(crawlAllVideosOf(brc.cloneBrowser(), account, dupes));
                }
            }
            for (final String section : new String[] { "moreData", "mostRecentVideosSection", "pornstarsVideoSection" }) {
                final String sectionContent = findVideoSection(br, section);
                if (sectionContent != null) {
                    final String[] vKeys = new Regex(sectionContent, "(?:_|-)vkey\\s*=\\s*\"(.+?)\"").getColumn(0);
                    if (vKeys != null) {
                        viewKeys.addAll(Arrays.asList(vKeys));
                    }
                }
            }
            if (viewKeys.size() == 0) {
                final String[] vKeysAll = br.getRegex("(?:_|-)vkey\\s*=\\s*\"(.+?)\"").getColumn(0);
                if (vKeysAll != null) {
                    viewKeys.addAll(Arrays.asList(vKeysAll));
                }
            }
            if (viewKeys.size() == 0) {
                logger.info("no vKeys found!");
            } else {
                foundItems += viewKeys.size();
                for (final String viewkey : viewKeys) {
                    if (dupes.add(viewkey)) {
                        final DownloadLink dl = createDownloadlink(br.getURL("/view_video.php?viewkey=" + viewkey).toString());
                        dl.setContainerUrl(containerURL);
                        ret.add(dl);
                        /* Makes testing easier for devs */
                        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                            distribute(dl);
                        }
                        numberofActuallyAddedItems++;
                        addedItems++;
                    } else {
                        numberOfDupeItems++;
                    }
                }
            }
            if (numberofActuallyAddedItems == 0) {
                logger.info("Stopping because this page did not contain any NEW content: page=" + page + "|max_page=" + maxPage + "|all_found=" + foundItems + "|all_added=" + addedItems + "|page_found:" + (viewKeys != null ? viewKeys.size() : -1) + "|page_added=" + numberofActuallyAddedItems + "|page_dupes=" + numberOfDupeItems);
                break;
            } else {
                logger.info("found NEW content: page=" + page + "|max_page=" + maxPage + "|all_found=" + foundItems + "|all_added=" + addedItems + "|page_found:" + (viewKeys != null ? viewKeys.size() : -1) + "|page_added=" + numberofActuallyAddedItems + "|page_dupes=" + numberOfDupeItems);
            }
            logger.info(String.format("Found %d new items", numberofActuallyAddedItems));
            final String next = br.getRegex("page_next[^\"]*\"[^>]*>\\s*<a href=\"([^\"]*?page=\\d+)\"").getMatch(0);
            final String nextAjax = br.getRegex("onclick=\"[^\"]*loadMoreDataStream\\(([^\\)]+)\\)").getMatch(0);
            if (nextAjax != null) {
                final String[] ajaxVars = nextAjax.replace("'", "").split(", ");
                if (ajaxVars == null || ajaxVars.length < 3) {
                    logger.info("Incompatible ajax data");
                    break;
                }
                ajaxPaginationURL = ajaxVars[0];
                // final String nextPageStr = ajaxVars[2];
                final String maxPageStr = ajaxVars[1];
                logger.info("Found max_page --> " + maxPageStr);
                maxPage = Integer.parseInt(maxPageStr);
            }
            if (isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (next != null && pages.add(next)) {
                logger.info("HTML pagination handling - parsing page: " + next);
                PornHubCom.getPage(br, br.createGetRequest(next));
            } else if (ajaxPaginationURL != null && page < maxPage) {
                /* E.g. max page given = 4 --> Stop AFTER counter == 3 as 3+1 == 4 */
                logger.info("Ajax pagination handling");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                final int postPage = page + 1;
                PornHubCom.getPage(br, br.createPostRequest(ajaxPaginationURL + "&page=" + postPage, "o=best&page=" + postPage));
            } else {
                logger.info("Stopping because: no next page! page=" + page + "|max_page=" + maxPage);
                break;
            }
        } while (true);
        return ret;
    }

    private String findVideoSection(Browser br, String section) {
        return br.getRegex("(<ul[^>]*(?:class\\s*=\\s*\"videos[^>]*id\\s*=\\s*\"" + section + "\"|[^>]*id\\s*=\\s*\"" + section + "\"[^>]*class\\s*=\\s*\"videos[^>]).*?)(<ul\\s*class\\s*=\\s*\"videos|</ul>)").getMatch(0);
    }

    private ArrayList<DownloadLink> crawlAllVideosOfAUser(final CryptedLink param, final PornHubCom hosterPlugin, final Account account) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        /* 2021-08-24: At this moment we never try to find the real/"nice" username - we always use the one that's in our URL. */
        // String username = null;
        final String galleryname;
        if (param.getCryptedUrl().matches(TYPE_PORNSTAR_VIDEOS_UPLOAD)) {
            galleryname = "Uploads";
        } else if (param.getCryptedUrl().matches(TYPE_PORNSTAR_VIDEOS)) {
            galleryname = "Upload Videos";
        } else if (param.getCryptedUrl().matches(TYPE_MODEL_VIDEOS)) {
            galleryname = "Upload Videos";
        } else if (param.getCryptedUrl().matches(TYPE_USER_FAVORITES)) {
            galleryname = "Favorites";
        } else if (param.getCryptedUrl().matches(TYPE_USER_VIDEOS_PUBLIC)) {
            galleryname = "Public Videos";
        } else if (param.getCryptedUrl().matches(TYPE_CHANNEL_VIDEOS)) {
            if (!param.getCryptedUrl().endsWith("/videos") && !param.getCryptedUrl().endsWith("/")) {
                param.setCryptedUrl(param.getCryptedUrl() + "/videos");
            }
            galleryname = "Channel Uploads";
        } else {
            galleryname = null;
        }
        /* Only access page if it hasn't been accessed before */
        PornHubCom.getFirstPageWithAccount(hosterPlugin, account, param.getCryptedUrl());
        handleErrorsAndCaptcha(this.br, account);
        PornHubCom.getPage(br, param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*There are no videos\\.\\.\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2021-08-24: E.g. given for "users/username/videos(/favorites)?" */
        final String totalNumberofItems = br.getRegex("class=\"totalSpan\">(\\d+)</span>").getMatch(0);
        final String totalNumberofItemsText;
        if (totalNumberofItems != null) {
            totalNumberofItemsText = totalNumberofItems;
        } else {
            totalNumberofItemsText = "Unknown";
        }
        final String seeAllURL = br.getRegex("(" + Regex.escape(br._getURL().getPath()) + "/[^\"]+)\" class=\"seeAllButton greyButton float-right\">").getMatch(0);
        if (seeAllURL != null) {
            /**
             * E.g. users/bla/videos --> /users/bla/videos/favorites </br>
             * Without this we might only see some of all items and no pagination which is needed to be able to find all items.
             */
            logger.info("Found seeAllURL: " + seeAllURL);
            PornHubCom.getPage(br, seeAllURL);
        }
        final FilePackage fp;
        final String username_url = getUsernameFromURL(br);
        if (username_url != null && galleryname != null) {
            fp = FilePackage.getInstance();
            fp.setName(username_url + " - " + galleryname);
        } else if (username_url != null) {
            fp = FilePackage.getInstance();
            fp.setName(username_url);
        } else if (galleryname != null) {
            fp = FilePackage.getInstance();
            fp.setName(galleryname);
        } else {
            fp = null;
        }
        int page = 1;
        int max_entries_per_page = 40;
        final Set<String> dupes = new HashSet<String>();
        String publicVideosHTMLSnippet = null;
        String base_url = null;
        do {
            boolean htmlSourceNeedsFiltering = true;
            if (page > 1) {
                // PornHubCom.getPage(br, "/users/" + username + "/videos/public/ajax?o=mr&page=" + page);
                // br.postPage(parameter + "/ajax?o=mr&page=" + page, "");
                /* e.g. different handling for '/model/' URLs */
                final String nextpage_url_old = br.getRegex("class=\"page_next\"[^>]*>\\s*<a href=\"(/[^\"]+\\?page=\\d+)\"").getMatch(0);
                final Regex nextpageAjaxRegEx = br.getRegex("onclick=\"loadMoreData\\(\\'(/users/[^<>\"\\']+)',\\s*'(\\d+)',\\s*'\\d+'\\)");
                final String nextpage_url;
                if (nextpage_url_old != null) {
                    /* Old handling */
                    nextpage_url = nextpage_url_old;
                    logger.info("Auto-found nextpage_url via old handling: " + nextpage_url);
                } else if (nextpageAjaxRegEx.matches()) {
                    /* New ajax handling */
                    /* Additional fail-safe */
                    final String nextPageStr = nextpageAjaxRegEx.getMatch(1);
                    if (!nextPageStr.equalsIgnoreCase(Integer.toString(page))) {
                        logger.warning("Expected nextPage: " + page + " | Found nextPage: " + nextPageStr);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final UrlQuery nextpageAjaxQuery = UrlQuery.parse(nextpageAjaxRegEx.getMatch(0));
                    nextpageAjaxQuery.addAndReplace("page", nextPageStr);
                    nextpage_url = nextpageAjaxQuery.toString();
                    htmlSourceNeedsFiltering = false;
                    logger.info("Auto-found nextpage_url via ajax handling: " + nextpage_url);
                } else {
                    /* Ajax fallback handling */
                    nextpage_url = base_url + "/ajax?o=mr&page=" + page;
                    logger.info("Custom build nextpage_url: " + nextpage_url);
                    br.getHeaders().put("Accept", "*/*");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    htmlSourceNeedsFiltering = false;
                }
                PornHubCom.getPage(br, br.createPostRequest(nextpage_url, ""));
                if (br.getHttpConnection().getResponseCode() == 404) {
                    break;
                }
            } else {
                /* Set this on first loop */
                base_url = br.getURL();
            }
            if (htmlSourceNeedsFiltering) {
                /* only parse videos of the user/pornstar/channel, avoid catching unrelated content e.g. 'related' videos */
                if (param.getCryptedUrl().contains("/pornstar/") || param.getCryptedUrl().contains("/model/")) {
                    publicVideosHTMLSnippet = br.getRegex("(class=\"videoUList[^\"]*?\".*?</section>)").getMatch(0);
                } else if (param.getCryptedUrl().contains("/channels/")) {
                    publicVideosHTMLSnippet = br.getRegex("<ul[^>]*?id=\"showAllChanelVideos\">.*?</ul>").getMatch(-1);
                    max_entries_per_page = 36;
                } else {
                    // publicVideosHTMLSnippet = br.getRegex("(>public Videos<.+?(>Load More<|</section>))").getMatch(0);
                    publicVideosHTMLSnippet = br.getRegex("(class=\"videoUList[^\"]*?\".*?</section>)").getMatch(0);
                }
                if (publicVideosHTMLSnippet != null) {
                    logger.info("publicVideosHTMLSnippet: " + publicVideosHTMLSnippet); // For debugging
                }
            } else {
                /* Pagination result --> Ideal as a source as it only contains the content we need */
                publicVideosHTMLSnippet = br.toString();
            }
            if (publicVideosHTMLSnippet == null) {
                throw new DecrypterException("Decrypter broken for link: " + param.getCryptedUrl());
            }
            final String[] viewkeys = new Regex(publicVideosHTMLSnippet, "(?:_|-)vkey\\s*=\\s*\"([a-z0-9]+)\"").getColumn(0);
            if (viewkeys == null || viewkeys.length == 0) {
                break;
            }
            for (final String viewkey : viewkeys) {
                if (dupes.add(viewkey)) {
                    // logger.info("http://www." + this.getHost() + "/view_video.php?viewkey=" + viewkey); // For debugging
                    final DownloadLink dl = createDownloadlink(br.getURL("/view_video.php?viewkey=" + viewkey).toString());
                    if (fp != null) {
                        fp.add(dl);
                    }
                    ret.add(dl);
                    if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        distribute(dl);
                    }
                }
            }
            logger.info("Links found on current page " + page + ": " + viewkeys.length + " | Found: " + ret.size() + " of " + totalNumberofItemsText);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (viewkeys.length < max_entries_per_page) {
                logger.info("Stopping because: Current page contains less items than: " + max_entries_per_page);
                break;
            } else {
                page++;
            }
        } while (!this.isAbort());
        return ret;
    }

    private static String getUsernameFromURL(final Browser br) {
        return getUsernameFromURL(br.getURL());
    }

    private static String getUsernameFromURL(final String url) {
        final String usernameModelhub = new Regex(url, "(?i)https?://[^/]+/([^/]+)/(videos|photos|bio)$").getMatch(0);
        if (usernameModelhub != null) {
            return usernameModelhub;
        } else {
            final String usernamePornhub = new Regex(url, "(?i)/(?:users|model|pornstar|channels)/([^/]+)").getMatch(0);
            return usernamePornhub;
        }
    }

    private ArrayList<DownloadLink> crawlAllGifsOfAUser(final CryptedLink param, final Account account) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final boolean webm = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(PornHubCom.GIFS_WEBM, true);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        FilePackage fp = null;
        if (StringUtils.endsWithCaseInsensitive(param.getCryptedUrl(), "/gifs/public")) {
            PornHubCom.getPage(br, param.getCryptedUrl());
            // user->pornstar redirect
            if (!br.getURL().matches("(?i).+/gifs/public")) {
                PornHubCom.getPage(br, br.getURL() + "/gifs/public");
            }
            final String user = getUsernameFromURL(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName(user + "'s GIFs");
            }
        } else if (StringUtils.endsWithCaseInsensitive(param.getCryptedUrl(), "/gifs/video")) {
            PornHubCom.getPage(br, param.getCryptedUrl());
            // user->pornstar redirect
            if (!br.getURL().matches("(?i).+/gifs/video")) {
                PornHubCom.getPage(br, br.getURL() + "/gifs/video");
            }
            final String user = getUsernameFromURL(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName("GIFs From " + user + "'s Videos");
            }
        } else if (StringUtils.endsWithCaseInsensitive(param.getCryptedUrl(), "/gifs/from_videos")) {
            PornHubCom.getPage(br, param.getCryptedUrl());
            // user->pornstar redirect
            if (!br.getURL().matches("(?i).+/gifs/from_videos") || !br.getURL().matches("(?i).+/gifs/video")) {
                PornHubCom.getPage(br, br.getURL() + "/gifs/video");
            }
            final String user = getUsernameFromURL(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName("GIFs From " + user + "'s Videos");
            }
        } else {
            PornHubCom.getPage(br, param.getCryptedUrl());
            /* Those URLs will go back into this crawler */
            ret.add(createDownloadlink(br.getURL() + "/public"));
            ret.add(createDownloadlink(br.getURL() + "/video"));
            return ret;
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int page = 1;
        final int max_entries_per_page = 50;
        int links_found_in_this_page;
        final Set<String> dupes = new HashSet<String>();
        String base_url = null;
        do {
            if (page > 1) {
                final String nextpage_url = base_url + "/ajax?page=" + page;
                PornHubCom.getPage(br, br.createPostRequest(nextpage_url, ""));
                if (br.getHttpConnection().getResponseCode() == 404) {
                    break;
                }
            } else {
                /* Set this on first loop */
                base_url = br.getURL();
            }
            final String[] items = new Regex(br.toString(), "(<li\\s*id\\s*=\\s*\"gif\\d+\"[^<]*>.*?</li>)").getColumn(0);
            if (items == null || items.length == 0) {
                break;
            }
            for (final String item : items) {
                final String viewKey = new Regex(item, "/gif/(\\d+)").getMatch(0);
                if (viewKey != null && dupes.add(viewKey)) {
                    final String name = new Regex(item, "class\\s*=\\s*\"title\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
                    final DownloadLink dl = createDownloadlink(br.getURL("/gif/" + viewKey).toString());
                    final String ext;
                    if (webm) {
                        ext = ".webm";
                    } else {
                        ext = ".gif";
                    }
                    if (name == null || StringUtils.containsIgnoreCase(name, "view_video.php")) {
                        dl.setName(viewKey + ext);
                    } else {
                        dl.setName(name + "_" + viewKey + ext);
                    }
                    /* Force fast linkcheck */
                    dl.setAvailable(true);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    ret.add(dl);
                    if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        distribute(dl);
                    }
                }
            }
            logger.info("Links found in page " + page + ": " + items.length + " | Total: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            }
            links_found_in_this_page = items.length;
            page++;
        } while (links_found_in_this_page >= max_entries_per_page);
        return ret;
    }

    private ArrayList<DownloadLink> crawlAllPhotoAlbumsOfAUser(final CryptedLink param, final Account account) throws Exception {
        PornHubCom.getPage(br, param.getCryptedUrl());
        handleErrorsAndCaptcha(this.br, account);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("seeAllButton")) {
            /* More/all albums are available on modelhub.com -> Let modelhub crawler do the job */
            logger.info("Looks like more photo albums of this user are available on modelhub.com");
            final String username = getUsernameFromURL(br);
            if (username == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            return crawlAllPhotoAlbumsOfAUserModelhub("https://www.modelhub.com/" + username + "/photos", account);
        } else {
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            final String[] photoAlbumURLs = br.getRegex("(/album/\\d+)").getColumn(0);
            if (photoAlbumURLs == null || photoAlbumURLs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String photoAlbumURL : photoAlbumURLs) {
                ret.add(this.createDownloadlink(br.getURL(photoAlbumURL).toString()));
            }
            return ret;
        }
    }

    private ArrayList<DownloadLink> crawlAllPhotoAlbumsOfAUserModelhub(final String url, final Account account) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        PornHubCom.getPage(br, url);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String username = getUsernameFromURL(url);
        if (username == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] jsPieces = br.getRegex("<script type=\"text/javascript\">(.*?)</script>").getColumn(0);
        if (jsPieces == null || jsPieces.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String jsPiece : jsPieces) {
            final Regex photojsregex = new Regex(jsPiece, "PHOTOS_ARRAY_(\\d+) = (\\{.*?\\})\\s+var");
            final String albumID = photojsregex.getMatch(0);
            final String photojs = photojsregex.getMatch(1);
            if (albumID == null) {
                /* Skip non matching js snippets */
                continue;
            }
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(photojs);
            /* Find album title */
            final Map<String, Object> photoIds = (Map<String, Object>) entries.get("photoIds");
            String albumTitle = null;
            final Iterator<Entry<String, Object>> iterator = photoIds.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<String, Object> entry = iterator.next();
                final Map<String, Object> photoInfo = (Map<String, Object>) entry.getValue();
                final String title = (String) photoInfo.get("title");
                if (title != null) {
                    albumTitle = new Regex(title, "(?i)Photo from album \"(.+)\"").getMatch(0);
                    if (albumTitle != null) {
                        break;
                    }
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            if (albumTitle != null) {
                fp.setName(username + " - " + albumID + " - " + albumTitle);
            } else {
                fp.setName(username + " - " + albumID);
            }
            final List<String> imageurls = (List<String>) entries.get("urls");
            for (final String imageurl : imageurls) {
                final DownloadLink image = this.createDownloadlink(imageurl);
                final String nicerFilenameThanDefault = new Regex(imageurl, "(original_.+)$").getMatch(0);
                if (nicerFilenameThanDefault != null) {
                    image.setFinalFileName(nicerFilenameThanDefault);
                }
                image.setProperty(PornHubCom.PROPERTY_USERNAME, username);
                image.setAvailable(true);
                image._setFilePackage(fp);
                ret.add(image);
            }
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlAllVideosOfAPlaylist(final Account account) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        int page = 1;
        int addedItems = 0;
        int foundItems = 0;
        final Set<String> dupes = new HashSet<String>();
        Browser brc = br.cloneBrowser();
        do {
            logger.info("Crawling page: " + page);
            int numberofActuallyAddedItems = 0;
            int numberOfDupeItems = 0;
            final String publicVideosHTMLSnippet = brc.getRegex("(id=\"videoPlaylist\".*?</section>)").getMatch(0);
            String[] viewKeys = new Regex(publicVideosHTMLSnippet, "(?:_|-)vkey\\s*=\\s*\"([a-z0-9]+)\"").getColumn(0);
            if (viewKeys == null || viewKeys.length == 0) {
                viewKeys = brc.getRegex("(?:_|-)vkey\\s*=\\s*\"([a-z0-9]+)\"").getColumn(0);
            }
            if (viewKeys == null || viewKeys.length == 0) {
                logger.info("no vKeys found!");
            } else {
                foundItems += viewKeys.length;
                for (final String viewKey : viewKeys) {
                    if (dupes.add(viewKey)) {
                        final DownloadLink dl = createDownloadlink(br.getURL("/view_video.php?viewkey=" + viewKey).toString());
                        ret.add(dl);
                        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                            distribute(dl);
                        }
                        numberofActuallyAddedItems++;
                        addedItems++;
                    } else {
                        numberOfDupeItems++;
                    }
                }
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (numberofActuallyAddedItems == 0) {
                logger.info("Stopping because: This page did not contain any NEW content: page=" + page + "|all_found=" + foundItems + "|all_added=" + addedItems + "|page_found:" + (viewKeys != null ? viewKeys.length : -1) + "|page_added=" + numberofActuallyAddedItems + "|page_dupes=" + numberOfDupeItems);
                if (dupes.size() == 0) {
                    return ret;
                } else {
                    break;
                }
            } else {
                logger.info("found NEW content: page=" + page + "|all_found=" + foundItems + "|all_added=" + addedItems + "|page_found:" + (viewKeys != null ? viewKeys.length : -1) + "|page_added=" + numberofActuallyAddedItems + "|page_dupes=" + numberOfDupeItems);
            }
            final String next = br.getRegex("lazyloadUrl\\s*=\\s*\"(/playlist/.*?)\"").getMatch(0);
            if (next == null) {
                logger.info("Stopping because: Reached last page?");
                break;
            } else {
                logger.info("HTML pagination handling - parsing page: " + next);
                brc = br.cloneBrowser();
                PornHubCom.getPage(brc, brc.createGetRequest(next + "&page=" + ++page));
            }
        } while (!this.isAbort());
        return ret;
    }

    private ArrayList<DownloadLink> crawlSingleVideo(final Browser br, final CryptedLink param, final Account account) throws Exception {
        final PornHubCom hosterPlugin = (PornHubCom) this.getNewPluginForHostInstance(this.getHost());
        PornHubCom.getFirstPageWithAccount(hosterPlugin, account, param.getCryptedUrl());
        handleErrorsAndCaptcha(this.br, account);
        if (PornHubCom.hasOfflineRemovedVideoText(br) || PornHubCom.hasOfflineVideoNotice(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean bestonly = cfg.getBooleanProperty(PornHubCom.BEST_ONLY, false);
        final boolean bestselectiononly = cfg.getBooleanProperty(PornHubCom.BEST_SELECTION_ONLY, false);
        final boolean fastlinkcheck = cfg.getBooleanProperty(PornHubCom.FAST_LINKCHECK, false);
        boolean crawlHLS = cfg.getBooleanProperty(PornHubCom.CRAWL_VIDEO_HLS, true);
        boolean crawlMP4 = cfg.getBooleanProperty(PornHubCom.CRAWL_VIDEO_MP4, true);
        final boolean crawlThumbnail = cfg.getBooleanProperty(PornHubCom.CRAWL_THUMBNAIL, false);
        if (!crawlHLS && !crawlMP4) {
            logger.info("User disabled HLS and HTTP versions -> Force-enable both");
            crawlHLS = true;
            crawlMP4 = true;
        }
        final boolean prefer_server_filename = cfg.getBooleanProperty("USE_ORIGINAL_SERVER_FILENAME", false);
        /* Convert embed links to normal links */
        if (param.getCryptedUrl().matches("(?i).+/embed/[a-z0-9]+")) {
            final String viewkey = PornHubCom.getViewkeyFromURL(param.getCryptedUrl());
            final String newLink = br.getRegex("(https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.(?:com|org)/view_video\\.php\\?viewkey=" + Pattern.quote(viewkey) + ")").getMatch(0);
            if (newLink == null) {
                checkVideoErrors(br);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            param.setCryptedUrl(newLink);
            PornHubCom.getPage(br, param.getCryptedUrl());
        } else if (param.getCryptedUrl().matches("(?i).+/embed_player\\.php\\?id=\\d+")) {
            if (br.containsHTML("No htmlCode read") || br.containsHTML("flash/novideo\\.flv")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String newLink = br.getRegex("<link_url>(https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.(?:com|org)/view_video\\.php\\?viewkey=[a-z0-9]+)</link_url>").getMatch(0);
            if (newLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            param.setCryptedUrl(newLink);
            PornHubCom.getPage(br, param.getCryptedUrl());
        }
        final String username = PornHubCom.getUserName(this, br);
        final String viewkey = PornHubCom.getViewkeyFromURL(param.getCryptedUrl());
        // PornHubCom.getPage(br, PornHubCom.createPornhubVideolink(viewkey, aa));
        final String siteTitle = PornHubCom.getSiteTitle(this, br);
        final Map<String, Map<String, String>> qualities = PornHubCom.getVideoLinks(this, br);
        if (qualities == null || qualities.isEmpty()) {
            this.checkVideoErrors(br);
        }
        logger.info("Debug info: foundLinks_all: " + qualities);
        if (!br.getURL().contains(viewkey)) {
            logger.info("Debug info: unknown: " + param.getCryptedUrl());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (qualities == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (qualities.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        boolean hasMP4 = false;
        for (Entry<String, Map<String, String>> entry : qualities.entrySet()) {
            if (entry.getValue().containsKey("mp4")) {
                hasMP4 = true;
                break;
            }
        }
        /* Important auto fallback because pornhub seems to be slowly removing http URLs and only provide HLS. */
        if (!hasMP4) {
            logger.info("Enable HLS because no MP4 found!");
            crawlHLS = true;
        }
        String categoriesCommaSeparated = "";
        final String categoriesSrc = br.getRegex("<div class=\"categoriesWrapper\">(.*?)</div>\\s+</div>").getMatch(0);
        if (categoriesSrc != null) {
            final String[] categories = new Regex(categoriesSrc, ", 'Category'[^\"]+\">([^<>\"]+)</a>").getColumn(0);
            for (int index = 0; index < categories.length; index++) {
                final boolean isLastItem = index == categories.length - 1;
                categoriesCommaSeparated += categories[index];
                if (!isLastItem) {
                    categoriesCommaSeparated += ",";
                }
            }
        }
        String uploadDate = PluginJSonUtils.getJson(br, "uploadDate");
        /* Try to get date only, without time */
        final String better_date = new Regex(uploadDate, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        if (better_date != null) {
            uploadDate = better_date;
        }
        boolean skippedFlag = false;
        for (final Entry<String, Map<String, String>> qualityEntry : qualities.entrySet()) {
            final String quality = qualityEntry.getKey();
            final Map<String, String> formatMap = qualityEntry.getValue();
            for (final Entry<String, String> formatEntry : formatMap.entrySet()) {
                final String format = formatEntry.getKey().toLowerCase(Locale.ENGLISH);
                final String url = formatEntry.getValue();
                if (StringUtils.isEmpty(url)) {
                    continue;
                } else if (!crawlHLS && "hls".equals(format)) {
                    logger.info("Don't grab:" + format + "/" + quality);
                    skippedFlag = true;
                    continue;
                } else if (!crawlMP4 && "mp4".equals(format)) {
                    logger.info("Don't grab:" + format + "/" + quality);
                    skippedFlag = true;
                    continue;
                }
                final boolean grab;
                if (bestonly) {
                    /* Best only = Grab all available qualities here and then pick the best later. */
                    grab = true;
                } else {
                    /* Either only user selected items or best of user selected --> bestselectiononly == true */
                    grab = cfg.getBooleanProperty(quality, true);
                }
                if (grab) {
                    logger.info("Grab:" + format + "/" + quality);
                    final String server_filename = PornHubCom.getFilenameFromURL(url);
                    String html_filename = siteTitle + "_";
                    final DownloadLink dl = getDecryptDownloadlink(viewkey, format, quality);
                    dl.setProperty(PornHubCom.PROPERT_DIRECTLINK, url);
                    dl.setProperty(PornHubCom.PROPERTY_QUALITY, quality);
                    dl.setProperty("mainlink", param.getCryptedUrl());
                    dl.setProperty(PornHubCom.PROPERTY_VIEWKEY, viewkey);
                    dl.setProperty(PornHubCom.PROPERT_FORMAT, format);
                    dl.setLinkID("pornhub://" + viewkey + "_" + format + "_" + quality);
                    if (!StringUtils.isEmpty(username)) {
                        html_filename = html_filename + username + "_";
                    }
                    if (StringUtils.equalsIgnoreCase(format, "hls")) {
                        html_filename += "hls_";
                    }
                    html_filename += quality + "p.mp4";
                    dl.setProperty("decryptedfilename", html_filename);
                    if (prefer_server_filename && server_filename != null) {
                        dl.setFinalFileName(server_filename);
                    } else {
                        dl.setFinalFileName(html_filename);
                    }
                    dl.setContentUrl(param.getCryptedUrl());
                    if (fastlinkcheck) {
                        dl.setAvailable(true);
                    }
                    ret.add(dl);
                } else {
                    skippedFlag = true;
                    logger.info("Don't grab:" + format + "/" + quality);
                }
            }
        }
        if (ret.size() == 0) {
            if (skippedFlag) {
                throw new DecrypterRetryException(RetryReason.PLUGIN_SETTINGS);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (bestonly || bestselectiononly) {
            DownloadLink best = null;
            for (final DownloadLink found : ret) {
                if (best == null) {
                    best = found;
                } else {
                    final String bestQuality = best.getStringProperty(PornHubCom.PROPERTY_QUALITY);
                    final String foundQuality = found.getStringProperty(PornHubCom.PROPERTY_QUALITY);
                    if (Integer.parseInt(foundQuality) > Integer.parseInt(bestQuality)) {
                        best = found;
                    } else {
                        final String foundFormat = found.getStringProperty(PornHubCom.PROPERT_FORMAT);
                        if (Integer.parseInt(foundQuality) == Integer.parseInt(bestQuality) && StringUtils.equalsIgnoreCase(foundFormat, "mp4")) {
                            best = found;
                        }
                    }
                }
            }
            if (best != null) {
                ret.clear();
                ret.add(best);
            }
        }
        if (crawlThumbnail) {
            // higher resolution
            String thumbnailURL = br.getRegex("\"thumbnailUrl\"\\s*:\\s*\"(https?://.*?)\"").getMatch(0);
            if (thumbnailURL == null) {
                // lower resolution
                thumbnailURL = br.getRegex("<img\\s*id\\s*=\\s*\"videoElementPoster\"[^>]*src\\s*=\\s*\"(https?://.*?)\"").getMatch(0);
                if (thumbnailURL == null) {
                    thumbnailURL = br.getRegex("<img\\s*src\\s*=\\s*\"(https?://.*?)\"[^>]*id\\s*=\\s*\"videoElementPoster\"").getMatch(0);
                }
            }
            if (thumbnailURL != null) {
                String html_filename = siteTitle;
                if (!StringUtils.isEmpty(username)) {
                    html_filename += "_" + username;
                }
                html_filename += getFileNameExtensionFromURL(thumbnailURL, ".jpg");
                DownloadLink dl = createDownloadlink("directhttp://" + thumbnailURL);
                dl.setProperty(DirectHTTP.FIXNAME, html_filename);
                dl.setFinalFileName(html_filename);
                dl.setContentUrl(param.getCryptedUrl());
                /*
                 * 2023-02-17: The following line of code contains a typo. This typo now needs to be there forever otherwise it would break
                 * the ability to find duplicates for thumbnails added in order versions :D
                 */
                dl.setLinkID("pornhub://" + viewkey + "_thumnail");
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                ret.add(dl);
            }
        }
        /* Add properties */
        for (final DownloadLink result : ret) {
            /* Set some Packagizer properties */
            if (!StringUtils.isEmpty(username)) {
                result.setProperty(PornHubCom.PROPERTY_USERNAME, username);
            }
            if (!StringUtils.isEmpty(uploadDate)) {
                result.setProperty(PornHubCom.PROPERT_DATE, uploadDate);
            }
            if (!StringUtils.isEmpty(categoriesCommaSeparated)) {
                result.setProperty(PornHubCom.PROPERTY_CATEGORIES_COMMA_SEPARATED, categoriesCommaSeparated);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(siteTitle);
        fp.addLinks(ret);
        return ret;
    }

    private void checkVideoErrors(final Browser br) throws PluginException, DecrypterRetryException {
        if (br.containsHTML(PornHubCom.html_purchase_only)) {
            throw new AccountRequiredException();
        } else if (PornHubCom.isFlagged(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (PornHubCom.isGeoRestricted(br)) {
            throw new DecrypterRetryException(RetryReason.GEO, "(GeoBlocked)url=" + br.getURL());
        } else if (br.containsHTML(PornHubCom.html_privatevideo)) {
            throw new AccountRequiredException();
        } else if (br.containsHTML(PornHubCom.html_premium_only)) {
            throw new AccountRequiredException();
        } else if (PornHubCom.hasOfflineRemovedVideoText(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (PornHubCom.hasOfflineVideoNotice(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isOfflineVideo(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /*
         * 2022-08-16: Generic, can also be GEO-blocked error e.g. <span class="removedVideoText">Dieses Video ist in Ihrem Land nicht
         * verfügbar.</span>
         */
        if (br.containsHTML("class=\"removedVideoText\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static boolean isOfflineVideo(final Browser br) {
        return (!StringUtils.containsIgnoreCase(br.getURL(), "/embed/") && !br.containsHTML("\\'embedSWF\\'")) || PornHubCom.hasOfflineRemovedVideoText(br) || isOfflineGeneral(br);
    }

    public static boolean isOfflineGeneral(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    private DownloadLink getDecryptDownloadlink(final String viewKey, final String format, final String quality) {
        return createDownloadlink("https://pornhubdecrypted/" + viewKey + "/" + format + "/" + quality);
    }

    public int getMaxConcurrentProcessingInstances() {
        return 2;// seems they try to block crawling
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}