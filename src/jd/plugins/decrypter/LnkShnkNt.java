//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

/**
 * Earn money sharing shrinked links<br />
 * Shrink URLs and earn money with Linkshrink.net<br />
 *
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkshrink.net" }, urls = { "https?://(www\\.)?linkshrink\\.net/[A-Za-z0-9]{6}" }, flags = { 0 })
public class LnkShnkNt extends PluginForDecrypt {

    public LnkShnkNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_invalid = "https?://(www\\.)?linkshrink\\.net/(report|login)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(parameter)) {
            logger.warning("Invalid Link!");
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 404 || br.getURL().matches(type_invalid)) {
            logger.warning("Invalid Link!");
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        if (br.containsHTML("api\\.solvemedia\\.com/papi")) {
            /* This part was coded blindly! */
            for (int i = 0; i <= 3; i++) {
                final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                final jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                File cf = null;
                try {
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                } catch (final Exception e) {
                    if (jd.plugins.decrypter.LnkCrptWs.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                        throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                    }
                    throw e;
                }
                final String code = getCaptchaCode(cf, param);
                final String chid = sm.getChallenge(code);
                br.postPage(br.getURL(), "adcopy_response=manual_challenge&adcopy_challenge=" + Encoding.urlEncode(chid));
                if (br.containsHTML("api\\.solvemedia\\.com/papi")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("api\\.solvemedia\\.com/papi")) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
        }
        final String continu = br.getRegex("href=\"([^\"]+)\">Continue").getMatch(0);
        if (continu == null) {
            return null;
        }
        br.getPage(continu);
        final String link = br.getRedirectLocation();
        if (link != null) {
            decryptedLinks.add(createDownloadlink(link));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}