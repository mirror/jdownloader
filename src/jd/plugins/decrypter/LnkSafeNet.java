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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "link-safe.net" }, urls = { "http://(www\\.)?link\\-safe\\.net/(folder/[a-z0-9]+\\-[a-z0-9]+|out/[a-z0-9]+\\-[a-z0-9]+/\\d+)" }, flags = { 0 })
public class LnkSafeNet extends PluginForDecrypt {

    public LnkSafeNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCustomCharset("windows-1251");
        br.getPage(parameter);
        if (parameter.matches("http://(www\\.)?link\\-safe\\.net/out/[a-z0-9]+\\-[a-z0-9]+/\\d+")) {
            if (br.containsHTML("<b>Fehler")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String finallink = decryptSingle();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            finallink = Encoding.htmlDecode(finallink);
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            if (br.containsHTML(">ID ссылки является недействительным\\. Пожалуйста, проверьте вашу ссылку")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String[] links = br.getRegex(":center;\"><a href=\"(http://.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(http://link\\-safe\\.net/out/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String aLink : links) {
                br.getPage(aLink);
                String finallink = decryptSingle();
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                finallink = Encoding.htmlDecode(finallink);
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        return decryptedLinks;
    }

    private String decryptSingle() {
        return br.getRegex("src=\"(.*?)\"").getMatch(0);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}