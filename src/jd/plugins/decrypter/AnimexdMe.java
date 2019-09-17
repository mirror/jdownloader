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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animexd.me" }, urls = { "https?://(www\\.)?animexd\\.me/(anime|watch)/.+" })
public class AnimexdMe extends PluginForDecrypt {
    public AnimexdMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String page = br.getPage(parameter);
        String fpName = null;
        String[][] links = null;
        if (br.containsHTML("<p class=\"anime-details\">")) {
            fpName = br.getRegex("<meta (?:name|property)=\"(?:og:)?(?:title|description)\" content=\"Watch ([^\"]*?) Episode(?:[^\"]*?)\"").getMatch(0);
            int listStart = page.indexOf("<div class=\"container-item tnTabber\">");
            int listEnd = page.indexOf("<div class=\"create-comment\">", listStart + 1);
            listEnd = (listEnd < listStart) ? page.indexOf("<textarea id=\"anime-comment\"", listStart + 1) : listEnd;
            listEnd = (listEnd < listStart) ? page.indexOf("Ongoing Animes", listStart + 1) : listEnd;
            if (listEnd > listStart) {
                String listContent = br.toString().substring(listStart, listEnd);
                links = new Regex(listContent, "href=\"([^<>\"]*/watch/[^<>\"]*?)\">[\r\t\n ]*([^<]*?)[\r\t\n ]*</a>").getMatches();
            }
        } else if (br.containsHTML("<div class=\"video-main-new watchFull\">")) {
            fpName = br.getRegex("<meta (?:name|property)=\"(?:og:)?(?:title|description)\" content=\"Watch ([^\"]*?) Episode(?:[^\"]*?)\"").getMatch(0);
            links = br.getRegex("file: '([^<>']*?)'").getMatches();
            if (links == null || links.length == 0) {
                int listStart = page.indexOf("<!-- Video -->");
                int listEnd = page.indexOf("vmn-player", listStart + 1);
                listEnd = (listEnd < listStart) ? page.indexOf("vmn-list", listStart + 1) : listEnd;
                listEnd = (listEnd < listStart) ? page.indexOf("<div class=\"create-comment\">", listStart + 1) : listEnd;
                listEnd = (listEnd < listStart) ? page.indexOf("<textarea id=\"anime-comment\"", listStart + 1) : listEnd;
                if (listEnd > listStart) {
                    String listContent = br.toString().substring(listStart, listEnd);
                    links = new Regex(listContent, "href=\"([^<>\"]*/watch/[^<>\"]*?)\">[\r\t\n ]*([^<]*?)[\r\t\n ]*</a>").getMatches();
                }
            }
        }
        if (links != null) {
            for (String[] link : links) {
                DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(link[0]));
                if (link.length > 1) {
                    dl.setComment(link[1].trim());
                }
                decryptedLinks.add(dl);
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