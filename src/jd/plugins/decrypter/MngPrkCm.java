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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangapark.com" }, urls = { "http://(www\\.)?manga(park|tank|window)\\.com/manga/[\\w\\-\\.\\%]+/(v\\d+/?)?(c(ex(tra)?[^/]+|[\\d\\.]+(v\\d|[^/]+)?)?|extra(\\+\\d+)?|\\+\\(Oneshot\\))" }, flags = { 0 })
public class MngPrkCm extends PluginForDecrypt {
    /**
     * @author raztoki
     */

    // DEV NOTES
    // protocol: no https
    // other: sister sites mangatank & mangawindows.
    // other: links are not nessairly transferable
    // other: regex is tight as possible, be very careful playing!

    private String HOST = "";

    public MngPrkCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void prepBrowser() {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9, de;q=0.8");
        br.setCookie(HOST, "lang", "english");
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("://manga", "://www.manga");
        String[][] grabThis = null;
        HOST = new Regex(parameter, "(https?://[^/]+)").getMatch(0);
        prepBrowser();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("(>Sorry, the page you have requested cannot be found.<|Either the URL of your requested page is incorrect|page has been removed or moved to a new URL)")) {
            logger.warning("Possible Plugin error, with finding download image: " + parameter);
            return null;
        }
        // limit the results
        grabThis = getThis(parameter);
        if (grabThis == null || grabThis.length == 0) {
            logger.warning("Issue with getThis! : " + parameter);
            return null;
        }
        // We get the title
        String[][] title = new Regex(grabThis[0][0], "title=\"(.+?) ((Vol\\.\\d+ )?([ ]+)?Ch\\. ?([\\d\\.]+|extra|\\(Oneshot\\)).+?)(\\- | )?(image|Page) \\d+").getMatches();
        if (title == null || title.length == 0) {
            title = new Regex(grabThis[0][1], "title=\"(.+?) ((Vol\\.\\d+ )?([ ]+)?Ch\\. ?([\\d\\.]+|extra|\\(Oneshot\\)).+?)(\\- | )?(image|Page) \\d+").getMatches();
            if (title == null || title.length == 0) {
                logger.warning("Title not found! : " + parameter);
                return null;
            }
        }
        String useTitle = title[0][0].trim() + " – " + title[0][1].trim();
        // grab the total pages within viewer
        String totalPages = br.getRegex(">\\d+ of (\\d+)</a></em>").getMatch(0);
        if (totalPages == null) {
            totalPages = br.getRegex("selected>\\d+ / (\\d+)</option>").getMatch(0);
            if (totalPages == null) {
                logger.warning("'TotalPages' not found! : " + parameter);
                return null;
            }
        }
        int numberOfPages = Integer.parseInt(totalPages);
        String format = "%02d";
        if (numberOfPages > 0) {
            format = String.format("%%0%dd", (int) Math.log10(numberOfPages) + 1);
        }

        progress.setRange(numberOfPages);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(useTitle);

        // We load each page and retrieve the URL of the picture
        for (int i = 1; i <= numberOfPages; i++) {
            String pageNumber = String.format(format, i);
            String img = null;
            if (i != 1) grabThis = getThis(parameter);
            // grab the image source
            if (grabThis != null && grabThis.length != 0) img = new Regex(grabThis[0][0], "href=\"(http[^\"]+)").getMatch(0);
            if (img == null && (grabThis != null && grabThis.length != 0)) {
                img = new Regex(grabThis[0][1], "src=\"(http[^\"]+)").getMatch(0);
                if (img == null) {
                    logger.warning("No images found for page : " + pageNumber + " : " + parameter);
                    logger.warning("Continuing...");
                    if (i != numberOfPages) {
                        // load next page for the 'for' loop.
                        br.getPage(parameter + "/" + (i + 1));
                    }
                    progress.increase(1);
                    continue;
                }
            }
            String extension = img.substring(img.lastIndexOf("."));
            DownloadLink link = createDownloadlink("directhttp://" + img);
            link.setFinalFileName((useTitle + " – page " + pageNumber + extension).replace(" ", "_"));
            fp.add(link);
            try {
                distribute(link);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(link);
            if (i != numberOfPages) {
                grabThis = null;
                // load next page for the 'for' loop.
                br.getPage(parameter + "/" + (i + 1));
            }
            progress.increase(1);
        }
        logger.warning("Task Complete! : " + parameter);
        HOST = "";
        return decryptedLinks;
    }

    private String[][] getThis(String parameter) {
        String[][] grabThis = br.getRegex("(<div class=\"tool\".+</span>[\r\n\t ]+</div>[\r\n\t ]+</div>).+(<a class=\"img\\-link\" href.+>[\r\n\t ]+</a>[\r\n\t ]+</div>)").getMatches();
        return grabThis;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}