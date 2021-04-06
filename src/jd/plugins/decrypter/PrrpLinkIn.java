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
import jd.http.Browser;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "peeplink.in", "alfalink.to" }, urls = { "https?://(?:www\\.)?peeplink\\.in/[a-f0-9]+", "https?://(?:www\\.)?alfalink\\.(?:info|to)/[a-f0-9]+" })
public class PrrpLinkIn extends PluginForDecrypt {
    public PrrpLinkIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.containsHTML("value=\"Enter Access Password\"")) {
            logger.info("Password protected URLs are not yet supported");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("class=\"QapTcha\"")) {
            final Browser brc = this.br.cloneBrowser();
            brc.postPage("/qaptcha/php/Qaptcha.jquery.php", "action=qaptcha");
            br.postPage(this.br.getURL(), "iQapTcha=");
        }
        String urlText = br.getRegex("<article.*?>(.*?)</article").getMatch(0);
        if (urlText == null) {
            if (br.containsHTML("hcaptcha\\.com")) {
                logger.warning("Unsupported captcha type hcaptcha");
                return null;
            } else {
                logger.warning("Fallback to scanning complete HTML");
                urlText = this.br.toString();
            }
        }
        final String[] finallinks = HTMLParser.getHttpLinks(urlText, "");
        for (final String aLink : finallinks) {
            if (!this.canHandle(aLink)) {
                decryptedLinks.add(createDownloadlink(aLink));
            }
        }
        return decryptedLinks;
    }
}
