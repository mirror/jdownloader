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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "8tracks.com" }, urls = { "http://(www\\.)?8tracks\\.com/[\\w\\-]+/[\\w\\-]+" }, flags = { 0 })
public class EightTracksCom extends PluginForDecrypt {

    private static final String MAINPAGE          = "http://8tracks.com/";
    private boolean             ATEND             = false;
    private String              clipData;
    private static final String UNSAUPPORTEDLINKS = "http://(www\\.)?8tracks\\.com/((assets_js/|explore|auth|settings|mixes|developers|users)/.+|[\\w\\-]+/homepage|sets/new)";

    public EightTracksCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String createFilename() {
        String album = getClipData("release_name");
        String title = getClipData("name");
        if (album == null || title == null) return null;
        if (album.contains(":")) album = album.substring(0, album.indexOf(":"));
        if (isEmpty(album)) album = title;
        album = album.trim().replaceAll("\\[|\\]|\\(|\\)|\\.\\.\\.", "_");
        title = title.trim().replaceAll("\\[|\\]|\\(|\\)|\\.\\.\\.", "_");
        if (album.equals(title)) return Encoding.htmlDecode(title);
        return Encoding.htmlDecode(album + "__" + title);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>(0);
        final String parameter = param.toString();
        setBrowserExclusive();

        if (parameter.matches(UNSAUPPORTEDLINKS)) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }

        br.setFollowRedirects(true);
        br.setReadTimeout(90 * 1000);
        /* nachfolgender UA sorgt für bessere Audioqualität */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (webOS/2.1.0; U; en-US) AppleWebKit/532.2 (KHTML, like Gecko) Version/1.0 Safari/532.2 Pre/1.2");
        br.getPage(parameter);

        if (br.containsHTML(">Sorry, that page doesn\\'t exist")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.getURL().contains("/explore/")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML(">The mix you're looking for is currently in private mode")) {
            logger.info("Link offline (this is a private link): " + parameter);
            return decryptedLinks;
        }

        String mixId = br.getRegex("mix_id=(\\d+)\"").getMatch(0);
        if (mixId == null) {
            mixId = br.getRegex("/mixes/(\\d+)/").getMatch(0);
        }

        String fpName = br.getRegex("<meta content=\"([^\"]+)\" property=\"og:title\"").getMatch(0);
        if (fpName == null) fpName = br.getRegex("alt=\"([^\"]+)\" id=\"cover_art\"").getMatch(0);
        if (fpName == null) fpName = br.getRegex("class=\"cover\" alt=\"([^\"]+)\"").getMatch(0);
        if (fpName == null) fpName = "8tracks_playlist" + System.currentTimeMillis();
        fpName = Encoding.htmlDecode(fpName.trim());

        /* tracks in mix */
        String tracksInMix = br.getRegex("<span[^>]+class=\"gray\">\\((\\d+) tracks?\\)</span>").getMatch(0);
        boolean bigPlayList = tracksInMix != null && Integer.parseInt(tracksInMix) > 100;

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (br.getRegex("name=\"csrf-token\" content=\"(.*?)\"").matches()) {
            br.getHeaders().put("X-CSRF-Token", br.getRegex("name=\"csrf-token\" content=\"(.*?)\"").getMatch(0));
        }
        // 20120212: die xml Version erfordert jetzt einen API Key.
        // Wechsel zur json Variante.
        clipData = br.getPage(MAINPAGE + "sets/new?format=jsonh");

        final String playToken = getClipData("play_token");
        if (playToken == null || mixId == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        long count = 8;
        progress.setRange(count);
        /* Start playlist */
        br.setFollowRedirects(false);
        clipData = br.getPage(MAINPAGE + "sets/" + playToken + "/play?mix_id=" + mixId + "&format=jsonh");
        String dllink = getClipData("track_file_stream_url");
        String filename = createFilename();
        String ext = "", sameLink = "";

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);

        /* limit to 100 API calls per minute */
        int call = 1;
        long a = 0, start = 0;

        while (!ATEND) {
            start = System.currentTimeMillis();
            /* ATEND=true --> end of playlist */
            ATEND = Boolean.parseBoolean(getClipData("at_end"));
            if (dllink != null && filename != null) {
                sameLink = dllink;
                ext = dllink.substring(dllink.lastIndexOf(".") + 1);
                ext = ext.equals("m4a") || ext.length() > 5 ? "m4a" : ext;
                if (!dllink.startsWith("http://([0-9a-z]+\\.)?8tracks")) {
                    dllink = "directhttp://" + dllink;
                }
                final DownloadLink dl = createDownloadlink(dllink);
                dl.setFinalFileName(filename + "." + ext);
                if (bigPlayList) dl.setAvailable(true);
                fp.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dl);
                try {
                    if (this.isAbort()) return decryptedLinks;
                } catch (Throwable e) {
                    /* does not exist in 09581 */
                }
                progress.increase(1);
                /* Anzahl der Titel unbestimmt. Siehe ATEND! */
                progress.setRange(count++);
            }
            clipData = br.getPage(MAINPAGE + "sets/" + playToken + "/next?mix_id=" + mixId + "&format=jsonh");

            if (clipData.contains("\"notices\":\"Sorry, but track skips are limited by our license.\"")) {
                // you can not do anymore than 3 requests in succession without the following happening
                System.out.print("BBBBBBBBBBBBBBBBBAD");
            }
            dllink = getClipData("track_file_stream_url");
            filename = createFilename();

            if ((!ATEND && dllink == null) || (!ATEND && dllink != null && dllink.equals(sameLink))) {
                ATEND = true;
            }
            a += (System.currentTimeMillis() - start);
            call++;
            if (call > 100) {
                if (a < 60 * 1000l) {
                    sleep((60 * 1000l) - a, param);
                    call = 1;
                    a = 0;
                }
            }
        }

        progress.doFinalize();
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private String getClipData(final String tag) {
        return new Regex(clipData, "\"" + tag + "\"\\s?:\\s?\"?(.*?)\"?,").getMatch(0);
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}