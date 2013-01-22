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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mirrorupload.net" }, urls = { "http://(www\\.)?mirrorupload\\.net/file/[A-Z0-9]{8}" }, flags = { 0 })
public class MirrorUploadNet extends PluginForDecrypt {

    public MirrorUploadNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // Slow server
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String fpName = br.getRegex("<title>MirrorUpload\\.net \\- Download \\- ([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<b>File : </b>([^<>\"]*?)<br />").getMatch(0);
        final String noCaptcha = br.getRegex("name=\"nocaptcha\" value=\"(\\d+)\"").getMatch(0);
        if (noCaptcha != null) {
            br.postPage(br.getURL(), "nocaptcha=" + noCaptcha);
        } else {
            if (!br.containsHTML("images/captcha/captcha\\.php")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (int i = 0; i <= 3; i++) {
                final String code = getCaptchaCode("http://www.mirrorupload.net/images/captcha/captcha.php", param);
                br.postPage(parameter, "captcha=" + code);
                if (!br.containsHTML("images/captcha/captcha\\.php")) break;
            }
            if (br.containsHTML("images/captcha/captcha\\.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String[] links = br.getRegex(">(http://(www\\.)?mirrorupload\\.net/host\\-\\d+/[A-Z0-9]{8}/?)<").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            br.getPage(singleLink);
            final String finallink = br.getRedirectLocation();
            if (finallink != null) decryptedLinks.add(createDownloadlink(finallink));
        }
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
