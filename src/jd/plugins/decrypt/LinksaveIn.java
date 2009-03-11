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

import java.io.File;
import java.util.ArrayList;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class LinksaveIn extends PluginForDecrypt {

    public LinksaveIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {

        br.getPage(param.getCryptedUrl());
        br.forceDebug(true);
        //Linksave_Besucherpasswort=%FEd10fb83.64625
   
        Form form = br.getFormbyProperty("name", "form");
        while (form != null) {
            String url = "captcha/cap.php?hsh="+form.getRegex("\\/captcha\\/cap\\.php\\?hsh=([^\"]+)").getMatch(0);
            File captchaFile = this.getLocalCaptchaFile(this);
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(url));

            String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
            if (captchaCode == null) return null;
            form.put("code", captchaCode);
            br.submitForm(form);
            if (br.containsHTML("Captcha-code ist falsch")) {
                br.getPage(param.getCryptedUrl());
                form = br.getFormbyProperty("name", "form");
            } else {
                break;
            }
        }
       
        String[] container = br.getRegex("link\\'\\)\\.href\\=\\'(.*?)\\'\\;").getColumn(0);
        if (container != null && container.length > 0) {
            
       
            
            File file = null;
            for (String c : container) {
                Context cx = Context.enter();
                Scriptable scope = cx.initStandardObjects();
                String fun = "function f(){ \nreturn '" + c + "';} f()";
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                c=result.toString();
                URLConnectionAdapter con = br.openGetConnection("http://linksave.in/" + c);
                if (con.getResponseCode() == 200) {
                    br.downloadConnection(file = JDUtilities.getResourceFile("tmp/linksave/" + c.replace(".cnl", ".dlc").replace("dlc://", "http://")), con);
                    break;
                } else {
                    con.disconnect();
                }
            }
            if (file != null && file.exists() && file.length() > 100) {
                JDUtilities.getController().loadContainerFile(file);
            } else {
                throw new DecrypterException("Out of date. Try Click'n'Load");
            }
        } else {
            throw new DecrypterException("Out of date. Try Click'n'Load");
        }
        return new ArrayList<DownloadLink>();
    }

    protected boolean isClickNLoadEnabled() {
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
