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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fas.li" }, urls = { "https?://(?:www\\.)?(?:fas\\.li|likn\\.xyz|sloomp\\.space)/[A-Za-z0-9]+" })
public class FasLi extends antiDDoSForDecrypt {
    public FasLi(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        /* 2019-10-30: Most URLs will show up as online at first - status is only visible after captcha. */
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form continueForm = br.getFormbyProperty("id", "skip-form");
        if (continueForm == null) {
            continueForm = br.getFormbyProperty("id", "skip");
        }
        if (continueForm == null) {
            logger.info("Failed to find captchaform");
            return null;
        }
        /* 2021-01-18: Captcha not required anymore? */
        if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(this.br)) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            continueForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        } else {
            logger.info("No captcha required");
        }
        br.setFollowRedirects(false);
        this.submitForm(continueForm);
        String finallink = br.getRedirectLocation();
        if (finallink == null) {
            /* Sometimes a 2nd step is required */
            continueForm = br.getFormbyProperty("id", "skip");
            if (continueForm == null) {
                /*
                 * 2019-10-30: Probably offline URL. They will display/embed this picture: <img src="https://i.imgflip.com/1lvz6g.jpg"
                 * class="img-responsive" alt="">
                 */
                logger.info("Failed to find continueForm");
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            submitForm(continueForm);
            finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Failed to find finallink");
                return null;
            } else if (this.canHandle(finallink) && finallink.matches(".+/deleted$")) {
                /* 2021-01-18 */
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
