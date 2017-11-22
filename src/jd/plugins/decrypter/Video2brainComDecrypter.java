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
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "video2brain.com" }, urls = { "https?://(?:www\\.)?video2brain\\.com/(de/videotraining/[a-z0-9\\-]+|en/courses/[a-z0-9\\-]+|fr/formation/[a-z0-9\\-]+|es/cursos/[a-z0-9\\-]+)" })
public class Video2brainComDecrypter extends PluginForDecrypt {
    public Video2brainComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final PluginForHost hosterplugin = JDUtilities.getPluginForHost("video2brain.com");
        final SubConfiguration cfg = hosterplugin.getPluginConfig();
        final boolean add_position = cfg.getBooleanProperty(jd.plugins.hoster.Video2brainCom.ADD_ORDERID, jd.plugins.hoster.Video2brainCom.defaultADD_ORDERID);
        boolean loggedIN = false;
        final Account aa = AccountController.getInstance().getValidAccount(hosterplugin);
        if (aa != null) {
            try {
                jd.plugins.hoster.Video2brainCom.login(this.br, aa);
                loggedIN = true;
            } catch (final Throwable e) {
            }
        }
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
        final String videoid_first_video = jd.plugins.hoster.Video2brainCom.getActiveVideoID(this.br);
        long counter = 1;
        if (productid == null) {
            logger.info("productid is null either the content is offline or it has not yet been released!");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        jd.plugins.hoster.Video2brainCom.prepareAjaxRequest(this.br);
        final String token = jd.plugins.hoster.Video2brainCom.getToken(this.br);
        final String json_update_loader_function_name = br.getRegex("Video\\.(updateWeeklySeriesEpisodes|updateDocumentaryProductChapters)").getMatch(0);
        String postData = "";
        if (token != null) {
            postData += "token=" + Encoding.urlEncode(token) + "&";
        }
        if (json_update_loader_function_name != null && videoid_first_video != null) {
            /*
             * Find all chapters (= single videos - same URL structure as normal courses but for some reason they are called 'Chapters'
             * here)
             */
            postData += "product_id=" + productid + "&active_video_id=" + videoid_first_video;
            /* Works fine via GET request too */
            final String postURL = "/" + url_language + "/custom/modules/video/video_ajax.cfc?method=" + json_update_loader_function_name + "JSON";
            jd.plugins.hoster.Video2brainCom.prepAjaxHeaders(this.br);
            this.br.postPage(postURL, postData);
            /* E.g. updateDocumentaryProductChapters */
            String[] videoIDs = this.br.getRegex("id=(?:\\\\)?\"video_cell_(\\d+)(?:\\\\)?\"").getColumn(0);
            if (videoIDs == null || videoIDs.length == 0) {
                /* E.g. updateWeeklySeriesEpisodes */
                videoIDs = this.br.getRegex("id=(?:\\\\)?\"video\\-row\\-(\\d+)(?:\\\\)?\"").getColumn(0);
            }
            if (videoIDs != null) {
                for (final String videoID : videoIDs) {
                    final String url = createOldDownloadURL(url_language, videoID);
                    final DownloadLink dl = this.createDownloadlink(url);
                    /* Set nice temporary name for host plugin */
                    final String temp_name = "video2brain_" + productid + "_" + videoID + "_" + url_language + "_unknown_content_" + "_" + fpName_for_filenames + ".mp4";
                    dl.setName(temp_name);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                    counter++;
                }
            }
        } else {
            if (loggedIN) {
                postData += "product_id=" + productid;
                this.br.postPage("/" + url_language + "/custom/modules/product/product_ajax.cfc?method=renderProductDetailTOC", postData);
                /* Remove js escape */
                this.br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
            }
            String[] htmls = br.getRegex("class=\"video\\-title lvl\\-\\d+ \"(.*?)class=\"additional\\-wrapper\"").getColumn(0);
            if (htmls != null) {
                for (final String html : htmls) {
                    String title = new Regex(html, "<strong>([^<>\"]+)</strong></a>").getMatch(0);
                    String url = new Regex(html, "HREF=\\'([^<>\"\\']+)\\'").getMatch(0);
                    String videoid_singlevideo = new Regex(html, "playVideoId\\((\\d+)").getMatch(0);
                    if (videoid_singlevideo == null) {
                        videoid_singlevideo = new Regex(html, "ID=\\'video_(\\d+)\\'").getMatch(0);
                    }
                    if (videoid_singlevideo == null && counter == 1) {
                        /* Special case: Last chance to get videoid for first video (only needed for nicer filenames) */
                        videoid_singlevideo = videoid_first_video;
                    }
                    if (url == null) {
                        /* Skip invalid content! */
                        continue;
                    }
                    if (!url.startsWith("http")) {
                        url = "https://www.video2brain.com/" + url_language + "/" + url;
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
                    String filename = "video2brain";
                    if (add_position) {
                        filename += "_" + jd.plugins.hoster.Video2brainCom.getFormattedVideoPositionNumber(counter);
                    }
                    if (productid != null && videoid_singlevideo != null) {
                        filename += "_" + productid + "_" + videoid_singlevideo;
                    }
                    filename += "_" + url_language;
                    /* Lets try to make the filenames as similar-looking as the final filenames as we can. */
                    if (html.contains("Video.playVideoId")) {
                        filename += "_tutorial";
                    } else {
                        filename += "_paid_content";
                    }
                    filename += "_" + title + ".mp4";
                    filename = encodeUnicode(filename);
                    final DownloadLink dl = this.createDownloadlink(url);
                    dl.setName(filename);
                    dl.setAvailable(true);
                    dl.setProperty("order_id", counter);
                    decryptedLinks.add(dl);
                    counter++;
                }
            }
        }
        /* If everything else fails we can at least add the video of the current page as there usually is one :) */
        if (decryptedLinks.size() == 0 && videoid_first_video != null) {
            decryptedLinks.add(this.createDownloadlink(this.createOldDownloadURL(url_language, videoid_first_video)));
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String createOldDownloadURL(final String url_language, final String videoID) {
        final String url = "https://www.video2brain.com/" + url_language + "/videos-" + videoID + ".htm";
        return url;
    }
}
