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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RapidRace extends PluginForDecrypt {
    static private final String HOST = "rapidrace.org";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidrace\\.org/rel\\.php\\?ID=.+", Pattern.CASE_INSENSITIVE);

    public RapidRace(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String finalUrl = "";
        String page = br.getPage(parameter);
        while (page.indexOf("http://www.rapidrace.org/load.php?ID") != -1) {
            finalUrl = "";
            page = page.substring(page.indexOf("http://www.rapidrace.org/load.php?ID"));
            String subPage = br.getPage(page.substring(0, page.indexOf("\"")));
            String tmp = subPage.substring(subPage.indexOf("document.write(fu('") + 19);
            tmp = tmp.substring(0, tmp.indexOf("'"));
            for (int i = 0; i < tmp.length(); i += 2) {
                finalUrl = finalUrl + (char) (Integer.parseInt(tmp.substring(i, i + 2), 16) ^ i / 2);
            }
            decryptedLinks.add(createDownloadlink(finalUrl));
            page = page.substring(20);
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "TheBlindProphet";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
