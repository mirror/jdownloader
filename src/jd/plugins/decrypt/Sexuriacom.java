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

/*
 * TODO bug fixen: 
 * Bei diesem Link geht das decryptete Passwort intern unter. 
 * Vermutlich da auf die Redirecturl kein Pattern passt und kein Plugin arbeitet.
 * Habe schon in der DistributeData nach ner Lösung geschaut, aber keine Gefunden,
 * es steht ja dort in Zeile 250 und 251:
 * dLinks.get(c).addSourcePluginPasswords(foundpassword);
 * dLinks.get(c).addSourcePluginPasswords(decrypted.getSourcePluginPasswords());
 * Hier der Link mit dem Bug:
 * http://sexuria.com/Pornos_Kostenlos_Crimson-Mansion-4-The-Catacombs_28460.html
 * Das Sexuria plugin scheint zu funktionieren, das habe ich mit Debug Ausgaben überprüft!
 */

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Sexuriacom extends PluginForDecrypt {

    private static final Pattern PATTEREN_SUPPORTED_MAIN = Pattern.compile("http://[\\w\\.]*?sexuria\\.com/Pornos_Kostenlos_.+?_(\\d+)\\.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_CRYPT = Pattern.compile("http://[\\w\\.]*?sexuria\\.com/dl_links_\\d+_(\\d+)\\.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_SUPPORTED_REDIRECT = Pattern.compile("http://[\\w\\.]*?sexuria\\.com/out.php\\?id=([0-9]+)\\&part=[0-9]+\\&link=[0-9]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_PASSWORD = Pattern.compile("<strong>Passwort: </strong></div></td>.*?bgcolor=\"#EFEFEF\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PATTERN_DL_LINK_PAGE = Pattern.compile("href=\"dl_links_(.*?)\" target=\"_blank\">", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_REDIRECT_LINKS = Pattern.compile("value=\"(http://sexuria\\.com/out\\.php\\?id=\\d+\\&part=\\d+\\&link=\\d+)\" readonly", Pattern.CASE_INSENSITIVE);

    public Sexuriacom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String downloadId;
        String password = null;
        br.setFollowRedirects(false);

        if (new Regex(parameter, PATTEREN_SUPPORTED_MAIN).matches()) {
            String page = br.getPage(parameter);
            String links[] = new Regex(page, PATTERN_DL_LINK_PAGE).getColumn(0);
            for (String link : links) {
                decryptedLinks.add(createDownloadlink("http://sexuria.com/dl_links_" + link));
            }
            return decryptedLinks;
        } else if (new Regex(parameter, PATTERN_SUPPORTED_CRYPT).matches()) {
            downloadId = new Regex(parameter, PATTERN_SUPPORTED_CRYPT).getMatch(0);
            String page = br.getPage("http://sexuria.com/Pornos_Kostenlos_info_" + downloadId + ".html");
            password = new Regex(page, PATTERN_PASSWORD).getMatch(0);
            page = br.getPage(parameter);
            String links[] = new Regex(page, PATTERN_REDIRECT_LINKS).getColumn(0);
            for (String link : links) {
                br.getPage(link);
                DownloadLink dlLink = createDownloadlink(br.getRedirectLocation());
                dlLink.addSourcePluginPassword(password);
                decryptedLinks.add(dlLink);
            }
            return decryptedLinks;
        } else if (new Regex(parameter, PATTERN_SUPPORTED_REDIRECT).matches()) {
            String id = new Regex(parameter, PATTERN_SUPPORTED_REDIRECT).getMatch(0);
            decryptedLinks.add(createDownloadlink("http://sexuria.com/Pornos_Kostenlos_liebe_" + id + ".html"));
            return decryptedLinks;
        }
        return null;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
