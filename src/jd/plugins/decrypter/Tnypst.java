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
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tinypaste.com" }, urls = { "http://[\\w\\.]*?tinypaste\\.com/([0-9a-z]+|.*?id=[0-9a-z]+)" }, flags = { 0 })
public class Tnypst extends PluginForDecrypt {

    private DownloadLink dl = null;

    public Tnypst(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String link = parameter.toString();
        br.getPage(link);
        boolean crypted = false;
        if (br.containsHTML("(Enter the correct password|has been password protected)")) {
            for (int i = 0; i <= 3; i++) {
                String id = new Regex(link, "tinypaste\\.com/.*?id=([0-9a-z]+)").getMatch(0);
                if (id == null) id = new Regex(link, "tinypaste\\.com/([0-9a-z]+)").getMatch(0);
                Form pwform = br.getForm(0);
                if (pwform == null || id == null) return null;
                // String pw = Plugin.getUserInput("Password?", parameter);
                String pw = "anon";
                pwform.put("password_" + id, pw);
                br.submitForm(pwform);
                if (br.containsHTML("(Enter the correct password|has been password protected)")) continue;
                break;
            }
            if (br.containsHTML("(Enter the correct password|has been password protected)")) throw new DecrypterException(DecrypterException.PASSWORD);
            crypted = true;
        }
        if (crypted) {
            logger.info("Link " + link + " is password protected, trying to find the links now...");
            String hash = br.getRegex("hash=([a-z0-9]+)(\"|')").getMatch(0);
            if (hash == null) return null;
            String linkPage = link + "/fullscreen.php?hash=" + hash;
            br.getPage(linkPage);
        }
        String allLinks = br.getRegex("<iframe frameborder='\\d+' id='pasteFrame(.*?)</iframe>").getMatch(0);
        if (allLinks == null) return null;
        String[] links = HTMLParser.getHttpLinks(allLinks, null);
        if (links == null || links.length == 0) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        ArrayList<String> pws = HTMLParser.findPasswords(allLinks);
        for (String element : links) {
            /* prevent recursion */
            if (element.contains("tinypaste.com")) continue;
            decryptedLinks.add(dl = createDownloadlink(element));
            dl.addSourcePluginPasswordList(pws);
        }
        return decryptedLinks;
    }
}
