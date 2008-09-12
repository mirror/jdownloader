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

/* Der Decrypter funktioniert nicht, weil JD einen bereits gefilterten Parameter an das Plugin übergibt.
 * Doperoms aber verwendet per Zufall unterschiedliche Steuerzeichen (mal ist es zb. %20 für ein leerzeichen, mal ist es %2025)
 * dadurch hat das Plugin keine Chance an den Orginallink zu kommen.
 */

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class DoperomsCom extends PluginForDecrypt {

    static private final Pattern patternSupported = Pattern.compile("http://[\\w.]*?doperoms\\.com/roms/(.+)/(.+).html", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternFilesize = Pattern.compile("<br>Filesize: ([0-9]{1,}\\.[0-9]{1,} (GB|MB|KB|B))<br>", Pattern.CASE_INSENSITIVE);

    public DoperomsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // br.setFollowRedirects(true);

        String rootCat = new Regex(parameter, patternSupported).getMatch(0);
        String filename = new Regex(parameter, patternSupported).getMatch(1);
        br.getPage("http://doperoms.com/");
        br.setCookie("http://doperoms.net/roms/" + rootCat + "/" + filename.replaceAll(" ", "%20") + ".html", "PHPSESSID", br.getCookie(br.getURL(), "PHPSESSID"));
        br.getPage("http://doperoms.net/roms/" + rootCat + "/" + filename.replaceAll(" ", "%20") + ".html");

        String file = new Regex(br, "http://[\\w.]*?doperoms\\.com/files/roms/.+" + rootCat + "/" + filename).getMatch(0);
        long filesize = Regex.getSize(new Regex(br, patternFilesize.pattern()).getMatch(0));
        DownloadLink dlLink = createDownloadlink(file);
        dlLink.setDownloadSize(filesize);
        dlLink.setName(filename);

        decryptedLinks.add(dlLink);

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}