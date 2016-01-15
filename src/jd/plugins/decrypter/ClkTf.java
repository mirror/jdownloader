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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 32094 $", interfaceVersion = 3, names = { "click.tf" }, urls = { "http://click\\.tf/[a-zA-Z0-9]{8,}(/.+)?" }, flags = { 0 })
public class ClkTf extends PluginForDecrypt {

    public ClkTf(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        // some links are delivered by redirects!!
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String finallink = br.getRedirectLocation();
        if (finallink != null) {
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }
        // there could be captcha
        String fpName = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        if (fpName != null) {
            // remove /
            fpName = fpName.substring(1);
        }
        handleCaptcha(param);
        addLinks(decryptedLinks, parameter);

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void addLinks(final ArrayList<DownloadLink> decryptedLinks, final String parameter) throws PluginException {
        // weird they show it in another form final action!
        final Form f = br.getForm(0);
        if (f == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String link = f.getAction();
        if (link == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(link));
    }

    private void handleCaptcha(final CryptedLink param) throws Exception {
        final int retry = 3;
        Form captcha = br.getForm(0);
        for (int i = 1; i < retry; i++) {
            if (captcha == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String captchaImage = captcha.getRegex("/captcha\\.php\\?cap_id=\\d+").getMatch(-1);
            if (captchaImage != null) {
                final String c = getCaptchaCode(captchaImage, param);
                captcha.put("ent_code", Encoding.urlEncode(c));
            }
            br.submitForm(captcha);
            if (br.containsHTML("<p style='color:\\s*red;'>Wrong CAPTCHA</p>")) {
                if (i + 1 > retry) {
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                } else {
                    captcha = br.getForm(0);
                    continue;
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