//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Arrays;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "unixmanga.com" }, urls = { "http://(www\\.)?unixmanga\\.(?:com|nl|net)/onlinereading/[^\\?]*?/.*?(c|ch)\\d+.*" })
public class UnixMangaCom extends antiDDoSForDecrypt {

    public UnixMangaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DEV NOTES
    // other: The index page seems only only list the first 30 pages/images
    // links, so we need to jump to the first image and catch the last page
    // within reader navigation panel. -raztoki
    // other: disregard server images (advertising/notices), so there will be
    // less images saved vs the next page count. -raztoki

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> tempList = new ArrayList<String>();
        br.setFollowRedirects(true);
        String url = parameter.toString();
        getPage(url);
        if (!br.containsHTML(">Select A Link To Start Reading<")) {
            logger.info("Unsupported link: " + parameter);
            return decryptedLinks;
        }

        // We get the title
        final String title = br.getRegex("<TITLE>READ(?:-->\\.\\.)?\\s*([^<>\"]*?)\\s*(?:\\.\\.<--ONLINE ACCESS|ONLINE)</TITLE>").getMatch(0);
        if (title == null) {
            logger.warning("Title not found! : " + parameter);
            return null;
        }
        // We load each page and retrieve the URL of the picture
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);

        {
            // Grab all available images. onlinereading/ only shows the first x images.
            final String[] allImages = br.getRegex("<A class=\"td2\" rel=\"nofollow\" HREF=\"(https?://[^<>\"]+)").getColumn(0);
            if (allImages == null || allImages.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            tempList.addAll(Arrays.asList(allImages));
        }
        // opening this url will show total pages.
        getPage(tempList.get(tempList.size() - 1));
        // We look for next page and load it ready for the 'for' loop.
        final String currentPage = br.getRegex("<a class=\"current\">(\\d+)</a>").getMatch(0);
        if (currentPage != null) {
            int i = Integer.parseInt(currentPage);
            String result = null;
            do {
                result = br.getRegex("<a href=\"([^\"]+)\">" + (++i) + "</a>").getMatch(0);
                if (result != null) {
                    result = Request.getLocation(result, br.getRequest());
                    tempList.add(result);
                } else {
                    getPage(tempList.get(tempList.size() - 1));
                    // already been added.
                    result = br.getRegex("<a href=\"([^\"]+)\">" + i + "</a>").getMatch(0);
                    if (result != null) {
                        result = Request.getLocation(result, br.getRequest());
                        tempList.add(result);
                    }
                }
            } while (result != null);
        }

        // decrypt all pages, it only
        final DecimalFormat df = new DecimalFormat(tempList.size() < 100 ? "00" : "000");
        boolean containsCredits = false;
        int counter = 1;
        final String imghost = "http://nas.unixmanga.net/onlinereading/";
        for (String img : tempList) {
            if (!containsCredits && counter == 1 && StringUtils.containsIgnoreCase(img, "credits")) {
                counter = 0;
                containsCredits = true;
            }
            img = img.replaceFirst("(?:&|\\?)server=.+", "").replaceFirst("https?://.*?/\\?image=", imghost);
            // remove unnecessary images from saving
            if (!img.contains("xxxhomeunixxxx.png") && !img.endsWith("homeunix.png")) {
                final String extension = getFileNameExtensionFromString(img, ".jpg");
                final DownloadLink link = createDownloadlink("directhttp://" + img.replace("?image=", ""));
                link.setFinalFileName(title + "–page_" + df.format(counter) + extension);
                fp.add(link);
                distribute(link);
                decryptedLinks.add(link);
            }
            counter++;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}