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
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kissanime.asia" }, urls = { "https?://(www\\d*\\.)?kissanime\\.asia/(episode/)?[^/]+/?" })
public class KissAnimeAsia extends antiDDoSForDecrypt {
    public KissAnimeAsia(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = null;
        fpName = br.getRegex("<meta[^>]+property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\"(?:Watch\\s+)([^<]+)[\\s\\-]+(?:English dubbed, English subbed online kissanime|full online English sub, English dub on kissanime)").getMatch(0);
        String linksBlock = br.getRegex("(<div\\s*class\\s*=\\s*\"tvseasons\"\\s*>[^~]+</div>)\\s*<div\\s*class\\s*=\\s*\"(?:pad|movies-list-wrap mlw-related)\"").getMatch(0);
        String[] links = StringUtils.isEmpty(linksBlock) ? null : HTMLParser.getHttpLinks(linksBlock, null);
        if (links != null) {
            for (String link : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        String serverOptionsBlock = br.getRegex("(<div\\s*class\\s*=\\s*\"form-group list-server\"[^>]*>[^~]+</div>)").getMatch(0);
        if (StringUtils.isNotEmpty(serverOptionsBlock)) {
            String[] serverOptions = new Regex(serverOptionsBlock, "value\\s*=\\s*\"([^\"]+)\"").getColumn(0);
            String filmID = br.getRegex("var\\s*filmId\\s*=\\s*\"([^\"]+)\"").getMatch(0);
            if (StringUtils.isNotEmpty(filmID)) {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                brc.getHeaders().put("Accept-Language", "en-AU,en;q=0.8");
                brc.getHeaders().put("Accept", "*/*");
                for (String serverOption : serverOptions) {
                    serverOption = Encoding.htmlDecode(serverOption);
                    String apiURL = brc.getURL("/ajax-get-link-stream/?server=" + serverOption + "&filmId=" + filmID).toString();
                    getPage(brc, apiURL);
                    String linkTarget = brc.getRegex("(?:^//)?(.*)").getMatch(0);
                    if (StringUtils.isNotEmpty(linkTarget) && !StringUtils.containsIgnoreCase(linkTarget, "No htmlCode read")) {
                        linkTarget = Encoding.htmlDecode(linkTarget);
                        if (!(linkTarget.startsWith("/") || linkTarget.startsWith("http"))) {
                            linkTarget = "https://" + linkTarget;
                        }
                        decryptedLinks.add(createDownloadlink(linkTarget));
                    }
                }
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}