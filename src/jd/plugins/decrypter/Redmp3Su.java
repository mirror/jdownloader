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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mp3-red.ru" }, urls = { "https?://(?:www\\.)?(?:red3?-?mp3|mp3-?red)\\.(?:cc|co|su|ru|me|org)/album/(\\d+)/([a-z0-9\\-]+)\\.html" })
public class Redmp3Su extends PluginForDecrypt {
    public Redmp3Su(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * 2020-01-31: They GEO-block certain countries. All links will lead to 404 when you're blocked. I was able to make it work with a
     * polish VPN.
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("album/")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String url_name = new Regex(parameter, this.getSupportedLinks()).getMatch(1);
        String artistName = br.getRegex("class=\"artist-link\">([^<>\"]+)<").getMatch(0);
        if (artistName != null) {
            artistName = artistName.trim();
        }
        /* 2020-01-02: full_title = <album_name> - <year> - <artist> (year might not always be included) */
        String full_title = br.getRegex("<title>(?:\\s*Album(?:\\s*:)?)?([^<>]+)</title>").getMatch(0);
        String fpName = null;
        if (artistName != null && full_title != null && full_title.contains(artistName)) {
            /* Modify full title to: <artist> - <album_name> - <year> (year might not always be included) */
            fpName = full_title.trim().replace(artistName, "");
            fpName = fpName.trim();
            fpName = artistName + " - " + fpName;
        } else if (full_title != null) {
            fpName = full_title.trim();
        } else {
            /* Final fallback */
            fpName = url_name.replace("-", " ").trim();
        }
        final String[] links = br.getRegex("(/\\d+/[a-z0-9\\-]+\\.html)").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String singleLink : links) {
            if (br.containsHTML("/artist" + singleLink)) {
                continue;
            }
            singleLink = br.getURL(singleLink).toString();
            final String name_url = new Regex(singleLink, "([a-z0-9\\-]+)\\.html$").getMatch(0);
            final DownloadLink dl = createDownloadlink(singleLink);
            dl.setName(name_url + ".mp3");
            /* We do not know the online status at this stage. */
            // dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
