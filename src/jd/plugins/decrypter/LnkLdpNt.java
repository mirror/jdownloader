//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

/**
 * @captcha recaptchav1
 * @antiddos cloudflare
 * @author User
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "link.ligadepeliculas.net" }, urls = { "http://link\\.ligadepeliculas\\.net/(?:index\\.php)?\\?v=(\\d+)" }) 
public class LnkLdpNt extends antiDDoSForDecrypt {

    public LnkLdpNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private Browser br2 = null;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("404 Not Found<|Sorry, the page could not be found")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        handleCaptcha(param);
        addLinks(decryptedLinks, parameter);
        final String fpName = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void addLinks(final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws PluginException {
        final String linksText = br2.getRegex("<h3>(.*?)</div>\\s*<center>").getMatch(0);
        if (linksText == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] links = HTMLParser.getHttpLinks(linksText, "");
        for (String singleLink : links) {
            if (!StringUtils.containsIgnoreCase(Request.getLocation(singleLink, br2.getRequest()), this.getHost())) {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
    }

    private void handleCaptcha(final CryptedLink param) throws Exception {
        br2 = br.cloneBrowser();
        final int retry = 3;
        Form captcha = br2.getForm(0);
        for (int i = 1; i < retry; i++) {
            if (captcha == null) {
                return;
            }
            if (br2.containsHTML("google\\.com/recaptcha")) {
                // recaptcha v1
                final Recaptcha rc = new Recaptcha(br2, this);
                rc.findID();
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, param);
                captcha.put("recaptcha_challenge_field", rc.getChallenge());
                captcha.put("recaptcha_response_field", Encoding.urlEncode(c));
                submitForm(br2, captcha);
                if (br2.containsHTML("google\\.com/recaptcha")) {
                    if (i + 1 > retry) {
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    } else {
                        captcha = br2.getForm(0);
                        continue;
                    }
                }
            }
            break;
        }
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

}