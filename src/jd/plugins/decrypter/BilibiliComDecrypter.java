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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bilibili.com" }, urls = { "https?://(?:www\\.)?bilibili\\.com/(?:mobile/)?video/av\\d+/|https?://static\\.hdslb\\.com/miniloader\\.swf\\?aid=\\d+" })
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
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
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
                /* Offline link - empty tables which do not contain any downloadurls. */
                if (!this.br.containsHTML("class=\\'Data_Data\\'><a href=")) {
                    decryptedLinks.add(this.createOfflinelink(parameter));
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleID : links) {
                this.br.setFollowRedirects(true);
                String continue_url = "http://www.bilibilijj.com/FreeDown/" + singleID + ".php";
                this.br.getPage(continue_url);
                continue_url = this.br.getRegex("Base64\\.encodeURI\\(\"(https?://[^<>\"]+)\"").getMatch(0);
                if (continue_url == null) {
                    return null;
                }
                this.br.setFollowRedirects(false);
                this.br.getPage(continue_url);
                /* Usually finallink is a ctdisk.com downloadurl. */
                final String finallink = this.br.getRedirectLocation();
                if (finallink == null) {
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
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
