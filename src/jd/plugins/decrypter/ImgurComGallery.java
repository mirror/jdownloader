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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

    /*
     * TODO: Add API handling for this URL type 'subreddit': https://apidocs.imgur.com/?version=latest#98f68034-15a0-4044-a9ac-5ff3dc75c6b0
     */
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
        /* TODO: Test API functionality and add missing API calls (e.g. "reddit style" stuff) */
        // final boolean useApiForAlbumCrawling = DebugMode.TRUE_IN_IDE_ELSE_FALSE;
        // final boolean useApiForGalleryCrawling = DebugMode.TRUE_IN_IDE_ELSE_FALSE;
        final boolean useApiForAlbumCrawling = false;
        final boolean useApiForGalleryCrawling = false;
        synchronized (CTRLLOCK) {
            if (parameter.matches(type_subreddit_single_post)) {
                /* Single "reddit-style" posts can contain multiple images */
                siteCrawlSingleRedditStylePost();
            } else if (parameter.matches(type_subreddit_gallery)) {
                siteCrawlSubredditStyleGallery();
            } else if (parameter.matches(type_album)) {
                this.siteCrawlAlbum();
            } else if (parameter.matches(type_gallery)) {
                if (useApiForGalleryCrawling) {
                    this.apiCrawlGallery();
                } else {
                    this.siteCrawlGallery();
                }
            } else {
                /* Single item */
                this.decryptedLinks.add(handleSingleItem(this.parameter, this.itemID));
            }
        }
        return decryptedLinks;
    }

    private void prepareAPIUsage() throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        final boolean useAPIInAnonymousMode = cfg.getBooleanProperty(ImgurComHoster.SETTING_USE_API_IN_ANONYMOUS_MODE, true);
        if (!ImgurComHoster.canUseAPI()) {
            logger.info("API usage is impossible");
            throw new DecrypterException("API usage not possible but required");
        }
        if (useAPIInAnonymousMode) {
            br.getHeaders().put("Authorization", ImgurComHoster.getAuthorization());
        } else {
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            if (account == null) {
                logger.warning("User needs to add an account or enable anonymous API usage");
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
     * instead.
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

    private void api_decrypt(Map<String, Object> data) throws DecrypterException, ParseException {
        final boolean user_prefers_mp4 = ImgurComHoster.userPrefersMp4();
        long status = JavaScriptEngineFactory.toLong(data.get("status"), 200);
        if (status == 404) {
            /* Well in case it's a gallery link it might be a single picture */
            if (parameter.matches(type_gallery)) {
                final DownloadLink dl = createDownloadlink(getHostpluginurl(itemID));
                decryptedLinks.add(dl);
                return;
            }
            decryptedLinks.addAll(createOfflineLink(parameter));
            return;
        }
        final long imgcount = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(data, "data/images_count"), 0);
        Object images = JavaScriptEngineFactory.walkJson(data, "data/images");
        author = (String) JavaScriptEngineFactory.walkJson(data, "data/account_url");
        if (!StringUtils.isEmpty(author) && !StringUtils.equalsIgnoreCase(author, "Imgur")) {
            /* Some errorhandling */
            this.fp = FilePackage.getInstance();
            this.fp.setName(this.author);
        }
        /*
         * using links (i.imgur.com/imgUID(s)?.extension) seems to be problematic, it can contain 's' (imgUID + s + .extension), but not
         * always! imgUid.endswith("s") is also a valid uid, so you can't strip them!
         */
        final List<Map<String, Object>> items = (List<Map<String, Object>>) images;
        final int padLength = getPadLength(imgcount);
        int itemNumber = 0;
        for (final Map<String, Object> item : items) {
            itemNumber++;
            String directlink = (String) item.get("link");
            String title = (String) item.get("title");
            final String itemnumber_formatted = String.format(Locale.US, "%0" + padLength + "d", itemNumber);
            final long size = JavaScriptEngineFactory.toLong(item.get("size"), -1);
            final long size_mp4 = JavaScriptEngineFactory.toLong(item.get("mp4_size"), -1);
            final String imgUID = (String) item.get("id");
            /* TODO: Find video_source */
            // videoSource = null;
            String filetype = (String) item.get("type");
            if (imgUID == null || (size == -1 && size_mp4 == -1) || directlink == null || filetype == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            if (filetype.matches("image/[A-Za-z0-9]+")) {
                /* E.g. 'image/gif' --> 'gif' */
                filetype = filetype.split("/")[1];
            }
            final long filesize;
            if (user_prefers_mp4 && size_mp4 > 0) {
                filesize = size_mp4;
                filetype = "mp4";
            } else {
                filesize = size;
            }
            final DownloadLink dl = createDownloadlink(this.getHostpluginurl(imgUID));
            dl.setAvailable(true);
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DIRECT_URL, directlink);
            if (!StringUtils.isEmpty(title)) {
                title = Encoding.htmlDecode(title);
                title = HTMLEntities.unhtmlentities(title);
                title = HTMLEntities.unhtmlAmpersand(title);
                title = HTMLEntities.unhtmlAngleBrackets(title);
                title = HTMLEntities.unhtmlSingleQuotes(title);
                title = HTMLEntities.unhtmlDoubleQuotes(title);
                title = encodeUnicode(title);
                dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_TITLE, title);
            }
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_USERNAME, author);
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_ORDERID, itemnumber_formatted);
            final String filename = ImgurComHoster.getFormattedFilename(dl);
            dl.setFinalFileName(filename);
            dl.setDownloadSize(filesize);
            /* No need to hide directlinks */
            dl.setContentUrl(ImgurComHoster.getURLContent(imgUID));
            /* TODO: Find video_source */
            // if (videoSource != null && grabVideoSource) {
            // decryptedLinks.add(this.createDownloadlink(videoSource));
            // }
            decryptedLinks.add(dl);
        }
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
            final String json = br.getRegex("image\\s*:\\s*(\\{.+\\})").getMatch(0);
            Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            this.author = (String) entries.get("author");
            entries = (Map<String, Object>) entries.get("album_images");
            this.crawlWebsiteJson(entries);
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
        fp.setName(albumID);
        this.br.getPage("https://" + this.getHost() + "/ajaxalbums/getimages/" + albumID + "/hit.json?all=true");
        /* 2020-09-29: Returns the following response on invalid albumID: {"data":[],"success":true,"status":200} */
        Map<String, Object> entries = JSonStorage.restoreFromString(this.br.toString(), TypeRef.HASHMAP);
        this.author = (String) entries.get("author");
        entries = (Map<String, Object>) entries.get("data");
        this.crawlWebsiteJson(entries);
    }

    private void apiCrawlGallery() throws Exception {
        prepareAPIUsage();
        br.getPage(ImgurComHoster.getAPIBaseWithVersion() + "/gallery/" + itemID);
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
            /* TODO: Improve this handling to add/check/parse image data right away! */
            /* We have a single picture and not an album. */
            final DownloadLink dl = createDownloadlink(getHostpluginurl(itemID));
            decryptedLinks.add(dl);
            return;
        } else if (is_album) {
            br.getPage(ImgurComHoster.getAPIBaseWithVersion() + "/album/" + itemID);
        }
        final Map<String, Object> data = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        String galleryTitle = (String) JavaScriptEngineFactory.walkJson(data, "data/title");
        api_decrypt(data);
        if (galleryTitle != null) {
            galleryTitle = Encoding.htmlDecode(galleryTitle).trim();
        }
        /* TODO: Fix usage of custom packagenames */
        // fpName = ImgurComHoster.getFormattedPackagename(author, galleryTitle, itemID);
        // fpName = encodeUnicode(fpName);
    }

    private void siteCrawlGallery() throws DecrypterException, ParseException, IOException {
        final String galleryID = new Regex(this.parameter, type_gallery).getMatch(0);
        this.fp = FilePackage.getInstance();
        fp.setName(galleryID);
        this.br.getPage("https://" + this.getHost() + "/gallery/" + galleryID + "/album_images/hit.json?all=true");
        /* 2020-09-29: Returns the following response on invalid albumID: {"data":[],"success":true,"status":200} */
        Map<String, Object> entries = JSonStorage.restoreFromString(this.br.toString(), TypeRef.HASHMAP);
        this.author = (String) entries.get("author");
        entries = (Map<String, Object>) entries.get("data");
        this.crawlWebsiteJson(entries);
    }

    private void crawlWebsiteJson(Map<String, Object> entries) throws ParseException {
        final boolean user_prefers_mp4 = ImgurComHoster.userPrefersMp4();
        final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("images");
        int itemNumber = 0;
        final int padLength = getPadLength(ressourcelist.size());
        for (final Object imageO : ressourcelist) {
            itemNumber++;
            entries = (Map<String, Object>) imageO;
            String title = (String) entries.get("title");
            final String itemnumber_formatted = String.format(Locale.US, "%0" + padLength + "d", itemNumber);
            final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            final String imgUID = (String) entries.get("hash");
            final String dateStr = (String) entries.get("datetime");
            final String video_source = (String) entries.get("video_source");
            /* TODO: What is this */
            // videoSource = PluginJSonUtils.getJson(item, "video_source");
            String ext = (String) entries.get("ext");
            if (StringUtils.isEmpty(imgUID) || StringUtils.isEmpty(ext)) {
                /* Skip invalid items */
                continue;
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
            dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_ORDERID, itemnumber_formatted);
            if (!StringUtils.isEmpty(dateFormatted)) {
                dl.setProperty(ImgurComHoster.PROPERTY_DOWNLOADLINK_DATE, dateFormatted);
            }
            final String filename = ImgurComHoster.getFormattedFilename(dl);
            dl.setFinalFileName(filename);
            /* No need to hide directlinks */
            dl.setContentUrl(ImgurComHoster.getURLContent(imgUID));
            if (!StringUtils.isEmpty(video_source) && grabVideoSource) {
                final DownloadLink externalVideo = this.createDownloadlink(video_source);
                decryptedLinks.add(externalVideo);
                distribute(externalVideo);
            }
            decryptedLinks.add(dl);
            distribute(dl);
        }
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
                        message += "Um Gallerien und Alben von imgur herunterladen zu können musst du deine eigenen Imgur API Zugangsdaten und/oder deinen imgur Account in JD eintragen.\r\n";
                    } else {
                        title = "Imgur.com - Gallery/Album crawler";
                        message += "Hello dear Imgur user\r\n";
                        message += "You need to add your own Imgur API credentials and or imgur account to JD in order to be able to crawl galleries & albums.\r\n";
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
}
