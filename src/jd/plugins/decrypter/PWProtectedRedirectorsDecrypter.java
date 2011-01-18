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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cliccami.info", "lempar.co.tv", "mylink4u.info", "linkoculto.com", "urlaxe.net", "dwarfurl.com", "skracaj.org", "l-x.pl", "xlurl.com", "cbuz.com", "shorten.ws", "smallizer.com", "zi.ma" }, urls = { "http://[\\w\\.]*?cliccami\\.info/[0-9a-z]+", "http://[\\w\\.]*?lempar\\.co\\.tv/[0-9a-z]+", "http://[\\w\\.]*?mylink4u\\.info/[a-z0-9]+", "http://[\\w\\.]*?linkoculto\\.com/[a-z0-9]+", "http://[\\w\\.]*?urlaxe\\.net/[0-9]+", "http://[\\w\\.]*?dwarfurl\\.com/[a-z0-9]+", "http://[\\w\\.]*?skracaj\\.org/[a-z0-9]+", "http://[\\w\\.]*?l-x\\.pl/[a-z0-9]+", "http://[\\w\\.]*?xlurl\\.com/[a-z0-9]+", "http://[\\w\\.]*?cbuz\\.com/[a-z0-9]+", "http://[\\w\\.]*?shorten\\.ws/[a-z0-9]+", "http://[\\w\\.]*?smallizer\\.com/[a-z0-9]+", "http://[\\w\\.]*?zi\\.ma/[a-z0-9]+" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class PWProtectedRedirectorsDecrypter extends PluginForDecrypt {

    public PWProtectedRedirectorsDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // TODO: Implement dwarfurl.com pw-protected links handling
    // TODO: Make a single handling for every site so it isn't such a mess

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.contains("http://dwarfurl")) parameter = parameter.replace("http://", "http://www.");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String domain = new Regex(parameter, "([a-z-]+)\\.").getMatch(0);
        if (br.containsHTML("(non ci sono URL|There is no such URL in our database|Esa url no se encuentra|Taki skrót <b>nie istnieje|Witaj na l-x.pl|URL proibito|questo URL e' scaduto|This URL expired)") || (br.getRedirectLocation() != null && br.getRedirectLocation().contains(domain)) || (br.getRedirectLocation() == null && !br.containsHTML("ADULT CONTENT WARNING"))) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String finallink = br.getRedirectLocation();
        if (finallink == null) {
            // For iframe stuff
            finallink = br.getRegex("id=\"frame\" src=\"(.*?)\"").getMatch(0);
            // Handling for ncane adult links
            if (finallink == null) finallink = br.getRegex("or older, click <a href=\"(.*?)\"").getMatch(0);
            // Handling for xlurl.com links
            if (finallink == null) finallink = br.getRegex("window\\.location = \"(.*?)\"").getMatch(0);
            // Handling for smallizer.com links
            if (finallink == null) finallink = br.getRegex("\"removeFrame\".*?href=\"(.*?)\"").getMatch(0);
            if (finallink == null && (br.containsHTML("(name=\"pass\"|name=\"p\"|name=\"shortcut_password\"|name=\"urlpass\")"))) {
                for (int i = 0; i <= 2; i++) {
                    Form pwform = br.getForm(0);
                    if (pwform == null) return null;
                    String passCode = getUserInput(null, param);
                    if (parameter.contains("urlaxe.net")) {
                        pwform.put("p", passCode);
                    } else if (parameter.contains("skracaj.org") || parameter.contains("l-x.pl")) {
                        pwform.put("shortcut_password", passCode);
                    } else if (parameter.contains("zi.ma")) {
                        pwform.put("urlpass", passCode);
                    } else {
                        pwform.put("pass", passCode);
                    }
                    br.submitForm(pwform);
                    if (br.containsHTML("(Wrong password|Not valid password|name=\"p\"|incorrecta|name=\"pass\"|name=\"pass\"|name=\"shortcut_password\"|name=\"urlpass\")")) continue;
                    // Iframe handling
                    finallink = br.getRegex("id=\"frame\" src=\"(.*?)\"").getMatch(0);
                    if (finallink == null) finallink = br.getRegex("download URL.*?href=\"(.*?)\"").getMatch(0);
                    if (finallink == null) finallink = br.getRedirectLocation();
                    break;
                }
                if (br.containsHTML("(Wrong password|Not valid password|name=\"p\"|incorrecta|name=\"pass\"|name=\"pass\"|name=\"shortcut_password\"|name=\"urlpass\")")) {
                    logger.warning("Wrong password!");
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }

            } else if (finallink == null) finallink = br.getURL();
        }
        if (finallink == null || finallink.matches(parameter)) return null;
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }
}
