//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "8tracks.com" }, urls = { "http://(www\\.)?8tracks\\.com/[\\w\\-]+/[\\w\\-]+" }, flags = { 0 })
public class EightTracksCom extends PluginForDecrypt {

    private static final String MAINPAGE = "http://8tracks.com/";
    private boolean             ATEND    = false;

    public EightTracksCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private String createFilename() {
        String album = br.getRegex("<release\\-name>(.*?)</release\\-name>").getMatch(0);
        final String title = br.getRegex("name>(.*?)</name>").getMatch(0);
        if (album == null || title == null) { return null; }
        if (album.contains(":")) {
            album = album.substring(0, album.indexOf(":"));
        }
        return Encoding.htmlDecode(album + "__" + title).trim();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>(0);
        final String parameter = param.toString();
        setBrowserExclusive();

        br.setReadTimeout(90 * 1000);
        /* nachfolgender UA sorgt für bessere Audioqualität */
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (webOS/2.1.0; U; en-US) AppleWebKit/532.2 (KHTML, like Gecko) Version/1.0 Safari/532.2 Pre/1.2 ");
        br.getPage(parameter);

        String mixId = br.getRegex("mix_id=(\\d+)\"").getMatch(0);
        if (mixId == null) {
            mixId = br.getRegex("/mixes/(\\d+)/").getMatch(0);
        }

        String name = br.getRegex("<title>(.*?)\\s\\|").getMatch(0);
        if (name == null) {
            name = br.getRegex("content=\"(.*?)\" property=\"og:title\"").getMatch(0);
            if (name == null) {
                name = "8tracks_playlist" + System.currentTimeMillis();
            }
        }

        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (br.getRegex("name=\"csrf-token\" content=\"(.*?)\"").matches()) {
            br.getHeaders().put("X-CSRF-Token", br.getRegex("name=\"csrf-token\" content=\"(.*?)\"").getMatch(0));
        }
        br.getPage(MAINPAGE + "sets/new.xml");

        final String playToken = br.getRegex("<play\\-token>(\\d+)</play\\-token>").getMatch(0);
        if (playToken == null || mixId == null) { return null; }

        long count = 8;
        progress.setRange(count);
        /* Start playlist */
        br.getPage(MAINPAGE + "sets/" + playToken + "/play.xml?mix_id=" + mixId);
        String dllink = br.getRegex("<url>(.*?)</url>").getMatch(0);
        String filename = createFilename();
        String ext = "", sameLink = "";

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(name.trim());
        while (!ATEND) {
            /* ATEND=true --> end of playlist */
            ATEND = Boolean.parseBoolean(br.getRegex("<at\\-end>(True|False)</at\\-end>").getMatch(0));
            if (dllink != null && filename != null) {
                sameLink = dllink;
                ext = dllink.substring(dllink.lastIndexOf(".") + 1);
                ext = ext.equals("m4a") || ext.length() > 5 ? "mp4" : ext;
                final DownloadLink dl = createDownloadlink(dllink);
                dl.setFinalFileName(filename + "." + ext);
                fp.add(dl);
                try {
                    distribute(dl);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dl);
                progress.increase(1);
                /* Anzahl der Titel unbestimmt. Siehe ATEND! */
                progress.setRange(count++);
            }
            br.getPage(MAINPAGE + "sets/" + playToken + "/next.xml?mix_id=" + mixId);
            dllink = br.getRegex("<url>(.*?)</url>").getMatch(0);
            filename = createFilename();

            if (!ATEND && dllink == null || !ATEND && dllink != null && dllink.equals(sameLink)) {
                ATEND = true;
            }
        }

        progress.doFinalize();
        if (decryptedLinks.size() == 0) {
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

}
