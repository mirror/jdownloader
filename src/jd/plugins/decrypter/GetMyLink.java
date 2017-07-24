//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.ArrayList;

import org.appwork.utils.encoding.Base64;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "get-my.link" }, urls = { "https?://(?:www\\.)?get\\-my\\.link/page(\\.php\\?f=|\\.html/)[a-zA-Z0-9_/\\+\\=\\-%]+" })
public class GetMyLink extends antiDDoSForDecrypt {

    public GetMyLink(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fuid = new Regex(parameter, "(f=|html/)(.+)").getMatch(1);
        if (fuid != null) {
            try {
                final String crypted = Base64.decodeToString(fuid);
                final String sb = Encoding.atbashDecode(crypted);
                final URL url = new URL(sb.toString());
                decryptedLinks.add(createDownloadlink(url.toExternalForm()));
                return decryptedLinks;
            } catch (final Throwable ignore) {
                logger.log(ignore);
            }
        }
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains(fuid)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Form form = br.getForm(0);
        if (form != null) {
            if (form.containsHTML("g-recaptcha")) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            br.submitForm(form);
        }
        final String finallink = getFinalLink();
        if (finallink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    private String getFinalLink() {
        String finallink = br.getRegex("class=\"dv_btn\" style=\"[^\"]*?\">\\s*?<a href=\"(http[^<>\"]+)>").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("href=\"(http[^<>\"]+)\">\\s*?<button type=\"button\" class=\"btn btn\\-primary\">Télécharger le fichier").getMatch(0);
        }
        return finallink;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.OuoIoCryptor;
    }

}
