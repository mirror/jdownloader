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

import java.util.ArrayList;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "8tracks.com" }, urls = { "http://(www\\.)?8tracks\\.com/[a-z0-9\\-_]+/[a-z0-9\\-_]+" }, flags = { 0 })
public class EightTracksCom extends PluginForDecrypt {

    private static final String  MAINPAGE          = "http://8tracks.com/";
    private static final String  UNSUPPORTEDLINKS  = "http://(www\\.)?8tracks\\.com/((assets_js/|explore|auth|settings|mixes|developers|users)/.+|[\\w\\-]+/homepage|sets/new)";
    private static final String  TYPE_SINGLE_TRACK = "http://(www\\.)?8tracks\\.com/tracks/\\d+";

    private String               clipData;
    private static final String  TEMP_EXT          = ".mp3";

    private static final boolean TEST_MODE         = false;
    private static final String  TEST_MODE_TOKEN   = null;

    public EightTracksCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>(0);
        final String parameter = param.toString();
        setBrowserExclusive();

        if (parameter.matches(UNSUPPORTEDLINKS)) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }

        br.setFollowRedirects(true);
        br.setReadTimeout(90 * 1000);
        /* nachfolgender UA sorgt für bessere Audioqualität */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (webOS/2.1.0; U; en-US) AppleWebKit/532.2 (KHTML, like Gecko) Version/1.0 Safari/532.2 Pre/1.2");
        br.getPage(parameter);

        final DownloadLink offline = createDownloadlink("http://8tracksdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
        offline.setName(new Regex(parameter, "8tracks\\.com/(.+)").getMatch(0));
        offline.setAvailable(false);
        offline.setProperty("offline", true);
        if (br.containsHTML(">Sorry, that page doesn\\'t exist")) {
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
            decryptedLinks.add(single_track);
        } else {
            String mixid = br.getRegex("mix_id=(\\d+)\"").getMatch(0);
            if (mixid == null) {
                mixid = br.getRegex("/mixes/(\\d+)/").getMatch(0);
            }

            String fpName = br.getRegex("<meta content=\"([^\"]+)\" property=\"og:title\"").getMatch(0);
            if (fpName == null) fpName = br.getRegex("alt=\"([^\"]+)\" id=\"cover_art\"").getMatch(0);
            if (fpName == null) fpName = br.getRegex("class=\"cover\" alt=\"([^\"]+)\"").getMatch(0);
            if (fpName == null) fpName = "8tracks_playlist" + System.currentTimeMillis();
            fpName = Encoding.htmlDecode(fpName.trim());
            fpName = encodeUnicode(fpName);

            /* tracks in mix */
            String tracksInMix = br.getRegex("<span[^>]+class=\"gray\">\\((\\d+) tracks?\\)</span>").getMatch(0);
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
                playToken = getClipData("play_token");
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

            final int tracks_in_mix = Integer.parseInt(tracksInMix);
            for (int i = 1; i <= tracks_in_mix; i++) {
                final DownloadLink dl = createDownloadlink("http://8tracksdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
                final String temp_name = fpName + "_track" + i;
                dl.setName(temp_name + TEMP_EXT);
                dl.setProperty("playtoken", playToken);
                dl.setProperty("mixid", mixid);
                dl.setProperty("mainlink", parameter);
                dl.setProperty("mixname", fpName);
                dl.setProperty("tempname", temp_name);
                dl.setProperty("tempname_with_ext", temp_name + TEMP_EXT);
                dl.setProperty("tracknumber", Integer.toString(i));
                dl.setProperty("lasttracknumber", tracks_in_mix);
                if (i == 1 && trackid != null && dllink != null && filename != null) {
                    dl.setProperty("trackid", trackid);
                    dl.setProperty("savedlink", dllink);
                    dl.setProperty("final_filename", filename);
                }
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private String getClipData(final String tag) {
        return new Regex(clipData, "\"" + tag + "\"\\s?:\\s?\"?(.*?)\"?,").getMatch(0);
    }

    private String updateTrackID() throws PluginException {
        final String currenttrackid = getClipData("id");
        return currenttrackid;
    }

    private String getDllink() {
        String dllink = null;
        final String soundcloud_trackID = new Regex(clipData, "\"uid\":\"sc\\-(\\d+)\"").getMatch(0);
        if (soundcloud_trackID != null) {
            dllink = "https://api.soundcloud.com/tracks/" + soundcloud_trackID + "/stream?client_id=" + jd.plugins.hoster.SoundcloudCom.CLIENTID;
        } else {
            dllink = getClipData("track_file_stream_url");
        }
        return dllink;
    }

    private String getFilename() {
        String filename = null;
        final Regex name_and_artist = new Regex(clipData, "\"name\":\"([^<>\"]*?)\",\"performer\":\"([^<>\"]*?)\"");
        String album = getClipData("release_name");
        String title = name_and_artist.getMatch(0);
        String artist = name_and_artist.getMatch(1);
        if (album == null || title == null) return null;
        if (album.contains(":")) album = album.substring(0, album.indexOf(":"));
        if (album.equals(title) || isEmpty(album)) album = null;
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

    private String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    private boolean isEmpty(final String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}