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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "watchseries.eu" }, urls = { "http://(www\\.)?watchseries\\.(eu|li)/episode/[^<>\"/]*?\\-(\\d+)\\.html" }, flags = { 0 })
public class WatchSeriesEu extends PluginForDecrypt {

    public WatchSeriesEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Um, Where did the page go|You either took a wrong turn or the site is screwed")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>Watch Online ([^<>\"]*?) \\- Watch Series</title>").getMatch(0);
        br.postPage("http://watchseries.li/getlinks.php", "domain=all&q=" + new Regex(parameter, "(\\d+)\\.html$").getMatch(0));
        final String[] links = br.getRegex("(/open/cale/\\d+/idepisod/\\d+\\.html)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links) {
            br.getPage("http://watchseries.li" + singleLink);
            final String continuelnk = br.getRegex("\"(http://watchseries\\.(eu|li)/gateway\\.php\\?link=[^<>\"]*?)\"").getMatch(0);
            if (continuelnk == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage(continuelnk);
            final String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            logger.info("Finallink: " + finallink);
            decryptedLinks.add(createDownloadlink(finallink));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
