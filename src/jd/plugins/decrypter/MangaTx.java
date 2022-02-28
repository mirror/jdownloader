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
import java.util.Arrays;
import java.util.HashSet;
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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "manga-tx.com" }, urls = { "https?://(?:www\\.)?manga-tx\\.com/manga/.+" })
public class MangaTx extends antiDDoSForDecrypt {
    public MangaTx(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        final FilePackage fp = FilePackage.getInstance();
        ArrayList<String> links = new ArrayList<String>();
        String itemID = new Regex(parameter, "/manga/([^/]+)").getMatch(0);
        String chapterID = new Regex(parameter, "/manga/[^/]+/([^/]+)").getMatch(0);
        if (StringUtils.isEmpty(chapterID)) {
            String[] chapters = br.getRegex("<li[^>]+class\\s*=\\s*\"\\s*[^\"]*wp-manga-chapter[^\"]*\"[^>]*>\\s*<a href\\s*=\\s*\"\\s*([^\"]+)\\s*").getColumn(0);
            if (chapters == null || chapters.length == 0) {
                getLogger().warning("Unable to find chapters!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String chapter : chapters) {
                final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(chapter));
                fp.add(dl);
                distribute(dl);
                decryptedLinks.add(dl);
            }
        } else {
            String[] pages = br.getRegex("<img[^>]+id\\s*=\\s*\"\\s*image-\\d+\\s*\"[^>]+data-src\\s*=\\s*\"\\s*([^\"]+)\\s*\"[^>]+class\\s*=\\s*\"\\s*\\s*wp-manga-chapter-img[^\"]*\"").getColumn(0);
            int pageCount = pages == null ? 0 : pages.length;
            if (pageCount <= 0) {
                getLogger().warning("Unable to retrieve page images!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                String fpName = Encoding.htmlOnlyDecode(br.getRegex("<h1[^>]+id\\s*=\\s*\"\\s*chapter-heading\\s*\"[^>]*>\\s*([^<]+)(?:\\s+-\\s+Chapter \\d+)\\s*").getMatch(0));
                String title = StringUtils.isEmpty(fpName) ? itemID : fpName;
                String[] chapters = br.getRegex("option[^>]+class\\s*=\\s*\"\\s*short\\s*\"[^>]+value\\s*=\\s*\"\\s*chapter-\\d+\"[^>]+data-redirect\\s*=\\s*\"\\s*([^\"]+)\"[^>]+>\\s*Chapter[^<]+").getColumn(0);
                if (chapters == null) {
                    getLogger().warning("Unable to determine chapter count!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                chapters = new HashSet<String>(Arrays.asList(chapters)).toArray(new String[0]);
                int chapterCount = chapters.length;
                int chapterNumber = Integer.parseInt(new Regex(chapterID, "-(\\d+)\\s*$").getMatch(0).toString());
                int pageNumber = 1;
                final int chapterPadlength = getPadLength(chapterCount);
                final int pagePadlength = getPadLength(pageCount);
                String chapter_formatted = String.format(Locale.US, "%0" + chapterPadlength + "d", chapterNumber);
                for (String page : pages) {
                    final DownloadLink dl = createDownloadlink(Encoding.htmlOnlyDecode(page));
                    String page_formatted = String.format(Locale.US, "%0" + pagePadlength + "d", pageNumber++);
                    String ext = getFileNameExtensionFromURL(page, ".jpg").replace("jpeg", "jpg");
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

    private int getPadLength(final int size) {
        return String.valueOf(size).length();
    }
}