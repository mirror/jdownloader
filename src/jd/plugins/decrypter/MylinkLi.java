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

import org.appwork.utils.Time;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mylink.vc" }, urls = { "https?://(?:www\\.)?(?:mylink\\.(?:li|how|cx|vc)|myl\\.li)/[A-Za-z0-9]+" })
public class MylinkLi extends antiDDoSForDecrypt {
    public MylinkLi(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // final String linkID = new Regex(param.getCryptedUrl(), "/([A-Za-z0-9]+)$").getMatch(0);
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        final String httpRedirect = br.getRegex("<meta http-equiv=\"refresh\"[^>]*url=(https://[^\"]+)\"[^>]*>").getMatch(0);
        if (httpRedirect != null) {
            this.getPage(httpRedirect);
        }
        final Form captchaForm = br.getFormbyProperty("id", "captcha");
        if (captchaForm == null) {
            logger.warning("Failed to find captchaForm");
            return null;
        }
        // final String phpsessid = br.getCookie(br.getHost(), "PHPSESSID", Cookies.NOTDELETEDPATTERN);
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        captchaForm.remove("submit");
        br.getHeaders().put("Origin", "https://mylink.vc");
        getAndSetSpecialCookie(this.br);
        submitForm(captchaForm);
        /*
         * Contains pretty much the same stuff as the first form and again our captcha result. This time, parameter "hash" is not empty.
         * "hash" usually equals our Cookie "PHPSESSID".
         */
        Form captchaForm2 = br.getFormbyProperty("id", "reCaptchaForm");
        if (captchaForm2 == null) {
            logger.warning("Failed to find captchaFollowupForm");
            return null;
        }
        /* 2nd captcha - this time, invisible reCaptchaV2 */
        final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
        final String recaptchaV2Response_2 = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {
            @Override
            public TYPE getType() {
                return TYPE.INVISIBLE;
            }
        }.getToken();
        captchaForm2.put("g-recaptcha-response", recaptchaV2Response_2);
        // getAndSetSpecialCookie(this.br);
        /* If the user needs more than 5 seconds to solve that captcha we don't have to wait :) */
        final long timeWait = 6100;
        final long waitLeft = timeWait - (Time.systemIndependentCurrentJVMTimeMillis() - timeBefore);
        if (waitLeft > 0) {
            this.sleep(waitLeft, param);
        }
        submitForm(captchaForm2);
        final Form shareForm = br.getFormbyKey("share");
        getAndSetSpecialCookie(br);
        this.submitForm(shareForm);
        /* A lot of Forms may appear here - all to force the user to share the link, bookmark their page, click on ads and so on ... */
        br.setFollowRedirects(false);
        Form goForm = null;
        int numberof404Responses = 0;
        final int maxLoops = 10;
        for (int i = 0; i <= maxLoops; i++) {
            logger.info("Loop: " + i + "/" + maxLoops);
            goForm = br.getFormbyKey("hash");
            if (goForm == null) {
                break;
            } else {
                getAndSetSpecialCookie(br);
                // goForm.remove("Continue");
                submitForm(goForm);
                /* 2021-07-08: Attempt to avoid strange error/adblock detection stuff hmm unsure about that... but it works! */
                if (br.containsHTML("<title>404</title>")) {
                    /* This should only happen once */
                    numberof404Responses++;
                    logger.info("Trying 404 avoidance | Attempt: " + numberof404Responses);
                    submitForm(goForm);
                }
            }
        }
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            if (numberof404Responses > 1) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    private void getAndSetSpecialCookie(final Browser br) {
        final String specialCookie = br.getRegex("\"/hkz\"\\);setCookie\\(\"([a-z0-9]+)\",1,").getMatch(0);
        if (specialCookie != null) {
            logger.info("Found new specialCookie: " + specialCookie);
            br.setCookie(br.getHost(), specialCookie, "1");
        } else {
            logger.info("Failed to find new specialCookie");
        }
    }
}
