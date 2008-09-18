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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class UrlShieldnet extends PluginForDecrypt {

    private String captchaCode;
    private File captchaFile;
    private String passCode = null;

    public UrlShieldnet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        try {

            boolean do_continue = true;
            Form form;
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());

            br.getPage(parameter);
            br.setFollowRedirects(false);
            for (int retry = 1; retry < 5; retry++) {
                if (br.containsHTML("Invalid Password")) {
                    br.getPage(parameter);
                }
                if (br.containsHTML("<b>Password</b>")) {
                    do_continue = false;
                    /* Passwort */
                    form = br.getForm(0);
                    if ((passCode = JDUtilities.getGUI().showUserInputDialog("Code?")) == null) {
                        do_continue = false;
                        break;
                    }
                    form.getVars().put("password", passCode);

                    br.submitForm(form);
                } else {
                    do_continue = true;
                    break;
                }
            }
            if (do_continue == true) {
                if (br.containsHTML("window.alert")) {
                    logger.severe(br.getRegex("window.alert\\(\"(.*?)\"\\)").getMatch(0));
                    do_continue = false;
                }
            }
            if (do_continue == true) {
                /* doofes JS */
                String all = Encoding.htmlDecode(br.getRegex(Pattern.compile("SCRIPT>eval\\(unescape\\(\"(.*?)\"\\)", Pattern.CASE_INSENSITIVE)).getMatch(0));
                String dec = br.getRegex(Pattern.compile("<SCRIPT>dc\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0);
                all = all.replaceAll("document\\.writeln\\(s\\);", "");
                Context cx = Context.enter();
                Scriptable scope = cx.initStandardObjects();
                String fun = "function f(){" + all + " \n return unescape(unc('" + dec + "'))} f()";
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                String java_page = Context.toString(result);
                Context.exit();

                /* Link zur richtigen Seiten */
                String page_link = new Regex(java_page, Pattern.compile("src=\"(/content\\.php\\?id=.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                String url = "http://www.urlshield.net" + page_link;

                for (int retry = 1; retry < 5; retry++) {
                    if (br.getRedirectLocation() != null) {
                        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
                        break;
                    }

                    br.getPage(url);
                    if (br.containsHTML("getkey.php?id")) {
                        String captchaurl = br.getRegex(Pattern.compile("src=\"(/getkey\\.php\\?id=.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                        form = br.getForm(0);
                        /* Captcha zu verarbeiten */
                        captchaFile = getLocalCaptchaFile(this);
                        HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.urlshield.net" + captchaurl).openConnection());
                        Browser.download(captchaFile, captcha_con);
                        /* CaptchaCode holen */
                        captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
                        form.getVars().put("userkey", captchaCode);

                        br.submitForm(form);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
