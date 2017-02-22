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
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deluxemusic.tv" }, urls = { "https?://(?:www\\.)?deluxemusic\\.tv/.*?\\.html" })
public class DeluxemusicTv extends PluginForDecrypt {

    public DeluxemusicTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);

        this.br.getPage(parameter);

        if (this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        final String playlist_embed_id = this.br.getRegex("https?://[^/]+/playlist_embed_3/playlist\\.php\\?playlist_id=(\\d+)").getMatch(0);
        if (playlist_embed_id == null) {
            logger.info("Seems like this page does not contain any playlist");
            return decryptedLinks;
        }
        this.br.postPage("https://deluxetv-vimp.mivitec.net/playlist_embed_3/search_playlist_videos.php", "playlist_id=" + playlist_embed_id);
        final String[] mediakeys = this.br.getRegex("\"mediakey\"\\s*?:\\s*?\"([a-f0-9]{32})\"").getColumn(0);
        for (final String mediakey : mediakeys) {
            final String url = String.format("https://deluxetv-vimp.mivitec.net/video/discodeluxe_set/%s", mediakey);
            final DownloadLink dl = this.createDownloadlink(url);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

}
