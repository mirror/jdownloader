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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "unixmanga.com" }, urls = { "http://(www\\.)?unixmanga\\.com/onlinereading/[^\\?]*?/.*?(c|ch)\\d+.*" }, flags = { 0 })
public class UnixMangaCom extends PluginForDecrypt {

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
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> tempList = new ArrayList<String>();
        br.setFollowRedirects(true);
        String url = parameter.toString();
        try {
            br.getPage(url);
        } catch (final Exception e) {
            logger.info("Server error -> Decrypt failed for link: " + parameter);
            return decryptedLinks;
        }
        if (!br.containsHTML(">Select A Link To Start Reading<")) {
            logger.info("Unsupported link: " + parameter);
            return decryptedLinks;
        }

        // We get the title
        String title = br.getRegex("<TITLE>READ\\-\\->\\.\\.([^<>\"]*?)\\.\\.<\\-\\-ONLINE ACCESS</TITLE>").getMatch(0);
        if (title == null) {
            logger.warning("Title not found! : " + parameter);
            return null;
        }
        // set sub + domain used, used later in page views
        String Url = new Regex(br.getURL(), "(?i)(https?://[^/]+)").getMatch(0);
        // We load each page and retrieve the URL of the picture
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title);

        // Grab all available images
        final String[] allImages = br.getRegex("<A class=\"td2\" rel=\"nofollow\" HREF=\"(http://[^<>\"]*?)\\&server=").getColumn(0);
        if (allImages == null || allImages.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String img : allImages) {
            tempList.add(img);
        }
        int counter = 1;
        for (final String img : tempList) {
            // remove unnecessary images from saving
            if (!img.contains("xxxhomeunixxxx.png")) {
                String extension = img.substring(img.lastIndexOf("."));
                final DownloadLink link = createDownloadlink("directhttp://" + img.replace("?image=", ""));
                link.setFinalFileName(title + "–page_" + counter + extension);
                fp.add(link);
                try {
                    distribute(link);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(link);
            }
            counter++;
        }
        br.getPage(tempList.get(counter - 2));
        while (true) {
            // We look for next page and load it ready for the 'for' loop.
            String nextPage = br.getRegex("document\\.write\\(\\'<a href =\"(\\?image=[^<>\"]*?)\"><b>\\[NEXT PAGE\\]<").getMatch(0);
            if (nextPage != null) {
                nextPage = "http://nas.homeunix.com/onlinereading/" + nextPage;
                br.getPage(nextPage);
                if (!nextPage.contains("xxxhomeunixxxx.png")) {
                    String extension = nextPage.substring(nextPage.lastIndexOf("."));
                    final DownloadLink link = createDownloadlink("directhttp://" + nextPage.replace("?image=", ""));
                    link.setFinalFileName(title + "–page_" + counter + extension);
                    fp.add(link);
                    try {
                        distribute(link);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    decryptedLinks.add(link);
                }
                continue;
            } else {
                break;
            }

        }
        return decryptedLinks;
    }
}
