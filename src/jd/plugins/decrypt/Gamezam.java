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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class Gamezam extends PluginForDecrypt {
    static private final String host = "Gamez.am";
    static private final Pattern patternSupported = Pattern.compile("javascript:laden\\('include/infos\\.php\\?id=(\\d+)',1\\)", Pattern.CASE_INSENSITIVE);

    public Gamezam(String cfgName){
        super(cfgName);
        this.setAcceptOnlyURIs(false);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String id = new Regex(parameter, patternSupported).getMatch(0);

        boolean gamez_continue = false;

        /* Passwort suchen */
        String pw = new Regex(br.getPage("http://www.gamez.am/start.php?"), Pattern.compile("<tr><td>Passwort:</td><td>(.*?)</td></tr>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String page = null;
        for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
            File captchaFile = this.getLocalCaptchaFile(this);
           Browser.download(captchaFile, br.openGetConnection("http://www.gamez.am/captcha.php"));
            String captchaCode = JDUtilities.getCaptcha(this, "gamez.am", captchaFile, false);
            if (captchaCode == null) {
                /* Abbruch geklickt */
                return null;
            }

            page = br.getPage("http://www.gamez.am/include/check.php?id=" + id + "&captcha=" + captchaCode);
            if (page.indexOf("Falscher Code") == -1) {
                gamez_continue = true;
                break;
            }
        }

        if (gamez_continue == true) {
            /* gamez.am hat böse üble probleme mit falschen links */
            String direct_links[][] = new Regex(page, Pattern.compile("<a href=\"(.*?)\" target=\"_blank\"", Pattern.CASE_INSENSITIVE)).getMatches();
            String extern_links[][] = new Regex(page, Pattern.compile("window\\.open\\('extern\\.php\\?nr=(.*?)'\\);", Pattern.CASE_INSENSITIVE)).getMatches();
            for (String[] element : direct_links) {
                DownloadLink link = createDownloadlink(element[0]);
                link.addSourcePluginPassword(pw);
                decryptedLinks.add(link);
                progress.increase(1);
            }
            progress.setRange(extern_links.length + direct_links.length);
            for (String[] element : extern_links) {
                for (int retry = 0; retry < 3; retry++) {
                    page = br.getPage("http://www.gamez.am/extern.php?nr=" + element[0]);
                    if (br.getRedirectLocation() != null) {
                        DownloadLink link = createDownloadlink(br.getRedirectLocation());
                        link.addSourcePluginPassword(pw);
                        decryptedLinks.add(link);
                        progress.increase(1);
                        break;
                    } else {
                        String follow_link = new Regex(page, Pattern.compile("extern\\.php\\?aktion=unten&nr=(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                        follow_link = new Regex(br.getPage("http://www.gamez.am/extern.php?aktion=unten&nr=" + follow_link), Pattern.compile("<form action=\"(.*?)\" method=\"post\">", Pattern.CASE_INSENSITIVE)).getMatch(0);
                        if (follow_link != null) {
                            DownloadLink link = createDownloadlink(follow_link);
                            link.addSourcePluginPassword(pw);
                            decryptedLinks.add(link);
                            progress.increase(1);
                            break;
                        }
                    }
                }
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
