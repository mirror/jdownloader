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

public class Woireless6xTo extends PluginForDecrypt {

    static private final String host = "chaoz.ws";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w.]*?chaoz\\.ws/woireless/page/album_\\d+\\.html", Pattern.CASE_INSENSITIVE);

    public Woireless6xTo() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(parameter);
        String fileId = new Regex(parameter, "album_(\\d+)\\.html").getMatch(0);
        String password = br.getRegex("Passwort:(.*?)<br />").getMatch(0);
        br.getPage("http://chaoz.ws/woireless/page/page/crypt.php?a=" + fileId + "&part=0&mirror=a");
        String link = br.getRegex("src=\"(.*?)\"").getMatch(0);
        if (link == null) return null;
        DownloadLink dl_link = createDownloadlink(link);
        dl_link.addSourcePluginPassword(password.trim());
        decryptedLinks.add(dl_link);
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
