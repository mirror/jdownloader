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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pasteca.sh" }, urls = { "https?://(?:www\\.)?pasteca\\.sh/[A-Za-z0-9]+" })
public class PastecaSh extends PluginForDecrypt {
    public PastecaSh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form captchaForm = br.getFormbyProperty("id", "skip");
        if (captchaForm == null) {
            captchaForm = br.getForm(0);
        }
        final String reCaptchaKey = br.getRegex("sitekey\\s*?:\\s*?\"([^<>\"]+)\"").getMatch(0);
        if (captchaForm == null || reCaptchaKey == null) {
            return null;
        }
        final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
        captchaForm.put("g-recaptcha-response", recaptchaV2Response);
        br.submitForm(captchaForm);
        String plaintxt = br.getRegex("<blockquote class=\"CodePaste\">(.*?)</blockquote>").getMatch(0);
        if (plaintxt == null) {
            plaintxt = br.toString();
        }
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no links in link: " + parameter);
            return decryptedLinks;
        }
        for (final String dl : links) {
            if (!new Regex(dl, getSupportedLinks()).matches()) {
                decryptedLinks.add(createDownloadlink(dl));
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}