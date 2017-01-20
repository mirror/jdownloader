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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "viz.com" }, urls = { "https?://(?:www\\.)?viz\\.com/[^/]+/chapters/digital/[^/]+/\\d+" })
public class VizCom extends antiDDoSForDecrypt {

    public VizCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex urlinfo = new Regex(parameter, "/chapters/digital/([^/]+)/(\\d+)");
        final String url_name = urlinfo.getMatch(0);
        final String id = urlinfo.getMatch(1);
        final DecimalFormat page_formatter_page = new DecimalFormat("000");
        final String ext = ".jpg";

        int page_added_num = 0;
        int page_current = 0;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(id + "_" + url_name);

        do {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            final String page_formatted = page_formatter_page.format(page_current);
            final String page_url = "https://www." + this.getHost() + "/manga/get_manga_url?manga_id=" + id + "&page=" + page_current + "&device_id=3&loadermax=1";
            this.br.getHeaders().put("Referer", "https://www.viz.com/assets/reader-" + System.currentTimeMillis() + ".swf");
            this.br.getPage(page_url);

            final String[] urls = this.br.getRegex("url=\"(http[^<>\"]+)\"").getColumn(0);
            page_added_num = urls.length;
            for (final String url : urls) {
                final String filename = id + "_" + url_name + "_" + page_formatted + ext;

                final DownloadLink dl = this.createDownloadlink("directhttp://" + url);
                dl._setFilePackage(fp);
                dl.setName(filename);
                // dl.setContentUrl(page_url);
                dl.setLinkID(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                distribute(dl);
                page_current++;
            }
        } while (page_added_num >= 2);

        return decryptedLinks;
    }

}
