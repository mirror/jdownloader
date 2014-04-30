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
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bergfiles.com" }, urls = { "http://(www\\.)?bergfiles\\.com/(i|description)/[a-z0-9]+" }, flags = { 0 })
public class BergFilesCom extends PluginForDecrypt {

    public BergFilesCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(parameter);
        if (parameter.matches("http://(www\\.)?bergfiles\\.com/i/[A-Za-z0-9]+")) {
            if (br.containsHTML("<title>File \\. Only direct download")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            final String fid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
            for (int i = 0; i <= 3; i++) {
                String code = getCaptchaCode("http://www.bergfiles.com/captcha/" + fid, param);
                ajaxBR.postPage(parameter, "id=" + fid + "&action=go&captcha=" + code);
                if (ajaxBR.containsHTML("Bad code from picture")) continue;
                break;
            }
            if (ajaxBR.containsHTML("Bad code from picture")) throw new DecrypterException(DecrypterException.CAPTCHA);
            final String finallink = ajaxBR.getRegex("<a target=_blank href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (ajaxBR.containsHTML("target=_blank href=\"\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String[] links = br.getRegex("<td class=\"down\\-cell\" valign=\"top\"><a href=\"(https?://[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links)
                decryptedLinks.add(createDownloadlink(singleLink));
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}