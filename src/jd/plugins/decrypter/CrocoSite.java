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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "croco.site" }, urls = { "https?://(?:www\\.)?croco\\.site/[a-zA-Z0-9]+" })
public class CrocoSite extends antiDDoSForDecrypt {

    private static Object CTRLLOCK = new Object();

    public CrocoSite(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Mainpage: croco.me */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        Form dform = null;
        // they seem to only show recaptchav2 once!!? they track ip session (as restarting client doesn't get recaptchav2, the only cookies
        // that are cached are cloudflare and they are only kept in memory, and restarting will flush it)
        synchronized (CTRLLOCK) {
            getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            dform = br.getFormbyKey("_token");
            if (dform == null) {
                dform = br.getForm(0);
            }
            if (dform == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            dform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        }
        br.setFollowRedirects(false);
        submitForm(dform);
        String finallink = br.getRedirectLocation();
        if (inValidate(finallink)) {
            finallink = br.getRegex("href=\"(https?://[^<>\"]+)\" id=\"btn\\-main\"").getMatch(0);
            if (inValidate(finallink)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

}
