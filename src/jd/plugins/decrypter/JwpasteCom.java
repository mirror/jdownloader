//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jwpaste.com" }, urls = { "https?://(?:www\\.)?jwpaste\\.com/v/\\d+" }) 
public class JwpasteCom extends PluginForDecrypt {

    public JwpasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("class=\"paste\\-error\"")) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String plaintxt = br.getRegex("<td nowrap align=\"left\"(.*?)</table>").getMatch(0);
        if (plaintxt == null && parameter.contains("raw.php")) {
            plaintxt = br.toString();
        }
        if (plaintxt == null) {
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
            if (!dl.contains(parameter) && this.canHandle(parameter)) {
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