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

import java.util.ArrayList;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 *
 * variant of OuoIo
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fas.li" }, urls = { "https?://(?:www\\.)?fas\\.li/(?:go/)?[A-Za-z0-9]{4,}" })
public class FasLi extends antiDDoSForDecrypt {

    public FasLi(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter = null;
    private String fuid      = null;
    private String slink     = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        set(param.toString());
        if (slink != null) {
            decryptedLinks.add(createDownloadlink(Encoding.urlDecode(slink, false)));
            return decryptedLinks;
        } else if (fuid == null && slink == null) {
            // fuid is just a URL owner identifier! slink value is needed, without it you can't get the end URL!
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        parameter = "http://" + Browser.getHost(parameter) + "/" + this.fuid;
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains(fuid)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // form
        final Form form = br.getForm(0);
        if (form != null) {
            if (form.containsHTML("g-recaptcha")) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            form.put("s_width", "1680");
            form.put("s_height", "890");
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
        String finallink = br.getRegex("\\s+id=(\"|'|)btn-main\\1[^>]+href=(\"|')\\s*([^\r\n]+)\\s*\\2").getMatch(2);
        if (finallink == null) {
            finallink = br.getRegex("<a href=\"(http[^<>\"]+)\" id=\"btn\\-main\"").getMatch(0);
        }
        return finallink;
    }

    private void set(final String downloadLink) {
        parameter = downloadLink;
        fuid = new Regex(parameter, ".+/([A-Za-z0-9]{4,})$").getMatch(0);
        slink = new Regex(parameter, "/s/[A-Za-z0-9]{4,}\\?s=((?:http|ftp).+)").getMatch(0);
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        set(link.getDownloadURL());
        if (slink != null) {
            return false;
        }
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
