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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "try2link.com" }, urls = { "http://[\\w\\.]*?try2link\\.com/[a-zA-Z0-9]+" }, flags = { 0 })
public class Try2LCm extends PluginForDecrypt {

    public Try2LCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String continueHereRegex = "content=\"\\d+; URL=(.*?)\"";
    public String captchaText = "sec_image.php\\?";
    public String finallinkRegex = "name=\"ifram\" src=\"(.*?)\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (!br.containsHTML("<a href=\"http://www.try2link.com\">Try2Link</a>")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (br.containsHTML(captchaText)) {
            for (int i = 0; i < 5; i++) {
                Form captchaForm = br.getForm(0);
                String captchaurl = br.getRegex("\"(sec_image\\.php\\?code=.*?)\"").getMatch(0);
                if (captchaurl == null || captchaForm == null) return null;
                captchaurl = "http://www.try2link.com/" + captchaurl;
                String code = getCaptchaCode(captchaurl, param);
                captchaForm.put("confirm_image", code);
                br.submitForm(captchaForm);
                if (br.containsHTML(captchaText)) continue;
                break;
            }
            if (br.containsHTML(captchaText)) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String continueHere = br.getRegex(continueHereRegex).getMatch(0);
        if (continueHere == null) return null;
        br.getPage("http://www.try2link.com/" + continueHere);
        String finallink = br.getRegex(finallinkRegex).getMatch(0);
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}