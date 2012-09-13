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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dwmp3.com" }, urls = { "http://(www\\.)?dwmp3\\.com/(?!category)[a-z0-9\\-]+" }, flags = { 0 })
public class Dwmp3Com extends PluginForDecrypt {

    public Dwmp3Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Error 404 \\- Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>([^<>\"]*?) \\| Zippyshare Mp3 Download</title>").getMatch(0);
        final Regex zippy = br.getRegex(">var zippywww=\"(\\d+)\";var zippyfile=\"(\\d+)\"");
        if (zippy.getMatches().length > 1) {
            decryptedLinks.add(createDownloadlink("http://www" + zippy.getMatch(0) + ".zippyshare.com/v/" + zippy.getMatch(1) + "/file.html"));
            return decryptedLinks;
        }
        String externID = br.getRegex("emd\\.sharebeast\\.com/embed\\.php\\?type=basic\\&#038;file=([a-z0-9]{12})\\&").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.sharebeast.com/" + externID));
            return decryptedLinks;
        }
        // for Zippyshare directlinks
        externID = br.getRegex("<p><a href=\"(http[^<>\"]*?)\" target=\"_blank\" class=\"zippyshare_link\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        if (externID == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return decryptedLinks;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
