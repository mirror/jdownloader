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
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ld.rtify.com" }, urls = { "http://(www\\.)?ld\\.rtify\\.com/\\d+" }, flags = { 0 })
public class LdRtifyCom extends PluginForDecrypt {

    public LdRtifyCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        String plaintxt = null;
        if (br.containsHTML("Enter the password")) {
            for (int i = 0; i <= 3; i++) {
                Form pwform = br.getFormbyKey("password");
                if (pwform == null) return decryptedLinks;
                String pw = getUserInput(null, param);
                pwform.put("password", pw);
                br.submitForm(pwform);
                if (!br.containsHTML("Enter the password")) break;
            }
            if (br.containsHTML("Enter the password")) throw new DecrypterException(DecrypterException.PASSWORD);
        }

        plaintxt = br.getRegex("<pre.*?>(.*?)</pre>").getMatch(0);
        if (plaintxt == null) return decryptedLinks;

        // Find all those links
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) return null;
        ArrayList<String> pws = HTMLParser.findPasswords(plaintxt);
        logger.info("Found " + links.length + " links in total.");

        DownloadLink dl;
        for (String elem : links) {
            if (elem.contains("ld.rtify.com")) continue;
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