//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hd-pornblog.org" }, urls = { "http://hd\\-pornblog\\.org/\\?p=\\d+" }, flags = { 0 })
public class HdPrnBlgOg extends PluginForDecrypt {

    /**
     * @author OhGod + raztoki
     */

    // Dev notes
    // only grabs lower qual stuff. (designed in that manner)

    public HdPrnBlgOg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String contentReleaseName = br.getRegex("<h1 class=\"entry\\-title\">(.*?)</h1>").getMatch(0);
        if (contentReleaseName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        contentReleaseName = Encoding.htmlDecode(contentReleaseName).trim();

        // Try to get different qualities and set another packagename for each
        // one
        final String[][] niceWay = { { "<img src=\"http://hd\\-pornblog\\.org/images/Download\\.png\" border=\"0\"/></p>(.*?)\"http://hd\\-pornblog\\.org/", " (Low res)" }, { "img src=\"http://hd\\-pornblog\\.org/images/HD720p\\.png\" border=\"0\"/></p>(.*?)\"http://hd\\-pornblog\\.org/", " (Mid res)" }, { "<p><img src=\"http://hd\\-pornblog\\.org/images/HD1080p\\.png\" border=\"0\"/></p>(.*?)\"http://hd\\-pornblog\\.org/", " (High res)" } };
        for (final String[] nice : niceWay) {
            String linktext = br.getRegex(nice[0]).getMatch(0);
            if (linktext != null) {
                final String tempLinks[] = new Regex(linktext, "<a href=\"(http[^<>\"]*?)\"").getColumn(0);
                if (tempLinks != null && tempLinks.length != 0) {
                    final FilePackage fp = FilePackage.getInstance();
                    fp.setName(contentReleaseName + nice[1]);
                    for (final String aLink : tempLinks) {
                        if (!aLink.matches("http://hd\\-pornblog\\.org/\\?p=\\d+")) {
                            final DownloadLink dl = createDownloadlink(aLink);
                            fp.add(dl);
                            try {
                                distribute(dl);
                            } catch (final Throwable e) {
                                /* does not exist in 09581 */
                            }
                            decryptedLinks.add(dl);
                        }
                    }
                }
            }
        }

        // If "nice" handling fails, just grab all links
        if (decryptedLinks.size() == 0) {
            final String[] links = new Regex(br.toString(), "<a href=\"(http[^\"]+)", Pattern.CASE_INSENSITIVE).getColumn(0);
            if (links == null || links.length == 0) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            for (final String link : links) {
                if (!link.matches("http://hd\\-pornblog\\.org/\\?p=\\d+")) decryptedLinks.add(createDownloadlink(link));
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(contentReleaseName);
            fp.addLinks(decryptedLinks);
        }
        // assuming that this img hoster is used exclusively.
        final String[] imgs = br.getRegex("\"(http://i\\d+\\.fastpic\\.ru/thumb/\\d{4}/\\d+/\\d+/[a-z0-9]+\\.jpeg)\"").getColumn(0);
        if (imgs != null && imgs.length != 0) {
            for (String img : imgs) {
                decryptedLinks.add(createDownloadlink("directhttp://" + img.replace("/thumb/", "/big/").replace(".jpeg", ".jpg")));
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(contentReleaseName + " (Images)");
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}