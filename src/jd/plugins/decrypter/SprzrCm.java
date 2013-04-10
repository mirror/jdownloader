//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sprezer.com" }, urls = { "http://(www\\.)?sprezer\\.com/[a-z0-9]{12}" }, flags = { 0 })
public class SprzrCm extends PluginForDecrypt {

    public SprzrCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setReadTimeout(3 * 60 * 1000);
        String parameter = param.toString();
        br.setCookie("http://sprezer.com", "lang", "english");
        br.getPage(parameter);
        if (br.containsHTML(">File Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>Download ([^<>\"\\']*?)</title>").getMatch(0);
        final String[] links = br.getRegex("id=\\'stat\\d+_\\d+\\'><a href=\\'(http://(www\\.)?" + this.getHost() + "/[a-z0-9]{2}_[a-z0-9]{12})\\'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String link : links) {
            // Saves traffic and time
            br.getHeaders().put("Referer", "http://www.sprezer.com/r_counter");
            br.getPage(link);
            final String finallink = br.getRegex("\\'>[\t\n\r ]+<frame src=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Link offline: " + link);
                continue;
            }
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}