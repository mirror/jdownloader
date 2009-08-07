//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.captcha.specials.Linksave;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class LinksaveIn extends PluginForDecrypt {

    public LinksaveIn(PluginWrapper wrapper) {
        super(wrapper);
        br.setRequestIntervalLimit(this.getHost(), 1000);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://linksave.in/", "Linksave_Language", "german");
        br.getPage(param.getCryptedUrl());
        if (br.containsHTML("Ordner nicht gefunden")) return decryptedLinks;
        Form form = br.getFormbyProperty("name", "form");
        for (int retry = 0; retry < 5; retry++) {
            if (form == null) break;
            if (form.containsHTML("besucherpasswort")) {
                String pw = getUserInput("Besucherpasswort", param);
                form.put("besucherpasswort", pw);
            }
            String url = "captcha/cap.php?hsh=" + form.getRegex("\\/captcha\\/cap\\.php\\?hsh=([^\"]+)").getMatch(0);
            File captchaFile = this.getLocalCaptchaFile();
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(url));
            Linksave.prepareCaptcha(captchaFile);
            String captchaCode = getCaptchaCode(captchaFile, param);
            form.put("code", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("Captcha-code ist falsch") || br.containsHTML("Besucherpasswort ist falsch")) {
                br.getPage(param.getCryptedUrl());
                form = br.getFormbyProperty("name", "form");
            } else {
                break;
            }
        }

        String[] container = br.getRegex("\\.href\\=unescape\\(\\'(.*?)\\'\\)\\;").getColumn(0);
        if (container != null && container.length > 0) {
            for (String c : container) {
                /*
                 * Context cx = Context.enter(); Scriptable scope =
                 * cx.initStandardObjects(); String fun =
                 * "function f(){ \nreturn '" + c + "';} f()"; Object result =
                 * cx.evaluateString(scope, fun, "<cmd>", 1, null);
                 * 
                 * c=result.toString();
                 */
                String test = Encoding.htmlDecode(c);
                File file = null;
                if (test.endsWith(".cnl")) {
                    URLConnectionAdapter con = br.openGetConnection("http://linksave.in/" + test.replace("dlc://linksave.in/", ""));
                    if (con.getResponseCode() == 200) {
                        file = JDUtilities.getResourceFile("tmp/linksave/" + test.replace(".cnl", ".dlc").replace("dlc://", "http://").replace("http://linksave.in", ""));
                        br.downloadConnection(file, con);
                    } else {
                        con.disconnect();
                    }
                } else if (test.endsWith(".rsdf")) {
                    URLConnectionAdapter con = br.openGetConnection(test);
                    if (con.getResponseCode() == 200) {
                        file = JDUtilities.getResourceFile("tmp/linksave/" + test.replace("http://linksave.in", ""));
                        br.downloadConnection(file, con);
                    } else {
                        con.disconnect();
                    }
                }
                if (file != null && file.exists() && file.length() > 100) {
                    decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                    if (decryptedLinks.size() > 0) return decryptedLinks;
                }
            }
            if (decryptedLinks.size() == 0) throw new DecrypterException("Out of date. Try Click'n'Load");
        } else {
            throw new DecrypterException("Out of date. Try Click'n'Load");
        }
        return decryptedLinks;
    }

    // @Override
    protected boolean isClickNLoadEnabled() {
        return true;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
