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

    private static final String CAPTCHATEXT = "/captcha/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling */
        if (br.getRedirectLocation() != null) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String cryptedlink = br.getRegex("\"(/download\\.php\\?ass=.*?\\&id=\\d+)\"").getMatch(0);
        if (cryptedlink == null) return null;
        cryptedlink = "http://www.zaycev.net" + cryptedlink;
        br.getPage(cryptedlink);
        if (br.containsHTML(CAPTCHATEXT)) {
            for (int i = 0; i <= 5; i++) {
                // Captcha handling
                String captchaID = getCaptchaID();
                if (captchaID == null) return null;
                String code = getCaptchaCode("http://www.zaycev.net/captcha/" + captchaID + "/", param);
                String captchapage = cryptedlink + "&captchaId=" + captchaID + "&text_check=" + code + "&ok=%F1%EA%E0%F7%E0%F2%FC";
                br.getPage(captchapage);
                if (br.containsHTML(CAPTCHATEXT)) continue;
                break;
            }
            if (br.containsHTML(CAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
        } else {
            String code = br.getRegex("<label>Ваш IP</label><span class=\"readonly\">[0-9\\.]+</span></div><input value=\"(.*?)\"").getMatch(0);
            String captchaID = getCaptchaID();
            if (code == null || captchaID == null) return null;
            String captchapage = cryptedlink + "&captchaId=" + captchaID + "&text_check=" + code + "&ok=%F1%EA%E0%F7%E0%F2%FC";
            br.getPage(captchapage);
        }
        String finallink = br.getRegex("http-equiv=\"Content-Type\"/><meta content=\"\\d+; URL=(http.*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("то нажмите на эту <a href=\\'(http.*?)\\'").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://dl\\.zaycev\\.net/[a-z0-9-]+/\\d+/\\d+/.*?)\"").getMatch(0);
        }
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        return decryptedLinks;
    }

    private String getCaptchaID() {
        String captchaID = br.getRegex("name=\"id\" type=\"hidden\"/><input value=\"(\\d+)\"").getMatch(0);
        if (captchaID == null) captchaID = br.getRegex("\"/captcha/(\\d+)").getMatch(0);
        return captchaID;
    }
}
