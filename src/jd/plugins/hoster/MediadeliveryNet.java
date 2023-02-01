//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.HexFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MediadeliveryNet extends antiDDoSForHost {
    public MediadeliveryNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // other: This plugin mostly handles embedded content from porn3dx.com.
    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /* Connection stuff */
    private static final int   free_maxdownloads        = -1;
    public static final String PROPERTY_AUTHOR          = "user";
    public static final String PROPERTY_TITLE           = "title";
    public static final String PROPERTY_POSITION        = "position";
    public static final String PROPERTY_PORN3DX_POST_ID = "porn3dx_post_id";

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "mediadelivery.net" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://iframe\\." + buildHostsPatternPart(domains) + "/embed/(\\d+/[a-f0-9\\-]+)(#.*)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://mediadelivery.net/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setFilename(link);
        if (!link.isNameSet()) {
            /* Fallback */
            link.setFinalFileName(this.getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        String forced_referer = new Regex(link.getPluginPatternMatcher(), "((\\&|\\?|#)forced_referer=.+)").getMatch(0);
        if (forced_referer != null) {
            forced_referer = new Regex(forced_referer, "forced_referer=([A-Za-z0-9=]+)").getMatch(0);
            if (forced_referer != null) {
                String ref = null;
                if (forced_referer.matches("^[a-fA-F0-9]+$") && forced_referer.length() % 2 == 0) {
                    final byte[] bytes = HexFormatter.hexToByteArray(forced_referer);
                    ref = bytes != null ? new String(bytes) : null;
                }
                if (ref == null) {
                    ref = Encoding.Base64Decode(forced_referer);
                }
                if (ref != null) {
                    try {
                        br.getPage(ref);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                }
            }
        }
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 403 || br.containsHTML("<h1>\\s*403\\s*</h1>")) {
            // Wrong referer
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("<h1>404</h1>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    public static void setFilename(final DownloadLink link) {
        final String author = link.getStringProperty(PROPERTY_AUTHOR);
        final String title = link.getStringProperty(PROPERTY_TITLE);
        final int position = link.getIntegerProperty(PROPERTY_POSITION, -1);
        final String porn3dxPostID = link.getStringProperty(PROPERTY_PORN3DX_POST_ID);
        if (porn3dxPostID != null && author != null && title != null && position != -1) {
            link.setFinalFileName(author + "_ " + porn3dxPostID + "_" + title + "_" + position + ".mp4");
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String hlsMaster = br.getRegex("\"(https?://[^\"]+playlist\\.m3u8)\"").getMatch(0);
        if (StringUtils.isEmpty(hlsMaster)) {
            if (br.containsHTML("playlist\\.drm")) {
                // m3u8 with aes-key
                throw new PluginException(LinkStatus.ERROR_FATAL, "DRM protected unsupported!");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(hlsMaster);
        if (!LinkCrawlerDeepInspector.looksLikeMpegURL(br.getHttpConnection())) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?");
        }
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, hlsbest.getDownloadurl());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}