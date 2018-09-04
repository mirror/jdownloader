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
//along with this program.  If not, see <https?://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "lovelyanime.com" }, urls = { "https?://(?:www\\.)?lovelyanime\\.com/[a-zA-Z0-9_/\\+\\=\\-%]+" })
public class LovelyAnime extends PluginForDecrypt {
    final String invalidLinks = ".+/(wp-content|wp-includes|wp-json|xmlrpc|anime-rss|comments|feed|forum|faq)/?.*?";

    public LovelyAnime(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String fpName = new String();
        String page = br.getPage(parameter);
        if (parameter.matches(invalidLinks)) {
            logger.info("Link invalid: " + parameter);
            return decryptedLinks;
        }
        if (page.contains("List of episodes for this anime")) {
            // Handle list paging
            fpName = br.getRegex("<title>([^<>\\\"]*?) - Anime Detail - Lovely Anime</title>").getMatch(0);
            final String[][] pageLinks = br.getRegex("https?://www.lovelyanime.com/[a-zA-Z0-9_+=\\-]+/episode-list/[0-9]+/?").getMatches();
            for (String[] pageLink : pageLinks) {
                String foundLink = Encoding.htmlDecode(pageLink[0]);
                decryptedLinks.add(createDownloadlink(foundLink));
            }
            // Handle episode list entries
            final String[][] episodeLinks = br.getRegex("<a class=\"lst\" href=\"([^<>\\\"]*?)\" title=\"[a-zA-Z0-9 -_]+\">").getMatches();
            for (String[] episodeLink : episodeLinks) {
                String foundLink = Encoding.htmlDecode(episodeLink[0]);
                decryptedLinks.add(createDownloadlink(foundLink));
            }
        } else if (page.contains("<b>Version</b>")) {
            // Handle episode page
            fpName = br.getRegex("<title>([^<>\\\"]*?) - Version [0-9]+ | /title>").getMatch(0);
            if (fpName != null) {
                fpName = fpName.replaceAll(" - Episode [0-9]+", "").trim();
            }
            String parameterBase = parameter.replaceAll("[/0-9]+$", "");
            // Handle version paging
            final String[][] versionLinks = br.getRegex(Pattern.quote(parameterBase) + "/[0-9]+/[/0-9]+").getMatches();
            for (String[] versionLink : versionLinks) {
                String foundLink = Encoding.htmlDecode(versionLink[0]);
                decryptedLinks.add(createDownloadlink(foundLink));
            }
            // Handle IFrame URLs
            final String[][] iframeLinks = br.getRegex("<iframe[^>]+src=\"([^<>\\\"]*?)\"").getMatches();
            for (String[] iframeLink : iframeLinks) {
                String foundLink = Encoding.htmlDecode(iframeLink[0]);
                if (!StringUtils.containsIgnoreCase(foundLink, "facebook.com/plugins/like.php")) {
                    decryptedLinks.add(createDownloadlink(foundLink));
                }
            }
        }
        if (fpName != null && !decryptedLinks.isEmpty()) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
            fp.setProperty("ALLOW_MERGE", true);
        }
        //
        return decryptedLinks;
    }
}
