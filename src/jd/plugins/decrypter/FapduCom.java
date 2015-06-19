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
import jd.plugins.PluginForDecrypt;

//EmbedDecrypter 0.1.1
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fapdu.com" }, urls = { "http://(www\\.)?fapdu\\.com/[a-z0-9\\-]+" }, flags = { 0 })
public class FapduCom extends PluginForDecrypt {

    public FapduCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINK = "http://(www\\.)?fapdu\\.com/(search|embed|sitemaps|rss|hd|register|community|pornstars|videos|pics|emo|channels|upload).*?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(INVALIDLINK)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        // Offline link
        if (br.containsHTML(">This video was removed")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        // Invalid link
        if (br.containsHTML("The page you were looking for isn|>Page Not Found") || !br.containsHTML("id=\"sharing\"")) {
            logger.info("Link invalid: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        String filename = br.getRegex("<meta itemprop=\"name\" content=\"([^<>\"]*?)\">").getMatch(0);
        decryptedLinks = jd.plugins.components.PornEmbedParser.findEmbedUrls(this.br, filename);
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}