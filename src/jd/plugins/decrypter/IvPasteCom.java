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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ivpaste.com" }, urls = { "http://(www\\.)?ivpaste\\.com/(v/|view\\.php\\?id=)[A-Za-z0-9]+" }, flags = { 0 })
public class IvPasteCom extends PluginForDecrypt {

    public IvPasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHAFAILED = "(The reCAPTCHA wasn\\'t entered correctly\\.|Go back and try it again\\.)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        br.getPage("http://ivpaste.com/p/" + new Regex(parameter, "ivpaste\\.com/(v/|view\\.php\\?id=)([A-Za-z0-9]+)").getMatch(1));
        if (br.containsHTML("<b>Acceda desde: <a")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("api\\.recaptcha\\.net") || br.containsHTML("google\\.com/recaptcha/api/")) {
            boolean failed = true;
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML(RECAPTCHAFAILED)) {
                    br.getPage(parameter);
                    continue;
                }
                failed = false;
                break;
            }
            if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String[] links = br.getRegex("<a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links) {
            if (!dl.matches("http://(www\\.)?ivpaste\\.com/(v/|view\\.php\\?id=)[A-Za-z0-9]+")) decryptedLinks.add(createDownloadlink(dl));
        }
        return decryptedLinks;
    }

}
