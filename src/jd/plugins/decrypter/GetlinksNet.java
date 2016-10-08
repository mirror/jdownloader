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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "getlinks.net" }, urls = { "https?://(?:www\\.)?getlinks\\.net/(web/folder/view/\\d+|web/folder/follow/\\d+)" })
public class GetlinksNet extends PluginForDecrypt {

    public GetlinksNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE = "https?://(?:www\\.)?getlinks\\.net/web/folder/follow/\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(false);
        if (parameter.matches(TYPE_SINGLE)) {
            final DownloadLink dl = decryptSingle(parameter);
            if (dl == null) {
                return null;
            }
            decryptedLinks.add(dl);
        } else {
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            Form freeform = this.br.getFormbyKey("_csrf");
            if (freeform == null) {
                freeform = this.br.getForm(0);
            }
            if (freeform == null) {
                return null;
            }
            final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br).getToken();
            freeform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            this.br.submitForm(freeform);
            String fpName = br.getRegex("<h1><center>([^<>]+)<").getMatch(0);
            final String[] links = br.getRegex("(/web/folder/follow/\\d+)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                final DownloadLink dl = decryptSingle(singleLink);
                if (dl == null) {
                    return null;
                }
                decryptedLinks.add(dl);
            }

            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    private DownloadLink decryptSingle(final String url) throws IOException {
        this.br.getPage(url);
        DownloadLink dl = null;
        final String finallink = this.br.getRedirectLocation();
        if (finallink != null) {
            dl = this.createDownloadlink(finallink);
        }
        return dl;
    }

}
