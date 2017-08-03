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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "camvault.xyz" }, urls = { "https?://(?:www\\.)?camvault\\.xyz/download/[A-Za-z0-9\\-_]+\\-\\d+\\.html" })
public class CamvaultXyz extends antiDDoSForDecrypt {
    public CamvaultXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[] videoTokens = br.getRegex("name=\"videoToken\" type=\"hidden\" value=\"([^<>\"]+)\"").getColumn(0);
        if (videoTokens == null || videoTokens.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (final String videoToken : videoTokens) {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            final Browser br = this.br.cloneBrowser();
            postPage(br, "/gallery/megadownload", "captcha=&token=" + Encoding.urlEncode(videoToken));
            final String reCaptchaSiteKey = PluginJSonUtils.getJsonValue(br, "sitekey");
            if (!StringUtils.isEmpty(reCaptchaSiteKey)) {
                /* Usually a reCaptchaV2 is required! */
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, reCaptchaSiteKey).getToken();
                postPage(br, "/gallery/megadownload", "captcha=" + Encoding.urlEncode(recaptchaV2Response) + "&token=" + Encoding.urlEncode(videoToken));
            }
            br.getRequest().setHtmlCode(Encoding.unicodeDecode(br.toString()));
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
            final String[] dllinks = br.getRegex("download\\-link\"><a href=\"(https?[^<>\"]+)\"").getColumn(0);
            for (final String dllink : dllinks) {
                decryptedLinks.add(createDownloadlink(dllink));
            }
        }
        return decryptedLinks;
    }
}
