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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gca.sh" }, urls = { "http://(www\\.)?gca\\.sh/[A-Za-z0-9]+" }, flags = { 0 })
public class GcaSh extends PluginForDecrypt {

    public GcaSh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("id=\"captcha-dialog\"")) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        boolean captchafailed = true;
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        /* ID 22.04.25: 6LcQedQSAAAAAH_O6lQcp-X-lrMa77g8TrNfxN-d */
        /* Params when reCaptcha is in use: last_key=9,i=fallback,captcha=captchaad,submit=Daten absenden */
        rc.findID();
        rc.load();
        for (int i = 0; i <= 5; i++) {
            final Form dlForm = br.getFormbyKey("captcha");
            if (dlForm == null) {
                return null;
            }
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode("recaptcha", cf, param);
            dlForm.put("recaptcha_challenge_field", rc.getChallenge());
            dlForm.put("recaptcha_response_field", Encoding.urlEncode(c));
            dlForm.put("submit", "Daten absenden");
            br.submitForm(dlForm);
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                rc.reload();
                continue;
            }
            captchafailed = false;
            break;
        }
        if (captchafailed) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

}
