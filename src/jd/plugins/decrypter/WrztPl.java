//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuta.pl" }, urls = { "http://[\\w\\.]*?wrzuta\\.pl/katalog/\\w+.+" }, flags = { 0 })
public class WrztPl extends PluginForDecrypt {

    public WrztPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setCookiesExclusive(true);
        br.getPage(parameter.getCryptedUrl());
        if (br.containsHTML("Nie odnaleziono użytkownika")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String user = br.getRegex("<div id=\"user_info_data_login\"><a href=\"http://(.*?)\\.wrzuta\\.pl/\"").getMatch(0);
        if (user == null) user = br.getRegex("title=\"RSS 2\\.0\" href=\"http://(.*?)\\.wrzuta\\.pl/").getMatch(0);
        // String id = new Regex(parameter.getCryptedUrl(),
        // "katalog/(\\w+)/?").getMatch(0);
        // if (id == null) return null;
        // id = id.trim();
        int counter = 0;
        String links[] = br.getRegex("<div class=\"title\"><a href=\"(.*?)\">.*?</a>").getColumn(0);
        for (String link : links) {
            if (new Regex(link, "http://[\\w\\.]*?wrzuta\\.pl/(audio|film|obraz)/\\w+.+").matches()) {
                DownloadLink dllink = this.createDownloadlink(link);
                dllink.setProperty("nameextra", counter);
                counter++;
                decryptedLinks.add(dllink);
            }
        }
        if (user != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(user.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
