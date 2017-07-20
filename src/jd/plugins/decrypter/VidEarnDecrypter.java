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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornxs.com" }, urls = { "https?://(www\\.)?pornxs\\.com/(video\\.php\\?id=|playlists/[^/]+/(?:[^/]\\.html)?|[a-z0-9\\-]+/\\d+\\-[a-z0-9\\-]*?\\.html)|https?://embed\\.pornxs\\.com/embed\\.php\\?id=\\d+" })
public class VidEarnDecrypter extends antiDDoSForDecrypt {
    public VidEarnDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This plugin takes videarn links and checks if there is also a filearn.com link available (partnersite)
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<String> dupelist = new ArrayList<String>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        final String vid = new Regex(parameter, "embed\\.php\\?id=(\\d+)$").getMatch(0);
        if (vid != null) {
            parameter = "http://pornxs.com/teen-amateur-mature-cumshot-webcams/" + vid + "-0123456789.html";
        }
        final String url_name;
        final boolean isPlaylist;
        final FilePackage fp = FilePackage.getInstance();
        String fpName = null;
        int counter = 0;
        int counterMax;
        if (parameter.matches(".+/playlists/\\d+\\-[^/]+/.*?")) {
            isPlaylist = true;
            final String playlist_id_and_name = new Regex(parameter, "playlists/(\\d+\\-[^/]+)").getMatch(0);
            /*
             * Make sure that we start the playlist from position 1 (although it might make more sense to add settings here: Download only
             * current video, start from current, download all)
             */
            parameter = String.format("http://pornxs.com/playlists/%s/", playlist_id_and_name);
            url_name = new Regex(playlist_id_and_name, "\\d+\\-([^/]+)").getMatch(0);
            fpName = url_name;
            /* This is only temporary */
            counterMax = 0;
        } else {
            isPlaylist = false;
            counterMax = 0;
        }
        String nextVideo = parameter;
        do {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            logger.info("Crawling object " + counter + " of " + counterMax);
            if (nextVideo.startsWith("/")) {
                nextVideo = "http://" + this.br.getHost() + nextVideo;
            }
            getPage(nextVideo);
            final DownloadLink mainlink;
            if (isPlaylist) {
                if (counter == 0) {
                    /* Try to find playlist-item-count */
                    final String count_items = this.br.getRegex("<em>total</em>\\s*?(\\d+) video").getMatch(0);
                    if (count_items != null) {
                        counterMax = Integer.parseInt(count_items);
                    } else {
                        /* Don't let an infinite loop happen! */
                        counterMax = 9;
                    }
                }
                final String videoidCurrent = this.br.getRegex("currentVideoId = (\\d+)").getMatch(0);
                if (videoidCurrent == null) {
                    logger.warning("videoidCurrent = null");
                    break;
                }
                if (dupelist.contains(videoidCurrent)) {
                    logger.info("Found dupe --> Stopping");
                    break;
                } else {
                    dupelist.add(videoidCurrent);
                }
                /* We have to build the correct URL for the current video manually ... */
                final String url_video = String.format("http://pornxsdecrypted.com/teen-video/%s-.html", videoidCurrent);
                mainlink = createDownloadlink(url_video);
            } else {
                /* URL for current video is given by user */
                mainlink = createDownloadlink(nextVideo.replaceAll("pornxs\\.com/", "pornxsdecrypted.com/"));
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                mainlink.setAvailable(false);
                mainlink.setProperty("offline", true);
            } else {
                String videoTitle = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
                if (videoTitle == null) {
                    videoTitle = nextVideo;
                }
                videoTitle = Encoding.htmlDecode(videoTitle);
                videoTitle = videoTitle.trim();
                if (isPlaylist) {
                    fp.setName(fpName);
                } else {
                    fp.setName(videoTitle);
                }
                String additionalDownloadlink = br.getRegex("\"(http://(www\\.)?filearn\\.com/files/get/.*?)\"").getMatch(0);
                if (additionalDownloadlink == null) {
                    additionalDownloadlink = br.getRegex("<div class=\"video\\-actions\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
                }
                if (additionalDownloadlink != null) {
                    final DownloadLink xdl = createDownloadlink(additionalDownloadlink);
                    xdl.setProperty("videarnname", videoTitle);
                    xdl._setFilePackage(fp);
                    decryptedLinks.add(xdl);
                    distribute(xdl);
                }
                mainlink.setName(videoTitle + ".mp4");
                mainlink.setAvailable(true);
            }
            if (!br.containsHTML("id=\"video\\-player\"|playerMainConfig")) {
                /* Check here to prevent faulty offline URLs. */
                mainlink.setAvailable(false);
                mainlink.setProperty("offline", true);
            }
            mainlink._setFilePackage(fp);
            decryptedLinks.add(mainlink);
            distribute(mainlink);
            nextVideo = this.br.getRegex("<a href=\"(/playlists/[^/]+/[^/]+\\.html)\".*?class=\"title single\">watch next</span>").getMatch(0);
            counter++;
        } while (nextVideo != null && counter <= counterMax);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}