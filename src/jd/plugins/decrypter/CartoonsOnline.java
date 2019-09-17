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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cartoonsonline.la" }, urls = { "https?://(www\\.)?cartoonsonline\\.la/cartoon/.*" })
public class CartoonsOnline extends antiDDoSForDecrypt {
    public CartoonsOnline(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fid = br.getRegex("/cartoon/([^/\"\\s]+)").getMatch(0);
        String fpName = br.getRegex("<title>([^<]+) +- Watch Cartoons Online for Free").getMatch(0);
        if (fid != null && fid.length() > 0) {
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getPage(br2.getURL("/inc/getvideo.php?url=" + fid.trim()).toString());
            if (br2.containsHTML("No htmlCode read")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String[] links = br2.getRegex("data-linkdata=\\W+([\\d\\w+\\-\\_\\.\\~]+)").getColumn(0);
            if (links != null && links.length > 0) {
                for (String link : links) {
                    postPage(br2, br2.getURL("/inc/getajaxelement.php").toString(), "linkdata=" + link.trim());
                    final String[] playURLs = HTMLParser.getHttpLinks(br2.toString(), null);
                    if (playURLs != null && playURLs.length > 0) {
                        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(playURLs[0])));
                    }
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