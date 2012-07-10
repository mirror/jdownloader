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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mystere-tv.com" }, urls = { "http://(www\\.)?mystere\\-tv\\.com/.*?\\-v\\d+\\.html" }, flags = { 0 })
public class MystereTvComDecrypter extends PluginForDecrypt {

    public MystereTvComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This decrypter finds out of there are embedded videos and creates the
    // links, if not it passes the mystere-tv.com link to the hosterplugin
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://www.mystere-tv.com/") || br.containsHTML("<title>Paranormal \\- Ovni \\- Mystere TV </title>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        // Same as in hosterplugin
        String filename = br.getRegex("<h1 class=\"videoTitle\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<br /><br /><strong><u>(.*?)</u></strong>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- Paranormal</title>").getMatch(0);
            }
        }
        if (filename == null) return null;
        filename = Encoding.htmlDecode(filename.trim());
        String externalLink = br.getRegex("\"(http://(www\\.)dailymotion\\.com/embed/video/.*?)\"").getMatch(0);
        if (externalLink != null) {
            decryptedLinks.add(createDownloadlink(externalLink.replace("/embed", "")));
            return decryptedLinks;
        }
        externalLink = br.getRegex("tagtele\\.com/v/(\\d+)\"").getMatch(0);
        if (externalLink != null) {
            br.getPage("http://www.tagtele.com/videos/playlist/" + externalLink);
            String finallink = br.getRegex("<location>(http://.*?)</location>").getMatch(0);
            if (finallink == null) return null;
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
        externalLink = br.getRegex("name=\"movie\" value=\"(http://(www\\.)youtube\\.com/v/.*?)\\&").getMatch(0);
        if (externalLink == null) externalLink = br.getRegex("></param><embed src=\"(http://(www\\.)?youtube\\.com/v/.*?)\\&").getMatch(0);
        if (externalLink != null) {
            decryptedLinks.add(createDownloadlink(externalLink));
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(parameter.replace("mystere-tv.com/", "decryptedmystere-tv.com/")));
        return decryptedLinks;
    }
}
