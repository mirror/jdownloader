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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
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
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        boolean valid = false;
        br.getPage(parameter);
        for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
            String captchaUrl = br.getRegex("<img src=\"(captcha/captcha\\.php\\?id=[\\d]+)\"").getMatch(0);

            File captchaFile = this.getLocalCaptchaFile(this);
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://uu.canna.to/cpuser/" + captchaUrl));
            String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
            Form captchaForm = br.getFormbyProperty("name", "download_form");
            captchaForm.put("sicherheitscode", captchaCode);
            br.submitForm(captchaForm);

            if (br.containsHTML("Der Sicherheitscode ist falsch!")) {
                /* Falscher Captcha, Seite neu laden */
                br.getPage(parameter);
            } else {
                valid = true;
                decryptedLinks.add(createDownloadlink(br.getRegex("URL=(.*?)\"").getMatch(0)));
                String links[] = br.getRegex("<a target=\"_blank\" href=\"(.*?)\">").getColumn(0);
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                }
                break;
            }
        }
        if (valid == false) throw new DecrypterException("Wrong Captcha Code");
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
