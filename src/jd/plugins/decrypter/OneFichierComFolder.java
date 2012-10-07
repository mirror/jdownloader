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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1fichier.com" }, urls = { "https?://(www\\.)?1fichier\\.com/((en|cn)/)?dir/[A-Za-z0-9]+" }, flags = { 0 })
public class OneFichierComFolder extends PluginForDecrypt {

    public OneFichierComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = "http://www.1fichier.com/en/dir/" + new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
        prepareBrowser(br);
        br.getPage(parameter + "?e=1");
        String passCode = null;
        if (br.containsHTML("password")) {
            for (int i = 0; i <= 3; i++) {
                passCode = getUserInput("Enter password for: " + parameter, param);
                br.postPage(parameter + "?e=1", "pass=" + Encoding.urlEncode(passCode));
                if (br.containsHTML("password")) continue;
                break;
            }
            if (br.containsHTML("password")) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        String[][] linkInfo = br.getRegex("(https?://[a-z0-9\\-]+\\..*?);([^;]+);([0-9]+)").getMatches();
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLinkInfo[] : linkInfo) {
            DownloadLink dl = createDownloadlink(singleLinkInfo[0]);
            dl.setFinalFileName(Encoding.htmlDecode(singleLinkInfo[1].trim()));
            long size = -1;
            dl.setDownloadSize(size = SizeFormatter.getSize(singleLinkInfo[2]));
            if (size > 0) {
                if (size > 0) dl.setProperty("VERIFIEDFILESIZE", size);
            }
            if (passCode != null) dl.setProperty("pass", passCode);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) { return; }
            br.getHeaders().put("User-Agent", "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.2.16) Gecko/20110323 Ubuntu/10.10 (maverick) Firefox/3.6.16");
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }
}
