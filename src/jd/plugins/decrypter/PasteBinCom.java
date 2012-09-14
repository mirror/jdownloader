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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pastebin.com" }, urls = { "http://(www\\.)?pastebin\\.com/(?!trends|signup|login|pro|tools|archive|login\\.php|faq)(raw.*?=)?[0-9A-Za-z]+" }, flags = { 0 })
public class PasteBinCom extends PluginForDecrypt {

    public PasteBinCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        /* Error handling for invalid links */
        if (br.containsHTML("(Unknown paste ID|Unknown paste ID, it may have expired or been deleted)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.nolinks", "Perhaps wrong URL or there are no links to add."));
        String plaintxt = br.getRegex("<textarea(.*?)</textarea>").getMatch(0);
        if (plaintxt == null && parameter.contains("raw.php")) {
            plaintxt = br.toString();
        }
        if (plaintxt == null) return null;
        // Find all those links
        String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no links in link: " + parameter);
            return decryptedLinks;
        }
        logger.info("Found " + links.length + " links in total.");
        for (String dl : links)
            if (!dl.contains(parameter) && !new Regex(dl, "http://(www\\.)?pastebin\\.com/(raw.*?=)?[0-9A-Za-z]+").matches()) decryptedLinks.add(createDownloadlink(dl));

        return decryptedLinks;
    }

}
