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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yep.pm" }, urls = { "http://(www\\.)?yep\\.pm/[A-Za-z0-9]+" }, flags = { 0 })
public class YepPm extends PluginForDecrypt {

    public YepPm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String finallink = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("Invalid Link")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        /* 1.Check for uncrypted links (direct redirects) */
        finallink = br.getRedirectLocation();
        if (finallink != null && !finallink.contains("yep.pm/")) {
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        } else if (finallink != null) {
            br.getPage(finallink);
        }
        /* 2.Handle captcha protected links */
        for (int i = 0; i <= 3; i++) {
            final String captchaid = br.getRegex("/captcha\\.php\\?cap_id=(\\d+)").getMatch(0);
            if (!br.containsHTML("captcha\\.php\\?cap_id=") || captchaid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String code = getCaptchaCode("http://yep.pm/captcha.php?cap_id=" + captchaid, param);
            br.postPage(br.getURL(), "cap_id=" + captchaid + "&HTTP_REFERER=&ent_code=" + Encoding.urlEncode(code));
            if (br.containsHTML("captcha\\.php\\?cap_id=")) {
                continue;
            }
            break;
        }
        if (br.containsHTML("captcha\\.php\\?cap_id=")) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        finallink = br.getRegex("name=\"next\" action=\"(https[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
