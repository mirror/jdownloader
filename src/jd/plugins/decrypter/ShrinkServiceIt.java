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

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "shrink-service.it" }, urls = { "https?://(?:www\\.)?shrink-service\\.it/s/[A-Za-z0-9]+|https?://get\\.shrink-service\\.it/[A-Za-z0-9]+|https?://(?:www\\.)?adshnk\\.com/[A-Za-z0-9]+" })
public class ShrinkServiceIt extends PluginForDecrypt {
    public ShrinkServiceIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String API_BASE = "https://www.shrink-service.it/v3/api";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br._getURL().getPath().equals("/HTTP404.html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2022-06-07: Plugin broken --> And website is broken */
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String cookie_bypass_v1 = br.getCookie(br.getHost(), "cookie_bypass_v1", Cookies.NOTDELETEDPATTERN);
        if (cookie_bypass_v1 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.postPage(API_BASE + "/prototype/init", "req=init&uri=" + Encoding.urlEncode(br.getURL()) + "&cookie_bypass_v1=" + Encoding.urlEncode(cookie_bypass_v1));
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final Map<String, Object> settings = (Map<String, Object>) entries.get("settings");
        final String rcKey = settings.get("rv2pk").toString();
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, rcKey).getToken();
        br.postPage("/recaptcha.php", "v=v3&response=" + Encoding.urlEncode(recaptchaV2Response));
        String finallink = br.getRegex("<input type='hidden'[^<>\">]*?value='([^<>\"']*?)'>").getMatch(0);
        if (StringUtils.isEmpty(finallink)) {
            /* 2021-12-10: adshnk.com --> Skips captcha and waittime */
            finallink = PluginJSonUtils.getJson(br, "destination");
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + param.getCryptedUrl());
            return null;
        } else if (finallink.equals("")) {
            /* Empty field --> Offline/invalid url */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        finallink = Encoding.htmlOnlyDecode(finallink);
        finallink = finallink.replace("&sol;", "/");
        finallink = finallink.replace("&colon;", ":");
        finallink = finallink.replace("&period;", ".");
        finallink = finallink.replace("&quest;", "?");
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
