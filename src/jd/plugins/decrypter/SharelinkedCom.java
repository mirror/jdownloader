//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

/**
 * NOTE: sharelinked.com and rawfile.co are run by the same people. <br/>
 * NOTE: sharelinked.net, maybe not... ?<br/>
 * NOTE: domains can not be switched
 *
 * @author psp
 * @author raztoki
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rawfile.co", "sharelinked.com", "sharelinked.net" }, urls = { "https?://(?:www\\.)?rawfile\\.co/\\?p=(\\d+)", "https?://(?:www\\.)?sharelinked\\.com/\\?p=(\\d+)", "https?://(?:www\\.)?sharelinked\\.net/\\?p=(\\d+)" })
public class SharelinkedCom extends antiDDoSForDecrypt {

    public SharelinkedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        getPage(Request.getLocation("/wp-admin/admin-ajax.php?post=" + fid + "&action=get_content", br.createGetRequest(parameter)));
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() <= 10) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String[] links = br.getRegex("href=(\"|')(https?://(?!" + Pattern.quote(getHost()) + "/)[^<>\"]+|https?://" + Pattern.quote(getHost()) + "/\\?across[^\"]+)\\1").getColumn(1);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        for (String singleLink : links) {
            if (singleLink.contains(getHost() + "/?across")) {
                // single link
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(false);
                getPage(br2, singleLink);
                singleLink = br2.getRedirectLocation();
            }
            decryptedLinks.add(createDownloadlink(singleLink));
        }

        return decryptedLinks;
    }

}
