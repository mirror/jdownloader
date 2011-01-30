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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

/**
 * supported: archiv doma, archiv markiza, fun tv, music tv (live stream capture
 * is not supported)
 * 
 * @author butkovip
 * 
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, urls = { "http://video\\.markiza\\.sk/archiv-tv-markiza/[-a-z0-9]+/[0-9]+", "http://doma\\.markiza\\.sk/archiv-doma/[-a-z0-9]+/[0-9]+", "http://video\\.markiza\\.sk/(mini-music-tv|fun-tv)/[0-9]+/[-a-z0-9]+/[0-9]+" }, flags = { 0, 0, 0 }, names = { "video.markiza.sk", "doma.markiza.sk", "video.markiza.sk" })
public class VideoMarkizaSk extends PluginForDecrypt {

    public VideoMarkizaSk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.clearCookies(getHost());
        br.setFollowRedirects(true);
        br.getPage(cryptedLink.getCryptedUrl());

        // retrieve playlist first
        String playlistId = br.getRegex("div onclick=\"flowplayerPlayerOnThumb[(]'video_player_(.*?)'").getMatch(0);
        if (null == playlistId || playlistId.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String playlistUrl = br.getRegex("var flowplayerJSConfigScript = '(.*?)'").getMatch(0);
        if (null == playlistUrl || playlistUrl.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String playlist = playlistUrl + "media=" + playlistId;
        br.getPage(playlist);

        // parse playlist for valid links
        String[][] links = br.getRegex("\"url\":\"http://vid1.markiza.sk/(.*?)[.]mp4\"").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                // we want valid entries only + no commercials
                if (null != link && 1 == link.length && null != link[0] && 0 < link[0].trim().length()) {
                    decryptedLinks.add(createDownloadlink("http://vid1.markiza.sk/" + link[0] + ".mp4"));
                }
            }
        }

        return decryptedLinks;
    }
}
