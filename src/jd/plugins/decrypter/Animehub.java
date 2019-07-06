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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 40182 $", interfaceVersion = 2, names = { "animehub.ac" }, urls = { "https?://(www\\.)?animehub\\.ac/(watch|detail)/.+" })
public class Animehub extends antiDDoSForDecrypt {
    public Animehub(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String page = br.toString();
        String fpName = br.getRegex("property=\"og:title\" content=\"Watch ([^\"]+) Online Free").getMatch(0);
        String itemName = new Regex(parameter, "/(?:tvshows|movies|episodes)/([^/]+)").getMatch(0);
        // Handle TV show overview pages
        if (StringUtils.containsIgnoreCase(parameter, "/detail/")) {
            String[][] links = br.getRegex("<div class=\"sli-name\">[ \t\r\n]+<a href=\"([^\"]+)\" title=\"Watch[^\"]+\">").getMatches();
            if (links != null && links.length > 0) {
                for (String[] link : links) {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(link[0])));
                }
            }
        } else if (StringUtils.containsIgnoreCase(parameter, "/watch/")) {
            String[][] sources = br.getRegex("<option value=\"/watch/[^\"]+ep=([0-9]+)&s=([^\"]+)\"").getMatches();
            if (sources != null && sources.length > 0) {
                for (String[] source : sources) {
                    Browser br2 = br.cloneBrowser();
                    String postURL = br2.getURL("/ajax/anime/load_episodes_v2?s=" + source[1]).toString();
                    PostRequest post = new PostRequest(postURL);
                    post.addVariable("episode_id", source[0]);
                    post.getHeaders().put("Origin", "https://animehub.ac");
                    post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                    br.setRequest(post);
                    postPage(br2, postURL, "episode_id=" + source[0]);
                    String postResult = br2.toString();
                    final String[] playURLs = HTMLParser.getHttpLinks(postResult, null);
                    if (playURLs.length > 0) {
                        String playURL = Encoding.htmlDecode(playURLs[0]);
                        getPage(br2, playURL);
                        String embedPage = br2.toString();
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(playURL)));
                    }
                }
            }
        }
        //
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}