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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tumblr.com" }, urls = { "http://(www\\.)?(tumblr\\.com/audio_file/\\d+/tumblr_[A-Za-z0-9]+|(?!\\d+\\.media\\.tumblr\\.com/.+)[\\w\\.\\-]*?\\.tumblr\\.com(/image/\\d+|/post/\\d+|/page/\\d+)?)" }, flags = { 0 })
public class TumblrComDecrypter extends PluginForDecrypt {

    public TumblrComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("www.", "");
        if (parameter.matches("http://tumblr\\.com/audio_file/\\d+/tumblr_[A-Za-z0-9]+")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.matches("http://[\\w\\.\\-]*?\\.tumblr\\.com/post/\\d+")) {
            // Single posts
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(parameter);
                if (con.getResponseCode() == 404) {
                    logger.info("Link offline (error 404): " + parameter);
                    return decryptedLinks;
                }
                br.followConnection();
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
            String fpName = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
            if (fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName.trim());

            String externID = br.getRegex("(http://(www\\.)?gasxxx\\.com/media/player/config_embed\\.php\\?vkey=\\d+)\"").getMatch(0);
            if (externID != null) {
                br.getPage(externID);
                externID = br.getRegex("<src>(http://[^<>\"]*?\\.flv)</src>").getMatch(0);
                if (externID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(fpName + ".flv");
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("\"(http://video\\.vulture\\.com/video/[^<>\"]*?)\"").getMatch(0);
            if (externID != null) {
                br.getPage(Encoding.htmlDecode(externID));
                String cid = br.getRegex("\\&media_type=video\\&content=([A-Z0-9]+)\\&").getMatch(0);
                if (cid == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                br.getPage("http://video.vulture.com/item/player_embed.js/" + cid);
                externID = br.getRegex("(http://videos\\.cache\\.magnify\\.net/[^<>\"]*?)\\'").getMatch(0);
                if (externID == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setFinalFileName(fpName + externID.substring(externID.lastIndexOf(".")));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("\"(http://(www\\.)?facebook\\.com/v/\\d+)\"").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink(externID.replace("/v/", "/video/video.php?v="));
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            externID = br.getRegex("name=\"twitter:player\" content=\"(https?://(www\\.)?youtube\\.com/v/[A-Za-z0-9\\-_]+)\\&").getMatch(0);
            if (externID != null) {
                final DownloadLink dl = createDownloadlink(externID);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }
            // Try to find the biggest picture
            for (int i = 1000; i >= 10; i--) {
                externID = br.getRegex("\"(http://\\d+\\.media\\.tumblr\\.com/tumblr_[a-z0-9]+_" + i + "\\.jpg)\"").getMatch(0);
                if (externID != null) break;
            }
            if (externID != null) {
                final DownloadLink dl = createDownloadlink("directhttp://" + externID);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
                return decryptedLinks;
            }

            if (externID == null) {
                logger.info("Found nothing here so the decrypter is either broken or there isn't anything to decrypt. Link: " + parameter);
                return decryptedLinks;
            }

        } else if (parameter.matches("http://(www\\.)?[\\w\\.\\-]*?\\.tumblr\\.com/image/\\d+")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            final String finallink = br.getRegex("class=\"fit_to_screen\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        } else {
            // Users
            String nextPage = "1";
            int counter = 1;
            boolean decryptSingle = parameter.matches("http://(www\\.)?[\\w\\.\\-]*?\\.tumblr\\.com/page/\\d+");
            br.getPage(parameter);
            if (br.containsHTML(">Not found\\.<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            while (nextPage != null) {
                logger.info("Decrypting page " + counter);
                if (!nextPage.equals("1")) br.getPage(parameter + nextPage);
                final String[] allPosts = br.getRegex("\"(http://(www\\.)?[\\w\\.\\-]*?\\.tumblr\\.com/post/\\d+)").getColumn(0);
                if (allPosts == null || allPosts.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String post : allPosts) {
                    final DownloadLink fpost = createDownloadlink(post);
                    fpost.setProperty("nopackagename", true);
                    decryptedLinks.add(fpost);
                }
                if (decryptSingle) break;
                nextPage = br.getRegex("\"(/page/" + (counter + 1) + ")\"").getMatch(0);
                counter++;
            }
        }
        return decryptedLinks;
    }
}
