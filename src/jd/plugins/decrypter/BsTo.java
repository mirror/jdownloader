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

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bs.to" }, urls = { "https?://(?:www\\.)?bs\\.to/(serie/[^/]+/\\d+/[^/]+(/[^/]+)?|out/\\d+)" })
public class BsTo extends PluginForDecrypt {

    public BsTo(PluginWrapper wrapper) {
        super(wrapper);
        Browser.setRequestIntervalLimitGlobal("bs.to", 200);
    }

    private static final String TYPE_SINGLE = "https?://(www\\.)?bs\\.to/serie/[^/]+/\\d+/[^/]+/[^/]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (StringUtils.contains(parameter, "bs.to/out")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.getRedirectLocation() == null || br.containsHTML("g-recaptcha")) {
                final Form form = br.getForm(0);
                if (form == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                form.put("response", recaptchaV2Response);
                br.submitForm(form);
            }
            final String finallink = br.getRedirectLocation();
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String urlpart = new Regex(parameter, "(serie/.+)").getMatch(0);
        if (parameter.matches(TYPE_SINGLE)) {
            String finallink = br.getRegex("\"(https?[^<>\"]*?)\" target=\"_blank\"><span class=\"icon link_go\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("<iframe\\s+[^>]+src\\s*=\\s*(\"|'|)(.*?)\\1").getMatch(1);
            }
            if (finallink == null) {
                throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
            } else if (finallink.contains("bs.to/out/")) {
                br.setFollowRedirects(false);
                br.getPage(finallink);
                if (br.getRedirectLocation() == null || br.containsHTML("g-recaptcha")) {
                    final Form form = br.getForm(0);
                    if (form == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    form.put("token", Encoding.urlEncode(recaptchaV2Response));
                    br.submitForm(form);
                }
                finallink = br.getRedirectLocation();
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            final String[] links = br.getRegex("class=\"v-centered icon [^<>\"]+\"[\t\n\r ]+href=\"(" + urlpart + "/[^/]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink(Request.getLocation("/" + singleLink, br.getRequest())));
            }
        }
        return decryptedLinks;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 5;
    }
}
