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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkblur.com" }, urls = { "http://[\\w\\.]*?linkblur\\.com/(\\?http://.+|.*?/[a-zA-Z0-9=/]+)" }, flags = { 0 })
public class LnkBlurCom extends PluginForDecrypt {

    public LnkBlurCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String finallink = null;
        String finallinkRegex = "<iframe.*?src=\"(http.*?)\"";
        String finallinkRegex2 = "noresize src=\"(http.*?)\"";
        if (parameter.contains("-prot") || parameter.contains("/prot/")) {
            Form continueForm = br.getForm(0);
            if (continueForm == null) return null;
            br.submitForm(continueForm);
            finallink = br.getRegex(finallinkRegex).getMatch(0);
        } else if (parameter.contains("/crypt/")) {
            finallink = br.getRegex("\"(http://linkblur\\.com/prot/.*?)\"").getMatch(0);
            if (finallink == null) finallink = br.getRegex(finallinkRegex2).getMatch(0);
        } else if (parameter.contains("-captcha") || parameter.contains("/captcha/")) {
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML("api.recaptcha.net")) {
                    logger.info("Captcha wrong count = " + i);
                    continue;
                }
                break;
            }
            if (br.containsHTML("api.recaptcha.net")) {
                logger.warning("The user entered too many wrong captchas, throwing exeption!");
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            finallink = br.getRegex(finallinkRegex).getMatch(0);
        } else {
            finallink = br.getRegex(finallinkRegex2).getMatch(0);
        }
        if (finallink == null) {
            logger.warning("Decrypter is defect, link = " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
