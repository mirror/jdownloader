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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 * Decrypts embedded vidobu links, the hosterplugin for this site exists for the other kind of links
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vidobu.com" }, urls = { "http://(www\\.)?vidobu\\.com/videolar/[a-z0-9\\-_]+/(\\d+/)?" }, flags = { 0 })
public class VidobuComDecrypter extends PluginForDecrypt {

    public VidobuComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("<a href=\"https?://(www\\.)?vidobu\\.com/satinal\\.php\"><img src=\"https?://(www\\.)?vidobu.com/images/uyari_ekran\\.png\"")) {
            logger.info("Video only available for registered users: " + parameter);
            return decryptedLinks;
        } else if (br.containsHTML("class=\"lock icon\"")) {
            logger.info("Video is private: " + parameter);
            return decryptedLinks;
        }
        String externID = br.getRegex("\"(http://player\\.vimeo\\.com/video/\\d+)").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            dl.setProperty("Referer", parameter);
            decryptedLinks.add(dl);
        }
        externID = br.getRegex("videolist: \"(http://[A-Za-z0-9\\-_]+\\.mynet\\.com/services/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(externID);
            decryptedLinks.add(dl);
        }
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}