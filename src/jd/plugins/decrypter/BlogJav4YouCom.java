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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blog.jav4you.com" }, urls = { "http://(www\\.)?blog\\.jav4you\\.com/\\d{4}/\\d{2}/[a-z0-9\\-]+/" }, flags = { 0 })
public class BlogJav4YouCom extends PluginForDecrypt {

    public BlogJav4YouCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>([^<>\"]*?) \\| JAV4You \\- Huge Japanese AV Place</title>").getMatch(0);
        final String[] links = br.getRegex("<br><a href=\"(http[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink : links) {
            if (singleLink.matches("http://(www\\.)?blog\\.jav4you\\.com/\\d{4}/\\d{2}/[a-z0-9\\-]+/")) continue;
            if (singleLink.matches("http://(www\\.)?l\\.jav4you\\.com/[A-Za-z0-9]+")) {
                br.getPage(singleLink);
                final String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    logger.info("Skipping failed link: " + singleLink);
                    continue;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else {
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }
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