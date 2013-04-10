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

//multiload.cz by pspzockerscene
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiload.cz" }, urls = { "http://[\\w\\.]*?multiload\\.cz/stahnout/[0-9]+/" }, flags = { 0 })
public class MltLadCz extends PluginForDecrypt {

    public MltLadCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.containsHTML("soubor neexistuje")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        /* File package handling */
        ArrayList<String> allinks = new ArrayList<String>();
        String pagepieces[] = br.getRegex("class=\"(manager\\-linky|manager\\-linky multishare\\-kod)\">(.*?)</p>").getColumn(1);
        for (String pagepiece : pagepieces) {
            String[] links = HTMLParser.getHttpLinks(pagepiece, "");
            for (String link : links) {
                if (!link.contains("multiload.cz")) allinks.add(link);
            }
        }
        for (String finallink : allinks) {
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}