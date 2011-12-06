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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

/** This decrypter decrypts links for the filecondo.com hosterplugin */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filecondo.com" }, urls = { "http://(www\\.)?filecondo\\.com/(download_regular\\.php\\?file=|dl\\.php\\?f=)[A-Za-z0-9]+" }, flags = { 0 })
public class FileCondoComDecrypter extends PluginForDecrypt {

    public FileCondoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("dl.php?f=", "download_regular.php?file=");
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        if (br.containsHTML("à¹„à¸¡à¹ˆà¸žà¸šà¹„à¸Ÿà¸¥à¹Œ / Link à¸œà¸´à¸”") || br.toString().length() < 200) {
            DownloadLink dl = createDownloadlink(parameter.replace("filecondo.com/", "filecondodecrypted.com/"));
            dl.setProperty("mainlink", parameter);
            dl.setAvailable(false);
            return decryptedLinks;
        }
        String fpName = br.getRegex("href=\"/download_vip_check\\.php\\?f=[A-Za-z0-9]+\" target=\"_blank\">(.*?)</a></td").getMatch(0);
        String[][] links = br.getRegex("<a href=\\'(http://(www\\.)?filecondo\\.com/download_regular_active\\.php\\?file=[A-Za-z0-9]+\\&part=\\d+)\\'>DOWNLOAD ([^<>/\"]+)</a>").getMatches();
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink[] : links) {
            DownloadLink dl = createDownloadlink(singleLink[0].replace("filecondo.com/", "filecondodecrypted.com/"));
            dl.setName(Encoding.htmlDecode(singleLink[2]));
            dl.setProperty("mainlink", parameter);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
