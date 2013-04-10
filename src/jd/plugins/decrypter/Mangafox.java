//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangafox.me" }, urls = { "http://[\\w\\.]*?mangafox\\.(com|me|mobi)/manga/.*?/(v\\d+/c[\\d\\.]+|c[\\d\\.]+)" }, flags = { 0 })
public class Mangafox extends PluginForDecrypt {

    public Mangafox(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String url = parameter.toString().replaceAll("://[\\w\\.]*?mangafox\\.(com|me|mobi)/", "://mangafox.me/");
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        // Access chapter one
        url = url.replaceAll("(c00\\d+)$", "c001");
        br.getPage(url + "/1.html");

        boolean nextChapterAvailable = true;
        while (nextChapterAvailable) {
            if (br.containsHTML("cannot be found|not available yet")) {
                logger.warning("Invalid link or release not yet available, check in your browser: " + parameter);
                return decryptedLinks;
            }
            if (!br.containsHTML("onclick=\"return enlarge\\(\\)\"")) {
                logger.warning("Invalid link: " + parameter);
                return decryptedLinks;
            }
            final String nextChapter = br.getRegex("<span>Next Chapter:</span> <a href=\"(http://mangafox\\.me/[^<>\"]*?)\">[^<>\"]*?</a></p>").getMatch(0);
            // We get the title
            String title = br.getRegex("<title>(.*?) \\- Read (.*?) Online \\- Page 1</title>").getMatch(0);
            if (title == null) {
                logger.warning("Decrypter broken for: " + parameter);
                return null;
            }
            title = Encoding.htmlDecode(title.trim());

            // We get the number of pages in the chapter
            String format = "%02d";
            int numberOfPages = Integer.parseInt(br.getRegex("of (\\d+)").getMatch(0));
            if (numberOfPages > 0) {
                format = String.format("%%0%dd", (int) Math.log10(numberOfPages) + 1);
            }
            // We load each page and retrieve the URL of the picture
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            int skippedPics = 0;
            for (int i = 1; i <= numberOfPages; i++) {
                br.getPage(url + "/" + i + ".html");
                String pageNumber = String.format(format, i);
                final String[][] unformattedSource = br.getRegex("onclick=\"return enlarge\\(\\);?\"><img src=\"(http://.*?(.[a-z]+))\"").getMatches();
                if (unformattedSource == null || unformattedSource.length == 0) {
                    skippedPics++;
                    if (skippedPics > 5) {
                        logger.info("Too many links were skipped, stopping...");
                        break;
                    }
                    continue;
                }
                String source = unformattedSource[0][0];
                String extension = unformattedSource[0][1];
                final DownloadLink link = createDownloadlink("directhttp://" + source);
                link.setFinalFileName(title + " – page " + pageNumber + extension);
                fp.add(link);
                try {
                    distribute(link);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(link);
            }
            if (nextChapter != null) {
                br.getPage(nextChapter);
                nextChapterAvailable = true;
                logger.info("Decrypting chapter: " + nextChapter);
            } else {
                nextChapterAvailable = false;
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}