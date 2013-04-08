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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega-protect.com" }, urls = { "http://(www\\.)?mega\\-protect\\.com/(?!inscription\\.php|forget\\.php).*?\\.php" }, flags = { 0 })
public class MegaPrtcCm extends PluginForDecrypt {

    public MegaPrtcCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("<title>404 Not Found</title>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("name=\"captcha\" id=\"captcha\\-form\"")) {
            // Captcha handling
            for (int i = 0; i <= 5; i++) {
                if (!br.containsHTML("captcha\\.php")) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                String captchalink = "http://www.mega-protect.com/captcha.php";
                String code = getCaptchaCode(captchalink, param);
                br.getPage(parameter + "?captcha=" + code);
                if (br.containsHTML("(Code incorrect|captcha.php)")) continue;
                break;
            }
            if (br.containsHTML("(Code incorrect|captcha.php)")) throw new DecrypterException(DecrypterException.CAPTCHA);
            String finallink = br.getRegex("style=\"text-align: center;\">.*?<a href=(.*?)>").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            final String[] links = br.getRegex(">(http://(www\\.)?mega\\-protect\\.com/[^<>\"]*?\\.php)</a>").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String finallink : links) {
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        return decryptedLinks;
    }

}
