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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.PietsmietDeConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

/**
 *
 * @author raztoki
 * @author TheCrap
 * @author Rua4da
 *
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pietsmiet.de" }, urls = { "https?://(?:www\\.)?pietsmiet\\.de/gallery/(?:playlists|categories)(?:/\\d+[a-z0-9\\-_]+){2}" })
public class PietSmietDe extends antiDDoSForDecrypt {

    public PietSmietDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return PietsmietDeConfig.class;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final PietsmietDeConfig pluginConfig = PluginJsonConfig.get(PietsmietDeConfig.class);
        final boolean grabBEST = pluginConfig.isGrabBestVideoVersionEnabled();
        final boolean grab1080p = pluginConfig.isGrab1080pVideoEnabled();
        final boolean grab720p = pluginConfig.isGrab720pVideoEnabled();
        final boolean grab480p = pluginConfig.isGrab480pVideoEnabled();
        String fpName = br.getRegex("<meta property=\"og:title\" content=\"([^\"]*)\"\\/>").getMatch(0);
        String baseURL = "https://e1." + br.getRegex("\\{ 'file': '\\/\\/(pietcdn\\.de\\/media\\/com_hwdmediashare\\/files\\/\\w{2}\\/\\w{2}\\/\\w{2}\\/)\\w{32}\\.mp4', type: 'mp4', label: '720p', \"default\": \"true\" \\}").getMatch(0);
        String[] sources = br.getRegex("\\{ 'file': '\\/\\/pietcdn\\.de\\/hls\\/\\w{2}\\/\\w{2}\\/\\w{2}\\/,(\\w{32}),(\\w{32}),(\\w{32}),\\.mp4\\.us\\/hmt\\.m3u8' \\},").getRow(0);
        if (fpName == null || baseURL == null || sources == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        fpName = Encoding.htmlOnlyDecode(fpName).trim();
        if (grabBEST) {
            final DownloadLink dl = this.createDownloadlink(baseURL + sources[2] + ".mp4");
            dl.setFinalFileName(fpName + "_1080p.mp4");
            decryptedLinks.add(dl);
        } else {
            if (grab1080p) {
                final DownloadLink dl = this.createDownloadlink(baseURL + sources[2] + ".mp4");
                dl.setFinalFileName(fpName + "_1080p.mp4");
                decryptedLinks.add(dl);
            }
            if (grab720p) {
                final DownloadLink dl = this.createDownloadlink(baseURL + sources[1] + ".mp4");
                dl.setFinalFileName(fpName + "_720p.mp4");
                decryptedLinks.add(dl);
            }
            if (grab480p) {
                final DownloadLink dl = this.createDownloadlink(baseURL + sources[0] + ".mp4");
                dl.setFinalFileName(fpName + "_480p.mp4");
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
