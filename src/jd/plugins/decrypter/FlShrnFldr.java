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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fileshare.in.ua folder" }, urls = { "http://[\\w\\.]*?fileshare\\.in\\.ua/f.+" }, flags = { 0 })
public class FlShrnFldr extends PluginForDecrypt {

    public FlShrnFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String[][] otherPages = null;
        br.getPage(parameter);
        if (parameter.contains("folder.aspx?id=")) {
            if (!br.containsHTML("Список файлов")) {
                // Folderview link workaround
                String newUrl = br.getRegex(Pattern.compile("blank\" href=\"(http://fileshare\\.in\\.ua/f[0-9]+)\">http:", Pattern.CASE_INSENSITIVE)).getMatch(0);
                br.getPage(newUrl);
            }
        } else {
            // check if there are more pages in this folder
            otherPages = br.getRegex("pager\" href=\"(folder\\.aspx\\?id=[0-9]+&page=[0-9]+)\">").getMatches();
            // else => only if link is no pagelink, if pagelink only decrypt
            // links on this page
        }
        int pageCnt = 0;
        do {
            // get the links from current site
            String[][] linkIDs = br.getRegex(Pattern.compile("<div class=\"list_link\">.*?href=\"/([f0-9]+)\">", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatches();
            for (int linkCnt = 0; linkCnt < linkIDs.length; linkCnt++) {
                decryptedLinks.add(createDownloadlink("http://fileshare.in.ua/" + linkIDs[linkCnt][0]));
            }
            // if there are more pages load them and get their links
            if (otherPages != null && pageCnt < otherPages.length) {
                br.getPage("http://fileshare.in.ua/" + otherPages[pageCnt][0]);
            }
            pageCnt++;
        } while (otherPages != null && pageCnt <= otherPages.length); // get all
        // sites'
        // links
        return decryptedLinks;
    }
}
