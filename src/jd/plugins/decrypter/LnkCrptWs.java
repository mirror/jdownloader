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
import jd.gui.UserIO;
import jd.http.Browser;
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
import org.mozilla.javascript.Scriptable;

@DecrypterPlugin(revision = "$Revision: 7139 $", interfaceVersion = 2, names = { "linkcrypt.ws" }, urls = { "http://[\\w\\.]*?linkcrypt\\.ws/dir/[\\w]+" }, flags = { 0 })
public class LnkCrptWs extends PluginForDecrypt {

    public LnkCrptWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * TODO: könntet ihr aus linkcrypt.ws/dirl/id linkcrypt.ws/dlc/id machen?
     * (bezogen auf CNL Links im browser)
     */
    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);

        br.getPage("http://linkcrypt.ws/dlc/" + containerId);
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
                if (password != null && password.hasInputFieldByName("password")) {
                    PluginUtils.informPasswordWrong(this, latestPassword);
                    return null;
                }
                getPluginConfig().setProperty("PASSWORD", latestPassword);
                getPluginConfig().save();
            }

        }
        logger.finest("Captcha Protected");

        boolean valid = true;
        for (int i = 0; i < 5; ++i) {
            Form captcha = br.getForm(0);
            if (br.containsHTML("Bitte in den offenen Kreis klicken")) {
                valid = false;
                File file = this.getLocalCaptchaFile();
                String url = captcha.getRegex("src=\"(.*?)\"").getMatch(0);
                Browser.download(file, br.cloneBrowser().openGetConnection(url));
                Point p = UserIO.getInstance().requestClickPositionDialog(file, JDL.L("plugins.decrypt.stealthto.captcha.title", "Captcha"), JDL.L("plugins.decrypt.stealthto.captcha", "Please click on the Circle with a gap"));
                if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                captcha.put("x", p.x + "");
                captcha.put("y", p.y + "");
                br.submitForm(captcha);
            } else if (captcha != null && !captcha.hasInputFieldByName("key")) {
                valid = false;
                File file = this.getLocalCaptchaFile();
                String url = captcha.getRegex("src=\"(.*?)\"").getMatch(0);
                Browser.download(file, br.cloneBrowser().openGetConnection(url));
                Point p = UserIO.getInstance().requestClickPositionDialog(file, JDL.L("plugins.decrypt.stealthto.captcha.title", "Captcha"), "");
                if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                captcha.put("x", p.x + "");
                captcha.put("y", p.y + "");
                br.submitForm(captcha);
            } else {

                valid = true;
                break;
            }
        }

        if (valid == false) throw new DecrypterException(DecrypterException.CAPTCHA);

        String[] containers = br.getRegex("eval\\((.*?\\,\\{\\}\\))\\)").getColumn(0);

        HashMap<String, String> map = new HashMap<String, String>();

        for (String c : containers) {
            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects();
            c = c.replace("return p}(", " return p}  f(").replace("function(p,a,c,k,e,d)", "function f(p,a,c,k,e,d)");
            Object result = cx.evaluateString(scope, c, "<cmd>", 1, null);

            String code = Context.toString(result);
            String[] row = new Regex(code, "href=\"(.*?)\"><img.*?image/(.*?)\\.").getRow(0);

            if (row != null) {
                map.put(row[1], row[0]);
            } else {
                System.out.println(code);
            }

        }
        File container = null;
        if (map.containsKey("dlc")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            if (!container.exists()) container.createNewFile();
            Browser.download(container, map.get("dlc"));
        } else if (map.containsKey("cnl")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            if (!container.exists()) container.createNewFile();
            Browser.download(container, map.get("cnl").replace("dlc://", "http://"));
        } else if (map.containsKey("ccf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".ccf");
            if (!container.exists()) container.createNewFile();
            Browser.download(container, map.get("ccf"));
        } else if (map.containsKey("rsdf")) {
            container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".rsdf");
            if (!container.exists()) container.createNewFile();
            Browser.download(container, map.get("rsdf"));
        }
        if (container != null) {
            //container available
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));

            container.delete();
            if (decryptedLinks.size() > 0) return decryptedLinks;

        }
        
        // webdecryption
        return null;
    }

    // @Override

}
