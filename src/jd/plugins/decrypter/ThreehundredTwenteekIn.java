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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "320k.in" }, urls = { "http://(www\\.)?320k\\.in/index\\.php\\?surf=(viewupload(\\&groupid=\\d+)?\\&uploadid=\\d+|redirect\\&url=[A-Za-z0-9 %=]+\\&uploadid=\\d+)" }, flags = { 0 })
public class ThreehundredTwenteekIn extends PluginForDecrypt {

    public ThreehundredTwenteekIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE = "http://(www\\.)?320k\\.in/index\\.php\\?surf=redirect\\&url=[A-Za-z0-9 %=]+\\&uploadid=\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (parameter.matches(TYPE_SINGLE)) {
            final String finallink = decryptSingle(param);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (finallink.contains("320k.in/") && !finallink.contains("320k.in/referer")) {
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            if (br.containsHTML(">Dieser Upload ist nicht mehr")) {
                logger.info("Link offline: " + parameter);
                final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            final String fpName = br.getRegex("<title>320k\\.in \\|([^<>\"]*?)</title>").getMatch(0);
            final String[] links = br.getRegex("\\&(?:amp;)?url=([A-Za-z0-9 %=]+)(\"|\\&)").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String finallink = null;
            for (final String singleLink : links) {
                br.getPage("http://320k.in/index.php?surf=redirect&url=" + singleLink);
                finallink = decryptSingle(param);
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (finallink.contains("320k.in/") && !finallink.contains("320k.in/referer")) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }

            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }

        return decryptedLinks;
    }

    private String decryptSingle(final CryptedLink param) throws Exception {
        String finallink = null;
        String captcha = br.getRegex("\"(cap\\.php\\?c=[a-z0-9]+)\"").getMatch(0);
        for (int i = 0; i <= 3; i++) {
            try {
                if (this.isAbort()) {
                    logger.info("Decryption aborted by user");
                    return null;
                }
            } catch (final Throwable e) {
                // Not available in old 0.9.581 Stable
            }
            final String code = getCaptchaCode(captcha, param);
            final String crypt = new Regex(captcha, "c=([a-z0-9]+)$").getMatch(0);
            br.postPage(br.getURL(), "code=" + Encoding.urlEncode(code) + "&crypt=" + crypt + "&send=Download%21");
            finallink = br.getRedirectLocation();
            if (finallink != null) {
                break;
            }
        }
        if (finallink == null && br.containsHTML("cap\\.php")) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        return finallink;
    }

}
