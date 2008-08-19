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

public class ProtectTehparadoxcom extends PluginForDecrypt {

    static private final String host = "Protect.Tehparadox.com";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?protect\\.tehparadox\\.com\\/[a-zA-Z0-9]+\\!", Pattern.CASE_INSENSITIVE);

    public ProtectTehparadoxcom() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String downloadlink = new Regex(br.postPage("http://protect.tehparadox.com/getdata.php", "id=" + parameter.substring(parameter.lastIndexOf("/") + 1, parameter.length() - 1)), Pattern.compile("<iframe name=\"ifram\" src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (downloadlink == null) return null;
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(downloadlink)));

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
