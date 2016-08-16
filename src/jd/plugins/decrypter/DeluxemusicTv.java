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
import java.util.Arrays;

import jd.PluginWrapper;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deluxemusic.tv" }, urls = { "https?://(?:www\\.)?deluxemusic\\.tv/.+" }, flags = { 0 })
public class DeluxemusicTv extends PluginForDecrypt {

    public DeluxemusicTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* 2015-11-09: Was not able to find any playlist_id higher than 165 */
    private static final short  playlist_id_max          = 200;
    /* 2015-11-15: More test values - please do not touch this - pspzockerscene */
    private static final long   production_video_ids_min = 0;
    private static final long   production_video_ids_max = 100000;
    private static final String playlist_id_dummy        = "999";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<String> playlist_ids = new ArrayList<String>();
        final ArrayList<String> production_video_ids = new ArrayList<String>();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final boolean testmode = SubConfiguration.getConfig("deluxemusic.tv").getBooleanProperty(jd.plugins.hoster.DeluxemusicTv.ENABLE_TEST_FEATURES, jd.plugins.hoster.DeluxemusicTv.defaultENABLE_TEST_FEATURES);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = null;
        final String[] playlists = br.getRegex("playlist_id=(\\d+)\"").getColumn(0);
        if (playlists == null || playlists.length == 0) {
            /* No content to crawl --> Do NOT add offline URLs in this case! */
            logger.info("Could not find any downloadable content in URL: " + parameter);
            return decryptedLinks;
        }
        if (playlists != null && playlists.length > 0) {
            playlist_ids.addAll(Arrays.asList(playlists));
        }

        // /* Do test-mode stuff #1 */
        if (testmode) {
            for (short i = 0; i <= playlist_id_max; i++) {
                final String playlist_id_temp = Short.toString(i);
                if (!playlist_ids.contains(playlist_id_temp)) {
                    playlist_ids.add(playlist_id_temp);
                }
            }
        }

        for (final String playlist_id : playlist_ids) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            final String playlist_url = "http://deluxemusic.tv.cms.ipercast.net/?playlist_id=" + playlist_id + "&xmldata=true";
            this.br.getPage(playlist_url);
            if (jd.plugins.hoster.DeluxemusicTv.isOffline(this.br)) {
                logger.info("Skip offline playlist: " + playlist_id);
                continue;
            }
            fpName = getXML("info");
            if (fpName == null) {
                /* This should never happen */
                fpName = playlist_id;
            }
            fpName = Encoding.htmlDecode(fpName).trim();
            int counter = 0;
            final String[] track_array = getTrackArray(this.br);
            for (final String track_xml : track_array) {
                final String production_video_id = new Regex(track_xml, "production/(\\d+)\\.mp4").getMatch(0);
                if (production_video_id != null && !production_video_ids.contains(production_video_id)) {
                    production_video_ids.add(production_video_id);
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                final DownloadLink dl = createDownloadlink("http://deluxemusic.tvdecrypted/" + playlist_id + "_" + counter);
                dl.setAvailableStatus(jd.plugins.hoster.DeluxemusicTv.parseTrackInfo(this, dl, this.br.toString(), track_array));
                dl.setProperty("playlist_url", playlist_url);
                dl.setProperty("playlist_id", playlist_id);
                dl.setContentUrl(parameter);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                counter++;
            }

        }

        /* Do test-mode stuff #2 */
        if (testmode && production_video_ids.size() > 0) {
            fpName = "deluxemusic_testmode_" + playlist_id_dummy;
            final String production_video_id_example = production_video_ids.get(0);
            logger.info("Example production video ID: " + production_video_id_example);
            for (long l = production_video_ids_min; l <= production_video_ids_max; l++) {
                final String production_video_id_str_temp = Long.toString(l);
                if (production_video_ids.contains(production_video_id_str_temp)) {
                    /* Do not decrypt IDs which we already decrypted above via the "normal" decrypter path. */
                    continue;
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName);
                final DownloadLink dl = createDownloadlink("http://deluxemusic.tvdecrypted/" + playlist_id_dummy + "_" + l);
                dl.setProperty("playlist_url", Property.NULL);
                dl.setProperty("playlist_id", playlist_id_dummy);
                dl.setProperty("streamer", "rtmp://flash4.ipercast.net/deluxemusic.tv/_definst_/");
                dl.setProperty("location", "production/" + production_video_id_str_temp + ".mp4");
                dl.setContentUrl(parameter);
                dl.setFinalFileName("0000-00-00_deluxemusictv_playlist_" + playlist_id_dummy + "_" + production_video_id_str_temp + ".mp4");
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
            }
        }

        return decryptedLinks;
    }

    public static final String[] getTrackArray(final Browser br) {
        return br.getRegex("<track>(.*?)</track>").getColumn(0);
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    public static String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>]*?)\\]\\]></" + parameter + ">").getMatch(0);
        }
        return result;
    }

}
