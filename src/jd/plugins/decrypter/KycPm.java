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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kyc.pm" }, urls = { "http://(www\\.)?kyc\\.pm/[A-Za-z0-9]+" }) 
public class KycPm extends PluginForDecrypt {

    public KycPm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.toString().length() < 30) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        String dllink = this.br.getRedirectLocation();
        if (dllink != null && dllink.contains("kyc.pm/")) {
            /* Should never happen */
            br.getPage(dllink);
            dllink = null;
        }
        if (dllink == null) {
            boolean failed = true;
            for (int i = 0; i <= 2; i++) {
                final Form dlform = br.getForm(0);
                final String captchaurl = br.getRegex("(/captcha\\.php\\?[^<>\"]*?)\"").getMatch(0);
                if (dlform == null || captchaurl == null) {
                    return null;
                }
                final String code = getCaptchaCode(captchaurl, param);
                dlform.put("ent_code", code);
                br.submitForm(dlform);
                if (br.containsHTML("/captcha\\.php")) {
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }

            dllink = br.getRegex("name=\"next\" action=\"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(dllink));

        return decryptedLinks;
    }

}
