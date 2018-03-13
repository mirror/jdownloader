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
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "camshowdownload.com" }, urls = { "https?://(?:www\\.)?camshowdownload\\.com/([^/]+/video/.+|dl/.+)" })
public class CamshowdownloadCom extends antiDDoSForDecrypt {
    public CamshowdownloadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> urlsToDecrypt = new ArrayList<String>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        String fpName = null;
        if (parameter.matches(".+/dl/.+")) {
            /* Add single url to array of urls to decrypt. */
            urlsToDecrypt.add(parameter);
        } else {
            getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            // they can anti bot routine here
            if (br.getHttpConnection().getResponseCode() == 503) {
                // 6LdM_AYTAAAAADrpgYaW-wHyMowkEizhAS72G6rw
                final Form captchaForm = br.getForm(0);
                if (captchaForm != null && captchaForm.containsHTML("=\"g-recaptcha\"")) {
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                    captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                    submitForm(captchaForm);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (fpName == null) {
                /* Fallback */
                fpName = new Regex(parameter, "([^/]+)$").getMatch(0);
            }
            final String[] links = br.getRegex("\"(/dl/[^<>\"]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            /* Add all found urls to Array of urls to decrypt. */
            urlsToDecrypt.addAll(Arrays.asList(links));
        }
        for (final String singleLink : urlsToDecrypt) {
            getPage(singleLink);
            final Form captchaForm = this.br.getFormbyKey("loc");
            if (captchaForm != null) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                super.submitForm(captchaForm);
            }
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null || finallink.contains("camshowdownload.com/")) {
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
