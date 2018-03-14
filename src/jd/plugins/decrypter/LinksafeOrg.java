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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linksafe.org" }, urls = { "https?://(?:www\\.)?linksafe\\.org/folder/[A-Za-z0-9]+" })
public class LinksafeOrg extends PluginForDecrypt {
    public LinksafeOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* First - handle captcha - captcha-type is usually different for every attempt!! */
        Form captchaForm = this.br.getFormByInputFieldKeyValue("do", "captcha");
        int counter = 0;
        while (counter <= 4 && captchaForm != null) {
            final String captchaType = captchaForm.getInputField("captcha_driver").getValue();
            if (captchaType.equalsIgnoreCase("simplecaptcha")) {
                final String c = this.getCaptchaCode("/captcha/simplecaptcha", param);
                captchaForm.put("simplecaptcha", Encoding.urlEncode(c));
            } else if (captchaType.equalsIgnoreCase("circlecaptcha")) {
                final File file = this.getLocalCaptchaFile();
                getCaptchaBrowser(br).getDownload(file, "/captcha/circlecaptcha");
                final ClickedPoint cp = getCaptchaClickedPoint(getHost(), file, param, null, "Click the open circle");
                captchaForm.put("button.x", cp.getX() + "");
                captchaForm.put("button.y", cp.getY() + "");
            } else if (captchaType.equalsIgnoreCase("recaptcha")) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            } else if (captchaType.equalsIgnoreCase("solvemedia")) {
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
            }
            this.br.submitForm(captchaForm);
            captchaForm = this.br.getFormByInputFieldKeyValue("do", "captcha");
            counter++;
        }
        if (captchaForm != null) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        /* Second - handle password */
        counter = 0;
        Form passwordForm = this.br.getFormByInputFieldKeyValue("do", "password");
        while (passwordForm != null && counter <= 2) {
            final String passCode = getUserInput("Password?", param);
            passwordForm.put("password", Encoding.urlEncode(passCode));
            this.br.submitForm(passwordForm);
            passwordForm = this.br.getFormByInputFieldKeyValue("do", "password");
            counter++;
        }
        if (passwordForm != null) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        /* Third - Decrypt links */
        this.br.setFollowRedirects(false);
        String fpName = br.getRegex("<h3>([^<>]+)</h3>").getMatch(0);
        final String[] links = br.getRegex("(https?://(?:www\\.)?linksafe\\.org/link/[^<>\"\\']+)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            this.br.getPage(singleLink);
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null) {
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
