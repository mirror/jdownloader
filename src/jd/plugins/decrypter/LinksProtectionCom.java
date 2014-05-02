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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "links-protection.com" }, urls = { "http://(www\\.)?links\\-protection\\.com/l=[A-Za-z0-9]+" }, flags = { 0 })
public class LinksProtectionCom extends PluginForDecrypt {

    public LinksProtectionCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);

        if (br.containsHTML(">Invalid Link|>The link you are looking for has been deleted")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String fpName = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (fpName == null) fpName = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);

        Form cform = br.getFormbyKey("name", "linkprotect");
        if (cform == null) cform = br.getForm(2);
        if (cform == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        for (int i = 1; i <= 5; i++) {
            final String c = getCaptchaCode("http://links-protection.com/captcha/captcha.php", param);
            cform.put("captcha", c);
            br.submitForm(cform);
            if (!br.containsHTML("\"captcha/captcha\\.php\"")) break;
        }
        if (br.containsHTML("\"captcha/captcha\\.php\"")) throw new DecrypterException(DecrypterException.CAPTCHA);

        final String[] links = br.getRegex("href=s=([A-Za-z0-9=]+)\\&ID=[A-Za-z0-9]+ target=_blank").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links)
            decryptedLinks.add(createDownloadlink(Encoding.Base64Decode(singleLink)));

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
