//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.config.SrfChConfig;
import org.jdownloader.plugins.components.config.SrfChConfig.QualitySelectionFallbackMode;
import org.jdownloader.plugins.components.config.SrfChConfig.QualitySelectionMode;
import org.jdownloader.plugins.config.PluginConfigInterface;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.SrfChCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { SrfChCrawler.class })
public class SrfCh extends PluginForHost {
    @SuppressWarnings("deprecation")
    public SrfCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return SrfChCrawler.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:audio|play)/.+\\?(?:id=[A-Za-z0-9\\-]+|urn=[a-z0-9\\-:]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "http://www.srf.ch/allgemeines/impressum";
    }

    private final String TYPE_OLD = "https?://[^/]+/(?:audio|play)/.+\\?(?:id=[A-Za-z0-9\\-]+|urn=[a-z0-9\\-:]+)";

    @Override
    public String getLinkID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(TYPE_OLD)) {
            return super.getLinkID(link);
        } else {
            /* New items */
            return this.getHost() + "://" + link.getStringProperty(SrfChCrawler.PROPERTY_URN) + "://" + link.getStringProperty(SrfChCrawler.PROPERTY_HEIGHT);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(TYPE_OLD)) {
            /* Legacy handling for items added up to revision 46490 */
            final SrfChCrawler crawler = (SrfChCrawler) this.getNewPluginForDecryptInstance(this.getHost());
            final ArrayList<DownloadLink> results = crawler.crawl(new CryptedLink(link.getPluginPatternMatcher()), QualitySelectionMode.BEST, QualitySelectionFallbackMode.BEST);
            if (results.isEmpty()) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DownloadLink firstMediaItem = null;
            for (final DownloadLink result : results) {
                if (result.getFinalFileName().matches("(?i).*\\.(mp3|mp4)")) {
                    firstMediaItem = result;
                    break;
                }
            }
            if (firstMediaItem == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setPluginPatternMatcher(firstMediaItem.getPluginPatternMatcher());
            link.setProperties(firstMediaItem.getProperties());
        }
        final String downloadurl = link.getPluginPatternMatcher();
        if (!isDownload) {
            if (downloadurl.contains(".m3u8")) {
                checkFFProbe(link, "Check a HLS Stream");
                br.getPage(downloadurl);
                final List<M3U8Playlist> list = M3U8Playlist.parseM3U8(br);
                final HLSDownloader downloader = new HLSDownloader(link, br, br.getURL(), list);
                final StreamInfo streamInfo = downloader.getProbe();
                if (streamInfo == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final long estimatedSize = downloader.getEstimatedSize();
                    if (estimatedSize > 0) {
                        link.setDownloadSize(estimatedSize);
                    }
                }
            } else {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(downloadurl);
                    this.connectionErrorhandling(br.getHttpConnection());
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    /* Audio URLs sometimes end with .jpg but redirect to .png files. */
                    final String finalFilename = link.getFinalFileName();
                    if (finalFilename != null) {
                        final String extensionByMinetype = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                        if (extensionByMinetype != null) {
                            final String filenameWithCorrectedExtension = this.correctOrApplyFileNameExtension(finalFilename, "." + extensionByMinetype);
                            if (!filenameWithCorrectedExtension.equals(finalFilename)) {
                                link.setFinalFileName(filenameWithCorrectedExtension);
                            }
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private void connectionErrorhandling(final URLConnectionAdapter con) throws PluginException {
        if (!this.looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Media broken?");
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        final String downloadurl = link.getPluginPatternMatcher();
        if (downloadurl.contains(".m3u8")) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, downloadurl);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadurl, true, 0);
            this.connectionErrorhandling(dl.getConnection());
            this.dl.startDownload();
        }
    }

    private void contentBlocked(final String blockReason) throws PluginException {
        final String userReadableBlockedReason;
        if (StringUtils.isEmpty(blockReason)) {
            userReadableBlockedReason = "Unknown";
        } else if (blockReason.equalsIgnoreCase("ENDDATE")) {
            userReadableBlockedReason = "Content is not available anymore (expired)";
        } else if (blockReason.equalsIgnoreCase("GEOBLOCK")) {
            userReadableBlockedReason = "GEO-blocked";
        } else {
            userReadableBlockedReason = "Unknown reason:" + blockReason;
        }
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Content not downloadable because " + userReadableBlockedReason);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return SrfChConfig.class;
    }
}