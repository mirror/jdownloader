//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "apple.com" }, urls = { "http://[\\w\\.]*?apple\\.com/trailers/[a-zA-Z0-9_/]+/" }, flags = { 0 })
public class AppleTrailer extends PluginForDecrypt {

    public AppleTrailer(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.getPage(parameter.toString());

        String title = br.getRegex("var trailerTitle = '(.*?)';").getMatch(0);
        if (title == null) title = br.getRegex("name=\"omni_page\" content=\"(.*?)\"").getMatch(0);
        Browser br2 = br.cloneBrowser();

        br2.getPage(parameter.toString() + "includes/playlists/web.inc");
        if (title == null) title = br2.getRegex("var trailerTitle = '(.*?)';").getMatch(0);
        if (title == null) {
            logger.warning("Plugin defect, could not find 'title' : " + parameter.toString());
            return null;
        }
        String[] hits = br2.getRegex("(<li class='trailer ([a-z]+)?'>.*?</li><)").getColumn(0);
        if (hits == null || hits.length == 0) {
            logger.warning("Plugin defect, could not find 'hits' : " + parameter.toString());
            return null;
        }
        if (hits.length == 1) {
            hits = new String[] { br2.toString() };
        }
        for (String hit : hits) {
            String hitname = new Regex(hit, "<h3>(.*?)</h3>").getMatch(0);
            if (hitname == null) {
                logger.warning("Plugin defect, could not find 'hitname' : " + parameter.toString());
                return null;
            }
            String filename = title + " - " + hitname;
            String[] vids = new Regex(hit, "<li class=\"hd\">(.*?)</li>").getColumn(0);
            if (vids == null || vids.length == 0) {
                logger.warning("Plugin defect, could not find 'vids' : " + parameter.toString());
                return null;
            }
            for (String vid : vids) {
                String[][] matches = new Regex(vid, "href=\"([^\"]+)#[^>]+>(.*?)</a>").getMatches();
                if (matches == null || matches.length == 0) {
                    logger.warning("Plugin defect, could not find 'matches' : " + parameter.toString());
                    return null;
                }
                for (String[] match : matches) {
                    String url = match[0];
                    String video_name = filename + " (" + match[1].replaceAll("</?span>", "_") + ")";
                    br2 = br.cloneBrowser();
                    url = url.replace("includes/", "includes/" + hitname.toLowerCase().replace(" ", "") + "/");
                    br2.getPage(url);
                    url = br2.getRegex("href=\"([^\\?\"]+).*?\">Click to Play</a>").getMatch(0);
                    if (url == null) {
                        logger.warning("Plugin defect, could not find 'url' : " + parameter.toString());
                        return null;
                    }
                    url = url.replace("/trailers.apple.com/", "/trailers.appledecrypted.com/");
                    String extension = url.substring(url.lastIndexOf("."));
                    DownloadLink dlLink = createDownloadlink(url);
                    dlLink.setFinalFileName(video_name + extension);
                    dlLink.setAvailable(true);
                    dlLink.setProperty("Referer", parameter.toString());
                    decryptedLinks.add(dlLink);
                }
            }
        }
        if (title != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(title.trim());
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}