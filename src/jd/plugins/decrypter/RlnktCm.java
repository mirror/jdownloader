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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "relink-it.com" }, urls = { "http://www\\.relink\\-it\\.com/\\?[a-z0-9]+" }, flags = { 0 })
public class RlnktCm extends PluginForDecrypt {

    public RlnktCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Saisissez les informations suivantes<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String lid = br.getRegex("getPage\\(\\'([^<>\"]*?)\\'\\);").getMatch(0);
        if (lid == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage("http://www.relink-it.com/include/ajax.php?action=getPage&link_id=" + lid);
        final String link = br.getRegex("src=\"(http[^<>\"]*?)\"").getMatch(0);
        if (link == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}