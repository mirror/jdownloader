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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "protect.ddl-island.ru", "protect.emule-island.ru" }, urls = { "http://(?:www\\.)?protect\\.ddl\\-island\\.(?:ru|su)/(?:other\\?id=)?([A-Za-z0-9]+)", "http://(?:www\\.)?protect\\.emule-island\\.ru/(?:other\\?id=)?([A-Za-z0-9]+)" })
public class ProtectDdlIslandRu extends PluginForDecrypt {
    public ProtectDdlIslandRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String invalidLinks = "http://(www\\.)?" + Pattern.quote(getHost()) + "/(img|other)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        /* other?id= = force text captcha */
        final String parameter = param.toString().replace("other?id=", "");
        final String fuid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        if (parameter.matches(invalidLinks)) {
            decryptedLinks.add(createOfflinelink(parameter, fuid, null));
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getContentType().matches("(application/javascript|text/css)")) {
            decryptedLinks.add(createOfflinelink(parameter, fuid, null));
            return decryptedLinks;
        }
        // recaptchav2 can also be used
        if (br.containsHTML("<div class=\"g-recaptcha\"")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            br.postPage(br.getURL(), "g-recaptcha-response=" + Encoding.urlEncode(recaptchaV2Response) + "&submit=Valider");
        } else {
            final String captchapass = Encoding.urlEncode(generatePass());
            br.postPage("/php/Qaptcha.jquery.php", "action=qaptcha&qaptcha_key=" + captchapass);
            br.postPage(parameter, captchapass + "=&submit=Submit+form");
            if (br.containsHTML("<b>Nom :</b></td><td></td></tr>")) {
                decryptedLinks.add(createOfflinelink(parameter, fuid, null));
                return decryptedLinks;
            }
            if (!br.containsHTML("img\\.php\\?get_captcha=true")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            boolean success = false;
            for (int i = 1; i <= 3; i++) {
                final File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection("/img.php?get_captcha=true"));
                final ClickedPoint cp = getCaptchaClickedPoint(getHost(), captchaFile, param, getHost() + " | " + String.valueOf(i + 1) + "/3", null);
                br.postPage(br.getURL(), "position%5B%5D.x=" + String.valueOf(cp.getX()) + "&position%5B%5D.y=" + String.valueOf(cp.getY()));
                if (br.containsHTML("img\\.php\\?get_captcha=true")) {
                    continue;
                }
                success = true;
                break;
            }
            if (!success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        final String finallink = br.getRegex(">Lien :</b></td><td><a href=\"(http[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("We can't find finallinks, either empty container or broken plugin. Please confirm in your Web Browser: " + parameter);
            return decryptedLinks;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    private String generatePass() {
        int nb = 32;
        final String chars = "azertyupqsdfghjkmwxcvbn23456789AZERTYUPQSDFGHJKMWXCVBN_-#@";
        String pass = "";
        for (int i = 0; i < nb; i++) {
            long wpos = Math.round(Math.random() * (chars.length() - 1));
            int lool = (int) wpos;
            pass += chars.substring(lool, lool + 1);
        }
        return pass;
    }

    @Override
    public String[] siteSupportedNames() {
        if ("protect.ddl-island.ru".equalsIgnoreCase(this.getHost())) {
            return new String[] { "protect.ddl-island.ru", "protect.ddl-island.su" };
        }
        return new String[] { getHost() };
    }
}
