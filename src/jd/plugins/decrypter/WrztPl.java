//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuta.pl" }, urls = { "http://[\\w\\.\\-]+?wrzuta\\.pl/katalog/[a-zA-Z0-9]{11}(/[\\w\\-\\.]+/\\d+)?" }, flags = { 0 })
public class WrztPl extends PluginForDecrypt {

    public WrztPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.getPage(parameter);
        String id = new Regex(parameter, "wrzuta\\.pl/katalog/([a-zA-Z0-9]{11})").getMatch(0);
        if (br.containsHTML(">Nie odnaleziono pliku\\.<")) {
            logger.warning("Invalid URL: " + parameter);
            return decryptedLinks;
        }

        // Set package name and prevent null field from creating plugin errors
        String fpName = br.getRegex("<div class=\"catalogue\\_name\">(.*?)</div>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>WRZUTA - (.*?) - katalog djszymonx</title>").getMatch(0);
        if (fpName == null) fpName = "Untitled";

        if (parameter.matches("http://[\\w\\.\\-]+?wrzuta\\.pl/katalog/[a-zA-Z0-9]{11}")) {
            parsePage(decryptedLinks, id);
            parseNextPage(decryptedLinks, id);
            fpName = fpName + " - All Pages";
        } else {
            parsePage(decryptedLinks, id);
            fpName = fpName + " - Page " + new Regex(parameter, "http://[\\w\\.\\-]+?wrzuta\\.pl/katalog/[a-zA-Z0-9]{11}/[\\w\\-\\.]+/(\\d+)").getMatch(0);
        }

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(ArrayList<DownloadLink> ret, String id) {
        String[] links = br.getRegex("<a id=\"file\\_list\\_file\\_thumb\\_href\\_[a-zA-Z0-9]{11}\" href=\"(http://[\\w\\.\\-]+?wrzuta\\.pl/(audio|film|obraz)/[a-zA-Z0-9]{11}/[\\w\\.\\-]+)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(http://[\\w\\.\\-]+?wrzuta\\.pl/(audio|film|obraz)/[a-zA-Z0-9]{11}/[\\w\\.\\-]+)\"").getColumn(0);
        if (links == null || links.length == 0) return;
        if (links != null && links.length != 0) {
            for (String dl : links)
                ret.add(createDownloadlink(dl));
        }
    }

    private boolean parseNextPage(ArrayList<DownloadLink> ret, String id) throws IOException {
        String nextPage = br.getRegex("<li[^>]+><a href=\"(http://[\\w\\.\\-]+?wrzuta\\.pl/katalog/" + id + "/[\\w\\.\\-]+/\\d+)\">Następna</a></li>").getMatch(0);
        nextPage = new Regex(nextPage, "(http://[\\w\\.\\-]+?wrzuta\\.pl/katalog/" + id + "/.*?/\\d+)").getMatch(0);
        if (nextPage != null) {
            br.getPage(nextPage);
            parsePage(ret, id);
            parseNextPage(ret, id);
            return true;
        }
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}