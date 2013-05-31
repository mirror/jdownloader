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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "urlguard.org" }, urls = { "http://(www\\.)?urlguard\\.org/[a-z0-9]+" }, flags = { 0 })
public class UrlGuardOrg extends PluginForDecrypt {

    public UrlGuardOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // Link offline
        if ("http://urlguard.org/".equals(br.getURL())) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Invalid link
        if (br.containsHTML(">403 Forbidden<|>404 Not Found<")) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
            final String id = br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            rc.setId(id);
            if (id == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            rc.load();
            for (int i = 0; i <= 3; i++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, param);
                br.postPage(br.getURL(), "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c));
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    rc.reload();
                    continue;
                }
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String singleLinkframe = br.getRegex("\"(/frame\\.php\\?\\d+)\"").getMatch(0);
        if (singleLinkframe != null) {
            br.getPage("http://urlguard.org" + singleLinkframe);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(finallink)));
        } else {
            final String allLinks = br.getRegex("var options = eval\\((.*?)</script>").getMatch(0);
            if (allLinks == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            String[] links = new Regex(allLinks, "\"([^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : links)
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(singleLink)));
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}