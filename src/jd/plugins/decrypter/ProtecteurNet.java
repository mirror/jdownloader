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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protecteur.net" }, urls = { "http://(www\\.)?protecteur\\.net/check\\.[a-z]+\\.html" }, flags = { 0 })
public class ProtecteurNet extends PluginForDecrypt {

    public ProtecteurNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    // All similar: IleProtectCom, ExtremeProtectCom, TopProtectNet, ProtecteurNet
    // top-protect and top-protection uids are not transferable. Keep script independent from domain.

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        br.postPage("/linkid.php", "linkid=" + new Regex(parameter, "([a-z]+)\\.html$").getMatch(0) + "&x=" + Integer.toString(new Random().nextInt(100)) + "&y=" + Integer.toString(new Random().nextInt(100)));
        final String fpName = br.getRegex("Title:[\t\n\r ]+</td>[\t\n\r ]+<td style='border:1px'>([^<>\"/]+)</td>").getMatch(0);
        String[] links = br.getRegex("target=_blank>(https?://[^<>\"']+)").getColumn(0);
        if (links == null || links.length == 0) {
            if (br.containsHTML("href= target=_blank></a><br></br><a")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            if (!new Regex(singleLink, "protecteur\\.net/").matches()) decryptedLinks.add(createDownloadlink(singleLink));
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