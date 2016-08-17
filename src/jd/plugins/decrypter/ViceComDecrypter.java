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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vice.com" }, urls = { "https?://([A-Za-z0-9]+\\.)?vice\\.com/.+" }) 
public class ViceComDecrypter extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public ViceComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String TYPE_SINGLEVIDEO = "https?://(?:[a-z0-9\\-]+\\.)?vice\\.com/[a-z]+/video/[a-z0-9\\-]+";

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        String externID = null;

        br.getPage(parameter);
        String externSource = br.getRegex("<div class=\"video\\-container youtube\">(.*?)</div>").getMatch(0);
        if (externSource == null) {
            externSource = br.getRegex("<section class=\"video\\-wrapper\">(.*?)</section>").getMatch(0);
        }
        if (externSource == null) {
            externSource = br.getRegex("<div class=\"resp-video-wrapper youtube-wrapper\">(.*?)</div><p>").getMatch(0);
        }
        if (externSource != null) {
            externID = new Regex(externSource, "data-youtube-id=\"([^<>\"]*?)\"").getMatch(0);
            if (externID == null) {
                externID = new Regex(externSource, "youtube\\.com/embed/([^<>\"]*?)\"").getMatch(0);
            }
            if (externID != null) {
                externID = "https://www.youtube.com/watch?v=" + externID;
            }
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        }

        /* Check for vice embedded videos */
        final String[] videoURLs = this.br.getRegex("share_url=https?://(?:[a-z0-9\\-]+\\.)?vice\\.com/([a-z]+/video/[a-z0-9\\-]+)\"").getColumn(0);
        if (!parameter.matches(TYPE_SINGLEVIDEO) && videoURLs != null && videoURLs.length > 0) {
            /*
             * E.g. http://www.vice.com/de/read/schlaege-beleidigungen-drohungen-das-sek-berlin-macht-eine-hausbegehung-809 &utm_medium=link
             */
            for (final String videourl : videoURLs) {
                decryptedLinks.add(createDownloadlink("http://vicedecrypted.com/" + videourl));
            }
        } else {
            /* No embedded video there --> We either have a single video or no downloadable content! */
            /* E.g. http://www.vice.com/de/video/heimat-ausgekohlt-der-kampf-um-die-kohle-101 */
            final DownloadLink main = createDownloadlink(parameter.replace("vice.com/", "vicedecrypted.com/"));
            main.setContentUrl(parameter);
            /* Check for offline */
            if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML(jd.plugins.hoster.ViceCom.HTML_VIDEO_EXISTS)) {
                main.setAvailable(false);
            }
            /* Found no external urls --> Return link for our host plugin */
            decryptedLinks.add(main);
        }

        return decryptedLinks;
    }

}
