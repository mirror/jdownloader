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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mangakakalot.com" }, urls = { "https?://(www\\.)?mangakakalot\\.com/(manga|chapter)/.*" })
public class Mangakakalot extends antiDDoSForDecrypt {
    public Mangakakalot(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> cryptedLinks = new ArrayList<String>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 503 });
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 NOT FOUND")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (br.getHttpConnection().getResponseCode() == 503) {
            logger.info("Too many requests - try again later");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String chapterBlock = br.getRegex("<div class=\"chapter-list\">(.*)<div class=\"comment-info\">").getMatch(0);
        final String[] chapters = chapterBlock == null ? null : HTMLParser.getHttpLinks(chapterBlock, null);
        if (chapters != null && chapters.length > 0) {
            for (final String chapter : chapters) {
                if (!cryptedLinks.contains(chapter)) {
                    decryptedLinks.add(createDownloadlink(chapter));
                }
            }
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<]+) ?- Mangakakalot.com</title>").getMatch(0);
        final String pageBlock = br.getRegex("<div class=\"vung-doc\"[^>]*>(.*)<div style=\"text-align:center;\">").getMatch(0);
        final String[] pages = pageBlock == null ? null : HTMLParser.getHttpLinks(pageBlock, null);
        if (pages == null || pages.length == 0 || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String page : pages) {
            if (!cryptedLinks.contains(page)) {
                cryptedLinks.add(page);
            }
        }
        fpName = Encoding.htmlDecode(fpName.trim()).replace("\n", "");
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        final DecimalFormat df = new DecimalFormat(new String(new char[String.valueOf(cryptedLinks.size()).length()]).replace("\0", "0"));
        int pageCounter = 1;
        for (final String currentPage : cryptedLinks) {
            if (isAbort()) {
                break;
            }
            final String decryptedlink = currentPage;
            final DownloadLink dd = createDownloadlink("directhttp://" + decryptedlink);
            dd.setAvailable(true);
            dd.setFinalFileName(fpName + "_" + df.format(pageCounter) + getFileNameExtensionFromString(decryptedlink, ".jpg"));
            fp.add(dd);
            distribute(dd);
            decryptedLinks.add(dd);
            pageCounter++;
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}