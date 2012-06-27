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
        br.setFollowRedirects(true);
        String url = parameter.toString();
        br.getPage(url);

        // We get the title
        String title = br.getRegex(".*?/(.+?)/.*?").getMatch(0);
        if (title == null) {
            logger.warning("Title not found! : " + parameter);
            return null;
        }
        // grab first page within the viewer
        br.getPage(br.getRegex("(?i)<A class=\"td2\" rel=\"nofollow\" HREF=\"(http[^\"]+)").getMatch(0));
        // set sub + domain used, used later in page views
        String Url = new Regex(br.getURL(), "(?i)(https?://[^/]+)").getMatch(0);
        // grab the total pages within viewer
        String TotalPages = br.getRegex("(?i)<a href=\".+\\?image=.+\\.html\">(\\d+)</a>[\r\n]+</center></div>").getMatch(0);
        if (TotalPages == null) {
            logger.warning("Intial page not found! : " + parameter);
            return null;
        } else {

            int numberOfPages = Integer.parseInt(TotalPages);
            String format = "%02d";
            if (numberOfPages > 0) {
                format = String.format("%%0%dd", (int) Math.log10(numberOfPages) + 1);
            }

            progress.setRange(numberOfPages);

            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            // We load each page and retrieve the URL of the picture
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            for (int i = 0; i < numberOfPages; i++) {
                String pageNumber = String.format(format, (i + 1));
                // grab the image source
                String Img = br.getRegex("(?i)<IMG ALT=\" \" STYLE=\"border: solid 1px #\\d+\" SRC=\"(http[^\"]+)").getMatch(0);
                if (Img == null) {
                    logger.warning("No images found for page : " + pageNumber + " : " + parameter);
                    logger.warning("Continuing...");
                }
                // remove unnecessary images from saving
                if (!Img.contains("xxxhomeunixxxx.png")) {
                    String extension = Img.substring(Img.lastIndexOf("."));
                    DownloadLink link = createDownloadlink("directhttp://" + Img);
                    link.setFinalFileName(title + "–page_" + pageNumber + extension);
                    fp.add(link);
                    try {
                        distribute(link);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    decryptedLinks.add(link);
                }
                progress.increase(1);
                // We look for next page and load it ready for the 'for' loop.
                String NextPage = br.getRegex("(?i)<a class=\"ne\" href =\"(http[^\"]+)").getMatch(0);
                if (NextPage != null) {
                    br.getPage(NextPage);
                    continue;
                }
                if (NextPage == null) {
                    NextPage = br.getRegex("(?i)<a class=\"ne\" href =\"(\\?image=[^\"]+)").getMatch(0);
                    if (NextPage != null) {
                        br.getPage(Url + "/onlinereading/" + NextPage);
                        continue;
                    } else if ((NextPage == null) && (!pageNumber.equals(TotalPages))) {
                        logger.warning("Plugin broken : " + parameter);
                        return null;
                    } else {
                        logger.warning("Task Complete! : " + parameter);
                    }
                }
            }
            return decryptedLinks;
        }
    }
}
