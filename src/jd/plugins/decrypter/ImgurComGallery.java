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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.ImgurComHoster;
import jd.utils.JDUtilities;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.scripting.JavaScriptEngineFactory;

/*Only accept single-imag URLs with an LID-length or either 5 OR 7 - everything else are invalid links or thumbnails*/
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ImgurComGallery extends PluginForDecrypt {
    public ImgurComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imgur.com" });
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
            final StringBuilder sb = new StringBuilder();
            final String hostsPatternPart = buildHostsPatternPart(domains);
            final String protocolPart = "https?://(?:www\\.|m\\.)?";
            /* Gallery URLs */
            sb.append(protocolPart + hostsPatternPart + "/(?:gallery|a)/[A-Za-z0-9]{5,7}");
            sb.append("|");
            /* Direct-URLs (and those without file-extension) */
            sb.append("https?://i\\." + hostsPatternPart + "/(?:[A-Za-z0-9]{7}|[A-Za-z0-9]{5})(?:\\.[A-Za-z0-9]{3,5})?");
            sb.append("|");
            /* "View"/Download URLs */
            sb.append(protocolPart + hostsPatternPart + "/(?!download/)(?:[A-Za-z0-9]{7}|[A-Za-z0-9]{5})");
            sb.append("|");
            /* "Reddit gallery" URLs (including those that only lead to single images) */
            sb.append(protocolPart + hostsPatternPart + "/r/[^/]+(?:/[A-Za-z0-9]{5,7})?");
            ret.add(sb.toString());
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2020-09-29: Preventive measure */
        return 1;
    }

    private final String            type_subreddit_single_post     = "https?://[^/]+/r/([^/]+/[A-Za-z0-9]{5,7})";
    private final String            type_subreddit_gallery         = "https?://[^/]+/r/([^/]+)$";
    private final String            type_album                     = "https?://[^/]+/a/([A-Za-z0-9]{5,7})";
    private final String            type_gallery                   = "https?://[^/]+/gallery/([A-Za-z0-9]{5,7})";
    public final static String      type_single_direct             = "https?://i\\.[^/]+/([A-Za-z0-9]{5,7})\\..+";
    public final static String      type_single_direct_without_ext = "https?://i\\.[^/]+/([A-Za-z0-9]{5,7})$";
    /* Constants */
    private static Object           CTRLLOCK                       = new Object();
    private ArrayList<DownloadLink> decryptedLinks                 = new ArrayList<DownloadLink>();
    private String                  parameter                      = null;
    private String                  itemID                         = null;
    private String                  author                         = null;
    private boolean                 grabVideoSource                = false;
    private FilePackage             fp                             = null;

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        parameter = param.toString().replace("://m.", "://").replace("http://", "https://").replaceFirst("/all$", "");
        if (this.parameter.matches(type_single_direct)) {
            itemID = new Regex(parameter, type_single_direct).getMatch(0);
        } else {
            /* For all other types, the ID we are looking for is at the end of our URL. */
            itemID = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        }
        this.prepBRWebsite(this.br);
        grabVideoSource = cfg.getBooleanProperty(ImgurComHoster.SETTING_GRAB_SOURCE_URL_VIDEO, ImgurComHoster.defaultSOURCEVIDEO);
        final boolean useAPI = ImgurComHoster.isAPIEnabled();
        synchronized (CTRLLOCK) {
            if (parameter.matches(type_subreddit_single_post)) {
                /* Single "reddit-style" posts can contain multiple images */
                siteCrawlSingleRedditStylePost();
            } else if (parameter.matches(type_subreddit_gallery)) {
                if (useAPI) {
                    this.apiCrawlSubredditStyleGallery();
                } else {
                    this.siteCrawlSubredditStyleGallery();
                }
            } else if (parameter.matches(type_album)) {
                if (useAPI) {
                    this.apiCrawlAlbum();
                } else {
                    this.siteCrawlAlbum();
                }
            } else if (parameter.matches(type_gallery)) {
                if (useAPI) {
                    this.apiCrawlGallery();
                } else {
                    this.siteCrawlGallery();
                }
            } else {
                /* Single item - no http request required thus no API/website difference! */
                this.decryptedLinks.add(handleSingleItem(this.parameter, this.itemID));
            }
        }
        return decryptedLinks;
    }

    /** Call this once before attempting any API requests! */
    private void prepareAPIUsage() throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean useAPIInAnonymousMode = cfg.getBooleanProperty(ImgurComHoster.SETTING_USE_API_IN_ANONYMOUS_MODE, true);
        if (!ImgurComHoster.canUseAPI()) {
            logger.info("API usage is impossible");
            ImgurComHoster.showAPIPreparationInformation();
            throw new DecrypterException("API usage not possible but required");
        }
        if (useAPIInAnonymousMode) {
            br.getHeaders().put("Authorization", ImgurComHoster.getAuthorization());
        } else {
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            if (account == null) {
                logger.warning("User needs to add an account or enable anonymous API usage");
                showLoginRequiredDialog();
                throw new AccountRequiredException();
            }
            final PluginForHost hostPlg = JDUtilities.getPluginForHost(this.getHost());
            if (account != null) {
                ((ImgurComHoster) hostPlg).login(this.br, account, false);
            }
        }
    }

    public static String getURLMp4Download(final String imgUID) {
        return getDirectURL(imgUID, "mp4");
    }

    public static String getURLGifDownload(final String imgUID) {
        return getDirectURL(imgUID, "gif");
    }

    public static String getDirectURL(final String imgUID, String ext) {
        if (ext.contains(".")) {
            ext = ext.replace(".", "");
        }
        return "https://i.imgur.com/" + imgUID + "." + ext;
    }

    private String getHostpluginurl(final String lid) {
        return "https://imgur.com/download/" + lid;
    }

    /**
     * Handles single imgur URLs - also respects users' settings if e.g. URL has a .gif ending but user prefers to download .mp4 files
     * instead. Does not perform any http requests.
     */
    private DownloadLink handleSingleItem(final String url, final String contentID) throws ParseException {
        /* Single images --> Host plugin without requiring any HTTP requests */
        final DownloadLink dl = createDownloadlink(getHostpluginurl(contentID));
        if (url.matches("https?://i\\.imgur\\.com/[A-Za-z0-9]+\\.(gif|gifv|mp4)")) {
            /* Direct-URL video */
            final String directurl;
            /* Obey user plugin setting. */
            if (jd.plugins.hoster.ImgurComHoster.userPrefersMp4()) {
                directurl = getURLMp4Download(contentID);
            } else {
                /* .gifv --> .gif */
                directurl = getURLGifDownload(contentID);
            }
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DIRECT_URL, directurl);
        } else if (this.parameter.matches("https?://i\\.imgur\\.com/[A-Za-z0-9]+\\.[a-z0-9]+")) {
            /* Direct-URL photo */
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DIRECT_URL, this.parameter);
        } else {
            /* URL without known file-extension */
        }
        dl.setContentUrl(url);
        return dl;
    }

    /** Crawls json results of gallery- and album API calls */
    private void apiCrawlJsonMultipleItems(Map<String, Object> data) throws DecrypterException, ParseException {
        apiCrawlJsonMultipleItems(data, 0);
    }

    private void apiCrawlJsonMultipleItems(Map<String, Object> data, final int index) throws DecrypterException, ParseException {
        final long status = JavaScriptEngineFactory.toLong(data.get("status"), 200);
        if (status == 404) {
            decryptedLinks.addAll(createOfflineLink(parameter));
            return;
        }
        this.author = (String) data.get("account_url");
        final String galleryTitle = (String) data.get("title");
        this.fp = FilePackage.getInstance();
        this.fp.setName(getFormattedPackagename(this.author, galleryTitle, this.itemID));
        final long imgcount = JavaScriptEngineFactory.toLong(data.get("images_count"), 0);
        Object images = data.get("images");
        final List<Map<String, Object>> items = (List<Map<String, Object>>) images;
        final int padLength = getPadLength(imgcount);
        int itemNumber = index;
        for (final Map<String, Object> item : items) {
            itemNumber++;
            final DownloadLink dl = apiCrawlJsonSingleItem(item);
            final String itemnumber_formatted = String.format(Locale.US, "%0" + padLength + "d", itemNumber);
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_ORDERID, itemnumber_formatted);
            final String filename = ImgurComHoster.getFormattedFilename(dl);
            dl.setFinalFileName(filename);
            decryptedLinks.add(dl);
        }
    }

    private DownloadLink apiCrawlJsonSingleItem(final Map<String, Object> item) throws DecrypterException, ParseException {
        final String imgUID = (String) item.get("id");
        final boolean is_album = item.containsKey("is_album") ? ((Boolean) item.get("is_album")).booleanValue() : false;
        if (imgUID == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        if (is_album) {
            final DownloadLink dl = this.createDownloadlink("https://" + this.getHost() + "/a/" + imgUID);
            return dl;
        } else {
            final String videoSource = (String) item.get("video_source");
            String title = (String) item.get("title");
            final String description = (String) item.get("description");
            final long size = JavaScriptEngineFactory.toLong(item.get("size"), -1);
            final long size_mp4 = JavaScriptEngineFactory.toLong(item.get("mp4_size"), -1);
            String filetype = (String) item.get("type");
            if ((size == -1 && size_mp4 == -1) || filetype == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            final boolean user_prefers_mp4 = ImgurComHoster.userPrefersMp4();
            if (filetype.matches("image/[A-Za-z0-9]+")) {
                /* E.g. 'image/gif' --> 'gif' */
                filetype = filetype.split("/")[1];
            }
            final long filesize;
            final String directlink;
            if (user_prefers_mp4 && size_mp4 > 0) {
                filesize = size_mp4;
                filetype = "mp4";
                directlink = getDirectURL(imgUID, "mp4");
            } else {
                filesize = size;
                directlink = (String) item.get("link");
            }
            final DownloadLink dl = createDownloadlink(this.getHostpluginurl(imgUID));
            dl.setAvailable(true);
            if (!StringUtils.isEmpty(directlink)) {
                dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DIRECT_URL, directlink);
            }
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_USERNAME, author);
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_USERNAME, author);
            if (!StringUtils.isEmpty(title)) {
                title = Encoding.htmlDecode(title);
                title = HTMLEntities.unhtmlentities(title);
                title = HTMLEntities.unhtmlAmpersand(title);
                title = HTMLEntities.unhtmlAngleBrackets(title);
                title = HTMLEntities.unhtmlSingleQuotes(title);
                title = HTMLEntities.unhtmlDoubleQuotes(title);
                dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_TITLE, title);
            }
            if (!StringUtils.isEmpty(description)) {
                dl.setComment(description);
            }
            final String filename = ImgurComHoster.getFormattedFilename(dl);
            dl.setFinalFileName(filename);
            dl.setDownloadSize(filesize);
            dl.setContentUrl(ImgurComHoster.getURLContent(imgUID));
            dl.setAvailable(true);
            if (videoSource != null && grabVideoSource) {
                decryptedLinks.add(this.createDownloadlink(videoSource));
            }
            return dl;
        }
    }

    public static void apiSetInformationOnDownloadLink(final DownloadLink link, final Map<String, Object> item) {
    }

    private void siteCrawlSubredditStyleGallery() throws PluginException, IOException, ParseException {
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String galleryName = new Regex(parameter, type_subreddit_gallery).getMatch(0);
        int page = 1;
        fp = FilePackage.getInstance();
        fp.setName(galleryName);
        final ArrayList<String> dupes = new ArrayList<String>();
        do {
            logger.info("Crawling page " + page);
            final String[] htmls = br.getRegex("(<div id=\"[A-Za-z0-9]+\" class=\"post\">.*?)</div>\\s+</div>").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                logger.info("Failed to find any items --> Assuming we've reached the end");
                break;
            }
            boolean foundNewItems = false;
            for (final String html : htmls) {
                final String contentID = new Regex(html, "id=\"([A-Za-z0-9]+)\"").getMatch(0);
                final String postInfo = new Regex(html, "class=\"post-info\">([^>]+)").getMatch(0);
                String title = new Regex(html, "<p>([^>]+)</p>").getMatch(0);
                if (contentID == null || postInfo == null || title == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dupes.contains(contentID)) {
                    /* Skip dupes */
                    continue;
                }
                dupes.add(contentID);
                foundNewItems = true;
                /* Now find out what kind of content we got */
                final DownloadLink dl;
                if (postInfo.contains("album")) {
                    /*
                     * 2020-09-29: We could return these as "normal album URLs" but this would require usage of their API --> We want to try
                     * to avoid that!
                     */
                    // dl = this.createDownloadlink("https://" + this.getHost() + "/a/" + contentID);
                    dl = this.createDownloadlink(this.parameter + "/" + contentID);
                } else {
                    final String url;
                    if (postInfo.contains("animated")) {
                        url = getDirectURL(contentID, "mp4");
                    } else {
                        /*
                         * Assume we got a single .jpg image. It could also be another file-extension such as .png in some cases but this
                         * will be corrected on download-attempt.
                         */
                        url = getDirectURL(contentID, "jpg");
                    }
                    dl = this.handleSingleItem(url, contentID);
                    if (!StringUtils.isEmpty(title)) {
                        if (Encoding.isHtmlEntityCoded(title)) {
                            title = Encoding.htmlDecode(title);
                        }
                        dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_TITLE, title);
                    }
                    dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DIRECT_URL, url);
                    /* Set original contentURL so user has the same URLs when copying one as in browser. */
                    dl.setContentUrl(this.parameter + "/" + contentID);
                    dl.setAvailable(true);
                    final String filename = ImgurComHoster.getFormattedFilename(dl);
                    if (filename != null) {
                        dl.setName(filename);
                    }
                }
                decryptedLinks.add(dl);
                distribute(dl);
            }
            /* Fail-safe - prevent infinite-loops! */
            if (!foundNewItems) {
                logger.info("Stopping because failed to find any new IDs on current page");
                break;
            }
            br.getPage("/r/" + galleryName + "/new/page/" + page++ + "/hit?scrolled");
        } while (!isAbort());
        if (decryptedLinks.isEmpty()) {
            /* Probably offline */
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
        }
    }

    /**
     * @throws Exception
     */
    private void apiCrawlSubredditStyleGallery() throws Exception {
        this.prepareAPIUsage();
        final ArrayList<String> dupes = new ArrayList<String>();
        final String subredditName = new Regex(this.parameter, this.type_subreddit_gallery).getMatch(0);
        this.fp = FilePackage.getInstance();
        this.fp.setName("/r/" + subredditName);
        int page = 0;
        final int maxcount = 100;
        int count;
        int index = 0;
        /* We can't know the number of items before */
        final int padLength = getPadLength(1000);
        do {
            logger.info("Crawling page: " + page);
            /* Nullification */
            count = 0;
            br.getPage(ImgurComHoster.getAPIBaseWithVersion() + "/gallery/r/" + subredditName + "/page/" + page);
            if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
                this.decryptedLinks.add(this.createOfflinelink(this.parameter));
                return;
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            Object images = entries.get("data");
            final List<Map<String, Object>> items = (List<Map<String, Object>>) images;
            boolean foundNewItems = false;
            for (final Map<String, Object> item : items) {
                final String id = (String) item.get("id");
                if (StringUtils.isEmpty(id)) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (dupes.contains(id)) {
                    /* Skip duplicates */
                    continue;
                }
                dupes.add(id);
                foundNewItems = true;
                final DownloadLink dl = apiCrawlJsonSingleItem(item);
                final String itemnumber_formatted = String.format(Locale.US, "%0" + padLength + "d", index);
                dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_ORDERID, itemnumber_formatted);
                final String filename = ImgurComHoster.getFormattedFilename(dl);
                dl.setFinalFileName(filename);
                decryptedLinks.add(dl);
                index++;
                count++;
            }
            /* Fail-safe */
            if (!foundNewItems) {
                logger.info("Stopping because failed to find any new items on current page");
                break;
            }
            page += 1;
        } while (count == maxcount && !this.isAbort());
    }

    private void siteCrawlSingleRedditStylePost() throws DecrypterException, ParseException, IOException, PluginException {
        /* Single "reddit-style" posts can contain multiple images */
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        }
        final String postTitle = new Regex(parameter, type_subreddit_single_post).getMatch(0);
        fp = FilePackage.getInstance();
        fp.setName(postTitle);
        final boolean preferWebsiteJson = true;
        if (preferWebsiteJson) {
            final String json = br.getRegex("image\\s*:\\s*(\\{.+\\});\\s+").getMatch(0);
            Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            this.author = (String) entries.get("author");
            final Map<String, Object> album_images = (Map<String, Object>) entries.get("album_images");
            if (album_images != null) {
                this.websiteCrawlJsonMultipleItems(album_images);
            } else {
                /* Assume we got a single item and not an album */
                decryptedLinks.add(this.websiteCrawlJsonSingleItem(entries));
            }
        } else {
            final String[] htmls = br.getRegex("(<div id=\"[A-Za-z0-9]+\" class=\"post-image-container[^\"]*\".*?</div>)").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String html : htmls) {
                final String contentID = new Regex(html, "id=\"([A-Za-z0-9]+)\"").getMatch(0);
                String title = null;
                if (contentID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /* Now find out what kind of content we got */
                final String url;
                if (html.contains("schema.org/VideoObject")) {
                    // itemscope itemtype="http://schema.org/VideoObject"
                    /* single gif/mp4 */
                    url = getDirectURL(contentID, "gif");
                } else {
                    // itemscope itemtype="http://schema.org/ImageObject"
                    /* single jpg or an other type of image */
                    url = getDirectURL(contentID, "jpg");
                }
                final DownloadLink dl = this.handleSingleItem(url, contentID);
                if (!StringUtils.isEmpty(title)) {
                    if (Encoding.isHtmlEntityCoded(title)) {
                        title = Encoding.htmlDecode(title);
                    }
                    dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_TITLE, title);
                }
                dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DIRECT_URL, url);
                /* Set original contentURL so user has the same URLs when copying one as in browser. */
                dl.setContentUrl(this.parameter + "/" + contentID);
                dl.setAvailable(true);
                final String filename = ImgurComHoster.getFormattedFilename(dl);
                if (filename != null) {
                    dl.setName(filename);
                }
                decryptedLinks.add(dl);
                distribute(dl);
            }
        }
        if (decryptedLinks.isEmpty()) {
            /* Probably offline */
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
        }
    }

    private void siteCrawlAlbum() throws DecrypterException, ParseException, IOException {
        final String albumID = new Regex(this.parameter, type_album).getMatch(0);
        this.fp = FilePackage.getInstance();
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        brc.getPage(parameter);
        this.fp.setName(siteGetPackagenameForGalleryAndAlbum(brc, albumID));
        this.br.getPage("https://" + this.getHost() + "/ajaxalbums/getimages/" + albumID + "/hit.json?all=true");
        /* 2020-09-29: Returns the following response on invalid albumID: {"data":[],"success":true,"status":200} */
        Map<String, Object> entries = JSonStorage.restoreFromString(this.br.toString(), TypeRef.HASHMAP);
        this.author = (String) entries.get("author");
        final Object dataO = entries.get("data");
        if (!(dataO instanceof Map)) {
            /* 2020-10-06: Offline content e.g.: {"data":[],"success":true,"status":200} */
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        }
        entries = (Map<String, Object>) dataO;
        this.websiteCrawlJsonMultipleItems(entries);
    }

    private void apiCrawlAlbum() throws Exception {
        prepareAPIUsage();
        br.getPage(ImgurComHoster.getAPIBaseWithVersion() + "/album/" + itemID);
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("data");
        apiCrawlJsonMultipleItems(entries);
    }

    private void apiCrawlGallery() throws Exception {
        prepareAPIUsage();
        /* In loggedIN state, "mature" param would naturally be obtained by users' account setting but we always want to get all images. */
        br.getPage(ImgurComHoster.getAPIBaseWithVersion() + "/gallery/" + itemID + "?mature=true");
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("data");
        boolean is_album = false;
        final Object is_albumO = entries.get("is_album");
        if (is_albumO != null && is_albumO instanceof Boolean) {
            is_album = ((Boolean) is_albumO).booleanValue();
        }
        if (parameter.matches(type_gallery) && !is_album) {
            /* We have a single picture and not an album. */
            final DownloadLink dl = this.apiCrawlJsonSingleItem(entries);
            decryptedLinks.add(dl);
            return;
        } else if (is_album) {
            /* Not a gallery but an album */
            this.apiCrawlAlbum();
            return;
        }
        int page = 0;
        int maxcount = 100;
        int index = 0;
        do {
            logger.info("Crawling page: " + page);
            apiCrawlJsonMultipleItems(entries, index);
            // final int imgcount = (int) JavaScriptEngineFactory.toLong(entries.get("images_count"), 0);
            final ArrayList<Object> data = (ArrayList<Object>) entries.get("images");
            index += data.size();
            if (data.size() < 100) {
                logger.info("Stopping because current page contained less than " + maxcount + " items");
                break;
            } else {
                /* TODO: Test & enable pagination support */
                if (true) {
                    break;
                }
                page++;
                br.getPage(ImgurComHoster.getAPIBaseWithVersion() + "/gallery/" + itemID + "/page/" + page + "?mature=true");
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                entries = (Map<String, Object>) entries.get("data");
            }
        } while (!this.isAbort());
    }

    private void siteCrawlGallery() throws DecrypterException, ParseException, IOException {
        final String galleryID = new Regex(this.parameter, type_gallery).getMatch(0);
        this.fp = FilePackage.getInstance();
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        brc.getPage(parameter);
        this.fp.setName(siteGetPackagenameForGalleryAndAlbum(brc, galleryID));
        this.br.getPage("https://" + this.getHost() + "/gallery/" + galleryID + "/album_images/hit.json?all=true");
        Map<String, Object> entries = JSonStorage.restoreFromString(this.br.toString(), TypeRef.HASHMAP);
        final Object dataO = entries.get("data");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /*
             * E.g.
             * {"data":{"error":"Invalid gallery hash XXXX for gallery: main","request":"\/gallery\/XXXX\/album_images\/hit.json","method":
             * "GET"},"success":false,"status":404}
             */
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        } else if (!(dataO instanceof Map)) {
            /* Very rare case: Single image and not a gallery: {"data":[],"success":true,"status":200} */
            this.decryptedLinks.add(this.createDownloadlink(this.getHostpluginurl(this.itemID)));
            return;
        }
        /* 2020-09-29: Returns the following response on invalid albumID: {"data":[],"success":true,"status":200} */
        this.author = (String) entries.get("author");
        entries = (Map<String, Object>) dataO;
        this.websiteCrawlJsonMultipleItems(entries);
    }

    private String siteGetPackagenameForGalleryAndAlbum(final Browser br, final String itemID) throws ParseException {
        String title = br.getRegex("<title>\\s*(.*?)\\s*-\\s*(Album on )?Imgur\\s*<").getMatch(0);
        if (title == null) {
            /* Fallback in case of RegEx failure AND because not all galleries/albums have a title set! */
            title = itemID;
        }
        return getFormattedPackagename(""/* username is only available via api */, title, itemID);
    }

    /** Website- and API json are very similar. Keep the crawlers in separate methods nonetheless!! */
    private void websiteCrawlJsonMultipleItems(Map<String, Object> entries) throws ParseException {
        final ArrayList<Object> imagesO = (ArrayList<Object>) entries.get("images");
        int itemNumber = 0;
        final int padLength = getPadLength(imagesO.size());
        for (final Object imageO : imagesO) {
            itemNumber++;
            entries = (Map<String, Object>) imageO;
            final String itemnumber_formatted = String.format(Locale.US, "%0" + padLength + "d", itemNumber);
            final DownloadLink dl = this.websiteCrawlJsonSingleItem(entries);
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_ORDERID, itemnumber_formatted);
            final String filename = ImgurComHoster.getFormattedFilename(dl);
            /*
             * 2020-10-08: Do NOT set final filename here as website json is often missing information compared to API. This way, extended
             * check will be allowed in host plugin!
             */
            dl.setName(filename);
            this.decryptedLinks.add(dl);
            distribute(dl);
        }
    }

    private DownloadLink websiteCrawlJsonSingleItem(final Map<String, Object> entries) throws ParseException {
        final boolean user_prefers_mp4 = ImgurComHoster.userPrefersMp4();
        String title = (String) entries.get("title");
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
        final String imgUID = (String) entries.get("hash");
        final String dateStr = (String) entries.get("datetime");
        final String video_source = (String) entries.get("video_source");
        String ext = (String) entries.get("ext");
        if (StringUtils.isEmpty(imgUID) || StringUtils.isEmpty(ext)) {
            /* Invalid item */
            return null;
        }
        /* Correct sometimes broken ext */
        if (ext.contains("?")) {
            ext = ext.substring(0, ext.lastIndexOf("?"));
        }
        if (!StringUtils.isEmpty(title)) {
            title = Encoding.htmlDecode(title);
            title = HTMLEntities.unhtmlentities(title);
            title = HTMLEntities.unhtmlAmpersand(title);
            title = HTMLEntities.unhtmlAngleBrackets(title);
            title = HTMLEntities.unhtmlSingleQuotes(title);
            title = HTMLEntities.unhtmlDoubleQuotes(title);
            title = encodeUnicode(title);
        }
        final String dateFormatted;
        if (!StringUtils.isEmpty(dateStr)) {
            dateFormatted = new Regex(dateStr, "(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        } else {
            dateFormatted = dateStr;
        }
        final String directlink;
        if (ext.matches("\\.?(gif|mp4)")) {
            /* Respect user-setting */
            if (user_prefers_mp4) {
                directlink = getDirectURL(imgUID, "mp4");
            } else {
                directlink = getDirectURL(imgUID, "gif");
            }
        } else {
            directlink = getDirectURL(imgUID, ext.replace(".", ""));
        }
        final DownloadLink dl = createDownloadlink(this.getHostpluginurl(imgUID));
        dl.setDownloadSize(filesize);
        dl.setAvailable(true);
        dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DIRECT_URL, directlink);
        dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_TITLE, title);
        if (!StringUtils.isEmpty(author)) {
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_USERNAME, author);
        }
        if (!StringUtils.isEmpty(dateFormatted)) {
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DATE, dateFormatted);
        }
        final String filename = ImgurComHoster.getFormattedFilename(dl);
        /*
         * 2020-10-08: Do NOT set final filename here as website json is often missing information compared to API. This way, extended check
         * will be allowed in host plugin!
         */
        dl.setName(filename);
        dl.setContentUrl(ImgurComHoster.getURLContent(imgUID));
        if (!StringUtils.isEmpty(video_source) && grabVideoSource) {
            final DownloadLink externalVideo = this.createDownloadlink(video_source);
            decryptedLinks.add(externalVideo);
            distribute(externalVideo);
        }
        decryptedLinks.add(dl);
        return dl;
    }

    @Override
    protected DownloadLink createDownloadlink(final String url) {
        final DownloadLink dl = createDownloadlink(url, true);
        if (this.fp != null) {
            dl._setFilePackage(fp);
        }
        return dl;
    }

    private final int getPadLength(final long size) {
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

    private Browser prepBRWebsite(final Browser br) {
        br.setLoadLimit(br.getLoadLimit() * 2);
        ImgurComHoster.prepBRWebsite(br);
        return br;
    }

    private ArrayList<DownloadLink> createOfflineLink(final String link) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final DownloadLink offline = this.createDownloadlink(link);
        if (itemID != null) {
            offline.setFinalFileName(itemID);
        }
        decryptedLinks.add(offline);
        return decryptedLinks;
    }

    /** 2020-10-01: Use this in case they shut off all of the old website methods. */
    private Thread showLoginRequiredDialog() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "Imgur.com - Gallerie/Album Crawler";
                        message += "Hallo liebe(r) Imgur NutzerIn\r\n";
                        message += "Um Gallerien und Alben von imgur herunterladen zu können musst du deinen Imgur Account in JD eintragen.\r\n";
                    } else {
                        title = "Imgur.com - Gallery/Album crawler";
                        message += "Hello dear Imgur user\r\n";
                        message += "You need to add your Imgur account to JD in order to be able to crawl galleries & albums.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(1 * 60 * 1000);
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** Returns user defined packagename. */
    @SuppressWarnings("deprecation")
    public static String getFormattedPackagename(final String... params) throws ParseException {
        final SubConfiguration cfg = SubConfiguration.getConfig("imgur.com");
        String username = params[0];
        String title = params[1];
        String galleryid = params[2];
        if (StringUtils.isEmpty(galleryid)) {
            galleryid = "-";
        }
        if (StringUtils.isEmpty(username)) {
            username = "-";
        }
        if (StringUtils.isEmpty(title)) {
            title = "-";
        }
        String formattedFilename = cfg.getStringProperty(ImgurComHoster.SETTING_CUSTOM_PACKAGENAME, ImgurComHoster.defaultCustomPackagename);
        if (StringUtils.isEmpty(formattedFilename)) {
            /* Fallback to default packagename pattern */
            formattedFilename = ImgurComHoster.defaultCustomPackagename;
        }
        formattedFilename = formattedFilename.replace("*galleryid*", galleryid);
        formattedFilename = formattedFilename.replace("*username*", username);
        formattedFilename = formattedFilename.replace("*title*", title);
        formattedFilename = formattedFilename.replaceFirst("^([ \\-_]+)", "").trim();
        return formattedFilename;
    }
}
