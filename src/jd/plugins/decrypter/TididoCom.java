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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tidido.com" }, urls = { "https?://(?:www\\.)?tidido\\.com/.+" })
public class TididoCom extends PluginForDecrypt {

    public TididoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String                  TYPE_ALBUM         = "https?://(?:www\\.)?tidido\\.com/(?:[a-z]{2}/)?(?:a[a-z0-9]+/)?al[a-z0-9]+";
    private final String                  TYPE_PLAYLIST      = "https?://(?:www\\.)?tidido\\.com/((?:[A-Za-z]{2}/)?u[a-z0-9]+/playlists/[a-z0-9]+)";
    private final String                  TYPE_PLAYLIST_MOOD = "https?://(?:www\\.)?tidido\\.com/([A-Za-z]{2})/moods/([^/]+)/([a-z0-9]+)";
    private final String                  TYPE_PLAYLIST_USER = "https?://(?:www\\.)?tidido\\.com/(?:[A-Za-z]{2}/)?u([a-z0-9]+)/playlists/([a-z0-9]+)";
    private final String                  TYPE_SONG          = "https?://(?:www\\.)?tidido\\.com/(?:[a-z]{2}/)?a[a-z0-9]+/al[a-z0-9]+/t[a-z0-9]+";
    private final String                  TYPE_ARTIST        = "https?://(?:www\\.)?tidido\\.com/(?:[a-z]{2}/)?a[a-z0-9]+.*?";

    private String                        fpName             = null;
    private String                        target_song_id     = null;
    private String                        album_id           = null;
    private String                        name_album         = null;
    private ArrayList<DownloadLink>       decryptedLinks     = new ArrayList<DownloadLink>();
    private LinkedHashMap<String, String> artist_info        = new LinkedHashMap<String, String>();
    private boolean                       fast_linkcheck     = false;

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final String parameter = param.toString();
        fast_linkcheck = SubConfiguration.getConfig("tidido.com").getBooleanProperty(jd.plugins.hoster.TididoCom.FAST_LINKCHECK, jd.plugins.hoster.TididoCom.defaultFAST_LINKCHECK);
        String name_playlist = null;

        fpName = null;
        target_song_id = null;
        album_id = null;

        String artist_id = null;
        String user_id = null;
        String playlist_areaname = null;
        String playlist_id = null;
        String playlist_mood = null;

        if (parameter.matches(TYPE_SONG)) {
            album_id = new Regex(parameter, "al([a-z0-9]+)").getMatch(0);
            artist_id = new Regex(parameter, "a([a-z0-9]+)").getMatch(0);
            target_song_id = new Regex(parameter, "t([^/]+)$").getMatch(0);
        } else if (parameter.matches(TYPE_ALBUM)) {
            album_id = new Regex(parameter, "al([a-z0-9]+)").getMatch(0);
            /* artist_id must not be given for album urls */
            artist_id = new Regex(parameter, "a([a-z0-9]+)").getMatch(0);
        } else if (parameter.matches(TYPE_ARTIST)) {
            artist_id = new Regex(parameter, "a([a-z0-9]+)").getMatch(0);
        } else if (parameter.matches(TYPE_PLAYLIST_MOOD)) {
            playlist_areaname = new Regex(parameter, TYPE_PLAYLIST_MOOD).getMatch(0);
            playlist_mood = new Regex(parameter, TYPE_PLAYLIST_MOOD).getMatch(1);
            playlist_id = new Regex(parameter, TYPE_PLAYLIST_MOOD).getMatch(2);
        } else if (parameter.matches(TYPE_PLAYLIST_USER)) {
            user_id = new Regex(parameter, TYPE_PLAYLIST_USER).getMatch(0);
            playlist_id = new Regex(parameter, TYPE_PLAYLIST_USER).getMatch(1);
        } else {
            /* Unsupported linktype */
            logger.info("Unsupported linktype");
            return decryptedLinks;
        }

