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
@DecrypterPlugin(revision = "$Revision: XXX$", interfaceVersion = 2, urls = { "http://video\\.markiza\\.sk/archiv-tv-markiza/[-a-z0-9]+/[0-9]+", "http://doma\\.markiza\\.sk/archiv-doma/[-a-z0-9]+/[0-9]+", "http://video\\.markiza\\.sk/(mini-music-tv|fun-tv)/[0-9]+/[-a-z0-9]+/[0-9]+" }, flags = { 0, 0, 0 }, names = { "video.markiza.sk", "doma.markiza.sk", "video.markiza.sk" })
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
        String playlist = br.getRegex("s1.addVariable[(]\"file\",encodeURIComponent[(]\"(.*?)\"[)][)];").getMatch(0);
        if (null == playlist || playlist.trim().length() == 0) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.getPage(playlist);

        String[][] links = br.getRegex("<item>\\s+<title>(.*?)</title>(.*?)medium=\"video\"\\s+url=\"(.*?)\"").getMatches();
        if (null != links && 0 < links.length) {
            for (String[] link : links) {
                // we want valid entries only + no commercials
                if (null != link && 3 == link.length && null != link[2] && 0 < link[2].length() && !link[0].trim().equals("Reklama:")) {
                    decryptedLinks.add(createDownloadlink(link[2]));
                }
            }
        }

        return decryptedLinks;
    }
}
