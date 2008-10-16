//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

public class RaidrushOrg extends PluginForDecrypt {

    public RaidrushOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String title = br.getRegex("<big><strong>(.*?)</strong></big>").getMatch(0);
        String pass = br.getRegex("<strong>Passwort\\:</strong> <small>(.*?)</small>").getMatch(0);
        FilePackage fp = new FilePackage();
        fp.setName(title);
        fp.setPassword(pass);

        String[][] matches = br.getRegex("ddl\\(\\'(.*?)\\'\\,\\'([\\d]*?)\\'\\)").getMatches();
        progress.setRange(matches.length);
        for (String[] match : matches) {
            br.getPage("http://raidrush.org/ext/exdl.php?go=" + match[0] + "&fid=" + match[1]);
            String link = br.getRegex("unescape\\(\"(.*?)\"\\)").getMatch(0);
            link = new Regex(Encoding.htmlDecode(link), "\"0\"><frame src\\=\"(.*?)\" name\\=\"GO_SAVE\"").getMatch(0);
            DownloadLink dl = createDownloadlink(link);
            dl.addSourcePluginPassword(pass);
            dl.setFilePackage(fp);
            decryptedLinks.add(dl);
            progress.increase(1);
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}