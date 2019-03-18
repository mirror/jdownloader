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
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wlnk.ec" }, urls = { "https?://(?:www\\.)?wlnk\\.ec/[A-Za-z0-9]+" })
public class WlnkEc extends antiDDoSForDecrypt {
    public WlnkEc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("text\\-center error")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = null;
        final String captchaImage = br.getRegex("<img\\s*src\\s*=\\s*\"([^\"]*?)\"\\s*onclick=").getMatch(0);
        final String captchaCode;
        if (captchaImage != null) {
            captchaCode = getCaptchaCode(captchaImage, param);
            postPage(this.br.getURL(), "submit=unlock&cr-nvar=" + Encoding.urlEncode(captchaCode.toUpperCase(Locale.ENGLISH)));
        } else {
            captchaCode = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            postPage(this.br.getURL(), "submit=unlock&captcha-response-newvar=" + Encoding.urlEncode(captchaCode.toUpperCase(Locale.ENGLISH)));
        }
        if (br.containsHTML("Captcha invalide")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final String[] links = br.getRegex("\"(http[^<>\"\\']+)\" rel=\"external nofollow\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            decryptedLinks.add(createDownloadlink(singleLink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
