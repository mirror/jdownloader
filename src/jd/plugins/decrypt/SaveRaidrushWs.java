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

public class SaveRaidrushWs extends PluginForDecrypt {
    static private final String host = "save.raidrush.ws";
    private static final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?save\\.raidrush\\.ws/\\?id\\=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public SaveRaidrushWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String[][] links = new Regex(br.getPage(parameter), Pattern.compile("get\\('(.*?)','FREE','(.*?)'\\);", Pattern.CASE_INSENSITIVE)).getMatches();
        progress.setRange(links.length);
        for (String[] elements : links) {
            decryptedLinks.add(createDownloadlink("http://" + br.getPage("http://save.raidrush.ws/404.php.php?id=" + elements[0] + "&key=" + elements[1])));
            progress.increase(1);
        }

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