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
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Stealth extends PluginForDecrypt {
    static private final String host = "Stealth.to";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?stealth\\.to/(\\?id\\=[a-zA-Z0-9]+|index\\.php\\?id\\=[a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE);

    public Stealth() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        Browser.clearCookies(host);
        br.getPage(parameter);
        for (int i = 0; i < 5; ++i) {
            if (br.containsHTML("captcha_img.php")) {
                String sessid = new Regex(br.getRequest().getCookieString(), "PHPSESSID=([a-zA-Z0-9]*)").getMatch(0);
                if (sessid == null) {
                    logger.severe("Error sessionid: " + br.getRequest().getCookieString());
                    return null;
                }
                logger.finest("Captcha Protected");
                String captchaAdress = "http://stealth.to/captcha_img.php?PHPSESSID=" + sessid;
                File file = this.getLocalCaptchaFile(this);
                Form form = br.getForm(0);
                Browser.download(file, br.cloneBrowser().openGetConnection(captchaAdress));
                form.put("txtCode", getCaptchaCode(file, this));
                br.submitForm(form);
            } else {
                break;
            }
        }

        String[] links = br.getRegex("popup.php\\?id=(\\d+?)\"\\,'dl'").getColumn(-1);
        progress.setRange(links.length);
        for (String element : links) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(new Regex(br.cloneBrowser().getPage("http://stealth.to/popup.php?id=" + element), Pattern.compile("frame src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0))));
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
