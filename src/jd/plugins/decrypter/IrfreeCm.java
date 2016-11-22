//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.util.Set;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

import org.appwork.utils.Regex;
import org.jdownloader.controlling.PasswordUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "irfree.com" }, urls = { "https?://(www\\.)?irfree\\.(com|eu)/.+/.*" })
public class IrfreeCm extends antiDDoSForDecrypt {

    public IrfreeCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "https?://(www\\.)?irfree\\.(com|eu)//?(templates|engine|user|tutorials|images|tv\\-shows).+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString().replace("irfree.eu/", "irfree.com/");
        if (parameter.matches(INVALIDLINKS) || parameter.contains("rss.xml") || parameter.endsWith(".xml")) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        getPage(parameter);
        if (br.containsHTML("(The article cannot be found\\.|>Ooops, Error\\!<)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final Set<String> pws = PasswordUtils.getPasswords(br.toString());
        String[] links = new Regex(br.toString(), "href=\"(https?://.*?)\"", Pattern.CASE_INSENSITIVE).getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String link : links) {
            final String crypted = new Regex(link, "irfree\\.com/engine/go\\.php\\?url=([^<>\"]*?)\"").getMatch(0);
            if (crypted != null) {
                link = Encoding.Base64Decode(Encoding.htmlDecode(crypted));
            }
            if (!new Regex(link, this.getSupportedLinks()).matches()) {
                DownloadLink dLink = createDownloadlink(link);
                if (pws != null && pws.size() > 0) {
                    dLink.setSourcePluginPasswordList(new ArrayList<String>(pws));
                }
                decryptedLinks.add(dLink);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}