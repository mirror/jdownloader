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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "reverbnation.com" }, urls = { "http://(www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|playlist/view_playlist/[0-9\\-]+\\?page_object=artist_\\d+|open_graph/song/\\d+|[A-Za-z0-9\\-_]+/song/\\d+|play_now/song_\\d+|page_object/page_object_photos/artist_\\d+|artist/downloads/\\d+|[A-Za-z0-9\\-_]{5,})" }, flags = { 0 })
public class ReverBnationCom extends PluginForDecrypt {

    public ReverBnationCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PLAYLISTLINK = "http://(www\\.)?reverbnation\\.com/playlist/view_playlist/[0-9\\-]+\\?page_object=artist_\\d+";
    private static final String INVALIDLINKS = "http://(www\\.)?reverbnation\\.com/(facebook_channel|main|mailto|user|promoter\\-promotion|fan\\-promotion|venue\\-promotion|label\\-promotion|javascript:|signup|appending|head|css|images|data:|band\\-promotion|static|https|press_releases|widgets|controller|yourboitc)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(false);
        if (parameter.matches("http://(www\\.)?reverbnation\\.com/page_object/page_object_photos/artist_\\d+")) {
            br.getPage(parameter);
            String fpName = getFpname();
            int counter = 1;
            DecimalFormat df = new DecimalFormat("000");
            final String[] pictures = br.getRegex("id=\"photo_\\d+\">[\t\n\r ]+<img data\\-crop=\"\\d+x\\d+\" data\\-full\\-size=\"(//[^<>\"]*?)\"").getColumn(0);
            for (String picture : pictures) {
                DownloadLink fina = createDownloadlink("directhttp://http:" + picture);
                String ext = picture.substring(picture.lastIndexOf("."));
                if (ext != null && ext.length() < 5 && fpName != null) {
                    fina.setFinalFileName("Photo " + df.format(counter) + ext);
                    counter++;
                }
                decryptedLinks.add(fina);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else if (parameter.matches("http://(www\\.)?reverbnation\\.com/(play_now/song_\\d+|open_graph/song/\\d+|[A-Za-z0-9\\-_]+/song/\\d+)")) {
            final String fileID = new Regex(parameter, "(\\d+)$").getMatch(0);
            if (parameter.matches("http://(www\\.)?reverbnation\\.com/open_graph/song/\\d+")) parameter = parameter.replace("open_graph/song/", "play_now/song_");
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String artistID = br.getRegex("onclick=\"playSongNow\\(\\'all_artist_songs_(\\d+)\\'\\)").getMatch(0);
            if (artistID == null) {
                artistID = br.getRegex("\\(\\'all_artist_songs_(\\d+)\\'\\)").getMatch(0);
                if (artistID == null) artistID = br.getRegex("artist/artist_songs/(\\d+)\\?").getMatch(0);
            }
            String filename = br.getRegex("data\\-song\\-id=\"" + fileID + "\" title=\"Play \\&quot;([^<>\"]*?)\\&quot;\"").getMatch(0);
            if (artistID == null || filename == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            final DownloadLink dlLink = createDownloadlink("http://reverbnationcomid" + fileID + "reverbnationcomartist" + artistID);
            if (filename.contains(".mp3"))
                dlLink.setName(filename);
            else
                dlLink.setName(filename + ".mp3");
            dlLink.setProperty("orgName", dlLink.getName());
            decryptedLinks.add(dlLink);
        } else if (parameter.matches("http://(www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|open_graph/song/\\d+|artist/downloads/\\d+|[^<>\"/]+)") || parameter.matches(PLAYLISTLINK)) {
            String fpName = null;
            String[][] allInfo = null;
            String artistID = null;
            if (parameter.matches("http://(www\\.)?reverbnation\\.com/(artist/artist_songs/\\d+|open_graph/song/\\d+)") || parameter.matches(PLAYLISTLINK)) {
                br.getPage(parameter);
                fpName = getFpname();
                allInfo = br.getRegex("data\\-url=\"/artist/artist_song/(\\d+)\\?song_id=(\\d+)\">.*?<a href=\"#\" class=\"[^<>\"/]+\" data\\-song\\-id=\"\\d+\" title=\"Play \\&quot;([^<>\"]*?)\\&quot;\"").getMatches();
            } else if (parameter.matches("http://(www\\.)?reverbnation\\.com/artist/downloads/\\d+")) {
                br.getPage(parameter);
                fpName = getFpname();
                allInfo = br.getRegex("production_public/Artist/(\\d+)/image/thumb/[a-z0-9_\\-]+\\.jpg\" /><a href=\"#\" class=\"size_48  standard_play_button song\\-action play\" data\\-song\\-id=\"(\\d+)\" title=\"Play &quot;([^<>\"]*?)&quot;\"").getMatches();
            } else {
                br.getPage(parameter);
                if (br.containsHTML(">Page Not Found<")) {
                    logger.info("Link offline/invalid: " + parameter);
                    return decryptedLinks;
                }
                if (br.containsHTML("rel=\"nofollow\" title=\"Listen to") || !br.containsHTML("<div class=\"profile_section_container profile_songs\">")) {
                    logger.info("No content to decrypt: " + parameter);
                    return decryptedLinks;
                }
                fpName = br.getRegex("<h1 class=\"profile_user_name\">([^<>\"]*?)</h1>").getMatch(0);
                final String showAllSongs = br.getRegex("<a href=\"([^<>\"]+/songs)\" class=\"standard_well see_more\">All Songs</a>").getMatch(0);
                if (showAllSongs != null) {
                    br.getPage("http://www.reverbnation.com" + showAllSongs);
                }
                artistID = br.getRegex("CURRENT_PAGE_OBJECT = \\'artist_(\\d+)\\';").getMatch(0);
                if (artistID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                allInfo = br.getRegex("data\\-song\\-id=\"(\\d+)\" title=\"Play \\&quot;([^<>\"]*?)\\&quot;\"").getMatches();
            }
            if (allInfo == null || allInfo.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleInfo[] : allInfo) {
                String name = null;
                String artistsID = artistID;
                String songID = null;
                if (artistsID != null) {
                    songID = singleInfo[0];
                    name = Encoding.htmlDecode(singleInfo[1]);
                } else {
                    artistsID = singleInfo[0];
                    songID = singleInfo[1];
                    name = Encoding.htmlDecode(singleInfo[2]);
                }
                final DownloadLink dlLink = createDownloadlink("http://reverbnationcomid" + songID + "reverbnationcomartist" + artistsID);
                if (name.contains(".mp3"))
                    dlLink.setName(name);
                else
                    dlLink.setName(name + ".mp3");
                dlLink.setProperty("orgName", dlLink.getName());
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

    private String getFpname() {
        String fpName = br.getRegex("<title>([^<>\"]*?) \\- ReverbNation</title>").getMatch(0);
        return fpName;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}