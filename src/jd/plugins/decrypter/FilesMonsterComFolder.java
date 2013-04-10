//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesmonster.comFolder" }, urls = { "https?://(www\\.)?filesmonster\\.com/folders\\.php\\?fid=([0-9a-zA-Z_-]{22}|\\d+)" }, flags = { 0 })
public class FilesMonsterComFolder extends PluginForDecrypt {

    // DEV NOTES:
    // packagename is useless, as Filesmonster decrypter creates its own..
    // most simple method to

    private String protocol = null;
    private String uid      = null;

    public FilesMonsterComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String parameter = param.toString();
        protocol = new Regex(parameter, "(https?)://").getMatch(0);
        uid = new Regex(parameter, "\\?fid=([0-9a-zA-Z_-]{22}|\\d+)").getMatch(0);
        if (protocol == null || uid == null) {
            logger.warning("Could not find dependancy information. " + parameter);
            return null;
        }

        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.containsHTML(">Folder does not exist<")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }

        // base/first page, count always starts at zero!
        parsePage(decryptedLinks, parameter, 0);

        return decryptedLinks;
    }

    /**
     * find all download and folder links, and returns ret;
     * */
    private void parsePage(ArrayList<DownloadLink> ret, String parameter, int s) throws IOException {
        // the 's' increment per page is 50, find the first link with the same uid and s+50 each page!
        s = s + 50;

        String lastPage = br.getRegex("<a href='(/?folders\\.php\\?fid=" + uid + "[^']+)'>Last Page</a>").getMatch(0);
        if (lastPage == null) {
            // not really needed by hey why not, incase they change html
            lastPage = br.getRegex("<a href='(/?folders\\.php\\?fid=" + uid + "&s=" + s + ")").getMatch(0);
        }

        String[] links = br.getRegex("<a class=\"green\" href=\"(https?://[\\w\\.\\d]*?filesmonster\\.com/(download|folders)\\.php.*?)\">").getColumn(0);

        if (links == null || links.length == 0) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                // prevent regex from finding itself, this is incase they change layout and creates infinite loop.
                if (!dl.contains("fid=" + uid)) ret.add(createDownloadlink(dl.replaceFirst("https?", protocol)));
        }

        if (lastPage != null && !br.getURL().endsWith(lastPage)) {
            br.getPage(parameter + "&s=" + s);
            parsePage(ret, parameter, s);
        } else {
            // can't find last page, but non spanning pages don't have a last page! So something we shouldn't be concerned about.
            logger.info("Success in processing " + parameter);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}