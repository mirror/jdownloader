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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tny.cz" }, urls = { "http://(www\\.)?(tinypaste\\.com|tny\\.cz)/(?!tools|terms|api|contact|login|register|press|index|getpaid|https|public)([0-9a-z]+|.*?id=[0-9a-z]+)" }, flags = { 0 })
public class Tnypst extends PluginForDecrypt {

    private DownloadLink dl = null;

    public Tnypst(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String link = parameter.toString().replace("tinypaste.com/", "tny.cz/");
        br.getPage(link);
        if (br.containsHTML("(Hello, my name is 404\\!<|The page you requested is no longer here)")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("(Enter the correct password|has been password protected)")) {
            for (int i = 0; i <= 3; i++) {
                String id = new Regex(link, "tny\\.cz/.*?id=([0-9a-z]+)").getMatch(0);
                if (id == null) id = new Regex(link, "tny\\.cz/([0-9a-z]+)").getMatch(0);
                Form pwform = br.getForm(0);
                if (pwform == null || id == null) return null;
                String pw = getUserInput(null, parameter);
                pwform.put("password_" + id, pw);
                br.submitForm(pwform);
                if (br.containsHTML("(Enter the correct password|has been password protected)")) continue;
                break;
            }
            if (br.containsHTML("(Enter the correct password|has been password protected)")) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        String pasteFrame = br.getRegex("frameborder=\\'0\\' id=\\'pasteFrame\\' src=\"(http://tny\\.cz/.*?)\"").getMatch(0);
        if (pasteFrame == null) pasteFrame = br.getRegex("\"(http://tny\\.cz/[a-z0-9]+/fullscreen\\.php\\?hash=[a-z0-9]+\\&linenum=(false|true))\"").getMatch(0);
        if (pasteFrame == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage(pasteFrame.trim());
        String[] links = HTMLParser.getHttpLinks(br.toString(), null);
        if (links == null || links.length == 0) return decryptedLinks;
        ArrayList<String> pws = HTMLParser.findPasswords(br.toString());
        for (String element : links) {
            /* prevent recursion */
            if (element.contains("tny.cz")) continue;
            decryptedLinks.add(dl = createDownloadlink(element));
            if (pws != null && pws.size() > 0) dl.setSourcePluginPasswordList(pws);
        }
        return decryptedLinks;
    }
}
