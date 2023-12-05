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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ProtectedTo extends PluginForDecrypt {
    public ProtectedTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "protected.to" });
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

    private static final String PATTERN_RELATIVE_FOLDER      = "/f-[a-f0-9]{16}";
    private static final String PATTERN_RELATIVE_SINGLE_ITEM = "/\\?code=.+";

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + "(" + PATTERN_RELATIVE_FOLDER + "|" + PATTERN_RELATIVE_SINGLE_ITEM + ")");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String urlpath = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        if (urlpath.matches(PATTERN_RELATIVE_SINGLE_ITEM)) {
            br.setFollowRedirects(false);
            // br.setCookie("protected.to", "ASP.NET_SessionId", "3ssf3qnhwp4bqga2u1adykwp");
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String redirect = br.getRedirectLocation();
            if (redirect == null) {
                redirect = br.getRegex("location\\.replace\\(\"(https?://[^\"]+)\"\\)").getMatch(0);
            }
            logger.info("Redirect = " + redirect);
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.canHandle(redirect)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ret.add(this.createDownloadlink(redirect));
        } else {
            br.setFollowRedirects(true);
            br.getPage(contenturl);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(">\\s*NotFound")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Form continueform = null;
            for (final Form form : br.getForms()) {
                if (form.containsHTML("Continue to folder")) {
                    continueform = form;
                    break;
                }
            }
            if (continueform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (CaptchaHelperCrawlerPluginRecaptchaV2.containsRecaptchaV2Class(continueform)) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                continueform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
            br.submitForm(continueform);
            String html = br.getRegex("<div class=\"well Encrypted-box[^\"]+\">(.*?)</div>\\s+</div>").getMatch(0);
            if (html == null) {
                /* Fallback */
                html = br.toString();
            }
            final String[] links = br.getRegex("<a href=\\'(https?://[^\\']+)'>").getColumn(0);
            if (links == null || links.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String singleLink : links) {
                ret.add(createDownloadlink(singleLink));
            }
        }
        return ret;
    }
}
