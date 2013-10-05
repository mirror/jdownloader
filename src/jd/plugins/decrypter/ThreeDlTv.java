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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "3dl.tv" }, urls = { "http://(www\\.)?(music|games|movies|serien|apps|porn)\\.3dl\\.tv/(download/\\d+/[^<>\"/]+/detail\\.html|folder/[a-z0-9]+)" }, flags = { 0 })
public class ThreeDlTv extends PluginForDecrypt {

    public ThreeDlTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* NOTE: no override to keep compatible to old stable */
    // If we do more at a time they will block our IP for 10 minutes
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> passwords = new ArrayList<String>();
        String parameter = param.toString();
        br.getPage(parameter);
        final String currentdomain = new Regex(parameter, "(http://(www\\.)?[a-z]+\\.3dl\\.tv)").getMatch(0);
        if (parameter.matches(".*?\\.3dl\\.tv/download/\\d+/[^<>\"/]+/detail\\.html")) {
            if (br.containsHTML(">Dieser Eintrag existiert nicht mehr")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String password = br.getRegex("<th>Passwort:</th><td colspan=\"3\"><input type=\"text\" value=\"([^<>\"]*?)\"").getMatch(0);
            String fpName = br.getRegex("<th>Titel:</th><td>([^<>\"]*?)<br").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<table cellpadding=\"0\" cellspacing=\"0\"><tr><td><div>([^<>\"]*?)</div></td><td").getMatch(0);
            final String[] folders = br.getRegex("(/folder/[a-z0-9]+)\"").getColumn(0);
            if (folders == null || folders.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String folder : folders) {
                DownloadLink linkl = createDownloadlink(currentdomain + folder);
                passwords.add(password);
                if (password != null) linkl.setSourcePluginPasswordList(passwords);
                decryptedLinks.add(linkl);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else {
            if (br.containsHTML(">Der Zugriff auf diesen Link Ordner wurde aus Sicherheitsgr")) {
                logger.info("Can't access link, IP blocked, wait at least 10 minutes!");
                return decryptedLinks;
            }
            if (br.containsHTML(">Error 404 \\- Ordner nicht gefunden<|>Error 404<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            for (int i = 1; i <= 5; i++) {
                final String captchaLink = br.getRegex("\"((https?://(\\w+\\.)?3dl\\.tv)?/index\\.php\\?action=captcha\\&token=[^\"\\']+)").getMatch(0);
                if (captchaLink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final String code = getCaptchaCode(captchaLink, param);
                br.postPage(parameter, "answer=" + code);
                if (br.containsHTML(">Die von dir eingegebene Anwort ist nicht g")) {
                    this.sleep(3 * 1000l, param);
                    continue;
                }
                break;
            }
            if (br.containsHTML(">Die von dir eingegebene Anwort ist nicht g")) throw new DecrypterException(DecrypterException.CAPTCHA);
            // Add links via CNL
            Form cnlform = br.getForm(0);
            if (cnlform != null) {
                final Browser cnlbr = br.cloneBrowser();
                cnlbr.setConnectTimeout(5000);
                cnlbr.getHeaders().put("jd.randomNumber", System.getProperty("jd.randomNumber"));
                try {
                    cnlbr.submitForm(cnlform);
                    if (cnlbr.containsHTML("success")) { return decryptedLinks; }
                } catch (final Throwable e) {
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}