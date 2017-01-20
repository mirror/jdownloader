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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "soulmanga.net" }, urls = { "https?://(?:www\\.)?soulmanga\\.net/m/c[^/]+/[a-z0-9\\-]+" })
public class SoulmangaNet extends antiDDoSForDecrypt {

    public SoulmangaNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString() + "/0/";
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final Regex urlinfo = new Regex(parameter, "/m/(c[^/]+)/([^/]+)/");
        final String url_chapter = urlinfo.getMatch(0);
        final String url_name = urlinfo.getMatch(1);
        final DecimalFormat page_formatter_page = new DecimalFormat("000");
        String ext = null;

        short page_max = 0;
        final String[] pages = this.br.getRegex("<option value=\"(\\d+)\" >Página").getColumn(0);
        for (final String page_temp_str : pages) {
            final short page_temp = Short.parseShort(page_temp_str);
            if (page_temp > page_max) {
                page_max = page_temp;
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_chapter + "_" + url_name);

        for (short page = 0; page <= page_max; page++) {
            if (this.isAbort()) {
                return decryptedLinks;
            }
            final String page_formatted = page_formatter_page.format(page);
            // final String finallink = "directhttp://" + server_urlpart + chapter_formatted + chapter_str_extra + "-" + page_formatted +
            // ext;
            final String page_url = "/m/" + url_chapter + "/" + url_name + "/" + page + "/";
            if (page > 0) {
                /* When we start we are already on page 0 (or 1). */
                this.br.getPage(page_url);
            }

            final String finallink = this.br.getRegex("class=\"img\\-responsive\" src=\"(http[^<>\"]+)\"").getMatch(0);
            if (finallink == null) {
                return null;
            }
            if (ext == null) {
                /* No general extension given? Get it from inside the URL. */
                ext = getFileNameExtensionFromURL(finallink, ".jpg");
            }
            final String filename = url_chapter + "_" + url_name + "_" + page_formatted + ext;

            final DownloadLink dl = this.createDownloadlink(finallink);
            dl._setFilePackage(fp);
            dl.setName(filename);
            // dl.setContentUrl(page_url);
            dl.setLinkID(filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            distribute(dl);
        }

        return decryptedLinks;
    }

}
