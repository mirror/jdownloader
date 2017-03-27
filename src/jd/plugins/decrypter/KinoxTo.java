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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "kinox.to" }, urls = { "https?://(?:www\\.)?kinox\\.(?:to|tv|nu|me|pe)/Stream/[A-Za-z0-9\\-_]+\\.html" })
public class KinoxTo extends antiDDoSForDecrypt {

    public KinoxTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String addr_id;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getRedirectLocation() != null && br.getRedirectLocation().contains("Error-404")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String url_name = new Regex(parameter, "kinox\\.to/Stream/([A-Za-z0-9\\-_]+)\\.html").getMatch(0);
        addr_id = this.br.getRegex("Addr=([^<>\"\\&]*?)&").getMatch(0);
        final String series_id = this.br.getRegex("SeriesID=(\\d+)").getMatch(0);
        String fpName = br.getRegex("<h1><span style=\"display: inline-block\">([^<>\"]*?)</span>").getMatch(0);
        fpName = Encoding.htmlDecode(fpName.trim());

        if (fpName == null) {
            fpName = url_name;
        }
        if (addr_id == null) {
            addr_id = url_name;
        }
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        if (this.br.containsHTML("id=\"SeasonSelection\"")) {
            if (series_id == null) {
                return null;
            }
            /* Crawl all Seasons | Episodes | Mirrors of a Series */
            final String[][] season_info_all = br2.getRegex("value=\"\\d+\" rel=\"([0-9,]+)\"[^>]*?>Staffel (\\d+)</option>").getMatches();
            for (final String[] season : season_info_all) {
                final String season_number = season[1];
                final String[] season_episodes = season[0].split(",");
                for (final String episode : season_episodes) {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user");
                        return decryptedLinks;
                    }
                    /* Crawl Season --> Find episodes */
                    br2 = br.cloneBrowser();
                    br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    getPage(br2, "/aGET/MirrorByEpisode/?Addr=" + addr_id + "&SeriesID=" + series_id + "&Season=" + season_number + "&Episode=" + episode);
                    /* Crawl Episode --> Find mirrors */
                    if (br2.getRegex("(<li id=\"Hoster_\\d+\".*?</div></li>)").getColumn(0).length > 0) {
                        decryptMirrors(decryptedLinks, br2, season_number, episode);
                    }
                }
            }
        } else {
            /* Crawl all Mirrors of a movie */
            decryptMirrors(decryptedLinks, br2, null, null);
        }

        return decryptedLinks;
    }

    private void decryptMirrors(List<DownloadLink> decryptedLinks, Browser br2, final String season_number, final String episode) throws Exception {
        final String[] mirrors = br2.getRegex("(<li id=\"Hoster_\\d+\".*?</div></li>)").getColumn(0);
        if (mirrors == null || mirrors.length == 0) {
            throw new DecrypterException("Decrypter broken");
        }
        final DecimalFormat df = new DecimalFormat("00");
        final FilePackage fp = FilePackage.getInstance();
        String fpname = addr_id;
        if (season_number != null && episode != null) {
            final int season_int = Integer.parseInt(season_number);
            final String season_formatted = "S" + df.format(season_int);
            fpname += " " + season_formatted;
        }
        fp.setName(fpname);
        for (final String mirror : mirrors) {
            /* Crawl Mirrors --> Find directlinks */
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return;
            }
            final String hoster_id = new Regex(mirror, "Hoster_(\\d+)").getMatch(0);
            String mirror_id = new Regex(mirror, "Mirror=(\\d+)").getMatch(0);
            if (mirror_id == null) {
                if (new Regex(mirror, "><b>Mirror</b>:[^<>]+1/1<br\\s*/\\s*>").matches()) {
                    /* Only 1 mirror available */
                    mirror_id = "1";
                } else {
                    // regex pattern needs updating..
                    System.out.println("errrrrrrrrrrror");
                }
            }
            if (hoster_id == null || mirror_id == null) {
                return;
            }
            String geturl = "/aGET/Mirror/" + addr_id + "&Hoster=" + hoster_id + "&Mirror=" + mirror_id;
            if (season_number != null && episode != null) {
                geturl += "&Season=" + season_number + "&Episode=" + episode;
            }
            final Browser br3 = br.cloneBrowser();
            br3.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage(br3, geturl);
            String finallink = PluginJSonUtils.getJson(br3, "Stream");
            if (finallink == null) {
                return;
            }
            finallink = new Regex(finallink, "(?:href|src)\\s*=\\s*('|\"|)(.*?)\\1").getMatch(1);
            final DownloadLink dl = createDownloadlink(Request.getLocation(finallink, br.getRequest()));
            fp.add(dl);
            decryptedLinks.add(dl);
            distribute(dl);
        }
    }
}
