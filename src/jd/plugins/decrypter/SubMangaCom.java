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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 149041111 $", interfaceVersion = 2, names = { "submanga.com" }, urls = { "http://(www\\.)?submanga\\.com/c/\\d+" }, flags = { 0 })
public class SubMangaCom extends PluginForDecrypt {

    public SubMangaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // DEV NOTES
    // other: decided to write like unixmanga.

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(false);
        String parameter = param.toString().replace("www.", "");
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404")) {
            logger.warning("Invalid URL! : " + parameter);
            return null;
        }
        // We get the title
        String[][] title = br.getRegex("<title>([\\w ]+) (\\d+) \\&mdash;").getMatches();
        if (title == null || title.length == 0) {
            logger.warning("Title not found! : " + parameter);
            return null;
        }
        // grab the total pages within viewer
        String TotalPages = br.getRegex("(?i)<option value=\"(\\d+)\">\\d+</option></select>").getMatch(0);
        if (TotalPages == null) {
            logger.warning("'TotalPages' not found! : " + parameter);
            return null;
        }
        int numberOfPages = Integer.parseInt(TotalPages);
        String format = "%02d";
        if (numberOfPages > 0) {
            format = String.format("%%0%dd", (int) Math.log10(numberOfPages) + 1);
        }

        progress.setRange(numberOfPages);

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // We load each page and retrieve the URL of the picture
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title[0][0] + "_" + title[0][1]);
        for (int i = 1; i <= numberOfPages; i++) {
            String pageNumber = String.format(format, (i));
            // grab the image source
            String img = br.getRegex("(?i)<img (width=\"\\d+\\%\" )?src=\"(https?://img\\d+\\.submanga\\.com/pages/\\d+/\\w+/\\d+\\.\\w{1,4})\"/>(</a></div><script)").getMatch(1);
            if (img == null) img = br.getRegex("(?i)<img (width=\"\\d+\\%\" )?src=\"(http[^\"]+)\"/></a></div><script").getMatch(1);
            if (img == null) {
                logger.warning("No images found for page : " + pageNumber + " : " + parameter);
                logger.warning("Continuing...");
            }

            String extension = img.substring(img.lastIndexOf("."));
            DownloadLink link = createDownloadlink("directhttp://" + img);
            link.setFinalFileName(title[0][0] + "_" + title[0][1] + "–page_" + pageNumber + extension);
            fp.add(link);
            try {
                distribute(link);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(link);
            if (i != numberOfPages) {
                // load next page for the 'for' loop.
                br.getPage(parameter + "/" + (i + 1));
            }
            progress.increase(1);
        }
        logger.warning("Task Complete! : " + parameter);
        return decryptedLinks;
    }
}
