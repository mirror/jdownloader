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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "raidrush.org" }, urls = { "http://(www\\.)?raidrush\\.org/ext/\\?fid\\=[\\w]+" }, flags = { 0 })
public class Rdrshrg extends PluginForDecrypt {

    public Rdrshrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        String title = br.getRegex("<big><strong>(.*?)</strong></big>").getMatch(0);
        String pass = br.getRegex("<strong>Passwort\\:</strong> <small>(.*?)</small>").getMatch(0);
        ArrayList<String> pwList = null;
        if (pass != null) {
            pwList = new ArrayList<String>(Arrays.asList(new String[] { pass.trim() }));
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(title);

        String[][] matches = br.getRegex("ddl\\(\\'(.*?)\\'\\,\\'([\\d]*?)\\'\\)").getMatches();
        progress.setRange(matches.length);
        for (String[] match : matches) {
            br.getPage("http://raidrush.org/ext/exdl.php?go=" + match[0] + "&fid=" + match[1]);
            String link = br.getRegex("unescape\\(\"(.*?)\"\\)").getMatch(0);
            link = new Regex(Encoding.htmlDecode(link), "\"0\"><frame src\\=\"(.*?)\" name\\=\"GO_SAVE\"").getMatch(0);
            DownloadLink dl = createDownloadlink(link);
            if (pwList != null) dl.setSourcePluginPasswordList(pwList);
            fp.add(dl);
            decryptedLinks.add(dl);
            progress.increase(1);
        }

        return decryptedLinks;
    }

}
