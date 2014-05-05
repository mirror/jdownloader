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
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cpvlink.com" }, urls = { "http://(www\\.)?shr77\\.com/[A-Za-z0-9]+" }, flags = { 0 })
public class CpvLinkCom extends PluginForDecrypt {

    public CpvLinkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        String finallink = null;
        final String rcid = br.getRegex("recaptcha/api/challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcid != null) {
            final String lid = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcid);
            rc.load();
            for (int i = 1; i <= 5; i++) {
                final String reid = br.getRegex("name=\"recaptcha_id\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
                final String urlid = br.getRegex("name=\"url_id\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
                String token = br.getRegex("name=\"_token\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
                if (reid == null || urlid == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (token == null) token = "";
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, param);
                final String postData = "_token=" + token + "&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&url_id=" + urlid + "&url_url=" + lid + "&recaptcha_id=" + reid;
                br.postPage("http://shr77.com/recaptcha", postData);
                handleContinueLink();
                if (br.containsHTML("404 \\- Page Not Found")) continue;
                final String redirect = br.getRedirectLocation();
                if (redirect == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (redirect.contains("shr77.com/")) continue;
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) continue;
                finallink = redirect;
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new DecrypterException(DecrypterException.CAPTCHA);
        } else {
            handleContinueLink();
            // No-ad link
            finallink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+(\\.\\d+)?; ?url=(http[^<>\"]*?)\"").getMatch(1);
            if (finallink == null) finallink = br.getRegex("\\$\\(\\'a#loading\\'\\)\\.attr\\(\\'href\\',\"(htt[^<>\"]*?)\"\\)").getMatch(0);
            // ad-link
            if (finallink == null) finallink = br.getRegex("class=\"head\\-button\">[\t\n\r ]+<a href=\"(http[^<>\"]*?)\"").getMatch(0);
            if (finallink == null || finallink.contains("shr77.com/")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    private void handleContinueLink() throws IOException {
        for (int i = 1; i <= 3; i++) {
            String continuelink = br.getRegex("window\\.location\\.replace\\(\"(http://shr77\\.com/[A-Za-z0-9]+)\"\\);").getMatch(0);
            if (continuelink == null) continuelink = br.getRegex("http\\-equiv=\"refresh\" content=\"\\d+(\\.\\d+)?; url=(http://shr77\\.com/[A-Za-z0-9]+)\"").getMatch(1);
            if (continuelink == null) continuelink = br.getRegex("Redirecting to <a href=\"(http://shr77\\.com/[A-Za-z0-9]+)\"").getMatch(0);
            if (continuelink != null)
                br.getPage(continuelink);
            else
                break;
        }
    }

}
