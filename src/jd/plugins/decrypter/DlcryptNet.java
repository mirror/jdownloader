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

import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dlcrypt.net" }, urls = { "https?://(?:www\\.)?dlcrypt\\.net/(gets|views)/([A-Za-z0-9]+)" })
public class DlcryptNet extends PluginForDecrypt {
    public DlcryptNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_VIEWS = "https?://(?:www\\.)?dlcrypt\\.net/views/([A-Za-z0-9]+)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String type = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (type.equalsIgnoreCase("gets")) {
            crawlGets(param, decryptedLinks);
        } else {
            crawlViews(param, decryptedLinks);
        }
        return decryptedLinks;
    }

    /**
     * Handles captcha protection.
     *
     * @throws PluginException
     * @throws DecrypterException
     * @throws InterruptedException
     */
    private void crawlGets(final CryptedLink param, final ArrayList<DownloadLink> decryptedLinks) throws IOException, PluginException, DecrypterException, InterruptedException {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return;
        }
        /* First, try to skip captcha & waittime (2020-11-09: Captcha is skippable!). */
        final String viewsURL = br.getRegex("(/views/[A-Za-z0-9]+)").getMatch(0);
        if (viewsURL != null) {
            logger.info("Skipping captcha");
            crawlViews(new CryptedLink(br.getURL(viewsURL).toString()), decryptedLinks);
            return;
        } else {
            /* 2020-11-09: All untested (and not needed atm.) */
            logger.info("Handling cpatcha");
            Form captchaForm = br.getFormbyProperty("id", "recaptcha");
            if (captchaForm == null) {
                for (final Form form : br.getForms()) {
                    if (form.containsHTML("id=\"recaptcha\"")) {
                        captchaForm = form;
                        break;
                    }
                }
            }
            if (captchaForm == null) {
                captchaForm = br.getForm(0);
            }
            if (captchaForm == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.setFollowRedirects(false);
            br.submitForm(captchaForm);
            final String redirect = br.getRedirectLocation();
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (!redirect.matches(TYPE_VIEWS)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            crawlViews(new CryptedLink(br.getURL(redirect).toString()), decryptedLinks);
        }
    }

    /**
     * Handles (optional) password protection and crawls URLs.
     *
     * @throws DecrypterException
     */
    private void crawlViews(final CryptedLink param, final ArrayList<DownloadLink> decryptedLinks) throws IOException, PluginException, DecrypterException {
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return;
        }
        Form pwForm = null;
        boolean pwSuccess = false;
        String passCode = param.getDecrypterPassword();
        int pwCounter = 0;
        do {
            pwForm = br.getFormbyKey("pass");
            if (pwForm != null) {
                /* First try preset password. */
                if (pwCounter > 0) {
                    passCode = getUserInput("Password?", param);
                }
                pwForm.put("pass", Encoding.urlEncode(passCode));
                br.submitForm(pwForm);
                pwCounter++;
                continue;
            } else {
                pwSuccess = true;
                break;
            }
        } while (!this.isAbort() && pwCounter < 3);
        if (!pwSuccess) {
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
        }
        final String[] links = br.getRegex("<a href=\"(https?://[^\"]+)\" target=\"_blank\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            if (new Regex(singleLink, this.getSupportedLinks()).matches()) {
                /* Skip URLs that would go back into this crawler. */
                continue;
            }
            final DownloadLink dl = createDownloadlink(singleLink);
            if (!StringUtils.isEmpty(passCode)) {
                /* TODO: 2020-11-09: Set this as extraction password */
            }
            decryptedLinks.add(dl);
        }
    }
}
