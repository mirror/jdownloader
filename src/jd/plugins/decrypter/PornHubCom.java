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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pornhub.com" }, urls = { "https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.com/(?:.*\\?viewkey=[a-z0-9]+|embed/[a-z0-9]+|embed_player\\.php\\?id=\\d+|pornstar/[^/]+(?:/gifs(/public|/video|/from_videos)?|/videos(/upload)?)?|users/[^/]+(?:/gifs(/public|/video|/from_videos)?|/videos(/public)?)?|model/[^/]+(?:/gifs(/public|/video|/from_videos)?|/videos)?|playlist/\\d+)" })
public class PornHubCom extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public PornHubCom(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal(getHost(), 333);
    }

    final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    private String                parameter      = null;
    private static final String   DOMAIN         = "pornhub.com";

    @SuppressWarnings({ "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister-host plugin */
        try {
            parameter = jd.plugins.hoster.PornHubCom.correctAddedURL(param.toString());
        } catch (PluginException e) {
            parameter = param.toString().replaceFirst("https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.com/", "https://www.pornhub.com/");
        }
        br.setFollowRedirects(true);
        jd.plugins.hoster.PornHubCom.prepBr(br);
        Boolean premium = false;
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            try {
                jd.plugins.hoster.PornHubCom.login(this, br, account, false);
                if (AccountType.PREMIUM.equals(account.getType())) {
                    premium = true;
                }
            } catch (PluginException e) {
                handleAccountException(account, e);
            }
        }
        if (premium) {
            parameter = parameter.replace("pornhub.com", "pornhubpremium.com");
        } else {
            parameter = parameter.replace("pornhubpremium.com", "pornhub.com");
        }
        jd.plugins.hoster.PornHubCom.getPage(br, parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<h2>Upgrade now<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("class=\"g-recaptcha\"") && br.containsHTML("/captcha/validate\\?token=")) {
            final Form form = br.getFormByInputFieldKeyValue("captchaType", "1");
            logger.info("Detected captcha method \"reCaptchaV2\" for this host");
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(form);
        }
        if (br.containsHTML(">Sorry, but this video is private") && br.containsHTML("href=\"/login\"") && account != null) {
            logger.info("Debug info: href= /login is found for private video + registered user, re-login now");
            jd.plugins.hoster.PornHubCom.login(this, br, account, true);
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
            if (br.containsHTML("href=\"/login\"")) {
                logger.info("Debug info: href= /login is found for registered user, re-login failed?");
            }
            if (br.containsHTML("class=\"g-recaptcha\"")) {
                // logger.info("Debug info: captcha handling is required now!");
                throw new DecrypterException("Decrypter broken, captcha handling is required now!");
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
            logger.info("Model/Pornstar");
            ret = decryptAllVideosOfAPornstar();
        } else if (parameter.matches("(?i).*/users/.*")) {
            if (new Regex(br.getURL(), "/(model|pornstar)/").matches()) { // Handle /users/ that has been switched to model|pornstar
                logger.info("Users->Model|pornstar");
                ret = decryptAllVideosOfAPornstar();
            } else {
                logger.info("Users");
                ret = decryptAllVideosOfAUser();
            }
        } else {
            logger.info("Video");
            ret = decryptSingleVideo();
        }
        if (ret == false && decryptedLinks.isEmpty()) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        return decryptedLinks;
    }

    private boolean decryptAllVideosOfAPornstar() throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        final Set<String> dupes = new HashSet<String>();
        final Set<String> pages = new HashSet<String>();
        do {
            if (this.isAbort()) {
                return true;
            }
            final String[] viewkeys = br.getRegex("<a href=\"(?:https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?.com)?/view_video.php\\?viewkey=([^\"]+?)\"").getColumn(0);
            logger.info("Links found: " + viewkeys.length);
            for (final String viewkey : viewkeys) {
                logger.info("viewkey: " + viewkey);
                if (dupes.add(viewkey)) {
                    final DownloadLink dl = createDownloadlink("https://www." + getHost() + "/view_video.php?viewkey=" + viewkey);
                    decryptedLinks.add(dl);
                    distribute(dl);
                }
            }
            final String next = br.getRegex("page_next[^\"]*?\"><a href=\"([^\"]+?)\"").getMatch(0);
            if (next != null && pages.add(next)) {
                br.getPage(next);
            } else {
                break;
            }
        } while (true);
        return true;
    }

    private boolean decryptAllVideosOfAUser() throws Exception {
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        FilePackage fp = null;
        // TODO: better check for user/model/pornstar and handle all possible cases
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
        } else {
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
        }
        if (br.containsHTML(">There are no videos...<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        // final String username = new Regex(parameter, "users/([^/]+)/").getMatch(0);
        int page = 1;
        final int max_entries_per_page = 40;
        int links_found_in_this_page;
        final Set<String> dupes = new HashSet<String>();
        String publicVideosHTMLSnippet = null;
        String base_url = null;
        boolean ret = false;
        do {
            if (this.isAbort()) {
                return true;
            }
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
                jd.plugins.hoster.PornHubCom.getPage(br, nextpage_url);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    break;
                }
            } else {
                /* Set this on first loop */
                base_url = br.getURL();
            }
            if (htmlSource) {
                /* only parse videos of the user, avoid catching videos from 'outside' html */
                if (parameter.contains("/pornstar/") || parameter.contains("/model/")) {
                    publicVideosHTMLSnippet = br.getRegex("(class=\"videoUList[^\"]*?\".*?</section>)").getMatch(0);
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
            final String[] viewkeys = new Regex(publicVideosHTMLSnippet, "_vkey=\"([a-z0-9]+)\"").getColumn(0);
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
                    distribute(dl);
                }
            }
            logger.info("Links found in page " + page + ": " + viewkeys.length);
            links_found_in_this_page = viewkeys.length;
            page++;
        } while (links_found_in_this_page >= max_entries_per_page);
        return ret;
    }

    private String getUser(Browser br) {
        final String ret = new Regex(br.getURL(), "/(?:users|model|pornstar)/([^/]+)").getMatch(0);
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
                    distribute(dl);
                }
            }
            logger.info("Links found in page " + page + ": " + items.length);
            links_found_in_this_page = items.length;
            page++;
        } while (links_found_in_this_page >= max_entries_per_page);
        return ret;
    }

    private boolean decryptAllVideosOfAPlaylist() {
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return true;
        }
        final Set<String> dupes = new HashSet<String>();
        final String publicVideosHTMLSnippet = br.getRegex("(id=\"videoPlaylist\".*?</section>)").getMatch(0);
        final String[] viewkeys = new Regex(publicVideosHTMLSnippet, "_vkey=\"([a-z0-9]+)\"").getColumn(0);
        if (viewkeys == null || viewkeys.length == 0) {
            return false;
        }
        for (final String viewkey : viewkeys) {
            if (dupes.add(viewkey)) {
                // logger.info("http://www." + this.getHost() + "/view_video.php?viewkey=" + viewkey); // For debugging
                final DownloadLink dl = createDownloadlink("https://www." + this.getHost() + "/view_video.php?viewkey=" + viewkey);
                decryptedLinks.add(dl);
                distribute(dl);
            }
        }
        return true;
    }

    private boolean decryptSingleVideo() throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(DOMAIN);
        final boolean bestonly = cfg.getBooleanProperty(jd.plugins.hoster.PornHubCom.BEST_ONLY, false);
        final boolean bestselectiononly = cfg.getBooleanProperty(jd.plugins.hoster.PornHubCom.BEST_SELECTION_ONLY, false);
        final boolean fastlinkcheck = cfg.getBooleanProperty(jd.plugins.hoster.PornHubCom.FAST_LINKCHECK, false);
        /* Convert embed links to normal links */
        if (parameter.matches(".+/embed_player\\.php\\?id=\\d+")) {
            if (br.containsHTML("No htmlCode read") || br.containsHTML("flash/novideo\\.flv")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return true;
            }
            final String newLink = br.getRegex("<link_url>(https?://(?:www\\.|[a-z]{2}\\.)?pornhub(?:premium)?\\.com/view_video\\.php\\?viewkey=[a-z0-9]+)</link_url>").getMatch(0);
            if (newLink == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            parameter = newLink;
            jd.plugins.hoster.PornHubCom.getPage(br, parameter);
        }
        final String username = br.getRegex("class=\"bolded\">([^<>]+)</a>").getMatch(0);
        final String viewkey = jd.plugins.hoster.PornHubCom.getViewkeyFromURL(parameter);
        // jd.plugins.hoster.PornHubCom.getPage(br, jd.plugins.hoster.PornHubCom.createPornhubVideolink(viewkey, aa));
        final String fpName = jd.plugins.hoster.PornHubCom.getSiteTitle(this, br);
        if (isOffline(br)) {
            final DownloadLink dl = createOfflinelink(parameter);
            dl.setFinalFileName("viewkey=" + viewkey);
            decryptedLinks.add(dl);
            logger.info("Debug info: isOffline: " + parameter);
            return true;
        }
        if (br.containsHTML(jd.plugins.hoster.PornHubCom.html_privatevideo)) {
            final DownloadLink dl = createOfflinelink(parameter);
            dl.setFinalFileName("This_video_is_private_" + fpName + ".mp4");
            decryptedLinks.add(dl);
            logger.info("Debug info: html_privatevideo: " + parameter);
            return true;
        }
        if (br.containsHTML(jd.plugins.hoster.PornHubCom.html_premium_only)) {
            decryptedLinks.add(createOfflinelink(parameter, fpName + ".mp4", "Private_video_Premium_required"));
            logger.info("Debug info: html_premium_only: " + parameter);
            return true;
        }
        final Map<String, String> foundLinks_all = jd.plugins.hoster.PornHubCom.getVideoLinksFree(this, br);
        logger.info("Debug info: foundLinks_all: " + foundLinks_all);
        boolean ret = false;
        if (foundLinks_all != null) {
            final Iterator<Entry<String, String>> it = foundLinks_all.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, String> next = it.next();
                final String qualityInfo = next.getKey();
                final String finallink = next.getValue();
                if (StringUtils.isEmpty(finallink)) {
                    continue;
                }
                final boolean grab;
                if (bestonly) {
                    grab = !bestselectiononly || cfg.getBooleanProperty(qualityInfo, true);
                } else {
                    grab = cfg.getBooleanProperty(qualityInfo, true);
                }
                if (grab) {
                    ret = true;
                    logger.info("Grab:" + qualityInfo);
                    String final_filename = fpName + "_";
                    if (!StringUtils.isEmpty(username)) {
                        final_filename += username + "_";
                    }
                    final_filename += qualityInfo + "p.mp4";
                    final DownloadLink dl = getDecryptDownloadlink(viewkey, qualityInfo);
                    dl.setProperty("directlink", finallink);
                    dl.setProperty("quality", qualityInfo);
                    dl.setProperty("decryptedfilename", final_filename);
                    dl.setProperty("mainlink", parameter);
                    dl.setProperty("viewkey", viewkey);
                    dl.setLinkID(getHost() + "://" + viewkey + qualityInfo);
                    dl.setFinalFileName(final_filename);
                    dl.setContentUrl(parameter);
                    if (fastlinkcheck) {
                        dl.setAvailable(true);
                    }
                    decryptedLinks.add(dl);
                    if (bestonly) {
                        /* Our LinkedHashMap is already in the right order so best = first entry --> Step out of the loop */
                        logger.info("User wants best-only");
                        break;
                    }
                } else {
                    logger.info("Don't grab:" + qualityInfo);
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return ret;
    }

    public static boolean isOffline(final Browser br) {
        return br.getURL().equals("http://www.pornhub.com/") || !br.containsHTML("\\'embedSWF\\'") || br.getHttpConnection().getResponseCode() == 404;
    }

    private DownloadLink getDecryptDownloadlink(final String viewKey, final String quality) {
        return createDownloadlink("https://pornhubdecrypted/" + viewKey + "/" + quality);
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