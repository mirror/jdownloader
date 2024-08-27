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
import java.util.HashSet;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mangahome.com" }, urls = { "https?://(?:www\\.)?(mangakoi|mangahome)\\.com/manga/[A-Za-z0-9\\-_]+(?:/v\\d+)?/c\\d+(?:\\.\\d+)?" })
public class MangahomeComCrawler extends antiDDoSForDecrypt {
    public MangahomeComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        /* Domain mangakoi.com is down / not owned by original owner anymore. */
        final String contenturl = param.toString().replaceFirst("mangakoi\\.com", "mangahome.com");
        getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex urlinfo = new Regex(contenturl, "https?://[^/]+/manga/([A-Za-z0-9\\-_]+)(?:/v\\d+)?/c(\\d+(?:\\.\\d+)?)");
        final String chapter_str = urlinfo.getMatch(1);
        final String chapter_str_main;
        String chapter_str_extra = "";
        if (chapter_str.contains(".")) {
            final String[] chapter_str_info = chapter_str.split("\\.");
            chapter_str_main = chapter_str_info[0];
            chapter_str_extra = "." + chapter_str_info[1];
        } else {
            chapter_str_main = chapter_str;
        }
        final short chapter_main = Short.parseShort(chapter_str_main);
        final String url_name = urlinfo.getMatch(0);
        final String url_fpname = url_name + "_chapter_" + chapter_str;
        final DecimalFormat df_chapter = new DecimalFormat("0000");
        final DecimalFormat df_page = new DecimalFormat("000");
        String ext = this.br.getRegex("(\\.[A-Za-z]+)\\?v=\\d+\" id=\"image\"").getMatch(0);
        if (ext == null) {
            ext = ".jpg";
        }
        final String[] urls = this.br.getRegex("class=\"image\" src=\"([^\"]+)").getColumn(0);
        if (urls == null || urls.length == 0) {
            if (br.containsHTML(">\\s*is not avaiable yet")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Content is not available yet");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final HashSet<String> dupes = new HashSet<String>();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (short page = 1; page < urls.length; page++) {
            String url = urls[page];
            url = br.getURL(url).toExternalForm();
            if (!dupes.add(url)) {
                /* Skip dupes */
                continue;
            }
            final String chapter_formatted = df_chapter.format(chapter_main);
            final String page_formatted = df_page.format(page);
            final DownloadLink dl = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
            final String filename = url_name + "_" + chapter_formatted + chapter_str_extra + "_" + page_formatted + ext;
            dl.setName(filename);
            dl.setProperty("filename", filename);
            dl.setAvailable(true);
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_fpname);
        fp.addLinks(ret);
        return ret;
    }
}
