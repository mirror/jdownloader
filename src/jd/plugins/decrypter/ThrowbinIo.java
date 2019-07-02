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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "throwbin.io" }, urls = { "https?://(?:www\\.)?throwbin\\.io/[A-Za-z0-9]+" })
public class ThrowbinIo extends PluginForDecrypt {
    public ThrowbinIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags: pastebin
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(new int[] { 500 });
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.br.getHttpConnection().getResponseCode() == 500) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String plaintxt = br.getRegex(",paste:\\{(.*?)</script>").getMatch(0);
        if (plaintxt == null) {
            /* Fallback */
            logger.info("Failed to find exact html, scanning full html code of website for downloadable content");
            plaintxt = br.toString();
        }
        /* Find URLs inside plaintext/html code */
        final String[] links = HTMLParser.getHttpLinks(plaintxt, "");
        if (links == null || links.length == 0) {
            logger.info("Found no links in plaintext: " + parameter);
            return decryptedLinks;
        }
        logger.info("Found " + links.length + " URLs in total");
        for (String dl : links) {
            if (!dl.contains(parameter) && this.canHandle(parameter)) {
                final DownloadLink link = createDownloadlink(dl);
                decryptedLinks.add(link);
            }
        }
        logger.info("Added " + decryptedLinks.size() + " URLs in total");
        return decryptedLinks;
    }
}