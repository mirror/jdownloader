//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gmd-music.com" }, urls = { "http://[\\w\\.]*?gmd-music\\.com/download/\\d+_[\\w-]+/" }, flags = { 0 })
public class GmdMscCm extends PluginForDecrypt {

    private String domain = "http://gmd-music.com/";

    public GmdMscCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String[] redirectLinks = br.getRegex("window\\.open\\(\"(redirect/\\d+.*?)\"\\)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) return null;
        String pass = br.getRegex("Passwort:.*?<td align='left' width='50%'>(.*?)</td>").getMatch(0);
        ArrayList<String> passwords = new ArrayList<String>();
        passwords.add("gmd.6x.to");
        passwords.add("gmd-music.com");
        if ((pass != null) && !pass.equals("-") && !pass.equals("kein Passwort")) passwords.add(pass);
        for (String redlnk : redirectLinks) {
            br.getPage(this.domain + redlnk);
            handleCaptcha();
            String[] hostLinks = br.getRegex("<textarea name='links' rows='12' cols='104'>(http://.*?)\\s*</textarea>").getColumn(0);

            for (String hstlnk : hostLinks) {
                DownloadLink dl = createDownloadlink(hstlnk);
                dl.setSourcePluginPasswordList(passwords);
                decryptedLinks.add(dl);
            }
        }

        return decryptedLinks;
    }

    public void handleCaptcha() throws Exception {
        boolean valid = true;

        for (int i = 0; i < 5; i++) {
            if (br.containsHTML("Klicken Sie auf den ge&ouml;ffneten Kreis!")) {
                Form captcha = br.getFormbyProperty("name", "captcha");
                captcha.setAction(this.domain + captcha.getAction());
                valid = false;
                File file = this.getLocalCaptchaFile();
                String url = this.domain + captcha.getRegex("input type='image' src='(.*?)'").getMatch(0);
                Browser.download(file, br.cloneBrowser().openGetConnection(url));
                int[] p = new jd.captcha.specials.GmdMscCm(file).getResult();
                if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                captcha.put("button", "Send");
                captcha.put("button.x", p[0] + "");
                captcha.put("button.y", p[1] + "");
                br.submitForm(captcha);
            } else {
                valid = true;
                break;
            }
        }

        if (valid == false) throw new DecrypterException(DecrypterException.CAPTCHA);
    }
}