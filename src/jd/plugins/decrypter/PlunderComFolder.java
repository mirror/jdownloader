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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "plunder.com" }, urls = { "http://(www\\.)?plunder\\.com/[^<>\"/]+(/[^<>\"/]+/)?" }, flags = { 0 })
public class PlunderComFolder extends PluginForDecrypt {

    public PlunderComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("(>Server Error<|>403 \\- Forbidden: Access is denied|>The file your are requesting is no longer available|<title>[\t\n\r ]+Download[\t\n\r ]+</title>)") || br.getURL().contains("/search/?f=")) {
            logger.info("Link offline or server error: " + parameter);
            return decryptedLinks;
        }
        if (!br.containsHTML("class=\\'shorturl\\'>") && !br.containsHTML("download\\-")) {
            logger.info("Link offline (no downloadlink): " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>.*?\\-(.*?)\\- Plunder").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<h1>.*?files \\-(.*?)</h1>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        String[] links = br.getRegex("\\'(http://(www\\.)?plunder\\.com/[a-z0-9\\-]+\\-download\\-[A-Z0-9]+\\.htm)\\'").getColumn(0);
        if (links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
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