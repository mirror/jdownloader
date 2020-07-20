//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freeviewmovies.com" }, urls = { "https?://(?:www\\.)?freeviewmovies\\.com/(?:porn|video)/(\\d+)/([a-z0-9\\-]+)" })
public class FreeViewMoviesComCrawler extends PornEmbedParser {
    public FreeViewMoviesComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // old /porn/ links redirect
        br.setFollowRedirects(true);
        String parameter = param.toString();
        final String fid = new Regex(parameter, this.getSupportedLinks()).getMatch(0);
        final String name_url = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        br.getPage(parameter);
        if (br.containsHTML("<div id=\"player\">\\s+This video has no trailer\\.\\s+</div>")) {
            logger.info("No public trailer!: " + parameter);
            return decryptedLinks;
        } else if (br.containsHTML("No htmlCode read") || br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(fid)) {
            logger.info("Link broken: " + parameter);
            return decryptedLinks;
        }
        decryptedLinks.addAll(findEmbedUrls(name_url));
        if (decryptedLinks.size() == 0) {
            /* Must be selfhosted content */
            decryptedLinks.add(this.createDownloadlink(parameter));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}