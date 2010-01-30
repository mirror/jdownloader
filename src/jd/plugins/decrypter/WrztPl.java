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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wrzuta.pl" }, urls = { "http://[\\w\\.]*?wrzuta\\.pl/katalog/\\w+.+" }, flags = { 0 })
public class WrztPl extends PluginForDecrypt {

    public WrztPl(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setCookiesExclusive(true);
        br.getPage(parameter.getCryptedUrl());
        String user = br.getRegex("<h3><strong>U.ytkownik</strong>/(.*?)</h3>").getMatch(0);
        if (user == null) return null;
        user = user.trim();
        FilePackage fp = FilePackage.getInstance();
        fp.setName(user);
        String id = new Regex(parameter.getCryptedUrl(), "katalog/(\\w+)/?").getMatch(0);
        if (id == null) return null;
        id = id.trim();
        int counter = 0;
        int page = 1;
        while (true) {
            br.getPage("http://" + user + ".pl.wrzuta.pl/dir.php?key=" + id + "&page=" + page + "#");
            page++;
            String links[] = br.getRegex("<div class=\"title\"><a href=\"(.*?)\">.*?</a>").getColumn(0);
            if (links == null || links.length == 0) break;
            for (String link : links) {
                if (new Regex(link, "http://[\\w\\.]*?wrzuta\\.pl/(audio|film|obraz)/\\w+.+").matches()) {
                    DownloadLink dllink = this.createDownloadlink(link);
                    dllink.setProperty("nameextra", counter);
                    fp.add(dllink);
                    counter++;
                    decryptedLinks.add(dllink);
                }
            }
        }

        return decryptedLinks;
    }

}
