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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mediafire.com" }, urls = { "http://[\\w\\.]*?(?!download)[\\w\\.]*?mediafire\\.com/(\\?sharekey=.+|(?!download|file)[^\\?]+)" }, flags = { 0 })
public class MdfrFldr extends PluginForDecrypt {

    public MdfrFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        if (br.containsHTML("The page cannot be found")) return decryptedLinks;
        Thread.sleep(500);
        String reqlink = br.getRegex(Pattern.compile("LoadJS\\(\".*?/js/myfiles\\.php/(.*?)\"")).getMatch(0);
        if (reqlink == null) { return null; }
        br.getPage("http://www.mediafire.com/js/myfiles.php/" + reqlink);
        String links[][] = br.getRegex(Pattern.compile("[a-z]{2}\\[\\d+\\]=Array\\('\\d+','\\d+',\\d+,'([a-z0-9]*?)','[a-f0-9]*?','(.*?)','(\\d+)'", Pattern.CASE_INSENSITIVE)).getMatches();
        progress.setRange(links.length);

        for (String[] element : links) {
            if (!element[2].equalsIgnoreCase("0")) {
                DownloadLink link = createDownloadlink("http://www.mediafire.com/download.php?" + element[0]);
                link.setName(element[1]);
                link.setDownloadSize(Long.parseLong(element[2]));
                link.setProperty("origin", "decrypter");
                decryptedLinks.add(link);
            }
            progress.increase(1);
        }

        return decryptedLinks;
    }

}
