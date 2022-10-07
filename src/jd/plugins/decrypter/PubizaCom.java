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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PubizaCom extends antiDDoSForDecrypt {
    public PubizaCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    // private Browser ajax = null;
    // private void ajaxPostPage(final String url, final LinkedHashMap<String, String> param) throws Exception {
    // ajax = br.cloneBrowser();
    // ajax.getHeaders().put("Accept", "*/*");
    // ajax.getHeaders().put("Connection-Type", "application/x-www-form-urlencoded");
    // ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    // postPage(ajax, url, param);
    // }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pubiza.com", "link.tl", "lnkload.com", "lnk.parts", "lnk.news" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?!login|payout-rates|register)[A-Za-z0-9\\-]{4,}");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        getPage(param.getCryptedUrl());
        /* Check for direct redirect */
        String redirect = br.getRedirectLocation();
        if (redirect == null) {
            redirect = br.getRegex("top\\.location\\.href = \"(http[^<>\"]*?)\"").getMatch(0);
        }
        if (redirect != null) {
            if (!this.canHandle(redirect)) {
                ret.add(createDownloadlink(redirect));
                return ret;
            } else {
                br.setFollowRedirects(true);
                br.followRedirect(true);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRequest().getHtmlCode().length() <= 100) {
            /* Empty page/error-page */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        final Form form1 = br.getFormbyProperty("id", "display_go_form");
        if (form1 != null) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            form1.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(form1);
            final String finallink = br.getRegex("goToUrl\\s*\\(\"(https?://[^\"]+)\"\\)").getMatch(0);
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ret.add(this.createDownloadlink(finallink));
        } else {
            /* Assume that no captcha is needed e.g. for "lnk.news" urls */
            br.getPage("/d" + br._getURL().getPath());
            final String finallink = br.getRegex("let redirectPage = \"(https?://[^\"]+)\"").getMatch(0);
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ret.add(this.createDownloadlink(finallink));
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return true;
    }
}