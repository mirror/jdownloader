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

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class UrlShieldnet extends PluginForDecrypt {

    static private final String host = "urlshield.net";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?urlshield\\.net/l/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    private String captchaCode;
    private File captchaFile;
    private String passCode = null;

    public UrlShieldnet() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url;
            url = new URL(cryptedLink);
            boolean do_continue = true;
            Form form;
            RequestInfo reqinfo = HTTP.getRequest(url);

            for (int retry = 1; retry < 5; retry++) {
                if (reqinfo.containsHTML("Invalid Password")) {
                    reqinfo = HTTP.getRequest(url);
                }
                if (reqinfo.containsHTML("<b>Password</b>")) {
                    do_continue = false;
                    /* Passwort */
                    form = reqinfo.getForms()[0];
                    if ((passCode = JDUtilities.getGUI().showUserInputDialog("Code?")) == null) {
                        do_continue = false;
                        break;
                    }
                    form.vars.put("password", passCode);
                    reqinfo = form.getRequestInfo(false);
                } else {
                    do_continue = true;
                    break;
                }
            }
            if (do_continue == true) {
                if (reqinfo.containsHTML("window.alert")) {
                    logger.severe(new Regex(reqinfo.getHtmlCode(), "window.alert\\(\"(.*?)\"\\)").getFirstMatch());
                    do_continue = false;
                }
            }
            if (do_continue == true) {
                /* doofes JS */
                String all = JDUtilities.htmlDecode(new Regex(reqinfo.getHtmlCode(), Pattern.compile("SCRIPT>eval\\(unescape\\(\"(.*?)\"\\)", Pattern.CASE_INSENSITIVE)).getFirstMatch());
                String dec = new Regex(reqinfo.getHtmlCode(), Pattern.compile("<SCRIPT>dc\\('(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                all = all.replaceAll("document\\.writeln\\(s\\);", "");
                Context cx = Context.enter();
                Scriptable scope = cx.initStandardObjects();
                String fun = "function f(){" + all + " \n return unescape(unc('" + dec + "'))} f()";
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                String java_page = Context.toString(result);
                Context.exit();

                /* Link zur richtigen Seiten */
                String page_link = new Regex(java_page, Pattern.compile("src=\"(/content\\.php\\?id=.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                url = new URL("http://www.urlshield.net" + page_link);

                for (int retry = 1; retry < 5; retry++) {
                    if (reqinfo.getLocation() != null) {
                        decryptedLinks.add(createDownloadlink(reqinfo.getLocation()));
                        break;
                    }
                    reqinfo = HTTP.getRequest(url);
                    if (reqinfo.containsHTML("getkey.php?id")) {
                        String captchaurl = new Regex(reqinfo.getHtmlCode(), Pattern.compile("src=\"(/getkey\\.php\\?id=.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                        form = reqinfo.getForms()[0];
                        /* Captcha zu verarbeiten */
                        captchaFile = getLocalCaptchaFile(this);
                        HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.urlshield.net" + captchaurl).openConnection());
                        if (!JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                            /* Fehler beim Captcha */
                            logger.severe("Captcha Download fehlgeschlagen!");
                            return null;
                        }
                        /* CaptchaCode holen */
                        if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) { return null; }
                        form.vars.put("userkey", captchaCode);
                        reqinfo = form.getRequestInfo(false);
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
