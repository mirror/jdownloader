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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "readallcomics.com" }, urls = { "https?://(?:www\\\\.)?readallcomics\\.com/(?:category/)?[^/]+/?" })
public class ReadAllComics extends antiDDoSForDecrypt {
    public ReadAllComics(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*([^<]+)\\s+&#124;\\s+Read\\s+All\\s+Comics\\s+Online").getMatch(0);
        String itemName = new Regex(parameter, "/(?:category/)?([^/]+)/?").getMatch(0);
        fpName = (StringUtils.isEmpty(fpName) ? Encoding.htmlDecode(itemName) : Encoding.htmlDecode(fpName)).replaceAll("…", "").trim();
        final FilePackage fp = FilePackage.getInstance();
        if (StringUtils.containsIgnoreCase(parameter, "/category/")) {
            String linkSection = br.getRegex("<ul class=\"list-story\">([^$]+)</ul>").getMatch(0);
            String[] chapters = new Regex(linkSection, "href=[\"\']([^\"\']+)[\"\']").getColumn(0);
            if (chapters != null && chapters.length > 0) {
                if (StringUtils.isNotEmpty(fpName)) {
                    fp.setName(Encoding.htmlDecode(fpName.trim()));
                    fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
                    fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
                }
                for (String chapter : chapters) {
                    final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(chapter));
                    fp.add(dl);
                    distribute(dl);
                    decryptedLinks.add(dl);
                }
            }
        } else {
            String linkSection = br.getRegex("<div[^>]+data-wpusb-component\\s*=\\s*\"[^\"]*buttons-section[^\"]*\"[^>]*>([^$]+)<div[^>]+data-wpusb-component\\s*=\\s*\"[^\"]*buttons-section[^\"]*\"[^>]*>").getMatch(0);
            String[] images = new Regex(linkSection, "<img[^>]+src\\s*=\\s*\"\\s*([^\"]+)\\s*\"[^>]*>").getColumn(0);
            if (images != null && images.length > 0) {
                fp.setName(Encoding.htmlDecode(fpName));
                final int padlength = getPadLength(images.length);
                int page = 1;
                for (String image : images) {
                    String page_formatted = String.format(Locale.US, "%0" + padlength + "d", page++);
                    image = Encoding.htmlDecode(image);
                    final DownloadLink dl = createDownloadlink("directhttp://" + image);
                    String ext = getFileNameExtensionFromURL(image, ".jpg");
                    dl.setFinalFileName(fpName + "_" + page_formatted + ext);
                    fp.add(dl);
                    distribute(dl);
                    decryptedLinks.add(dl);
                }
            }
        }
        return decryptedLinks;
    }

    private int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}