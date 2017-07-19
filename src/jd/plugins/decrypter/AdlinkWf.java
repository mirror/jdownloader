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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "adlink.wf" }, urls = { "https?://(?:www\\.)?adlink\\.wf/[A-Za-z0-9]{9}" })
public class AdlinkWf extends PluginForDecrypt {
    public AdlinkWf(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Invalid Link\\.")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        boolean success = false;
        for (int i = 0; i <= 2; i++) {
            final Form captchaForm = this.br.getForm(0);
            final String captchaURL = captchaForm.getRegex("(/captcha\\.php\\?cap_id=\\d+)").getMatch(0);
            if (captchaURL == null) {
                success = true;
                break;
            }
            final String code = this.getCaptchaCode(captchaURL, param);
            captchaForm.put("ent_code", code);
            br.submitForm(captchaForm);
        }
        if (!success) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String finallink = this.br.getRegex("name=\"next\"[^<>]*?action=\"(http[^<>\"]+)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
