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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eroxia.com" }, urls = { "http://(www\\.)?eroxia.com/([A-Za-z0-9_\\-]+/\\d+/.*?|video/[a-z0-9\\-]+\\d+)\\.html" }, flags = { 0 })
public class EroxiaCom extends PluginForDecrypt {

    public EroxiaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        boolean dh = false;
        br.getPage(parameter);
        String filename = br.getRegex("<title>Sex Video \\- ([^<>\"]*?) \\- Amateur Homemade Porn Eroxia</title>").getMatch(0);
        String tempID = br.getRedirectLocation();

        /* homesexdaily */
        if (tempID == null) {
            tempID = br.getRegex("(http://(www\\.)?homesexdaily\\.com/video/[^<>\"/]*?\\.html)").getMatch(0);
            if (tempID == null) {
                if (tempID == null) tempID = br.getRegex("config=(http://(www\\.)?homesexdaily\\.com/flv_player/data/playerConfigEmbed/\\d+\\.xml)\\'").getMatch(0);
                if (tempID == null) tempID = br.getRegex("src=\"(http://www.homesexdaily.com/stp/embed\\.php\\?video=[^\"]+)").getMatch(0);
                if (tempID != null) {
                    br.getPage(tempID);
                    if (br.containsHTML("is marked as crashed and should be repaired")) {
                        logger.info("Link broken/offline: " + parameter);
                        return decryptedLinks;
                    }
                }
                tempID = br.getRegex("\\'flashvars\\',\\'\\&file=(http://[^<>\"]*?)\\&").getMatch(0);
                if (tempID == null) tempID = br.getRegex(" escape\\(\'(http://[^\'\\)]+)").getMatch(0);
                if (tempID != null) {
                    dh = true;
                    filename = tempID.endsWith(".mp4") ? filename + ".mp4" : filename + ".flv";
                }
            }
        }
        /* freeadultmedia */
        if (br.containsHTML("<param name=\"movie\" value=\"http://www\\.freeadultmedia\\.com/hosted/famplayer\\.swf\"")) {
            String file = br.getRegex("<param name=\"FlashVars\" value=\"file=(.*?)\\&").getMatch(0);
            br.getPage("http://www.freeadultmedia.com/famconfig.xml");
            String server = br.getRegex("<server>(.*?)</server>").getMatch(0);
            if (server == null || file == null) return null;
            tempID = server + file;
            filename = filename + ".flv";

        }
        if (br.containsHTML("\"http://video\\.megarotic\\.com/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Assume that we can hand it over to the host plugin
        if (tempID == null) {
            final DownloadLink dl = createDownloadlink(parameter.replace("eroxia.com/", "eroxiadecrypted.com/"));
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (dh) tempID = "directhttp://" + tempID;
        final DownloadLink dl = createDownloadlink(tempID);
        if (filename != null) dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}