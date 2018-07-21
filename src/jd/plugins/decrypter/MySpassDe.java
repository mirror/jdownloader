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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "myspass.de" }, urls = { "https?://(?:www\\.)?myspass\\.de/(?:myspass/)?shows/(?:tv|web)shows/.+" })
public class MySpassDe extends PluginForDecrypt {
    public MySpassDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_hoster = "^https?://(?:www\\.)?myspass\\.de/(?:myspass/)?shows/(?:tv|web)shows/([a-z0-9\\-_]+/[A-Za-z0-9\\-_]+/|.+video\\.php\\?id=)\\d+/?$";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final boolean fastlinkcheck = JDUtilities.getPluginForHost(this.getHost()).getPluginConfig().getBooleanProperty("FAST_LINKCHECK", true);
        if (parameter.matches(type_hoster)) {
            final DownloadLink dl = createDownloadlink(parameter.replace("myspass.de", "myspassdecrypted.de/"));
            dl.setContentUrl(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 - SEITE NICHT GEFUNDEN")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String show = this.br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?) Videos\"").getMatch(0);
        if (show == null) {
            /* Fallback to url */
            show = new Regex(parameter, ".+/shows/[^/]+/([^/]+)/?").getMatch(0);
            show = show.replace("-", " ");
        }
        show = Encoding.htmlDecode(show).trim();
        String videoid = br.getRegex("data-videoid=\"(\\d+)\" ").getMatch(0);
        short seasonnumber_parsed;
        String seasonnumber_formatted;
        boolean yearInsteadOfSeasonNumber;
        boolean needs_series_filename;
        final DecimalFormat df = new DecimalFormat("00");
        // class="float-left seasonTab baxx-tabbes-tab full_episode_seasonTab"
        final String[] html_list_season = this.br.getRegex("(<li[^>]*?class=\"float\\-left seasonTab [^\"]*?full_episode_seasonTab[^\"]*?\".*?</li>)").getColumn(0);
        if (html_list_season == null || html_list_season.length == 0 && videoid != null) {
            final DownloadLink dl = createDownloadlink(parameter.replace("myspass.de", "myspassdecrypted.de/") + videoid + "/");
            dl.setContentUrl(parameter);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        if (html_list_season == null || html_list_season.length == 0) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (final String html_season : html_list_season) {
            /* Reset variables which we re-use. */
            yearInsteadOfSeasonNumber = false;
            needs_series_filename = true;
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
            /* Seasonnumbers for http requests --> They have nothing todo with the real year- or seasonnumber! */
            /* 2016-11-23 */
            String seasonnumber_intern = new Regex(html_season, "seasonId=(\\d+)").getMatch(0);
            /* 2016-11-23 */
            final String format_intern = new Regex(html_season, "formatId=(\\d+)").getMatch(0);
            /* Real seasonnumber or year instead. */
            String seasonnumber = new Regex(html_season, "data\\-target=\"#episodes_season_\\d+_category_full_episode\">([^<>\"]+)</li>").getMatch(0);
            if (format_intern == null || seasonnumber_intern == null || seasonnumber == null) {
                /* E.g. Skip "Highlights"-tab. */
                continue;
            }
            seasonnumber = seasonnumber.trim();
            if (seasonnumber.matches("Staffel\\s*?\\d+")) {
                seasonnumber_parsed = Short.parseShort(new Regex(seasonnumber, "(\\d+)$").getMatch(0));
            } else {
                /* We don't have a season-number but a YEAR instead. */
                seasonnumber_parsed = Short.parseShort(new Regex(seasonnumber, "(\\d+)").getMatch(0));
                yearInsteadOfSeasonNumber = true;
                needs_series_filename = false;
            }
            seasonnumber_formatted = df.format(seasonnumber_parsed);
            this.br.getPage("//www." + this.getHost() + "/frontend/php/ajax.php?ajax=true&query=getEpisodeListFromSeason&formatId=" + format_intern + "&seasonId=" + seasonnumber_intern + "&category=full_episode&sortBy=episode_desc");
            final String fpName;
            if (yearInsteadOfSeasonNumber) {
                fpName = show + " " + seasonnumber;
            } else {
                fpName = show + " S" + seasonnumber_formatted;
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            final String[] html_episode_list = this.br.getRegex("<div[^>]*?class=\"myspassTeaser _seasonId seasonlistItem full_episode\"[^>]*?>(.*?)class=\"full_episode msvideo\"").getColumn(0);
            if (html_episode_list == null || html_episode_list.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String html_episode : html_episode_list) {
                final String singleLink = new Regex(html_episode, "<a href=\"(/(?:myspass/)?shows/[^/]+/[^/]+/[^/]+/\\d+/)\"").getMatch(0);
                final String episodenumber = new Regex(html_episode, "Folge\\s*?(\\d+)").getMatch(0);
                if (singleLink == null) {
                    return null;
                }
                final String url_content = "http://myspass.de" + singleLink;
                final String fid = new Regex(singleLink, "(\\d+)/$").getMatch(0);
                final DownloadLink dl = createDownloadlink("http://myspassdecrypted.de" + singleLink);
                dl.setProperty("needs_series_filename", needs_series_filename);
                dl.setContentUrl(url_content);
                dl.setLinkID(fid);
                final String filename_temp;
                if (episodenumber != null) {
                    if (yearInsteadOfSeasonNumber) {
                        filename_temp = fid + "_" + show + "_" + seasonnumber + "E" + episodenumber;
                    } else {
                        filename_temp = fid + "_" + show + "_S" + seasonnumber_formatted + "E" + episodenumber;
                    }
                } else {
                    filename_temp = fid;
                }
                dl.setName(filename_temp);
                dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
                if (fastlinkcheck) {
                    dl.setAvailable(true);
                }
                dl._setFilePackage(fp);
                distribute(dl);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }
}
