//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ilix.in" }, urls = { "http://(www\\.)?(ilix\\.in|priva\\.us|urlink\\.at)/[0-9a-z]+" }, flags = { 0 })
public class IlxN extends PluginForDecrypt {

    public IlxN(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = "http://ilix.in/" + new Regex(param.toString(), "([0-9a-z]+)$").getMatch(0);
        br.setFollowRedirects(false);
        br.getPage(parameter);
        for (int i = 0; i <= 5; i++) {
            if (br.containsHTML("captcha.php")) {
                String captchalink = "http://ilix.in/captcha2/captcha.php";
                String code = getCaptchaCode(captchalink, param);
                br.postPage(parameter, "n=0&captcha=" + code);
                if (br.containsHTML("captcha.php")) continue;
                break;
            }
        }
        if (br.containsHTML("captcha.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
        // "Redirect" links handling
        String unescape = br.getRegex("document\\.write\\(unescape\\(\\'(.*?)\\'\\)").getMatch(0);
        if (unescape != null) {
            unescape = Encoding.htmlDecode(unescape);
            final String finallink = new Regex(unescape, "name=\"iframe?\" src=\"(http[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else if (br.containsHTML("src='encrypt.php'")) {
            br.getPage("http://ilix.in/encrypt.php");
            String finallink0 = br.getRegex("\\(unescape\\('(.*?)'\\)\\)").getMatch(0);
            if (finallink0 == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            finallink0 = Encoding.htmlDecode(finallink0);
            String finallink = new Regex(finallink0, "ifram\" src=\"(.*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            // Multiple/Single links handling
            String[] multipleLinks = br.getRegex("name='n' value='([0-9]{1,3})'>").getColumn(0);
            if (multipleLinks != null && multipleLinks.length != 0) {
                for (String link : multipleLinks) {
                    final Form form = new Form();
                    form.setMethod(Form.MethodType.POST);
                    form.put("n", link);
                    br.submitForm(form);
                    br.getPage("http://ilix.in/encrypt.php");
                    String finallink0 = br.getRegex("\\(unescape\\('(.*?)'\\)\\)").getMatch(0);
                    if (finallink0 == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    finallink0 = Encoding.htmlDecode(finallink0);
                    String finallink = new Regex(finallink0, "ifram\" src=\"(.*?)\"").getMatch(0);
                    if (finallink == null) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    }
                    decryptedLinks.add(createDownloadlink(finallink));
                }
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}