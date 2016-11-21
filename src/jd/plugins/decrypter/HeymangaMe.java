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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "heymanga.me" }, urls = { "https?://(?:www\\.)?heymanga\\.me/manga/[^/]+/\\d+" })
public class HeymangaMe extends PluginForDecrypt {

    public HeymangaMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String pagination_base = parameter;
        this.br.setFollowRedirects(true);
        br.setCookie(getHost(), "heymanga_adult", "yes");
        br.getPage(parameter + "/1");
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex urlinfo = new Regex(parameter, "manga/([^/]+)/(\\d+)");
        final String chapter_str = urlinfo.getMatch(1);
        final short chapter = Short.parseShort(chapter_str);
        final String url_name = urlinfo.getMatch(0);
        final String url_fpname = url_name + "_chapter_" + chapter_str;
        final DecimalFormat df_chapter = new DecimalFormat("0000");
        final DecimalFormat df_page = new DecimalFormat("000");

        short page_max = 1;
        final String[] pages = this.br.getRegex("<option value=\"(\\d+)\">Page").getColumn(0);
        for (final String page_temp_str : pages) {
            final short page_temp = Short.parseShort(page_temp_str);
            if (page_temp > page_max) {
                page_max = page_temp;
            }
        }

        short page = 1;
        do {
            /*
             * Every page usually contains the current direct-url plus the direct-url of the next page --> We can crawl faster by always
             * crawling both!
             */
            if (this.isAbort()) {
                return decryptedLinks;
            }
            if (page > 1) {
                this.br.getPage(pagination_base + "/" + page);
            }
            final String[] urls = this.br.getRegex("src=\"(//[^<>\"\\']+\\d+\\.jpe?g)\" onerror=\"this\\.src=\\'(//[^<>\"\\']+\\d+\\.jpe?g)\\'").getColumn(0);
            if (urls == null || urls.length == 0) {
                if (!this.br.containsHTML("id=\"page_list\"")) {
                    /* Empty page --> Offline url */
                    decryptedLinks.add(this.createDownloadlink(parameter));
                    return decryptedLinks;
                }
                break;
            }
            for (String finallink : urls) {
                finallink = "directhttp://" + br.getURL(finallink).toExternalForm();
                final String realPageStr = new Regex(finallink, "(\\d+)\\.jpe?g$").getMatch(0);
                final String chapter_formatted = df_chapter.format(chapter);
                final String page_formatted = df_page.format(page);
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl.setFinalFileName(url_name + "_" + chapter_formatted + "_" + page_formatted + ".jpg");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                page++;
            }
        } while (page <= page_max);

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_fpname);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}
