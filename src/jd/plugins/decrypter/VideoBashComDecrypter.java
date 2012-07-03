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
 * This decrypted decrypts embedded videos from videobash.com.. If no embedded
 * video is found the link gets passed over to the hosterplugin
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobash.com" }, urls = { "http://(www\\.)?videobash\\.com/video_show/[a-z0-9\\-]+\\-\\d+" }, flags = { 0 })
public class VideoBashComDecrypter extends PluginForDecrypt {

    public VideoBashComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"/>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>xxxbunker.com : ([^<>\"]*?)</title>").getMatch(0);
        }
        String externID = br.getRegex("youtube\\.com/embed/([^<>\"]*?)\\?autoplay=").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.youtube.com/watch?v=" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("recordsetter\\.com/embedvideo/(\\d+)\\?").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://recordsetter.com/world-record/x/" + externID));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("videobash.com/", "videobashdecrypted.com/")));
        return decryptedLinks;
    }

}
