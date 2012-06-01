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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 16623 $", interfaceVersion = 2, names = { "paidpaste.com" }, urls = { "http://(www\\.)?(en\\.)?(paidpaste\\.com|ppst\\.me)/[A-Za-z0-9]+" }, flags = { 0 })
public class PdPsteCm extends PluginForDecrypt {

    public PdPsteCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("ppst.me/", "paidpaste.com/");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML(">This paste is no longer available")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Password handling
        if (br.containsHTML("name=\"paste_password\"")) {
            for (int i = 0; i <= 3; i++) {
                String passCode = getUserInput(null, param);
                br.postPage(parameter, "submit=Authenticate&paste_password=" + passCode);
                if (br.containsHTML("name=\"paste_password\"")) continue;
                break;
            }
            if (br.containsHTML("name=\"pass\"")) {
                logger.warning("Wrong password!");
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        final String plaintxt = br.getRegex("<pre class=\"highlighted\\-text monospace\\-text\">(.*?)<pre class=\"line\\-numbers monospace\\-text\"><a href=\\'").getMatch(0);
        if (plaintxt == null) return null;
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links.length == 0) {
            logger.info("No links found: " + parameter);
            return decryptedLinks;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));

        return decryptedLinks;
    }

}
