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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "noisetrade.com" }, urls = { "http://(?:www\\.|books\\.)?noisetrade\\.com/[a-z0-9\\-]+/[a-z0-9\\-]+" }, flags = { 0 })
public class NoiseTradeCom extends PluginForDecrypt {

    public NoiseTradeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String parameter = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* Load host plugin */
        JDUtilities.getPluginForHost("noisetrade.com");
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null || br.getURL().contains("noisetrade.com/info/error") || br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        final String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        String album_name = br.getRegex("class=\"artist\">([^<>\"]*?)</h1>").getMatch(0);
        final Regex info = br.getRegex("<h2 class=\"album\"><a href=\"/([a-z0-9\\-]+)\">([^<>\"]*?)</a></h2>");
        final String username = info.getMatch(0);
        String artist = info.getMatch(1);
        if (fpName == null || artist == null || username == null || album_name == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        artist = encodeUnicode(Encoding.htmlDecode(artist)).trim();
        album_name = encodeUnicode(Encoding.htmlDecode(album_name)).trim();

        if (br.getURL().contains("books.noisetrade.com/")) {
            final DownloadLink main = getDownloadlink();
            main.setProperty("directartist", artist);
            main.setProperty("type", ".epub");
            main.setProperty("directusername", username);
            main.setProperty("directalbum", album_name);
            main.setProperty("directlink", null);
            main.setName(jd.plugins.hoster.NoiseTradeCom.getFormattedFilename(main));
            main.setAvailable(true);
            decryptedLinks.add(main);
        } else {
            if (!br.containsHTML("class=\"col_album_playlist\"")) {
                try {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                } catch (final Throwable e) {
                    /* Not available in old 0.9.581 Stable */
                }
                return decryptedLinks;
            }
            final String[] links = br.getRegex("<Track(.*?) />").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                final String title = new Regex(singleLink, "Name=\"([^<>\"]*?)\"").getMatch(0);
                String directlink = new Regex(singleLink, "MP3=\"([^<>\"]*?)\"").getMatch(0);
                if (title == null || directlink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = getDownloadlink();
                dl.setProperty("directtitle", encodeUnicode(Encoding.htmlDecode(title)));
                dl.setProperty("directartist", artist);
                dl.setProperty("type", ".mp3");
                dl.setProperty("directusername", username);
                dl.setProperty("directalbum", album_name);
                dl.setProperty("directlink", "http://s3.amazonaws.com/static.noisetrade.com/" + directlink);
                dl.setAvailable(true);
                dl.setName(jd.plugins.hoster.NoiseTradeCom.getFormattedFilename(dl));
                decryptedLinks.add(dl);
            }

            final DownloadLink main = getDownloadlink();
            main.setProperty("directartist", artist);
            main.setProperty("type", ".zip");
            main.setProperty("directusername", username);
            main.setProperty("directalbum", album_name);
            main.setProperty("directlink", null);
            main.setName(jd.plugins.hoster.NoiseTradeCom.getFormattedFilename(main));
            main.setAvailable(true);
            decryptedLinks.add(main);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(null));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    @SuppressWarnings("deprecation")
    private DownloadLink getDownloadlink() {
        final DownloadLink dl = createDownloadlink("http://noisetradedecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(1000000000));
        try {
            dl.setContentUrl(parameter);
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
            dl.setBrowserUrl(parameter);
        }
        dl.setProperty("mainlink", parameter);
        return dl;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
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

}
