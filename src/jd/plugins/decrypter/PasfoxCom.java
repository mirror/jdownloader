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

import org.appwork.utils.DebugMode;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pasfox.com" }, urls = { "https?://(?:www\\.)?pasfox\\.com/([A-Za-z0-9]+)" })
public class PasfoxCom extends PluginForDecrypt {
    public PasfoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl().replaceFirst("http://", "https://");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            /* 2022-10-25: This plugin is broken */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        Form captchaForm = br.getFormbyActionRegex(".*validateCaptcha.*");
        if (captchaForm == null) {
            captchaForm = br.getFormbyProperty("id", "form_captcha");
        }
        if (captchaForm == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String csrftoken = br.getRegex("name=\"csrf-token\" content=\"([^\"]+)\"").getMatch(0);
        if (csrftoken == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Content-Type", "application/json");
        br.getHeaders().put("x-csrf-token", csrftoken);
        br.getHeaders().put("x-livewire", "true");
        br.getHeaders().put("Origin", "https://" + this.getHost());
        br.postPageRaw("https://pasfox.com/livewire/message/show-paste", "TODO");
        // final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        // captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        br.submitForm(captchaForm);
        String html = br.getRegex("<div class=\"card__body flex\">(.*?)<div class=\"card__footer\">").getMatch(0);
        if (html == null) {
            /* Fallback */
            logger.warning("Fallback required! Possible crawler failure!");
            html = br.toString();
        }
        final String[] urls = HTMLParser.getHttpLinks(html, br.getURL());
        for (final String url : urls) {
            if (!this.canHandle(url)) {
                ret.add(createDownloadlink(url));
            }
        }
        return ret;
    }
}
