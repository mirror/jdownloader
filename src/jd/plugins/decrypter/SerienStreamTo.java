//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 39902 $", interfaceVersion = 3, names = { "s.to" }, urls = { "https?://(?:www\\.)?s\\.to/[^/]+/.*" })
public class SerienStreamTo extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public SerienStreamTo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:").replaceAll("[/]+$", "");
        br.setFollowRedirects(true);
        final String page = br.getPage(parameter);
        final String[][] titleDetail = br.getRegex("<meta property=\"og:title\" content=\"(Episode \\d+\\s|Staffel \\d+\\s|von+\\s)+([^\"]+)\"/>").getMatches();
        final String title = (titleDetail.length > 0) ? (titleDetail[0][titleDetail[0].length - 1]) : null;
        String[][] episodeLinks = br.getRegex("<a itemprop=\"url\"[^>]+href=\"([^\"]+)\"[^>]*>").getMatches();
        for (String[] episodeLink : episodeLinks) {
            String episodeURL = "https://s.to" + episodeLink[0];
            final Browser brLink = br.cloneBrowser();
            brLink.setFollowRedirects(true);
            brLink.getPage(episodeURL);
            String targetURL = brLink.getURL();
            decryptedLinks.add(createDownloadlink(targetURL));
        }
        if (title != null) {
            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setName(title);
            filePackage.setProperty("ALLOW_MERGE", true);
            filePackage.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}