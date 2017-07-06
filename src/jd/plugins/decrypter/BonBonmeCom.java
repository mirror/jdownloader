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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bonbonme.com" }, urls = { "http://(?:(?:av|dl)\\.)?(?:bonbonme\\.com|jizz99\\.com)/(?!makemoney|data/|forum/)(?:a/)?[A-Za-z0-9\\-_]+/(?!list_)[A-Za-z0-9\\-_]+\\.html" })
public class BonBonmeCom extends PornEmbedParser {

    public BonBonmeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "bonbonme.com", "jizz99.com" };
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter, "Offline Content"));
            return decryptedLinks;
        }
        if (br.containsHTML("<tr><td>null</td></tr>")) {
            logger.info("Link offline: " + parameter);
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<div class=\"title\">[\t\n\r ]+<h2>([^<>\"]*?)(</h2>| 觀看次數:<script)").getMatch(0);
        // player url internal. you cant hit this url without having correct referer info
        final String player = br.getRegex("=('|\")((?:https?:)?(?://(?:www\\.)?(?:bonbonme\\.com|jizz99\\.com))?/player/.*?)\\1").getMatch(1);
        if (player == null) {
            return null;
        }
        getPage(player);
        decryptedLinks.addAll(findEmbedUrls(filename));
        return decryptedLinks;

    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}