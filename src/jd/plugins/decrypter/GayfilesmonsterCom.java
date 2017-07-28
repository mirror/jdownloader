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
        if (parameter.matches(".+go\\.php.+")) {
            final DownloadLink dl = decryptSingleURL(parameter);
            if (dl == null) {
                return null;
            }
            decryptedLinks.add(dl);
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
            for (final String singleLink : links) {
                final DownloadLink dl = decryptSingleURL(singleLink);
                if (dl == null) {
                    return null;
                }
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    private DownloadLink decryptSingleURL(final String url) {
        final String b64 = new Regex(url, "/go\\.php\\?file=(.+)").getMatch(0);
        /* Decrypt base64 */
        final String b64_decrypted = Encoding.Base64Decode(b64);
        /* Fix URL inside the decrypted base64 */
        final String fileid;
        if (b64_decrypted.contains("sptth")) {
            fileid = new Regex(b64_decrypted, "download\\.php\\?id=([^/]+)$").getMatch(0);
        } else {
            final Regex finfo = new Regex(b64_decrypted, "download\\.php\\?id=(.{5})([^/]+)$");
            final String fileid_part1_reversed = finfo.getMatch(0);
            final char[] fileid_part1_reversed_array = fileid_part1_reversed.toCharArray();
            String fileid_part1 = "";
            for (int i = fileid_part1_reversed_array.length - 1; i > -1; i--) {
                final char currentChar = fileid_part1_reversed_array[i];
                fileid_part1 += currentChar;
            }
            final String fileid_part2 = finfo.getMatch(1);
            fileid = fileid_part1 + fileid_part2;
        }
        return this.createDownloadlink("https://filesmonster.com/download.php?id=" + fileid);
    }
}
