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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sharelinked.com" }, urls = { "https?://(?:www\\.)?sharelinked\\.com/\\?p=\\d+" })
public class SharelinkedCom extends PluginForDecrypt {

    public SharelinkedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "(\\d+)$").getMatch(0);
        br.getPage("http://sharelinked.com/wp-admin/admin-ajax.php?post=" + fid + "&action=get_content");
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() <= 10) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = null;
        final String[] links = br.getRegex("href=\"(https?://(?!sharelinked\\.com/)[^<>\"]+|https?://sharelinked\\.com/\\?across[^\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links) {
            if (singleLink.contains("sharelinked.com/?across")) {
                // single link
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(false);
                br2.getPage(singleLink);
                singleLink = br2.getRedirectLocation();
            }
            decryptedLinks.add(createDownloadlink(singleLink));
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
