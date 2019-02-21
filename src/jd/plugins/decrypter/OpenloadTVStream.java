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
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 40396 $", interfaceVersion = 2, names = { "openloadtvstream.me" }, urls = { "https?://(www\\.)?openloadtvstream\\.me/(tvshows|movies|episodes)/.+" })
public class OpenloadTVStream extends antiDDoSForDecrypt {
    public OpenloadTVStream(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String page = br.toString();
        String fpName = br.getRegex("<title>([^<]+) \\| Just Watch").getMatch(0);
        String itemName = new Regex(parameter, "/(?:tvshows|movies|episodes)/([^/]+)").getMatch(0);
        // Handle TV show overview pages
        if (StringUtils.containsIgnoreCase(parameter, "/tvshows/")) {
            String[][] links = br.getRegex("<a href=\"([^\"]+/(?:tvshows|films|episodes)/" + Regex.escape(itemName) + "[^\"]+)\"").getMatches();
            if (links != null && links.length > 0) {
                for (String[] link : links) {
                    if (!new Regex(link[0], Regex.escape(parameter) + "/?").matches()) {
                        decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(link[0])));
                    }
                }
            }
        } else {
            String[][] sources = br.getRegex("data-type=[\"']([a-zA-Z0-9]+)[\"'] data-post=[\"']([0-9]+)[\"'] data-nume=[\"']([0-9]+)[\"']").getMatches();
            for (String[] source : sources) {
                final PostRequest post = new PostRequest(br.getURL("/wp-admin/admin-ajax.php"));
                post.addVariable("action", "doo_player_ajax");
                post.addVariable("post", source[1]);
                post.addVariable("nume", source[2]);
                post.addVariable("type", source[0]);
                post.getHeaders().put("Origin", "https://openloadtvstream.me");
                post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
                String postResult = br.getPage(post);
                String videoURL = new Regex(postResult, "src=[\"']([^\"']+)[\"']").getMatch(0);
                if (videoURL != null) {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(videoURL)));
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