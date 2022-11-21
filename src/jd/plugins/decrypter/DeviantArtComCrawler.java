//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.config.DeviantArtComConfig;
import org.jdownloader.plugins.components.config.DeviantArtComConfig.ImageDownloadMode;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DeviantArtCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deviantart.com" }, urls = { "https?://[\\w\\.\\-]*?deviantart\\.com/(?!(?:[^/]+/)?art/|status/)[^<>\"]+" })
public class DeviantArtComCrawler extends PluginForDecrypt {
    /**
     * @author raztoki, pspzockerscene
     */
    public DeviantArtComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    // This plugin grabs range of content depending on parameter.
    // profile.devart.com/gallery/uid*
    // profile.devart.com/favorites/uid*
    // profile.devart.com/gallery/*
    // profile.devart.com/favorites/*
    // * = ?offset=\\d+
    //
    // All of the above formats should support spanning pages, but when
    // parameter contains '?offset=x' it will not span.
    //
    // profilename.deviantart.com/art/uid/ == grabs the 'download image' (best
    // quality available).
    //
    // I've created the plugin this way to allow users to grab as little or as
    // much, content as they wish. Hopefully this wont create any
    // issues.
    private final String        PATTERN_USER           = "^https?://[^/]+/([\\w\\-]+)$";
    private final String        PATTERN_USER_FAVORITES = "^https?://[^/]+/([\\w\\-]+)/favourites.*";
    private final String        PATTERN_GALLERY        = "^https?://[^/]+/([\\w\\-]+)/gallery/(\\d+)/([\\w\\-]+)$";
    private final String        PATTERN_COLLECTIONS    = "https?://[\\w\\.\\-]*?deviantart\\.com/.*?/collections(/.+)?";
    private static final String PATTERN_CATPATH_ALL    = "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?(gallery|favourites)/\\?catpath(=.+)?";
    private static final String PATTERN_CATPATH_1      = "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?(gallery|favourites)/\\?catpath(=(/|%2F([a-z0-9]+)?|[a-z0-9]+)(\\&offset=\\d+)?)?";
    private static final String PATTERN_CATPATH_2      = "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?(gallery|favourites)/\\?catpath=[a-z0-9]{1,}(\\&offset=\\d+)?";
    private static final String PATTERN_JOURNAL        = "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?journal.+";
    private static final String PATTERN_JOURNAL2       = "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?journal/[\\w\\-]+/?";
    private static final String PATTERN_BLOG           = "https?://[\\w\\.\\-]*?deviantart\\.com/(?:[^/]+/)?blog/(\\?offset=\\d+)?";
    // private static final String TYPE_INVALID = "https?://[\\w\\.\\-]*?deviantart\\.com/stats/*?";
    private String              parameter              = null;
    private final boolean       fastLinkCheck          = true;
    private boolean             forceHtmlDownload      = false;
    private long                decryptedUrlsNum       = 0;

