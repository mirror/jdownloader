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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "liens-protect.com" }, urls = { "http://(www\\.)?liens\\-protect\\.com/[A-Za-z0-9_\\-]+" }, flags = { 0 })
public class LiensProtectCom extends PluginForDecrypt {

    public LiensProtectCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().contains("/error.php")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        boolean failed = true;
        for (int i = 1; i <= 3; i++) {
            String captchaLink = br.getRegex("\"\\.(/securimage_show[^<>\"]*?)\"").getMatch(0);
            if (captchaLink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            captchaLink = "http://www.liens-protect.com" + captchaLink;
            final String c = getCaptchaCode(captchaLink, param);
            br.postPage(br.getURL(), "do=contact&ct_captcha=" + c);
            if (br.containsHTML("/securimage_show")) continue;
            failed = false;
            break;
        }
        if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        final String[] links = br.getRegex("target=_blank>(http[^<>\"]*?)</a>").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links)
            decryptedLinks.add(createDownloadlink(singleLink));

        return decryptedLinks;
    }

}
