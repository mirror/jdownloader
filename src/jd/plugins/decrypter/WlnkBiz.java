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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wlnk.biz" }, urls = { "http://(?:www\\.)?wlnk\\.biz/[A-Za-z0-9]+" }) 
public class WlnkBiz extends PluginForDecrypt {

    public WlnkBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form form = this.br.getFormbyKey("_token");
        if (form == null) {
            form = this.br.getForm(0);
        }
        if (form == null) {
            return null;
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        this.br.submitForm(form);
        if (this.br.containsHTML("La page que vous avez demandée n'a pas été trouvée")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String finallink = this.br.getRegex("<b>Lien :</b></td>.*?href=\"(http[^<>\"]*?)\"").getMatch(0);
        if (finallink == null || finallink.matches(".+wlnk\\.biz.+")) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
