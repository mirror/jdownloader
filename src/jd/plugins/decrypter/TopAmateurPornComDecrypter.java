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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "topamateurporn.com" }, urls = { "http://(www\\.)?topamateurporn\\.com/[A-Za-z0-9\\-]+/\\d+/.*?\\.html" }, flags = { 0 })
public class TopAmateurPornComDecrypter extends PluginForDecrypt {

    public TopAmateurPornComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("No htmlCode read")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String tempID = br.getRedirectLocation();
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("\\&url=(http://(www\\.)homesexdaily\\.com/video/.*?\\.html)\\&wm").getMatch(0);
        if (tempID != null) {
            decryptedLinks.add(createDownloadlink(tempID));
            return decryptedLinks;
        }

        // Filename needed for all IDs below
        String filename = br.getRegex("<h2 class=\"detail\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("Viewing Media \\- ([^<>\"]*?)</title>").getMatch(0);
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        tempID = br.getRegex("\\&settings=(http://(www\\.)?eroxia\\.com/playerConfig\\.php[^<>\"]*?)\"").getMatch(0);
        if (tempID != null) {
            br.getPage(Encoding.htmlDecode(tempID));
            tempID = br.getRegex("flvMask:(http://[^<>\"]*?);").getMatch(0);
            if (tempID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(tempID));
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
