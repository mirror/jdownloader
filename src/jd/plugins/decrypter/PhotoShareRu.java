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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "photoshare.ru" }, urls = { "https?://(?:www\\.)?photoshare\\.ru/(?:album\\d+\\.html|login/album\\.php\\?id=\\d+)" })
public class PhotoShareRu extends PluginForDecrypt {

    public PhotoShareRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String html_passwordprotected = "name=\"password\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> allPages = new ArrayList<String>();
        allPages.add("1");
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "(\\d+)(?:\\.html)?$").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://photoshare.ru/")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML(">Альбом пуст<")) {
            logger.info("Link offline (album empty): " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        if (this.br.containsHTML(html_passwordprotected)) {
            boolean failed = true;
            for (int i = 0; i <= 2; i++) {
                final String passCode = getUserInput("Password?", param);
                this.br.postPage("/login/album.php?id=" + fid + "&from=", "password=" + Encoding.urlEncode(passCode));
                if (this.br.containsHTML(html_passwordprotected)) {
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }

        final Regex fpn = br.getRegex("<h1 style=\"margin: 0px; padding: 0px;\">([^<>\"]*?)</h1>([^<>\"]*?)</div>");
        if (fpn.getMatches().length != 1) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String fpName = Encoding.htmlDecode(fpn.getMatch(0).trim() + " - " + fpn.getMatch(1).trim());

        br.getPage("http://photoshare.ru/do/change_mpp.php?mpp=100");
        br.getPage(parameter);

        final String albumID = new Regex(parameter, "album(\\d+)\\.html$").getMatch(0);
        final String[] pages = br.getRegex("<a href=\"/album" + albumID + "\\-(\\d+)\\.html\"").getColumn(0);
        if (pages != null && pages.length != 0) {
            for (final String page : pages) {
                if (!allPages.contains(page)) {
                    allPages.add(page);
                }
            }
        }

        for (final String currentPage : allPages) {
            logger.info("Decrypting page " + currentPage + " of " + allPages.size());
            if (!currentPage.equals("1")) {
                br.getPage("http://photoshare.ru/album" + albumID + "-" + currentPage + ".html");
            }
            final String[][] links = br.getRegex("class=\"phototxt\"><b><a href=\"(/photo\\d+\\.html)\" class=\"title\">([^<>\"]*?)</a>").getMatches();
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink[] : links) {
                final DownloadLink dl = createDownloadlink("http://photoshare.ru" + singleLink[0]);
                // Ending may be changed before download
                dl.setName(Encoding.htmlDecode(singleLink[1].trim()) + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}