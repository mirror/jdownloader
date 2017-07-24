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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 * NOTE: uses cloudflare
 *
 * @author raztoki
 * @author psp
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkgen.st" }, urls = { "https?://(?:www\\.)?(?:mylinkgen\\.com|linkgen\\.st)/(p|g)/([A-Za-z0-9]+)" }) 
public class MyLinkGenCom extends antiDDoSForDecrypt {

    public MyLinkGenCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Heavily modified safelinking.net script, thus not using SflnkgNt class */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http://", "https://").replace("mylinkgen.com/", "linkgen.st/");
        final String uid = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        final String type = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        // at times they will state redirect to site owner 'http://ganool.ag', set referrer solves this.
        br.getHeaders().put("Referer", "http://ganool.ag/");
        getPage(parameter);
        if (br.toString().matches("(?i)file not exist|File does not exist")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        if ("p".equalsIgnoreCase(type)) {
            String continuelink = br.getRegex("\"(http[^<>\"]*?)\" class=\"btn btn-default\">Continue to file").getMatch(0);
            if (continuelink == null) {
                continuelink = "/g/" + uid;
            }
            getPage(continuelink);
        }
        final String finallink = br.getRegex("target=\"_blank\" href=\"(http[^<>\"]*?)\" class=\"btn btn-default\"").getMatch(0);
        if (finallink == null) {
            if (br.containsHTML(">\\s*Generating failed\\. Requested file no longer available\\.")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
