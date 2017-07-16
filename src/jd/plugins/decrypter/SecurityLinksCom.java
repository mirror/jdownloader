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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "security-links.com" }, urls = { "http://(www\\.)?security-links\\.com/(?:\\d+/[A-Za-z0-9:;/\\.@#]+|[A-Za-z0-9]+)" })
public class SecurityLinksCom extends PluginForDecrypt {

    public SecurityLinksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("il y a une ereur")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML("\"generate\\.php\"")) {
            for (int i = 0; i <= 3; i++) {
                final String code = getCaptchaCode("http://security-links.com/generate.php", param);
                br.postPage(br.getURL(), "submit_1=Valider&secure=" + Encoding.urlEncode(code));
                if (br.containsHTML("\"generate\\.php\"")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("\"generate\\.php\"")) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        } else if (br.containsHTML("le lien est protegé par un mot de passe")) {
            for (int i = 0; i <= 3; i++) {
                final String pass = getUserInput("Password?", param);
                br.postPage(br.getURL(), "submit=Valider&passe=" + Encoding.urlEncode(pass));
                if (br.containsHTML("le lien est protegé par un mot de passe")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("le lien est protegé par un mot de passe")) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String[] links = br.getRegex("\\d+\\| <a href=\\'(http[^<>\"]*?)\\'").getColumn(0);
        if (links == null || links.length == 0) {
            // for /\\d+/[A-Za-z0-9:;]+/[A-Za-z0-9:;]+
            final String filter = br.getRegex("<div id=\"hideshow\".*?</div>").getMatch(-1);
            if (filter != null) {
                links = new Regex(filter, "href=('|\")(.*?)\\1").getColumn(1);
            }
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink));
        }

        return decryptedLinks;
    }

}
