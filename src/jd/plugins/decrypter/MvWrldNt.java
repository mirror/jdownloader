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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mov-world.net" }, urls = { "http://[\\w\\.]*?mov-world\\.net/.*?/.*?-\\d+\\.html" }, flags = { 0 })
public class MvWrldNt extends PluginForDecrypt {

    public MvWrldNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        Boolean decrypterUnfinished = true;
        if (decrypterUnfinished) {
            logger.warning("The mov-wrld decrypter is still in development and does not work at the moment!!");
            return null;
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("<h1>Dieses Release ist nur noch bei <a")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String password = br.getRegex("class=\"password\">Password: (.*?)</p>").getMatch(0);
        String captchaUrl = br.getRegex("\"(/captcha/[a-zA-Z0-9]+\\.gif)\"").getMatch(0);
        Form captchaForm = br.getForm(0);
        if (captchaUrl == null || captchaForm == null) return null;
        Browser brc = br.cloneBrowser();
        captchaUrl = "http://mov-world.net" + captchaUrl;
        File captchaFile = getLocalCaptchaFile();
        brc.getDownload(captchaFile, captchaUrl);
        String code = null;
        for (int i = 0; i <= 5; i++) {
            if (i > 0) {
                // Recognition failed, ask the user!
                code = getCaptchaCode(null, captchaFile, param);
            } else {
                code = getCaptchaCode("mov-world.net", captchaFile, param);
            }
            captchaForm.put("code", code);
            br.submitForm(captchaForm);
            if (br.containsHTML("\"Der Sicherheits Code")) continue;
            break;
        }
        if (br.containsHTML("\"Der Sicherheits Code")) throw new DecrypterException(DecrypterException.CAPTCHA);

        // Continue here, you gotta find out how to decrypt those links^^

        String[] links = br.getRegex("").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links) {
            DownloadLink downLink = createDownloadlink(dl);
            downLink.addSourcePluginPassword("mov-world.net");
            if (password != null && !password.equals("")) downLink.addSourcePluginPassword(password.trim());
            decryptedLinks.add(downLink);
        }

        return decryptedLinks;
    }

}
