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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.components.Balloon;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.nutils.nativeintegration.LocalBrowser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+" }, flags = { 0 })
public class LnkCrptWs extends PluginForDecrypt {

    private final HashMap<String, String> map = new HashMap<String, String>();

    public LnkCrptWs(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        setBrowserExclusive();
        prepareBrowser(RandomUserAgent.generate());
        final String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);
        br.getPage("http://linkcrypt.ws/dir/" + containerId);
        if (br.containsHTML("Error 404 - Ordner nicht gefunden")) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        br.cloneBrowser().openGetConnection("http://linkcrypt.ws/js/jquery.js");
        // Different captcha types
        boolean valid = true;
        // Key-Captchas not supported yet
        for (int i = 0; i < 3; i++) {
            if (br.containsHTML("Key-Captcha")) {
                br.clearCookies("linkcrypt.ws");
                br.getPage("http://linkcrypt.ws/dir/" + containerId);
            }
        }
        if (br.containsHTML("Key-Captcha")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        if (br.containsHTML("CaptX|TextX")) {
            final int max_attempts = 3;
            for (int attempts = 0; attempts < max_attempts; attempts++) {
                if (valid && attempts > 0) {
                    break;
                }
                final Form[] captchas = br.getForms();
                String url = null;
                for (final Form captcha : captchas) {
                    if (captcha != null && br.containsHTML("CaptX|TextX")) {
                        url = captcha.getRegex("src=\"(.*?secid.*?)\"").getMatch(0);
                        if (url != null) {
                            valid = false;
                            final String capDescription = captcha.getRegex("<b>(.*?)</b>").getMatch(0);
                            final File file = this.getLocalCaptchaFile();
                            br.cloneBrowser().getDownload(file, url);
                            final Point p = UserIO.getInstance().requestClickPositionDialog(file, "LinkCrypt.ws | " + String.valueOf(max_attempts - attempts), capDescription);
                            captcha.put("x", p.x + "");
                            captcha.put("y", p.y + "");
                            br.submitForm(captcha);
                            if (!br.containsHTML("(Our system could not identify you as human beings\\!|Your choice was wrong\\! Please wait some seconds and try it again\\.)")) {
                                valid = true;
                            } else {
                                br.getPage("http://linkcrypt.ws/dir/" + containerId);
                            }
                        }
                    }
                }
            }
        }
        if (!valid) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        // check for a password. Store latest password in DB
        Form password = br.getForm(0);
        if (password != null && password.hasInputFieldByName("password")) {
            String latestPassword = getPluginConfig().getStringProperty("PASSWORD");
            if (latestPassword != null) {
                password.put("password", latestPassword);
                br.submitForm(password);
            }
            // no defaultpassword, or defaultpassword is wrong
            for (int i = 0; i <= 3; i++) {
                password = br.getForm(0);
                if (password != null && password.hasInputFieldByName("password")) {
                    latestPassword = Plugin.getUserInput(null, param);
                    password.put("password", latestPassword);
                    br.submitForm(password);
                    password = br.getForm(0);
                    if (password != null && password.hasInputFieldByName("password")) {
                        continue;
                    }
                    getPluginConfig().setProperty("PASSWORD", latestPassword);
                    getPluginConfig().save();
                    break;
                }
                break;
            }
        }
        if (password != null && password.hasInputFieldByName("password")) { throw new DecrypterException(DecrypterException.PASSWORD); }
        // Look for containers
        final String[] containers = br.getRegex("eval(.*?)\n").getColumn(0);
        for (final String c : containers) {
            Object result = new Object();
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                result = engine.eval(c);
            } catch (final Throwable e) {
            }
            final String code = result.toString();
            String[] row = new Regex(code, "href=\"([^\"]+)\"[^>]*>.*?<img.*?image/(.*?)\\.").getRow(0);
            if (row == null && br.containsHTML("dlc.png")) {
                row = new Regex(code, "href=\"(http.*?)\".*?(dlc)").getRow(0);
            }
            if (row != null) {
                map.put(row[1], row[0]);
            }
        }

