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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bilibili.com" }, urls = { "https?://(?:www\\.)?bilibili\\.com/(?:mobile/)?video/av\\d+/?|https?://(?:www\\.)?bilibilijj\\.com/video/av\\d+/|https?://static\\.hdslb\\.com/miniloader\\.swf\\?aid=\\d+" })
public class BilibiliComDecrypter extends PluginForDecrypt {
    public BilibiliComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        jd.plugins.hoster.BilibiliCom.prepBR(this.br);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String vid = getFID(parameter);
        final String url_video_main = "http://www.bilibili.com/video/av" + vid + "/";
        final String url_download_overview = "http://www.bilibilijj.com/video/av" + vid + "/";
        this.br.getPage(url_video_main);
        if (jd.plugins.hoster.BilibiliCom.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        /* Find packagename */
        String fpName = jd.plugins.hoster.BilibiliCom.getTitle(this.br);
        if (fpName == null) {
            fpName = vid;
        }
        /* Find video-parts */
        String[] links = br.getRegex("<option value=\\'(/video/av" + vid + "/index_\\d+\\.html)\\'>").getColumn(0);
        if (links == null || links.length == 0) {
            /* Probably a video with only one part */
            links = new String[1];
            links[0] = "/video/av" + vid + "/index_1.html";
        }
        for (String singleURL : links) {
            final String contenturl = "http://www.bilibili.com" + singleURL;
            singleURL = "http://www.bilibilidecrypted.com" + singleURL;
            final DownloadLink dl = createDownloadlink(singleURL);
            dl.setContentUrl(contenturl);
            decryptedLinks.add(dl);
        }
        try {
            /* Now let's decrypt the (ctdisk.com) downloadurls. */
            br.getPage(url_download_overview);
            if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 502) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            links = br.getRegex("/Files/DownLoad/(\\d+)\\.mp4").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("/DownLoad/Cid/(\\d+)\\'").getColumn(0);
            }
            if (links == null || links.length == 0) {
                /*
                 * Return links because maybe there simply are no downloadlinks available. From the code above we should at least have one
                 * stream-url which the user can download!
                 */
                logger.warning("Downloadlink-Decrypter might be broken for link: " + parameter);
                return decryptedLinks;
            }
            for (final String singleID : links) {
                this.br.setFollowRedirects(true);
                String continue_url = "http://www.bilibilijj.com/DownLoad/Cid/" + singleID;
                this.br.getPage(continue_url);
                final String html_with_multiple_downloadurls = this.br.getRegex("<div class=\"D\">(.*?)</div>").getMatch(0);
                if (html_with_multiple_downloadurls != null) {
                    final String[] dlurls = new Regex(html_with_multiple_downloadurls, "<a href=\\'(http[^<>\"\\']+)\\' target=\\'_blank\\'>").getColumn(0);
                    for (final String dlurl : dlurls) {
                        decryptedLinks.add(createDownloadlink("directhttp://" + dlurl));
                    }
                } else {
                    continue_url = this.br.getRegex("Base64\\.encodeURI\\(\"(https?://[^<>\"]+)\"").getMatch(0);
                    if (continue_url == null) {
                        /*
                         * Return links because maybe there simply are no downloadlinks available. From the code above we should at least
                         * have one stream-url which the user can download!
                         */
                        logger.warning("Downloadlink-Decrypter might be broken for link: " + parameter);
                        return decryptedLinks;
                    }
                    String finallink = null;
                    if (continue_url.contains("http://www.bilibilijj.comhttp")) {
                        finallink = continue_url.replace("http://www.bilibilijj.comhttp://", "http");
                    } else {
                        this.br.setFollowRedirects(false);
                        this.br.getPage(continue_url);
                        /* Usually finallink is a ctdisk.com downloadurl. */
                        finallink = this.br.getRedirectLocation();
                        if (finallink == null) {
                            /*
                             * Return links because maybe there simply are no downloadlinks available. From the code above we should at
                             * least have one stream-url which the user can download!
                             */
                            logger.warning("Downloadlink-Decrypter might be broken for link: " + parameter);
                            return decryptedLinks;
                        }
                    }
                    /*
                     * Make sure directlinks with endings and parameters get added correctly. TODO: Maybe find a better way o identify
                     * directlinks!
                     */
                    if ((finallink.contains(".mp4") || finallink.contains(".flv")) && finallink.contains("?")) {
                        finallink = "directhttp://" + finallink;
                    }
                    decryptedLinks.add(createDownloadlink(finallink));
                }
            }
        } catch (final Throwable e) {
            logger.warning("Failed to grab downloadurls");
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getFID(final String url_source) {
        String fid = new Regex(url_source, "/av(\\d+)").getMatch(0);
        if (fid == null) {
            fid = new Regex(url_source, "/av(\\d+)").getMatch(0);
        }
        return new Regex(url_source, "/av(\\d+)").getMatch(0);
    }
}
