//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "chauthanh.info" }, urls = { "http://[\\w\\.]*?chauthanh\\.info/(animeDownload/anime/.*?|anime/view/[a-z0-9\\-]+)\\.html" }, flags = { 0 })
public class ChThnhInfo extends PluginForDecrypt {

    public ChThnhInfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String ANIMEVIEW = "http://[\\w\\.]*?chauthanh\\.info/anime/view/[a-z0-9\\-]+\\.html";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Licensed, no download available<")) {
            logger.info("Link offline (not downloadable): " + parameter);
            return decryptedLinks;
        }
        String fpName = null;
        if (parameter.matches(ANIMEVIEW)) {
            if (br.containsHTML("class=\"center\">Server</th>[\t\n\r ]+<th>Size</th>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.getURL().equals("http://chauthanh.info/404")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            fpName = br.getRegex("<h2 itemprop=\"name\">([^<>\"]*?)</h2>").getMatch(0);
            final String[] links = br.getRegex("\\'\\.\\.(/download/[^<>\"]*?)\\'").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String finallink : links) {
                finallink = "http://chauthanh.info/anime" + Encoding.htmlDecode(finallink);
                decryptedLinks.add(createDownloadlink(finallink));
            }
        } else {
            if (br.containsHTML("The series information was not found on this server")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML("Removed due to licensed\\.")) {
                logger.info("Link offline (deleted): " + parameter);
                return decryptedLinks;
            }
            if (br.containsHTML(">No files available for this series")) {
                logger.info("Link offline ('No files available for this series'): " + parameter);
                return decryptedLinks;
            }
            if (br.getURL().equals("http://chauthanh.info/404")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            fpName = br.getRegex("<title>Download anime(.*?)\\- Download Anime").getMatch(0);
            if (fpName == null) fpName = br.getRegex("class=\"bold1\">Download anime(.*?)</span>").getMatch(0);
            String[] links = br.getRegex("<tr><td><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(/animeDownload/download/\\d+/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\\'\\.\\.(/download/[^<>\"]*?)\\'").getColumn(0);
            if (links == null || links.length == 0) {
                if (br.containsHTML("<th>Size</th>")) {
                    logger.info("Link offline: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String finallink : links) {
                if (finallink.startsWith("/download/")) {
                    finallink = "http://chauthanh.info/anime" + Encoding.htmlDecode(finallink);
                } else {
                    finallink = "http://chauthanh.info" + Encoding.htmlDecode(finallink);
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}