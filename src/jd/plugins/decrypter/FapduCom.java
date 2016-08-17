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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "fapdu.com" }, urls = { "http://(www\\.)?fapdu\\.com/[a-z0-9\\-]+" }) 
public class FapduCom extends PornEmbedParser {

    public FapduCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINK = "https?://(?:www\\.)?fapdu\\.com/(search|embed|sitemaps|rss|hd|register|community|pornstars|videos|pics|emo|channels|upload).*?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(INVALIDLINK)) {
            decryptedLinks.add(createOfflinelink(parameter, "Invalid Link"));
            return decryptedLinks;
        }
        br.getPage(parameter);
        // Offline link
        if (br.containsHTML(">This video was removed")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(createOfflinelink(parameter, "Offline Link"));
            return decryptedLinks;
        }
        // Invalid link
        if (br.containsHTML("The page you were looking for isn|>Page Not Found") || !br.containsHTML("id=\"sharing\"")) {
            decryptedLinks.add(createOfflinelink(parameter, "Invalid Link"));
            return decryptedLinks;
        }
        String filename = br.getRegex("<meta itemprop=\"name\" content=\"([^<>\"]*?)\">").getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }

        /* Assume we have a selfhosted video */
        decryptedLinks = new ArrayList<DownloadLink>();
        final DownloadLink main = this.createDownloadlink(parameter.replace("fapdu.com/", "fapdudecrypted.com/"));
        main.setContentUrl(parameter);
        decryptedLinks.add(main);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}