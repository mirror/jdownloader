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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "apptrackr.org" }, urls = { "http://(www\\.)?apptrackr\\.(org|cd)/s/\\?url=[A-Za-z0-9_\\-]+" }, flags = { 0 })
public class ApTrackrOrg extends PluginForDecrypt {

    public ApTrackrOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String site = new Regex(parameter, "https?://([^/]+)").getMatch(0);
        br.setFollowRedirects(false);
        br.getPage(parameter);
        for (int i = 0; i <= 5; i++) {
            String captchaLink = br.getRegex("\"(https?://" + site + "/captcha\\.php\\?captchakey=[A-Za-z0-9_\\-]+)\"").getMatch(0);
            if (captchaLink == null) captchaLink = br.getRegex("<img width=\"140px\" src=\"(https?://" + site + "/[^<>\"\\']+)\"").getMatch(0);
            String captchaKey = br.getRegex("<input type=\"hidden\" value=\"([A-Za-z0-9_\\-]+)\"").getMatch(0);
            if (captchaKey == null) captchaKey = br.getRegex("captcha\\.php\\?captchakey=([A-Za-z0-9_\\-]+)\"").getMatch(0);
            if (captchaLink == null || captchaKey == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String code = getCaptchaCode(captchaLink, param);
            br.postPage(parameter + "&verify=true", "response=" + code + "&captcha_key=" + captchaKey);
            if (br.containsHTML("/captcha\\.php\\?captchakey=")) continue;
            break;
        }
        if (br.containsHTML("/captcha\\.php\\?captchakey=")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String finallink = br.getRedirectLocation();
        if (finallink == null) {
            // apptrackr.cd
            String blah = br.getHttpConnection().getHeaderField("refresh");
            if (blah != null) finallink = Encoding.urlDecode(new Regex(blah, "\\?url=(.+)").getMatch(0), false);
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (!finallink.contains(site)) decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

}
