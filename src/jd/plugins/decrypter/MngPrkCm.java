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
        HOST = new Regex(parameter, "(https?://[^/]+)").getMatch(0);
        prepBrowser();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().contains("search?") || br.containsHTML("(>Sorry, the page you have requested cannot be found.<|Either the URL of your requested page is incorrect|page has been removed or moved to a new URL)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final Regex srv_info = br.getRegex("target=\"_blank\" href=\"(http://[a-z0-9]+\\.mpcdn\\.net/[^<>\"]*?)(\\d+)(\\.(jpg|png))\"");
        final String srv_link = srv_info.getMatch(0);
        final String extension = srv_info.getMatch(2);
        String fpName = br.getRegex("</a> / ([^<>\"]*?)<em class=\"refresh\"").getMatch(0);
        fpName = Encoding.htmlDecode(fpName).trim();
        fpName = encodeUnicode(fpName);
        if (srv_link == null || fpName == null || extension == null) {
            if (br.containsHTML("class=\"manga\"")) {
                logger.info("Link offline (unsupported link): " + parameter);
                return decryptedLinks;
            }
            logger.warning("Issue with getThis! : " + parameter);
            return null;
        }
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
        FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);

        for (int i = 1; i <= numberOfPages; i++) {
            final String img = srv_link + i + extension;
            final DownloadLink link = createDownloadlink("directhttp://" + img);
            link.setFinalFileName((fpName + " – page " + i + extension).replace(" ", "_"));
            link.setAvailable(true);
            fp.add(link);
            // try {
            // distribute(link);
            // } catch (final Throwable e) {
            // /* does not exist in 09581 */
            // }
            decryptedLinks.add(link);
        }
        logger.warning("Task Complete! : " + parameter);
        HOST = "";
        return decryptedLinks;
    }

    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}