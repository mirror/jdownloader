//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zaycev.net" }, urls = { "http://[\\w\\.]*?zaycev\\.net/pages/[0-9]+/[0-9]+\\.shtml" }, flags = { 0 })
public class ZyvNt extends PluginForDecrypt {

    public ZyvNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CRYPTEDLINK = ".*?download\\.php.*?id=[0-9]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.getRedirectLocation() != null) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String cryptedlink = null;
        String[] lol = HTMLParser.getHttpLinks(br.toString(), "");
        for (String link : lol) {
            if (link.matches(CRYPTEDLINK)) {
                cryptedlink = link;
                break;
            }
        }
        if (cryptedlink == null) return null;
        br.getPage(cryptedlink);
        String ass = null;
        for (int i = 0; i <= 5; i++) {
            // Captcha handling
            String captchaurl = null;
            String captchapart = br.getRegex("\"(/captcha\\.php\\?id=[a-z0-9]+)\"").getMatch(0);
            if (captchapart != null) {
                captchaurl = "http://www.zaycev.net" + captchapart;
            } else {
                captchapart = br.getRegex("captcha_id\" value=\"([a-z0-9]+)\"").getMatch(0);
                if (captchapart != null) captchaurl = "http://www.zaycev.net/captcha.php?id=" + captchapart;
            }
            String id = br.getRegex("name=\"id\" value=\"(.*?)\"").getMatch(0);
            ass = br.getRegex("name=\"ass\" value=\"(.*?)\"").getMatch(0);
            String phpssid = br.getCookie("http://www.zaycev.net", "PHPSESSID");
            String captchaid = new Regex(captchapart, "id=([a-z0-9]+)").getMatch(0);
            if (phpssid == null || captchaid == null || captchaurl == null || ass == null || id == null) return null;
            ass = ass.replace("amp;", "");
            String ass2 = ass.replace("amp;", "").replace(" ", "+");
            String code = getCaptchaCode(captchaurl, param);
            br.getPage("http://www.zaycev.net/download.php?PHPSESSID=" + phpssid + "&id=" + id + "&ass=" + ass2 + "&captcha_id=" + captchaid + "&text_check=" + code + "&ok=Ñêà÷àòü");
            if (br.containsHTML("вы ввели не верный код")) continue;
            break;
        }
        if (br.containsHTML("вы ввели не верный код")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String finallink = br.getRegex("Refresh Content=\"2;URL=(http.*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("нажмите на эту <a href=\"(http.*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("(http://dl\\.zaycev\\.net/[a-z0-9]+/[0-9]+/[0-9]+/.*?)\"").getMatch(0);
            }
        }
        if (finallink == null) return null;
        // The replace fixes a problem with the filename
        String replace = new Regex(finallink, "dl\\.zaycev\\.net/[a-z0-9]+/[0-9]+/[0-9]+/([\\w\\.%-]+)").getMatch(0);
        if (replace != null) {
            finallink = finallink.replace(replace, ass);
        } else {
            logger.warning("Replace regex is defect!");
        }
        decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        return decryptedLinks;
    }
}
