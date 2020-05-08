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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pixiv.net" }, urls = { "https?://(?:www\\.)?pixiv\\.net/([a-z]{2}/)?(artworks/\\d+|member_illust\\.php\\?mode=[a-z0-9]+\\&illust_id=\\d+|users/\\d+(/(?:artworks|illustrations|manga|bookmarks/artworks))?)" })
public class PixivNetGallery extends PluginForDecrypt {
    public PixivNetGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_GALLERY = ".+/(?:member_illust\\.php\\?mode=[a-z]+\\&illust_id=|artworks/)(\\d+)";

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
        String itemID = new Regex(parameter, "id=(\\d+)").getMatch(0);
        br.setFollowRedirects(true);
        String fpName = null;
        String uploadDate = null;
        if (parameter.matches(TYPE_GALLERY)) {
            itemID = new Regex(parameter, TYPE_GALLERY).getMatch(0);
            if (itemID == null) {
                return null;
            }
            br.getPage("https://www." + this.getHost() + "/ajax/illust/" + itemID);
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* {"error":true,"message":"Work has been deleted or the ID does not exist.","body":[]} */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            entries = (Map<String, Object>) entries.get("body");
            /* 2020-05-08: 0 = image, 1 = ??, 2 = animation (?) */
            final long illustType = JavaScriptEngineFactory.toLong(entries.get("illustType"), 0);
            if (illustType == 2) {
                logger.info("Found animation (?)");
            }
            uploadDate = (String) entries.get("uploadDate");
            int pagecount = (int) JavaScriptEngineFactory.toLong(entries.get("pageCount"), 1);
            String userName = null;
            String illustUploadDate = null;
            String illustTitle = null;
            String tags = null;
            // final String illustId = (String) entries.get("illustId");
            illustTitle = (String) entries.get("illustTitle");
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
            decryptedLinks.add(generateDownloadLink(parameter, itemID, illustTitle, illustUploadDate, userName, tags, singleLink));
            if (pagecount > 1) {
                /* == click on "Load more" */
                br.getPage(String.format("https://www.%s/ajax/illust/%s/pages?lang=en", this.getHost(), itemID));
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
                    decryptedLinks.add(generateDownloadLink(parameter, itemID, illustTitle, illustUploadDate, userName, tags, directurl));
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(itemID + " " + illustTitle);
                fp.addLinks(decryptedLinks);
            }
            return decryptedLinks;
        } else {
            /* Decrypt user */
            if (itemID == null) {
                /* 2020-01-27 */
                itemID = new Regex(parameter, "users/(\\d+)").getMatch(0);
            }
            if (itemID == null) {
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
            boolean isManga = false;
            final UrlQuery querybase = new UrlQuery();
            if (parameter.contains("/bookmarks")) {
                url = String.format("https://www.%s/ajax/user/%s/illusts/bookmarks", br.getHost(), itemID);
                isBookmarks = true;
            } else if (parameter.contains("/manga")) {
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
                    ArrayList<Object> works = null;
                    if (worksO instanceof Map) {
                        works = new ArrayList<Object>();
                        works.add(worksO);
                    } else {
                        works = (ArrayList<Object>) worksO;
                    }
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
                if (illusts != null && !isManga) {
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
                            final DownloadLink dl = createDownloadlink(PixivNet.createSingleImageUrl(galleryID));
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
            fpName = itemID;
        } else {
            fpName = Encoding.htmlOnlyDecode(fpName);
            fpName = itemID + "_" + fpName;
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
        }
        if (title != null) {
            thistitle += title;
        }
        if (username != null) {
            thistitle += " - " + username;
        }
        return galleryID + "_p" + picNumberStr + " " + thistitle;
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

    // private boolean isAdultImageLoginRequired(String lid) {
    // /* 2019-01-18: TODO: Add way to get around r18-block (without account) --> 2020-05-08: Way around is using ajax request */
    // return br.containsHTML("r18=true") || br.containsHTML("\"illustId\"\\s*:\\s*\"" + lid + "\".*?\"xRestrict\"\\s*:\\s*1") ||
    // br.containsHTML("r18-image") || br.containsHTML("\"tag\":\"R-18\",\"locked\":true");
    // }
    public static boolean isAccountOrRightsRequired(final Browser br) {
        return br.getURL().contains("return_to=");
    }
}
