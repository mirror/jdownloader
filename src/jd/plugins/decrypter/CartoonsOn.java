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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision: 41129 $", interfaceVersion = 2, names = { "cartoonson.tv" }, urls = { "https?://(www[0-9]*\\.)?cartoonson\\.tv/cartoons/(?:watch|view).*/.+" })
public class CartoonsOn extends antiDDoSForDecrypt {
    public CartoonsOn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceFirst("watch-preview", "watch");
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>(?:Watch )?([^<]*)- CartoonsOn").getMatch(0);
        String[] links = null;
        // Video embeds
        String[] embeds = br.getRegex("id=\"thePlayer\">[^>]*<iframe[^>]*src=\"([^\"]*)\"").getColumn(0);
        if (embeds != null && embeds.length > 0) {
            for (final String embed : embeds) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(embed)));
            }
        }
        // Video sources
        links = br.getRegex("<li>[^>]*<a[^>]*href=\"([^\"]+)\"[^<]*</a>[^<]*</li>").getColumn(0);
        if (links == null || links.length == 0) {
            if (links == null || links.length == 0) {
                // Video previews
                links = br.getRegex("href=\"([^\"]*)\"[^>]*><i[^>]*class=\"[^\"]*icon-overlay fa fa-play\">").getColumn(0);
                if (links != null && links.length > 0) {
                    links[0] = links[0].replaceAll("/watch-preview/id/", "/watch/id/");
                } else {
                    // Episode links
                    links = br.getRegex("href=\"([^\"]*)\"[^>]*class=\"[^\"]*play-episode-btn btn btn-default\">").getColumn(0);
                    if (links == null || links.length == 0) {
                        // Season links
                        links = br.getRegex("<h3[^>]+><a[^>]+href=\"([^\"]*)\">").getColumn(0);
                    }
                }
            }
        }
        if (links != null && links.length > 0) {
            for (final String link : links) {
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