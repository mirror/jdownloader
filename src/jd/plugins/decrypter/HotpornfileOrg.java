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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hotpornfile.org" }, urls = { "https?://(?:www\\.)?hotpornfile\\.org/([^/]+)/(\\d+)" })
public class HotpornfileOrg extends PluginForDecrypt {
    public HotpornfileOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String fid = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* 2019-07-18: Stream URLs may expire which is why we don't grab them for now. */
        // br.postPage("https://www." + this.getHost() + "/wp-admin/admin-ajax.php", "action=get_stream&postId=" + fid);
        /* 2019-07-18: reCaptchaKey hardcoded */
        String recaptchaV2Response = this.getPluginConfig().getStringProperty("recaptcha_response", null);
        String json_type = null;
        if (recaptchaV2Response != null) {
            br.postPage("https://www." + this.getHost() + "/wp-admin/admin-ajax.php", "action=bypass_captcha&postId=" + fid + "&challenge=" + Encoding.urlEncode(recaptchaV2Response));
            json_type = PluginJSonUtils.getJson(br, "type");
        }
        if (recaptchaV2Response == null || !StringUtils.equalsIgnoreCase(json_type, "success")) {
            logger.info("Failed to re-use previous recaptchaV2Response");
            br.getPage(parameter);
            recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, "6Lf1jhYUAAAAAN8kNxOBBEUu3qBPcy4UNu4roO5K").getToken();
            br.postPage("https://www." + this.getHost() + "/wp-admin/admin-ajax.php", "action=get_protected_links&postId=" + fid + "&response=" + Encoding.urlEncode(recaptchaV2Response));
            json_type = PluginJSonUtils.getJson(br, "type");
        } else {
            logger.info("Successfully re-used previous recaptchaV2Response");
        }
        if (!StringUtils.equalsIgnoreCase(json_type, "success")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String src = PluginJSonUtils.getJson(br, "msg");
        if (src == null) {
            /* Fallback */
            src = br.toString();
        }
        String fpName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String[] links = new Regex(src, "\"(https?://[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        if (decryptedLinks.size() > 0) {
            /* Save reCaptchaV2 response as we might be able to use it multiple times! */
            this.getPluginConfig().setProperty("recaptcha_response", recaptchaV2Response);
        }
        return decryptedLinks;
    }
}
