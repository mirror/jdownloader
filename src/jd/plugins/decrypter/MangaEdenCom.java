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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangaeden.com" }, urls = { "https?://(www\\.)?mangaeden\\.com/(?:[a-z]{2}/)?[a-z0-9\\-]+/[a-z0-9\\-]+/\\d+(?:\\.\\d+)?/1/" })
public class MangaEdenCom extends antiDDoSForDecrypt {
    public MangaEdenCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> cryptedLinks = new ArrayList<String>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.containsHTML("404 NOT FOUND")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Isn't Out!<")) {
            logger.info("Link offline (next chapter isn't out yet): " + parameter);
            return decryptedLinks;
        }
        final String thisLinkpart = new Regex(br.getURL(), "mangaeden\\.com(/.*?)1/$").getMatch(0);
        String fpName = br.getRegex("<title>\\s*([^<>\"]*?)(?:\\s*-\\s*page \\d+)?\\s*-\\s*(?:Read Manga Online Free|Manga Eden)").getMatch(0);
        final String[] pages = br.getRegex("<option[^>]+value=\"(" + thisLinkpart + "\\d+/)\"").getColumn(0);
        if (pages == null || pages.length == 0 || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        fpName = Encoding.htmlDecode(fpName.trim()).replace("\n", "");
        for (final String currentPage : pages) {
            if (!cryptedLinks.contains(currentPage)) {
                cryptedLinks.add(currentPage);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        // decrypt all pages
        final DecimalFormat df = new DecimalFormat(cryptedLinks.size() < 100 ? "00" : "000");
        int counter = 1;
        for (final String currentPage : cryptedLinks) {
            if (isAbort()) {
                break;
            }
            if (!br.getURL().endsWith(currentPage)) {
                getPage(currentPage);
            }
            final String decryptedlink = getSingleLink();
            final DownloadLink dd = createDownloadlink("directhttp://" + decryptedlink);
            dd.setAvailable(true);
            dd.setFinalFileName(fpName + "_" + df.format(counter) + getFileNameExtensionFromString(decryptedlink, ".jpg"));
            fp.add(dd);
            distribute(dd);
            decryptedLinks.add(dd);
            counter++;
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getSingleLink() {
        String finallink = br.getRegex("<img[^>]+id=\"mainImg\"[^>]+src=\"((?:https?:)?//[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("\"((?:https?:)?//(?:www\\.)?cdn\\.mangaeden\\.com/mangasimg/[^<>\"]*?)\"").getMatch(0);
        }
        if (finallink == null) {
            return null;
        }
        finallink = Request.getLocation(finallink, br.getRequest());
        return finallink;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}