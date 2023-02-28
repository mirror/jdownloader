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
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ShrinkServiceIt extends PluginForDecrypt {
    public ShrinkServiceIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "shrink-service.it", "adshnk.com", "dshnk.com" });
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
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "/(?:s/)?[A-Za-z0-9]{2,}");
        }
        return ret.toArray(new String[0]);
    }

    private final String API_BASE = "https://www.shrink-service.it/v3/api";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br._getURL().getPath().equals("/HTTP404.html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String cookie_bypass_v1 = br.getCookie(br.getHost(), "cookie_bypass_v1", Cookies.NOTDELETEDPATTERN);
        if (cookie_bypass_v1 == null) {
            /* 2022-11-02 */
            cookie_bypass_v1 = "false";
        }
        final Browser brc = br.cloneBrowser();
        brc.postPage(API_BASE + "/prototype/init", "req=init&uri=" + Encoding.urlEncode(br.getURL()) + "&cookie_bypass_v1=" + Encoding.urlEncode(cookie_bypass_v1));
        String finallink = null;
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(brc.toString());
        final Map<String, Object> response0 = (Map<String, Object>) entries.get("0");
        if (response0 != null) {
            finallink = (String) response0.get("destination");
            if (finallink == null) {
                /* 2022-11-02: Unsure about that */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        if (StringUtils.isEmpty(finallink)) {
            logger.info("Looks like captcha is required");
            final Map<String, Object> settings = (Map<String, Object>) entries.get("settings");
            final String rcKey = settings.get("rv2pk").toString();
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, rcKey).getToken();
            br.postPage("/recaptcha.php", "v=v3&response=" + Encoding.urlEncode(recaptchaV2Response));
            finallink = br.getRegex("<input type='hidden'[^<>\">]*?value='([^<>\"']*?)'>").getMatch(0);
            if (StringUtils.isEmpty(finallink)) {
                /* 2021-12-10: adshnk.com --> Skips captcha and waittime */
                finallink = PluginJSonUtils.getJson(br, "destination");
            }
        } else {
            logger.info("Skipped captcha");
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (finallink.equals("")) {
            /* Empty field --> Offline/invalid url */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        finallink = Encoding.htmlOnlyDecode(finallink);
        finallink = finallink.replace("&sol;", "/");
        finallink = finallink.replace("&colon;", ":");
        finallink = finallink.replace("&period;", ".");
        finallink = finallink.replace("&quest;", "?");
        ret.add(createDownloadlink(finallink));
        return ret;
    }
}
