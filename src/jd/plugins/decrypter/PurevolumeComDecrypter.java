//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "purevolume.com" }, urls = { "http://(www\\.)?purevolume\\.com/(new/)?\\w+(/albums/[\\w\\+\\-]+)?" }, flags = { 0 })
public class PurevolumeComDecrypter extends PluginForDecrypt {

    public PurevolumeComDecrypter(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?purevolume\\.com/(search|signup|faq|artist_promotion|labels|news|festivals|events|advertise|past_features|login|charts|support|top_songs|about_us|browse|people)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("id=\"page_not_found\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getURL().contains("purevolume.com/login") || parameter.matches(INVALIDLINKS)) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 403) {
            logger.info("Link offline (server error): " + parameter);
            return decryptedLinks;
        }
        String type = "Artist";
        String songId = br.getRegex("\'Artist\'\\s?,\\s?\'(\\d+)\'").getMatch(0);
        if (songId == null) {
            songId = br.getRegex("\'Album\'\\s?,\\s?\'(\\d+)\'").getMatch(0);
            type = "Album";
        }

        if (songId == null) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }

        FilePackage fp = FilePackage.getInstance();
        br.getPage("/_controller/playlist.php?userType=" + type + "&userId=" + songId + "&type=" + type + "Playlist&offset=0&moduleId=player_container&action=getMoreTracks");
        String playListItems = br.getRegex("<playlist_items>(.*?)</playlist_items>").getMatch(0);

        if (playListItems == null) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }

        br.getRequest().setHtmlCode(Encoding.htmlDecode(playListItems));

        String artist = br.getRegex("\'album_name\'\\s?:\\s?\'([^\']+)'").getMatch(0);
        String album = br.getRegex("\'artist_name\'\\s?:\\s?\'([^\']+)'").getMatch(0);
        if (artist == null) artist = "Unknown";
        if (album == null) album = "Unknown";

        fp.setName(Encoding.htmlDecode(album.trim() + "@" + artist.trim()).replaceAll("\\\\", ""));

        parameter = parameter.replace("http://", "decrypted://");

        for (String item[] : br.getRegex("alias=\"([^\"]+)\">([^<]+)</a>").getMatches()) {
            DownloadLink link = createDownloadlink(parameter + "&songId=" + item[0]);
            link.setProperty("SONGID", item[0]);
            link.setFinalFileName(Encoding.htmlDecode(item[1].trim()) + ".mp3");
            if (!getPluginConfig().getBooleanProperty("FILESIZECHECK", false)) link.setAvailable(true);
            fp.add(link);
            try {
                distribute(link);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(link);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}