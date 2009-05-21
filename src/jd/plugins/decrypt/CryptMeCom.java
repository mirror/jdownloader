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

package jd.plugins.decrypt;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ClickPositionDialog;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class CryptMeCom extends PluginForDecrypt {

    public CryptMeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookie("http://crypt-me.com/", "CMLang", "DE");
        br.getPage(parameter);
        boolean cont = false;
        for (int retry = 0; retry < 5; retry++) {
            if (br.containsHTML("Sicherheitscode Falsch")) {
                br.getPage(parameter);
                cont = false;
            }
            /* Calculation-Captcha */
            if (br.containsHTML("<img src=\"http://crypt-me.com/rechen-captcha.php\">")) {
                cont = false;
                Form form = br.getForm(0);
                String captchaAddress = "http://crypt-me.com/rechen-captcha.php";
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
                String captchaCode = getCaptchaCode("crypt-me.com.Calc", captchaFile, param);
                /* Calculation process */
                captchaCode = captchaCode.replaceAll("_", "-").replaceAll("=", "").replaceAll("!", "");
                if (captchaCode.contains("-")) {
                    String[] values = captchaCode.split("-");
                    captchaCode = Integer.toString(Integer.parseInt(values[0]) - Integer.parseInt(values[1]));
                } else if (captchaCode.contains("+")) {
                    String[] values = captchaCode.split("\\+");
                    captchaCode = Integer.toString(Integer.parseInt(values[0]) + Integer.parseInt(values[1]));
                }
                form.put("sicherheitscode", captchaCode);
                br.submitForm(form);
                /* "Normal" Captcha */
            } else if (br.containsHTML("Bitte geben sie Captcha ein")) {
                cont = false;
                Form form = br.getForm(0);
                String captchaAddress = br.getRegex("<img src=\"(http://crypt-me\\.com/captchanew/show\\.php.*?)\"").getMatch(0);
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
                String captchaCode = getCaptchaCode("linkprotect.in", captchaFile, param);
                form.put("code", captchaCode);
                br.submitForm(form);
            } else {
                cont = true;
                break;
            }
        }
        if (!cont) throw new DecrypterException(DecrypterException.CAPTCHA);

        // Angebotene Containerformate herausfinden
        String[][] containers = br.getRegex("<a href='(http://crypt-me.com/dl\\.php\\?file=.*?(\\..*?))' target='_blank'>").getMatches();

        for (String[] container : containers) {
            File containerFile = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + container[1]);
            Browser.download(containerFile, container[0]);
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(containerFile));
            containerFile.delete();
            if (decryptedLinks.size() != 0) return decryptedLinks;
        }
        String folderId = new Regex(parameter, "folder/([a-zA-Z0-9]+)\\.html").getMatch(0);
        int folderSize = br.getRegex("<a onclick=\"return newpopup\\(('.*?', '.*?')\\);\" ").count();
        Browser brc = null;
        for (int i = 1; i <= folderSize; i++) {
            String url = "http://crypt-me.com/go.php?id=" + folderId + "&lk=" + i;
            for (int retry = 0; retry < 5; retry++) {
                brc = br.cloneBrowser();
                brc.getPage(url);
                File file = this.getLocalCaptchaFile();
                Form form = new Form();
                form.setAction(url);
                Browser.download(file, brc.cloneBrowser().openGetConnection("http://crypt-me.com/kreiscaptcha.php"));
                ClickPositionDialog d = ClickPositionDialog.show(SimpleGUI.CURRENTGUI, file, "Captcha", JDLocale.L("plugins.decrypt.charts4you.captcha", "Please click on the Circle with a gap"), 20, null);
                if (d.abort == true) throw new DecrypterException(DecrypterException.CAPTCHA);
                Point p = d.result;
                form.setMethod(MethodType.POST);
                form.put("button", "send");
                form.put("button.x", p.x + "");
                form.put("button.y", p.y + "");
                brc.submitForm(form);
                String tmp2 = decrypt(brc);
                if (tmp2.contains("kreiscaptcha")) continue;
                String encodedLink = new Regex(tmp2, "<iframe src=\"(.*?)\"").getMatch(0);
                if (encodedLink != null) {
                    decryptedLinks.add(createDownloadlink(encodedLink));
                    break;
                }
            }
        }
        return decryptedLinks;
    }

    private String decrypt(Browser br) {
        String c = br.getRegex("c=\"(.*?)\"").getMatch(0);
        String x = br.getRegex("x\\(\"(.*?)\"\\)").getMatch(0);
        String f = decrypt1(c);
        String dec2 = decrypt2(f, x);
        return dec2;
    }

    private String decrypt1(String in) {
        String ret = "";
        for (int i = 0; i < in.length(); i++) {
            if (i % 3 == 0) {
                ret += "%";
            } else {
                ret += in.charAt(i);
            }
        }
        return Encoding.htmlDecode(ret);
    }

    private String decrypt2(String f, String x) {
        f = f.replaceAll("document\\.write\\(r\\)", "return r");
        f = f.replaceAll("b=1024", "b=8192");/* damit alles decoded wird */
        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){z=\"" + x + "\"; " + f + "\nreturn x(z)} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        String ret = Context.toString(result);
        return ret;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
