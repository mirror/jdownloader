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

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.MissavComCrawler;
import jd.plugins.hoster.MissavCom.MissavComConfig.VideoQuality;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { MissavComCrawler.class })
public class MissavCom extends PluginForHost {
    public MissavCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        return MissavComCrawler.getPluginDomains();
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
            /* No regex. Links get added via crawler. */
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost();
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
        return new Regex(link.getPluginPatternMatcher(), "([A-Za-z0-9\\-]+)$").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(fid + extDefault);
        }
        this.setBrowserExclusive();
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean isSelfhostedVideo = br.containsHTML("dvdId: '" + Encoding.urlEncode(fid));
        if (!isSelfhostedVideo) {
            /* E.g. https://missav.com/pt/new */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>([^<]+)").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setFinalFileName(title + extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String videoHash = br.getRegex("nineyu\\.com.?/([a-f0-9\\-]+)").getMatch(0);
        if (videoHash == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Origin", "https://" + br.getHost());
        br.getPage("https://surrit.com/" + videoHash + "/playlist.m3u8");
        final VideoQuality qual = PluginJsonConfig.get(MissavComConfig.class).getVideoQuality();
        int targetHeight = 0;
        if (qual == VideoQuality.Q360P) {
            targetHeight = 360;
        } else if (qual == VideoQuality.Q480P) {
            targetHeight = 480;
        } else if (qual == VideoQuality.Q720P) {
            targetHeight = 720;
        } else if (qual == VideoQuality.Q1080P) {
            targetHeight = 1080;
        }
        HlsContainer preferredQuality = null;
        final List<HlsContainer> hlscontainers = HlsContainer.getHlsQualities(br);
        for (final HlsContainer container : hlscontainers) {
            if (container.getHeight() == targetHeight) {
                preferredQuality = container;
                break;
            }
        }
        if (preferredQuality == null || qual == VideoQuality.BEST) {
            /* Fallback and/or user prefers best quality */
            preferredQuality = HlsContainer.findBestVideoByBandwidth(hlscontainers);
        }
        logger.info("Downloading quality: " + preferredQuality.getHeight() + "p");
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, preferredQuality.getDownloadurl());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return MissavComConfig.class;
    }

    public static interface MissavComConfig extends PluginConfigInterface {
        public static final TRANSLATION  TRANSLATION  = new TRANSLATION();
        public static final VideoQuality DEFAULT_MODE = VideoQuality.BEST;

        public static class TRANSLATION {
            public String getVideoQuality_label() {
                return "Preferred video quality";
            }
        }

        public static enum VideoQuality implements LabelInterface {
            Q1080P {
                @Override
                public String getLabel() {
                    return "1080p";
                }
            },
            Q720P {
                @Override
                public String getLabel() {
                    return "720p";
                }
            },
            Q480P {
                @Override
                public String getLabel() {
                    return "480p";
                }
            },
            Q360P {
                @Override
                public String getLabel() {
                    return "360p";
                }
            },
            BEST {
                @Override
                public String getLabel() {
                    return "Best";
                }
            },
            DEFAULT {
                @Override
                public String getLabel() {
                    return "Default: " + BEST.getLabel();
                }
            };
        }

        @AboutConfig
        @DefaultEnumValue("DEFAULT")
        @Order(10)
        @DescriptionForConfigEntry("Select preferred video quality")
        @DefaultOnNull
        VideoQuality getVideoQuality();

        void setVideoQuality(final VideoQuality mode);
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