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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.components.config.SexixNetConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sexix.net" }, urls = { "https?://(?:www\\.)?sexix\\.net/video\\d+[a-z0-9\\-_]+/" })
public class SexixNet extends antiDDoSForDecrypt {
    public SexixNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return SexixNetConfig.class;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/video")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final SexixNetConfig pluginConfig = PluginJsonConfig.get(SexixNetConfig.class);
        final boolean grabBEST = pluginConfig.isGrabBestVideoVersionEnabled();
        final boolean grab1080p = pluginConfig.isGrab1080pVideoEnabled();
        final boolean grab720p = pluginConfig.isGrab720pVideoEnabled();
        final boolean grab480p = pluginConfig.isGrab480pVideoEnabled();
        final boolean grab360p = pluginConfig.isGrab360pVideoEnabled();
        final HashMap<String, DownloadLink> qualities = new HashMap<String, DownloadLink>();
        String fpName = br.getRegex("<title>([^<>]+)</title>").getMatch(0);
        String embedurl = br.getRegex("(/v\\.php\\?u=[^<>\"\\']+)").getMatch(0);
        if (embedurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(embedurl);
        embedurl = br.getRegex("(/[^/]+/playlist\\.php\\?u=[^<>\"\\']+)").getMatch(0);
        if (embedurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        getPage(embedurl);
        /* Content is usually hosted on googlevideo (Google drive) */
        final String[] xmls = br.getRegex("<jwplayer:source file=[^>]+>").getColumn(-1);
        if (xmls == null || xmls.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String xml : xmls) {
            final String directurl = new Regex(xml, "file=\"(http[^\"<>]+)").getMatch(0);
            final String quality = new Regex(xml, "\"(\\d+p)\"").getMatch(0);
            if (directurl == null || quality == null) {
                continue;
            }
            final DownloadLink dl = createDownloadlink(directurl);
            dl.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            final String videoID = new Regex(parameter, "/(video\\d+)").getMatch(0);
            if (videoID != null) {
                dl.setName(videoID + "_" + quality + ".mp4");
            }
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
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
