//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "dompl3.info" }, urls = { "http://dompl3\\.info/index\\.php\\?v=\\d+" }) 
public class Do3Inf extends antiDDoSForDecrypt {

    public Do3Inf(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // two parts to make up package name.
        final String name = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        final String[][] tabs = br.getRegex("<div href=\"(#tab\\d+)\"><b>(.*?)</b></div>").getMatches();
        if (tabs != null) {
            for (final String[] tab : tabs) {
                // multiple tabs for different formats. we will place them in separate packages.
                final String fpName = name + " - " + tab[1];
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                // find the links
                final String results = br.getRegex("<div id=\"" + Pattern.quote(tab[0].replace("#", "")) + "\".*?</div>").getMatch(-1);
                if (results != null) {
                    final String[] links = HTMLParser.getHttpLinks(results, null);
                    if (links != null) {
                        for (final String link : links) {
                            if (decryptedLinks.contains(link)) {
                                continue;
                            }
                            final DownloadLink dl = createDownloadlink(link);
                            fp.add(dl);
                            decryptedLinks.add(dl);
                        }
                    }
                }

            }
        }
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

}