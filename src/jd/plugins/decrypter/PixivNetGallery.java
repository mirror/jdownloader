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
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.config.PixivNetConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.PixivNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { PixivNet.class })
public class PixivNetGallery extends PluginForDecrypt {
    public PixivNetGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return PixivNet.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z]{2}/)?(artworks/\\d+|member_illust\\.php\\?mode=[a-z0-9]+\\&illust_id=\\d+|users/\\d+(/(?:artworks|illustrations|manga|bookmarks/artworks))?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private static final String TYPE_GALLERY = ".+/(?:member_illust\\.php\\?mode=[a-z]+\\&illust_id=|artworks/)(\\d+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PixivNet hostplugin = (PixivNet) getNewPluginForHostInstance(this.getHost());
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        PixivNet.prepBR(br);
        boolean loggedIn = false;
        if (account != null) {
            try {
                hostplugin.login(account, false);
                loggedIn = true;
            } catch (PluginException e) {
                handleAccountException(account, e);
            }
        }
        String itemID = new Regex(param.getCryptedUrl(), "id=(\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        String uploadDate = null;
        if (param.getCryptedUrl().matches(TYPE_GALLERY)) {
            itemID = new Regex(param.getCryptedUrl(), TYPE_GALLERY).getMatch(0);
            if (itemID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find galleryid");
            }
            br.getPage("https://www." + this.getHost() + "/ajax/illust/" + itemID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* {"error":true,"message":"Work has been deleted or the ID does not exist.","body":[]} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("body");
            /* 2020-05-08: illustType 0 = image, 1 = ??, 2 = animation (?) */
            final long illustType = JavaScriptEngineFactory.toLong(entries.get("illustType"), 0);
            uploadDate = (String) entries.get("uploadDate");
            int pagecount = (int) JavaScriptEngineFactory.toLong(entries.get("pageCount"), 1);
            final String username = (String) entries.get("userName");
            final String illustUploadDate = (String) entries.get("createDate");
            String illustTitle = (String) entries.get("illustTitle");
            String tags = null;
            // final String illustId = (String) entries.get("illustId");
            /*
             * Users want to have a maximum of information in filenames: https://board.jdownloader.org/showpost.php?p=462062&postcount=41
             */
            final String alt = (String) entries.get("alt");
            if (!StringUtils.isEmpty(alt) && !StringUtils.isEmpty(illustTitle)) {
                if (alt.contains(illustTitle)) {
                    illustTitle = alt;
                } else {
                    illustTitle = alt + " " + illustTitle;
                }
            } else {
                /* Fallback */
                illustTitle = itemID;
            }
            final String singleLink = (String) JavaScriptEngineFactory.walkJson(entries, "urls/regular");
            decryptedLinks.add(generateDownloadLink(param.getCryptedUrl(), itemID, illustTitle, illustUploadDate, username, tags, singleLink));
            if (pagecount > 1) {
                /* == click on "Load more" */
                br.getPage(String.format("https://www.%s/ajax/illust/%s/pages?lang=en", this.getHost(), itemID));
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final List<Object> additionalpics = (List<Object>) entries.get("body");
                int counter = 0;
                for (final Object picO : additionalpics) {
                    counter++;
                    if (counter == 1) {
                        /* Skip first object as we already crawled that! */
                        continue;
                    }
                    entries = (Map<String, Object>) picO;
                    final String directurl = (String) JavaScriptEngineFactory.walkJson(entries, "urls/regular");
                    if (StringUtils.isEmpty(directurl)) {
                        /* Skip invalid items */
                        continue;
                    }
                    decryptedLinks.add(generateDownloadLink(param.getCryptedUrl(), itemID, illustTitle, illustUploadDate, username, tags, directurl));
                }
            }
            if (illustType == 2) {
                logger.info("Found animation");
                try {
                    final String animationsMetadataURL = this.br.getURL("/ajax/illust/" + itemID + "/ugoira_meta?lang=en").toString();
                    br.getPage(animationsMetadataURL);
                    final String filenameBase = itemID + "_" + illustTitle;
                    if (PluginJsonConfig.get(this.getConfigInterface()).isCrawlAnimationsMetadata()) {
                        final DownloadLink meta = this.createDownloadlink(animationsMetadataURL);
                        meta.setFinalFileName(filenameBase + ".json");
                        meta.setAvailable(true);
                        meta.setProperty(PixivNet.ANIMATION_META, br.toString());
                        if (!StringUtils.isEmpty(uploadDate)) {
                            meta.setProperty(PixivNet.PROPERTY_UPLOADDATE, uploadDate);
                        }
                        if (!StringUtils.isEmpty(username)) {
                            /* Packagizer property */
                            meta.setProperty(PixivNet.PROPERTY_UPLOADER, username);
                        }
                        decryptedLinks.add(meta);
                    }
                    final String zipURL = PluginJSonUtils.getJson(br, "originalSrc");
                    if (!StringUtils.isEmpty(zipURL)) {
                        final DownloadLink zip = createDownloadlink(zipURL.replaceAll("https?://", "decryptedpixivnet://"));
                        zip.setProperty(PixivNet.PROPERTY_MAINLINK, param.getCryptedUrl());
                        // dl.setProperty(PixivNet.PROPERTY_GALLERYID, userid);
                        if (!StringUtils.isEmpty(uploadDate)) {
                            zip.setProperty(PixivNet.PROPERTY_UPLOADDATE, uploadDate);
                        }
                        if (!StringUtils.isEmpty(username)) {
                            /* Packagizer property */
                            zip.setProperty(PixivNet.PROPERTY_UPLOADER, username);
                        }
                        zip.setProperty(PixivNet.PROPERTY_GALLERYURL, br.getURL());
                        zip.setContentUrl(param.getCryptedUrl());
                        zip.setFinalFileName(filenameBase + ".zip");
                        zip.setAvailable(true);
                        decryptedLinks.add(zip);
                    }
                } catch (final Throwable e) {
                    logger.warning("Failure in animation crawler handling");
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(itemID + " " + illustTitle);
            fp.addLinks(decryptedLinks);
            return decryptedLinks;
        } else {
            /* Decrypt user */
            if (itemID == null) {
                /* 2020-01-27 */
                itemID = new Regex(param.getCryptedUrl(), "users/(\\d+)").getMatch(0);
                if (itemID == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find userid");
                }
            }
            br.getPage(param.getCryptedUrl());
            String fpName = br.getRegex("<meta property=\"og:title\" content=\"(.*?)(?:\\s*\\[pixiv\\])?\">").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            if (fpName == null) {
                fpName = itemID;
            } else {
                fpName = Encoding.htmlOnlyDecode(fpName);
                fpName = itemID + "_" + fpName;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            if (!PluginJsonConfig.get(this.getConfigInterface()).isCrawlUserWorksIndividually()) {
                fp.setAllowInheritance(true);
            }
            uploadDate = PluginJSonUtils.getJson(br, "uploadDate");
            if (isOffline(br)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (isAccountOrRightsRequired(br) && !loggedIn) {
                logger.info("Account required to crawl this particular content");
                throw new AccountRequiredException();
            }
            // final String total_numberof_items = br.getRegex("class=\"count-badge\">(\\d+) results").getMatch(0);
            int numberofitems_found_on_current_page = 0;
            final int max_numbeofitems_per_page = 20;
            int page = 0;
            int maxitemsperpage = 100;
            int offset = 0;
            int itemcounter = 0;
            String url = null;
            boolean isBookmarks = false;
            boolean isManga = false;
            final UrlQuery querybase = new UrlQuery();
            if (param.getCryptedUrl().contains("/bookmarks")) {
                url = String.format("https://www.%s/ajax/user/%s/illusts/bookmarks", br.getHost(), itemID);
                isBookmarks = true;
            } else if (param.getCryptedUrl().contains("/manga")) {
                url = String.format("https://www.%s/ajax/user/%s/profile/all", br.getHost(), itemID);
                // url = String.format("https://www.%s/ajax/user/%s/profile/illusts", br.getHost(), userid);
                // querybase.append("ids[]", "", false);
                // querybase.append("work_category", "manga", false);
                // querybase.append("is_first_page", "1", false);
                isManga = true;
            } else {
                /* 2020-04-06: E.g. "/illustrations" */
                url = String.format("https://www.%s/ajax/user/%s/profile/all", br.getHost(), itemID);
                /* All items on one page! */
                maxitemsperpage = -1;
            }
            do {
                numberofitems_found_on_current_page = 0;
                final HashSet<String> dups = new HashSet<String>();
                final Browser brc = br.cloneBrowser();
                brc.setLoadLimit(5 * 1024 * 1024);
                final UrlQuery query = querybase;
                if (page > 0) {
                    query.remove("is_first_page");
                }
                if (page == 0 && !isBookmarks) {
                    query.append("lang", "en", false);
                    brc.getPage(url + "?" + query.toString());
                } else {
                    query.append("tag", "", false);
                    query.append("offset", offset + "", false);
                    query.append("limit", maxitemsperpage + "", false);
                    query.append("rest", "show", false);
                    brc.getPage(url + "?" + query.toString());
                }
                final Map<String, Object> map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                if (map == null) {
                    break;
                }
                final Map<String, Object> body = (Map<String, Object>) map.get("body");
                if (body == null) {
                    break;
                }
                final Object worksO = body.get("works");
                if (worksO != null) {
                    /* E.g. bookmarks */
                    List<Object> works = null;
                    if (worksO instanceof Map) {
                        works = new ArrayList<Object>();
                        works.add(worksO);
                    } else {
                        works = (List<Object>) worksO;
                    }
                    for (final Object workO : works) {
                        final Map<String, Object> entries = (Map<String, Object>) workO;
                        String galleryID = StringUtils.valueOfOrNull(entries.get("illustId"));
                        if (StringUtils.isEmpty(galleryID)) {
                            /* 2020-10-23 */
                            galleryID = StringUtils.valueOfOrNull(entries.get("id"));
                        }
                        if (!StringUtils.isEmpty(galleryID) && dups.add(galleryID)) {
                            itemcounter++;
                            final DownloadLink dl = createDownloadlink(PixivNet.createSingleImageUrl(galleryID));
                            fp.add(dl);
                            decryptedLinks.add(dl);
                            distribute(dl);
                            numberofitems_found_on_current_page++;
                        }
                    }
                }
                final Object illustsObject = body.get("illusts");
                final Map<String, Object> illusts = illustsObject instanceof Map ? (Map<String, Object>) illustsObject : null;
                if (illusts != null && !isManga) {
                    for (Map.Entry<String, Object> entry : illusts.entrySet()) {
                        final String galleryID = entry.getKey();
                        if (dups.add(galleryID)) {
                            itemcounter++;
                            final DownloadLink dl = createDownloadlink(PixivNet.createSingleImageUrl(galleryID));
                            if (!StringUtils.isEmpty(uploadDate)) {
                                dl.setProperty(PixivNet.PROPERTY_UPLOADDATE, uploadDate);
                            }
                            fp.add(dl);
                            decryptedLinks.add(dl);
                            distribute(dl);
                            numberofitems_found_on_current_page++;
                        }
                    }
                }
                final Object mangaObject = body.get("manga");
                final Map<String, Object> manga = mangaObject instanceof Map ? (Map<String, Object>) mangaObject : null;
                if (manga != null) {
                    for (Map.Entry<String, Object> entry : manga.entrySet()) {
                        final String galleryID = entry.getKey();
                        if (dups.add(galleryID)) {
                            itemcounter++;
                            final DownloadLink dl = createDownloadlink(PixivNet.createSingleImageUrl(galleryID));
                            fp.add(dl);
                            decryptedLinks.add(dl);
                            distribute(dl);
                            numberofitems_found_on_current_page++;
                        }
                    }
                }
                offset += numberofitems_found_on_current_page;
                page++;
            } while (numberofitems_found_on_current_page >= max_numbeofitems_per_page && maxitemsperpage != -1 && !this.isAbort());
        }
        return decryptedLinks;
    }

    /** Generates DownloadLink for a single picture item. */
    private DownloadLink generateDownloadLink(final String parameter, final String contentID, String title, final String uploadDate, final String username, String tags, final String directurl) {
        if (title == null) {
            title = contentID;
        }
        final String filename_url = new Regex(directurl, "/([^/]+\\.[a-z]+)$").getMatch(0);
        String filename;
        final String picNumberStr = new Regex(directurl, "/[^/]+_p(\\d+)[^/]*\\.[a-z]+$").getMatch(0);
        if (picNumberStr != null) {
            filename = this.generateFilename(contentID, title, username, tags, picNumberStr, null);
        } else {
            /* Fallback - just use the given filename (minus extension)! */
            filename = filename_url.substring(0, filename_url.lastIndexOf("."));
        }
        if (StringUtils.isEmpty(filename)) {
            return null;
        }
        final String ext = getFileNameExtensionFromString(directurl, PixivNet.default_extension);
        if (StringUtils.equalsIgnoreCase(ext, ".zip")) {
            final String resolution = new Regex(directurl, "(\\d+x\\d+)").getMatch(0);
            if (resolution != null) {
                filename += "_" + resolution;
            }
        }
        filename += ext;
        final DownloadLink dl = createDownloadlink(directurl.replaceAll("https?://", "decryptedpixivnet://"));
        dl.setProperty(PixivNet.PROPERTY_MAINLINK, parameter);
        // dl.setProperty(PixivNet.PROPERTY_GALLERYID, userid);
        dl.setProperty(PixivNet.PROPERTY_GALLERYURL, br.getURL());
        if (!StringUtils.isEmpty(uploadDate)) {
            dl.setProperty(PixivNet.PROPERTY_UPLOADDATE, uploadDate);
        }
        if (!StringUtils.isEmpty(username)) {
            /* Packagizer property */
            dl.setProperty(PixivNet.PROPERTY_UPLOADER, username);
        }
        dl.setContentUrl(parameter);
        dl.setFinalFileName(filename);
        dl.setAvailable(true);
        return dl;
    }

    /** Returns filename without extension */
    private String generateFilename(final String galleryID, String title, final String username, String tags, final String picNumberStr, final String extension) {
        if (galleryID == null || picNumberStr == null) {
            return null;
        }
        if (tags != null) {
            tags = tags.trim();
        }
        if (title != null) {
            title = title.trim();
        }
        String thistitle;
        if (tags == null || (title != null && tags.equalsIgnoreCase(title))) {
            thistitle = "";
        } else {
            thistitle = tags;
        }
        if (title != null) {
            thistitle += title;
        }
        if (username != null) {
            thistitle += " - " + username;
        }
        return galleryID + "_p" + picNumberStr + " " + thistitle;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("この作品は削除されました。|>This work was deleted|>Artist has made their work private\\.");
    }

    // private boolean isAdultImageLoginRequired(String lid) {
    // /* 2019-01-18: TODO: Add way to get around r18-block (without account) --> 2020-05-08: Way around is using ajax request */
    // return br.containsHTML("r18=true") || br.containsHTML("\"illustId\"\\s*:\\s*\"" + lid + "\".*?\"xRestrict\"\\s*:\\s*1") ||
    // br.containsHTML("r18-image") || br.containsHTML("\"tag\":\"R-18\",\"locked\":true");
    // }
    public static boolean isAccountOrRightsRequired(final Browser br) {
        return br.getURL().contains("return_to=");
    }

    @Override
    public Class<? extends PixivNetConfig> getConfigInterface() {
        return PixivNetConfig.class;
    }
}
