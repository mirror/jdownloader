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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filespart.com" }, urls = { "http://(www\\.)?filespart\\.com/(dl|go)/[0-9a-zA-Z]+\\.html" }, flags = { 0 })
public class FilesPartCom extends PluginForDecrypt {

    public FilesPartCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (parameter.contains("filespart.com/go")) {
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            if (br.containsHTML(">Error 404 \\-")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            /** Captcha only appears if user adds maaany links */
            if (br.containsHTML(">Please Enter Captcha For Download:")) {
                for (int i = 0; i <= 3; i++) {
                    final String captchaLink = br.getRegex("(/ot/captcha\\.aspx\\?capid=[A-Z0-9]+)\"").getMatch(0);
                    if (captchaLink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    final String code = getCaptchaCode("http://filespart.com" + captchaLink, param);
                    br.postPage(parameter, "capid=" + code);
                    if (br.containsHTML(">Please Enter Captcha For Download:")) continue;
                    break;
                }
                if (br.containsHTML(">Please Enter Captcha For Download:")) throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            final String linkTextarea = br.getRegex("onclick=\"this\\.select\\(\\);\">(.*?)</textarea>").getMatch(0);
            if (linkTextarea == null) { return null; }
            final String[] allLinks = HTMLParser.getHttpLinks(linkTextarea, null);
            if (allLinks == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : allLinks) {
                if (!singleLink.contains("filespart.com/")) decryptedLinks.add(createDownloadlink(singleLink));
            }
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}