        final Form preRequest = br.getForm(0);
        if (preRequest != null) {
            final String url = preRequest.getRegex("http://.*/captcha\\.php\\?id=\\d+").getMatch(-1);
            if (url != null) {
                br.cloneBrowser().openGetConnection(url);
            }
        }

        File container = null;
        if (map.containsKey("dlc")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc", true);
            if (!container.exists()) {
                container.createNewFile();
            }
            br.cloneBrowser().getDownload(container, map.get("dlc"));
        } else if (map.containsKey("ccf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf", true);
            if (!container.exists()) {
                container.createNewFile();
            }
            br.cloneBrowser().getDownload(container, map.get("ccf"));
        } else if (map.containsKey("rsdf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf", true);
            if (!container.exists()) {
                container.createNewFile();
            }
            br.cloneBrowser().getDownload(container, map.get("rsdf"));
        }
        if (container != null) {
            // container available
            logger.info("Container found: " + container);
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            if (decryptedLinks.size() > 0) { return decryptedLinks; }
        }
        /* we have to open the normal page for weblinks */
        if (br.containsHTML("BlueHeadLine.*?>(Weblinks)<")) {
            br.getPage("http://linkcrypt.ws/dir/" + containerId);
            logger.info("ContainerID is null, trying webdecryption...");
            final Form[] forms = br.getForms();
            progress.setRange(forms.length - 8);
            for (final Form form : forms) {
                Browser clone;
                if (form.getInputField("file") != null && form.getInputField("file").getValue() != null && form.getInputField("file").getValue().length() > 0) {
                    progress.increase(1);
                    clone = br.cloneBrowser();
                    clone.submitForm(form);
                    final String[] srcs = clone.getRegex("<frame scrolling.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getColumn(0);
                    for (String col : srcs) {
                        if (col.contains("out.pl=head")) {
                            continue;
                        }
                        col = Encoding.htmlDecode(col);
                        if (col.contains("out.pl")) {
                            clone.getPage(col);
                            // Thread.sleep(600);
                            if (clone.containsHTML("eval")) {
                                final String[] evals = clone.getRegex("eval(.*?)\n").getColumn(0);
                                for (final String c : evals) {
                                    Object result = new Object();
                                    final ScriptEngineManager manager = new ScriptEngineManager();
                                    final ScriptEngine engine = manager.getEngineByName("javascript");
                                    try {
                                        result = engine.eval(c);
                                    } catch (final Throwable e) {
                                    }
                                    final String code = result.toString();
                                    if (code.contains("ba2se") || code.contains("premfree")) {
                                        String versch;
                                        versch = new Regex(code, "ba2se='(.*?)'").getMatch(0);
                                        if (versch == null) {
                                            versch = new Regex(code, ".*?='([^']*)'").getMatch(0);
                                            versch = Encoding.Base64Decode(versch);
                                            versch = new Regex(versch, "<iframe.*?src\\s*?=\\s*?\"?([^\"> ]{20,})\"?\\s?").getMatch(0);
                                        }
                                        versch = Encoding.Base64Decode(versch);
                                        versch = Encoding.htmlDecode(new Regex(versch, "100.*?src=\"(.*?)\"></iframe>").getMatch(0));
                                        if (versch != null) {
                                            decryptedLinks.add(createDownloadlink(versch));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.info("No links found, let's see if CNL2 is available!");
            if (map.containsKey("cnl")) {
                LocalBrowser.openDefaultURL(new URL(parameter));
                Balloon.show(JDL.L("jd.controlling.CNL2.checkText.title", "Click'n'Load"), null, JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
                throw new DecrypterException(JDL.L("jd.controlling.CNL2.checkText.message", "Click'n'Load URL opened"));
            }
            logger.warning("Decrypter out of date for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    @Override
    protected String getInitials() {
        return "LC";
    }

    private void prepareBrowser(final String userAgent) {
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Language", "en-EN");
        br.getHeaders().put("User-Agent", userAgent);
        br.getHeaders().put("Connection", null);
    }
}
