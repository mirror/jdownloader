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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bonbonme.com" }, urls = { "http://((av|dl)\\.)?bonbonme\\.com/(?!makemoney|data/|forum/)[A-Za-z0-9\\-_]+/(?!list_)[A-Za-z0-9\\-_]+\\.html" })
public class BonBonmeCom extends PornEmbedParser {

    public BonBonmeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(parameter);
            if (con.getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter, "Offline Content"));
                return decryptedLinks;
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        if (this.br.containsHTML("<tr><td>null</td></tr>")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<div class=\"title\">[\t\n\r ]+<h2>([^<>\"]*?)(</h2>| 觀看次數:<script)").getMatch(0);
        String externID = br.getRegex("/player/redtube_\\.php\\?vid=(\\d+)").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink("http://www.redtube.com/" + externID));
            return decryptedLinks;
        }
        externID = br.getRegex("/player/plus\\.php\\?vid=([^<>\"]*?)\\&").getMatch(0);
        if (externID != null) {
            /* Double b64 encoded finallink */
            externID = Encoding.Base64Decode(externID);
            if (!externID.contains("http")) {
                externID = Encoding.Base64Decode(externID);
            }
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        /* Open RegEx - should suit most other cases! */
        externID = br.getRegex("/player/[^<>\"]+\\.php\\?vid=([^<>\"]+)").getMatch(0);
        if (externID != null) {
            externID = Encoding.htmlDecode(externID);
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }
        decryptedLinks.addAll(findEmbedUrls(filename));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}