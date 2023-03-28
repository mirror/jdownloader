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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BesargajiCom extends PluginForDecrypt {
    public BesargajiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ponselharian.com", "besargaji.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        final Form form = br.getFormbyProperty("id", "slu-form");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* E.g. redirect to mainpage or random advertisement page. */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.sleep(7 * 1000, param);
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        // see: https://besargaji.com/wp-content/plugins/akismet/_inc/akismet-frontend.js?ver=1669965005
        form.getInputField("captcha").setDisabled(false);
        form.put("ak_bib", "");
        form.put("ak_bfs", Long.toString(System.currentTimeMillis()));
        form.put("ak_bkpc", "0");
        form.put("ak_bkp", "");
        form.put("ak_bmc", "39");
        form.put("ak_bmcc", "1");
        form.put("ak_bmk", "");
        form.put("ak_bck", "");
        form.put("ak_bmmc", "11");
        form.put("ak_btmc", "0");
        form.put("ak_bsc", "2");
        form.put("ak_bte", "");
        form.put("ak_btec", "0");
        form.put("ak_bmm", "0");
        br.setCookie(br.getHost(), "next", "1");
        br.getHeaders().put("Origin", "https://besargaji.com");
        br.submitForm(form);
        final String finallink = this.br.getRegex("document\\.getElementById\\(\"slu-link\"\\)\\.setAttribute\\(\"href\", \"(http[^\"]+)\"").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            ret.add(createDownloadlink(finallink));
            return ret;
        }
    }
}
