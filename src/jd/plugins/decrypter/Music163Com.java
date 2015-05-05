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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "music.163.com" }, urls = { "http://(www\\.)?music\\.163\\.com/(#/)?album\\?id=\\d+" }, flags = { 0 })
public class Music163Com extends PluginForDecrypt {

    public Music163Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Settings stuff */
    private static final String FAST_LINKCHECK = "FAST_LINKCHECK";

    /* Other possible API calls: http://music.163.com/api/playlist/detail?id=%s http://music.163.com/api/artist/%s */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load sister host plugin */
        JDUtilities.getPluginForHost("music.163.com");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String id_album = new Regex(parameter, "(\\d+)$").getMatch(0);
        final SubConfiguration cfg = SubConfiguration.getConfig("music.163.com");
        final boolean fastcheck = cfg.getBooleanProperty(FAST_LINKCHECK, false);
        jd.plugins.hoster.Music163Com.prepareAPI(this.br);
        br.getPage("http://music.163.com/api/album/" + id_album + "/");
        // br.getPage("http://music.163.com/api/artist/albums/10557?offset=0&");
        // br.getPage("http://music.163.com/api/artist/albums/" + id_album + "?offset=0&limit=1000");
        // br.getPage("http://music.163.com/api/artist/<id>/");
        if (br.getHttpConnection().getResponseCode() != 200) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("album");
        LinkedHashMap<String, Object> artistinfo = (LinkedHashMap<String, Object>) entries.get("artist");
        final ArrayList<Object> songs = (ArrayList) entries.get("songs");
        final String name_artist = (String) artistinfo.get("name");
        final String name_album = (String) entries.get("name");
        final String fpName = name_artist + " - " + name_album;
        for (final Object songo : songs) {
            String ext = null;
            long filesize = 0;
            final LinkedHashMap<String, Object> song_info = (LinkedHashMap<String, Object>) songo;
            final Object song_hMusico = song_info.get("hMusic");
            final LinkedHashMap<String, Object> song_bMusic = (LinkedHashMap<String, Object>) song_info.get("bMusic");
            final String songname = (String) song_info.get("name");
            final String fid = Long.toString(jd.plugins.hoster.DummyScriptEnginePlugin.toLong(song_info.get("id"), -1));
            if (song_hMusico != null) {
                /* HQ (usually 320 KB/s) [officially] only available for registered users */
                final LinkedHashMap<String, Object> song_hMusic = (LinkedHashMap<String, Object>) song_hMusico;
                ext = (String) song_hMusic.get("extension");
                filesize = jd.plugins.hoster.DummyScriptEnginePlugin.toLong(song_hMusic.get("size"), -1);
            } else {
                /* LQ */
                ext = (String) song_bMusic.get("extension");
                filesize = jd.plugins.hoster.DummyScriptEnginePlugin.toLong(song_bMusic.get("size"), -1);
            }
            final String filename = name_artist + " - " + name_album + " - " + songname + "." + ext;
            final DownloadLink dl = createDownloadlink("http://music.163.com/song?id=" + fid);
            dl.setLinkID(fid);
            dl.setFinalFileName(filename);
            // if (fastcheck) {
            // dl.setAvailable(true);
            // }
            dl.setAvailable(true);
            dl.setDownloadSize(filesize);
            decryptedLinks.add(dl);
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
