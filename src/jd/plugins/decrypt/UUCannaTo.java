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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class UUCannaTo extends PluginForDecrypt {

    public UUCannaTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        boolean valid = false;
        br.getPage(parameter);
        for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
            String captchaUrl = br.getRegex("<img src=\"(captcha/captcha\\.php\\?id=[\\d]+)\"").getMatch(0);

            File captchaFile = this.getLocalCaptchaFile(this);
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://uu.canna.to/cpuser/" + captchaUrl));
            String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
            Form captchaForm = br.getForm(1);
            captchaForm.put("sicherheitscode", captchaCode);
            br.submitForm(captchaForm);

            if (br.containsHTML("Der Sicherheitscode ist falsch!")) {
                /* Falscher Captcha, Seite neu laden */
                br.getPage(parameter);
            } else {
                valid = true;
                decryptedLinks.add(createDownloadlink(br.getRegex("URL=(.*?)\"").getMatch(0)));
                break;
            }
        }
        if (valid == false) throw new DecrypterException("Wrong Captcha Code");
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
