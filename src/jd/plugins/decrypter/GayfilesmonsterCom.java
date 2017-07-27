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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gayfilesmonster.com" }, urls = { "https?://(?:www\\.)?gayfilesmonster\\.com/(video/go\\.php\\?file=[a-zA-Z0-9_/\\+\\=\\-%]+|[^/]+/video/[^/]+\\.html)" })
public class GayfilesmonsterCom extends PluginForDecrypt {
    public GayfilesmonsterCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String b64 = new Regex(parameter, "/go\\.php\\?file=(.+)").getMatch(0);
        if (b64 != null) {
            /* Decrypt base64 */
            final String b64_decrypted = Encoding.Base64Decode(b64);
            /* Fix URL inside the decrypted base64 */
            final String finallink = new Regex(b64_decrypted, "(filesmonster\\.com/.+)").getMatch(0);
            decryptedLinks.add(this.createDownloadlink("http://" + finallink));
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/video/")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final String[] links = br.getRegex("\"(https?://gayfilesmonster\\.com/go\\.php\\?file=[a-zA-Z0-9_/\\+\\=\\-%]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            /* Add these single URLs --> Will go back into this decrypter and get decrypted. */
            for (final String singleLink : links) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
        return decryptedLinks;
    }
}
