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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagefap.com" }, urls = { "https?://(www\\.)?imagefap\\.com/(gallery\\.php\\?p?gid=.+|gallery/.+|pictures/\\d+/.*|photo/\\d+|organizer/\\d+|(usergallery|showfavorites)\\.php\\?userid=\\d+(&folderid=-?\\d+)?)" })
public class MgfpCm extends PluginForDecrypt {
    public MgfpCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_invalid = "https?://(www\\.)?imagefap\\.com/gallery/search=.+";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        final String oid = new Regex(parameter, "(?:organizer)/(\\d+)").getMatch(0);
        if (oid != null) {
            /** organizerID link **/
            int pageIndex = 0;
            br.setFollowRedirects(true);
            while (true) {
                br.getPage("https://www.imagefap.com/organizer/" + oid + "/?page=" + (pageIndex > 0 ? Integer.toString(pageIndex) : ""));
                pageIndex++;
                final String galleries[] = br.getRegex("(/gallery/\\d+|/gallery\\.php\\?gid=\\d+)").getColumn(0);
                if (galleries == null || galleries.length == 0) {
                    break;
                }
                for (final String gallery : galleries) {
                    final DownloadLink link = createDownloadlink("https://www.imagefap.com" + gallery);
                    decryptedLinks.add(link);
                }
            }
            return decryptedLinks;
        }
        final String userID = new Regex(parameter, "userid=(\\d+)").getMatch(0);
        final String folderID = new Regex(parameter, "folderid=(-?\\d+)").getMatch(0);
        if (userID != null && folderID != null) {
            /** user/folderID link **/
            final boolean userGallery = StringUtils.containsIgnoreCase(parameter, "usergallery.php");
            final boolean favoriteGallery = StringUtils.containsIgnoreCase(parameter, "showfavorites.php");
            int pageIndex = 0;
            br.setFollowRedirects(true);
            while (true) {
                if (userGallery) {
                    br.getPage("https://www.imagefap.com/usergallery.php?userid=" + userID + "&folderid=" + folderID + "&page=" + (pageIndex > 0 ? Integer.toString(pageIndex) : ""));
                } else if (favoriteGallery) {
                    br.getPage("https://www.imagefap.com/showfavorites.php?userid=" + userID + "&folderid=" + folderID + "&page=" + (pageIndex > 0 ? Integer.toString(pageIndex) : ""));
                } else {
                    return null;
                }
                pageIndex++;
                final String galleries[] = br.getRegex("(/gallery/\\d+|/gallery\\.php\\?gid=\\d+)").getColumn(0);
                if (galleries == null || galleries.length == 0) {
                    break;
                }
                for (final String gallery : galleries) {
                    final DownloadLink link = createDownloadlink("https://www.imagefap.com" + gallery);
                    decryptedLinks.add(link);
                }
            }
            return decryptedLinks;
        } else if (userID != null) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            final String galleries[] = br.getRegex("((usergallery|showfavorites)\\.php\\?userid=\\d+&folderid=-?\\d+)").getColumn(0);
            if (galleries != null) {
                for (final String gallery : galleries) {
                    final DownloadLink link = createDownloadlink("https://www.imagefap.com/" + gallery);
                    decryptedLinks.add(link);
                }
            }
            return decryptedLinks;
        }
        String gid = new Regex(parameter, "(?:pictures|gallery)/(\\d+)/?").getMatch(0);
        if (gid == null) {
            gid = new Regex(parameter, "gallery\\.php\\?p?gid=(\\d+)").getMatch(0);
        }
        final ArrayList<String> allPages = new ArrayList<String>();
        allPages.add("0");
        if (parameter.matches(type_invalid)) {
            final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + new Random().nextInt(1000000));
            link.setFinalFileName(new Regex(parameter, "imagefap\\.com/(.+)").getMatch(0));
            link.setAvailable(false);
            link.setProperty("offline", true);
            decryptedLinks.add(link);
            return decryptedLinks;
        }
        if (parameter.matches("https?://(www\\.)?imagefap\\.com/photo/\\d+")) {
            final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + new Regex(parameter, "(\\d+)$").getMatch(0));
            try {
                link.setContentUrl(parameter + "/");
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            decryptedLinks.add(link);
        } else {
            parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
            if (new Regex(parameter, "imagefap\\.com/gallery\\.php\\?pgid=").matches()) {
                /**
                 * Workaround to get all images on one page for private galleries (site buggy)
                 */
                br.getPage("https://www.imagefap.com/gallery.php?view=2");
            } else if (!parameter.contains("view=2")) {
                parameter = addParameter(parameter, "view=2");
                parameter = addParameter(parameter, "gid=" + gid);
            }
            try {
                br.getPage(parameter);
            } catch (final BrowserException e) {
                final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + new Random().nextInt(1000000));
                link.setFinalFileName(new Regex(parameter, "imagefap\\.com/(.+)").getMatch(0));
                link.setAvailable(false);
                link.setProperty("offline", true);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            if (br.containsHTML(">This gallery has been flagged")) {
                final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + new Random().nextInt(1000000));
                link.setFinalFileName(new Regex(parameter, "imagefap\\.com/(.+)").getMatch(0));
                link.setAvailable(false);
                link.setProperty("offline", true);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            if (br.getRedirectLocation() != null) {
                if (br.getRedirectLocation().contains("/pictures/")) {
                    parameter = br.getRedirectLocation();
                    parameter = addParameter(parameter, "view=2");
                    logger.info("New parameter is set: " + parameter);
                    br.getPage(parameter);
                } else {
                    logger.warning("Getting unknown redirect page");
                    br.getPage(br.getRedirectLocation());
                }
            }
            if (br.getURL().contains("imagefap.com/404.php") || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Could not find gallery<")) {
                final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + new Random().nextInt(1000000));
                link.setFinalFileName(new Regex(parameter, "imagefap\\.com/(.+)").getMatch(0));
                link.setAvailable(false);
                link.setProperty("offline", true);
                decryptedLinks.add(link);
                return decryptedLinks;
            }
            // First find all the information we need (name of the gallery, name of
            // the galleries author)
            String galleryName = br.getRegex("<title>Porn pics of (.*?) \\(Page 1\\)</title>").getMatch(0);
            if (galleryName == null) {
                galleryName = br.getRegex("<font face=\"verdana\" color=\"white\" size=\"4\"><b>(.*?)</b></font>").getMatch(0);
                if (galleryName == null) {
                    galleryName = br.getRegex("<meta name=\"description\" content=\"Airplanes porn pics - Imagefap\\.com\\. The ultimate social porn pics site\" />").getMatch(0);
                }
                if (galleryName == null) {
                    galleryName = br.getRegex("<font[^<>]*?itemprop=\"name\"[^<>]*?>([^<>]+)<").getMatch(0);
                }
            }
            String authorsName = br.getRegex("<b><font size=\"3\" color=\"#CC0000\">Uploaded by ([^<>\"]+)</font></b>").getMatch(0);
            if (authorsName == null) {
                authorsName = br.getRegex("<td class=\"mnu0\"><a href=\"https?://(www\\.)?imagefap\\.com/profile\\.php\\?user=([^<>\"]+)\"").getMatch(0);
            }
            if (galleryName == null) {
                logger.warning("Gallery name could not be found!");
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            if (authorsName == null) {
                authorsName = "Anonymous";
            }
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
            DecimalFormat df = new DecimalFormat("0000");
            for (final String page : allPages) {
                if (!page.equals("0")) {
                    br.getPage(parameter + "&page=" + page);
                }
                final String info[][] = br.getRegex("<span id=\"img_(\\d+)_desc\">.*?<font face=verdana color=\"#000000\"><i>([^<>\"]*?)</i>").getMatches();
                if (info == null || info.length == 0) {
                    throw new DecrypterException("Decrypter broken for link: " + parameter);
                }
                for (final String elements[] : info) {
                    final String orderID = df.format(counter);
                    final String fid = elements[0];
                    final String original_filename = Encoding.htmlDecode(elements[1].trim());
                    final DownloadLink link = createDownloadlink("https://imagefap.com/imagedecrypted/" + fid);
                    link.setContentUrl("https://www.imagefap.com/photo/" + fid + "/");
                    link.setProperty("orderid", orderID);
                    link.setProperty("galleryname", galleryName);
                    link.setProperty("directusername", authorsName);
                    link.setProperty("original_filename", original_filename);
                    link.setName(jd.plugins.hoster.ImageFap.getFormattedFilename(link));
                    link.setAvailable(true);
                    decryptedLinks.add(link);
                    counter++;
                }
            }
            // Finally set the packagename even if its set again in the linkgrabber
            // available check of the imagefap hosterplugin
            final String galleryID = br.getRegex("\"galleryid_input\"\\s*value\\s*=\\s*\"(\\d+)").getMatch(0);
            FilePackage fp = FilePackage.getInstance();
            if (galleryID != null) {
                fp.setName(authorsName + " - " + galleryName + " - " + galleryID);
            } else {
                fp.setName(authorsName + " - " + galleryName);
            }
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String addParameter(String input, final String data) {
        if (!input.contains(data)) {
            if (input.contains("?")) {
                input += "&" + data;
            } else {
                input += "?" + data;
            }
        }
        return input;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}