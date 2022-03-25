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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "readcomicsonline.ru" }, urls = { "https?://(?:www\\.)?readcomicsonline\\.ru/comic/.+" })
public class ReadComicsOnlineRu extends antiDDoSForDecrypt {
    public ReadComicsOnlineRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        final FilePackage fp = FilePackage.getInstance();
        ArrayList<String> links = new ArrayList<String>();
        String itemID = new Regex(parameter, "/comic/([^/]+)").getMatch(0);
        String chapterID = new Regex(parameter, "/comic/[^/]+/([^/]+)").getMatch(0);
        if (StringUtils.isEmpty(chapterID)) {
            String[] chapters = br.getRegex("class\\s*=\\s*\"[^\"]*chapter-title-rtl[^\"]*\"[^>]*>\\s*<a[^>]*href\\s*=\\s*\"([^\"]+)\"").getColumn(0);
            for (String chapter : chapters) {
                final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(chapter));
                fp.add(dl);
                distribute(dl);
                decryptedLinks.add(dl);
            }
        } else {
            String pageBlock = br.getRegex("<div[^>]+id=\"all\"[^>]*>\\s*(<img[^$]*)\\s*<\\/div>").getMatch(0);
            String[] pages = new Regex(pageBlock, "data-src\\s*=\\s*[\"']\\s*([^\"']+)\\s*[\"']").getColumn(0);
            int pageCount = pages.length;
            if (pageCount > 0) {
                String fpName = Encoding.htmlOnlyDecode(br.getRegex("<h3>\\s*(?:<b>)?\\s*([^<]+)\\s*(?:<\\/b>)?\\s*(?:<small>)?Release\\s+Information").getMatch(0));
                String title = StringUtils.isEmpty(fpName) ? itemID : fpName;
                String chapterBlock = br.getRegex("(id=\"chapter-list\" class=\"dropdown\">[^§]*)</ul>").getMatch(0);
                if (chapterBlock.indexOf("</ul>") > 0) {
                    chapterBlock = chapterBlock.substring(0, chapterBlock.indexOf("</ul"));
                }
                int chapterCount = new Regex(chapterBlock, "<li").count();
                int chapterNumber = Integer.parseInt(chapterID);
                int pageNumber = 1;
                final int pagePadlength = StringUtils.getPadLength(pageCount);
                String chapter_formatted = String.format(Locale.US, "%0" + StringUtils.getPadLength(chapterCount) + "d", chapterNumber);
                for (String page : pages) {
                    final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(page));
                    String page_formatted = String.format(Locale.US, "%0" + pagePadlength + "d", pageNumber++);
                    String ext = getFileNameExtensionFromURL(page, ".jpg");
                    dl.setFinalFileName(title + "_" + chapter_formatted + "_" + page_formatted + ext);
                    if (StringUtils.isNotEmpty(title)) {
                        fp.setName(title);
                    }
                    fp.add(dl);
                    distribute(dl);
                    decryptedLinks.add(dl);
                }
            }
        }
        return decryptedLinks;
    }
}