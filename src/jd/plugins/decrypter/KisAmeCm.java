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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 *
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision: 20515 $", interfaceVersion = 3, names = { "kissanime.com", "kissasian.com", "kisscartoon.me" }, urls = { "https?://(?:www\\.)?kissanime\\.(?:com|to)/anime/[a-zA-Z0-9\\-\\_]+/[a-zA-Z0-9\\-\\_]+(?:\\?id=\\d+)?", "http://kissasian\\.com/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?", "http://kisscartoon\\.me/[^/]+/[A-Za-z0-9\\-]+/[^/]+(?:\\?id=\\d+)?" }) 
public class KisAmeCm extends antiDDoSForDecrypt {

    public KisAmeCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        /* Error handling */
        if (br.containsHTML("Page Not Found") || br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final Form ruh = br.getFormbyAction("/Special/AreYouHuman");
        // recaptchav2 event can happen here
        if (br.containsHTML("<title>\\s*Are You Human\\s*</title>") || ruh != null) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            ruh.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(ruh);
        }
        String title = br.getRegex("<title>\\s*(.*?)\\s*- Watch\\s*\\1[^<]*</title>").getMatch(0);
        if (title == null) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        title = title.replaceAll("\\s+", " ");
        // we have two things we need to base64decode
        final String qualityselection = br.getRegex("<select id=\"selectQuality\">.*?</select").getMatch(-1);
        if (qualityselection != null) {
            final String[][] quals = new Regex(qualityselection, "<option [^>]*value\\s*=\\s*('|\"|)(.*?)\\1[^>]*>(\\d+p)").getMatches();
            if (quals != null) {
                for (final String qual[] : quals) {
                    String decode = Encoding.Base64Decode(qual[1]);
                    if (StringUtils.contains(decode, "blogspot.com/")) {
                        // this is redirect bullshit
                        final Browser test = new Browser();
                        test.getPage(decode);
                        decode = test.getRedirectLocation();
                    }
                    DownloadLink dl = createDownloadlink(decode);
                    dl.setFinalFileName(title + "-" + qual[2] + ".mp4");
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                    decryptedLinks.add(dl);
                }
            }
        } else {
            // iframed.. seen openload.. but maybe others
            final String link = br.getRegex("\\$\\('#divContentVideo'\\)\\.html\\('<iframe\\s+[^>]* src=\"(.*?)\"").getMatch(0);
            if (link != null) {
                decryptedLinks.add(createDownloadlink(link));
            }

        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}