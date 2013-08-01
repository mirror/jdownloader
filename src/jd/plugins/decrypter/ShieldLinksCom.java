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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shieldlinks.com" }, urls = { "http://(www\\.)?shieldlinks\\.com/\\d+\\.html" }, flags = { 0 })
public class ShieldLinksCom extends PluginForDecrypt {

    public ShieldLinksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setReadTimeout(180 * 1000);
        br.setConnectTimeout(180 * 1000);
        br.getPage(parameter);
        if (br.containsHTML("<div class=\"tc\">\\(\\-\\- XX \\-\\-\\)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String id = br.getRegex("type=\"hidden\" name=\"id\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (id == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.findID();
        rc.load();
        for (int i = 1; i <= 5; i++) {
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, param);
            br.postPage(br.getURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&getln=Get+links&id=" + id);
            if (br.containsHTML("type=\"text/javascript\" src=\"http://www\\.google\\.com/recaptcha/api/challenge")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("type=\"text/javascript\" src=\"http://www\\.google\\.com/recaptcha/api/challenge")) throw new DecrypterException(DecrypterException.CAPTCHA);
        final String fpName = br.getRegex(" target=\"_blank\">Inscrivez\\-vous et telecharger \\&bull; <strong>([^<>\"]*?)</strong>").getMatch(0);
        final String[] links = br.getRegex("<a href=\"(http[^<>\"]*?)\" target=\"_blank\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links)
            if (!singleLink.contains("shieldlinks.com/")) decryptedLinks.add(createDownloadlink(singleLink));
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
