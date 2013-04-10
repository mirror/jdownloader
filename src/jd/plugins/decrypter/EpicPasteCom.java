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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "epicpaste.com" }, urls = { "http://(www\\.)?epicpaste\\.com/index\\.php/view/(raw/)?[a-z0-9]+" }, flags = { 0 })
public class EpicPasteCom extends PluginForDecrypt {

    public EpicPasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = "http://epicpaste.com/index.php/view/raw/" + new Regex(param.toString(), "([a-z0-9]+)$").getMatch(0);
        br.getPage(parameter);
        /** Link offline/invalid? */
        if (br.containsHTML("(>404 Page Not Found<|>The page you requested was not found|<title>Stikked</title>)")) return decryptedLinks;
        final String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        final String pagePiece = br.getRegex("<pre>(.*?)</pre>").getMatch(0);
        if (pagePiece == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String[] links = HTMLParser.getHttpLinks(pagePiece, "");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            if (!new Regex(singleLink, "http://(www\\.)?epicpaste\\.com/index\\.php/view/(raw/)?[a-z0-9]+").matches()) decryptedLinks.add(createDownloadlink(singleLink));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}