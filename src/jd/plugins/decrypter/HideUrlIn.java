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

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hideurl.in" }, urls = { "http://(www\\.)?hideurl\\.in/(check\\.[a-z0-9]+|[a-z0-9]+\\-[a-z0-9\\-_]+)\\.html" }, flags = { 0 })
public class HideUrlIn extends PluginForDecrypt {

    public HideUrlIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<td style=\\'border:1px;font\\-weight:bold;font\\-size:90%;font\\-family:Arial,Helvetica,sans\\-serif;\\'>([^<>\"]*?)</td>").getMatch(0);
        final String lid = new Regex(br.getURL(), "hideurl\\.in/check\\.([a-z0-9]+)\\.html").getMatch(0);
        final String rcID = br.getRegex("challenge\\?k=([^<>\"]*?)\"").getMatch(0);
        if (rcID == null || lid == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(rcID);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, param);
            String postData = "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&linkid=" + lid + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100);
            if (br.containsHTML("<h2>Password:</h2>")) {
                final String passCode = getUserInput("Please enter password for: " + parameter, param);
                postData += "&password=" + passCode;
            }
            br.postPage("http://hideurl.in/linkid.php", postData);
            if (br.containsHTML("google\\.com/recaptcha/")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("google\\.com/recaptcha/")) throw new DecrypterException(DecrypterException.CAPTCHA);
        final String linktext = br.getRegex("<table width=\\'100%\\' align=\\'MIDDLE\\' valign=\\'MIDDLE\\'>  <tr>(.*?)</table>").getMatch(0);
        if (linktext == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String[] links = HTMLParser.getHttpLinks(linktext, "");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links)
            decryptedLinks.add(createDownloadlink(singleLink));
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
