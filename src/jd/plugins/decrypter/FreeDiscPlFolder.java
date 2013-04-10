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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freedisc.pl" }, urls = { "http://(www\\.)?freedisc\\.pl/[A-Za-z0-9\\-_]+,d\\-\\d+" }, flags = { 0 })
public class FreeDiscPlFolder extends PluginForDecrypt {

    public FreeDiscPlFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String username = new Regex(parameter, "freedisc\\.pl/([A-Za-z0-9\\-_]+),d\\-\\d+").getMatch(0);
        final String fpName = br.getRegex("<title>([^<>\"]*?)\\-  Freedisc\\.pl</title>").getMatch(0);
        final String[] links = br.getRegex("<div style=\\'float: left; overflow: auto;\\'>[\t\n\r ]+<a  href=\"(/[^<>\"]*?)\"").getColumn(0);
        if ((links == null || links.length == 0) && br.containsHTML("<a href=\"/" + username + ",d\\-0\" class=\"directoryText previousDirLinkFS\"")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links)
            decryptedLinks.add(createDownloadlink("http://freedisc.pl" + singleLink));
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}