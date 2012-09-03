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
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filecrop.com" }, urls = { "http://[\\w\\.]*?filecrop\\.com/\\d+/index\\.html" }, flags = { 0 })
public class FlCrpCm extends PluginForDecrypt {

    public FlCrpCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // Every IP only has to type in a captcha ONCE so the decrypter modules
        // shouldn't do this step simultan
        synchronized (LOCK) {
            br.getPage(parameter);
            if (br.containsHTML("<title>404 Not Found</title>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String captchaurl = br.getRegex("\"(/captcha\\.php\\?id=[a-z0-9]+)\"").getMatch(0);
            if (captchaurl != null) {
                for (int i = 0; i <= 3; i++) {
                    captchaurl = br.getRegex("\"(/captcha\\.php\\?id=[a-z0-9]+)\"").getMatch(0);
                    Form captchaForm = br.getForm(0);
                    if (captchaForm == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    String code = getCaptchaCode("http://www.filecrop.com" + captchaurl, param);
                    captchaForm.put("captcha", code);
                    br.submitForm(captchaForm);
                    if (br.containsHTML("(captcha.php?|red>Invalid access code)")) continue;
                    break;
                }
                if (br.containsHTML("(captcha.php?|red>Invalid access code)")) throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        }
        String[] lol = HTMLParser.getHttpLinks(br.toString(), "");
        if (lol == null || lol.length == 0) {
            logger.warning("HTML Parser Array is null...");
            return null;
        }
        for (String pwned : lol) {
            if (!pwned.equals(parameter)) decryptedLinks.add(createDownloadlink(pwned));
        }
        return decryptedLinks;
    }
}
