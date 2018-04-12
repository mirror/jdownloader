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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ur.ly" }, urls = { "https?://(?:www\\.)?ur\\.ly/[A-Za-z0-9]+" })
public class UrLy extends PluginForDecrypt {
    public UrLy(PluginWrapper wrapper) {
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
        final Form captchaForm = br.getFormbyProperty("id", "form-captcha");
        if (captchaForm == null) {
            return null;
        }
        final String reCaptchaKey = br.getRegex("sitekey\\s*?:\\s*?\"([^<>\"]+)\"").getMatch(0);
        if (reCaptchaKey == null) {
            return null;
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
        captchaForm.put("g-recaptcha-response", recaptchaV2Response);
        br.submitForm(captchaForm);
        String finallink = this.br.getRegex("\"(https?://[^<>\"/]+/goii/[^<>\"]+)\"").getMatch(0);
        if (finallink == null) {
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
