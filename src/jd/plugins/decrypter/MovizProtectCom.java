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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 32094 $", interfaceVersion = 2, names = { "moviz-protect.com" }, urls = { "https?://(www\\.)?moviz\\-protect\\.com/go\\.php\\?t=[a-zA-Z0-9_/\\+\\=\\-%]+" })
public class MovizProtectCom extends antiDDoSForDecrypt {

    public MovizProtectCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.setFollowRedirects(false);
        getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String initial_url = this.br.getURL();
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        postPage(br.getURL(), "envoyer=Envoyer&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));

        final String slider_captcha_answer = Encoding.urlEncode(getSoup());
        /* Handle slider captcha */
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("/php/Qaptcha.jquery.php", "action=qaptcha&qaptcha_key=" + slider_captcha_answer);
        final String error = PluginJSonUtils.getJsonValue(this.br, "error");
        if (error == null || "true".equals(PluginJSonUtils.getJsonValue(this.br, "error"))) {
            /* This should never happen! */
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        postPage(initial_url, "submit=Continuer&" + slider_captcha_answer);
        if (br.containsHTML("URL Invalide\\!")) {
            /* Finally after 3 steps we can get to know that our url is offline - yeey! */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final String url = br.getRedirectLocation();
        if (url == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(url));

        return decryptedLinks;
    }

    private String getSoup() {
        final Random r = new Random();
        final String soup = "azertyupqsdfghjkmwxcvbn23456789AZERTYUPQSDFGHJKMWXCVBN_-#@";
        String v = "";
        for (int i = 0; i < 31; i++) {
            v = v + soup.charAt(r.nextInt(soup.length()));
        }
        return v;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}