    protected void distribute(final DownloadLink dl) {
        super.distribute(dl);
        decryptedUrlsNum++;
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        DeviantArtCom.prepBR(this.br);
        forceHtmlDownload = PluginJsonConfig.get(DeviantArtComConfig.class).getImageDownloadMode() == ImageDownloadMode.HTML;
        parameter = param.toString();
        /* Remove trash */
        final String replace = new Regex(parameter, "(#.+)").getMatch(0);
        if (replace != null) {
            parameter = parameter.replace(replace, "");
        }
        /* Fix journallinks: http://xx.deviantart.com/journal/poll/xx/ */
        parameter = parameter.replaceAll("/(poll|stats)/", "/");
        if (parameter.matches(PATTERN_JOURNAL2)) {
            final DownloadLink journal = createDownloadlink(parameter.replace("deviantart.com/", "deviantartdecrypted.com/"));
            journal.setName(new Regex(parameter, "deviantart\\.com/(?:[^/]+/)?journal/([\\w\\-]+)").getMatch(0) + ".html");
            if (fastLinkCheck) {
                journal.setAvailable(true);
            }
            distribute(journal);
            return ret;
        }
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        /* Login if possible. Sometimes not all items of a gallery are visible without being logged in. */
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (account != null) {
            final DeviantArtCom plg = (DeviantArtCom) this.getNewPluginForHostInstance(this.getHost());
            plg.login(account, false);
        }
        if (param.getCryptedUrl().matches(PATTERN_USER_FAVORITES)) {
            return this.crawlProfileFavorites(param);
        } else if (parameter.matches(PATTERN_JOURNAL)) {
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            crawlJournals();
        } else if (new Regex(parameter, Pattern.compile(PATTERN_COLLECTIONS, Pattern.CASE_INSENSITIVE)).matches()) {
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            crawlCollections();
        } else if (parameter.matches(PATTERN_BLOG)) {
            br.getPage(parameter);
            if (this.br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            crawlBlog();
        } else if (parameter.matches(PATTERN_GALLERY)) {
            return this.crawlProfileOrGallery(param);
        } else {
            return this.crawlProfileOrGallery(param);
        }
        if (decryptedUrlsNum == 0) {
            logger.info("Failed to find any results: " + parameter);
            return ret;
        }
        return ret;
    }

    private void crawlJournals() throws DecrypterException, IOException, PluginException {
        if (br.containsHTML("class=\"empty\\-state journal\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String username = getSiteUsername();
        if (username == null) {
            username = getURLUsername();
        }
        String paramdecrypt;
        if (parameter.contains("catpath=/")) {
            paramdecrypt = parameter.replace("catpath=/", "catpath=%2F") + "&offset=";
        } else {
            paramdecrypt = parameter + "?offset=";
        }
        if (username == null) {
            logger.warning("Plugin broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + " - Journal");
        fp.setAllowMerge(true);
        String next = null;
        int previousOffset = 0;
        int currentOffset = 0;
        final boolean stop_after_first_run = getOffsetFromURL() != null;
        do {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user: " + parameter);
                return;
            }
            logger.info("Decrypting offset " + next);
            if (next != null) {
                currentOffset = Integer.parseInt(next);
                /* Fail safe */
                if (currentOffset <= previousOffset) {
                    logger.info("Seems like we're done!");
                    break;
                }
                br.getPage(paramdecrypt + next);
            }
            final String jinfo[] = br.getRegex("data\\-deviationid=\"\\d+\" href=\"(https?://[\\w\\.\\-]*?\\.deviantart\\.com/(?:[^/]+/)?journal/[\\w\\-]+)\"").getColumn(0);
            if (jinfo == null || jinfo.length == 0) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            for (final String link : jinfo) {
                final String urltitle = new Regex(link, "deviantart\\.com/(?:[^/]+/)?journal/([\\w\\-]+)").getMatch(0);
                final DownloadLink dl = createDownloadlink(link.replace("deviantart.com/", "deviantartdecrypted.com/"));
                if (fastLinkCheck) {
                    dl.setAvailable(true);
                }
                /* No reason to hide their single links */
                dl.setContentUrl(link);
                dl.setName(urltitle + ".html");
                dl.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.HTML);
                dl._setFilePackage(fp);
                distribute(dl);
            }
            next = br.getRegex("class=\"next\"><a class=\"away\" data\\-offset=\"(\\d+)\"").getMatch(0);
            previousOffset = currentOffset;
            if (stop_after_first_run) {
                logger.info("Decrypted given offset, stopping...");
                break;
            }
        } while (next != null);
    }

    private void crawlCollections() throws DecrypterException {
        final String[] links = br.getRegex("<a href=\"(https?://[^<>\"/]+\\.deviantart\\.com/(?:[^/]+/)?(art|journal)/[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Plugin broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (final String aLink : links) {
            final DownloadLink dl = createDownloadlink(aLink);
            if (fastLinkCheck) {
                dl.setAvailable(true);
            }
            if (forceHtmlDownload) {
                dl.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.HTML);
            } else {
                dl.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            }
            distribute(dl);
        }
    }

    private void crawlBlog() throws DecrypterException, IOException, PluginException {
        if (br.containsHTML("(?i)>\\s*Sorry\\! This blog entry cannot be displayed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("name=\"og:title\" content=\"([^<>\"]*?) on DeviantArt\"").getMatch(0);
        final FilePackage fp;
        if (fpName != null) {
            fp = FilePackage.getInstance();
            fp.setName(fpName + " - Journal");
            fp.setAllowMerge(true);
        } else {
            fp = null;
        }
        final boolean stop_after_first_run = getOffsetFromURL() != null;
        int currentOffset = 0;
        do {
            logger.info("Decrypting offset " + currentOffset);
            if (currentOffset > 0) {
                accessOffset(currentOffset);
            }
            final String[] links = br.getRegex("<a href=\"(https?://[^<>\"/]+\\.deviantart\\.com/(?:[^/]+/)?journal/[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Plugin broken for link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            for (final String aLink : links) {
                final DownloadLink dl = createDownloadlink(aLink);
                if (fastLinkCheck) {
                    dl.setAvailable(true);
                }
                dl.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.HTML);
                if (fp != null) {
                    dl._setFilePackage(fp);
                }
                distribute(dl);
            }
            if (this.isAbort()) {
                logger.info("Crawler aborted by user");
                return;
            }
            currentOffset += 5;
        } while (br.containsHTML("class=\"next\"><a class=\"away\" data\\-offset=\"\\d+\"") && !stop_after_first_run);
    }

    private ArrayList<DownloadLink> crawlProfileFavorites(final CryptedLink param) throws IOException, PluginException {
        final String username = new Regex(param.getCryptedUrl(), PATTERN_USER_FAVORITES).getMatch(0);
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username + " - Favorites");
        final UrlQuery query = new UrlQuery();
        query.add("username", username);
        query.add("all_folder", "true");
        return this.crawlPagination(fp, "/_napi/da-user-profile/api/collection/contents", query);
    }

    private ArrayList<DownloadLink> crawlProfileOrGallery(final CryptedLink param) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String username = new Regex(br.getURL(), "https?://[^/]+/([^/\\?]+)").getMatch(0);
        final Regex gallery = new Regex(br.getURL(), PATTERN_GALLERY);
        String galleryID = null;
        String gallerySlug = null;
        if (gallery.matches()) {
            galleryID = gallery.getMatch(1);
            gallerySlug = gallery.getMatch(2);
        }
        if (username == null) {
            /* Developer mistake */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String csrftoken = br.getRegex("window\\.__CSRF_TOKEN__\\s*=\\s*'([^<>\"\\']+)';").getMatch(0);
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (gallerySlug != null) {
            fp.setName(username + " - " + gallerySlug.replace("-", " ").trim());
        } else {
            fp.setName(username);
        }
        final UrlQuery query = new UrlQuery();
        query.add("username", username);
        if (galleryID != null) {
            query.add("folderid", galleryID);
        } else {
            query.add("all_folder", "true");
        }
        return crawlPagination(fp, "/_napi/da-user-profile/api/gallery/contents", query);
    }

    private ArrayList<DownloadLink> crawlPagination(final FilePackage fp, final String action, final UrlQuery query) throws IOException, PluginException {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String csrftoken = br.getRegex("window\\.__CSRF_TOKEN__\\s*=\\s*'([^<>\"\\']+)';").getMatch(0);
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int page = 0;
        final int maxItemsPerPage = 24;
        int offset = 0;
        final HashSet<String> dupes = new HashSet<String>();
        query.add("limit", Integer.toString(maxItemsPerPage));
        query.add("csrf_token", Encoding.urlEncode(csrftoken));
        do {
            query.addAndReplace("offset", Integer.toString(offset));
            page++;
            br.getPage(action + "?" + query.toString());
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Number nextOffset = (Number) entries.get("nextOffset");
            final List<Map<String, Object>> results = (List<Map<String, Object>>) entries.get("results");
            if (results.isEmpty()) {
                if (ret.isEmpty()) {
                    logger.info("This item doesn't contain any items");
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    logger.info("Stopping because: Current page doesn't contain any items");
                    break;
                }
            }
            int numberofNewItems = 0;
            for (final Map<String, Object> result : results) {
                final Map<String, Object> deviation = (Map<String, Object>) result.get("deviation");
                final Map<String, Object> author = (Map<String, Object>) deviation.get("author");
                final String type = deviation.get("type").toString();
                final String url = deviation.get("url").toString();
                if (dupes.add(url)) {
                    numberofNewItems++;
                    final DownloadLink link = this.createDownloadlink(deviation.get("url").toString());
                    /**
                     * This file extension may change later when file is downloaded. </br>
                     * 2022-11-11: Items of type "literature" (or simply != "image") will not get any file extension at all at this moment.
                     */
                    String assumedFileExtension = "";
                    if (forceHtmlDownload) {
                        assumedFileExtension = ".html";
                    } else if (type.equalsIgnoreCase("image")) {
                        assumedFileExtension = ".jpg";
                    } else {
                        assumedFileExtension = ".html";
                    }
                    link.setName(deviation.get("title") + " by " + author.get("username") + "_" + author.get("userId") + assumedFileExtension);
                    link.setAvailable(true);
                    if (fp != null) {
                        link._setFilePackage(fp);
                    }
                    link.setProperty(DeviantArtCom.PROPERTY_TYPE, type);
                    ret.add(link);
                    distribute(link);
                }
            }
            logger.info("Crawled page " + page + " | Offset: " + offset + " | nextOffset: " + nextOffset + " | Found items so far: " + ret.size());
            if (this.isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else if (!(Boolean) entries.get("hasMore") || nextOffset == null) {
                logger.info("Stopping because: Reached end");
                break;
            } else if (numberofNewItems == 0) {
                /* Extra fail-safe */
                logger.info("Stopping because: Failed to find new items on page " + page);
                break;
            } else {
                /* Continue to next page */
                offset = nextOffset.intValue();
                continue;
            }
        } while (true);
        return ret;
    }

    private void accessOffset(final int offset) throws IOException {
        if (parameter.contains("?")) {
            br.getPage(parameter + "&offset=" + offset);
        } else {
            br.getPage(parameter + "?offset=" + offset);
        }
    }

    private String getOffsetFromURL() {
        return new Regex(parameter, "offset=(\\d+)").getMatch(0);
    }

    private String getSiteUsername() {
        return br.getRegex("name=\"username\" value=\"([^<>\"]*?)\"").getMatch(0);
    }

    private String getURLUsername() {
        return new Regex(parameter, "https?://(?:www\\.)?([A-Za-z0-9\\-]+)\\.deviantart.com/.+").getMatch(0);
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}