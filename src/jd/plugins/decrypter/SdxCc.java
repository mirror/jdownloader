//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Arrays;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sdx.cc" }, urls = { "http://(www\\.)?sdx\\.cc/downloads_detail\\.php\\?download_id=\\d+" }, flags = { 0 })
public class SdxCc extends PluginForDecrypt {

    public SdxCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = cryptedLink.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getURL().equals("http://www.sdx.cc/news.php")) {
            logger.info("Link broken/offline: " + parameter);
            return decryptedLinks;
        }
        String pw = br.getRegex("<tr>[\t\n\r ]+<td align=\"center\" valign=\"top\" width=\"20%\">([^<>\"]*?)</td>").getMatch(0);
        pw = pw != null ? pw.trim() : "sdx.cc";
        String[] links = br.getRegex("\\'(http://(www\\.)?relink\\.us/(f/|view\\.php\\?id=)[a-z0-9]+)\\'").getColumn(0);
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(link));
        }
        ArrayList<String> pwList = new ArrayList<String>(Arrays.asList(new String[] { pw }));
        for (DownloadLink dlLink : decryptedLinks) {
            dlLink.setSourcePluginPasswordList(pwList);
        }
        return decryptedLinks.size() > 0 ? decryptedLinks : null;
    }

}
