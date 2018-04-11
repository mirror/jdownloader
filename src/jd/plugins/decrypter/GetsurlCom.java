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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "getsurl.com" }, urls = { "https?://(?:www\\.)?(?:gslink\\.co|gsul\\.me|gsur\\.in|gurl\\.ly|gsurl\\.in|gsurl\\.me|g5u\\.pw)/[A-Za-z0-9]+" })
public class GetsurlCom extends PluginForDecrypt {
    public GetsurlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String redirect = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+;URL=\\'(/capatcha/\\?i=[A-Za-z0-9]+)\\'\"").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        final Form continueForm = br.getForm(0);
        if (br.containsHTML("data\\-sitekey")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            continueForm.put("g-recaptcha-response", recaptchaV2Response);
        }
        br.submitForm(continueForm);
        // br.postPage(br.getURL(), "sub=Continue");
        String finallink = this.br.getRegex("(https?://[^<>\"]+/o\\?i=\\d+)").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.setFollowRedirects(false);
        br.getPage(finallink);
        finallink = br.getRedirectLocation();
        if (finallink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
