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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "camvault.xyz" }, urls = { "https?://(?:www\\.)?camvault\\.xyz/download/[A-Za-z0-9\\-_]+\\-\\d+\\.html" })
public class CamvaultXyz extends PluginForDecrypt {

    public CamvaultXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = null;
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
            br.postPage("/gallery/megadownload", "captcha=&token=" + Encoding.urlEncode(videoToken));
            final String reCaptchaSiteKey = PluginJSonUtils.getJsonValue(this.br, "sitekey");
            if (!StringUtils.isEmpty(reCaptchaSiteKey)) {
                /* Usually a reCaptchaV2 is required! */
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, reCaptchaSiteKey).getToken();
                br.postPage("/gallery/megadownload", "captcha=" + Encoding.urlEncode(recaptchaV2Response) + "&token=" + Encoding.urlEncode(videoToken));
            }
            br.getRequest().setHtmlCode(Encoding.unescape(this.br.toString()));
            final String[] dllinks = this.br.getRegex("download\\-link\"><a href=\"(https[^<>\"]+)\"").getColumn(0);
            for (final String dllink : dllinks) {
                decryptedLinks.add(createDownloadlink(dllink));
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
