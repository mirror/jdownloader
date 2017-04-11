//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.HashSet;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "3ddl.tv" }, urls = { "https?://(www\\.)?3ddl\\.tv/download/\\S+/" })
public class ThreeDlTv extends antiDDoSForDecrypt {

    public ThreeDlTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> passwords = new ArrayList<String>();
        final String parameter = param.toString();
        getPage(parameter);
        final String pid = br.getRegex(",t=\"(\\d+)\"").getMatch(0);
        final String[] mirrors = br.getRegex("data-dl=\"(dl-\\d+)\"").getColumn(0);
        if (mirrors == null || pid == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        final String password = br.getRegex(">Passwort:</th>\\s*<td[^>]*>\\s*(.*?)\\s*</td>").getMatch(0);
        if (password != null) {
            passwords.add(password);
        }
        final String fpName = br.getRegex(">Release:</th>\\s*<td[^>]*>\\s*(.*?)\\s*</td>").getMatch(0);

        for (final String mirror : mirrors) {
            final Browser ajax = br.cloneBrowser();
            ajax.getHeaders().put("Accept", "*/*");
            ajax.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            postPage(ajax, "/drop.php", "option=" + mirror + "&pid=" + pid + "&ceck=sec");
            // they also use relinks and hoster links either base encoded or raw within source.
            // we will use link found within base encoding or clicknload

            // clicknload is easiest // clicknloaw (raw)
            {
                final String link = ajax.getRegex("VALUE=\"(.*?)\"></form>").getMatch(0);
                if (link == null) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                final String[] links = link.split("[\r\n]+");
                for (final String ink : links) {
                    final DownloadLink dl = createDownloadlink(ink);
                    if (!passwords.isEmpty()) {
                        dl.setSourcePluginPasswordList(passwords);
                    }
                    decryptedLinks.add(dl);
                }
            }
            if (decryptedLinks.isEmpty()) {
                // failover, standard href (base64 encoded)
                String[] links = ajax.getRegex("<div id=\"dlstatus\"></div><a rel=\"nofollow\" href=\"(.*?)\"").getColumn(0);
                if (links == null) {
                    throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
                }
                for (final String link : links) {
                    final String go = new Regex(link, "/go/(.+)").getMatch(0);
                    final HashSet<String> inks = GenericBase64Decrypter.handleBase64Decode(go);
                    for (final String l : inks) {
                        final DownloadLink dl = createDownloadlink(l);
                        if (!passwords.isEmpty()) {
                            dl.setSourcePluginPasswordList(passwords);
                        }
                        decryptedLinks.add(dl);
                    }
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}