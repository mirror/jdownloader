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
import org.jdownloader.plugins.components.config.SexixNetConfig;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sexix.net" }, urls = { "https?://(?:www\\.)?sexix\\.net/video\\d+[a-z0-9\\-_]+/" })
public class SexixNet extends PluginForDecrypt {

    public SexixNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return SexixNetConfig.class;
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/video")) {
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
        String embedurl = this.br.getRegex("(/v\\.php\\?u=[^<>\"\\']+)").getMatch(0);
        if (embedurl == null) {
            return null;
        }
        this.br.getPage(embedurl);
        embedurl = this.br.getRegex("(/[^/]+/playlist\\.php\\?u=[^<>\"\\']+)").getMatch(0);
        if (embedurl == null) {
            return null;
        }
        this.br.getPage(embedurl);
        /* Content is usually hosted on googlevideo (Google drive) */
        final String[] xmls = br.getRegex("<jwplayer:source file=[^>]+>").getColumn(-1);
        if (xmls == null || xmls.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
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
            /* Best comes first --> Simply quit the loop if user wants best quality. */
            if (grabBEST) {
                decryptedLinks.add(dl);
                break;
            }
            qualities.put(quality, dl);
        }

        if (!grabBEST) {
            if (grab1080p && qualities.containsKey("1080p")) {
                decryptedLinks.add(qualities.get("1080p"));
            }
            if (grab720p && qualities.containsKey("720p")) {
                decryptedLinks.add(qualities.get("720p"));
            }
            if (grab480p && qualities.containsKey("480p")) {
                decryptedLinks.add(qualities.get("480p"));
            }
            if (grab360p && qualities.containsKey("360p")) {
                decryptedLinks.add(qualities.get("360p"));
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
