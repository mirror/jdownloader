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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginException;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision: 37760 $", interfaceVersion = 3, names = { "caat.site" }, urls = { "https?://(?:www\\.)?caat\\.site/[A-Za-z0-9]+" })
public class CaatSite extends antiDDoSForDecrypt {
    public CaatSite(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br = new Browser();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("was not found on this server")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Form captcha = br.getForm(0);
        if (captcha != null && captcha.containsHTML("invisibleCaptchaShortlink")) {
            final String recaptchaV2Response = getCaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            captcha.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(captcha);
            // Always only {"status":"error","message":"Bad Request.","url":""} after the second request
            final Form captcha2 = br.getForm(0);
            br.submitForm(captcha2);
        }
        final String[] links = br.getRegex("<a href=\"([^\"]*)\" class=\"btn btn-primary btn-goo get-link\">Proceed").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        return decryptedLinks;
    }

    protected CaptchaHelperCrawlerPluginRecaptchaV2 getCaptchaHelperCrawlerPluginRecaptchaV2(CaatSite caatSite, Browser br) throws PluginException {
        return new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, this.getReCaptchaKey()) {
            @Override
            public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractCaptchaHelperRecaptchaV2.TYPE getType() {
                return TYPE.INVISIBLE;
            }
        };
    }

    public String getReCaptchaKey() {
        /* 2020-01-05 */
        return "6Ldzj74UAAAAAAVQ7-WIlUUfNGJFaKdgRxA7qH94";
    }
}
