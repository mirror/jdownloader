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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flacforthemasses.com" }, urls = { "https://?flacforthemasses\\.com/node/\\d+" }, flags = { 0 })
public class FlacForTheMassesCom extends PluginForDecrypt {

    public FlacForTheMassesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("file not found\\!")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String artist = br.getRegex("class=\"tags\\-artists\">([^<>\"]*?)</a>").getMatch(0);
        final String year = br.getRegex("<title>.*? \\((\\d{4})\\)").getMatch(0);
        final String title = br.getRegex("class=\"album\">([^<>\"]*?)<").getMatch(0);
        final String linktext = br.getRegex("<pre class=\"download\\-link\\-pre jDownloader\"(.*?)</pre>").getMatch(0);
        if (linktext == null || artist == null || title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String fpName;
        if (year != null)
            fpName = Encoding.htmlDecode(artist.trim()) + " - " + year + " " + Encoding.htmlDecode(title.trim());
        else
            fpName = Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(title.trim());
        final String[] links = HTMLParser.getHttpLinks(linktext, null);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            decryptedLinks.add(createDownloadlink(singleLink));

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
