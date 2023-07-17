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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.XvideosCom;
import jd.plugins.hoster.XvideosCore;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XvideosComProfile extends PluginForDecrypt {
    public XvideosComProfile(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xvideos.com", "xvideos.es", "xvideos2.com", "xvideos2.es", "xvideos3.com", "xvideos3.es", "xvideos4.com", "xvideos5.com", "xvideos.red" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(getHost(), 100);
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/");
            sb.append("(");
            sb.append("pornstar-channels/[A-Za-z0-9\\-_]+#_tabRed");
            sb.append("|(?:profiles|(?:pornstar-|amateur-|model-)?(?:channels|models|pornstars))/[A-Za-z0-9\\-_]+.*");
            sb.append("|favorite/\\d+/[a-z0-9\\_]+");
            sb.append("|account/favorites/\\d+");
            sb.append(")");
            ret.add(sb.toString());
        }
        return ret.toArray(new String[0]);
    }

    private static final String TYPE_FAVOURITES           = "(?i)https?://[^/]+/favorite/(\\d+)/([a-z0-9\\-_]+).*";
    private static final String TYPE_FAVOURITES_ACCOUNT   = "(?i)https?://[^/]+/account/favorites/(\\d+)";
    private static final String TYPE_USER                 = "(?i)https?://[^/]+/(?:profiles|(?:pornstar-|amateur-|model-)?(?:channels|models|pornstars))/([A-Za-z0-9\\-_]+).*";
    private static final String TYPE_USER_PREMIUM         = "(?i)https?://[^/]+/pornstar-channels/([A-Za-z0-9\\-_]+)#_tabRed$";
    private static final String TYPE_SINGLE_VIDEO_QUICKIE = "(?i).+#quickies/./(\\d+)";

    private boolean requiresAccount(final CryptedLink param) {
        return requiresPremiumAccount(param) || param.getCryptedUrl().matches(TYPE_FAVOURITES_ACCOUNT);
    }

    private boolean requiresPremiumAccount(final CryptedLink param) {
        return Browser.getHost(param.getCryptedUrl()).equals("xvideos.red");
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final String singleVideoQuickieVideoID = new Regex(param.getCryptedUrl(), TYPE_SINGLE_VIDEO_QUICKIE).getMatch(0);
        if (singleVideoQuickieVideoID != null) {
            /* Special case: Modify URL and return so it can be handled by hosterplugin. */
            final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
            ret.add(this.createDownloadlink(this.createSingleVideoURL(singleVideoQuickieVideoID, null)));
            return ret;
        }
        /*
         * 2020-10-12: In general, we use the user-added domain but some are dead but the content might still be alive --> Use main plugin
         * domain for such cases
         */
        final PluginForHost plg = this.getNewPluginForHostInstance(this.getHost());
        for (final String deadDomain : ((jd.plugins.hoster.XvideosCom) plg).getDeadDomains()) {
            if (param.getCryptedUrl().contains(deadDomain)) {
                param.setCryptedUrl(param.getCryptedUrl().replaceFirst(org.appwork.utils.Regex.escape(deadDomain) + "/", this.getHost() + "/"));
                break;
            }
        }
        final boolean accountRequired = this.requiresAccount(param);
        final boolean premiumAccountRequired = this.requiresPremiumAccount(param);
        br.addAllowedResponseCodes(new int[] { 400 });
        br.setFollowRedirects(true);
        final Account account = AccountController.getInstance().getValidAccount(getHost());
        if (account != null) {
            ((jd.plugins.hoster.XvideosCom) plg).login(account, false);
        }
        if (accountRequired && account == null) {
            /* Account required! */
            throw new AccountRequiredException();
        } else if (premiumAccountRequired && account != null && account.getType() != AccountType.PREMIUM) {
            logger.info("Account available but free account and premium is required to crawl current URL");
            throw new AccountRequiredException();
        }
        XvideosCom.disableAutoTranslation(this, Browser.getHost(param.getCryptedUrl()), br);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 403) {
            /* E.g. no permission to access private favorites list of another user. */
            throw new AccountRequiredException();
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* E.g. xvideos.com can redirect to xvideos.red when account is active. */
        final boolean premiumAccountActive = this.br.getHost().equals("xvideos.red");
        if (param.getCryptedUrl().matches(TYPE_FAVOURITES)) {
            return this.crawlFavourites(param.getCryptedUrl());
        } else if (param.getCryptedUrl().matches(TYPE_FAVOURITES_ACCOUNT)) {
            if (account == null) {
                throw new AccountRequiredException();
            }
            return crawlFavouritesAccount(param);
        } else if (param.getCryptedUrl().matches(".+/photos/.+")) {
            return crawlPhotos(param.getCryptedUrl());
        } else if ((param.getCryptedUrl().matches(TYPE_USER) && premiumAccountActive) || param.getCryptedUrl().matches(TYPE_USER_PREMIUM)) {
            return crawlChannelPremium(param);
        } else {
            return crawlChannel(param.getCryptedUrl());
        }
    }

    private ArrayList<DownloadLink> crawlFavourites(final String parameter) throws IOException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String fpname = new Regex(parameter, TYPE_FAVOURITES).getMatch(1);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpname);
        String nextpage = null;
        final Set<String> dupeList = new HashSet<String>();
        do {
            final String[] urls = br.getRegex("\"(/video\\d+/[^<>\"]+)").getColumn(0);
            int foundOnPage = 0;
            for (String url : urls) {
                final String videoID = new Regex(url, "/video(.*?)/").getMatch(0);
                if (dupeList.add(videoID)) {
                    foundOnPage++;
                    url = br.getURL(url).toString();
                    final String urlTitle = new Regex(url, "/video\\d+/([^/\\?]+)").getMatch(0);
                    final DownloadLink dl = this.createDownloadlink(url);
                    /* Save http requests */
                    dl.setAvailable(true);
                    dl.setName(cleanUrlTitle(urlTitle) + ".mp4");
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                } else {
                    // logger.info("Found dupe: " + videoID);
                }
            }
            if (foundOnPage < 27) {
                // normally we have 27 videos per page
                logger.info("Found less items than expected on page:" + foundOnPage + " | " + br.getURL());
            }
            nextpage = br.getRegex("href=\"(/favorite/\\d+/[^/]+/\\d+)\"[^>]*class=\"no-page next-page\"").getMatch(0);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (nextpage != null) {
                logger.info("Working on page: " + nextpage);
                br.getPage(nextpage);
            } else {
                logger.info("Stopping because: Failed to find nextpage");
                break;
            }
        } while (true);
        return ret;
    }

    @Override
    public void distribute(DownloadLink... links) {
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            super.distribute(links);
        }
    }

    /** Crawl favorites of account -> Account required */
    private ArrayList<DownloadLink> crawlFavouritesAccount(final CryptedLink param) throws IOException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String listID = new Regex(param.getCryptedUrl(), TYPE_FAVOURITES_ACCOUNT).getMatch(0);
        String fpname = br.getRegex("<span id=\"favListName\">([^<>\"]+)</span>").getMatch(0);
        if (fpname == null) {
            fpname = br.getRegex("\\{\"id\":" + listID + "[^\\}]+\"name\":\"([^\"]+)\"").getMatch(0);
        }
        if (fpname == null) {
            /* Fallback */
            fpname = listID;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpname);
        String nextpage = null;
        final Set<String> dupeList = new HashSet<String>();
        do {
            final String[] urls = br.getRegex("(/video\\d+/[^<>\"\\']+)").getColumn(0);
            for (String url : urls) {
                final String videoID = new Regex(url, "/video(.*?)/").getMatch(0);
                if (dupeList.add(videoID)) {
                    url = br.getURL(url).toString();
                    final String urlTitle = new Regex(url, "/video\\d+/([^/\\?]+)").getMatch(0);
                    final DownloadLink dl = this.createDownloadlink(url);
                    /* Save http requests by pre-setting online status */
                    dl.setAvailable(true);
                    dl.setName(cleanUrlTitle(urlTitle) + ".mp4");
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                } else {
                    // logger.info("Found dupe: " + videoID);
                }
            }
            /* TODO: Check/add pagination */
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            }
            nextpage = null;
            if (nextpage != null) {
                logger.info("Working on page: " + nextpage);
                br.getPage(nextpage);
            } else {
                break;
            }
        } while (true);
        return ret;
    }

    private ArrayList<DownloadLink> crawlChannel(final String url) throws IOException, PluginException {
        final String username = new Regex(url, TYPE_USER).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (!br.getURL().contains(username)) {
            /* E.g. redirect to mainpage */
            logger.info("Profile does not exist anymore");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Set<String> dupeList = new HashSet<String>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        final String userID = br.getRegex("profile-report-form-(\\d+)_").getMatch(0);
        /* Crawl quickies if user has some -> Short 30 seconds clips */
        crawlQuickies: if (br.containsHTML("\"has_quickies\":true")) {
            logger.info("Crawling quickies...");
            if (userID == null) {
                logger.warning("Quickies crawler failed because: Failed to find userID");
                break crawlQuickies;
            }
            final Browser brc = br.cloneBrowser();
            brc.postPage("/quickies-api/profilevideos/all/none/B/" + userID + "/0", "");
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final List<Map<String, Object>> videos = (List<Map<String, Object>>) entries.get("videos");
            for (final Map<String, Object> video : videos) {
                final String singleLink = video.get("url").toString();
                final String videoID = video.get("id").toString();
                final String title = (String) video.get("title");
                if (dupeList.add(videoID)) {
                    final String nameTemp;
                    final DownloadLink dl = createDownloadlink(brc.getURL(singleLink).toString());
                    /* Usually we will crawl a lot of URLs at this stage --> Set onlinestatus right away! */
                    dl.setAvailable(true);
                    fp.add(dl);
                    if (!StringUtils.isEmpty(title)) {
                        nameTemp = videoID + "_" + title;
                    } else {
                        nameTemp = videoID;
                    }
                    dl.setName(nameTemp + ".mp4");
                    /* Packagizer properties */
                    dl.setProperty(XvideosCore.PROPERTY_USERNAME, username);
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                }
            }
            logger.info("Crawled quickies: " + videos.size());
        }
        final String url_base = this.br.getURL("/channels/" + username + "/videos/new").toString();
        short pageNum = 0;
        final short maxItemsPerPage = 36;
        final Browser brc = br.cloneBrowser();
        do {
            if (pageNum == 0) {
                // br.postPage("https://www.xvideos.com/channels/" + username + "/videos/best", "is_first=true&main_cats=false");
                brc.postPage(url_base, "is_first=true&main_cats=false");
            } else {
                // br.getPage("/" + type + "/" + username + "/videos/best/" + pageNum);
                brc.postPage(url_base + "/" + pageNum, "main_cats=false");
            }
            // users don't always have profile... as bug reporter Guardao finds links from google... false positive.
            if (brc.getHttpConnection().getResponseCode() == 403 || brc.getHttpConnection().getResponseCode() == 400) {
                if (ret.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    /* Stop in the middle -> Should never happen */
                    logger.info("Stopping because: Server issued error-response during crawl");
                    break;
                }
            }
            final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final int totalNumberofItems = ((Number) entries.get("nb_videos")).intValue();
            if (totalNumberofItems == 0) {
                logger.info("Stopping because: User doesn't have any videos");
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            int numberofItemsOnCurrentPage = 0;
            final List<Map<String, Object>> videos = (List<Map<String, Object>>) entries.get("videos");
            for (final Map<String, Object> video : videos) {
                final String singleLink = video.get("u").toString();
                final String videoID = video.get("id").toString();
                /* Only add new URLs */
                if (dupeList.add(videoID)) {
                    final String titleURL = new Regex(singleLink, "/([^/]+)$").getMatch(0);
                    final String nameTemp;
                    final DownloadLink dl = createDownloadlink(br.getURL(singleLink).toString());
                    /* Usually we will crawl a lot of URLs at this stage --> Set onlinestatus right away! */
                    dl.setAvailable(true);
                    fp.add(dl);
                    if (titleURL != null) {
                        nameTemp = videoID + "_" + cleanUrlTitle(titleURL);
                    } else {
                        nameTemp = videoID;
                    }
                    dl.setName(nameTemp + ".mp4");
                    /* Packagizer properties */
                    dl.setProperty(XvideosCore.PROPERTY_USERNAME, username);
                    dl._setFilePackage(fp);
                    ret.add(dl);
                    distribute(dl);
                    numberofItemsOnCurrentPage++;
                } else {
                    // logger.info("Found dupe: " + videoID);
                }
            }
            logger.info("Crawled page: " + pageNum + " | Crawled items on current page: " + numberofItemsOnCurrentPage + " | Progress overall: " + ret.size() + "/" + totalNumberofItems);
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (ret.size() >= totalNumberofItems) {
                /* We found all items */
                logger.info("Stopping because: Reached the end");
                break;
            } else if (numberofItemsOnCurrentPage < maxItemsPerPage) {
                /* Fail-safe */
                logger.info("Stopping because: Probably reached the end");
                break;
            } else {
                /* Proceed to next page */
                pageNum++;
            }
        } while (true);
        if (ret.isEmpty()) {
            logger.warning("Decrypter broken for link: " + url);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private String cleanUrlTitle(final String urlTitle) {
        if (urlTitle == null) {
            return null;
        }
        String clean = urlTitle.replaceAll("(watch_)?(free_)?(live_)?camgirls_at_(www(_|\\.))?teenhdcams(_|\\.)com$", "");
        clean = clean.replaceAll("(watch_)?free_at_(www(_|\\.))?teenhdcams(_|\\.)com$", "");
        clean = clean.replaceAll("(watch_)?full_video_at_(www(_|\\.))?teenhdcams(_|\\.)com$", "");
        clean = clean.replaceAll("\\.*_*$", "");
        clean = clean.replace("_", " ");
        return clean;
    }

    /**
     * 2020-10-06: Special method for xvideos.red though this will also work for xvideos.com. Old method is still in use as it's still
     * working!
     */
    private ArrayList<DownloadLink> crawlChannelPremium(final CryptedLink param) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final ArrayList<String> dupeList = new ArrayList<String>();
        final Regex urlinfo = new Regex(param.getCryptedUrl(), "https?://[^/]+/([^/]+)/([^/#]+)");
        final String type = urlinfo.getMatch(0);
        final String username = urlinfo.getMatch(1);
        if (!br.getURL().contains(username)) {
            /* E.g. redirect to mainpage */
            logger.info("Profile does not exist anymore");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(username.trim()));
        fp.addLinks(ret);
        short pageNum = 0;
        int decryptedLinksNum;
        do {
            logger.info(String.format("Crawling page %d", pageNum));
            decryptedLinksNum = 0;
            br.getPage("/" + type + "/" + username + "/videos-premium/best/" + pageNum);
            // users don't always have profile... as Guardao finds links from google... false positive.
            if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 400) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Unescape json */
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.getRequest().getHtmlCode()));
            final String[] links = br.getRegex("(/video\\d+/[^\"]+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.info("Stopping because: Failed to find anything on current page");
                break;
            }
            decryptedLinksNum = links.length;
            for (String singleLink : links) {
                final Regex urlRegex = new Regex(singleLink, "/video(\\d+)/([^\"]+)");
                final String videoID = urlRegex.getMatch(0);
                /* Only add new URLs */
                if (!dupeList.contains(videoID)) {
                    singleLink = "https://www." + this.br.getHost() + singleLink;
                    final String urlTitle = urlRegex.getMatch(1);
                    final String nameTemp;
                    final DownloadLink dl = createDownloadlink(singleLink);
                    /* Usually we will crawl a lot of URLs at this stage --> Set onlinestatus right away! */
                    dl.setAvailable(true);
                    fp.add(dl);
                    if (urlTitle != null) {
                        nameTemp = videoID + "_" + cleanUrlTitle(urlTitle);
                    } else {
                        nameTemp = videoID;
                    }
                    dl.setName(nameTemp + ".mp4");
                    /* Packagizer properties */
                    dl.setProperty(XvideosCore.PROPERTY_USERNAME, username);
                    ret.add(dl);
                    distribute(dl);
                    decryptedLinksNum++;
                    dupeList.add(videoID);
                }
            }
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (decryptedLinksNum < 36) {
                logger.info("Stopping because: Crawled page contains less items than a full page -> Reached end?");
                break;
            } else {
                pageNum++;
                continue;
            }
        } while (true);
        if (ret.size() == 0) {
            logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private ArrayList<DownloadLink> crawlPhotos(final String parameter) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex urlinfo = new Regex(parameter, "https?://[^/]+/([^/]+)/([^/]+)/([^/]+)/([^/]+)/([^/]+)");
        final String type = urlinfo.getMatch(0);
        final String username = urlinfo.getMatch(1);
        final String galleryID = urlinfo.getMatch(3);
        final String galleryName = urlinfo.getMatch(4);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + " - " + galleryName);
        fp.addLinks(ret);
        /* These are direct-URLs */
        final String[] links = br.getRegex("class=\"embed\\-responsive\\-item\" href=\"(https?[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int counter = 1;
        for (String singleLink : links) {
            String url_filename = getFileNameFromURL(new URL(singleLink));
            if (url_filename == null) {
                url_filename = "_" + counter;
            }
            url_filename = username + "_" + galleryID + "_" + galleryName + url_filename;
            final DownloadLink dl = createDownloadlink(singleLink);
            /* Usually we will crawl a lot of URLs at this stage --> Set onlinestatus right away! */
            dl.setAvailable(true);
            fp.add(dl);
            dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            dl.setFinalFileName(url_filename);
            /* Packagizer properties */
            dl.setProperty(XvideosCore.PROPERTY_USERNAME, username);
            ret.add(dl);
            counter++;
        }
        if (ret.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    private String createSingleVideoURL(final String videoID, final String slug) {
        String videourl = "https://www." + this.getHost() + "/video" + videoID;
        if (slug != null) {
            videourl += "/" + slug;
        } else {
            videourl += "/x";
        }
        return videourl;
    }
}
