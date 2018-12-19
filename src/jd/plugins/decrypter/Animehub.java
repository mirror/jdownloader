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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40182 $", interfaceVersion = 2, names = { "animehub.ac" }, urls = { "https?://(www\\.)?animehub\\.ac/(watch|detail)/.+" })
public class Animehub extends PluginForDecrypt {
    public Animehub(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        String page = br.getPage(parameter);
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
            String[][] sources = br.getRegex("<option value=\"/watch/[^\"]+ep=([0-9]+)&s=([^\"]+)\" selected>[^<]+</option>").getMatches();
            if (sources != null && sources.length > 0) {
                for (String[] source : sources) {
                    final PostRequest post = new PostRequest(br.getURL("/ajax/anime/load_episodes_v2?s=" + source[1]));
                    post.addVariable("episode_id", source[0]);
                    post.getHeaders().put("Origin", "https://animehub.ac");
                    post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                    String postResult = br.getPage(post);
                    String playURL = new Regex(postResult, "[\"']value[\"']:[\"']([^\"']+)[\"']").getMatch(0);
                    if (playURL != null) {
                        playURL = Encoding.htmlOnlyDecode(playURL).replaceAll("\\\\/", "/");
                        Browser br2 = br.cloneBrowser();
                        String playHTML = br2.getPage(playURL);
                        String[][] videoURLs = br2.getRegex("[\"']file[\"']:[\"']([^\"']+)[\"']").getMatches();
                        if (videoURLs != null && videoURLs.length > 0) {
                            for (String[] videoURL : videoURLs) {
                                videoURL[0] = Encoding.htmlOnlyDecode(videoURL[0]).replaceAll("\\\\/", "/");
                                decryptedLinks.add(createDownloadlink(videoURL[0]));
                            }
                        }
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