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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * have password, though i couldn't create one to test.
 *
 * @author raztoki
 * */
@DecrypterPlugin(revision = "$Revision: 20515 $", interfaceVersion = 2, names = { "pastee.org" }, urls = { "https?://(?:www\\.)?pastee.org/[a-z0-9]{5}" }, flags = { 0 })
public class PasteeOrg extends PluginForDecrypt {

    public PasteeOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("Page Not Found") || br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String plaintxt = br.getRegex("<div[^>]+id=\"paste\"[^>]*>(.*?)\\s+</div>\\s+").getMatch(0);
        if (plaintxt == null) {
            return decryptedLinks;
        }
        final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no hosterlinks in plaintext from link " + parameter);
            return decryptedLinks;
        }
        /* avoid recursion */
        for (final String link : links) {
            if (!this.canHandle(link)) {
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}