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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kipurl.com" }, urls = { "http://(www\\.)?kipurl\\.com/\\d/[a-z0-9]+" }, flags = { 0 })
public class KipUrlCom extends PluginForDecrypt {

    public KipUrlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.toString().length() < 100) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        for (int i = 0; i <= 3; i++) {
            final Form captchaForm = br.getForm(0);
            if (captchaForm == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String code = getCaptchaCode("http://kipurl.com/captcha/" + captchaForm.getInputField("code").getValue(), param);
            captchaForm.put("captcha", code);
            br.submitForm(captchaForm);
            if (br.containsHTML("\"/captcha/")) continue;
            break;
        }
        if (br.containsHTML("\"/captcha/")) throw new DecrypterException(DecrypterException.CAPTCHA);
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }
}
