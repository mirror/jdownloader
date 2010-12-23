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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.RandomUserAgent;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "lof.cc" }, urls = { "http://[\\w\\.]*?(lof\\.cc|92\\.241\\.168\\.5)/[!a-zA-Z0-9_]+" }, flags = { 0 })
public class LdTTemp extends PluginForDecrypt {

    public LdTTemp(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            String parameter = param.toString();
            br.getPage(parameter);
            String waittime = br.getRegex("<p>Du musst noch (\\d+) Sekunden warten bis du").getMatch(0);
            if (waittime != null) {
                int wait = Integer.parseInt(waittime);
                if (wait > 80) {
                    logger.warning("Limit erreicht!");
                    logger.warning(br.toString());
                    return null;
                }
                sleep(wait * 1001l, param);
            }
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML("(api.recaptcha.net|Das war leider Falsch)")) continue;
                if (br.containsHTML("das Falsche Captcha eingegeben")) {
                    sleep(60 * 1001l, param);
                    br.getHeaders().put("User-Agent", RandomUserAgent.generate());
                    br.getPage(parameter);
                    continue;
                }
                break;
            }
            if (br.containsHTML("(api.recaptcha.net|Das war leider Falsch|das Falsche Captcha eingegeben)")) throw new DecrypterException(DecrypterException.CAPTCHA);
            String links[] = br.getRegex("<a href=\"(http.*?)\" target=\"_blank\" onclick=").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("First LdTTemp regex failed, trying the second one...");
                links = HTMLParser.getHttpLinks(br.toString(), "");
            }
            if (links.length == 0) return null;
            for (String finallink : links) {
                if (!finallink.contains("iload.to") && !finallink.contains("lof.cc") && !finallink.endsWith(".gif") && !finallink.endsWith(".swf")) decryptedLinks.add(createDownloadlink(finallink));
            }

            return decryptedLinks;
        }
    }

}
