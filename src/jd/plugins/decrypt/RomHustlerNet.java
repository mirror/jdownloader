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
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RomHustlerNet extends PluginForDecrypt {

    static private final String host = "romhustler.net";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w.]*?romhustler\\.net/rom/.*?/\\d+/.+", Pattern.CASE_INSENSITIVE);

    public RomHustlerNet() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String file[] = new Regex(parameter, "romhustler\\.net/rom/(.*?)/(\\d+)/.+").getRow(0);
        if (file.length != 2) return null;
        decryptedLinks.add(createDownloadlink("http://romhustler.net/download/" + file[0] + "/" + file[1]));

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
        String ret = new Regex("$Revision: 2354 $", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
