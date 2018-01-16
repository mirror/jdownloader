//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "8tracks.com" }, urls = { "https?://(www\\.)?8tracks\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+" })
public class EightTracksCom extends PluginForDecrypt {
    // NOTE: short link is within Rdrctr.
    private static final String  MAINPAGE          = "https://8tracks.com/";
    private static final String  UNSUPPORTEDLINKS  = "https?://(www\\.)?8tracks\\.com/((assets_js|explore|auth|settings|mixes|developers|users|job)/.+|[\\w\\-]+/homepage|sets/new|collections/.+|sonos/.+)";
    private static final String  TYPE_GENERAL      = "https?://(www\\.)?8tracks\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+";
    private static final String  TYPE_SINGLE_TRACK = "https?://(www\\.)?8tracks\\.com/tracks/\\d+";
    private String               clipData;
    private static final String  TEMP_EXT          = ".mp3";
    private static final boolean TEST_MODE         = false;
    private static final String  TEST_MODE_TOKEN   = null;

    public EightTracksCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>(0);
        String parameter = param.toString();
        final DownloadLink offline = createDownloadlink("https://8tracksdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
        offline.setName(new Regex(parameter, "8tracks\\.com/(.+)").getMatch(0));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        setBrowserExclusive();
        if (parameter.matches(UNSUPPORTEDLINKS)) {
            logger.info("Invalid link: " + parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.setReadTimeout(90 * 1000);
        /* nachfolgender UA sorgt für bessere Audioqualität */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (webOS/2.1.0; U; en-US) AppleWebKit/532.2 (KHTML, like Gecko) Version/1.0 Safari/532.2 Pre/1.2");
        br.getPage(parameter);
        if (br.containsHTML(">Sorry, that page doesn't exist")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.getURL().contains("/explore/")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML(">The mix you're looking for is currently in private mode")) {
            logger.info("Link offline (this is a private link): " + parameter);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (parameter.matches(TYPE_SINGLE_TRACK)) {
            final String artist = br.getRegex("<h2 id=\"artist_name\">([^<>\"]*?)</h2>").getMatch(0);
            final String album = br.getRegex(">Album:</strong>([^<>\"]*?)\\&nbsp;\\&nbsp;").getMatch(0);
            final String songName = br.getRegex("\"name\":\"([^<>\"]*?)\"").getMatch(0);
            if (artist == null || album == null || songName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String track_id = new Regex(parameter, "(\\d+)$").getMatch(0);
            final DownloadLink single_track = createDownloadlink("http://8tracksdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
            final String temp_name = Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(album.trim()) + " - " + Encoding.htmlDecode(songName.trim());
            single_track.setName(temp_name + TEMP_EXT);
            single_track.setProperty("trackid", track_id);
            single_track.setProperty("mainlink", parameter);
            single_track.setProperty("tempname", temp_name);
            single_track.setProperty("tempname_with_ext", temp_name + TEMP_EXT);
            single_track.setProperty("tracknumber", -1);
            single_track.setProperty("single_link", true);
            single_track.setAvailable(true);
            single_track.setContentUrl(parameter);
            decryptedLinks.add(single_track);
        } else {
            String mixid = br.getRegex("mix_id=(\\d+)\"").getMatch(0);
            if (mixid == null) {
                mixid = br.getRegex("/mixes/(\\d+)/").getMatch(0);
            }
            String fpName = br.getRegex("<meta content=\"([^\"]+)\" property=\"og:title\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("alt=\"([^\"]+)\" id=\"cover_art\"").getMatch(0);
            }
            if (fpName == null) {
                fpName = br.getRegex("class=\"cover\" alt=\"([^\"]+)\"").getMatch(0);
            }
            if (fpName == null) {
                fpName = "8tracks_playlist" + System.currentTimeMillis();
            }
            fpName = Encoding.htmlDecode(fpName.trim());
            fpName = encodeUnicode(fpName);
            /* tracks in mix */
            String tracksInMix = br.getRegex("<span[^>]+class=\"gray\">\\((\\d+) tracks?\\)</span>").getMatch(0);
            if (tracksInMix == null) {
                tracksInMix = this.br.getRegex("id=\"tracks_count\">(\\d+)").getMatch(0);
            }
            if (tracksInMix == null || mixid == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String trackid = null;
            String filename = null;
            String dllink = null;
            String playToken = null;
            if (TEST_MODE && TEST_MODE_TOKEN != null) {
                playToken = "608739506";
            } else {
                // /* Get token */
                clipData = br.getPage(MAINPAGE + "sets/new?format=jsonh");
                playToken = PluginJSonUtils.getJsonValue(this.br, "play_token");
                if (playToken == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                /* Play one track and get the token */
                clipData = br.getPage(MAINPAGE + "sets/" + playToken + "/play?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&format=jsonh");
                trackid = updateTrackID();
                dllink = getDllink();
                filename = getFilename();
                // clipData = br.getPage(MAINPAGE + "sets/" + playToken +
                // "/skip?player=sm&include=track%5Bfaved%2Bannotation%2Bartist_details%5D&mix_id=" + mixid + "&track_id=" + trackid +
                // "&format=jsonh");
            }
            /*
             * For GEO-blocked playlists, users cannot listen to them from 8tracks but they can watch YouTube videos --> This way WE can get
             * the track-names :)
             */
            this.br.getPage("/mixes/" + mixid + "/tracks_for_international.jsonh");
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) entries.get("tracks");
            final int listmax = ressourcelist.size() - 1;
            final int tracks_in_mix = Integer.parseInt(tracksInMix);
            final DecimalFormat df = (tracks_in_mix < 100 ? new DecimalFormat("00") : new DecimalFormat("000"));
            for (int i = 1; i <= tracks_in_mix; i++) {
                final String formatted_tracknumber = df.format(i);
                String temp_name = null;
                if (i - 1 <= listmax) {
                    entries = (LinkedHashMap<String, Object>) ressourcelist.get(i - 1);
                    final String artist = (String) entries.get("performer");
                    final String title = (String) entries.get("name");
                    if (artist != null && !artist.equals("") && title != null && !title.equals("")) {
                        temp_name = formatted_tracknumber + "." + artist + " - " + title;
                    }
                }
                if (temp_name == null) {
                    temp_name = fpName + "_track" + formatted_tracknumber;
                }
                final DownloadLink dl = createDownloadlink("https://8tracksdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                dl.setName(temp_name.concat(TEMP_EXT));
                dl.setProperty("playtoken", playToken);
                dl.setProperty("mixid", mixid);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("mixname", fpName);
                dl.setProperty("tempname", temp_name);
                dl.setProperty("tempname_with_ext", temp_name.concat(TEMP_EXT));
                dl.setProperty("tracknumber", i);
                dl.setProperty("lasttracknumber", tracks_in_mix);
                if (i == 1 && trackid != null && dllink != null && filename != null) {
                    dl.setProperty("trackid", trackid);
                    dl.setProperty("savedlink", dllink);
                    dl.setProperty("final_filename", filename);
                }
                dl.setContentUrl(parameter);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String updateTrackID() throws PluginException {
        final String currenttrackid = PluginJSonUtils.getJsonValue(this.br, "id");
        return currenttrackid;
    }

    private String getDllink() {
        String dllink = null;
        final String soundcloud_trackID = new Regex(clipData, "\"uid\":\"sc\\-(\\d+)\"").getMatch(0);
        if (soundcloud_trackID != null) {
            dllink = "https://api.soundcloud.com/tracks/" + soundcloud_trackID + "/stream?client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID_8TRACKS;
        } else {
            dllink = PluginJSonUtils.getJsonValue(this.br, "track_file_stream_url");
        }
        return dllink;
    }

    private String getFilename() {
        String filename = null;
        final Regex name_and_artist = new Regex(clipData, "\"name\":\"([^<>\"]*?)\",\"performer\":\"([^<>\"]*?)\"");
        String album = PluginJSonUtils.getJsonValue(this.br, "release_name");
        String title = name_and_artist.getMatch(0);
        String artist = name_and_artist.getMatch(1);
        if (album == null || title == null) {
            return null;
        }
        if (album.contains(":")) {
            album = album.substring(0, album.indexOf(":"));
        }
        if (album.equals(title) || isEmpty(album)) {
            album = null;
        }
        title = encodeUnicode(Encoding.htmlDecode(title.trim()));
        artist = encodeUnicode(Encoding.htmlDecode(artist.trim()));
        if (album != null) {
            album = encodeUnicode(Encoding.htmlDecode(album.trim()));
            filename = artist + " - " + album + " - " + title;
        } else {
            filename = artist + " - " + title;
        }
        return filename;
    }

    private boolean isEmpty(final String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}