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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.JDUtilities;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "music.163.com" }, urls = { "http://(www\\.)?music\\.163\\.com/(?:#/)?(?:album\\?id=|artist/album\\?id=|playlist\\?id=)\\d+" })
public class Music163Com extends PluginForDecrypt {

    public Music163Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE_ALBUM = "http://(www\\.)?music\\.163\\.com/(?:#/)?album\\?id=\\d+";
    private static final String TYPE_ARTIST       = "http://(www\\.)?music\\.163\\.com/(?:#/)?artist/album\\?id=\\d+";
    private static final String TYPE_PLAYLIST     = "http://(www\\.)?music\\.163\\.com/(?:#/)?playlist\\?id=\\d+";

    /** Settings stuff */
    private static final String FAST_LINKCHECK    = "FAST_LINKCHECK";
    private static final String GRAB_COVER        = "GRAB_COVER";

    /* Other possible API calls: http://music.163.com/api/playlist/detail?id=%s http://music.163.com/api/artist/%s */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister host plugin */
        JDUtilities.getPluginForHost(this.getHost());
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String lid = new Regex(parameter, "(\\d+)$").getMatch(0);
        String formattedDate = null;
        final SubConfiguration cfg = SubConfiguration.getConfig("music.163.com");
        final boolean fastcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        final String[] qualities = jd.plugins.hoster.Music163Com.audio_qualities;
        LinkedHashMap<String, Object> entries = null;
        ArrayList<Object> resourcelist = null;
        jd.plugins.hoster.Music163Com.prepareAPI(this.br);
        if (parameter.matches(TYPE_ARTIST)) {
            br.getPage("http://music.163.com/api/artist/albums/" + lid + "?id=" + lid + "&offset=0&total=true&limit=1000");
            if (isApiOffline(this.br)) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            resourcelist = (ArrayList) entries.get("hotAlbums");
            for (final Object albumo : resourcelist) {
                final LinkedHashMap<String, Object> album_info = (LinkedHashMap<String, Object>) albumo;
                final String album_id = Long.toString(JavaScriptEngineFactory.toLong(album_info.get("id"), -1));
                final DownloadLink dl = createDownloadlink("http://music.163.com/album?id=" + album_id);
                decryptedLinks.add(dl);
            }
        } else {
            long publishedTimestamp = 0;
            LinkedHashMap<String, Object> artistinfo = null;
            String name_artist = null;
            String name_album = null;
            String fpName = null;
            String coverurl = null;
            String name_creator = null;
            String name_playlist = null;

            if (parameter.matches(TYPE_PLAYLIST)) {
                /* Playlist */
                br.getPage("http://music.163.com/api/playlist/detail?id=" + lid);
                if (isApiOffline(this.br)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("result");
                artistinfo = (LinkedHashMap<String, Object>) entries.get("creator");
                resourcelist = (ArrayList) entries.get("tracks");

                coverurl = (String) entries.get("coverImgUrl");
                name_playlist = (String) entries.get("name");
                name_creator = (String) artistinfo.get("signature");
                fpName = name_creator + " - " + name_playlist;
            } else {
                /* Album */
                br.getPage("http://music.163.com/api/album/" + lid + "/");
                if (isApiOffline(this.br)) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("album");
                artistinfo = (LinkedHashMap<String, Object>) entries.get("artist");
                coverurl = (String) entries.get("picUrl");
                publishedTimestamp = JavaScriptEngineFactory.toLong(entries.get("publishTime"), 0);
                resourcelist = (ArrayList) entries.get("songs");
                name_album = (String) entries.get("name");
                name_artist = (String) artistinfo.get("name");
                fpName = name_artist + " - " + name_album;
            }
            final DecimalFormat df;
            if (resourcelist.size() < 100) {
                df = new DecimalFormat("00");
            } else {
                df = new DecimalFormat("000");
            }
            int counter = 1;
            if (publishedTimestamp > 0) {
                final SimpleDateFormat formatter = new SimpleDateFormat(jd.plugins.hoster.Music163Com.dateformat_en);
                formattedDate = formatter.format(publishedTimestamp);
            }
            for (final Object songo : resourcelist) {
                String ext = null;
                long filesize = 0;
                final LinkedHashMap<String, Object> song_info = (LinkedHashMap<String, Object>) songo;
                final ArrayList<Object> artists = (ArrayList) song_info.get("artists");
                final LinkedHashMap<String, Object> artist_info = (LinkedHashMap<String, Object>) artists.get(0);
                final String content_title = (String) song_info.get("name");
                final String fid = Long.toString(JavaScriptEngineFactory.toLong(song_info.get("id"), -1));
                final String tracknumber = df.format(counter);
                final String artist = (String) artist_info.get("name");
                /* Now find the highest quality available */
                for (final String quality : qualities) {
                    final Object musicO = song_info.get(quality);
                    if (musicO != null) {
                        final LinkedHashMap<String, Object> musicmap = (LinkedHashMap<String, Object>) musicO;
                        ext = (String) musicmap.get("extension");
                        filesize = JavaScriptEngineFactory.toLong(musicmap.get("size"), -1);
                        break;
                    }
                }

                if (ext == null || content_title == null || fid.equals("-1") || filesize == -1) {
                    return null;
                }
                final DownloadLink dl = createDownloadlink("http://music.163.com/song?id=" + fid);
                dl.setLinkID(fid);
                dl.setProperty("tracknumber", tracknumber);
                dl.setProperty("directtitle", content_title);
                dl.setProperty("directartist", artist);
                dl.setProperty("contentid", fid);
                dl.setProperty("type", ext);
                if (name_album != null) {
                    dl.setProperty("directalbum", name_album);
                }
                if (publishedTimestamp > 0) {
                    dl.setProperty("originaldate", publishedTimestamp);
                }
                final String name_song = jd.plugins.hoster.Music163Com.getFormattedFilename(dl);
                dl.setName(name_song);
                dl.setAvailable(true);
                dl.setDownloadSize(filesize);
                decryptedLinks.add(dl);
                counter++;
            }
            if (cfg.getBooleanProperty(GRAB_COVER, false) && coverurl != null) {
                final DownloadLink dlcover = createDownloadlink("decrypted://music.163.comcover" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String ext = getFileNameExtensionFromString(coverurl, ".jpg");
                if (fastcheck) {
                    dlcover.setAvailable(true);
                }
                dlcover.setContentUrl(parameter);
                dlcover.setProperty("mainlink", parameter);
                dlcover.setProperty("directlink", coverurl);
                dlcover.setProperty("contentid", lid);
                if (name_creator != null) {
                    dlcover.setProperty("directartist", name_creator);
                }
                dlcover.setProperty("type", ext);
                if (name_album != null) {
                    dlcover.setProperty("directalbum", name_album);
                }
                if (publishedTimestamp > 0) {
                    dlcover.setProperty("originaldate", publishedTimestamp);
                }
                final String name_cover = jd.plugins.hoster.Music163Com.getFormattedFilename(dlcover);
                dlcover.setName(name_cover);
                decryptedLinks.add(dlcover);
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                try {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                }
                return decryptedLinks;
            }
            if (formattedDate != null) {
                fpName = formattedDate + " - " + fpName;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    public static boolean isApiOffline(final Browser br) {
        final String msg = PluginJSonUtils.getJsonValue(br, "msg");
        if (br.getHttpConnection().getResponseCode() != 200 || "no resource".equalsIgnoreCase(msg)) {
            return true;
        } else {
            return false;
        }
    }
}
