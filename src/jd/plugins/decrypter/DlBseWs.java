//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import java.io.File;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dlbase.ws" }, urls = { "http://[\\w\\.]*?dlbase\\.ws/(season|game|program|music|xxx|movie)_details\\.php\\?id=[0-9]+" }, flags = { 0 })
public class DlBseWs extends PluginForDecrypt {

    public DlBseWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex("\"top\"><strong>(.*?)</strong>").getMatch(0).trim();
        fp.setName(fpName);
        String[] dlinks = br.getRegex("\"((season|game|program|music|xxx|movie)_details\\.php\\?.*?&download=.*?)\"").getColumn(0);
        if (dlinks == null || dlinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        for (String dlink : dlinks) {
            dlink = "http://dlbase.ws/" + dlink;
            br.getPage(dlink);
            // Password handling
            String password = br.getRegex("\"top\"><strong>(.*?)</strong>").getMatch(0).trim();
            ArrayList<String> passwords = new ArrayList<String>();
            if (password != null && !password.equals("-kein Passwort-") && password.length() != 0) {
                passwords.add(password);
            }
            String[] links = br.getRegex("\"(go\\.php\\?.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String link : links) {
                link = "http://dlbase.ws/" + link;
                br.getPage(link);
                /* captcha handling */
                String downlink = null;
                if (!br.containsHTML("kreiscaptcha.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                for (int i = 0; i <= 3; i++) {
                    File file = this.getLocalCaptchaFile();
                    Browser.download(file, br.cloneBrowser().openGetConnection("http://dlbase.ws/kreiscaptcha.php"));
                    int[] p = new jd.captcha.specials.GmdMscCm(file).getResult();
                    if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                    Form captchaForm = new Form();
                    captchaForm.setMethod(Form.MethodType.POST);
                    captchaForm.setAction(link);
                    captchaForm.put("button", "Send");
                    captchaForm.put("button.x", p[0] + "");
                    captchaForm.put("button.y", p[1] + "");
                    br.submitForm(captchaForm);

                    if (br.containsHTML("kreiscaptcha\\.php")) continue;
                    downlink = br.getRegex("URL=(.*?)\"></fieldset").getMatch(0);
                    if (downlink != null) break;
                }
                if (downlink == null && br.containsHTML("kreiscaptcha\\.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
                if (downlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                decryptedLinks.add(createDownloadlink(downlink));
                progress.increase(1);
            }
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}