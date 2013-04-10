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
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sho.rtify.com" }, urls = { "http://(www\\.)?sho\\.rtify\\.com/\\d+" }, flags = { 0 })
public class ShoRtifyCom extends PluginForDecrypt {

    public ShoRtifyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        String plaintxt = null;
        if (br.containsHTML("<label class=\"passProtected\" for=\"thePass\">")) {
            Browser br2 = null;
            for (int i = 0; i <= 3; i++) {
                br2 = br.cloneBrowser();
                Form pwform = br2.getFormbyKey("thePassword");
                if (pwform == null) return decryptedLinks;
                String pw = getUserInput(null, param);
                pwform.put("thePassword", pw);
                br2.submitForm(pwform);
                if (!br2.containsHTML("Sorry, the password you entered was incorrect.")) break;
            }
            if (br2.containsHTML("Sorry, the password you entered was incorrect.")) throw new DecrypterException(DecrypterException.PASSWORD);
            plaintxt = br2.getRegex("<textarea.*?>(.*?)</textarea>").getMatch(0);
        } else {
            plaintxt = br.getRegex("<textarea.*?>(.*?)</textarea>").getMatch(0);
        }

        if (plaintxt == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }

        // Find all those links
        final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Quitting, no links found for link: " + parameter);
            return decryptedLinks;
        }
        ArrayList<String> pws = HTMLParser.findPasswords(plaintxt);
        logger.info("Found " + links.length + " links in total.");

        DownloadLink dl;
        for (String elem : links) {
            if (elem.contains("sho.rtify.com")) continue;
            decryptedLinks.add(dl = createDownloadlink(elem));
            if (pws != null && pws.size() > 0) dl.setSourcePluginPasswordList(pws);
        }

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}