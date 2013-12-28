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

//Finds and decrypts embedded videos from worldstarhiphop.com
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "worldstarhiphop.com" }, urls = { "http://(www\\.)?worldstarhiphop\\.com/videos/video(\\d+)?\\.php\\?v=[a-zA-Z0-9]+" }, flags = { 0 })
public class WorldStarHipHopComDecrypter extends PluginForDecrypt {

    public WorldStarHipHopComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String externID = br.getRegex("\"file\",\"(http://(www\\.)?youtube\\.com/v/[^<>\"]*?)\"\\);").getMatch(0);
        if (externID == null) externID = br.getRegex("\"file\",\"(https?://(www\\.)?youtube\\.com/v/[A-Za-z0-9\\-_]+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://player\\.vimeo\\.com/video/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://media\\.mtvnservices\\.com/(embed/)?mgid:uma:video:mtv\\.com:\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(https?://(www\\.)?facebook\\.com/video/embed\\?video_id=\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(externID.trim()));
            dl.setProperty("nologin", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(http://cdnapi\\.kaltura\\.com/index\\.php/kwidget/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("<iframe src=\"(http://(www\\.)?bet\\.com/[^<>\"]*?)\" ").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        // Probably no external video, pass it over to the hoster plugin
        final DownloadLink dl = createDownloadlink(parameter.replace("worldstarhiphop.com/", "worldstarhiphopdecrypted.com/"));
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}