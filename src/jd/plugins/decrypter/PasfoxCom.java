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
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PasfoxCom extends PluginForDecrypt {
    public PasfoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.PASTEBIN };
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "pasfox.co", "pasfox.com" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String parameter = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        /* 2023-10-05: pasfox.com domain is down but content behind it still works with new domain pasfox.co. */
        final String domainFromURL = Browser.getHost(parameter, false);
        parameter = parameter.replaceFirst(Pattern.quote(domainFromURL), this.getHost());
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Form captchaForm = br.getFormbyActionRegex(".*validateCaptcha.*");
        if (captchaForm == null) {
            captchaForm = br.getFormbyProperty("id", "form_captcha");
        }
        if (captchaForm != null) {
            final String csrftoken = br.getRegex("name=\"csrf-token\" content=\"([^\"]+)\"").getMatch(0);
            if (csrftoken == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* json for their framework stuff (laravel-livewire.com) */
            String wirejson = br.getRegex("wire:initial-data=\"([^\"]+)").getMatch(0);
            if (wirejson == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            wirejson = wirejson.replace("&quot;", "\"");
            if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                /* 2022-10-25: This plugin is partly broken see ticket https://svn.jdownloader.org/issues/90267 */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            // br.submitForm(captchaForm);
            final Map<String, Object> postData = restoreFromString(wirejson, TypeRef.MAP);
            // TODO: "id" field is still wrong
            String jsonUpdatesRaw = "[  {    \"type\": \"callMethod\",    \"payload\": {      \"id\": \"53a\",      \"method\": \"$set\",      \"params\": [        \"captcha\",        \"JD_RCV2\"      ]    }  },  {    \"type\": \"fireEvent\",    \"payload\": {      \"id\": \"uewj\",      \"event\": \"validateCapchat\",      \"params\": [      ]    }  },  {    \"type\": \"callMethod\",    \"payload\": {      \"id\": \"y5eh\",      \"method\": \"$set\",      \"params\": [        \"captcha\",        \"JD_RCV2\"      ]    }  },  {    \"type\": \"fireEvent\",    \"payload\": {      \"id\": \"o0gt\",      \"event\": \"validateCapchat\",      \"params\": [      ]    }  }]";
            jsonUpdatesRaw = jsonUpdatesRaw.replaceAll("JD_RCV2", recaptchaV2Response);
            final List<Object> updates = restoreFromString(jsonUpdatesRaw, TypeRef.LIST);
            postData.put("updates", updates);
            // br.getPage(param.getCryptedUrl());
            br.getHeaders().put("Content-Type", "application/json");
            br.getHeaders().put("x-csrf-token", csrftoken);
            br.getHeaders().put("x-livewire", "true");
            br.getHeaders().put("Origin", "https://" + this.getHost());
            br.postPageRaw("/livewire/message/show-paste", JSonStorage.serializeToJson(postData));
        }
        String html = br.getRegex("id=\"myTabContent\"(.*?)</div>\\s*</div>").getMatch(0);
        if (html == null) {
            /* Fallback */
            logger.warning("Fallback required! Possible crawler failure!");
            html = br.getRequest().getHtmlCode();
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
