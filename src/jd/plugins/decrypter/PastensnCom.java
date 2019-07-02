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

import java.io.File;
import java.util.ArrayList;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pastensn.com" }, urls = { "https?://(?:www\\.)?pastensn\\.com/[A-Za-z0-9]+" })
public class PastensnCom extends PluginForDecrypt {
    public PastensnCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: pastebin
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter + "?full-page");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form captchaForm = null;
        for (int i = 0; i <= 4; i++) {
            captchaForm = br.getFormByInputFieldKeyValue("SubmitSM", " Enviar ");
            if (captchaForm == null) {
                try {
                    captchaForm = br.getForms()[br.getForms().length - 1];
                } catch (final Throwable e) {
                }
            }
            if (captchaForm == null) {
                break;
            }
            if (captchaForm.containsHTML("adcopy_challenge")) {
                final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                File cf = null;
                try {
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                } catch (final Exception e) {
                    if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                    }
                    throw e;
                }
                final String code = getCaptchaCode("solvemedia", cf, param);
                final String chid = sm.getChallenge(code);
                captchaForm.put("adcopy_challenge", chid);
                captchaForm.put("adcopy_response", "manual_challenge");
                br.submitForm(captchaForm);
            } else {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                captchaForm.put("g-recaptcha-response", recaptchaV2Response);
                br.submitForm(captchaForm);
                captchaForm = null;
                break;
            }
            if (br.containsHTML("Paste Not Avaible")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
        }
        if (captchaForm != null) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final String plaintxt = br.toString();
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no links in link: " + parameter);
            return decryptedLinks;
        }
        for (final String dl : links) {
            if (!new Regex(dl, getSupportedLinks()).matches()) {
                decryptedLinks.add(createDownloadlink(dl));
            }
        }
        return decryptedLinks;
    }
}