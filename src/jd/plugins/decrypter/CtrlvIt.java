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
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ctrlv.it" }, urls = { "https?://(?:www\\.)?ctrlv\\.it/id/\\d+/(\\d+)" })
public class CtrlvIt extends PluginForDecrypt {
    public CtrlvIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String id = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(id)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String plaintxt = br.getRegex("<div id=\"pasted\">(.*?)div class=\"footer\"").getMatch(0);
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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}