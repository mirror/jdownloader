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
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RockHouseIn extends PluginForDecrypt {
    final static String host = "rock-house.in";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rock-house\\.in/warez/warez_download\\.php\\?id=\\d+", Pattern.CASE_INSENSITIVE);

    public RockHouseIn() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String page = br.getPage(parameter);
        String links[][] = new Regex(page, Pattern.compile("<td><a href=\'(.*?)\' target=\'_blank\'>", Pattern.CASE_INSENSITIVE)).getMatches();
        String pw = Encoding.htmlDecode(new Regex(page, Pattern.compile("<td class=\'button\'>Passwort:</td><td class=\'button\'>(.*?)<", Pattern.CASE_INSENSITIVE)).getMatch(0));
        for (String[] element : links) {
            DownloadLink link = createDownloadlink(element[0].replaceAll("\n", ""));
            link.addSourcePluginPassword(pw);
            decryptedLinks.add(link);
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}