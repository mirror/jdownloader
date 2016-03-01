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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "camshowdownloads.com" }, urls = { "https?://(?:www\\.)?camshowdownloads\\.com/[A-Za-z0-9]+/model/.+" }, flags = { 0 })
public class CamshowdownloadsCom extends antiDDoSForDecrypt {

    public CamshowdownloadsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // String fpName = br.getRegex("").getMatch(0);
        String next = null;
        do {
            br.setFollowRedirects(true);
            if (next != null) {
                br.getPage(next);
            }
            if (br.containsHTML("class=\"captcha\"")) {
                /* Usually happens on all pages > 1 */
                final String url_part = new Regex(this.br.getURL(), "camshowdownloads\\.com(/.+)").getMatch(0);
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                this.br.postPage("https://camshowdownloads.com/captcha", "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response) + "&loc=" + Encoding.urlEncode(url_part));
            }
            final String[] links = br.getRegex("\"(/dl/[^<>\"]*?)\"").getColumn(0);
            if (links != null && links.length > 0) {
                br.setFollowRedirects(false);
                for (final String singleLink : links) {
                    if (this.isAbort()) {
                        return decryptedLinks;
                    }
                    getPage(singleLink);
                    final String finallink = this.br.getRedirectLocation();
                    if (finallink != null) {
                        final DownloadLink dl = createDownloadlink(finallink);
                        decryptedLinks.add(dl);
                        distribute(dl);
                    }
                }

                // if (fpName != null) {
                // final FilePackage fp = FilePackage.getInstance();
                // fp.setName(Encoding.htmlDecode(fpName.trim()));
                // fp.addLinks(decryptedLinks);
                // }
            }
            next = this.br.getRegex("class=\"icon\\-chevron\\-right\" href=\"(/[^<>\"]+\\d+)\"").getMatch(0);
        } while (next != null);

        return decryptedLinks;
    }

}
