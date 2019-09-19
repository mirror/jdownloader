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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "viet69.net" }, urls = { "https?://(www\\.)?viet69\\.net/[^/]+" })
public class Viet69Net extends antiDDoSForDecrypt {
    public Viet69Net(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = br.getRegex("<title>\\s*([^<]+)\\s+-\\s+Phim\\s+sex\\s+-\\s+Clip\\s+sex").getMatch(0);
        String[] videoDetails = br.getRegex("<div\\s+class\\s*=\\s*\"movieLoader\"\\s+data-movie\\s*=\\s*\"([^\"\\s]+)\"\\s+data-type\\s*=\\s*\"([^\"]*)\"").getRow(0);
        if (videoDetails != null && videoDetails.length > 1) {
            final Browser br2 = br.cloneBrowser();
            postPage(br2, "/get.video.php", "movie_id=" + videoDetails[0] + "&type=" + videoDetails[1] + "&index=1");
            final String[] links = br2.getRegex("\"file\"\\s*:\\s*\"([^\"]+)\"").getColumn(0);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}