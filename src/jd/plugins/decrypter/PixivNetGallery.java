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
import java.util.Set;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pixiv.net" }, urls = { "https?://(?:www\\.)?pixiv\\.net/.+" })
public class PixivNetGallery extends PluginForDecrypt {
    public PixivNetGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

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
        String title = null;
        String username = null;
        Boolean single = null;
        String uploadDate = null;
        if (parameter.matches(TYPE_GALLERY) || parameter.matches(TYPE_ARTWORKS)) {
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
            uploadDate = PluginJSonUtils.getJson(br, "uploadDate");
            /* Decrypt gallery */
            final Set<String> links = new HashSet<String>();
            String json = null;
            if (Boolean.TRUE.equals(single) || (single == null && br.containsHTML("指定されたIDは複数枚投稿ではありません|t a multiple-image submission<"))) {
                /* Not multiple urls --> Switch to single-url view */
                if (single == null) {
                    br.getPage(PixivNet.createSingleImageUrl(userid));
                }
                title = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)(?:\\[pixiv\\])?\">").getMatch(0);
                if (title == null) {
                    title = br.getRegex("<title>(.*?)</title>").getMatch(0);
                }
                boolean found = add(links, br, "data-illust-id=\"\\d+\"><img src=\"(https?[^<>\"']+)\"");
                if (!found) {
                    // old layout
                    found = add(links, br, "data-title=\"registerImage\"><img src=\"(https?[^<>\"']+)\"");
                    if (!found) {
                        // regular(new layout)
                        found = add(links, br, "\"illustId\"\\s*:\\s*\"" + userid + "\".*?\"regular\"\\s*:\\s*\"(https?[^<>\"']+)\"");
                    }
                }
                if (links.isEmpty()) {
                    found = add(links, br, "data-src=\"(https?[^<>\"]+)\"[^>]+class=\"original-image\"");
                    if (!found) {
                        // original(new layout)
                        found = add(links, br, "\"illustId\"\\s*:\\s*\"" + userid + "\".*?\"original\"\\s*:\\s*\"(https?[^<>\"']+)\"");
                    }
                }
                if (links.isEmpty()) {
                    add(links, br, "pixiv\\.context\\.ugokuIllustFullscreenData\\s*=\\s*\\{\\s*\"src\"\\s*:\\s*\"(https?.*?)\"");
                }
                if (links.isEmpty()) {
                    add(links, br, "pixiv\\.context\\.ugokuIllustData\\s*=\\s*\\{\\s*\"src\"\\s*:\\s*\"(https?.*?)\"");
                }
                json = br.getRegex("(\\{[^{]*\"illustId\"\\s*:\\s*\"" + userid + "[^{]*\\})").getMatch(0);
                if (json != null && json.matches(".*\"illustType\"\\s*:\\s*2.*")) {
                    final Browser brc = br.cloneBrowser();
                    brc.getPage("https://www.pixiv.net/ajax/illust/" + userid + "/ugoira_meta");
                    add(links, brc, "(https?.*?)\"");
                }
                if (links.isEmpty() && isOffline(br)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                } else if (links.isEmpty() && isAdultImageLoginRequired(userid) && !loggedIn) {
                    logger.info("Adult content: Account required");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM);
                } else if (links.isEmpty()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                /* Multiple urls */
                /* Check for offline */
                if (isOffline(br)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                } else if (isAccountOrRightsRequired(br) && !loggedIn) {
                    logger.info("Account required to crawl this particular content");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM);
                } else if (isAdultImageLoginRequired(userid) && !loggedIn) {
                    logger.info("Adult content: Account required");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM);
                }
                title = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)(?:\\[pixiv\\])?\">").getMatch(0);
                if (title == null) {
                    title = br.getRegex("<title>(.*?)</title>").getMatch(0);
                }
                // old layout
                boolean found = add(links, br, "pixiv\\.context\\.images\\[\\d+\\]\\s*=\\s*\"(https?[^\"]+)\"");
                if (!found) {
                    // original(new layout)
                    found = add(links, br, "\"illustId\"\\s*:\\s*\"" + userid + "\".*?\"original\"\\s*:\\s*\"(https?[^<>\"']+)\"");
                    if (!found) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        final Integer pageCount = getPageCount(br, userid);
                        if (pageCount != null && links.size() != pageCount.intValue()) {
                            final String template = links.iterator().next();
                            for (int page = 0; page < pageCount.intValue(); page++) {
                                links.add(template.replaceAll("(_p0\\.)", "_p" + page + "."));
                            }
                        }
                    }
                }
                json = br.getRegex("(\\{[^{]*\"illustId\"\\s*:\\s*\"" + userid + "[^{]*\\})").getMatch(0);
            }
            final String tags = br.getRegex("<title>([^<>\"]+)\\s*/[^<>]*?</title>").getMatch(0);
            if (json != null) {
                username = PluginJSonUtils.getJson(json, "userName");
                final String illustTitle = PluginJSonUtils.getJson(json, "illustTitle");
                if (!StringUtils.isEmpty(illustTitle)) {
                    /* Prefer this over title grabbed from html */
                    title = illustTitle;
                }
            }
            if (title != null) {
                title = Encoding.htmlDecode(title).trim();
            } else {
                /* Fallback */
                title = userid;
            }
            if (title != null) {
                fpName = title;
                if (!fpName.startsWith("「")) {
                    fpName = "「" + fpName;
                }
                if (!fpName.endsWith("」")) {
                    fpName += "」";
                }
            }
            for (String singleLink : links) {
                singleLink = singleLink.replaceAll("\\\\", "");
                final String filename_url = new Regex(singleLink, "/([^/]+\\.[a-z]+)$").getMatch(0);
                String filename;
                final String picNumberStr = new Regex(singleLink, "/[^/]+_p(\\d+)[^/]*\\.[a-z]+$").getMatch(0);
                if (picNumberStr != null) {
                    filename = this.generateFilename(userid, title, username, tags, picNumberStr, null);
                } else {
                    /* Fallback - just use the given filename (minus extension)! */
                    filename = filename_url.substring(0, filename_url.lastIndexOf("."));
                }
                if (StringUtils.isEmpty(filename)) {
                    return null;
                }
                final String ext = getFileNameExtensionFromString(singleLink, PixivNet.default_extension);
                if (StringUtils.equalsIgnoreCase(ext, ".zip")) {
                    final String resolution = new Regex(singleLink, "(\\d+x\\d+)").getMatch(0);
                    if (resolution != null) {
                        filename += "_" + resolution;
                    }
                }
                filename += ext;
                final DownloadLink dl = createDownloadlink(singleLink.replaceAll("https?://", "decryptedpixivnet://"));
                dl.setProperty(PixivNet.PROPERTY_MAINLINK, parameter);
                dl.setProperty(PixivNet.PROPERTY_GALLERYID, userid);
                dl.setProperty(PixivNet.PROPERTY_GALLERYURL, br.getURL());
                if (!StringUtils.isEmpty(uploadDate)) {
                    dl.setProperty(PixivNet.PROPERTY_UPLOADDATE, uploadDate);
                }
                dl.setContentUrl(parameter);
                dl.setFinalFileName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
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
                if (page == 0 && !isBookmarks) {
                    brc.getPage(url);
                } else {
                    brc.getPage(url + String.format("?tag=&offset=%d&limit=%d&rest=show", offset, maxitemsperpage));
                }
                final java.util.Map<String, Object> map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                if (map == null) {
                    break;
                }
                final java.util.Map<String, Object> body = (Map<String, Object>) map.get("body");
                if (body == null) {
                    break;
                }
                if (body.containsKey("works")) {
                    /* E.g. bookmarks */
                    final ArrayList<Object> works = (ArrayList<Object>) body.get("works");
                    for (final Object workO : works) {
                        final java.util.Map<String, Object> entries = (Map<String, Object>) workO;
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
                final java.util.Map<String, Object> illusts = (Map<String, Object>) body.get("illusts");
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
                final java.util.Map<String, Object> manga = (Map<String, Object>) body.get("manga");
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

    /** Returns filename without extension */
    private String generateFilename(final String galleryID, final String title, final String username, final String tags, final String picNumberStr, final String extension) {
        if (galleryID == null || picNumberStr == null) {
            return null;
        }
        String thistitle;
        if (tags == null) {
            /* Should never happen */
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
