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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xfast.link" }, urls = { "https?://(?:www\\.)?xfast\\.link/[A-Za-z0-9]+" })
public class XfastLink extends antiDDoSForDecrypt {

    public XfastLink(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("he entered link does not exist")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        Form dlform = br.getFormbyKey("form__token");
        if (dlform == null) {
            dlform = br.getForm(0);
        }
        if (dlform == null) {
            return null;
        }
        if (br.containsHTML("g-recaptcha")) {
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            dlform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
        }
        br.setFollowRedirects(false);
        submitForm(dlform);
        final String finallink = br.getRedirectLocation();
        if (finallink == null || finallink.contains(this.getHost() + "/")) {
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

}
