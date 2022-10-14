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
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CamshowdownloadCom extends antiDDoSForDecrypt {
    public CamshowdownloadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void init() {
        /* 2022-10-14: Applying this limit will drastically reduce the amount of captchas (especially for the single "/dl/.+" URLs). */
        for (final String[] domaingroup : getPluginDomains()) {
            for (final String domain : domaingroup) {
                Browser.setRequestIntervalLimitGlobal(domain, true, 5000);
            }
        }
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "camshowdownload.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([^/]+/video/.+|dl/.+|[\\w\\-]+/[\\w\\-]+/model/[\\w\\-]+/[\\w\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String addedurl = param.toString().replaceFirst("http://", "https://");
        String fpName = null;
        if (addedurl.matches("https?://[^/]+/dl/.+")) {
            /* Add single url to array of urls to decrypt. */
            br.setFollowRedirects(false);
            getPage(addedurl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Form captchaForm = this.br.getFormbyKey("loc");
            if (captchaForm != null) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                super.submitForm(captchaForm);
            }
            String redirect = this.br.getRedirectLocation();
            if (redirect != null && redirect.matches("https?://[^/]+/dl/.+")) {
                /* Redirect to url before can happen after captcha (basically one step extra). */
                this.getPage(redirect);
                redirect = br.getRedirectLocation();
            }
            if (redirect == null || this.canHandle(redirect)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ret.add(createDownloadlink(redirect));
        } else {
            br.setFollowRedirects(true);
            getPage(addedurl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            // they can anti bot routine here
            if (br.getHttpConnection().getResponseCode() == 503) {
                // 6LdM_AYTAAAAADrpgYaW-wHyMowkEizhAS72G6rw
                final Form captchaForm = br.getForm(0);
                if (captchaForm == null || !captchaForm.containsHTML("=\"g-recaptcha\"")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                submitForm(captchaForm);
            }
            fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (fpName == null) {
                /* Fallback */
                fpName = new Regex(addedurl, "([^/]+)$").getMatch(0);
            }
            final String[] links = br.getRegex("\"(/dl/[^<>\"]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + addedurl);
                return null;
            }
            /* Those URLs will go back into our crawler and will be crawled one by one (captcha is required for each item). */
            for (final String url : links) {
                ret.add(this.createDownloadlink(br.getURL(url).toString()));
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(ret);
            }
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
