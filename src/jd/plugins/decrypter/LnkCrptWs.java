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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginUtils;
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+" }, flags = { 0 })
public class LnkCrptWs extends PluginForDecrypt {

    public LnkCrptWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);

        br.getPage("http://linkcrypt.ws/dlc/" + containerId);
        if (br.containsHTML("Error 404 - Ordner nicht gefunden")) return decryptedLinks;
        // check for a password. STore latest password in DB
        Form password = br.getForm(0);
        if (password != null && password.hasInputFieldByName("password")) {
            // Password Protected

            String latestPassword = this.getPluginConfig().getStringProperty("PASSWORD");
            if (latestPassword != null) {
                password.put("password", latestPassword);
                br.submitForm(password);
            }
            // no defaultpassword, or defaultpassword is wrong
            password = br.getForm(0);
            if (password != null && password.hasInputFieldByName("password")) {
                latestPassword = PluginUtils.askPassword(this);
                password.put("password", latestPassword);
                br.setDebug(true);
                br.submitForm(password);
                password = br.getForm(0);
                if (password != null && password.hasInputFieldByName("password")) throw new DecrypterException(DecrypterException.PASSWORD);
                getPluginConfig().setProperty("PASSWORD", latestPassword);
                getPluginConfig().save();
            }

        }

        // Different captcha types
        boolean valid = true;
        for (int i = 0; i < 15; i++) {
            Form captcha = br.getForm(i);
            String url = null;
            if (captcha != null) {
                url = captcha.getRegex("src=\"([^\"]*\\.php\\?id=[^\"]*)\"[^>]*style=\"cursor:(?![^>]*display[^>]*none)").getMatch(0);
                if (url == null) url = captcha.getRegex("style=\"cursor:(?![^>]*display[^>]*none)src=\"([^\"]*\\.php\\?id=[^\"]*)\"").getMatch(1);
                if (url == null && captcha != null && !captcha.hasInputFieldByName("key")) url = captcha.getRegex("src=\"([^\"]*\\.php\\?id=[^\"]*)\"").getMatch(0);
                if (url != null) {
                    valid = false;
                    File file = this.getLocalCaptchaFile();
                    String id = url.replaceFirst(".*id=", "");
                    br.cloneBrowser().getDownload(file, "http://linkcrypt.ws/captx.php?id=" + id);

                    String code = getCaptchaCode("lnkcrptwsCircles", file, param);
                    if (code == null) continue;
                    String[] codep = code.split(":");
                    Point p = new Point(Integer.parseInt(codep[0]), Integer.parseInt(codep[1]));
                    captcha.put("x", p.x + "");
                    captcha.put("y", p.y + "");
                    br.submitForm(captcha);
                }
            } else {
                valid = true;
                break;
            }
        }
        if (valid == false) throw new DecrypterException(DecrypterException.CAPTCHA);
        // System.out.println(br);

        // Look for containers
        String[] containers = br.getRegex("eval\\((.*?\\,\\{\\}\\))\\)").getColumn(0);
        HashMap<String, String> map = new HashMap<String, String>();
        for (String c : containers) {
            Context cx = null;
            try {
                cx = ContextFactory.getGlobal().enterContext();

                Scriptable scope = cx.initStandardObjects();
                c = c.replace("return p}(", " return p}  f(").replace("function(p,a,c,k,e,d)", "function f(p,a,c,k,e,d)");

                Object result = cx.evaluateString(scope, c, "<cmd>", 1, null);

                String code = Context.toString(result);
                // System.out.println(code);
                String[] row = new Regex(code, "href=\"([^\"]+)\"[^>]*>.*?<img.*?image/(.*?)\\.").getRow(0);
                if (row != null) {
                    map.put(row[1], row[0]);
                }
            } finally {
                if (cx != null) Context.exit();
            }

        }

        File container = null;
        if (map.containsKey("dlc")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc", true);
            if (!container.exists()) container.createNewFile();

            br.cloneBrowser().getDownload(container, map.get("dlc"));
        } else if (map.containsKey("cnl")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc", true);
            if (!container.exists()) container.createNewFile();
            br.cloneBrowser().getDownload(container, map.get("cnl").replace("dlc://", "http://"));
        } else if (map.containsKey("ccf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf", true);
            if (!container.exists()) container.createNewFile();
            br.cloneBrowser().getDownload(container, map.get("ccf"));
        } else if (map.containsKey("rsdf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf", true);
            if (!container.exists()) container.createNewFile();
            br.cloneBrowser().getDownload(container, map.get("rsdf"));
        }

        if (container != null) {
            // container available
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));

            container.delete();
            if (decryptedLinks.size() > 0) return decryptedLinks;
        }
        /* we have to open the normal page for weblinks */
        br.getPage("http://linkcrypt.ws/dir/" + containerId);
        // IF container decryption failed, try webdecryption
        Form[] forms = br.getForms();
        progress.setRange(forms.length / 2);
        for (Form form : forms) {
            Browser clone;
            if (form.getInputField("key") != null && form.getInputField("key").getValue() != null && form.getInputField("key").getValue().length() > 0) {
                progress.increase(1);
                clone = br.cloneBrowser();
                clone.submitForm(form);
                String[] srcs = clone.getRegex("<iframe.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getColumn(0);
                for (String col : srcs) {
                    col = Encoding.htmlDecode(col);
                    clone.getPage(col);
                    if (clone.containsHTML("eval")) {
                        String[] evals = clone.getRegex("eval\\((.*?\\,\\{\\}\\))\\)").getColumn(0);

                        for (String c : evals) {
                            Context cx = ContextFactory.getGlobal().enterContext();
                            Scriptable scope = cx.initStandardObjects();
                            c = c.replace("return p}(", " return p}  f(").replace("function(p,a,c,k,e", "function f(p,a,c,k,e");
                            Object result = cx.evaluateString(scope, c, "<cmd>", 1, null);
                            String code = Context.toString(result);
                            String versch;
                            versch = new Regex(code, ".*?='([^']*)'").getMatch(0);
                            versch = Encoding.Base64Decode(versch);
                            versch = new Regex(versch, "<iframe.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getMatch(0);
                            versch = Encoding.htmlDecode(versch);
                            decryptedLinks.add(this.createDownloadlink(versch));

                            String[] row = new Regex(code, "href=\"(.*?)\"><img.*?image/(.*?)\\.").getRow(0);

                            if (row != null) {
                                map.put(row[1], row[0]);
                            } else {
                                // System.out.println(code);
                            }

                        }

                    }

                }

            }

        }
        // webdecryption
        return decryptedLinks;
    }

}
