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

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Sexuriacom extends PluginForDecrypt {
    private static final String HOST = "sexuria.com";

    private static final Pattern patternSupported_Main = Pattern.compile("http://[\\w\\.]*?sexuria\\.com/Pornos_Kostenlos_.+?_(\\d+)\\.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_Crypt = Pattern.compile("http://[\\w\\.]*?sexuria\\.com/dl_links_\\d+_(\\d+)\\.html", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupportetRedirect = Pattern.compile("http://[\\w\\.]*?sexuria\\.com/out.php\\?id=([0-9]+)\\&part=[0-9]+\\&link=[0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported = Pattern.compile(patternSupported_Main.pattern() + "|" + patternSupported_Crypt + "|" + patternSupportetRedirect.pattern(), Pattern.CASE_INSENSITIVE);

    private Browser br;

    public Sexuriacom(String cfgName) {
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String downloadId;
        String password = null;
        br = new Browser();
        br.setFollowRedirects(false);

        if (new Regex(parameter, patternSupported_Main).matches()) {
            String page = br.getPage(parameter);
            String Links[] = new Regex(page, "href=\"dl_links_(.*?)\" target=\"_blank\">", Pattern.CASE_INSENSITIVE).getColumn(0);
            for (String link : Links) {
                decryptedLinks.add(createDownloadlink("http://sexuria.com/dl_links_" + link));
            }
            return decryptedLinks;
        } else if (new Regex(parameter, patternSupported_Crypt).matches()) {
            downloadId = new Regex(parameter, patternSupported_Crypt).getMatch(0);
            String page = br.getPage("http://sexuria.com/Pornos_Kostenlos_info_" + downloadId + ".html");
            password = new Regex(page, "<strong>Passwort: </strong></div></td>.*?bgcolor=\"#EFEFEF\">(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).getMatch(0);
            page = br.getPage(parameter);
            String Links[] = new Regex(page, "value=\"(http://sexuria\\.com/out\\.php\\?id=\\d+\\&part=\\d+\\&link=\\d+)\" readonly", Pattern.CASE_INSENSITIVE).getColumn(0);
            for (String Link : Links) {
                br.getPage(Link);
                DownloadLink dl_link = createDownloadlink(br.getRedirectLocation());
                dl_link.addSourcePluginPassword(password);
                decryptedLinks.add(dl_link);
                logger.info(br.getRedirectLocation());
            }
            return decryptedLinks;
        } else if (new Regex(parameter, patternSupportetRedirect).matches()) {
            String id = new Regex(parameter, patternSupportetRedirect).getMatch(0);
            decryptedLinks.add(createDownloadlink("http://sexuria.com/Pornos_Kostenlos_liebe_" + id + ".html"));
            System.out.println(decryptedLinks.get(0));
            return decryptedLinks;
        }
        return null;
    }

    @Override
    public String getCoder() {
        return "ToKaM";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
