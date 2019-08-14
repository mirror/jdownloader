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
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mylink.li" }, urls = { "https?://(?:www\\.)?(?:mylink\\.(?:li|how|cx)|myl\\.li)/[A-Za-z0-9]+" })
public class MylinkLi extends antiDDoSForDecrypt {
    public MylinkLi(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String linkID = new Regex(parameter, "/([A-Za-z0-9]+)$").getMatch(0);
        br = new Browser();
        br.setFollowRedirects(true);
        if (true) {
            logger.warning("This crawler does not yet work!");
            return null;
        }
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        final Form captchaForm = br.getFormbyProperty("id", "captcha");
        if (captchaForm == null) {
            logger.warning("Failed to find captchaForm");
            return null;
        }
        String debug_hash = null;
        if (captchaForm.hasInputFieldByName("hash")) {
            debug_hash = captchaForm.getInputFieldByName("hash").getValue();
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        captchaForm.put("g-recaptcha-response", recaptchaV2Response);
        captchaForm.remove("submit");
        submitForm(captchaForm);
        Form captchaFollowupForm = br.getFormbyProperty("id", "reCaptchaForm");
        if (captchaFollowupForm == null) {
            logger.warning("Failed to find captchaFollowupForm");
            return null;
        }
        captchaFollowupForm.put("g-recaptcha-response", recaptchaV2Response);
        // {
        // /* Debug test */
        // captchaFollowupForm = new Form();
        // captchaFollowupForm.setMethod(MethodType.POST);
        // captchaFollowupForm.put("uri", linkID);
        // // captchaFollowupForm.put("hash", "");
        // captchaFollowupForm.put("g-recaptcha-response", recaptchaV2Response);
        // }
        this.sleep(5001l, param);
        submitForm(captchaFollowupForm);
        // if (br.toString().length() < 100) {
        // /* 2019-08-14: Empty page: Offline or website broken?? Same happens via browser! */
        // decryptedLinks.add(this.createOfflinelink(parameter));
        // return decryptedLinks;
        // }
        Form shareForm = br.getFormbyProperty("id", "share");
        {
            /* Debug test */
            if (shareForm == null) {
                shareForm = new Form();
                shareForm.setMethod(MethodType.POST);
                shareForm.put("share", "myl.li/" + linkID);
                shareForm.put("uri", linkID);
                if (captchaFollowupForm.hasInputFieldByName("hash")) {
                    debug_hash = captchaFollowupForm.getInputFieldByName("hash").getValue();
                    shareForm.put("hash", debug_hash);
                }
            }
        }
        if (shareForm == null) {
            logger.warning("Failed to find finalForm");
            return null;
        }
        submitForm(shareForm);
        /* A lot of Forms may appear here - all to force the user to share the link, bookmark their page and so on ... */
        Form goForm = null;
        for (int i = 0; i <= 10; i++) {
            logger.info("Loop: " + i);
            goForm = br.getFormbyProperty("id", "go");
            final Form continueForm = br.getForm(0);
            if (continueForm == null || goForm != null) {
                break;
            }
            {
                /* Debug test - this should not happen, we should be 'behind' that 2nd captcha Form already! */
                if (continueForm.containsHTML("reCaptchaForm")) {
                    continueForm.put("g-recaptcha-response", recaptchaV2Response);
                }
            }
            submitForm(continueForm);
        }
        if (goForm == null) {
            logger.warning("Failed to find goForm");
            return null;
        }
        submitForm(goForm);
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
