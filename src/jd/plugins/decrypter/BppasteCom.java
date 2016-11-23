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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bppaste.com" }, urls = { "https?://(?:www\\.)?bppaste\\.com/(?:login\\.php\\?id=)?\\d+" })
public class BppasteCom extends PluginForDecrypt {

    public BppasteCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String urlstring_pwprotected = "login.php";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || (this.br.containsHTML("id=\\'error\\'") && this.br.containsHTML("Error\\! este codigo de paste no existe"))) {
            /* E.g. <div id='error' style='display:block;'>Error! este codigo de paste no existe</div> */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final String getURL = this.br.getURL();
        int counter = 0;
        while (this.br.getURL().contains(urlstring_pwprotected) && counter <= 2) {
            final String passCode = getUserInput("Enter password", param);
            this.br.getPage(getURL + "&submit=Aceptar&password=" + Encoding.urlEncode(passCode));
            counter++;
        }
        if (this.br.getURL().contains(urlstring_pwprotected)) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }

        String fpName = br.getRegex("class=\"textbox1\">([^<>]+)<").getMatch(0);
        String sourcehtml = this.br.getRegex("<div[^<>]*?class=\\'textbox2\\'[^<>]*?>(.*?)</div>").getMatch(0);
        if (sourcehtml == null) {
            /* Fallback */
            sourcehtml = this.br.toString();
        }
        final String[] links = HTMLParser.getHttpLinks(sourcehtml, null);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (new Regex(singleLink, this.getSupportedLinks()).matches()) {
                continue;
            }
            decryptedLinks.add(createDownloadlink(singleLink));
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
