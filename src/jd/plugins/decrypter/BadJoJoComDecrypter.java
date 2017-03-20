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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "badjojo.com" }, urls = { "http://(www\\.)?badjojo\\.com/\\d+/.{1}" })
public class BadJoJoComDecrypter extends PornEmbedParser {

    public BadJoJoComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if ("http://www.badjojo.com/".equals(br.getRedirectLocation()) || br.getRequest().getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter, "Offline Content"));
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        // <a href="/out.php?siteid=89&amp;id=14064863&amp;url=http%3A%2F%2Fnudez.com%2Fvideo%2F...-221490.html"
        if (br.containsHTML("<h4>Source</h4>")) {
            String externID = br.getRegex("<h4>Source</h4>\\s*<a href=\"[^\"]+?url=([^\"]+)\"").getMatch(0);
            if (externID != null) {
                externID = Encoding.urlDecode(externID, true);
                logger.info("externID: " + externID);
                decryptedLinks.add(createDownloadlink(externID));
                return decryptedLinks;
            }
        }

        decryptedLinks = new ArrayList<DownloadLink>();
        decryptedLinks.add(createDownloadlink(parameter.replace("badjojo.com/", "decryptedbadjojo.com/")));
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}