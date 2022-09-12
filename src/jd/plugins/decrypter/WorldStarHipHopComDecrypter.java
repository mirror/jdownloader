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
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.WorldStarHipHopCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WorldStarHipHopComDecrypter extends PluginForDecrypt {
    public WorldStarHipHopComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "worldstarhiphop.com", "worldstar.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(video(\\d+)?\\.php\\?v=[a-zA-Z0-9]+|videos/[A-Za-z0-9]+/[a-z0-9\\-]+|embed/\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (WorldStarHipHopCom.isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String externID = br.getRegex("\"file\",\"(https?://(www\\.)?youtube\\.com/v/[^<>\"]*?)\"\\);").getMatch(0);
        if (externID == null) {
            externID = br.getRegex("\"file\",\"(https?://(www\\.)?youtube\\.com/v/[A-Za-z0-9\\-_]+)\"").getMatch(0);
        }
        if (externID == null) {
            externID = br.getRegex("<iframe src=\"(https?://(?:www\\.)?youtube\\.com/[^<>\"]*?)\"").getMatch(0);
        }
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(https?://player\\.vimeo\\.com/video/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(https?://media\\.mtvnservices\\.com/(embed/)?mgid:uma:video:mtv\\.com:\\d+)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("\"(https?://(www\\.)?facebook\\.com/video/embed\\?video_id=\\d+)\"").getMatch(0);
        if (externID != null) {
            final DownloadLink dl = createDownloadlink(Encoding.htmlDecode(externID.trim()));
            dl.setProperty("nologin", true);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }
        externID = br.getRegex("\"(https?://cdnapi\\.kaltura\\.com/index\\.php/kwidget/[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("<iframe src=\"(https?://(www\\.)?bet\\.com/[^<>\"]*?)\" ").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        externID = br.getRegex("url=(https?%3A%2F%2Fapi\\.soundcloud\\.com%2Ftracks%2F[^<>\"]*?)\"").getMatch(0);
        if (externID != null) {
            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
            return decryptedLinks;
        }
        if (externID == null) {
            externID = br.getRegex("<iframe[^<>]*?src=\"([^\"]+)\"").getMatch(0);
            if (!externID.contains("worldstarhiphop")) {
                logger.info("External link found: " + externID);
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(externID.trim())));
                return decryptedLinks;
            }
        }
        final WorldStarHipHopCom hosterPlugin = (WorldStarHipHopCom) this.getNewPluginForHostInstance(this.getHost());
        // Probably no external video but a selfhosted one, pass it over to the hoster plugin
        final DownloadLink dl = new DownloadLink(hosterPlugin, this.getHost(), br.getURL());
        hosterPlugin.requestFileInformation(dl, br);
        dl.setAvailable(true);
        decryptedLinks.add(dl);
        return decryptedLinks;
    }

    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}