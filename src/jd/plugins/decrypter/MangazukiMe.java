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
import java.util.Locale;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangazuki.me" }, urls = { "https?://(?:www\\.)?(?:mangazuki\\.me|yomanga\\.info)/manga/[^/]+(?:/\\w+.+)?" })
public class MangazukiMe extends antiDDoSForDecrypt {
    public MangazukiMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        final String fpName = br.getRegex("<title>(?:Read\\s*)?([^<]+)\\s*(?:Free|-\\s*YoManga)").getMatch(0);
        final Regex urlinfo = new Regex(parameter, "/manga/([^/]+)/[^/]+-(\\d+)");
        final String[] chapters = br.getRegex("<li class=\"wp-manga-chapter\\s*\">\\s*<a href=\"([^\"]+)\">").getColumn(0);
        if (chapters != null && chapters.length > 0) {
            for (String chapter : chapters) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(chapter)));
            }
            return decryptedLinks;
        }
        final String url_name = urlinfo.getMatch(0);
        final String url_chapter = urlinfo.getMatch(1);
        final String[] chapterUrls = br.getRegex("<option class=\"short\"[^>]+data-redirect=\"([^\"]+)\"").getColumn(0);
        final String url_chapter_formatted = String.format(Locale.US, "%0" + getPadLength(chapterUrls.length) + "d", Integer.parseInt(url_chapter));
        String ext = null;
        final FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            fp.setName(fpName);
        } else {
            fp.setName(url_name + "_Chapter_" + url_chapter_formatted);
        }
        final String[] images = br.getRegex("<img[^>]+src=\"\\s*([^\"]+)\\s*\" class=\"wp-manga-chapter-img\">").getColumn(0);
        if (images == null || images.length == 0) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final int padLength = getPadLength(images.length);
        int page = 1;
        for (final String url_image : images) {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            final String page_formatted = String.format(Locale.US, "%0" + padLength + "d", page);
            if (ext == null) {
                /* No general extension given? Get it from inside the URL. */
                ext = getFileNameExtensionFromURL(url_image, ".jpg");
            }
            String filename = url_name + "_Chapter_" + url_chapter_formatted + "_" + page_formatted + ext;
            DownloadLink dl = createDownloadlink(url_image);
            dl._setFilePackage(fp);
            dl.setFinalFileName(filename);
            dl.setLinkID(filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            distribute(dl);
            page++;
        }
        return decryptedLinks;
    }

    private final int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}
