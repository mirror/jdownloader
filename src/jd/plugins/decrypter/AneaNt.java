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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animea.net" }, urls = { "http://((www\\.)?animea\\.net/download/[\\d\\-]+/[\\w\\-]+episode\\-[\\d+\\.]+\\.html|manga\\.animea\\.net/[\\w\\-]+chapter\\-[\\d+\\.]+(\\-page\\-[\\d+\\.]+)?\\.html)" }, flags = { 0 })
public class AneaNt extends PluginForDecrypt {

    /**
     * @author raztoki
     */

    // DEV NOTES
    // protocol: no https
    // other: manga.* like unixmanga/submanga. Export into AnimeaNet hoster
    // because jd urldecodes %23 into #. Which results in 404. Impractical to
    // use directhttp

    private String agent = null;

    public AneaNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        this.br.setRequestIntervalLimit(this.getHost(), 1000);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches("http://manga\\.animea\\.net/[\\w\\-]+chapter\\-[\\d+\\.]+(\\-page\\-[\\d+\\.]+)?\\.html")) {
            parameter = parameter.replaceAll("chapter\\-\\d+(\\-page\\-\\d+)\\.html", "chapter-1.html");
            param.setCryptedUrl(parameter);
        }
        br.setFollowRedirects(false);
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.setRequestIntervalLimit("manga.animea.net", 2000);
        br.setCookie("http://animea.net", "lang", "english");
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        br.getHeaders().put("User-Agent", agent);
        br.getPage(parameter);
        if (br.containsHTML("> 404 Not Found") || (br.getRedirectLocation() != null && br.getRedirectLocation().contains("manga.animea.net/browse.html"))) {
            logger.warning("Chapter doesn't exist, or URL doesn't exist: " + parameter);
            return decryptedLinks;
        }
        // manga reader
        if (parameter.contains("manga.animea.net/")) {
            // We get the title
            String[][] title = br.getRegex("(?i)<title>(.+) (chapter ([\\d\\.]+)) - Page 1 of (\\d+)</title>").getMatches();
            if (title == null || title.length == 0) {
                logger.warning("Title not found! : " + parameter);
                return null;
            }
            String useTitle = Encoding.htmlDecode(title[0][0] + " – " + title[0][1]).trim().replaceAll("\"", "");
            // grab the total pages within viewer
            String totalPages = title[0][3];
            if (totalPages == null) totalPages = br.getRegex("(?i)\\d+</option>[\r\n\t ]+</select>[\r\n\t ]+of (\\d+)").getMatch(0);
            if (totalPages == null) {
                logger.warning("'TotalPages' not found! : " + parameter);
                return null;
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
                String pageNumber = String.format(format, (i));
                // grab the image source
                String img = br.getRegex("(?i)<img src=\"(http?://[^\"]+)\" onerror").getMatch(0);
                if (img == null) img = br.getRegex("(?i)(http?://img.manga.animea.net/[^\"]+)").getMatch(0);
                if (img == null) {
                    logger.warning("No images found for page : " + pageNumber + " : " + parameter);
                    logger.warning("Continuing...");
                    if (i != numberOfPages) {
                        // load next page for the 'for' loop.
                        br.getPage(parameter.replace(".html", "-page-" + (i + 1) + ".html"));

                    }
                    progress.increase(1);
                    continue;
                }
                String extension = img.substring(img.lastIndexOf("."));
                DownloadLink link = createDownloadlink("ANIMEA://" + img);
                link.setFinalFileName((useTitle + " – page " + pageNumber + extension).replace(" ", "_"));
                link.setAvailable(true);
                link.setProperty("fastAdd", "true");
                fp.add(link);
                try {
                    distribute(link);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(link);
                if (i != numberOfPages) {
                    // load next page for the 'for' loop.
                    br.getPage(parameter.replace(".html", "-page-" + (i + 1) + ".html"));
                }
                progress.increase(1);
            }
            logger.warning("Task Complete! : " + parameter);
        }
        // /download/
        else {
            String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
            String grabFrame = br.getRegex("Download links</h2>(.+)<script type=").getMatch(0);
            if (grabFrame == null) {
                logger.warning("Possible Plugin error, with finding download image: " + parameter);
                return null;
            }
            String[] links = new Regex(grabFrame, "<td><a href=\"(https?://[^\"]+)").getColumn(0);
            if (links == null || links.length == 0) return null;
            if (links != null && links.length != 0) {
                for (String dl : links)
                    decryptedLinks.add(createDownloadlink(dl));
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }
}