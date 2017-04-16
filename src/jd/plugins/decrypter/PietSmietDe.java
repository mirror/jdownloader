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
import java.util.HashMap;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.PietsmietDeConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.components.PluginJSonUtils;

/**
 *
 * @author raztoki
 * @author TheCrap
 *
 */
@DecrypterPlugin(revision = "$Revision: 36548 $", interfaceVersion = 3, names = { "pietsmiet.de" }, urls = { "https?://(?:www\\.)?pietsmiet\\.de/gallery/(?:playlists|categories)(?:/\\d+[a-z0-9\\-_]+){2}" })
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
        final boolean grab360p = pluginConfig.isGrab360pVideoEnabled();
        final HashMap<String, DownloadLink> qualities = new HashMap<String, DownloadLink>();

        String fpName = br.getRegex("<title>(.*?)\\s*-\\s*PietSmiet\\s*-\\s*Videos, News und Spiele</title>").getMatch(0);
        final String player = br.getRegex("jwplayer\\(\"media-jwplayer-\\d+\"\\)\\.setup\\(\\{(.*?)\\}\\);").getMatch(0);
        if (player == null || fpName == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        fpName = Encoding.htmlOnlyDecode(fpName).trim();
        final String[] sources = PluginJSonUtils.getJsonResultsFromArray(PluginJSonUtils.getJsonArray(player, "sources"));
        for (final String source : sources) {
            // for file they 'file'
            final String directurl = PluginJSonUtils.getJson(source, "file");
            // for label they do not use quotation mark it, then we have to use regex =[
            final String quality = new Regex(source, "label:\\s*'(.*?)'").getMatch(0);
            if (directurl == null || quality == null) {
                continue;
            }
            final DownloadLink dl = createDownloadlink(Request.getLocation(directurl, br.getRequest()));
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            dl.setFinalFileName(fpName + "_" + quality + ".mp4");
            qualities.put(quality, dl);
        }

        if ((decryptedLinks.isEmpty() || !grabBEST) && grab1080p && qualities.containsKey("1080p")) {
            decryptedLinks.add(qualities.get("1080p"));
        }
        if ((decryptedLinks.isEmpty() || !grabBEST) && grab720p && qualities.containsKey("720p")) {
            decryptedLinks.add(qualities.get("720p"));
        }
        if ((decryptedLinks.isEmpty() || !grabBEST) && grab480p && qualities.containsKey("480p")) {
            decryptedLinks.add(qualities.get("480p"));
        }
        if ((decryptedLinks.isEmpty() || !grabBEST) && grab360p && qualities.containsKey("360p")) {
            decryptedLinks.add(qualities.get("360p"));
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
