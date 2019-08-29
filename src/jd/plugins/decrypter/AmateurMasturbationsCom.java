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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amateurmasturbations.com" }, urls = { "http://(www\\.)?amateurmasturbations\\.com/(\\d+/[a-z0-9\\-]+/|video/.*?\\.html)" })
public class AmateurMasturbationsCom extends PornEmbedParser {
    public AmateurMasturbationsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        while (true) {
            if (br.getRedirectLocation() != null && br.getRedirectLocation().contains(this.getHost())) {
                br.followRedirect();
            } else {
                break;
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("Page Not Found")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String externID = br.getRedirectLocation();
        if (externID != null && !externID.contains("amateurmasturbations.com/")) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        } else if (externID != null && externID.contains("/404.php")) {
            logger.info("Link offline: " + parameter);
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        } else if (externID != null) {
            br.getPage(externID);
        }
        if (!br.getURL().matches("http://(www\\.)?amateurmasturbations\\.com/(\\d+/[a-z0-9\\-]+/|video/.*?\\.html)")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String filename = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        return decryptedLinks;
    }
}