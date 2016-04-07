//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.http.Request;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.UserAgents;

/**
 *
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision: 23331 $", interfaceVersion = 2, names = { "ddlprotect.com" }, urls = { "http://(www\\.)?ddlprotect\\.com/ml/[a-zA-Z]{5}" }, flags = { 0 })
public class DdlPrtCm extends PluginForDecrypt {

    // DEV NOTES
    // - No https

    public DdlPrtCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getHeaders().put("User-Agent", UserAgents.stringUserAgent());
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // error clauses
        if (br.getHttpConnection().getResponseCode() == 404) {
            logger.info("Invalid URL: " + parameter);
            return decryptedLinks;
        }
        // find tables, they have Téléchargement(en:Download), and Streaming(en:Streaming) tables.
        final String[] tables = br.getRegex("<table(.*?)</table>").getColumn(0);
        if (tables == null) {
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        for (final String table : tables) {
            // find links
            final String[] links = HTMLParser.getHttpLinks(table, null);
            if (links == null || links.length == 0) {
                logger.warning("Possible plugin issue: " + parameter);
                continue;
            }
            for (final String link : links) {
                final String corrected = Request.getLocation(link, br.getRequest());
                if (!corrected.contains("ddlprotect.com/")) {
                    decryptedLinks.add(createDownloadlink(corrected));
                }
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}