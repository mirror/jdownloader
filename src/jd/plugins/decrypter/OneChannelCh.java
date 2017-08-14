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
import java.util.LinkedHashSet;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 *
 * note: primewire.ag using cloudflare. -raztoki20150225
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1channel.ch" }, urls = { "https?://(?:www\\.)?(?:vodly\\.to|primewire\\.ag|primewire\\.unblocked\\.cc)/(?:watch\\-\\d+([A-Za-z0-9\\-_]+)?|tv\\-\\d+[A-Za-z0-9\\-_]+/season\\-\\d+\\-episode\\-\\d+)|http://(?:www\\.)?letmewatchthis\\.lv/movies/view/watch\\-\\d+[A-Za-z0-9\\-]+" })
public class OneChannelCh extends antiDDoSForDecrypt {

    public OneChannelCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        String parameter = param.toString().replace("vodly.to/", "primewire.ag/");
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.containsHTML("\\(TV Show\\) \\-  on 1Channel \\| LetMeWatchThis</title>")) {
            final String[] episodes = br.getRegex("class=\"tv_episode_item\"> <a href=\"(/tv[^<>\"]*?)\"").getColumn(0);
            if (episodes == null || episodes.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : episodes) {
                decryptedLinks.add(createDownloadlink("http://www.1channel.ch" + singleLink));
            }
        } else {
            if (br.getURL().equals("http://www.primewire.ag/") || br.getURL().contains("/index.php")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            } else if (br.containsHTML(">No episodes listed<")) {
                logger.info("Link offline (no downloadlinks available): " + parameter);
                return decryptedLinks;
            } else if (br.containsHTML("class=\"tv_container\"")) {
                logger.info("Linktype (series overview) is not supported: " + parameter);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<title>Watch ([^<>\"]*?) online.*?\\| [^<>\"]*?</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\">").getMatch(0);
            }
            if (parameter.contains("season-") && fpName != null) {
                final Regex seasonAndEpisode = br.getRegex("<a href=\"/tv\\-[^<>\"/]*?/[^<>\"/]*?\">([^<>\"]*?)</a>[\t\n\r ]+</strong>[\t\n\r ]+> <strong>([^<>\"]*?)</strong>");
                if (seasonAndEpisode.getMatches().length != 0) {
                    fpName = Encoding.htmlDecode(fpName.trim());
                    fpName = fpName + " - " + Encoding.htmlDecode(seasonAndEpisode.getMatch(0)) + " - " + Encoding.htmlDecode(seasonAndEpisode.getMatch(1));
                }
            }
            final String[] links = br.getRegex("(/\\w+\\.php[^\"]*[&?](?:url|link)=[^\"]*?|/(?:external|goto|gohere)\\.php[^<>\"]*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                if (br.containsHTML("\\'HD Sponsor\\'")) {
                    logger.info("Found no downloadlink in link: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.setFollowRedirects(false);
            for (final String singleLink : links) {
                if (!dupe.add(singleLink)) {
                    continue;
                }
                String finallink;
                final String b64link = new Regex(singleLink, "[&?](?:url|link)=([^<>\"&]+)").getMatch(0);
                if (b64link != null) {
                    finallink = Encoding.Base64Decode(b64link);
                    finallink = Request.getLocation(finallink, br.getRequest());
                } else {
                    final Browser br2 = br.cloneBrowser();
                    getPage(br2, singleLink);
                    finallink = br2.getRedirectLocation();
                    if (finallink == null) {
                        finallink = br2.getRegex("<frame src=\"(http[^<>\"]*?)\"").getMatch(0);
                    }
                }
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (dupe.add(finallink)) {
                    decryptedLinks.add(createDownloadlink(finallink));
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}