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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mystere-tv.com" }, urls = { "http://(www\\.)?mystere\\-tv\\.com/.*?\\-v\\d+\\.html" })
public class MystereTvComDecrypter extends PluginForDecrypt {
    public MystereTvComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    /* Porn_plugin */

    // This decrypter finds out of there are embedded videos and creates the
    // links, if not it passes the mystere-tv.com link to the hosterplugin
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (jd.plugins.hoster.MystereTvCom.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        } else if (isPremiumonly(this.br)) {
            /* Premiumonly */
            decryptedLinks.add(createDownloadlink(parameter.replace("mystere-tv.com/", "decryptedmystere-tv.com/")));
            return decryptedLinks;
        }
        // Same as in hosterplugin
        final String filename = jd.plugins.hoster.MystereTvCom.getFilename(this.br, parameter);
        final String[] externalURLs = br.getRegex("<iframe[^<>]*?(dailymotion\\.com/embed/video/[^\"]+)\"").getColumn(0);
        if (externalURLs.length > 0) {
            for (String externalURL : externalURLs) {
                externalURL = "https://www." + externalURL;
                decryptedLinks.add(createDownloadlink(externalURL));
            }
            return decryptedLinks;
        }
        String externalLink = br.getRegex("tagtele\\.com/v/(\\d+)\"").getMatch(0);
        if (externalLink != null) {
            br.getPage("http://www.tagtele.com/videos/playlist/" + externalLink);
            String finallink = br.getRegex("<location>(http://.*?)</location>").getMatch(0);
            if (finallink == null) {
                return null;
            }
            DownloadLink dl = createDownloadlink("directhttp://" + Encoding.htmlDecode(finallink.trim()));
            dl.setFinalFileName(filename + ".flv");
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externalLink = br.getRegex("googleplayer\\.swf\\?docid=(\\-?\\d+)\\&").getMatch(0);
        if (externalLink != null) {
            decryptedLinks.add(createDownloadlink("http://video.google.com/videoplay?docid=" + externalLink));
            return decryptedLinks;
        }
        externalLink = br.getRegex("name=\"movie\" value=\"https?://(?:www\\.)youtube\\.com/v/(.*?)\\&").getMatch(0);
        if (externalLink == null) {
            externalLink = br.getRegex("></param><embed src=\"https?://(?:www\\.)?youtube\\.com/v/(.*?)\\&").getMatch(0);
        }
        if (externalLink == null) {
            externalLink = br.getRegex("\"https?://(?:www\\.)?youtube\\.com/embed/([^<>\"]*?)\"").getMatch(0);
        }
        if (externalLink != null) {
            decryptedLinks.add(createDownloadlink("https://www.youtube.com/watch?v=" + externalLink));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("mystere-tv.com/", "decryptedmystere-tv.com/")));
        return decryptedLinks;
    }

    public static boolean isPremiumonly(final Browser br) {
        return br.containsHTML("class=\"playerwrapper\">[\t\n\r ]+<a href=\"inscription\\.html\"");
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}