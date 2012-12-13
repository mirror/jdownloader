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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "free-way.me" }, urls = { "https://(www\\.)?free\\-way\\.me/container\\.php\\?contid=[a-z0-9]+\\&contname=.+" }, flags = { 0 })
public class FreeWayMeContainerDecrypter extends PluginForDecrypt {

    public FreeWayMeContainerDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        int counter = 1;
        final Regex folderInfo = new Regex(parameter, "\\?contid=([a-z0-9]+)\\&contname=(.+)");
        final String contID = folderInfo.getMatch(0);
        final String contName = folderInfo.getMatch(1);
        String passCode = null;
        do {
            passCode = getUserInput("Password?", param);
            br.getPage("https://www.free-way.me/ajax/jd.php?id=6&contid=" + contID + "&contname=" + Encoding.urlEncode(contName) + "&passwd=" + Encoding.urlEncode(passCode));
            counter++;
        } while (counter <= 3 && br.containsHTML("Falsches Passwort"));
        if (br.containsHTML("Kein Container gefunden")) {
            logger.info("This containerlink is empty: " + parameter);
            return decryptedLinks;
        }
        final String[] links = br.getRegex("\"([a-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            final DownloadLink dl = createDownloadlink("http://free-way.me/decryptedcontainerlink/" + singleLink);
            dl.setProperty("password", passCode);
            dl.setProperty("contname", contName);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(contName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
