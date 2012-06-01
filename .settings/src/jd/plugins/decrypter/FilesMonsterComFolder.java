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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 15834 $", interfaceVersion = 2, names = { "filesmonster.comFolder" }, urls = { "http://(www\\.)?filesmonster\\.com/folders\\.php\\?fid=([0-9a-zA-Z_-]{22}|\\d+)" }, flags = { 0 })
public class FilesMonsterComFolder extends PluginForDecrypt {

    public FilesMonsterComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    // DEV NOTES:
    // packagename is useless, as Filesmonster decrypter creates its own..

    
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setReadTimeout(3 * 60 * 1000);
        br.setFollowRedirects(false);
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        if (br.containsHTML(">Folder does not exist<")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }

        parsePage(decryptedLinks);

        String firstpanel = br.getRegex("<(.*?)<table").getMatch(0);
        if (firstpanel == null) {
            logger.warning("FilesMonster Folder Decrypter: Page finding broken: " + parameter);
            logger.warning("FilesMonster Folder Decrypter: Please report to JDownloader Development Team.");
            logger.warning("FilesMonster Folder Decrypter: Continuing with the first page only.");
        }
        String[] Pages = new Regex(firstpanel, "\\&nbsp\\;<a href=\\'(folders.php\\?fid=.*?)\\'").getColumn(0);
        if (Pages == null || Pages.length == 0) return null;
        if (Pages != null && Pages.length != 0) {
            for (String page : Pages) {
                br.getPage("http://filesmonster.com/" + page);
                parsePage(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret) {
        String[] links = br.getRegex("<a class=\"green\" href=\"(http://[\\w\\.\\d]*?filesmonster\\.com/.*?)\">").getColumn(0);
        if (links == null || links.length == 0) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                ret.add(createDownloadlink(dl));
        }
    }
}
