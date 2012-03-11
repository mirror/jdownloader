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
import java.util.HashSet;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

// "old style" , "new style", "redirect url shorting service" 
@DecrypterPlugin(revision = "$Revision: 15544 $", interfaceVersion = 2, names = { "promodj.com", "promoDJ2", "promoDJ3" }, urls = { "https?://[\\w\\-]+\\.(djkolya\\.net|pdj\\.ru|promodeejay\\.(net|ru)|promodj\\.(ru|com))/(?!top100|podsafe)((foto/\\d+/?)(\\d+\\.html(#(foto|full)\\d+)?)?|(acapellas|groups|mixes|podcasts|radioshows|realtones|remixes|samples|tracks|videos)/\\d+|((acapellas|foto|music|mixes|podcasts|radioshows|realtones|remixes|samples|tracks|video)/))", "https?://(djkolya\\.net|pdj\\.ru|promodeejay\\.(net|ru)|promodj\\.(ru|com))/(?!top100|podsafe)[\\w\\-]+/((foto/\\d+/?)(\\d+\\.html(#(foto|full)\\d+)?)?|(acapellas|groups|mixes|podcasts|radioshows|realtones|remixes|samples|tracks|videos)/\\d+|((acapellas|foto|music|mixes|podcasts|radioshows|realtones|remixes|samples|tracks|video)/))", "https?://pdj\\.cc/\\w+" }, flags = { 0, 0, 0 })
public class TESTgetsupportedlinks extends PluginForDecrypt {

    private static final String HOSTS = "(djkolya\\.net|pdj\\.(cc|ru)|promodeejay\\.(net|ru)|promodj\\.(ru|com))";

    public TESTgetsupportedlinks(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // - use this to check problem
        Pattern regex = this.getSupportedLinks();
        String fpName = null;
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("The page you were looking for doesn\\'t exist\\.")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        // find the final domain after piles of redirects, for stable compliance
        String host = new Regex(br.getURL(), "https?://([^/:]+)").getMatch(0);

        // domain shorting services first, then change parameter as it saves
        // reloading pages
        if (parameter.matches("https?://pdj\\.cc/\\w+")) {
            decryptedLinks.add(createDownloadlink(br.getURL()));
        }

        // grab all the provided download stuff here
        else if (parameter.matches(".*/(acapellas|mixes|podcasts|radioshows|realtones|remixes|samples|tracks|videos)/\\d+")) {
            // parseArticle(decryptedLinks, parameter);
        }
        // photos type crawling
        else if (parameter.contains("/foto/")) {
            // parseFoto(decryptedLinks, parameter);
        }
        // find groups data and export it.
        else if (parameter.matches(".*/groups/\\d+")) {
            HashSet<String> filter = new HashSet<String>();
            String frame = br.getRegex("(<div class=\"dj_bblock\">.*</div>[\r\n ]+</div>)").getMatch(0);
            fpName = new Regex(frame, ">([^<]+)</span>[\r\n ]+</h2>").getMatch(0);
            String[] posts = new Regex(frame, "href=\"(https?://([\\w\\-]+\\.)?" + HOSTS + "/[^\"]+)\" class=\"avatar\"").getColumn(0);
            if (posts != null && posts.length != 0) {
                for (String link : posts) {
                    if (filter.add(link) == false) continue;
                    decryptedLinks.add(createDownloadlink(link));
                }
            }

        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}