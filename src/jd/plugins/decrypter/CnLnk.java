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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 32927 $", interfaceVersion = 2, names = { "coinlink.co" }, urls = { "https?://(?:www\\.)?coinlink\\.co/[A-Za-z0-9]+" }, flags = { 0 })
public class CnLnk extends PluginForDecrypt {

    public CnLnk(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form form = br.getForm(0);
        if (form == null) {
            return null;
        }
        // captcha here
        if (form.hasInputFieldByName("captcha")) {
            final String code = getCaptchaCode("cp.php", param);
            form.put("captcha", Encoding.urlEncode(code));
        }
        br.submitForm(form);
        if (br.containsHTML("<script>alert\\('(?:Empty Captcha|Incorrect Captcha)\\s*!'\\);")) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        form = br.getForm(0);
        if (form == null) {
            return null;
        }
        // we want redirect off here
        br.setFollowRedirects(false);
        br.submitForm(form);
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            if (br.containsHTML("<script>alert\\('(?:Link not found)\\s*!'\\);")) {
                // invalid link
                logger.warning("Invalid link : " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}