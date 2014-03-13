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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1channel.ch" }, urls = { "http://(www\\.)?(vodly\\.to|primewire\\.ag)/(watch\\-\\d+([A-Za-z0-9\\-_]+)?|tv\\-\\d+[A-Za-z0-9\\-_]+/season\\-\\d+\\-episode\\-\\d+)" }, flags = { 0 })
public class OneChannelCh extends PluginForDecrypt {

    public OneChannelCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("vodly.to/", "primewire.ag/");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("\\(TV Show\\) \\-  on 1Channel \\| LetMeWatchThis</title>")) {
            final String[] episodes = br.getRegex("class=\"tv_episode_item\"> <a href=\"(/tv[^<>\"]*?)\"").getColumn(0);
            if (episodes == null || episodes.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : episodes)
                decryptedLinks.add(createDownloadlink("http://www.1channel.ch" + singleLink));
        } else {
            if (br.getURL().equals("http://www.1channel.ch/")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            } else if (br.containsHTML(">No episodes listed<")) {
                logger.info("Link offline (no downloadlinks available): " + parameter);
                return decryptedLinks;
            } else if (br.containsHTML("class=\"tv_container\"")) {
                logger.info("Linktype (series overview) is not supported: " + parameter);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<title>Watch ([^<>\"]*?) online \\-  on 1Channel \\| [^<>\"]*?</title>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\">").getMatch(0);
            if (parameter.contains("season-") && fpName != null) {
                final Regex seasonAndEpisode = br.getRegex("<a href=\"/tv\\-[^<>\"/]*?/[^<>\"/]*?\">([^<>\"]*?)</a>[\t\n\r ]+</strong>[\t\n\r ]+> <strong>([^<>\"]*?)</strong>");
                if (seasonAndEpisode.getMatches().length != 0) {
                    fpName = Encoding.htmlDecode(fpName.trim());
                    fpName = fpName + " - " + Encoding.htmlDecode(seasonAndEpisode.getMatch(0)) + " - " + Encoding.htmlDecode(seasonAndEpisode.getMatch(1));
                }
            }
            final String[] links = br.getRegex("\"(/external\\.php[^<>\"]*?)\"").getColumn(0);
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
                br.getPage("http://www.primewire.ag" + singleLink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) finallink = br.getRegex("<frame src=\"(http[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
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