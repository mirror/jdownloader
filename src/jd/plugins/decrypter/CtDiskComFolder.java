//  jDownloader - Downloadmanager
//  Copyright (C) 2012  JD-Team support@jdownloader.org
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ctdisk.com" }, urls = { "https?://(www\\.)?(ctdisk|400gb|pipipan|t00y)\\.com/u/\\d{6,7}(/\\d{6,7})?" }, flags = { 0 })
public class CtDiskComFolder extends PluginForDecrypt {

    // DEV NOTES
    // protocol: no https.
    // t00y doesn't seem to work as alais but ill addd it anyway.

    private static final String domains = "(ctdisk|400gb|pipipan|t00y)\\.com";

    public CtDiskComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("https://", "http://").replaceAll(domains + "/", "400gb.com/");
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        String id = new Regex(parameter, domains + "/u/(\\d+)").getMatch(1);
        if (br.containsHTML("(Due to the limitaion of local laws, this url has been disabled\\!<|该用户还未打开完全共享\\。|您目前无法访问他的资源列表\\。)")) {
            logger.warning("Invalid URL: " + parameter);
            return null;
        }

        // Set package name and prevent null from creating 100s of packages
        String fpName = br.getRegex("</a> / (.*?)</h2>").getMatch(0);
        if (fpName == null) fpName = br.getRegex(">当前位置：(.*?) /").getMatch(0);
        if (fpName == null) fpName = "Untitled";

        parsePage(decryptedLinks, parameter, id);

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String parameter, String id) throws IOException {
        String results[][] = br.getRegex("(<tr >.*?(https?://(www\\.)?" + domains + "/file/\\d+)[^>]+>(.*?)<.*?>(\\d+(\\.\\d+)? ?(KB|MB|GB))<.*?</tr>)").getMatches();
        if (results == null || results.length == 0) {
            logger.warning("Can not find 'results' : " + parameter);
            return;
        }
        for (String[] args : results) {
            DownloadLink dl = createDownloadlink(args[1]);
            if (args[4] != null) {
                dl.setName(args[4]);
                if (args[5] != null) dl.setDownloadSize(SizeFormatter.getSize(args[5]));
                dl.setAvailable(true);
            }
            ret.add(dl);
        }
        // export folders back into decrypter again.
        String[] folders = new Regex(results, "<a href=\"(/u/" + id + "/\\d+)\">").getColumn(0);
        if (folders != null && folders.length != 0) {
            for (String folder : folders) {
                ret.add(createDownloadlink(new Regex(parameter, "(https?://(www\\.)?" + domains + ")").getMatch(0) + folder));
            }
        }
        String nextPage = br.getRegex("<a href=\"(/u/" + id + "/\\d+(/\\d+)?)\" class=\"p_redirect\">\\&#8250;</a>").getMatch(0);
        if (nextPage != null) {
            br.getPage(nextPage);
            parsePage(ret, parameter, id);
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}