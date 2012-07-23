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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "justin.tv" }, urls = { "http://(www\\.)?(justin\\.tv|(de\\.)?(twitchtv\\.com|twitch\\.tv))/[^<>/\"]+/((b|c)/\\d+|videos(\\?page=\\d+)?)" }, flags = { 0 })
public class JustinTvDecrypt extends PluginForDecrypt {

    public JustinTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String HOSTURL = "http://www.justindecrypted.tv";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // twitchtv belongs to justin.tv
        br.setCookie("http://justin.tv", "language", "en");
        br.setCookie("http://justin.tv", "mature_allowed", "true");
        String parameter = param.toString().replaceAll("(de\\.)?(twitchtv\\.com|twitch\\.tv)", "justin.tv");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (parameter.contains("/videos")) {
            if (br.containsHTML("<strong id=\"videos_count\">0")) {
                logger.info("Nothing to decrypt here: " + parameter);
                return decryptedLinks;
            }
            String[] links = br.getRegex("<p class=\"title\"><a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("<div class=\"left\">[\t\n\r ]+<a href=\"(/.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(HOSTURL + dl));
        } else {
            if (br.getURL().contains("/videos")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"/>").getMatch(0);
            if (filename == null) filename = br.getRegex("<h2 class=\"clip_title\">(.*?)</h2>").getMatch(0);
            br.getPage("http://api.justin.tv/api/broadcast/by_archive/" + new Regex(parameter, "(\\d+)$").getMatch(0) + ".xml");
            String[] links = br.getRegex("<video_file_url>(http://[^<>\"]*?)</video_file_url>").getColumn(0);
            int counter = 1;
            if (links == null || links.length == 0 || filename == null) {
                logger.warning("Decrypter broken: " + parameter);
                return null;
            }
            filename = Encoding.htmlDecode(filename.trim());
            for (String dl : links) {
                DownloadLink dlink = createDownloadlink(dl.replace("twitch.tv/", "twitchdecrypted.tv/").replace("justin.tv/", "justindecrypted.tv/"));
                dlink.setProperty("directlink", "true");
                if (links.length != 1) {
                    dlink.setFinalFileName(filename + " - Part " + counter + ".flv");
                } else {
                    dlink.setFinalFileName(filename + ".flv");
                }
                decryptedLinks.add(dlink);
                counter++;
            }
            FilePackage fp = FilePackage.getInstance();
            fp.setName(filename);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
