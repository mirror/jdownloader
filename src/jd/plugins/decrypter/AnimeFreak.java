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

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animefreak.tv" }, urls = { "https?://(www\\.)?animefreak\\.tv/watch/.+" })
public class AnimeFreak extends antiDDoSForDecrypt {
    public AnimeFreak(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*(?:Watch\\s+)?([^<]+)\\s+(?:English [SD]ubbed)").getMatch(0);
        String itemID = new Regex(parameter, "watch/([^/]+)").getMatch(0);
        String[] links = null;
        links = br.getRegex("(?:var\\s+)?file\\s*[=:]\\s*\"([^\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("href\\s*=\\s*\"([^\"]+/watch/" + Pattern.quote(itemID).toString() + "/[^\"]+)\"").getColumn(0);
        }
        for (String link : links) {
            link = processPrefixSlashes(Encoding.htmlDecode(link));
            decryptedLinks.add(createDownloadlink(link));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String processPrefixSlashes(String link) throws IOException {
        link = link.trim().replaceAll("^//", "https://");
        if (link.startsWith("/")) {
            link = this.br.getURL(link).toString();
        }
        return link;
    }
}