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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gpaste.us" }, urls = { "https?://(?:www\\.)?gpaste\\.us/[a-z0-9]+" })
public class GpasteUs extends PluginForDecrypt {

    public GpasteUs(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String html_pwprotected = "class=\"icon icon\\-unlock\"";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Typically with redirect to --> "/404" */
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        int counter = 0;
        while (this.br.containsHTML(html_pwprotected) && counter <= 2) {
            final String passCode = getUserInput("Enter password", param);
            this.br.postPage(this.br.getURL(), "password=" + Encoding.urlEncode(passCode));
            counter++;
        }
        if (this.br.containsHTML(html_pwprotected)) {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }

        String fpName = br.getRegex("class=\"heading\">([^<>]+)<").getMatch(0);
        String sourcehtml = this.br.getRegex("<div[^<>]*?class=\"overthrow content\"[^<>]*?>(.*?)</div>").getMatch(0);
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
