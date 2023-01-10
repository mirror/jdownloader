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

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hotpornfile.org" }, urls = { "https?://(?:www\\.)?hotpornfile\\.org/(?!page)([^/]+)/(\\d+)" })
public class HotpornfileOrg extends PluginForDecrypt {
    public HotpornfileOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    private final String PROPERTY_recaptcha_response = "recaptcha_response";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        br.getPage(parameter);
        String fpName = br.getRegex("<title>\\s*(.*?)\\s*(\\s*-\\s*Hotpornfile\\s*)?</title>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        }
        final String postID = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2019-07-18: Stream URLs may expire which is why we don't grab them for now. */
        // br.postPage("https://www." + this.getHost() + "/wp-admin/admin-ajax.php", "action=get_stream&postId=" + fid);
        /* 2019-07-18: reCaptchaKey hardcoded */
        String recaptchaV2Response = this.getPluginConfig().getStringProperty(PROPERTY_recaptcha_response);
        String json_type = null;
        boolean accessMainLinkAgain = false;
        if (recaptchaV2Response != null) {
            logger.info("Re-using last recaptchaV2Response: " + recaptchaV2Response);
            try {
                br.postPage("/wp-admin/admin-ajax.php", "action=bypass_captcha&postId=" + postID + "&challenge=" + Encoding.urlEncode(recaptchaV2Response));
                json_type = PluginJSonUtils.getJson(br, "type");
                accessMainLinkAgain = true;
            } catch (final BrowserException ignore) {
            }
        }
        if (recaptchaV2Response == null || !StringUtils.equalsIgnoreCase(json_type, "success")) {
            logger.info("Failed to re-use previous recaptchaV2Response");
            if (accessMainLinkAgain) {
                br.getPage(parameter);
            }
            recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br, "6Lf1jhYUAAAAAN8kNxOBBEUu3qBPcy4UNu4roO5K").getToken();
            final UrlQuery query = new UrlQuery();
            query.add("action", "get_links");
            query.add("postId", postID);
            query.add("challenge", Encoding.urlEncode(recaptchaV2Response));
            query.add("cid", "null");
            br.postPage("/wp-admin/admin-ajax.php", query);
            json_type = PluginJSonUtils.getJson(br, "type");
        } else {
            logger.info("Successfully re-used previous recaptchaV2Response");
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        String src = (String) entries.get("links");
        if (StringUtils.isEmpty(src)) {
            /* Fallback */
            src = br.toString();
        }
        final String[] links = new Regex(src, "\"(https?://[^\"]+)").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            ret.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        if (ret.size() > 0) {
            /* Save reCaptchaV2 response as we might be able to use it multiple times! */
            this.getPluginConfig().setProperty(PROPERTY_recaptcha_response, recaptchaV2Response);
        }
        return ret;
    }
}
