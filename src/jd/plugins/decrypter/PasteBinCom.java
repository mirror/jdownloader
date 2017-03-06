//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pastebin.com" }, urls = { "http://(www\\.)?pastebin\\.com/(?:download\\.php\\?i=|raw.*?=|raw/|dl/)?[0-9A-Za-z]+" })
public class PasteBinCom extends PluginForDecrypt {

    public PasteBinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String type_invalid = "http://(www\\.)?pastebin\\.com/(messages|report|dl|scraping|languages|trends|signup|login|pro|profile|tools|archive|login\\.php|faq|search|settings|alerts|domains|contact|stats|etc|favicon|users|api|download|privacy|passmailer)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(type_invalid)) {
            return decryptedLinks;
        } else if (parameter.contains("/download.php?i=") || parameter.contains(".com/dl/")) {
            decryptedLinks.add(createDownloadlink("directhttp://" + parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* Error handling for invalid links */
        if (br.containsHTML("(Unknown paste ID|Unknown paste ID, it may have expired or been deleted)") || br.getURL().equals("http://pastebin.com/") || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String plaintxt = br.getRegex("<textarea(.*?)</textarea>").getMatch(0);
        if (plaintxt == null && (parameter.contains("raw.php") || parameter.contains("/raw/"))) {
            plaintxt = br.toString();
        }
        if (plaintxt == null) {
            if (!br.containsHTML("</textarea>")) {
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // Find all those links
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no links in link: " + parameter);
            return decryptedLinks;
        }
        logger.info("Found " + links.length + " links in total.");
        for (String dl : links) {
            if (!dl.contains(parameter) && !new Regex(dl, "http://(www\\.)?pastebin\\.com/(raw.*?=)?[0-9A-Za-z]+").matches()) {
                final DownloadLink link = createDownloadlink(dl);
                decryptedLinks.add(link);
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}