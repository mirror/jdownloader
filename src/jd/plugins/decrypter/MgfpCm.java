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
import java.util.HashSet;
import java.util.Set;

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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.ImageFap;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "https?://(?:www\\.)?imagefap\\.com/(gallery\\.php\\?p?gid=.+|gallery/.+|pictures/\\d+/.*|photo/\\d+|organizer/\\d+|(usergallery|showfavorites)\\.php\\?userid=\\d+(&folderid=-?\\d+)?)" })
public class MgfpCm extends PluginForDecrypt {
    public MgfpCm(PluginWrapper wrapper) {
        super(wrapper);
        try {
            // Browser.setRequestIntervalLimitGlobal(getHost(), 600, 100, 60000);
            Browser.setRequestIntervalLimitGlobal(getHost(), 750);
        } catch (final Throwable e) {
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX, LazyPlugin.FEATURE.IMAGE_GALLERY };
    }

    private static final String type_invalid = "(?i)https?://[^/]+/gallery/search=.+";

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /*
         * 2020-09-16: One measure to try not to exceed rate limits:
         * https://www.imagefaq.cc/forum/viewtopic.php?f=4&t=17675&sid=0ed66fda947338862f2cb3d32622e030
         */
        return 1;
    }

    private String getPage(final Browser br, final String url) throws Exception {
        jd.plugins.hoster.ImageFap.getRequest(this, br, br.createGetRequest(url));
        return br.toString();
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        jd.plugins.hoster.ImageFap.prepBR(this.br);
        String parameter = param.toString();
        final Set<String> dupes = new HashSet<String>();
        final String oid = new Regex(parameter, "(?:organizer)/(\\d+)").getMatch(0);
        if (oid != null) {
            /** organizerID link **/
            int pageIndex = 0;
            br.setFollowRedirects(true);
            do {
                getPage(this.br, "https://www." + this.getHost() + "/organizer/" + oid + "/?page=" + (pageIndex > 0 ? Integer.toString(pageIndex) : ""));
                pageIndex++;
                final String galleries[] = br.getRegex("(/gallery/\\d+|/gallery\\.php\\?gid=\\d+)").getColumn(0);
                if (galleries == null || galleries.length == 0) {
                    break;
                } else {
                    for (final String gallery : galleries) {
                        if (dupes.add(gallery)) {
                            final DownloadLink link = createDownloadlink("https://www.imagefap.com" + gallery);
                            ret.add(link);
                        }
                    }
                }
            } while (!this.isAbort());
            return ret;
        }
        final String userID = new Regex(parameter, "userid=(\\d+)").getMatch(0);
        final String folderID = new Regex(parameter, "folderid=(-?\\d+)").getMatch(0);
        if (userID != null && folderID != null) {
            /** user/folderID link **/
            final boolean userGallery = StringUtils.containsIgnoreCase(parameter, "usergallery.php");
            final boolean favoriteGallery = StringUtils.containsIgnoreCase(parameter, "showfavorites.php");
            int pageIndex = 0;
            br.setFollowRedirects(true);
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
                for (final String gallery : galleries) {
                    if (dupes.add(gallery)) {
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
            br.setFollowRedirects(true);
            getPage(this.br, parameter);
            final String galleries[] = br.getRegex("((usergallery|showfavorites)\\.php\\?userid=\\d+&folderid=-?\\d+)").getColumn(0);
            if (galleries != null) {
                for (final String gallery : galleries) {
                    if (dupes.add(gallery)) {
                        final DownloadLink link = createDownloadlink("https://www.imagefap.com/" + gallery);
                        ret.add(link);
                    }
                }
            }
            return ret;
        } else {
            String galleryIDStr = new Regex(parameter, "(?:pictures|gallery)/(\\d+)/?").getMatch(0);
            if (galleryIDStr == null) {
                galleryIDStr = new Regex(parameter, "gallery\\.php\\?p?gid=(\\d+)").getMatch(0);
            }
            final ArrayList<String> allPages = new ArrayList<String>();
            allPages.add("0");
            if (parameter.matches(type_invalid)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (parameter.matches("https?://[^/]+/photo/\\d+")) {
                final String photoID = new Regex(parameter, "(\\d+)$").getMatch(0);
                final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + photoID);
                link.setContentUrl(parameter + "/");
                /* Set Packagizer property */
                link.setProperty("photoID", Long.parseLong(photoID));
                ret.add(link);
            } else {
                parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
                if (new Regex(parameter, "imagefap\\.com/gallery\\.php\\?pgid=").matches()) {
                    /**
                     * Workaround to get all images on one page for private galleries (site buggy)
                     */
                    getPage(this.br, "https://www." + this.getHost() + "/gallery.php?view=2");
                } else if (!parameter.contains("view=2")) {
                    parameter = addParameter(parameter, "view=2");
                    parameter = addParameter(parameter, "gid=" + galleryIDStr);
                }
                getPage(this.br, parameter);
                if (br.containsHTML("(?i)>\\s*This gallery has been flagged")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (br.getRedirectLocation() != null) {
                    if (br.getRedirectLocation().contains("/pictures/")) {
                        parameter = br.getRedirectLocation();
                        parameter = addParameter(parameter, "view=2");
                        logger.info("New parameter is set: " + parameter);
                        getPage(this.br, parameter);
                    } else {
                        logger.warning("Getting unknown redirect page");
                        getPage(this.br, br.getRedirectLocation());
                    }
                }
                if (br.getURL().contains("/404.php") || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*Could not find gallery")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404.php")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                // First find all the information we need (name of the gallery, name of
                // the galleries author)
                String galleryName = ImageFap.getGalleryName(br, null, false);
                if (galleryName == null) {
                    throw new DecrypterException("Decrypter broken for link: " + parameter);
                }
                String authorsName = ImageFap.getUserName(br, null, false);
                galleryName = Encoding.htmlDecode(galleryName.trim());
                authorsName = Encoding.htmlDecode(authorsName.trim());
                /**
                 * Max number of images per page = 1000, if we got more we always have at least 2 pages
                 */
                final String[] pages = br.getRegex("<a class=link3 href=\"\\?(pgid=&(?:amp;)?gid=\\d+&(?:amp;)?page=|gid=\\d+&(?:amp;)?page=)(\\d+)").getColumn(1);
                if (pages != null && pages.length != 0) {
                    for (final String page : pages) {
                        if (!allPages.contains(page)) {
                            allPages.add(page);
                        }
                    }
                }
                int counter = 1;
                final DecimalFormat df = new DecimalFormat("0000");
                final HashSet<String> incompleteOriginalFilenameWorkaround = new HashSet<String>();
                if (galleryIDStr == null) {
                    galleryIDStr = br.getRegex("\"galleryid_input\"\\s*value\\s*=\\s*\"(\\d+)").getMatch(0);
                }
                if (galleryIDStr == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Long galleryID = Long.parseLong(galleryIDStr);
                final FilePackage fp = FilePackage.getInstance();
                if (galleryID != null) {
                    fp.setName(authorsName + " - " + galleryName + " - " + galleryID);
                } else {
                    fp.setName(authorsName + " - " + galleryName);
                }
                final String baseURL = URLHelper.getUrlWithoutParams(br._getURL());
                final UrlQuery query = new UrlQuery();
                query.add("gid", galleryIDStr);
                query.add("view", "0");
                for (final String page : allPages) {
                    if (!page.equals("0")) {
                        query.addAndReplace("page", page);
                        getPage(this.br, baseURL + "?" + query.toString());
                    }
                    final String info[][] = br.getRegex("<span id=\"img_(\\d+)_desc\">.*?<font face=verdana color=\"#000000\"><i>([^<>\"]*?)</i>").getMatches();
                    if (info == null || info.length == 0) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    int numberofNewItems = 0;
                    for (final String elements[] : info) {
                        final String orderID = df.format(counter);
                        final String photoID = elements[0];
                        if (dupes.add(photoID)) {
                            final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + photoID);
                            link._setFilePackage(fp);
                            String original_filename = Encoding.htmlDecode(elements[1].trim());
                            if (!incompleteOriginalFilenameWorkaround.add(original_filename) || original_filename.matches(".*[\\.]{2,}$")) {
                                // some filenames are incomplete and end with ...
                                // this workaround removes ... and adds orderID
                                original_filename = original_filename.replaceFirst("([\\.]{2,})$", "") + "_" + orderID + ".jpg";
                                link.setProperty("incomplete_filename", original_filename);
                            } else {
                                link.setProperty("original_filename", original_filename);
                            }
                            link.setContentUrl("https://www." + this.getHost() + "/photo/" + photoID + "/");
                            link.setProperty("orderid", orderID);
                            /* Set Packagizer property */
                            link.setProperty("photoID", Long.parseLong(photoID));
                            if (galleryID != null) {
                                link.setProperty("galleryID", galleryID);
                            }
                            link.setProperty("galleryname", galleryName);
                            link.setProperty("directusername", authorsName);
                            link.setName(jd.plugins.hoster.ImageFap.getFormattedFilename(link));
                            link.setAvailable(true);
                            ret.add(link);
                            distribute(link);
                            counter++;
                            numberofNewItems++;
                        }
                    }
                    logger.info("Crawled page: " + page + "/" + allPages.size() + " | Found items so far: " + ret.size());
                    if (numberofNewItems == 0) {
                        logger.info("Stopping because: Failed to find new items on current page");
                        break;
                    } else if (isAbort()) {
                        logger.info("Stopping because: Aborted by user");
                        break;
                    }
                }
            }
        }
        return ret;
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

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}