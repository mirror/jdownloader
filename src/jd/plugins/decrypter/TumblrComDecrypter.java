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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tumblr.com" }, urls = { "http://(www\\.)?(tumblr\\.com/audio_file/\\d+/tumblr_[A-Za-z0-9]+|[\\w\\.\\-]*?\\.tumblr\\.com(/post/\\d+|/page/\\d+)?)" }, flags = { 0 })
public class TumblrComDecrypter extends PluginForDecrypt {

    public TumblrComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches("http://(www\\.)?tumblr\\.com/audio_file/\\d+/tumblr_[A-Za-z0-9]+")) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (parameter.matches("http://(www\\.)?[\\w\\.\\-]*?\\.tumblr\\.com/post/\\d+")) {
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
            final String fpName = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);

            final String[][] regexes = { { "<meta (property=\"og:image\"|name=\"twitter:image\") content=\"(http://[^<>\"]*?)\"", "1" }, { "<p><img src=\"(http://(www\\.)?media\\.tumblr\\.com/[^<>\"]*?)\"", "0" }, { "src=\\\\x22(http://[\\w\\.\\-]*?\\.tumblr\\.com/video_file/\\d+/tumblr_[a-z0-9]+)\\\\x22", "0" } };
            for (String[] regex : regexes) {
                final String[] links = br.getRegex(Pattern.compile(regex[0], Pattern.CASE_INSENSITIVE)).getColumn(Integer.parseInt(regex[1]));
                if (links != null && links.length > 0) {
                    for (final String piclink : links) {
                        final DownloadLink dl = createDownloadlink("directhttp://" + piclink);
                        try {
                            distribute(dl);
                        } catch (final Exception e) {
                            // Not available in 0.851 Stable
                        }
                        decryptedLinks.add(dl);
                    }
                }
            }

            if (decryptedLinks == null || decryptedLinks.size() == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }

            if (fpName != null && !param.getBooleanProperty("nopackagename")) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                // Make only one package with same packagenames
                fp.setProperty("ALLOW_MERGE", true);
                fp.addLinks(decryptedLinks);
            }
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
