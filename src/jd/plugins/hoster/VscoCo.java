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

import jd.PluginWrapper;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.VscoCoCrawler;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VscoCo extends PluginForHost {
    public VscoCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    public static final String   PROPERTY_MEDIA_ID = "media_id";
    public static final String   PROPERTY_QUALITY  = "quality";
    private final String         PROPERTY_HLS_URL  = "hls_url";

    public static List<String[]> getPluginDomains() {
        return VscoCoCrawler.getPluginDomains();
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
        for (int i = 0; i < getPluginDomains().size(); i++) {
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://vsco.co/about/terms_of_use";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getMediaID(link);
        if (linkid != null && (isHLSVideo(link) || link.getStringProperty(PROPERTY_HLS_URL) != null)) {
            return this.getHost() + "://" + "/" + getUsername(link) + "/" + linkid + "/" + getQuality(link);
        } else {
            return super.getLinkID(link);
        }
    }

    private String getMediaID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_MEDIA_ID);
    }

    private String getQuality(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_QUALITY, "best");
    }

    private String getUsername(final DownloadLink link) {
        return link.getStringProperty(VscoCoCrawler.PROPERTY_USERNAME);
    }

    private boolean isVideo(final DownloadLink link) {
        if (link.getName().contains(".mp4") || link.getPluginPatternMatcher().contains(".mp4")) {
            return true;
        } else if (isHLSVideo(link)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isHLSVideo(final DownloadLink link) {
        if (link.getPluginPatternMatcher().contains(".m3u8")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        final String singleHLSURL = link.getStringProperty(PROPERTY_HLS_URL);
        try {
            if (singleHLSURL != null) {
                con = br.openHeadConnection(singleHLSURL);
            } else if (isHLSVideo(link)) {
                con = br.openGetConnection(link.getPluginPatternMatcher());
            } else {
                /* 2022-08-30: No HEAD-request allowed! */
                con = br.openGetConnection(link.getPluginPatternMatcher());
            }
            if (LinkCrawlerDeepInspector.looksLikeMpegURL(con)) {
                if (singleHLSURL == null) {
                    /* First run: Find best quality */
                    br.followConnection();
                    final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br);
                    final HlsContainer bestQuality = HlsContainer.findBestVideoByBandwidth(qualities);
                    link.setProperty(PROPERTY_QUALITY, bestQuality.getHeight());
                    link.setProperty(PROPERTY_HLS_URL, bestQuality.getDownloadurl());
                    logger.info("Set best quality on first full linkcheck: " + bestQuality.getHeight() + "p");
                }
            } else if (this.looksLikeDownloadableContent(con)) {
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (isHLSVideo(link)) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, link.getStringProperty(PROPERTY_HLS_URL));
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), free_resume, free_maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
            dl.startDownload();
        }
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
        link.removeProperty(PROPERTY_HLS_URL);
    }
}