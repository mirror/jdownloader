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

import jd.controlling.DistributeData;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Cryptlinkws extends PluginForDecrypt {

    static private String host = "cryptlink.ws";
    final static private Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/crypt\\.php\\?file=[0-9]+", Pattern.CASE_INSENSITIVE);
    final static private Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?cryptlink\\.ws/\\?file=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    final static private Pattern patternSupported = Pattern.compile(patternSupported_Folder.pattern() + "|" + patternSupported_File.pattern(), Pattern.CASE_INSENSITIVE);

    public Cryptlinkws() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = null;
            RequestInfo reqinfo = null;
            if (cryptedLink.matches(patternSupported_File.pattern())) {
                /* eine einzelne Datei */
                url = new URL(cryptedLink);
                reqinfo = HTTP.getRequest(url);
                String link = new Regex(reqinfo.getHtmlCode(), "unescape\\(('|\")(.*?)('|\")\\)").getFirstMatch(2);
                link = Encoding.htmlDecode(Encoding.htmlDecode(link));
                url = new URL("http://www.cryptlink.ws/" + link);
                reqinfo = HTTP.getRequest(url);
                link = new Regex(reqinfo.getHtmlCode(), "unescape\\(('|\")(.*?)('|\")\\)").getFirstMatch(2);
                link = Encoding.htmlDecode(Encoding.htmlDecode(link));
                if (link.startsWith("cryptfiles/")) {
                    /* weiterleitung durch den server */
                    url = new URL("http://www.cryptlink.ws/" + link);
                    reqinfo = HTTP.getRequest(url);
                    decryptedLinks.addAll(new DistributeData(reqinfo.getHtmlCode()).findLinks(false));
                } else {
                    /* direkte weiterleitung */
                    decryptedLinks.add(createDownloadlink(link));
                }
            } else if (cryptedLink.matches(patternSupported_Folder.pattern())) {
                /* ein Folder */
                boolean do_continue = false;
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                    String post_parameter = "";
                    url = new URL(cryptedLink);
                    reqinfo = HTTP.getRequest(url);
                    if (reqinfo.containsHTML(">Ordnerpasswort:<")) {
                        String password = JDUtilities.getGUI().showUserInputDialog("Ordnerpasswort?");
                        if (password == null) {
                            /* auf abbruch geklickt */
                            return decryptedLinks;
                        }
                        post_parameter += "folderpass=" + Encoding.urlEncode(password);
                    }
                    if (reqinfo.containsHTML("captcha.php")) {
                        File captchaFile = getLocalCaptchaFile(this);
                        String captchaCode;
                        HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.cryptlink.ws/captcha.php").openConnection());
                        captcha_con.setRequestProperty("Referer", cryptedLink);
                        captcha_con.setRequestProperty("Cookie", reqinfo.getCookie());
                        if (!captcha_con.getContentType().contains("text") && !Browser.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                            /* Fehler beim Captcha */
                            logger.severe("Captcha Download fehlgeschlagen!");
                            return decryptedLinks;
                        }
                        /* CaptchaCode holen */
                        if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) { return decryptedLinks; }
                        if (post_parameter != "") {
                            post_parameter += "&";
                        }
                        post_parameter += "captchainput=" + Encoding.urlEncode(captchaCode);
                    }
                    if (post_parameter != "") {
                        reqinfo = HTTP.postRequest(new URL("http://www.cryptlink.ws/index.php?action=getfolder"), reqinfo.getCookie(), cryptedLink, null, post_parameter, false);
                    }
                    if (!reqinfo.containsHTML("Wrong Password! Klicken Sie") && !reqinfo.containsHTML("Wrong Captchacode! Klicken Sie")) {
                        do_continue = true;
                        break;
                    }
                }
                if (do_continue == true) {
                    String[] links = new Regex(reqinfo.getHtmlCode(), Pattern.compile("href=\"crypt\\.php\\?file=(\\d+)\"", Pattern.CASE_INSENSITIVE)).getMatches(1);
                    progress.setRange(links.length);
                    for (String element : links) {
                        decryptedLinks.add(createDownloadlink("http://www.cryptlink.ws/crypt.php?file=" + element));
                        progress.increase(1);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
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