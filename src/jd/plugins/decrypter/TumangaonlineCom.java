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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tumangaonline.com" }, urls = { "https?://(?:www\\.)?tumangaonline\\.com/lector/[^/]+/\\d+/\\d+\\.\\d{2}/\\d+" })
public class TumangaonlineCom extends antiDDoSForDecrypt {

    public TumangaonlineCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Tags: MangaPictureCrawler */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        final Regex urlinfo = new Regex(parameter, "lector/([^/]+)/(\\d+)/([0-9\\.]+)/(\\d+)");
        final String url_name = urlinfo.getMatch(0);
        final String url_idManga = urlinfo.getMatch(1);
        final String url_idScanlation = urlinfo.getMatch(3);
        final String url_numeroCapitulo = urlinfo.getMatch(2);
        /* Double (as String) to int */
        final String url_chapter = new Regex(url_numeroCapitulo, "(\\d+)\\..+").getMatch(0);
        final DecimalFormat page_formatter_page = new DecimalFormat("000");

        String ext = null;

        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        getPage(String.format("https://tumangaonline.com/api/v1/imagenes?idManga=%s&idScanlation=%s&numeroCapitulo=%s&visto=true", url_idManga, url_idScanlation, url_numeroCapitulo));
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());

        final String official_downloadurl = (String) entries.get("urlDescarga");
        final String images_array_text = Encoding.unicodeDecode((String) entries.get("imagenes"));

        final FilePackage fp = FilePackage.getInstance();
        /* 2017-30-31: First chapter name, then chapter number (user request, thread 73101) */
        fp.setName(url_name + "_" + url_chapter);

        final String[] images = new Regex(images_array_text, "\"([^<>\"]+)\"").getColumn(0);
        int page = 0;
        for (final String image : images) {
            if (this.isAbort()) {
                return decryptedLinks;
            }

            final String page_formatted = page_formatter_page.format(page);
            final String finallink = String.format("https://img1.tumangaonline.com/subidas/%s/%s/%s/%s", url_idManga, url_numeroCapitulo, url_idScanlation, image);
            if (finallink == null) {
                return null;
            }
            ext = getFileNameExtensionFromURL(finallink, ".jpg");
            final String filename = url_chapter + "_" + url_name + "_" + page_formatted + ext;

            final DownloadLink dl = this.createDownloadlink("directhttp://" + finallink);
            dl._setFilePackage(fp);
            dl.setName(filename);
            dl.setLinkID(filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            distribute(dl);
            page++;
        }

        if (!StringUtils.isEmpty(official_downloadurl)) {
            /* E.g. single mega.co.nz url. Do not add it to the package which contains the single images! */
            decryptedLinks.add(this.createDownloadlink(official_downloadurl));
        }

        return decryptedLinks;
    }

}
