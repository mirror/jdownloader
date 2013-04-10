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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lixy.in" }, urls = { "http://[\\w\\.]*?lixy\\.in/-[0-9]+" }, flags = { 0 })
public class LxyN extends PluginForDecrypt {

    public LxyN(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This is just another redirector decrypter
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("404 No Page Found")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        Form cryptform = br.getForm(0);
        if (cryptform == null) return null;
        if (!br.containsHTML("code\\.php\\?id=")) {
            br.submitForm(cryptform);
        } else {
            boolean failed = true;
            for (int i = 0; 0 <= 3; i++) {
                cryptform.put("code", getCaptchaCode("http://lixy.in/code.php?id=" + new Regex(parameter, "lixy\\.in/-(\\d+)").getMatch(0), param));
                br.submitForm(cryptform);
                if (br.containsHTML("Code Incorrect<br>")) {
                    br.getPage(parameter);
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String finallink = br.getRedirectLocation();
        if (finallink == null) return null;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}