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

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vidcloud.co" }, urls = { "https?://(?:www\\.)?vidcloud\\.co/(?:embed|v)/([a-z0-9]+)" })
public class VidcloudCo extends PluginForHost {
    public VidcloudCo(PluginWrapper wrapper) {
        super(wrapper);
    }
    /* DEV NOTES */
    // Tags: Porn plugin
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String default_extension = ".mp4";
    /* Connection stuff */
    private static final int    free_maxdownloads = -1;
    private String              dllink            = null;
    private boolean             server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://vcstream.to/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        return this.getHost() + "://" + getFID(link);
    }

    public String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "([a-z0-9]+)$").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = getFID(link);
        String filename = null;
        final boolean crawlWebsiteFirst = false;
        if (crawlWebsiteFirst) {
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]+)\"/>").getMatch(0);
        }
        br.getPage("https://" + this.getHost() + "/player?fid=" + fid + "&page=embed");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = br.getRegex("title\\s*?:\\s*?\\'([^<>\"\\']+)\\'").getMatch(0);
        }
        if (StringUtils.isEmpty(filename)) {
            filename = fid;
        }
        dllink = PluginJSonUtils.getJson(br.toString().replaceAll("\\\\", "").replaceAll("\\\\", ""), "file");
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(this.dllink);
        final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
        this.dllink = hlsbest.getDownloadurl();
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, this.dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return VidcloudConfigInterface.class;
    }

    public static interface VidcloudConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getFastLinkcheckEnabled_label() {
                return _JDT.T.lit_enable_fast_linkcheck();
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(false)
        @Order(9)
        boolean isFastLinkcheckEnabled();

        void setFastLinkcheckEnabled(boolean b);
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
