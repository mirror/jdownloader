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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vcrypt.net" }, urls = { "https?://(?:www\\.)?vcrypt\\.(?:net|pw)/([a-z0-9]{6}|[^/]+/[a-z0-9]+)" })
public class VcryptNet extends antiDDoSForDecrypt {

    public VcryptNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://").replace("vcrypt.pw/", "vcrypt.net/");
        br.setFollowRedirects(false);
        getPage(parameter);
        if (br.containsHTML(">Error folder unavailable<") || this.br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (parameter.matches("https?://[^/]+/[^/]+/[a-z0-9]+")) {
            Form continueForm = br.getFormByInputFieldKeyValue("submit", "Continue");
            if (continueForm == null) {
                continueForm = br.getFormBySubmitvalue("Continue");
            }
            if (continueForm != null) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                continueForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                submitForm(continueForm);
            }
            /* Single redirect url */
            final String redirect = br.getRedirectLocation();
            if (redirect != null && !redirect.contains(this.getHost() + "/")) {
                decryptedLinks.add(createDownloadlink(redirect));
            } else if (redirect != null && redirect.contains("/banned")) {
                logger.info("Reconnect required to continue decryption");
            } else {
                // can be a list of links
                final String button = br.getRegex("<button id=\"tt\" class=\"clickme\" value=\"(.*?)\"").getMatch(0);
                if (button == null) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                final String[] links = HTMLParser.getHttpLinks(button, null);
                for (final String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }
            }
        } else {
            final String[] links = br.getRegex("href=\\'(http[^<>\"]*?)\\' target=_blank>").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                if (!singleLink.contains("vcrypt.net/")) {
                    decryptedLinks.add(createDownloadlink(singleLink));
                }
            }
        }

        return decryptedLinks;
    }

}
