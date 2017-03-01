//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.keycaptcha.KeyCaptcha;

/**
 * @author raztoki
 * @author psp
 *
 *         Name: urlink.biz Address: 37.48.120.85<br/>
 *         Name: ur-link.biz Address: 37.48.120.85<br/>
 *         Name: url-ink.biz Address: 37.48.120.85 Name: url-ink.com Address: 37.48.120.85<br/>
 *         Name: urli-nk.com Address: 37.48.120.85<br/>
 *
 */
@DecrypterPlugin(revision = "$Revision: 25143 $", interfaceVersion = 2, names = { "urlink.biz" }, urls = { "https?://(?:www\\.)?(?:urlink\\.biz|ur-link\\.biz|url-ink\\.biz|url-ink\\.com|urli-nk\\.com)/[A-Za-z0-9]{4,6}" })
public class UrLnkBz extends PluginForDecrypt {

    public UrLnkBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "urlink.biz", "ur-link.biz", "url-ink.biz", "url-ink.com", "urli-nk.com" };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 400 });
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 400 || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // keycaptcha
        int counter = 0;
        final int retryCount = 4;
        String dl = null;
        while (dl == null && counter++ <= retryCount) {
            final Form captchaForm = br.getForm(0);
            if (captchaForm != null && captchaForm.containsHTML("capcode")) {
                String result = null;
                try {
                    result = handleCaptchaChallenge(new KeyCaptcha(this, br, createDownloadlink(parameter)).createChallenge(this));
                } catch (final DecrypterException d) {
                    if (DecrypterException.CAPTCHA.equals(d.getMessage())) {
                        continue;
                    }
                    throw d;
                }
                if (StringUtils.isEmpty(result)) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                }
                if ("CANCEL".equals(result)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                }
                captchaForm.put("capcode", Encoding.urlEncode(result));
                br.submitForm(captchaForm);
                // captcha form and inputfield is shown even when the correct response is given.
            }
            dl = br.getRegex("document\\.location\\.href\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
        }
        if (dl == null) {
            if (counter >= retryCount) {
                logger.info("Exceeded counter");
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            logger.info("Can't find 'dl'");
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        if (dl != null) {
            decryptedLinks.add(createDownloadlink(dl));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}