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
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "320k.me" }, urls = { "https?://(?:www\\.)?320k\\.(?:in|me)/index\\.php\\?surf=(viewupload(\\&groupid=\\d*)?\\&uploadid=\\d+|redirect\\&url=[A-Za-z0-9 %=]+(?:\\&uploadid=\\d+)?)" })
public class ThreehundredTwenteekMe extends antiDDoSForDecrypt {

    public ThreehundredTwenteekMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE = ".+/index\\.php\\?surf=redirect&url=[A-Za-z0-9 %=]+(?:&uploadid=\\d+)?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("320k.in/", "320k.me/");
        br.setFollowRedirects(false);
        getPage(parameter);
        if (parameter.matches(TYPE_SINGLE)) {
            final String finallink = decryptSingle(param);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (finallink.contains("320k.in/") && !finallink.contains("320k.in/referer")) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            if (br.containsHTML(">Dieser Upload ist nicht mehr") || this.br.getHttpConnection().getResponseCode() == 404) {
                final DownloadLink offline = this.createOfflinelink(parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            final String fpName = br.getRegex("<title>320k\\.me \\|([^<>\"]*?)</title>").getMatch(0);
            FilePackage fp = null;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
            }
            final String[] links = br.getRegex("\\&(?:amp;)?url=([A-Za-z0-9 %=]+)(\"|\\&)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String finallink = null;
            for (final String singleLink : links) {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return decryptedLinks;
                }
                finallink = Request.getLocation("/index.php?surf=redirect&url=" + singleLink, br.getRequest());
                final DownloadLink dl = createDownloadlink(finallink);
                if (fp != null) {
                    fp.add(dl);
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }

        return decryptedLinks;
    }

    @SuppressWarnings("deprecation")
    private String decryptSingle(final CryptedLink param) throws Exception {
        String finallink = null;
        String captcha = br.getRegex("\"(cap\\.php\\?c=[a-z0-9]+)\"").getMatch(0);
        for (int i = 0; i <= 3; i++) {
            String code = getCaptchaCode(captcha, param);
            /* Website only accepts uppercase! */
            code = code.toUpperCase();
            final String crypt = new Regex(captcha, "c=([a-z0-9]+)$").getMatch(0);
            postPage(br.getURL(), "code=" + Encoding.urlEncode(code) + "&crypt=" + crypt + "&send=Download%21");
            finallink = br.getRedirectLocation();
            if (finallink != null) {
                break;
            }
            invalidateLastChallengeResponse();
        }
        if (finallink == null && br.containsHTML("cap\\.php")) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        validateLastChallengeResponse();
        return finallink;
    }

}
