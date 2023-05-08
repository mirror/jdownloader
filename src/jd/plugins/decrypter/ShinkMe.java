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
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.SiteType.SiteTemplate;

/**
 * NOTE: <br />
 * - contains recaptchav2, and uids are not case senstive any longer -raztoki 20150427 - regex pattern seems to be case sensitive, our url
 * listener is case insensitive by default... so we need to ENFORCE case sensitivity. -raztoki 20150308 <br />
 * - uid seems to be fixed to 5 chars (at this time) -raztoki 20150308 <br />
 * - uses cloudflare -raztoki 20150308 <br />
 *
 * @author psp
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ShinkMe extends antiDDoSForDecrypt {
    private static Object CTRLLOCK = new Object();

    public ShinkMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "shink.me", "shink.in", "shon.xyz" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:s/)?[a-zA-Z0-9]{5}");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String url = param.getCryptedUrl().replace(".in/", ".me/");
        br.setFollowRedirects(true);
        // br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)
        // Chrome/112.0.0.0 Safari/537.36");
        // br.getHeaders().put("sec-ch-ua", "\"Chromium\";v=\"112\", \"Google Chrome\";v=\"112\", \"Not:A-Brand\";v=\"99\"");
        // br.getHeaders().put("sec-ch-ua-mobile", "?0");
        // br.getHeaders().put("sec-ch-ua-platform", "\"Windows\"");
        // br.getHeaders().put("upgrade-insecure-requests", "1");
        // br.getHeaders().put("Origin", "https://shon.xyz");
        // br.getHeaders().put("Accept",
        // "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        // br.getHeaders().put("sec-fetch-site", "same-origin");
        // br.getHeaders().put("sec-fetch-mode", "navigate");
        // br.getHeaders().put("sec-fetch-user", "?1");
        // br.getHeaders().put("sec-fetch-dest", "document");
        Form form1 = null;
        /* 2021-03-16: reCaptcha not present anymore */
        // they seem to only show recaptchav2 once!! they track ip session (as restarting client doesn't get recaptchav2, the only cookies
        // that are cached are cloudflare and they are only kept in memory, and restarting will flush it)
        synchronized (CTRLLOCK) {
            getPage(url);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            form1 = br.getFormbyProperty("id", "skip");
            if (form1 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2023-05-08: Looks like they've moved the captcha to the 2nd form. */
            if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(form1)) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form1.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
        }
        br.setFollowRedirects(false);
        submitForm(form1);
        final Form form2 = br.getFormbyProperty("id", "skip");
        if (form2 != null) {
            final String reCaptchav2FieldKey = "g-recaptcha-response";
            if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(form2) || form2.hasInputFieldByName(reCaptchav2FieldKey)) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form2.put(reCaptchav2FieldKey, Encoding.urlEncode(recaptchaV2Response));
            }
            submitForm(form2);
        } else {
            logger.warning("Failed to find form2");
        }
        String finallink = br.getRedirectLocation();
        if (inValidate(finallink)) {
            finallink = br.getRegex("<a [^>]*href=('|\")(.*?)\\1[^>]*>GET LINK</a>").getMatch(1);
            if (inValidate(finallink)) {
                finallink = br.getRegex("<a class=('|\")\\s*btn btn-primary\\s*\\1 href=('|\")(.*?)\\2").getMatch(2);
                if (inValidate(finallink)) {
                    logger.warning("Decrypter broken for link: " + url);
                    return null;
                }
            }
        }
        if (StringUtils.equalsIgnoreCase(finallink, "http://deleted/")) {
            /* 2023-05-05 */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        ret.add(createDownloadlink(finallink));
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
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
