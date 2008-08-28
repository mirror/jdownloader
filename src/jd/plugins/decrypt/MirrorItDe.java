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

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class MirrorItDe extends PluginForDecrypt {

    final static String host = "mirrorit.de";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?mirrorit\\.de/\\?id=[a-zA-Z0-9]{16}", Pattern.CASE_INSENSITIVE);

    public MirrorItDe() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String[][] links = new Regex(br.getPage(parameter), Pattern.compile("launchDownloadURL\\(\'(.*?)\', \'(.*?)\'\\)", Pattern.CASE_INSENSITIVE)).getMatches();
        progress.setRange(links.length);
        for (String[] element : links) {
            br.getPage("http://www.mirrorit.de/Out?id=" + URLDecoder.decode(element[0], "UTF-8") + "&num=" + element[1]);
            br.getPage(br.getRedirectLocation());
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