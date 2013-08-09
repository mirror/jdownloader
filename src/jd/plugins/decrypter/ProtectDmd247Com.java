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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect.dmd247.com" }, urls = { "http://(www\\.)?protect\\.dmd247\\.com/[^<>\"/]+" }, flags = { 0 })
public class ProtectDmd247Com extends PluginForDecrypt {

    public ProtectDmd247Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">The ID was not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String captchaAdress = br.getRegex("\"(CaptchaSecurityImages\\.php[^<>\"]*?)\"").getMatch(0);
        if (captchaAdress != null) {
            captchaAdress = "http://protect.dmd247.com/" + captchaAdress;
            for (int i = 1; i <= 3; i++) {
                final String code = getCaptchaCode(captchaAdress, param);
                br.postPage(br.getURL(), "submit1=Submit&security_code=" + Encoding.urlEncode(code));
                if (br.containsHTML(">Sorry, you have provided an invalid security code")) continue;
                break;
            }
            if (br.containsHTML(">Sorry, you have provided an invalid security code")) throw new DecrypterException(DecrypterException.CAPTCHA);
        } else if (br.containsHTML(">Password Protected Link")) {
            for (int i = 1; 0 <= 3; i++) {
                final String passCode = getUserInput("Password?", param);
                br.postPage(br.getURL(), "Submit0=Submit&Pass1=" + Encoding.urlEncode(passCode));
                if (br.containsHTML(">You have entered an incorrect password\\.")) continue;
                break;
            }
            if (br.containsHTML(">You have entered an incorrect password\\.")) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        final String linktext = br.getRegex("<TEXTAREA NAME=\"information\"(.*?)</TEXTAREA>").getMatch(0);
        final String[] links = HTMLParser.getHttpLinks(linktext, "");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links)
            decryptedLinks.add(createDownloadlink(singleLink));

        return decryptedLinks;
    }

}
