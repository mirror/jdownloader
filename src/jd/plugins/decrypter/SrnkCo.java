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
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "srnk.co" }, urls = { "https?://srnk\\.co/i/[A-Za-z0-9]+" }) 
public class SrnkCo extends PluginForDecrypt {

    public SrnkCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static Object LOCK = new Object();

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        synchronized (LOCK) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            final String parameter = param.toString();
            final String fid = parameter.substring(parameter.lastIndexOf("/") + 1);
            this.br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/i/")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final Form form = br.getForm(0);
            if (form == null) {
                return null;
            }
            try {
                this.br.cloneBrowser().getPage("/js/ads.js");
                this.br.cloneBrowser().getPage("/js/ads.js");
            } catch (final Throwable e) {
            }
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            form.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(form);
            /* We can skip this one request */
            // this.br.postPage(this.br.getURL(),
            // "_method=post&authenticity_token=");
            this.br.setCookie(this.br.getHost(), "noadvtday", "0");
            br.postPage("http://srnk.co/i/" + fid + ".js", "");
            final String finallink = this.br.getRegex("var link = \"(http[^<>\"]+)\";").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));

            return decryptedLinks;
        }
    }

}
