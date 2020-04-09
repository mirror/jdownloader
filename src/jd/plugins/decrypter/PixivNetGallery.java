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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.hoster.PixivNet;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pixiv.net" }, urls = { "https?://(?:www\\.)?pixiv\\.net/([a-z]{2}/)?(artworks/\\d+|users/\\d+/(?:artworks|illustrations|bookmarks/artworks))" })
public class PixivNetGallery extends PluginForDecrypt {
    public PixivNetGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* TODO: Update these */
    private static final String TYPE_GALLERY        = ".+/member_illust\\.php\\?mode=[a-z]+\\&illust_id=\\d+";
    private static final String TYPE_ARTWORKS       = ".+/artworks/\\d+";
    private static final String TYPE_GALLERY_MEDIUM = ".+/member_illust\\.php\\?mode=medium\\&illust_id=\\d+";
    private static final String TYPE_GALLERY_MANGA  = ".+/member_illust\\.php\\?mode=manga\\&illust_id=\\d+";

    private Integer getPageCount(Browser br, final String lid) {
        // bookmarkData only exists when link is bookmarked
        final String userIllust = br.getRegex("(\\{[^{]*\"illustId\"\\s*:\\s*\"" + lid + "(?:[^{]*\"bookmarkData\"\\s*:\\s*\\{)?[^{]*\\})").getMatch(0);
        final String pageCount = new Regex(userIllust, "\"pageCount\"\\s*:\\s*(\\d+)").getMatch(0);
        if (pageCount != null) {
            return Integer.parseInt(pageCount);
        } else {
            return null;
        }
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final PluginForHost hostplugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostplugin);
        PixivNet.prepBR(br);
        boolean loggedIn = false;
        if (aa != null) {
            try {
                PixivNet.login(this, br, aa, false, false);
                loggedIn = true;
            } catch (PluginException e) {
                handleAccountException(aa, e);
            }
        }
        String userid = new Regex(parameter, "id=(\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        String fpName = null;
        Boolean single = null;
        String uploadDate = null;
        if (parameter.matches(TYPE_GALLERY) || parameter.matches(TYPE_ARTWORKS)) {
            /* 2020-04-28: TODO: Check this with a lot of URLs */
            if (parameter.matches(TYPE_ARTWORKS)) {
                userid = new Regex(parameter, "artworks/(\\d+)").getMatch(0);
                br.getPage(parameter);
                final Integer pageCount = getPageCount(br, userid);
                if (pageCount != null) {
                    single = pageCount.intValue() == 1;
                }
            } else if (parameter.matches(TYPE_GALLERY_MEDIUM)) {
                br.getPage(PixivNet.createSingleImageUrl(userid));
                final Integer pageCount = getPageCount(br, userid);
                if (br.containsHTML("mode=manga&amp;illust_id=" + userid) || (pageCount != null && pageCount.intValue() > 1)) {
                    parameter = PixivNet.createGalleryUrl(userid);
                    br.getPage(parameter);
                    single = Boolean.FALSE;
                } else {
                    single = Boolean.TRUE;
                }
            } else if (parameter.matches(TYPE_GALLERY_MANGA)) {
                br.getPage(PixivNet.createGalleryUrl(userid));
                final Integer pageCount = getPageCount(br, userid);
                if (br.containsHTML("指定されたIDは複数枚投稿ではありません|t a multiple-image submission<") | (pageCount != null && pageCount.intValue() == 1)) {
                    parameter = PixivNet.createSingleImageUrl(userid);
                    br.getPage(parameter);
                    single = Boolean.TRUE;
                } else {
                    single = Boolean.FALSE;
                }
            } else {
                br.getPage(PixivNet.createGalleryUrl(userid));
            }
            if (userid == null) {
                return null;
            }
            uploadDate = PluginJSonUtils.getJson(br, "uploadDate");
            /* Decrypt gallery */
            String json = br.getRegex("id=\"meta-preload-data\" content='(\\{.*?\\})'").getMatch(0);
            /* New attempt 2020-04-08 */
            Map<String, Object> entries = JSonStorage.restoreFromString(json, TypeRef.HASHMAP);
            final Map<String, Object> illust = (Map<String, Object>) entries.get("illust");
            String userName = null;
            String illustUploadDate = null;
            String illustId = null;
            String illustTitle = null;
            String tags = null;
            final Set<Entry<String, Object>> illustSet = illust.entrySet();
            for (Map.Entry<String, Object> entry : illustSet) {
                final Map<String, Object> illustInfo = (Map<String, Object>) entry.getValue();
                illustId = (String) illustInfo.get("illustId");
                illustTitle = (String) illustInfo.get("illustTitle");
                final String singleLink = (String) JavaScriptEngineFactory.walkJson(illustInfo, "urls/regular");
                if (StringUtils.isEmpty(illustId)) {
                    /* Skip invalid items */
                    continue;
                }
                decryptedLinks.add(generateDownloadLink(parameter, illustId, illustTitle, illustUploadDate, userName, tags, singleLink));
            }
            if (illustSet.size() == 1) {
                /* == click on "Load more" */
                br.getPage(String.format("https://www.%s/ajax/illust/%s/pages?lang=en", this.getHost(), userid));
                entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                final ArrayList<Object> additionalpics = (ArrayList<Object>) entries.get("body");
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
                    decryptedLinks.add(generateDownloadLink(parameter, illustId, illustTitle, illustUploadDate, userName, tags, directurl));
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(illustTitle);
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        } else {
            /* Decrypt user */
            if (userid == null) {
                /* 2020-01-27 */
                userid = new Regex(parameter, "users/(\\d+)").getMatch(0);
            }
            if (userid == null) {
                logger.warning("Failed to find userid");
                return null;
            }
            br.getPage(parameter);
            fpName = br.getRegex("<meta property=\"og:title\" content=\"(.*?)(?:\\s*\\[pixiv\\])?\">").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
            uploadDate = PluginJSonUtils.getJson(br, "uploadDate");
            if (isOffline(br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            } else if (isAccountOrRightsRequired(br) && !loggedIn) {
                logger.info("Account required to crawl this particular content");
                throw new PluginException(LinkStatus.ERROR_PREMIUM);
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
            if (parameter.contains("/bookmarks")) {
                url = String.format("https://www.%s/ajax/user/%s/illusts/bookmarks", br.getHost(), userid);
                isBookmarks = true;
            } else {
                /* 2020-04-06: E.g. "/illustrations" */
                url = String.format("https://www.%s/ajax/user/%s/profile/all", br.getHost(), userid);
                /* All items on one page! */
                maxitemsperpage = -1;
            }
            do {
                numberofitems_found_on_current_page = 0;
                final HashSet<String> dups = new HashSet<String>();
                final Browser brc = br.cloneBrowser();
                brc.setLoadLimit(5 * 1024 * 1024);
                final UrlQuery query = new UrlQuery();
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
                if (body.containsKey("works")) {
                    /* E.g. bookmarks */
                    final ArrayList<Object> works = (ArrayList<Object>) body.get("works");
                    for (final Object workO : works) {
                        final Map<String, Object> entries = (Map<String, Object>) workO;
                        final String galleryID = (String) entries.get("illustId");
                        if (dups.add(galleryID)) {
                            itemcounter++;
                            final DownloadLink dl = createDownloadlink(PixivNet.createSingleImageUrl(galleryID));
                            decryptedLinks.add(dl);
                            distribute(dl);
                            numberofitems_found_on_current_page++;
                        }
                    }
                }
                final Map<String, Object> illusts = (Map<String, Object>) body.get("illusts");
                if (illusts != null) {
                    for (Map.Entry<String, Object> entry : illusts.entrySet()) {
                        final String galleryID = entry.getKey();
                        if (dups.add(galleryID)) {
                            itemcounter++;
                            final DownloadLink dl = createDownloadlink(PixivNet.createSingleImageUrl(galleryID));
                            if (!StringUtils.isEmpty(uploadDate)) {
                                dl.setProperty(PixivNet.PROPERTY_UPLOADDATE, uploadDate);
                            }
                            decryptedLinks.add(dl);
                            distribute(dl);
                            numberofitems_found_on_current_page++;
                        }
                    }
                }
                final Map<String, Object> manga = (Map<String, Object>) body.get("manga");
                if (manga != null) {
                    for (Map.Entry<String, Object> entry : manga.entrySet()) {
                        final String galleryID = entry.getKey();
                        if (dups.add(galleryID)) {
                            itemcounter++;
                            final DownloadLink dl = createDownloadlink(PixivNet.createGalleryUrl(galleryID));
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
        if (fpName == null) {
            fpName = userid;
        } else {
            fpName = Encoding.htmlOnlyDecode(fpName);
            fpName = userid + "_" + fpName;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

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
            if (!thistitle.startsWith("「")) {
                thistitle = "「" + thistitle;
            }
            thistitle += tags;
        }
        if (title != null) {
            if (thistitle.isEmpty()) {
                thistitle = "「";
            } else {
                if (!thistitle.startsWith("「")) {
                    thistitle = "「" + thistitle;
                }
                thistitle += " _ ";
            }
            thistitle += title;
        }
        if (username != null) {
            thistitle += " - " + username;
        }
        if (!thistitle.endsWith("」")) {
            thistitle += "」";
        }
        return galleryID + "_p" + picNumberStr + thistitle;
    }

    private boolean add(Set<String> set, Browser br, final String pattern) {
        final String links[] = br.getRegex(pattern).getColumn(0);
        if (links != null && links.length > 0) {
            return set.addAll(Arrays.asList(links));
        } else {
            return false;
        }
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 400 || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("この作品は削除されました。|>This work was deleted|>Artist has made their work private\\.");
    }

    private boolean isAdultImageLoginRequired(String lid) {
        /* 2019-01-18: TODO: Add way to get around r18-block (without account) */
        return br.containsHTML("r18=true") || br.containsHTML("\"illustId\"\\s*:\\s*\"" + lid + "\".*?\"xRestrict\"\\s*:\\s*1") || br.containsHTML("r18-image") || br.containsHTML("\"tag\":\"R-18\",\"locked\":true");
    }

    public static boolean isAccountOrRightsRequired(final Browser br) {
        return br.getURL().contains("return_to=");
    }
}
