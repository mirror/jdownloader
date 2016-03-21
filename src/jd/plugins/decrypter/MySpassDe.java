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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "myspass.de" }, urls = { "http://(?:www\\.)?myspass\\.de/myspass/shows/(?:tv|web)shows/.+" }, flags = { 0 })
public class MySpassDe extends PluginForDecrypt {

    public MySpassDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_hoster = "^https?://(?:www\\.)?myspass\\.de/(?:myspass/)?shows/(?:tv|web)shows/([a-z0-9\\-_]+/[A-Za-z0-9\\-_]+/|.+video\\.php\\?id=)\\d+/?$";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        boolean needs_series_filename = true;
        final String parameter = param.toString();
        if (parameter.matches(type_hoster)) {
            final DownloadLink dl = createDownloadlink(parameter.replace("myspass.de", "myspassdecrypted.de/"));
            dl.setContentUrl(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String show = this.br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?) Videos\"").getMatch(0);
        if (show == null) {
            /* Fallback to url */
            show = new Regex(parameter, ".+/myspass/shows/[^/]+/([^/]+)/").getMatch(0);
            show = show.replace("-", " ");
        }
        show = Encoding.htmlDecode(show).trim();

        short seasoncounter = 1;
        final DecimalFormat df = new DecimalFormat("00");
        final String[] html_list = this.br.getRegex("(<li[^>]*?data\\-target=\"#episodesBySeason_full_episode_\\d+\".*?</li>)").getColumn(0);
        for (final String html : html_list) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            /*
             * Normally we could use seasoncounter here but site is buggy - sometimes they either dont have all seasons or they just start
             * with- or use random numbers e.g. here the "first season" from 2011 has the season number 9:
             * http://www.myspass.de/myspass/shows/tvshows/tv-total-wok-wm/
             * 
             * --> Let's use their buggy numbers but set correct filenames below :)
             */
            String seasonnumber = new Regex(html, "season=(\\d+)").getMatch(0);
            final String format = new Regex(html, "format=([^<>\"/\\&]*?)&").getMatch(0);
            if (format == null) {
                return null;
            }
            if (seasonnumber == null) {
                seasonnumber = Short.toString(seasoncounter);
            }
            this.br.getPage("http://www.myspass.de/myspass/includes/php/ajax.php?v=2&ajax=true&action=getEpisodeListFromSeason&format=" + format + "&season=" + seasonnumber + "&category=full_episode&id=&sortBy=episode_asc");

            /* Sometimes we don't have a season (Season 1, 2 and so on) but a YEAR instead. */
            String season_name_name = new Regex(html, "\">([^<>\"]*?)</a></span>").getMatch(0);
            if (season_name_name != null) {
                season_name_name = Encoding.htmlDecode(season_name_name).trim();
            }
            if (season_name_name == null || season_name_name.matches("Staffel \\d+")) {
                /* Nothing found or we know it is a season --> Use season-names */
                season_name_name = "S" + df.format(seasoncounter);
            } else {
                needs_series_filename = false;
            }

            final FilePackage fp = FilePackage.getInstance();
            fp.setName(show + " " + season_name_name);

            final String[] links = br.getRegex("class=\"season_episode_delim\" onclick=\"location\\.href=\\'(/myspass/shows/[^/]+/[^/]+/[^/]+/\\d+/)\\'\"").getColumn(0);
            if (html_list == null || html_list.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                final String url_content = "http://myspass.de" + singleLink;
                final DownloadLink dl = createDownloadlink("http://myspassdecrypted.de" + singleLink);
                dl.setProperty("needs_series_filename", needs_series_filename);
                dl.setContentUrl(url_content);
                dl._setFilePackage(fp);
                distribute(dl);
                decryptedLinks.add(dl);
            }
            seasoncounter++;
        }

        return decryptedLinks;
    }

}
