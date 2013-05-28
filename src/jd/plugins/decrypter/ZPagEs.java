//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.http.RandomUserAgent;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zpag.es" }, urls = { "http://(www\\.)?zpag\\.es/([a-zA-Z0-9]{4}|\\d+/.+)" }, flags = { 0 })
public class ZPagEs extends PluginForDecrypt {

    // DEVNOTES
    // [a-zA-Z0-9]{4} seems that 4 is the min and max(that could see) from googling.
    // \\d+/.+ = userid ?? + '/title'
    // raztoki

    public ZPagEs(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* must be static so all plugins share same lock */
    private static Object       LOCK         = new Object();
    private static final String LINKREGEX    = "window\\.location = \"(https?://.*?)\"";
    private static final String CAPTCHATEXT  = "google\\.com/recaptcha/api";
    private static final String CAPTCHATEXT2 = "zpag.es/cap";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String parameter = param.toString();
        br.setFollowRedirects(true);
        synchronized (LOCK) {
            br.getPage(parameter);
            if (br.containsHTML(">zPag\\.es \\- Invalid Page<")) return decryptedLinks;
            if (br.containsHTML(CAPTCHATEXT) || br.getURL().contains(CAPTCHATEXT2)) {
                for (int i = 0; i <= 5; i++) {
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.getForm().setAction("http://zpag.es/receiveRecaptcha.do");
                    rc.getForm().put("URL", parameter);
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, param);
                    rc.setCode(c);
                    if (br.containsHTML(CAPTCHATEXT) || br.getURL().contains(CAPTCHATEXT2)) continue;
                    break;
                }
                if (br.containsHTML(CAPTCHATEXT) || br.getURL().contains(CAPTCHATEXT2)) throw new DecrypterException(DecrypterException.CAPTCHA);

            }
            final String link = br.getRegex(LINKREGEX).getMatch(0);
            if (link == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            // If we don't wait we have to enter reCaptchas more often which
            // would need even more time for ;)
            sleep(3000, param);
            decryptedLinks.add(createDownloadlink(link));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}