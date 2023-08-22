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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dlslink.net" }, urls = { "https?://(?:www\\.)?(?:dlslink\\.net|dlvisit\\.com)/([a-z0-9]+)" })
public class DlslinkNet extends PluginForDecrypt {
    public DlslinkNet(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* Tags: dlplatforms.com, dlupload.com, khabarbabal.online, dlslink.net, dlvisit.com */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String contentID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        br.setAllowedResponseCodes(new int[] { 500 });
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        }
        br.getPage("https://" + this.getHost() + "/" + contentID);
        /* Set two important cookies. */
        final String expiryCookie = br.getRegex("SetCookieWith5MinutesExpiry\\(\"PageExpiryDate\", '([^<>\"\\']+)'\\);").getMatch(0);
        if (expiryCookie != null) {
            br.setCookie(br.getHost(), "PageExpiryDate", expiryCookie);
        }
        br.setCookie(br.getHost(), "PageNo", "3");
        br.getPage("/go/" + contentID);
        final String redirect = br.getRegex("window\\.location\\.replace\\('(https?://[^\\']+)").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        // brc.getPage("/Url/Page3/" + fid);
        final Form captchaForm = br.getFormbyActionRegex(".*ValidateCaptchaAndGetUrl");
        if (captchaForm == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, brc).getToken();
        captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        brc.submitForm(captchaForm);
        final Map<String, Object> entries = restoreFromString(brc.toString(), TypeRef.MAP);
        final String finallink = (String) entries.get("actualUrl");
        if (StringUtils.isEmpty(finallink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
