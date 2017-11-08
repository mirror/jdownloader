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
import jd.http.Browser;
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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mirrorace.com" }, urls = { "https?://(?:www\\.)?mirrorace.com/m/[A-Za-z0-9]+" })
public class MirroraceCom extends PluginForDecrypt {
    public MirroraceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>\\s*(?:Download)?\\s*([^<]*?)\\s*(?:-\\s*MirrorAce)?\\s*</title>").getMatch(0);
        final String[] links = br.getRegex("\"(https?://mirrorace\\.com/m/[A-Za-z0-9]+/\\d+\\?t=[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        logger.info("Links found: " + links.length + " for " + parameter);
        for (final String singleLink : links) {
            // logger.info("singleLink: " + singleLink);
            if (this.isAbort()) {
                return decryptedLinks;
            }
            br.getPage(singleLink);
            // Sometimes we get g-recaptcha here
            int counter = -1;
            int retry = 9;
            while (counter++ < retry && br.containsHTML("g-recaptcha")) {
                Form captchaForm = null;
                final Form[] allForms = br.getForms();
                if (allForms != null && allForms.length != 0) {
                    for (final Form aForm : allForms) {
                        if (aForm.containsHTML("captcha")) {
                            captchaForm = aForm;
                            break;
                        }
                    }
                }
                final String captcha = captchaForm != null ? captchaForm.getRegex("(/captcha/[^<>\"]*?)\"").getMatch(0) : null;
                if (captchaForm != null && captchaForm.containsHTML("=\"g-recaptcha\"")) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    submitForm(captchaForm);
                } else {
                    logger.info("Captcha is detected but not g-recaptcha");
                    throw new DecrypterException("Decrypter broken for link: " + parameter);
                }
            }
            if (counter == retry && br.containsHTML("g-recaptcha")) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            final String finallink = br.getRegex("<a class=\"uk\\-button uk\\-button\\-primary\" href=\"(http[^<>\"]+)").getMatch(0);
            logger.info("\r\nfinallink: " + finallink + "\n for singleLink: " + singleLink);
            if (finallink == null) {
                // return null;
                // throw new DecrypterException("Decrypter broken for link: " + parameter);
                continue;
            }
            final DownloadLink dl;
            if (finallink.contains("mirrorace.com")) {
                final Browser brc = br.cloneBrowser();
                brc.setFollowRedirects(false);
                brc.getPage(finallink);
                if (brc.getRedirectLocation() != null) {
                    logger.info("External link: " + brc.getRedirectLocation());
                    dl = createDownloadlink(brc.getRedirectLocation());
                } else {
                    continue;
                }
            } else {
                dl = createDownloadlink(finallink);
            }
            decryptedLinks.add(dl);
            distribute(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private final void submitForm(final Form form) throws Exception {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(form);
        // cleanUpHTML();
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}
