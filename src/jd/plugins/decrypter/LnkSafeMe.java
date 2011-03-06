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
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linksafe.me" }, urls = { "http://(www\\.)?linksafe\\.me/(d|p)/[a-z0-9]+" }, flags = { 0 })
public class LnkSafeMe extends PluginForDecrypt {

    public LnkSafeMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PASSWORDFAILED  = ">Bad Password, try again";
    private static final String RECAPTCHAFAILED = "incorrect-captcha-sol";
    private static final String LINKREGEX       = "(d/[a-z0-9]+)(<|\")";
    private static final String MAINPAGE        = "http://linksafe.me/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        if (parameter.contains("/d/")) {
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                finallink = br.getRegex("window.location=\"(http:.*?)\"").getMatch(0);
            }
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            for (int i = 0; i <= 5; i++) {
                String postData = "";
                if (br.containsHTML("api\\.recaptcha\\.net") || br.containsHTML("google\\.com/recaptcha/api/")) {
                    PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                    jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, param);
                    postData = "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c;
                } else if (br.containsHTML("ajax-fc-container")) {
                    Browser br2 = br.cloneBrowser();
                    br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br2.getPage("http://linksafe.me/captcha/4/captcha.php");
                    String response = br2.toString();
                    if (response.length() > 500) {
                        logger.warning("Response string is too big...");
                        return null;
                    }
                    postData = "captcha=" + response.trim();
                }
                if (br.containsHTML("valign=\"top\">Password:")) {
                    String passCode = getUserInput(null, param);
                    postData = postData + "&password=" + passCode;
                }
                if (!postData.equals("")) br.postPage(parameter, postData + "&postcheck=1");
                if (br.containsHTML(RECAPTCHAFAILED) || br.containsHTML(PASSWORDFAILED)) continue;
                break;
            }
            if (br.containsHTML(RECAPTCHAFAILED) && br.containsHTML(PASSWORDFAILED)) {
                logger.info("Wrong password and wrong captcha!");
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            if (br.containsHTML(RECAPTCHAFAILED)) throw new DecrypterException(DecrypterException.CAPTCHA);
            if (br.containsHTML(PASSWORDFAILED)) throw new DecrypterException(DecrypterException.PASSWORD);
            String[] links = br.getRegex(LINKREGEX).getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String dl : links) {
                br.getPage(MAINPAGE + dl);
                String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    finallink = br.getRegex("window.location=\"(http:.*?)\"").getMatch(0);
                }
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink(finallink));
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }

}
