//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sdx.cc" }, urls = { "http://(www\\.)?sdx\\.cc/\\d+/.{1}" }, flags = { 0 })
public class SdxCc extends PluginForDecrypt {

    public SdxCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = cryptedLink.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (!br.containsHTML("class=\"r10\\-unit rater\"")) {
            logger.info("Link broken/offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>([^<>\"]*?) \\- SDX\\.CC \\| Feel the Speed</title>").getMatch(0);
        String pw = br.getRegex("<b>Passwort</b></td><td><i>([^<>\"]*?)</i>").getMatch(0);
        pw = pw != null ? pw.trim() : "sdx.cc";
        ArrayList<String> pwList = new ArrayList<String>(Arrays.asList(new String[] { pw, "sdx.cc" }));
        final String[] links = br.getRegex("\"(mirror/\\d+/\\d+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return decryptedLinks;
        }
        final Browser ajaxBR = br.cloneBrowser();
        ajaxBR.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (final String link : links) {
            br.getPage("http://www.sdx.cc/" + link);
            final String theKey = br.getRegex("<form id=\"links_form\" class=\"([^<>\"]*?)\"").getMatch(0);
            if (theKey == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return decryptedLinks;
            }
            ajaxBR.postPage("http://www.sdx.cc/ajax.php", "linklist=" + Encoding.urlEncode(theKey));
            final String[] plainlinks = ajaxBR.getRegex("<a href=\"(http[^<>\"]*?)\"").getColumn(0);
            if (plainlinks == null || plainlinks.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return decryptedLinks;
            }
            for (final String decryptedLink : plainlinks) {
                final DownloadLink dl = createDownloadlink(decryptedLink);
                dl.setSourcePluginPasswordList(pwList);
                decryptedLinks.add(dl);
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
        return false;
    }

}