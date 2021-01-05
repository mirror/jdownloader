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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornHubCom extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public PornHubCom(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 333);
    }

    final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private String                parameter      = null;
    private static final String   DOMAIN         = "pornhub.com";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pornhub.com", "pornhub.org", "pornhubpremium.com", "pornhubpremium.org" });
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
            /* All videos of a pornstar/model */
            pattern += "(pornstar|model)/[^/]+(/public|/videos/premium|/videos/paid|/videos|/from_videos)?|";
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

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        try {
            parameter = jd.plugins.hoster.PornHubCom.correctAddedURL(param.toString());
        } catch (PluginException e) {
            logger.log(e);
            parameter = param.toString().replaceFirst("https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.(?:com|org)/", "https://www.pornhub.com/");
        }
        br.setFollowRedirects(true);
        jd.plugins.hoster.PornHubCom.prepBr(br);
        Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            try {
                jd.plugins.hoster.PornHubCom.login(this, br, account, false);
            } catch (PluginException e) {
                handleAccountException(account, e);
                account = null;
            }
        }
        jd.plugins.hoster.PornHubCom.getFirstPageWithAccount(this, account, br, parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Offline because 404");
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.containsHTML("<h2>Upgrade now<")) {
            logger.info("Offline because premiumonly");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        if (AbstractRecaptchaV2.containsRecaptchaV2Class(br) && br.containsHTML("/captcha/validate\\?token=")) {
            final Form form = br.getFormByInputFieldKeyValue("captchaType", "1");
            logger.info("Detected captcha method \"reCaptchaV2\" for this host");
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(form);
        }
        if (br.containsHTML(">\\s*Sorry, but this video is private") && br.containsHTML("href\\s*=\\s*\"/login\"") && account != null) {
            logger.info("Debug info: href= /login is found for private video + registered user, re-login now");
            jd.plugins.hoster.PornHubCom.login(this, br, account, true);
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            if (br.containsHTML("href\\s*=\\s*\"/login\"")) {
                logger.info("Debug info: href= /login is found for registered user, re-login failed?");
            }
            if (AbstractRecaptchaV2.containsRecaptchaV2Class(br)) {
                // logger.info("Debug info: captcha handling is required now!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final boolean ret;
        if (parameter.matches("(?i).*/playlist/.*")) {
            logger.info("PlayList");
            ret = decryptAllVideosOfAPlaylist();
        } else if (parameter.matches("(?i).*/gifs.*")) {
            logger.info("Gif");
            ret = decryptAllGifsOfAUser();
        } else if (parameter.matches("(?i).*/(model|pornstar)/.*")) {
            /* Main profile URL --> Assume user wants to have all videos of that profile */
            logger.info("Model/Pornstar");
            ret = decryptAllVideosOfAPornstar(br, account, new HashSet<String>());
        } else if (parameter.matches("(?i).*/(?:users|channels).*")) {
            if (new Regex(br.getURL(), "/(model|pornstar)/").matches()) { // Handle /users/ that has been switched to model|pornstar
                logger.info("Users->Model|pornstar");
                ret = decryptAllVideosOfAPornstar(br, account, new HashSet<String>());
            } else {
                logger.info("Users / Channels");
                ret = decryptAllVideosOfAUser();
            }
        } else {
            logger.info("Video");
            return decryptSingleVideo();
        }
        if (ret == false && decryptedLinks.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return decryptedLinks;
        }
    }

    /** Handles pornhub.com/bla/(model|pornstar)/bla */
    private boolean decryptAllVideosOfAPornstar(Browser br, Account account, Set<String> dupes) throws Exception {
        if (isOfflineGeneral(br)) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        final Set<String> pages = new HashSet<String>();
        int page = 0;
        int maxPage = -1;
        int addedItems = 0;
        int foundItems = 0;
        String ajaxPaginationURL = null;
        final String containerURL = br.getURL();
        do {
            int numberofActuallyAddedItems = 0;
            int numberOfDupeItems = 0;
            page++;
            logger.info(String.format("Crawling page %s| %d / %d", br.getURL(), page, maxPage));
            final Set<String> viewKeys = new HashSet<String>();
            if (account != null) {
                final String paidVideosSection = findVideoSection(br, "paidVideosSection");// /videos/paid
                if (paidVideosSection != null) {
                    // /videos/premium contains paid/available premium videos while
                    // /videos/paid contains all un-paid videos
                    final Browser brc = br.cloneBrowser();
                    jd.plugins.hoster.PornHubCom.getPage(brc, brc.createGetRequest(br.getURL() + "/premium"));
                    decryptAllVideosOfAPornstar(brc, account, dupes);
                }
                final String fanVideosSection = findVideoSection(br, "fanVideosSection");// /videos/fanonly
                if (false && fanVideosSection != null) {
                    final Browser brc = br.cloneBrowser();
                    jd.plugins.hoster.PornHubCom.getPage(brc, brc.createGetRequest(br.getURL() + "/fanonly"));
                    decryptAllVideosOfAPornstar(brc.cloneBrowser(), account, dupes);
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
                    if (isAbort()) {
                        return true;
                    } else if (dupes.add(viewkey)) {
                        final DownloadLink dl = createDownloadlink("https://www." + getHost() + "/view_video.php?viewkey=" + viewkey);
                        dl.setContainerUrl(containerURL);
                        decryptedLinks.add(dl);
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
            final String next = br.getRegex("page_next[^\"]*?\"><a href=\"([^\"]+?)\"").getMatch(0);
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
                return true;
            } else if (next != null && pages.add(next)) {
                logger.info("HTML pagination handling - parsing page: " + next);
                jd.plugins.hoster.PornHubCom.getPage(br, br.createGetRequest(next));
            } else if (ajaxPaginationURL != null && page < maxPage) {
                /* E.g. max page given = 4 --> Stop AFTER counter == 3 as 3+1 == 4 */
                logger.info("Ajax pagination handling");
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                final int postPage = page + 1;
                jd.plugins.hoster.PornHubCom.getPage(br, br.createPostRequest(ajaxPaginationURL + "&page=" + postPage, "o=best&page=" + postPage));
            } else {
                logger.info("no next page! page=" + page + "|max_page=" + maxPage);
                break;
            }
        } while (true);
        return true;
    }

    private String findVideoSection(Browser br, String section) {
        return br.getRegex("(<ul[^>]*(?:class\\s*=\\s*\"videos[^>]*id\\s*=\\s*\"" + section + "\"|[^>]*id\\s*=\\s*\"" + section + "\"[^>]*class\\s*=\\s*\"videos[^>]).*?)(<ul\\s*class\\s*=\\s*\"videos|</ul>)").getMatch(0);
    }

    private boolean decryptAllVideosOfAUser() throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        FilePackage fp = null;
        // TODO: better check for user[channel]/model/pornstar and handle all possible cases
        if (parameter.matches("(?i).*/pornstar/[^/]+/videos/upload")) {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName("Videos uploaded by " + user);
            }
        } else if (parameter.matches("(?i).*/pornstar/[^/]+/videos/?")) {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName(user + " - Upload Videos");
            }
        } else if (parameter.matches("(?i).*/model/[^/]+/videos/?")) {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName(user + " - Upload Videos");
            }
        } else if (parameter.matches("(?i).*/users/[^/]*/?(videos/?)?")) {
            jd.plugins.hoster.PornHubCom.getPage(br, new Regex(parameter, "(.*/users/[^/]*)").getMatch(0) + "/videos/public");
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName(user + "'s Public Videos");
            }
        } else if (parameter.matches("(?i).*/channels/.+")) {
            if (!parameter.endsWith("/videos") && !parameter.endsWith("/")) {
                parameter += "/videos";
            }
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName(user + " Channel Uploads");
            }
        } else {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">There are no videos\\.\\.\\.<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        // final String username = new Regex(parameter, "users/([^/]+)/").getMatch(0);
        int page = 1;
        int max_entries_per_page = 40;
        int links_found_in_this_page;
        final Set<String> dupes = new HashSet<String>();
        String publicVideosHTMLSnippet = null;
        String base_url = null;
        boolean ret = false;
        do {
            boolean htmlSource = true;
            if (page > 1) {
                // jd.plugins.hoster.PornHubCom.getPage(br, "/users/" + username + "/videos/public/ajax?o=mr&page=" + page);
                // br.postPage(parameter + "/ajax?o=mr&page=" + page, "");
                /* e.g. different handling for '/model/' URLs */
                String nextpage_url = br.getRegex("class=\"page_next\"><a href=\"(/[^\"]+\\?page=\\d+)\"").getMatch(0);
                if (nextpage_url == null) {
                    nextpage_url = base_url + "/ajax?o=mr&page=" + page;
                    br.getHeaders().put("Accept", "*/*");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    htmlSource = false;
                }
                jd.plugins.hoster.PornHubCom.getPage(br, br.createPostRequest(nextpage_url, ""));
                if (br.getHttpConnection().getResponseCode() == 404) {
                    break;
                }
            } else {
                /* Set this on first loop */
                base_url = br.getURL();
            }
            if (htmlSource) {
                /* only parse videos of the user/pornstar/channel, avoid catching unrelated content e.g. 'related' videos */
                if (parameter.contains("/pornstar/") || parameter.contains("/model/")) {
                    publicVideosHTMLSnippet = br.getRegex("(class=\"videoUList[^\"]*?\".*?</section>)").getMatch(0);
                } else if (parameter.contains("/channels/")) {
                    publicVideosHTMLSnippet = br.getRegex("<ul[^>]*?id=\"showAllChanelVideos\">.*?</ul>").getMatch(-1);
                    max_entries_per_page = 36;
                } else {
                    // publicVideosHTMLSnippet = br.getRegex("(>public Videos<.+?(>Load More<|</section>))").getMatch(0);
                    publicVideosHTMLSnippet = br.getRegex("(class=\"videoUList[^\"]*?\".*?</section>)").getMatch(0);
                }
            } else {
                /* Pagination result --> Ideal as a source as it only contains the content we need */
                publicVideosHTMLSnippet = br.toString();
            }
            if (publicVideosHTMLSnippet == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            logger.info("publicVideos: " + publicVideosHTMLSnippet); // For debugging
            final String[] viewkeys = new Regex(publicVideosHTMLSnippet, "(?:_|-)vkey\\s*=\\s*\"([a-z0-9]+)\"").getColumn(0);
            if (viewkeys == null || viewkeys.length == 0) {
                break;
            }
            for (final String viewkey : viewkeys) {
                if (dupes.add(viewkey)) {
                    // logger.info("http://www." + this.getHost() + "/view_video.php?viewkey=" + viewkey); // For debugging
                    final DownloadLink dl = createDownloadlink("https://www." + this.getHost() + "/view_video.php?viewkey=" + viewkey);
                    if (fp != null) {
                        fp.add(dl);
                    }
                    ret = true;
                    decryptedLinks.add(dl);
                    if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        distribute(dl);
                    }
                }
            }
            logger.info("Links found in page " + page + ": " + viewkeys.length);
            links_found_in_this_page = viewkeys.length;
            page++;
        } while (!this.isAbort() && links_found_in_this_page >= max_entries_per_page);
        return ret;
    }

    private String getUser(final Browser br) {
        final String ret = new Regex(br.getURL(), "/(?:users|model|pornstar|channels)/([^/]+)").getMatch(0);
        return ret;
    }

    private boolean decryptAllGifsOfAUser() throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        FilePackage fp = null;
        if (StringUtils.endsWithCaseInsensitive(parameter, "/gifs/public")) {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            // user->pornstar redirect
            if (!br.getURL().matches("(?i).+/gifs/public")) {
                jd.plugins.hoster.PornHubCom.getPage(br, br.getURL() + "/gifs/public");
            }
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName(user + "'s GIFs");
            }
        } else if (StringUtils.endsWithCaseInsensitive(parameter, "/gifs/video")) {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            // user->pornstar redirect
            if (!br.getURL().matches("(?i).+/gifs/video")) {
                jd.plugins.hoster.PornHubCom.getPage(br, br.getURL() + "/gifs/video");
            }
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName("GIFs From " + user + "'s Videos");
            }
        } else if (StringUtils.endsWithCaseInsensitive(parameter, "/gifs/from_videos")) {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            // user->pornstar redirect
            if (!br.getURL().matches("(?i).+/gifs/from_videos") || !br.getURL().matches("(?i).+/gifs/video")) {
                jd.plugins.hoster.PornHubCom.getPage(br, br.getURL() + "/gifs/video");
            }
            final String user = getUser(br);
            if (user != null) {
                fp = FilePackage.getInstance();
                fp.setName("GIFs From " + user + "'s Videos");
            }
        } else {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            decryptedLinks.add(createDownloadlink(br.getURL() + "/public"));
            decryptedLinks.add(createDownloadlink(br.getURL() + "/video"));
            return true;
        }
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        int page = 1;
        final int max_entries_per_page = 50;
        int links_found_in_this_page;
        final Set<String> dupes = new HashSet<String>();
        String base_url = null;
        boolean ret = false;
        do {
            if (this.isAbort()) {
                return true;
            }
            if (page > 1) {
                final String nextpage_url = base_url + "/ajax?page=" + page;
                jd.plugins.hoster.PornHubCom.getPage(br, br.createPostRequest(nextpage_url, ""));
                if (br.getHttpConnection().getResponseCode() == 404) {
                    break;
                }
            } else {
                /* Set this on first loop */
                base_url = br.getURL();
            }
            final String[] items = new Regex(br.toString(), "(<li\\s*id\\s*=\\s*\"gif\\d+\"\\s*>.*?</li>)").getColumn(0);
            if (items == null || items.length == 0) {
                break;
            }
            for (final String item : items) {
                final String viewKey = new Regex(item, "/gif/(\\d+)").getMatch(0);
                if (viewKey != null && dupes.add(viewKey)) {
                    final String name = new Regex(item, "class\\s*=\\s*\"title\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
                    final DownloadLink dl = createDownloadlink("https://www." + this.getHost() + "/gif/" + viewKey);
                    if (name != null) {
                        dl.setName(name + "_" + viewKey + ".webm");
                    } else {
                        dl.setName(viewKey + ".webm");
                    }
                    /* Force fast linkcheck */
                    dl.setAvailable(true);
                    ret = true;
                    if (fp != null) {
                        fp.add(dl);
                    }
                    decryptedLinks.add(dl);
                    if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        distribute(dl);
                    }
                }
            }
            logger.info("Links found in page " + page + ": " + items.length);
            links_found_in_this_page = items.length;
            page++;
        } while (links_found_in_this_page >= max_entries_per_page);
        return ret;
    }

    private boolean decryptAllVideosOfAPlaylist() throws Exception {
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
                    if (isAbort()) {
                        return true;
                    } else if (dupes.add(viewKey)) {
                        final DownloadLink dl = createDownloadlink("https://www." + this.getHost() + "/view_video.php?viewkey=" + viewKey);
                        decryptedLinks.add(dl);
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
                logger.info("Stopping because this page did not contain any NEW content: page=" + page + "|all_found=" + foundItems + "|all_added=" + addedItems + "|page_found:" + (viewKeys != null ? viewKeys.length : -1) + "|page_added=" + numberofActuallyAddedItems + "|page_dupes=" + numberOfDupeItems);
                if (dupes.size() == 0) {
                    return false;
                } else {
                    break;
                }
            } else {
                logger.info("found NEW content: page=" + page + "|all_found=" + foundItems + "|all_added=" + addedItems + "|page_found:" + (viewKeys != null ? viewKeys.length : -1) + "|page_added=" + numberofActuallyAddedItems + "|page_dupes=" + numberOfDupeItems);
            }
            final String next = br.getRegex("lazyloadUrl\\s*=\\s*\"(/playlist/.*?)\"").getMatch(0);
            if (isAbort()) {
                return true;
            } else if (next != null) {
                logger.info("HTML pagination handling - parsing page: " + next);
                brc = br.cloneBrowser();
                jd.plugins.hoster.PornHubCom.getPage(brc, brc.createGetRequest(next + "&page=" + ++page));
            } else {
                logger.info("no next page! page=" + page);
                break;
            }
        } while (!this.isAbort());
        return true;
    }

    private ArrayList<DownloadLink> decryptSingleVideo() throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean bestonly = cfg.getBooleanProperty(jd.plugins.hoster.PornHubCom.BEST_ONLY, false);
        final boolean bestselectiononly = cfg.getBooleanProperty(jd.plugins.hoster.PornHubCom.BEST_SELECTION_ONLY, false);
        final boolean fastlinkcheck = cfg.getBooleanProperty(jd.plugins.hoster.PornHubCom.FAST_LINKCHECK, false);
        final boolean prefer_server_filename = cfg.getBooleanProperty("USE_ORIGINAL_SERVER_FILENAME", false);
        /* Convert embed links to normal links */
        if (parameter.matches(".+/embed_player\\.php\\?id=\\d+")) {
            if (br.containsHTML("No htmlCode read") || br.containsHTML("flash/novideo\\.flv")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String newLink = br.getRegex("<link_url>(https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.(?:com|org)/view_video\\.php\\?viewkey=[a-z0-9]+)</link_url>").getMatch(0);
            if (newLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            parameter = newLink;
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
        }
        final String username = jd.plugins.hoster.PornHubCom.getUserName(this, br);
        final String viewkey = jd.plugins.hoster.PornHubCom.getViewkeyFromURL(parameter);
        if (!br.getURL().contains(viewkey)) {
            /* Offline - initial URL redirected to some other URL (unsupported/offline) */
            final DownloadLink dl = createOfflinelink(parameter);
            dl.setFinalFileName("viewkey=" + viewkey);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        // jd.plugins.hoster.PornHubCom.getPage(br, jd.plugins.hoster.PornHubCom.createPornhubVideolink(viewkey, aa));
        final String siteTitle = jd.plugins.hoster.PornHubCom.getSiteTitle(this, br);
        final Map<String, Map<String, String>> qualities = jd.plugins.hoster.PornHubCom.getVideoLinks(this, br);
        logger.info("Debug info: foundLinks_all: " + qualities);
        if ((qualities == null || qualities.isEmpty()) && br.containsHTML(jd.plugins.hoster.PornHubCom.html_purchase_only)) {
            logger.info("Debug info: This video has to be purchased separately: " + parameter);
            throw new AccountRequiredException();
        } else if (isFlagged(br)) {
            logger.info("Debug info: flagged: " + parameter);
            final DownloadLink dl = createOfflinelink(parameter);
            dl.setFinalFileName("(Flagged)viewkey=" + viewkey);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (isGeoRestricted(br)) {
            logger.info("Debug info: geo_blocked: " + parameter);
            final DownloadLink dl = createOfflinelink(parameter);
            dl.setFinalFileName("(GeoBlocked)viewkey=" + viewkey);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (isOfflineVideo(br)) {
            logger.info("Debug info: offline: " + parameter);
            final DownloadLink dl = createOfflinelink(parameter);
            dl.setFinalFileName("viewkey=" + viewkey);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else if (br.containsHTML(jd.plugins.hoster.PornHubCom.html_privatevideo)) {
            logger.info("Debug info: html_privatevideo: " + parameter);
            throw new AccountRequiredException();
        } else if (br.containsHTML(jd.plugins.hoster.PornHubCom.html_premium_only)) {
            logger.info("Debug info: html_premium_only: " + parameter);
            throw new AccountRequiredException();
        }
        String uploadDate = PluginJSonUtils.getJson(br, "uploadDate");
        /* Try to get date only, without time */
        final String better_date = new Regex(uploadDate, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        if (better_date != null) {
            uploadDate = better_date;
        }
        if (qualities != null) {
            if (qualities.isEmpty()) {
                final DownloadLink dl = createOfflinelink(parameter);
                dl.setFinalFileName("viewkey=" + viewkey);
                decryptedLinks.add(dl);
                return decryptedLinks;
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
                        final String server_filename = jd.plugins.hoster.PornHubCom.getFilenameFromURL(url);
                        String html_filename = siteTitle + "_";
                        final DownloadLink dl = getDecryptDownloadlink(viewkey, format, quality);
                        dl.setProperty("directlink", url);
                        dl.setProperty("quality", quality);
                        dl.setProperty("mainlink", parameter);
                        dl.setProperty("viewkey", viewkey);
                        dl.setProperty("format", format);
                        dl.setLinkID("pornhub://" + viewkey + "_" + format + "_" + quality);
                        if (!StringUtils.isEmpty(username)) {
                            html_filename += username + "_";
                            /* This property is only for the user (packagizer) and not required anywhere in our host plugin! */
                            dl.setProperty("username", username);
                        }
                        if (StringUtils.equalsIgnoreCase(format, "hls")) {
                            html_filename += "hls_" + quality + "p.mp4";
                        } else {
                            html_filename += quality + "p.mp4";
                        }
                        if (prefer_server_filename && server_filename != null) {
                            dl.setFinalFileName(server_filename);
                        } else {
                            dl.setFinalFileName(html_filename);
                        }
                        dl.setProperty("decryptedfilename", html_filename);
                        if (!StringUtils.isEmpty(uploadDate)) {
                            dl.setProperty("date", uploadDate);
                        }
                        dl.setContentUrl(parameter);
                        if (fastlinkcheck) {
                            dl.setAvailable(true);
                        }
                        decryptedLinks.add(dl);
                    } else {
                        skippedFlag = true;
                        logger.info("Don't grab:" + format + "/" + quality);
                    }
                }
            }
            if (decryptedLinks.size() == 0) {
                if (skippedFlag) {
                    throw new DecrypterRetryException(RetryReason.PLUGIN_SETTINGS);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (bestonly || bestselectiononly) {
                DownloadLink best = null;
                for (final DownloadLink found : decryptedLinks) {
                    if (best == null) {
                        best = found;
                    } else {
                        final String bestQuality = best.getStringProperty("quality");
                        final String foundQuality = found.getStringProperty("quality");
                        if (Integer.parseInt(foundQuality) > Integer.parseInt(bestQuality)) {
                            best = found;
                        } else {
                            final String bestFormat = best.getStringProperty("format");
                            final String foundFormat = found.getStringProperty("format");
                            if (Integer.parseInt(foundQuality) == Integer.parseInt(bestQuality) && StringUtils.equalsIgnoreCase(foundFormat, "mp4")) {
                                best = found;
                            }
                        }
                    }
                }
                if (best != null) {
                    decryptedLinks.clear();
                    decryptedLinks.add(best);
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(siteTitle);
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    public static boolean isGeoRestricted(final Browser br) {
        return br.containsHTML(">\\s*This content is unavailable in your country.?\\s*<");
    }

    public static boolean isFlagged(final Browser br) {
        return br.containsHTML(">\\s*Video has been flagged for verification in accordance with our trust and safety policy.?\\s*<");
    }

    public static boolean isOfflineVideo(final Browser br) {
        return !br.containsHTML("\\'embedSWF\\'") || br.containsHTML("<span>\\s*Video has been removed at the request of") || isOfflineGeneral(br);
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

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}