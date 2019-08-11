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
import java.util.Arrays;

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

@DecrypterPlugin(revision = "$Revision: 41073 $", interfaceVersion = 2, names = { "123moviesnew.org" }, urls = { "https?://(www[0-9]*\\.)?123moviesnew\\.org/(?:movies|series|watch).+" })
public class OneTwoThreeMovies extends antiDDoSForDecrypt {
    public OneTwoThreeMovies(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta name=\"description\" content=\"(?:Watch )?(?:Full Movies )?([^\"]+)(?: - 123movies| Online Full| Online for FREE)").getMatch(0);
        String[] links = null;
        links = br.getRegex("<iframe[^>]*src=\"([^\"]+/player/stream\\.php[^\"]+)\"[^>]*>").getColumn(0);
        if (links != null && links.length > 0) {
            // Handle video embed
            final Browser brc = br.cloneBrowser();
            brc.getPage(Encoding.htmlOnlyDecode(links[0]));
            links = brc.getRegex("<iframe[^>]*src=(?:\"|\')[^\"\']+url=([^\"\'&]+)").getColumn(0);
            if (links == null || links.length == 0) {
                links = brc.getRegex("Player\\(\"([^\"]+)\",0\\)").getColumn(0);
            }
            String postURL = brc.getURL("/player/streamv1.php?url=" + links[0]).toString();
            postPage(brc, postURL, "url=" + links[0] + "&server=0");
            String[][] servers = brc.getRegex("<span class=\"btnt btnt-primary[^>]*data-id=\"([^\"]+)\"[^>]*data-server=\"([^\"]+)\"[^>]*>").getMatches();
            ArrayList<String> tempLinks = new ArrayList<String>();
            links = new String[] {};
            for (String[] server : servers) {
                postURL = brc.getURL("/playerv1/result.php").toString();
                postPage(brc, postURL, "id=" + server[0] + "&server=" + server[1]);
                tempLinks.addAll(Arrays.asList(brc.getRegex("<iframe[^>]*src=(?:\"|\')([^\"\']+)(?:\"|\')[^>]*>").getColumn(0)));
            }
            links = tempLinks.toArray(new String[tempLinks.size()]);
        } else {
            // Handle episodes, if that fails look for seasons
            links = br.getRegex("<ul[^>]*id=\"episode-list\"[^>]*>(.*)<div[^>]*id=\"vid-modal\"[^>]*>").getColumn(0);
            if (links != null && links.length > 0) {
                links = new Regex(links[0], "href=\"([^\"]+)\"").getColumn(0);
                links = HTMLParser.getHttpLinks(StringUtils.join(links, " "), null);
            } else {
                links = br.getRegex("<a class=\"btn btn-primary\" href=\"([^\"]+)\"").getColumn(0);
            }
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(link)));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}