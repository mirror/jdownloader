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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroxia.com" }, urls = { "http://(www\\.)?eroxia.com/[A-Za-z0-9_\\-]+/\\d+/.*?\\.html" }, flags = { 0 })
public class EroxiaCom extends PluginForDecrypt {

    public EroxiaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>Sex Video \\- ([^<>\"]*?) \\- Amateur Homemade Porn Eroxia</title>").getMatch(0);
        String tempID = br.getRedirectLocation();
        if (tempID != null) {
            DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("(http://(www\\.)?homesexdaily\\.com/video/[^<>\"/]*?\\.html)").getMatch(0);
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink(tempID);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        tempID = br.getRegex("config=(http://(www\\.)?homesexdaily\\.com/flv_player/data/playerConfigEmbed/\\d+\\.xml)\\'").getMatch(0);
        if (tempID != null) {
            br.getPage(tempID);
            if (br.containsHTML("is marked as crashed and should be repaired")) {
                logger.info("Link broken/offline: " + parameter);
                return decryptedLinks;
            }
            // Handling not done yet, had no working links
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        // filename is needed for all stuff below
        if (filename == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        filename = Encoding.htmlDecode(filename.trim());
        tempID = br.getRegex("\\'flashvars\\',\\'\\&file=(http://[^<>\"]*?)\\&").getMatch(0);
        if (tempID != null) {
            final DownloadLink dl = createDownloadlink("directhttp://" + tempID);
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (br.containsHTML("\"http://video\\.megarotic\\.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (tempID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
