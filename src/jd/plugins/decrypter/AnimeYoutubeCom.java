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
import java.util.Collections;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "animeyoutube.com" }, urls = { "https?://(www\\.)?animeyoutube\\.com/(episode/)?[^/]+" })
public class AnimeYoutubeCom extends antiDDoSForDecrypt {
    public AnimeYoutubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>(?:Watch)?\\s*([^<]+)\\s+(?:-\\s+Kissanime|online anime free)").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<h3>\\s*<a[^>]+href\\s*=\\s*\"([^\"]+/episode/[^\"]+)\"").getColumn(0));
        Collections.addAll(links, br.getRegex("<div[^>]+class\\s*=\\s*\"[^\"]*link-video\"[^>]*>\\s*<input[^>]+type\\s*=\\s*\"hidden\"[^>]+value\\s*=\\s*\"([^\"]+)\"").getColumn(0));
        for (String link : links) {
            if (link != null) {
                link = processPrefixSlashes(br, Encoding.htmlDecode(link));
                decryptedLinks.add(createDownloadlink(link));
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String processPrefixSlashes(Browser br, String link) throws IOException {
        link = link.trim().replaceAll("^//", "https://");
        if (link.startsWith("/") || !link.startsWith("http")) {
            link = br.getURL(link).toString();
        }
        return link;
    }
}