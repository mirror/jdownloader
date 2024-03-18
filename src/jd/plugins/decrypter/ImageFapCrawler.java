//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.URLHelper;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.ImageFap;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "https?://(?:www\\.)?imagefap\\.com/(gallery\\.php\\?p?gid=.+|gallery/.+|pictures/\\d+/.*|photo/\\d+|organizer/\\d+|(usergallery|showfavorites)\\.php\\?userid=\\d+(&folderid=-?\\d+)?)" })
public class ImageFapCrawler extends PluginForDecrypt {
    public ImageFapCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public void init() {
        ImageFap.setRequestIntervalLimitGlobal();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private static final String type_invalid = "(?i)https?://[^/]+/gallery/search=.+";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /*
         * 2020-09-16: One measure to try not to exceed rate limits: https://www.imagefap.com/forum/viewtopic.php?f=4&t=17675
         */
        return 1;
    }

    private String getPage(final Browser br, final String url) throws Exception {
        ImageFap.getRequest(this, br, br.createGetRequest(url));
        return br.getRequest().getHtmlCode();
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ImageFap.prepBR(this.br);
        String contenturl = param.getCryptedUrl();
        final Map<String, String> dupes = new HashMap<String, String>();
        final PluginForHost hosterplugin = this.getNewPluginForHostInstance(getHost());
        final String oid = new Regex(contenturl, "(?:organizer)/(\\d+)").getMatch(0);
        if (oid != null) {
            /** organizerID link **/
            int pageIndex = 0;
            do {
                getPage(br, "https://www." + this.getHost() + "/organizer/" + oid + "/?page=" + (pageIndex > 0 ? Integer.toString(pageIndex) : ""));
                pageIndex++;
                final String galleries[] = br.getRegex("(/gallery/\\d+|/gallery\\.php\\?gid=\\d+)").getColumn(0);
                if (galleries == null || galleries.length == 0) {
                    break;
                } else {
                    final String pageURL = br.getURL();
                    for (final String gallery : galleries) {
                        final String dupePage = dupes.put(gallery, pageURL);
                        if (dupePage != null) {
                            logger.info("Skipping dupe:" + gallery + "\t" + dupePage + "\t" + pageURL);
                        } else {
                            final DownloadLink link = createDownloadlink("https://www.imagefap.com" + gallery);
                            ret.add(link);
                        }
                    }
                }
            } while (!this.isAbort());
            return ret;
        }
        final String userID = new Regex(contenturl, "(?i)userid=(\\d+)").getMatch(0);
        final String folderID = new Regex(contenturl, "(?i)folderid=(-?\\d+)").getMatch(0);
        if (userID != null && folderID != null) {
            /** user/folderID link **/
            final boolean userGallery = StringUtils.containsIgnoreCase(contenturl, "usergallery.php");
            final boolean favoriteGallery = StringUtils.containsIgnoreCase(contenturl, "showfavorites.php");
            int pageIndex = 0;
            do {
                if (userGallery) {
                    getPage(this.br, "https://www." + this.getHost() + "/usergallery.php?userid=" + userID + "&folderid=" + folderID + "&page=" + (pageIndex > 0 ? Integer.toString(pageIndex) : ""));
                } else if (favoriteGallery) {
                    getPage(this.br, "https://www." + this.getHost() + "/showfavorites.php?userid=" + userID + "&folderid=" + folderID + "&page=" + (pageIndex > 0 ? Integer.toString(pageIndex) : ""));
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                logger.info("Crawled page " + pageIndex + " | Found items so far: " + ret.size());
                pageIndex++;
                final String galleries[] = br.getRegex("(/gallery/\\d+|/gallery\\.php\\?gid=\\d+)").getColumn(0);
                if (galleries == null || galleries.length == 0) {
                    logger.info("Stopping because: Failed to find more items on current page");
                    break;
                }
                final String pageURL = br.getURL();
                for (final String gallery : galleries) {
                    final String dupePage = dupes.put(gallery, pageURL);
                    if (dupePage == null) {
                        final DownloadLink link = createDownloadlink("https://www.imagefap.com" + gallery);
                        ret.add(link);
                        distribute(link);
                    }
                }
                if (this.isAbort()) {
                    logger.info("Stopping because: Aborted by user");
                    break;
                }
            } while (true);
            return ret;
        } else if (userID != null) {
            getPage(this.br, contenturl);
            final String galleries[] = br.getRegex("((usergallery|showfavorites)\\.php\\?userid=\\d+&folderid=-?\\d+)").getColumn(0);
            if (galleries != null) {
                final String pageURL = br.getURL();
                for (final String gallery : galleries) {
                    final String dupePage = dupes.put(gallery, pageURL);
                    if (dupePage == null) {
                        final DownloadLink link = createDownloadlink("https://www.imagefap.com/" + gallery);
                        ret.add(link);
                    }
                }
            }
            return ret;
        }
        if (contenturl.matches(type_invalid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex singlephoto = new Regex(contenturl, "(?i)https?://[^/]+/photo/(\\d+)");
        if (singlephoto.patternFind()) {
            /* Single photo */
            final String photoID = singlephoto.getMatch(0);
            final DownloadLink link = new DownloadLink(hosterplugin, this.getHost(), contenturl + "/");
            link.setProperty(ImageFap.PROPERTY_PHOTO_ID, photoID);
            ret.add(link);
            return ret;
        }
        /* Gallery */
        /* view=2 -> "One page" view -> More images on each page */
        final UrlQuery query = UrlQuery.parse(contenturl);
        String galleryIDStr = new Regex(contenturl, "(?:pictures|gallery)/(\\d+)/?").getMatch(0);
        if (galleryIDStr == null) {
            galleryIDStr = query.get("gid");
        }
        contenturl = URLHelper.getUrlWithoutParams(contenturl);
        /* Change to "One Page View" */
        query.addAndReplace("view", "2");
        /* Overwrite possibly given page parameter. This is important! */
        query.addAndReplace("page", "0");
        /* Build new contenturl with changed/corrected parameters. */
        contenturl = contenturl + "?" + query.toString();
        getPage(this.br, contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*This gallery has been flagged")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("/404.php")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*Could not find gallery")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // First find all the information we need (name of the gallery, name of
        // the galleries author)
        String galleryName = ImageFap.getGalleryName(br, null, false);
        if (galleryName == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String authorsName = ImageFap.getUserName(br, null, false);
        galleryName = Encoding.htmlDecode(galleryName).trim();
        authorsName = Encoding.htmlDecode(authorsName).trim();
        if (galleryIDStr == null) {
            galleryIDStr = br.getRegex("\"galleryid_input\"\\s*value\\s*=\\s*\"(\\d+)").getMatch(0);
        }
        if (galleryIDStr == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        query.addAndReplace("gid", galleryIDStr);
        int counter = 1;
        final DecimalFormat df = new DecimalFormat("0000");
        final HashSet<String> incompleteOriginalFilenameWorkaround = new HashSet<String>();
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(authorsName + " - " + galleryName + " - " + galleryIDStr);
        final String baseURL = URLHelper.getUrlWithoutParams(br._getURL());
        int maxPage = this.findMaxPage(br);
        int page = 0;
        while (!this.isAbort()) {
            final String info[][] = br.getRegex("<span id=\"img_(\\d+)_desc\">.*?<font face=verdana color=\"#000000\"><i>([^<>\"]*?)</i>").getMatches();
            if (info == null || info.length == 0) {
                if (ret.size() > 0) {
                    logger.info("Stopping because: Current page contains no items -> Possibly buggy website with error 'Gallery not found' on last page");
                    break;
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            int numberofNewItems = 0;
            final String pageURL = br.getURL();
            for (final String elements[] : info) {
                final String orderID = df.format(counter);
                final String photoID = elements[0];
                final String dupePage = dupes.put(photoID, pageURL);
                if (dupePage == null) {
                    final DownloadLink link = new DownloadLink(hosterplugin, this.getHost(), generateSinglePhotoURL(photoID));
                    link._setFilePackage(fp);
                    String original_filename = Encoding.htmlDecode(elements[1]).trim();
                    if (!incompleteOriginalFilenameWorkaround.add(original_filename) || original_filename.matches(".*[\\.]{2,}$")) {
                        // some filenames are incomplete and end with ...
                        // this workaround removes ... and adds orderID
                        original_filename = original_filename.replaceFirst("([\\.]{2,})$", "") + "_" + orderID + ".jpg";
                        link.setProperty(ImageFap.PROPERTY_INCOMPLETE_FILENAME, original_filename);
                    } else {
                        link.setProperty(ImageFap.PROPERTY_ORIGINAL_FILENAME, original_filename);
                    }
                    link.setProperty(ImageFap.PROPERTY_ORDER_ID, orderID);
                    link.setProperty(ImageFap.PROPERTY_PHOTO_ID, photoID);
                    link.setProperty(ImageFap.PROPERTY_ALBUM_ID, galleryIDStr);
                    link.setProperty(ImageFap.PROPERTY_PHOTO_PAGE_NUMBER, page);
                    link.setProperty(ImageFap.PROPERTY_PHOTO_GALLERY_TITLE, galleryName);
                    link.setProperty(ImageFap.PROPERTY_USERNAME, authorsName);
                    link.setProperty(ImageFap.PROPERTY_PHOTO_INDEX, (counter - 1));
                    link.setName(ImageFap.getFormattedFilename(link));
                    link.setAvailable(true);
                    ret.add(link);
                    distribute(link);
                    counter++;
                    numberofNewItems++;
                }
            }
            if (page == maxPage) {
                /**
                 * Find new max page value if it looks like we're currently on the last page. </br>
                 * E.g. if we are on page one, highest page number we can see is 10 even though the item may have 20+ pages.
                 */
                final int maxPageValueOfCurrentPage = findMaxPage(br);
                if (maxPageValueOfCurrentPage > maxPage) {
                    logger.info("Found new maxPage value: old: " + maxPage + " --> New: " + maxPageValueOfCurrentPage);
                    maxPage = maxPageValueOfCurrentPage;
                }
            }
            logger.info("Crawled page: " + page + "/" + maxPage + " | Found items so far: " + ret.size());
            if (page == maxPage) {
                logger.info("Stopping because: Reached last page: " + maxPage);
                break;
            } else if (numberofNewItems == 0) {
                /* Additional fail-safe */
                logger.info("Stopping because: Failed to find new items on current page");
                break;
            } else if (isAbort()) {
                logger.info("Stopping because: Aborted by user");
                break;
            } else {
                /* Continue to next page */
                page++;
                query.addAndReplace("page", Integer.toString(page));
                getPage(this.br, baseURL + "?" + query.toString());
            }
        }
        return ret;
    }

    private String generateSinglePhotoURL(final String photoID) {
        return "https://www." + this.getHost() + "/photo/" + photoID + "/";
    }

    private int findMaxPage(final Browser br) {
        int maxPage = 0;
        final String[] pages = br.getRegex("(?i)page=(\\d+)").getColumn(0);
        for (final String pageStr : pages) {
            final int pageInt = Integer.parseInt(pageStr);
            if (pageInt > maxPage) {
                maxPage = pageInt;
            }
        }
        return maxPage;
    }

    private String addParameter(String url, final String data) {
        if (!url.contains(data)) {
            if (url.contains("?")) {
                url += "&" + data;
            } else {
                url += "?" + data;
            }
        }
        return url;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}