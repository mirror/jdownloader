//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "disk.tom.ru" }, urls = { "http://([\\w]+\\.)?disk\\.tom\\.ru/[a-z0-9]{7}" }, flags = { 0 })
public class DiskTomRu extends PluginForDecrypt {

    /**
     * @author rnw
     */
    public DiskTomRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String uid = new Regex(parameter, "([a-z0-9]{7})$").getMatch(0);

        br.getPage(parameter);
        if (br.containsHTML("<div id=\"error\">пакет не найден<")) {
            logger.info("Wrong URL or the package no longer exists.");
            return decryptedLinks;
        }
        String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (title == null) title = "";

        /* Password protected package */
        if (br.containsHTML(">пакет защищен паролем<")) {
            for (int i = 0; i <= 3; i++) {
                final String passCode = Plugin.getUserInput("Enter password for: " + title, param);
                br.postPage(parameter, "put_pwd=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(">пакет защищен паролем<")) continue;
                break;
            }
            if (br.containsHTML(">пакет защищен паролем<")) throw new DecrypterException(DecrypterException.PASSWORD);
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        }

        String domain = new Regex(br.getURL(), "(https?://[^/]+)").getMatch(0);

        String[] links = br.getRegex("href=\"(/download/" + uid + "/[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        for (String link : links) {
            decryptedLinks.add(createDownloadlink("directhttp://" + domain + link));
        }

        if (title != null || !title.equals("")) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(title.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}