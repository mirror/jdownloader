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

import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "proxer.me" }, urls = { "https?://(?:www\\.)?proxer\\.me/watch/\\d+/\\d+/(ger|eng)sub" })
public class ProxrMe extends PluginForDecrypt {
    public ProxrMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        br.getPage(contenturl);
        if (br.containsHTML("id=\"checkCaptcha\"")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            final Browser brc = br.cloneBrowser();
            final UrlQuery query = new UrlQuery();
            query.appendEncoded("response", recaptchaV2Response);
            brc.postPage("/components/com_proxer/misc/captcha/recaptcha.php", query);
            logger.info("Reload page after captcha");
            br.getPage(contenturl);
            /* Offline/404 can happen AFTER captcha! */
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("/images/misc/404\\.png\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String replace = br.getRegex("\"replace\":\"(http[^<>\"]*?)\"").getMatch(0);
        String code = br.getRegex("\"code\":\"([^<>\"]*?)\"").getMatch(0);
        if (replace == null || code == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        replace = replace.replace("\\", "").replace("#", "");
        code = code.replace("\\", "").replace("#", "");
        final String finallink = replace + code;
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        ret.add(createDownloadlink(finallink));
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        /* 2024-09-05: Only 1 in order to avoid captchas */
        return 1;
    }
}
