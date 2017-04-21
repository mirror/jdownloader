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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "protect-mylinks.com" }, urls = { "https?://(?:www\\.)?protect\\-mylinks\\.com/(?:decrypt|f)\\?i=[a-z0-9]+" })
public class ProtectMylinksCom extends PluginForDecrypt {

    public ProtectMylinksCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        String fpName = null;
        br.getPage(parameter);
        if (parameter.matches(".+/decrypt\\?i=.+")) {
            /* Multiple links + captcha */
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("alert alert\\-danger text\\-center")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            fpName = br.getRegex("value=\"Title: ([^<>\"]+)\"").getMatch(0);
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            this.br.postPage(this.br.getURL(), "submit=Decrypt+link&g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response));
            final String[] links = br.getRegex("(v\\?auth=[^<>\"\\']+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            this.br.setFollowRedirects(false);
            for (final String singleLink : links) {
                if (this.isAbort()) {
                    return decryptedLinks;
                }
                br.getPage(singleLink);
                final String finallink = this.br.getRedirectLocation();
                if (finallink == null || finallink.contains(this.getHost() + "/")) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            /* Single link */
            /* 2017-04-20: Server will always return 404 (fake 404)Server */
            final String finallink = this.br.getRegex("window\\.location\\s*?=\\s*?\"(http[^<>\"]+)\";").getMatch(0);
            if (finallink == null && this.br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            } else if (finallink == null) {
                return null;
            }
            decryptedLinks.add(this.createDownloadlink(finallink));
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
