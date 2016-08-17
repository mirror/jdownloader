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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gca.sh" }, urls = { "http://(www\\.)?gca\\.sh/[A-Za-z0-9]+" }) 
public class GcaSh extends PluginForDecrypt {

    public GcaSh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        // german for german users, english for the rest.
        if (!"de".equalsIgnoreCase(System.getProperty("user.language"))) {
            br.setHeader("Accept-Language", "en-gb, en;q=0.8");
        }
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("id=\"captcha-dialog\"")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            final Recaptcha rc = new Recaptcha(br, this);
            /* ID 22.04.25: 6LcQedQSAAAAAH_O6lQcp-X-lrMa77g8TrNfxN-d */
            /* Params when reCaptcha is in use: last_key=9,i=fallback,captcha=captchaad,submit=Daten absenden */
            rc.findID();
            rc.load();
            final int retry = 3;
            for (int i = 0; i <= retry; i++) {
                final Browser br2 = br.cloneBrowser();
                final Form dlForm = br2.getFormbyKey("captcha");
                if (dlForm == null) {
                    return null;
                }
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, param);
                dlForm.put("recaptcha_challenge_field", rc.getChallenge());
                dlForm.put("recaptcha_response_field", Encoding.urlEncode(c));
                dlForm.put("submit", "Daten absenden");
                br2.submitForm(dlForm);
                if (br2.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    if (i + 1 == retry) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                    rc.reload();
                    continue;
                }
                br = br2;
                break;
            }
        } else if (br.containsHTML("confidenttechnologies\\.com/")) {
            // todo: review confidentialcaptcha and fix buildblock.
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // final Form con = br.getForm(0);
            // final String xy = new CaptchaHelperCrawlerPluginConfidentCaptcha(this, br).getToken();
            // final String[][] inputs = new Regex(xy, "\\[\\s*\"(.*?)\",\\s\"(.*?)\"\\s*\\]").getMatches();
            // // we have bugs, this should work around them
            // Form newForm = new Form();
            // newForm.setAction(con.getAction());
            // newForm.setMethod(con.getMethod());
            // for (final String[] input : inputs) {
            // newForm.put(input[0], Encoding.urlEncode(input[1]));
            // }
            // newForm.put("submit", "Submit");
            // // say this isn't needed.
            // br.setCookie(br.getHost(), "arp_scroll_position", "0");
            // br.submitForm(newForm);
        }

        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
