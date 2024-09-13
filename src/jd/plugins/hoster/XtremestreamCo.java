// jDownloader - Downloadmanager
// Copyright (C) 2017 JD-Team support@jdownloader.org
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.XtremestreamCoConfig;
import org.jdownloader.plugins.components.config.XtremestreamCoConfig.Quality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class XtremestreamCo extends antiDDoSForHost {
    public XtremestreamCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }
    /* DEV NOTES */
    // Tags: Porn plugin
    // other:

    /* Connection stuff */
    private static final int free_maxdownloads = -1;

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "xtremestream.co", "xtremestream.xyz" });
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
            ret.add("https?://\\w+\\." + buildHostsPatternPart(domains) + "/player/index\\.php\\?data=([a-f0-9]{32})");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://" + this.getHost();
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
        final String ext = ".mp4";
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ext);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String referer = link.getReferrerUrl();
        if (referer == null || StringUtils.containsIgnoreCase(referer, "tube.perverzija.com")) {
            referer = "https://tube.perverzija.com/";
        }
        br.getHeaders().put("Referer", referer);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getRequest().getHtmlCode().length() <= 100) {
            /* Empty page */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("var video_title = .([^\"]*?)`;").getMatch(0);
        if (filename != null) {
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        /* 2022-02-28: They're using an abnormal kind of m3u8 lists which is why a plugin is required in the first place. */
        String hlsMaster = br.getRegex("var m3u8_loader_url = `(https://[^<>\"']+data=)`;").getMatch(0);
        if (hlsMaster == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        hlsMaster += this.getFID(link);
        br.getPage(hlsMaster);
        final List<HlsContainer> qualities = HlsContainer.getHlsQualities(this.br);
        final HlsContainer bestQuality = HlsContainer.findBestVideoByBandwidth(qualities);
        HlsContainer selectedQuality = null;
        final int preferredQualityHeight = this.getUserPreferredQualityHeight();
        for (final HlsContainer quality : qualities) {
            if (quality.getHeight() == preferredQualityHeight) {
                selectedQuality = quality;
                break;
            }
        }
        final String downloadurl;
        if (selectedQuality != null) {
            downloadurl = selectedQuality.getDownloadurl();
        } else {
            downloadurl = bestQuality.getDownloadurl();
        }
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, downloadurl);
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

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return XtremestreamCoConfig.class;
    }

    private int getUserPreferredQualityHeight() {
        final Quality quality = PluginJsonConfig.get(XtremestreamCoConfig.class).getPreferredStreamQuality();
        switch (quality) {
        case Q360:
            return 360;
        case Q480:
            return 480;
        case Q720:
            return 720;
        case Q1080:
            return 1080;
        case Q2160:
            return 2160;
        default:
            /* E.g. BEST */
            return 2160;
        }
    }
}