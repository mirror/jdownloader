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

import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class FrozenRomsIn extends PluginForDecrypt {
    final static String host = "frozen-roms.in";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?frozen-roms\\.in/(details_[0-9]+|get_[0-9]+_[0-9]+)\\.html", Pattern.CASE_INSENSITIVE);

    public FrozenRomsIn(String cfgName) {
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String getLinks[][];

        if (parameter.indexOf("get") != -1) {
            getLinks = new Regex(parameter, Pattern.compile("http://[\\w\\.]*?frozen-roms\\.in/get_(.*?)\\.html", Pattern.CASE_INSENSITIVE)).getMatches();
        } else {
            getLinks = new Regex(br.getPage(parameter), Pattern.compile("href=\"http://[\\w\\.]*?frozen-roms\\.in/get_(.*?)\\.html\"")).getMatches();
        }
        progress.setRange(getLinks.length);
        for (String[] element : getLinks) {
            br.getPage("http://frozen-roms.in/get_" + element[0] + ".html");
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
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