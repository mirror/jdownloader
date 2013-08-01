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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freakenergy.ru" }, urls = { "http://(www\\.)?freakenergy\\.ru/([a-z0-9\\-_]+\\.html|engine/go\\.php\\?url=[A-Za-z0-9%=]+)" }, flags = { 0 })
public class FreakEngyRu extends PluginForDecrypt {

    public FreakEngyRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String B64LINK = "http://(www\\.)?freakenergy\\.ru/engine/go\\.php\\?url=[A-Za-z0-9%=]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(B64LINK)) {
            final String finallink = decryptSingle(parameter);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            br.getPage(parameter);
            if (br.containsHTML("<b>Внимание, обнаружена ошибка<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            final String[] links = br.getRegex("\"(http[^<>\"]*?)\" target=\"_blank\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                String finallink = null;
                if (singleLink.matches(B64LINK))
                    finallink = decryptSingle(singleLink);
                else
                    finallink = singleLink;
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (!finallink.contains("freakenergy.ru/")) decryptedLinks.add(createDownloadlink(finallink));
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private String decryptSingle(String input) {
        input = Encoding.htmlDecode(input);
        return Encoding.Base64Decode(new Regex(input, "engine/go\\.php\\?url=(.+)").getMatch(0));
    }

}
