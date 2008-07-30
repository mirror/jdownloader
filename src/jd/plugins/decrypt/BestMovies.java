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
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class BestMovies extends PluginForDecrypt {
    static private final String HOST = "best-movies.us";
    private String VERSION = "1.0.0";    
    static private final Pattern patternSupported = Pattern.compile("http://crypt\\.best-movies\\.us/go\\.php\\?id\\=\\d+", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternCaptcha_Needed = Pattern.compile("<img src=\"captcha.php\"");
    static private final Pattern patternCaptcha_Wrong = Pattern.compile("Der Sicherheitscode ist falsch");
    static private final Pattern patternIframe = Pattern.compile("<iframe src=\"(.+?)\"", Pattern.DOTALL);

    public BestMovies() {
        super();
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return HOST;
    }

  

    
    public String getPluginName() {
        return HOST;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo reqInfo = null;
            Matcher matcher;
            boolean bestmovies_continue = false;
            reqInfo = HTTP.getRequest(url);
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                matcher = patternCaptcha_Wrong.matcher(reqInfo.getHtmlCode());
                if (matcher.find()) {
                    /* Falscher Captcha, Seite neu laden */
                    reqInfo = HTTP.getRequest(url);
                }
                /* Alle Requests müssen mit Cookie und Referer stattfinden */
                String cookie = reqInfo.getCookie();
                cookie = cookie.substring(0, cookie.indexOf(";") + 1);
                matcher = patternCaptcha_Needed.matcher(reqInfo.getHtmlCode());
                if (matcher.find()) {
                    /* Captcha vorhanden */
                    File captchaFile = this.getLocalCaptchaFile(this);
                    URL captcha_url = new URL("http://crypt.best-movies.us/captcha.php");
                    HTTPConnection captcha_con = new HTTPConnection(captcha_url.openConnection());
                    captcha_con.setRequestProperty("Referer", cryptedLink);
                    captcha_con.setRequestProperty("Cookie", cookie);
                    if (!JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                        /* Fehler beim Captcha */
                        logger.severe("Captcha Download fehlgeschlagen!");
                        return null;
                    }
                    String captchaCode = Plugin.getCaptchaCode(captchaFile, this);
                    if (captchaCode == null) {
                        /* abbruch geklickt */
                        return null;
                    }
                    reqInfo = HTTP.postRequest(new URL(cryptedLink), cookie, cryptedLink, null, "sicherheitscode=" + captchaCode + "&submit=Submit+Query", false);
                } else {
                    /* Kein Captcha */
                    bestmovies_continue = true;
                    break;
                }
            }
            if (bestmovies_continue == true) {
                matcher = patternIframe.matcher(reqInfo.getHtmlCode());
                if (matcher.find()) {
                    /* EinzelLink gefunden */
                    String link = matcher.group(1);
                    decryptedLinks.add(this.createDownloadlink((link)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    public boolean doBotCheck(File file) {
        return false;
    }
}
