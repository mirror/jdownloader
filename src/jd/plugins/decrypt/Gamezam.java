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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Gamezam extends PluginForDecrypt {
    static private final String host = "Gamez.am";
    private String version = "1.0.0.0";
    static private final Pattern patternSupported = Pattern.compile("javascript:laden\\('include/infos\\.php\\?id=(\\d+)',1\\)", Pattern.CASE_INSENSITIVE);

    public Gamezam() {
        super();
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        String id = new Regex(cryptedLink, patternSupported).getFirstMatch();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            boolean gamez_continue = false;

            URL url;
            RequestInfo reqInfo = HTTP.getRequest(new URL("http://www.gamez.am/start.php?"), null, null, false);
            String cookie = reqInfo.getConnection().getHeaderFields().get("Set-Cookie").toString().replaceAll("\\[|\\]", "");
            /* Passwort suchen */
            reqInfo = HTTP.getRequest(new URL("http://www.gamez.am/include/infos.php?id=" + id), cookie, "http://www.gamez.am/start.php?", false);
            String pw = new Regex(reqInfo.getHtmlCode(), Pattern.compile("<tr><td>Passwort:</td><td>(.*?)</td></tr>", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.gamez.am/captcha.php").openConnection());
                captcha_con.setRequestProperty("Referer", "http://www.gamez.am/start.php?");
                captcha_con.setRequestProperty("Cookie", cookie);
                File captchaFile = this.getLocalCaptchaFile(this);
                if (!JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                    /* Fehler beim Captcha */
                    return null;
                }
                String captchaCode = JDUtilities.getCaptcha(this, "gamez.am", captchaFile, false);
                if (captchaCode == null) {
                    /* abbruch geklickt */
                    return null;
                }
                url = new URL("http://www.gamez.am/include/check.php?id=" + id + "&captcha=" + captchaCode);
                reqInfo = HTTP.getRequest(url, cookie, "http://www.gamez.am/start.php?", false);
                if (!reqInfo.containsHTML("Falscher Code")) {
                    gamez_continue = true;
                    break;
                }
            }
            if (gamez_continue == true) {
                /* gamez.am hat böse üble probleme mit falschen links */
                String direct_links[][] = new Regex(reqInfo.getHtmlCode(), Pattern.compile("<a href=\"(.*?)\" target=\"_blank\"", Pattern.CASE_INSENSITIVE)).getMatches();
                String extern_links[][] = new Regex(reqInfo.getHtmlCode(), Pattern.compile("window\\.open\\('extern\\.php\\?nr=(.*?)'\\);", Pattern.CASE_INSENSITIVE)).getMatches();
                for (int i = 0; i < direct_links.length; i++) {
                    DownloadLink link = this.createDownloadlink(direct_links[i][0]);
                    link.addSourcePluginPassword(pw);
                    decryptedLinks.add(link);
                    this.progress.increase(1);
                }
                this.progress.setRange(extern_links.length + direct_links.length);
                for (int i = 0; i < extern_links.length; i++) {
                    for (int retry = 0; retry < 3; retry++) {
                        RequestInfo ri_extern_link = HTTP.getRequest(new URL("http://www.gamez.am/extern.php?nr=" + extern_links[i][0]), cookie, "http://www.gamez.am/start.php?", false);
                        if (ri_extern_link.getLocation() != null) {
                            DownloadLink link = this.createDownloadlink(ri_extern_link.getLocation());
                            link.addSourcePluginPassword(pw);
                            decryptedLinks.add(link);
                            this.progress.increase(1);
                            break;
                        } else {
                            String follow_link = new Regex(ri_extern_link.getHtmlCode(), Pattern.compile("extern\\.php\\?aktion=unten&nr=(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                            ri_extern_link = HTTP.getRequest(new URL("http://www.gamez.am/extern.php?aktion=unten&nr=" + follow_link), cookie, "http://www.gamez.am/start.php?", false);
                            follow_link = new Regex(ri_extern_link.getHtmlCode(), Pattern.compile("<form action=\"(.*?)\" method=\"post\">", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                            if (follow_link != null) {
                                DownloadLink link = this.createDownloadlink(follow_link);
                                link.addSourcePluginPassword(pw);
                                decryptedLinks.add(link);
                                this.progress.increase(1);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return host;
    }

   

    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}
