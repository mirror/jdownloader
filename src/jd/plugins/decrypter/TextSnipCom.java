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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "textsnip.com" }, urls = { "http://(www\\.)?textsnip\\.com/(?!user|acc|about|terms)[a-z0-9]+" }, flags = { 0 })
public class TextSnipCom extends PluginForDecrypt {

    public TextSnipCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.setFollowRedirects(false);
        /* Error handling */
        if ("http://textsnip.com/".equals(br.getRedirectLocation())) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("action=\"create\\.php\"") || br.containsHTML(">Index of")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getRedirectLocation() != null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String plaintxt = br.getRegex("<code>(.*?)</code>").getMatch(0);
        if (plaintxt == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no hosterlinks in plaintext from link " + parameter);
            return decryptedLinks;
        }
        /* avoid recursion */
        for (String link : links) {
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