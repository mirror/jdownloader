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
import java.util.LinkedHashMap;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pubiza.com" }, urls = { "https?://(?:www\\.)?(link\\.tl|pubiza\\.com|lnkload\\.com|lnk\\.parts)/[A-Za-z0-9\\-]{4,}" })
public class PubizaCom extends antiDDoSForDecrypt {
    public PubizaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser ajax = null;

    private void ajaxPostPage(final String url, final LinkedHashMap<String, String> param) throws Exception {
        ajax = br.cloneBrowser();
        ajax.getHeaders().put("Accept", "*/*");
        ajax.getHeaders().put("Connection-Type", "application/x-www-form-urlencoded");
        ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        postPage(ajax, url, param);
    }

    /**
     * Important note: Via browser the videos are streamed via RTMP (maybe even in one part) but with this method we get HTTP links which is
     * fine.
     */
    // NOTE: Similar plugins: BcVc, AdliPw, AdcrunCh,
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        getPage(parameter);
        /* Check for direct redirect */
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            redirect = br.getRegex("top\\.location\\.href = \"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (redirect != null) {
            if (!this.canHandle(redirect)) {
                decryptedLinks.add(createDownloadlink(redirect));
                return decryptedLinks;
            } else {
                br.followRedirect(true);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        final Form form1 = br.getFormbyProperty("id", "display_go_form");
        if (form1 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        form1.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.submitForm(form1);
        final String finallink = br.getRegex("goToUrl\\s*\\(\"(https?://[^\"]+)\"\\)").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(this.createDownloadlink(finallink));
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }
}