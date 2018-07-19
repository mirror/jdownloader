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
import java.util.Random;

import org.appwork.utils.StringUtils;
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
 * @author raztoki
 * @author psp
 * @tags: similar to MightyScriptAdLinkFly
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ouo.io", "cpmlink.net", "uskip.me" }, urls = { "https?://(?:www\\.)?ouo\\.(?:io|press)/(:?s/[A-Za-z0-9]{4,}\\?s=(?:http|ftp).+|[A-Za-z0-9]{4,})", "https?://cpmlink\\.net/[A-Za-z0-9]+", "https?://uskip\\.me/[A-Za-z0-9]+" })
public class OuoIo extends antiDDoSForDecrypt {
    public OuoIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter = null;
    private String fuid      = null;
    private String slink     = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // they don't support https.. redirects to http.
        set(param.toString().replace("//www.", "//").replace("https://", "http://"));
        if (slink != null) {
            decryptedLinks.add(createDownloadlink(Encoding.urlDecode(slink, false)));
            return decryptedLinks;
        } else if (fuid == null && slink == null) {
            // fuid is just a URL owner identifier! slink value is needed, without it you can't get the end URL!
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        do {
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                if (!Browser.getHost(redirect).equals(Browser.getHost(parameter))) {
                    // don't follow redirects to other hosts
                    decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
                    return decryptedLinks;
                } else {
                    getPage(redirect);
                }
            }
        } while (br.getRedirectLocation() != null);
        Form captchaForm = br.getFormbyProperty("id", "skip");
        if (captchaForm == null) {
            captchaForm = br.getFormbyProperty("id", "form-captcha");
        }
        if (captchaForm == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (captchaForm.hasInputFieldByName("s_width") && captchaForm.hasInputFieldByName("s_height")) {
            /* E.g. uskip.me */
            captchaForm.remove("s_width");
            captchaForm.remove("s_height");
            captchaForm.put("s_width", Integer.toString(new Random().nextInt(1000)));
            captchaForm.put("s_height", Integer.toString(new Random().nextInt(1000)));
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.setFollowRedirects(true);
        br.submitForm(captchaForm);
        final String finallink = getFinalLink();
        if (StringUtils.isEmpty(finallink)) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    private String getFinalLink() throws Exception {
        final String finallink;
        // can be another form after captcha - 20170326
        final Form f = br.getForm(0);
        if (f != null && f.containsHTML(">\\s*Get Link<")) {
            br.setFollowRedirects(false);
            submitForm(f);
            finallink = br.getRedirectLocation();
        } else {
            finallink = br.getRegex("\"\\s*([^\r\n]+)\\s*\"\\s+id=\"btn-main\"").getMatch(0);
        }
        return finallink;
    }

    private void set(final String downloadLink) {
        parameter = downloadLink;
        fuid = new Regex(parameter, "https?://[^/]+/([A-Za-z0-9]+)").getMatch(0);
        slink = new Regex(parameter, "\\.(?:io|press)/s/[A-Za-z0-9]{4,}\\?s=((?:http|ftp).+)").getMatch(0);
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
