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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "video2brain.com" }, urls = { "https?://(?:www\\.)?video2brain\\.com/(de/videotraining/[a-z0-9\\-]+|en/courses/[a-z0-9\\-]+|fr/formation/[a-z0-9\\-]+|es/cursos/[a-z0-9\\-]+)" }, flags = { 0 })
public class Video2brainComDecrypter extends PluginForDecrypt {

    public Video2brainComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().matches("https?://(?:www\\.)?video2brain\\.com/[a-z]{2}/[^/]+/[a-z0-9\\-]+")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback to url-packagename */
            fpName = parameter.substring(parameter.lastIndexOf("/") + 1);
        }
        final String url_language = new Regex(parameter, "video2brain\\.com/([^/]+)").getMatch(0);
        final String productid = this.br.getRegex("var support_product_id[\t\n\r ]*?=[\t\n\r ]*?(\\d+);").getMatch(0);

        /*
         * Spanish courses sometimes have completely different structures e.g.
         * https://www.video2brain.com/es/cursos/chris-korn-diseno-digital-de-personajes
         */
        final String[] chapters = this.br.getRegex("href=\"([^<>\"\\']+)\" itemprop=\"url\">").getColumn(0);

        String[] htmls = br.getRegex("<div class=\"length\"(.*?)class=\"additional\\-wrapper\"").getColumn(0);
        if (htmls == null || htmls.length == 0) {
            htmls = br.getRegex("class=\"other  has\\-teaser\">(.*?)class=\"video\\-bookmark bookmark\\-locked\"").getColumn(0);
        }
        if (htmls == null || htmls.length == 0) {
            /* May no downloadable content available - lets add all chapters if we found some */
            for (String chapter : chapters) {
                if (!chapter.startsWith("http")) {
                    chapter = "https://video2brain.com/" + url_language + "/" + chapter;
                }
                if (new Regex(chapter, this.getSupportedLinks()).matches()) {
                    /* Prevent decryption loops */
                    continue;
                }
                decryptedLinks.add(this.createDownloadlink(chapter));
            }
            return decryptedLinks;
        }
        for (final String html : htmls) {

            String title = new Regex(html, "<strong>([^<>\"]+)</strong></a>").getMatch(0);
            String url = new Regex(html, "HREF=\\'([^<>\"\\']+)\\'").getMatch(0);
            String videoid = new Regex(html, "playVideoId\\((\\d+)").getMatch(0);
            if (videoid == null) {
                videoid = new Regex(html, "ID=\\'video_(\\d+)\\'").getMatch(0);
            }
            if (url == null) {
                /* Skip invalid content! */
                continue;
            }

            if (!url.startsWith("http")) {
                url = "https://video2brain.com/" + url_language + "/" + url;
            }
            if (new Regex(url, this.getSupportedLinks()).matches()) {
                /* Prevent decryption loops */
                continue;
            }

            if (title == null) {
                title = new Regex(url, "([^/]+)$").getMatch(0);
            }

            if (title == null) {
                /* Should never happen */
                continue;
            }

            title = Encoding.htmlDecode(title.trim());
            String filename = "video2brain_" + url_language;
            if (html.contains("class=\"icon icon-icon-lock\"")) {
                filename += "_paid_content";
            } else {
                filename += "_tutorial";
            }
            if (productid != null && videoid != null) {
                filename += "_" + productid + "_" + videoid;
            }
            filename += "_" + title + ".mp4";
            filename = encodeUnicode(filename);
            final DownloadLink dl = this.createDownloadlink(url);
            dl.setName(filename);
            dl.setAvailable(true);

            decryptedLinks.add(dl);
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

}
