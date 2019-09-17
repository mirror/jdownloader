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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "0123moviestv.com" }, urls = { "https?://(www[0-9]*\\.)?0123moviestv\\.com/watch.+/.+" })
public class ZeroToThreeMoviesTV extends antiDDoSForDecrypt {
    public ZeroToThreeMoviesTV(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<meta property=\"og:title\" content=\"(?:Watch )?([^\"]+)(?: 0123movies)").getMatch(0);
        String[] episodeLinks = br.getRegex("<a class=\"btn-list[^\"]+\" href=\"([^\"]+season[\\-0-9]+episode[^\"]+)\"").getColumn(0);
        String[] seasonlinks = br.getRegex("<a class=\"btn-list[^\"]+\" href=\"([^\"]+season[\\-0-9]+online[^\"]+)\"").getColumn(0);
        ArrayList<String> videoLinks = getVideoLinks(br);
        appendListToDecryptedLinks(decryptedLinks, (Arrays.asList(episodeLinks)));
        appendListToDecryptedLinks(decryptedLinks, (Arrays.asList(seasonlinks)));
        appendListToDecryptedLinks(decryptedLinks, videoLinks);
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void appendListToDecryptedLinks(ArrayList<DownloadLink> decryptedLinks, Iterable<String> list) {
        if (list != null) {
            for (String listItem : list) {
                decryptedLinks.add(createDownloadlink(listItem));
            }
        }
    }

    private ArrayList<String> getVideoLinks(Browser br) throws IOException {
        final ArrayList<String> result = new ArrayList<String>();
        final String baseLinks = br.getRegex("data: \'([^\']+)\'").getMatch(0);
        String moreLinks = br.getRegex("\\{[^\\}]+( id:[^\\}]+)[^\\}]+\\}").getMatch(0);
        if (baseLinks != null) {
            result.addAll(retrieveVideoLinks(br, baseLinks.toString()));
        }
        if (moreLinks != null) {
            moreLinks = moreLinks.toString().replaceAll(" ", "").replaceAll(":", "=").replaceAll(",", "&").replaceAll("'", "");
            result.addAll(retrieveVideoLinks(br, moreLinks.toString()));
        }
        final LinkedHashSet<String> ret = new LinkedHashSet<String>(result);
        return new ArrayList<String>(ret);
    }

    private List<String> retrieveVideoLinks(Browser br, String queryString) throws IOException {
        final Browser brc = br.cloneBrowser();
        brc.getPage(brc.getURL("/includes/links.php?" + queryString));
        final String[] links = HTMLParser.getHttpLinks(brc.toString(), null);
        return Arrays.asList(links);
    }
}