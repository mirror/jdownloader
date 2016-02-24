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

    @SuppressWarnings("deprecation")
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
        final String fpName_for_filenames = encodeUnicode(fpName);
        final String url_language = new Regex(parameter, "video2brain\\.com/([^/]+)").getMatch(0);
        final String productid = jd.plugins.hoster.Video2brainCom.getProductID(this.br);
        final String videoid = jd.plugins.hoster.Video2brainCom.getActiveVideoID(this.br);

        if (this.br.containsHTML("Video\\.updateDocumentaryProductChapters") && productid != null && videoid != null) {
            /*
             * Find all chapters (= single videos - same URL structure as normal courses but for some reason they are called 'Chapters'
             * here)
             */
            jd.plugins.hoster.Video2brainCom.prepareAjaxRequest(this.br);
            final String postData = "product_id=" + productid + "&active_video_id=" + videoid;
            /* Works fine via GET request too */
            this.br.postPage("/" + url_language + "/custom/modules/video/video_ajax.cfc?method=updateDocumentaryProductChaptersJSON", postData);
            final String[] videoIDs = this.br.getRegex("id=(?:\\\\)?\"video_cell_(\\d+)(?:\\\\)?\"").getColumn(0);
            for (final String videoID : videoIDs) {
                final String url = "https://www.video2brain.com/" + url_language + "/videos-" + videoID + ".htm";
                final DownloadLink dl = this.createDownloadlink(url);
                /* Set nice temporary name for host plugin */
                dl.setName(fpName_for_filenames + "_" + productid + "_" + videoID + ".mp4");
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            String[] htmls = br.getRegex("<div class=\"length\"(.*?)class=\"additional\\-wrapper\"").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                /* Sometimes we need a 2nd attempt. TODO: Remove this if possible - one RegEx should be enough! */
                htmls = br.getRegex("class=\"other  has\\-teaser\">(.*?)class=\"video\\-bookmark bookmark\\-locked\"").getColumn(0);
            }
            if (htmls == null || htmls.length == 0) {
                logger.warning("Decrypter broken!");
                return null;
            }
            for (final String html : htmls) {
                String title = new Regex(html, "<strong>([^<>\"]+)</strong></a>").getMatch(0);
                String url = new Regex(html, "HREF=\\'([^<>\"\\']+)\\'").getMatch(0);
                String videoid_singlevideo = new Regex(html, "playVideoId\\((\\d+)").getMatch(0);
                if (videoid_singlevideo == null) {
                    videoid_singlevideo = new Regex(html, "ID=\\'video_(\\d+)\\'").getMatch(0);
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
                /* Lets try to make the filenames as similar-looking as the final filenames as we can. */
                if (html.contains("class=\"icon icon-icon-lock\"")) {
                    filename += "_paid_content";
                } else {
                    filename += "_tutorial";
                }
                if (productid != null && videoid_singlevideo != null) {
                    filename += "_" + productid + "_" + videoid_singlevideo;
                }
                filename += "_" + title + ".mp4";
                filename = encodeUnicode(filename);

                final DownloadLink dl = this.createDownloadlink(url);
                dl.setName(filename);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
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
