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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "camwhores.tv" }, urls = { "https?://(?:www\\.)?camwhores\\.tv/videos/\\d+/.*?" })
public class CamwhoresTv extends PornEmbedParser {

    public CamwhoresTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    /* Porn_plugin */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        this.br.setCookiesExclusive(true);
        final String parameter = param.toString();
        br.getPage(parameter);
        if (jd.plugins.hoster.CamwhoresTv.isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        decryptedLinks.addAll(findEmbedUrls(filename));
        if (decryptedLinks.size() == 0) {
            /* Probably a selfhosted video. */
            final DownloadLink dl = createDownloadUrlForHostPlugin(this.createDownloadlink(parameter));
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    public static DownloadLink createDownloadUrlForHostPlugin(final DownloadLink dl) {
        dl.setUrlDownload(dl.getDownloadURL().replace("camwhores.tv/", "camwhoresdecrypted.tv/"));
        return dl;
    }

}
