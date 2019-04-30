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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "short.am" }, urls = { "https?://(?:www\\.)?s(ho)?rt\\.am/(?!images|include|login|templates|js|assets|join|manage)[A-Za-z0-9]+" })
public class ShortAm extends antiDDoSForDecrypt {
    public ShortAm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form continueform = br.getFormbyKey("_token");
        if (continueform == null) {
            continueform = br.getForm(0);
            if (continueform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (false) {
            // https://github.com/adsbypasser/adsbypasser/blob/master/src/sites/link/short.am.js
            String sitekey = br.getRegex("sitekey\\s*?:\\s*?\"([^<>\"]+)\"").getMatch(0);
            if (sitekey == null) {
                /* 2018-08-07: Fallback */
                sitekey = "6LevHzcUAAAAAJgJHvtcVzlRxasZsJgZWJI5ZUvF";
            }
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, sitekey).getToken();
            continueform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        } else {
            sleep(2000l, param);
        }
        submitForm(continueform);
        continueform = br.getForm(0);
        if (continueform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        sleep(6123l, param);
        br.setFollowRedirects(false);
        submitForm(continueform);
        final String finallink = br.getRegex("window\\.location\\.replace\\('(http[^<>\"\\']+)'\\)").getMatch(0);
        if (finallink == null) {
            /* Basically via browser there just isn't any redirect present if a user uses an invalid url. */
            if (br.containsHTML("Please wait \\d+s")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
