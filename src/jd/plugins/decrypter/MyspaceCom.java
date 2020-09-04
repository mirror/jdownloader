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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "myspace.com" }, urls = { "https?://(?:www\\.)?myspace\\.com/([^/]+)/photos" })
public class MyspaceCom extends PluginForDecrypt {
    public MyspaceCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final ArrayList<String> dupes = new ArrayList<String>();
        final String parameter = param.toString();
        final String username = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username);
        final String hash = PluginJSonUtils.getJson(br, "hashMashter");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        /* 2020-09-04: We do not need this header */
        // br.getHeaders().put("Client",
        // "persistentId=...");
        int page = 0;
        int itemCounter = 0;
        final DecimalFormat df = new DecimalFormat("0000");
        do {
            page++;
            logger.info("Crawling page: " + page);
            final String[] htmls = br.getRegex("<li[^>]*class\\s*=\\s*\"item media photo flexHeight cap size_300\"\\s*(.*?)\\s*</li>").getColumn(0);
            if (htmls == null || htmls.length == 0) {
                logger.info("Stopping because there are no new items on current page");
                break;
            }
            String lastPhotoID = null;
            boolean foundNewItem = false;
            for (final String html : htmls) {
                final String photoID = new Regex(html, "data-photoId\\s*=\\s*\"(\\d+)\"").getMatch(0);
                String url = new Regex(html, "data-image-url\\s*=\\s*\"(https?://[^\"]+)\"").getMatch(0);
                if (photoID == null || url == null) {
                    continue;
                } else if (dupes.contains(photoID)) {
                    continue;
                }
                itemCounter++;
                final String filename;
                String title = new Regex(html, "data-title\\s*=\\s*\"([^\"]+)\"").getMatch(0);
                if (!StringUtils.isEmpty(title)) {
                    title = Encoding.htmlDecode(title);
                    filename = username + "_" + df.format(itemCounter) + "_" + title + ".jpg";
                } else {
                    filename = username + "_" + df.format(itemCounter) + ".jpg";
                }
                foundNewItem = true;
                /* Try to "convert" Thumbnail URL --> Full size (best quality) */
                final String resolution = new Regex(url, "(/\\d+x\\d+\\.jpg)$").getMatch(0);
                if (resolution != null) {
                    url = url.replace(resolution, "/full.jpg");
                }
                final DownloadLink dl = createDownloadlink(url);
                dl.setForcedFileName(filename);
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                lastPhotoID = photoID;
            }
            if (!foundNewItem) {
                logger.info("Stopping because failed to find new items on current pagination section");
                break;
            }
            if (StringUtils.isEmpty(hash)) {
                logger.info("Cannot do pagination because hash is missing");
                break;
            }
            br.getHeaders().put("Hash", hash);
            /* Pagination (async reload mechanism) */
            br.postPage("/ajax/" + username + "/photosStream", "lastImageId=" + lastPhotoID);
            /* Decode json response */
            br.getRequest().setHtmlCode(PluginJSonUtils.unescape(br.toString()));
        } while (!this.isAbort());
        return decryptedLinks;
    }
}
