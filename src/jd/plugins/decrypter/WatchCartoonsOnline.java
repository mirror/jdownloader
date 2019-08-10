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

import org.appwork.utils.StringUtils;
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

@DecrypterPlugin(revision = "$Revision: 41072 $", interfaceVersion = 2, names = { "watchcartoonsonline.la" }, urls = { "https?://(www[0-9]*\\.)?watchcartoonsonline\\.la/.+" })
public class WatchCartoonsOnline extends antiDDoSForDecrypt {
    public WatchCartoonsOnline(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String page = br.toString();
        String fpName = br.getRegex("property=\"og:title\" content=\"Watch ([^\"]+) (?:full episodes cartoon|Full Free Online|full online)").getMatch(0);
        String[] links = null;
        links = br.getRegex("'sources':\\[([^\\]]+)\\]").getColumn(0);
        if (links != null && links.length > 0) {
            links = HTMLParser.getHttpLinks(StringUtils.join(links, " "), null);
        } else {
            links = br.getRegex("([^\"]+watchcartoonsonline\\.la/watch/[^\"]+)").getColumn(0);
        }
        if (links != null && links.length > 0) {
            final Browser brc = br.cloneBrowser();
            brc.setKeepResponseContentBytes(false);
            for (String link : links) {
                if (StringUtils.containsIgnoreCase(link, "get_video.php")) {
                    try {
                        brc.setReadTimeout(1000);
                        brc.getPage(link);
                    } catch (Exception e) {
                        //
                    } finally {
                        link = brc.getRedirectLocation() == null ? brc.getURL() : brc.getRedirectLocation();
                    }
                }
                decryptedLinks.add(createDownloadlink(Encoding.htmlOnlyDecode(link)));
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