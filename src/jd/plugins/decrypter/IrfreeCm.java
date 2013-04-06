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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "irfree.com" }, urls = { "http://(www\\.)?irfree\\.(com|eu)/.+/.*" }, flags = { 0 })
public class IrfreeCm extends PluginForDecrypt {

    public IrfreeCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://(www\\.)?irfree\\.(com|eu)//?(templates|applications|engine|user|tutorials|images|tv\\-shows).+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> passwords;
        br.setFollowRedirects(true);
        String parameter = param.toString().replace("irfree.eu/", "irfree.com/");
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        br.getPage(parameter);
        if (br.containsHTML("(The article cannot be found\\.|>Ooops, Error\\!<)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        passwords = HTMLParser.findPasswords(br.toString());
        String[] links = new Regex(br.toString(), "<a href=\"(http://.*?)\"", Pattern.CASE_INSENSITIVE).getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String link : links) {
            if (!new Regex(link, this.getSupportedLinks()).matches() && DistributeData.hasPluginFor(link, true)) {
                DownloadLink dLink = createDownloadlink(link);
                if (passwords != null && passwords.size() > 0) dLink.setSourcePluginPasswordList(passwords);
                decryptedLinks.add(dLink);
            }
        }
        return decryptedLinks;
    }

}
