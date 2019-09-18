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
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.controlling.PasswordUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pasted.co" }, urls = { "https?://(?:www\\.)?(?:tinypaste\\.com|tny\\.cz|pasted\\.co|controlc\\.com)/(?!tools|terms|api|contact|login|register|press)([0-9a-z]+|.*?id=[0-9a-z]+)" })
public class PastedCo extends PluginForDecrypt {
    public PastedCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String domains = "(?:tinypaste\\.com|tny\\.cz|pasted\\.co|controlc\\.com)";

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String link = parameter.toString().replaceFirst("tinypaste\\.com/|tny\\.cz/|pasted.co/", "controlc.com");
        br.getPage(link);
        if (br.containsHTML(">404 - URL not found<|>The content has either been deleted|>Paste deleted<|>There does not seem to be anything here") || this.br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("(Enter the correct password|has been password protected)")) {
            for (int i = 0; i <= 3; i++) {
                String id = new Regex(link, domains + "/.*?id=([0-9a-z]+)").getMatch(0);
                if (id == null) {
                    id = new Regex(link, domains + "/([0-9a-z]+)").getMatch(0);
                }
                final Form pwform = br.getForm(0);
                if (pwform == null || id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String pw = getUserInput(null, parameter);
                pwform.put("password_" + id, pw);
                br.submitForm(pwform);
                if (br.containsHTML("(Enter the correct password|has been password protected)")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("(Enter the correct password|has been password protected)")) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String pasteFrame = br.getRegex("frameborder='0'\\s*id='pasteFrame'\\s*src\\s*=\\s*\"(https?://(?:www\\.)?" + domains + "/.*?)\"").getMatch(0);
        if (pasteFrame == null) {
            pasteFrame = br.getRegex("\"(https?://(?:www\\.)?" + domains + "/[a-z0-9]+/fullscreen\\.php\\?hash=[a-z0-9]+\\&linenum=(false|true))\"").getMatch(0);
        }
        if (pasteFrame == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(pasteFrame.trim());
        String[] links = HTMLParser.getHttpLinks(br.toString(), null);
        if (links == null || links.length == 0) {
            return decryptedLinks;
        }
        final Set<String> pws = PasswordUtils.getPasswords(br.toString());
        for (String element : links) {
            /* prevent recursion */
            if (element.matches("(?i-).+" + domains + ".+")) {
                continue;
            } else {
                final DownloadLink dl = createDownloadlink(element);
                if (pws != null && pws.size() > 0) {
                    dl.setSourcePluginPasswordList(new ArrayList<String>(pws));
                }
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}