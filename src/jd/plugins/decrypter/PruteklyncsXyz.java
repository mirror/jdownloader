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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pruteklyncs.xyz" }, urls = { "https?://(?:www\\.)?(pruteklyncs\\.xyz|dirtybandit\\.com)/[A-Za-z0-9\\-]+($|/|\\?)" })
public class PruteklyncsXyz extends PluginForDecrypt {
    public PruteklyncsXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2021-03-17: Preventive measure */
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*Page Not Found\\s*<") || br.toString().length() <= 100) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String iframeURL = br.getRegex("<iframe src\\s*=\\s*\"(https?://[^\"]+)\">").getMatch(0);
        if (iframeURL != null) {
            if (!this.canHandle(iframeURL)) {
                decryptedLinks.add(createDownloadlink(iframeURL));
                return decryptedLinks;
            }
            br.getPage(iframeURL);
            if (br.containsHTML(">\\s*Page Not Found\\s*<") || br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
        }
        final String redirect = br.getRegex("http-equiv=\"refresh\" content=\"0; URL=(https?://[^\"]+)\"").getMatch(0);
        if (redirect != null) {
            /** 2021-02-01 */
            logger.info("Found additional redirect");
            br.getPage(redirect);
        }
        if (br.containsHTML("passster-captcha-js") && br.containsHTML(">\\s*Protected Area\\s*<")) {
            /* 2020-10-26: Cheap clientside captcha */
            final String nonce = PluginJSonUtils.getJson(br, "nonce");
            final String post_id = PluginJSonUtils.getJson(br, "post_id");
            final String captchaID = br.getRegex("data-psid=\"([^\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(nonce) || StringUtils.isEmpty(post_id)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            Form captchaForm = br.getFormbyProperty("class", "captcha-form");
            if (captchaForm == null) {
                captchaForm = new Form();
                captchaForm.setMethod(MethodType.POST);
            }
            captchaForm.put("action", "validate_input");
            captchaForm.put("nonce", nonce);
            captchaForm.put("captcha", "success");
            captchaForm.put("post_id", post_id);
            captchaForm.put("type", "captcha");
            // captchaForm.put("protection", "");
            /* 2021-03-17 */
            captchaForm.put("protection", "full");
            captchaForm.put("elementor_content", "");
            captchaForm.put("captcha_id", Encoding.urlEncode(captchaID));
            br.getHeaders().put("x-requested-with", "XMLHttpRequest");
            captchaForm.setAction("/wp-admin/admin-ajax.php");
            // br.postPage("/wp-admin/admin-ajax.php", query);
            this.br.submitForm(captchaForm);
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
        }
        String[] links = br.getRegex("href=\"(https?://[^\"]+)\" target=\"_blank\"").getColumn(0);
        if (links.length == 0) {
            /* Fallback */
            links = HTMLParser.getHttpLinks(br.toString(), br.getURL());
        }
        if (links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (!this.canHandle(singleLink)) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        return decryptedLinks;
    }
}
