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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "reverbnation.com" }, urls = { "https?://(?:www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|playlist/view_playlist/[0-9\\-]+\\?page_object=artist_\\d+|open_graph/song/\\d+|[A-Za-z0-9\\-_]+/song/\\d+|play_now/song_\\d+|page_object/page_object_photos/artist_\\d+|artist/downloads/\\d+|[A-Za-z0-9\\-_]{5,})" })
public class ReverBnationCom extends antiDDoSForDecrypt {

    public ReverBnationCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PLAYLISTLINK = "https?://(?:www\\.)?reverbnation\\.com/playlist/view_playlist/[0-9\\-]+\\?page_object=artist_\\d+";
    private static final String INVALIDLINKS = "https?://(?:www\\.)?reverbnation\\.com/(facebook_channel|main|mailto|user|promoter\\-promotion|fan\\-promotion|venue\\-promotion|label\\-promotion|javascript:|signup|appending|head|css|images|data:|band\\-promotion|static|https|press_releases|widgets|controller|yourboitc)";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        /* Load host plugin */
        JDUtilities.getPluginForHost(this.getHost());
        String username = null;
        String title = null;
        String artist = null;
        String artistsID = null;
        String songID = null;
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        if (parameter.matches("https?://(www\\.)?reverbnation\\.com/page_object/page_object_photos/artist_\\d+")) {
            br.getPage(parameter);
            String fpName = null;
            int counter = 1;
            DecimalFormat df = new DecimalFormat("000");
            final String[] pictures = br.getRegex("id=\"photo_\\d+\">[\t\n\r ]+<img data\\-crop=\"\\d+x\\d+\" data\\-full\\-size=\"(//[^<>\"]*?)\"").getColumn(0);
            for (String picture : pictures) {
                DownloadLink fina = createDownloadlink("directhttp://http:" + picture);
                String ext = getFileNameExtensionFromString(picture);
                if (ext != null && ext.length() < 5 && fpName != null) {
                    fina.setFinalFileName("Photo " + df.format(counter) + ext);
                    counter++;
                }
                decryptedLinks.add(fina);
            }
        } else if (parameter.matches("https?://(www\\.)?reverbnation\\.com/(play_now/song_\\d+|open_graph/song/\\d+|[A-Za-z0-9\\-_]+/song/\\d+)")) {
            songID = new Regex(parameter, "(\\d+)$").getMatch(0);
            if (parameter.matches("https?://(www\\.)?reverbnation\\.com/open_graph/song/\\d+")) {
                parameter = parameter.replace("open_graph/song/", "play_now/song_");
            }
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                logger.info("Link offline: " + parameter);
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            username = br.getRegex("<a[^<>]*?href=\"/([^<>\"/]*?)\"[^<>]*?>« Back to Profile</a>").getMatch(0);
            if (username == null) {
                username = br.getRegex("property=\"og:url\" content=\"https?://(?:www\\.)?reverbnation\\.com/([^/]+)/song/").getMatch(0);
            }
            title = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (title == null) {
                title = br.getRegex("<title>([^<>\"]+) \\| ReverbNation</title>").getMatch(0);
            }
            if (title == null) {
                title = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
            }
            artist = br.getRegex("property=\"reverbnation_fb:musician\" content=\"([^<>\"]*?)\"").getMatch(0);
            if (artist == null) {
                artist = this.br.getRegex("class=\"profile\\-header__info__title qa\\-artist-name[^<>]*?\">\\s*([^<>\"]+)\\s*<").getMatch(0);
            }
            artistsID = br.getRegex("onclick=\"playSongNow\\(\\'all_artist_songs_(\\d+)\\'\\)").getMatch(0);
            if (artistsID == null) {
                artistsID = br.getRegex("\\(\\'all_artist_songs_(\\d+)\\'\\)").getMatch(0);
            }
            if (artistsID == null) {
                artistsID = br.getRegex("playSongNow\\(\\&#39;all_artist_songs_(\\d+)").getMatch(0);
            }
            if (artistsID == null) {
                artistsID = br.getRegex("artist/artist_songs/(\\d+)\\?").getMatch(0);
            }
            if (artistsID == null) {
                artistsID = br.getRegex("artist_id=(\\d+)").getMatch(0);
            }
            if (artistsID == null) {
                artistsID = br.getRegex("artist_(\\d+)").getMatch(0);
            }
            if (username == null || songID == null || artistsID == null || title == null || artist == null) {
                logger.info("username: " + username + ", songID: " + songID + ", artistsID: " + artistsID + ", title: " + title + ", artist: " + artist);
                throw new DecrypterException("Decrypter broken for link:" + parameter);
            }
            title = Encoding.htmlDecode(title).trim();
            artist = Encoding.htmlDecode(artist).trim();
            final DownloadLink dlLink = getSongDownloadlink(songID, artistsID);
            dlLink.setProperty("orgName", dlLink.getName());
            dlLink.setProperty("mainlink", parameter);
            dlLink.setProperty("directusername", username);
            dlLink.setProperty("directartist", artist);
            dlLink.setProperty("directtitle", title);
            dlLink.setName(jd.plugins.hoster.ReverBnationComHoster.getFormattedFilename(dlLink));
            dlLink.setAvailable(true);
            decryptedLinks.add(dlLink);
        } else if (parameter.matches("https?://(www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|open_graph/song/\\d+|artist/downloads/\\d+|[^<>\"/]+)") || parameter.matches(PLAYLISTLINK)) {
            String fpName = null;
            String[] allInfo = null;
            String artistID = null;
            String artist_name_general = null;
            if (parameter.matches("https?://(www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|open_graph/song/\\d+)") || parameter.matches(PLAYLISTLINK)) {
                br.getPage(parameter);
            } else if (parameter.matches("https?://(www\\.)?reverbnation\\.com/artist/downloads/\\d+")) {
                br.getPage(parameter);
                /* TODO! */
                throw new DecrypterException("Decrypter broken for link:" + parameter);
            } else {
                username = new Regex(parameter, "reverbnation\\.com/([^<>\"/]+)").getMatch(0);
                if (!parameter.endsWith("/songs")) {
                    parameter += "/songs";
                }
                br.getPage(parameter);
                if (br.containsHTML(">Page Not Found<")) {
                    logger.info("Link offline: " + parameter);
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                final String showAllSongs = br.getRegex("<a href=\"([^<>\"]+/songs)\" class=\"standard_well see_more\">All Songs</a>").getMatch(0);
                if (showAllSongs != null) {
                    br.getPage(showAllSongs);
                }
            }
            if (br.containsHTML("rel=\"nofollow\" title=\"Listen to") || !br.containsHTML("class=\"artist_name\"") || br.getHttpConnection().getResponseCode() == 404) {
                logger.info("No content to decrypt: " + parameter);
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            artist_name_general = br.getRegex("class=\"artist_name\">By: ([^<>\"]*?)</span>").getMatch(0);
            if (artist_name_general == null) {
                artist_name_general = br.getRegex("class=\"artist_name\">By: <span title=\"([^<>\"]*?)\"").getMatch(0);
            }
            artistID = PluginJSonUtils.getJsonValue(br, "current_page_object");
            if (artistID == null) {
                throw new DecrypterException("Decrypter broken for link:" + parameter);
            }
            artistID = new Regex(artistID, "\\d+$").getMatch(-1);
            allInfo = br.getRegex("<div class=\"play_details\">(.*?)</li>").getColumn(0);
            if (allInfo == null || allInfo.length == 0 || artist_name_general == null) {
                throw new DecrypterException("Decrypter broken for link:" + parameter);
            }
            if (username == null) {
                username = artist_name_general;
            }
            fpName = username + " - " + artist_name_general + " - " + artistID;
            for (final String singleInfo : allInfo) {
                artistsID = artistID;
                songID = new Regex(singleInfo, "data-song-id=\"(\\d+)\"").getMatch(0);
                if (songID == null) {
                    songID = new Regex(singleInfo, "download_song/(\\d+)").getMatch(0);
                }
                title = new Regex(singleInfo, "title=\"(?:Play \\&quot;)?([^<>\"]*?)(?:\\&quot;)?\"").getMatch(0);
                artist = new Regex(singleInfo, "<em>by <a href=\"/[^<>\"]*?\">([^<>\"]*?)</a>").getMatch(0);
                /* Maybe the whole user/playlist/whatever has only a single artist. */
                if (artist == null) {
                    artist = artist_name_general;
                }
                if (songID == null || title == null || artist == null) {
                    throw new DecrypterException("Decrypter broken for link:" + parameter);
                }
                title = Encoding.htmlDecode(title).trim();
                artist = Encoding.htmlDecode(artist).trim();
                final DownloadLink dlLink = getSongDownloadlink(songID, artistsID);
                dlLink.setProperty("orgName", dlLink.getName());
                dlLink.setProperty("mainlink", parameter);
                dlLink.setProperty("directusername", username);
                dlLink.setProperty("directartist", artist);
                dlLink.setProperty("directtitle", title);
                dlLink.setName(jd.plugins.hoster.ReverBnationComHoster.getFormattedFilename(dlLink));
                dlLink.setAvailable(true);
                decryptedLinks.add(dlLink);
            }

            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }

        }

        return decryptedLinks;
    }

    private DownloadLink getSongDownloadlink(final String songID, final String artistID) {
        final DownloadLink dlLink = createDownloadlink("http://reverbnationcomid" + songID + "reverbnationcomartist" + artistID);
        dlLink.setProperty("directsongid", songID);
        dlLink.setProperty("directartistid", artistID);
        final String content_url = createPlayUrl(songID);
        dlLink.setContentUrl(content_url);
        dlLink.setLinkID(songID);
        return dlLink;
    }

    private String createContentURL(final String songID) {
        final String content_url = "https://www." + this.getHost() + "/controller/audio_player/download_song/" + songID + "?modal=true";
        return content_url;
    }

    private String createPlayUrl(final String songID) {
        final String play_url = "https://www." + this.getHost() + "/play_now/song_" + songID;
        return play_url;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}