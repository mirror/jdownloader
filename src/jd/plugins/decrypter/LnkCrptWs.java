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
import jd.captcha.utils.Utilities;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
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
import jd.utils.locale.JDL;

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
        br.setReadTimeout(150000);
//        logger.info("br.ReadTimeout " + br.getReadTimeout());
        try {
            br.getPage("http://linkcrypt.ws/dir/" + containerId);
        } catch (Exception e) {
            if (Utilities.isLoggerActive()) {
                logger.severe("Error Server: " + e.getLocalizedMessage());
            }
            throw e;
        }     
        if (br.containsHTML("Error 404 - Ordner nicht gefunden")) {
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
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
        if (br.containsHTML("CaptX|ColorX|TextX")) {
            int max_attempts = 3;
            for (int attempts = 0; attempts < max_attempts; attempts++) {
                if (valid && attempts > 0) break;
                Form[] captchas = br.getForms();
                String url = null;
                for (Form captcha : captchas) {
                    if (captcha != null && br.containsHTML("CaptX|ColorX|TextX")) {
                        url = captcha.getRegex("src=\"([^\"]*\\.php\\?secid=[^\"]*)\"[^>]*style=\"cursor:(?![^>]*display[^>]*none)").getMatch(0);
                        if (url == null) url = captcha.getRegex("style=\"cursor:(?![^>]*display[^>]*none)src=\"([^\"]*\\.php\\?secid=[^\"]*)\"").getMatch(1);
                        if (url == null && captcha != null && !captcha.hasInputFieldByName("key")) url = captcha.getRegex("src=\"(.*?secid.*?)\"").getMatch(0);
                        if (url != null) {
                            valid = false;
                            String capDescription = captcha.getRegex("<b>(.*?)</b>").getMatch(0);
                            File file = this.getLocalCaptchaFile();
                            br.cloneBrowser().getDownload(file, url);
                            progress.setInitials(String.valueOf(max_attempts - attempts));
                            Point p = UserIO.getInstance().requestClickPositionDialog(file, "LinkCrypt.ws", capDescription);
                            captcha.put("x", p.x + "");
                            captcha.put("y", p.y + "");
                            br.submitForm(captcha);
                            if (!br.containsHTML("CaptX|ColorX|TextX") && br.containsHTML("eval") || br.getForms() != null) valid = true;
                        }
                    }
                }
            }
            progress.setInitials("LC");
        }
        if (!valid) throw new DecrypterException(DecrypterException.CAPTCHA);
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
                if (row == null && br.containsHTML("dlc.png")) {
                    row = new Regex(code, "href=\"(http.*?)\".*?(dlc)").getRow(0);
                }
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
            logger.info("Container found: " + container);
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            if (decryptedLinks.size() > 0) return decryptedLinks;
        }
        /* we have to open the normal page for weblinks */
        if (br.containsHTML("BlueHeadLine.*?>(Weblinks)<")) {
            br.getPage("http://linkcrypt.ws/dir/" + containerId);
            // IF container decryption failed, try webdecryption
            Form[] forms = br.getForms();
            progress.setRange(forms.length - 8);
            for (Form form : forms) {
                Browser clone;
                if (form.getInputField("key") != null && form.getInputField("key").getValue() != null && form.getInputField("key").getValue().length() > 0) {
                    progress.increase(1);
                    clone = br.cloneBrowser();
                    clone.submitForm(form);
                    String[] srcs = clone.getRegex("<iframe.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getColumn(0);
                    found_out_pl:
                    for (String col : srcs) {
                        col = Encoding.htmlDecode(col);
                        if (col.contains("out.pl")) {
                            clone.getPage(col);
                            Thread.sleep(600);
                            if (clone.containsHTML("eval")) {
                                String[] evals = clone.getRegex("eval\\((.*?\\,\\{\\}\\))\\)").getColumn(0);
                                for (String c : evals) {
                                    Context cx = ContextFactory.getGlobal().enterContext();
                                    Scriptable scope = cx.initStandardObjects();
                                    c = c.replace("return p}(", " return p}  f(").replace("function(p,a,c,k,e", "function f(p,a,c,k,e");
                                    Object result = cx.evaluateString(scope, c, "<cmd>", 1, null);
                                    String code = Context.toString(result);
                                    if (code.contains("ba2se") || code.contains("premfree")) {
                                        String versch;
                                        versch = new Regex(code, "Base64.decode\\('(.*?)'\\)").getMatch(0);
                                        if (versch == null) {
                                            versch = new Regex(code, ".*?='([^']*)'").getMatch(0);
                                            versch = Encoding.Base64Decode(versch);
                                            versch = new Regex(versch, "<iframe.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getMatch(0);
                                        }
                                        versch = Encoding.Base64Decode(versch);
                                        versch = Encoding.htmlDecode(versch);
                                        if (versch != null)
                                            decryptedLinks.add(this.createDownloadlink(versch));
                                    }
                                }
                            }
                            break found_out_pl;
                        }
                    }
                }
            }
        }
        if (decryptedLinks.size() == 0) { throw new Exception("Decrypter out of date. Try Click'n'Load"); }
        return decryptedLinks;
    }

    @Override
    protected String getInitials() {
        return "LC";
    }
}
