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

public class FlyLoadnet extends PluginForDecrypt {
    static private final String host = "flyload.net";
    private static final Pattern patternSupported_Download = Pattern.compile("http://[\\w\\.]*?flyload\\.net/download\\.php\\?view\\.(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_Request = Pattern.compile("http://[\\w\\.]*?flyload\\.net/request_window\\.php\\?(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_Safe = Pattern.compile("http://[\\w\\.]*?flyload\\.net/safe\\.php\\?id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternSupported_Safe.pattern() + "|" + patternSupported_Request.pattern() + "|" + patternSupported_Download.pattern(), Pattern.CASE_INSENSITIVE);

    public FlyLoadnet() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        if (new Regex(parameter, patternSupported_Download).matches()) {
            String id = new Regex(parameter, patternSupported_Download).getFirstMatch();
            decryptedLinks.add(createDownloadlink("http://flyload.net/request_window.php?" + id));
        } else if (new Regex(parameter, patternSupported_Request).matches()) {
            String id = new Regex(parameter, patternSupported_Request).getFirstMatch();
            String pw = new Regex(br.getPage("http://flyload.net/download.php?view." + id), Pattern.compile("<td color:red;' class='forumheader3'>(?!<b>)(.*?)</td>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            String links[][] = new Regex(br.getPage(parameter), Pattern.compile("value='(.*?)' readonly onclick", Pattern.CASE_INSENSITIVE)).getMatches();
            progress.setRange(links.length);
            for (String[] element : links) {
                DownloadLink link = createDownloadlink(element[0]);
                if (!pw.matches("-") && !pw.matches("Kein Passwort")) link.addSourcePluginPassword(pw);
                decryptedLinks.add(link);
                progress.increase(1);
            }
        } else if (new Regex(parameter, patternSupported_Safe).matches()) {
            String links[][] = new Regex(br.getPage(parameter), Pattern.compile("onclick='popup\\(\"([a-zA-Z0-9]+)\",\"([a-zA-Z0-9]+)\"\\);", Pattern.CASE_INSENSITIVE)).getMatches();
            progress.setRange(links.length);
            for (String[] element : links) {
                br.getPage("http://flyload.net/safe.php?link_id=" + element[0] + "&link_hash=" + element[1]);
                if (br.getRedirectLocation() != null) {
                    decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
                }
                progress.increase(1);
            }
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
