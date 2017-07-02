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

import java.util.ArrayList;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 * NOTE: <br />
 * - contains recaptchav2, and uids are not case senstive any longer -raztoki 20150427 - regex pattern seems to be case sensitive, our url
 * listener is case insensitive by default... so we need to ENFORCE case sensitivity. -raztoki 20150308 <br />
 * - uid seems to be fixed to 5 chars (at this time) -raztoki 20150308 <br />
 * - uses cloudflare -raztoki 20150308 <br />
 *
 * @author psp
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "shink.in" }, urls = { "https?://(www\\.)?shink\\.in/(s/)?(?-i)[a-zA-Z0-9]{5}" })
public class ShinkIn extends antiDDoSForDecrypt {

    private static Object CTRLLOCK = new Object();

    public ShinkIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        Form dform = null;
        // they seem to only show recaptchav2 once!! they track ip session (as restarting client doesn't get recaptchav2, the only cookies
        // that are cached are cloudflare and they are only kept in memory, and restarting will flush it)
        synchronized (CTRLLOCK) {
            getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            dform = br.getFormbyProperty("id", "skip");
            if (dform == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            // can contain recaptchav2
            if (dform.containsHTML("class=(\"|')g-recaptcha\\1")) {
                final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
                dform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            }
        }
        br.setFollowRedirects(false);
        submitForm(dform);
        // now a form
        final Form f = br.getFormbyActionRegex("/redirect/");
        if (f != null) {
            submitForm(f);
        }
        String finallink = br.getRedirectLocation();
        if (inValidate(finallink)) {
            finallink = br.getRegex("<a [^>]*href=('|\")(.*?)\\1[^>]*>GET LINK</a>").getMatch(1);
            if (inValidate(finallink)) {
                finallink = br.getRegex("<a class=('|\")\\s*btn btn-primary\\s*\\1 href=('|\")(.*?)\\2").getMatch(2);
                if (inValidate(finallink)) {

                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
            }
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

}
