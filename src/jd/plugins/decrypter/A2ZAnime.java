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
import java.util.Collections;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "a2zanime.com" }, urls = { "https?://(www\\.)?a2zanime\\.com/(anime|watch)/.+" })
public class A2ZAnime extends antiDDoSForDecrypt {
    public A2ZAnime(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta[^>]+property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\"([^\"]+)\\s+at\\s+A2zAnime\\s*\"").getMatch(0);
        // Handle TV show overview pages
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<a[^>]+class\\s*=\\s*\"infovan\"[^>]+href\\s*=\\s*\"([^\\s\"]+)\"").getColumn(0));
        Collections.addAll(links, br.getRegex("<div[^>]+class\\s*=\\s*\"[^\"]*player[^\"]*\"[^>]*><iframe[^>]+src\\s*=\\s*\"([^\\s\"]+)\"").getColumn(0));
        String[][] apiLinks = br.getRegex("<a[^>]+onclick\\s*=\\s*\"[^\"]*ch_get_video\\s*\\(\\s*'([^']+)'\\s*,\\s*'([^']+)'\\s*\\)[^\"]*\"[^>]*>").getMatches();
        if (apiLinks != null && apiLinks.length > 0) {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            for (String[] apiLink : apiLinks) {
                getPage(brc, "/episode/cur_video/" + apiLink[0] + "/" + apiLink[1]);
                String[] embedLinks = brc.getRegex("src\\s*=\\s*[\\\\\"]+([^\"]+)[\\\\\"]+").getColumn(0);
                if (embedLinks != null && embedLinks.length > 0) {
                    for (String embedLink : embedLinks) {
                        links.add(embedLink.replace("\\", ""));
                    }
                }
            }
        }
        if (links != null && links.size() > 0) {
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