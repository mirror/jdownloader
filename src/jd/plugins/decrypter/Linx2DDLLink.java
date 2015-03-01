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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linx.2ddl.link" }, urls = { "http://(www\\.)?linx\\.2ddl\\.link/[A-Za-z0-9]+" }, flags = { 0 })
public class Linx2DDLLink extends PluginForDecrypt {

    public Linx2DDLLink(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("The ID was not found")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML(">Password Protected Link")) {
            for (int i = 0; i <= 3; i++) {
                final String passCode = getUserInput("Password?", param);
                br.postPage(br.getURL(), "Pass1=" + Encoding.urlEncode(passCode) + "&Submit0=Submit");
                if (br.containsHTML(">Password Protected Link")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML(">Password Protected Link")) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        } else {
            for (int i = 0; i <= 3; i++) {
                final String code = getCaptchaCode("/CaptchaSecurityImages.php?width=100&height=40&characters=5", param);
                br.postPage(br.getURL(), "security_code=" + Encoding.urlEncode(code) + "&submit1=Submit");
                if (br.containsHTML("CaptchaSecurityImages\\.php")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("CaptchaSecurityImages\\.php")) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        }
        final String[] links = br.getRegex("<p><a href=\"([^<>\"]*?)\"").getColumn(0);
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