        LinkedHashMap<String, Object> entries = null;
        LinkedHashMap<String, Object> entries2 = null;
        ArrayList<Object> ressourcelist = null;
        ArrayList<Object> song_array = null;
        if (parameter.matches(TYPE_PLAYLIST) || parameter.matches(TYPE_PLAYLIST_MOOD)) {
            if (parameter.matches(TYPE_PLAYLIST_MOOD)) {
                this.br.getPage("http://tidido.com/api/music/mood/" + playlist_mood + "?areaname=" + playlist_areaname + "&playlists=true");
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "playlists_data/playlists");
            } else {
                this.br.getPage("http://tidido.com/api/music/playlist?action=userPlaylists&u=" + user_id);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                ressourcelist = (ArrayList) entries.get("playlists");
            }
            /* Find song_ids of our playlist ... */
            for (final Object playlisto : ressourcelist) {
                entries2 = (LinkedHashMap<String, Object>) playlisto;
                final String playlistid_tmp = (String) entries2.get("id");
                final String playlistname = (String) entries2.get("name");
                if (playlistid_tmp == null || playlistname == null) {
                    continue;
                }
                if (playlistid_tmp.equals(playlist_id)) {
                    name_playlist = playlistname;
                    break;
                }
            }
            /* Collect song_ids to access all via URL */
            String song_ids_url = "/api/music/song?sids=";
            ressourcelist = (ArrayList) entries2.get("sids");
            for (final Object songido : ressourcelist) {
                song_ids_url += (String) songido + "_";
            }
            this.br.getPage(song_ids_url);
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            song_array = (ArrayList) entries.get("songs");
            ressourcelist = (ArrayList) entries.get("artists");
            /* Set packagename */
            if (playlist_mood != null) {
                fpName = playlist_mood + " - " + name_playlist;
            } else {
                fpName = name_playlist;
            }
            addSongs(song_array, ressourcelist);
        } else {
            if (album_id != null) {
                this.br.getPage("http://tidido.com/api/music/album/" + album_id);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                song_array = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "songs/songs");
                name_album = (String) JavaScriptEngineFactory.walkJson(entries, "songs/albums/{0}/name");
                ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "songs/artists");
                addSongs(song_array, ressourcelist);
            } else {
                final int max_entries = 100;
                int offset = 0;
                int added_entries = 0;
                do {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user");
                        return decryptedLinks;
                    }
                    this.br.getPage("http://tidido.com/api/gene/artist/" + artist_id + "?section=topSongs&limit=" + max_entries + "&offset=" + offset);
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                    song_array = (ArrayList) entries.get("songs");
                    ressourcelist = (ArrayList) entries.get("artists");
                    addSongs(song_array, ressourcelist);
                    offset += song_array.size();
                    added_entries = song_array.size();
                } while (added_entries >= max_entries - 10);
            }
        }

        return decryptedLinks;
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    private void addSongs(final ArrayList<Object> song_array, final ArrayList<Object> ressourcelist) {
        LinkedHashMap<String, Object> entries2 = null;
        LinkedHashMap<String, String> artist_info = new LinkedHashMap<String, String>();

        /* First fill artist_info */
        for (final Object artisto : ressourcelist) {
            entries2 = (LinkedHashMap<String, Object>) artisto;
            final String artistid = (String) entries2.get("id");
            final String artistname = (String) entries2.get("fullname");
            if (artistid == null || artistname == null) {
                continue;
            }
            if (fpName == null) {
                if (name_album != null) {
                    fpName = artistname + " - " + name_album;
                } else {
                    fpName = artistname;
                }
            }
            artist_info.put(artistid, artistname);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));

        for (final Object songo : song_array) {
            entries2 = (LinkedHashMap<String, Object>) songo;
            String artist = null;
            final long artistid = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries2, "artistIds/{0}"), 0);
            final String songid = (String) entries2.get("id");
            final String songname = (String) entries2.get("name");
            final String directlink = (String) entries2.get("url");
            final long bitrate = JavaScriptEngineFactory.toLong(entries2.get("bitrate"), 0);
            String albumID_this_song = (String) entries2.get("albumId");
            if (albumID_this_song == null) {
                /* Small fallback attempt */
                albumID_this_song = album_id;
            }
            if (albumID_this_song == null || songid == null || songname == null) {
                continue;
            }
            String filename_temp = null;
            String filename = null;
            if (artistid != 0 && artist_info.containsKey(Long.toString(artistid))) {
                artist = artist_info.get(Long.toString(artistid));
                filename = artist + " - " + songname;
            } else {
                filename = songname;
            }
            final String bitrate_str;
            if (bitrate == 0) {
                bitrate_str = "_bitrate_unknown";
            } else {
                bitrate_str = "_bitrate_" + bitrate;
            }
            filename = encodeUnicode(filename);
            /* Include bitrate in temp filename but not in final filename. */
            filename_temp = filename + "_" + bitrate_str + ".mp3";
            filename += ".mp3";

            final String content_url = "http://tidido.com/a" + artistid + "/al" + albumID_this_song + "/t" + songid;
            final String content_url_decrypted = "http://tididodecrypted.com/a" + artistid + "/al" + albumID_this_song + "/t" + songid;
            final DownloadLink dl = createDownloadlink(content_url_decrypted);
            dl.setContentUrl(content_url);
            dl.setName(filename_temp);
            dl._setFilePackage(fp);
            dl.setProperty("decryptedfilename", filename);
            dl.setProperty("directlink", directlink);
            if (target_song_id != null && songid.equals(target_song_id)) {
                /* We were looking for one specified track only - remove previously added data and step out of the loop. */
                /*
                 * Do not set availibiity as used only added a single URL --> Does not take much time to check and it makes sense to always
                 * show the filesize.
                 */
                decryptedLinks.clear();
                decryptedLinks.add(dl);
                distribute(dl);
                return;
            } else {
                /* Simply add track - set availibility on user choice to speed up decryption of many links. */
                if (fast_linkcheck) {
                    dl.setAvailable(true);
                }
                distribute(dl);
                decryptedLinks.add(dl);
            }

        }
    }

}
