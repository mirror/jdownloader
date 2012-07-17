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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypt2.be" }, urls = { "http://(www\\.)?crypt2\\.be/file/[a-z0-9]+" }, flags = { 0 })
public class Crypt2Be extends PluginForDecrypt {

    public Crypt2Be(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String finallink = findLink();
        if (finallink == null && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            final String rcID = br.getRegex("k=([^<>\"]*?)\"").getMatch(0);
            if (rcID == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(rcID);
            for (int i = 0; i <= 5; i++) {
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, param);
                br.postPage(parameter, "submit=true&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c);
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) continue;
                break;
            }
            if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) throw new Exception(DecrypterException.CAPTCHA);
            finallink = findLink();
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    private String findLink() {
        String finallink = br.getRegex("id=\"iframe\" src=\"([^\"\\'<>]+)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("name=\"movie\" value=\"([^\"\\'<>]+)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRedirectLocation();
            }
        }
        return finallink;
    }

    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
