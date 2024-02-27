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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.BbcComDecrypter;
import jd.plugins.decrypter.BbcComiPlayerCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bbc.com" }, urls = { "" })
public class BbcCom extends PluginForHost {
    public BbcCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://www.bbc.com/terms/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid + "_" + getType(link);
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_VIDEOID);
    }

    public static String getFID(final String url) {
        return new Regex(url, "/([^/]+)$").getMatch(0);
    }

    public static boolean isVideo(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(TYPE_OLD) || StringUtils.containsIgnoreCase(link.getPluginPatternMatcher(), ".m3u8")) {
            return true;
        } else {
            return false;
        }
    }

    public static String getExt(final DownloadLink link) {
        if (isVideo(link)) {
            return ".mp4";
        } else {
            return ".ttml";
        }
    }

    private String getType(final DownloadLink link) {
        if (isVideo(link)) {
            return "video";
        } else {
            return "subtitle";
        }
    }

    int                        numberofFoundMedia             = 0;
    public static final String PROPERTY_TITLE                 = "title";
    public static final String PROPERTY_DATE                  = "date";
    public static final String PROPERTY_TV_BRAND              = "brand";
    public static final String PROPERTY_QUALITY_IDENTIFICATOR = "quality_identificator";
    public static final String PROPERTY_VIDEOID               = "videoid";
    @Deprecated
    public static final String TYPE_OLD                       = "http://bbcdecrypted/[a-z][a-z0-9]{7}";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        /* 2022-07-14: Last revision with old handling: 45973 */
        if (link.getPluginPatternMatcher().matches(TYPE_OLD)) {
            // TODO: Remove this
            final String vpID = new Regex(link.getPluginPatternMatcher(), "/([^/]+)$").getMatch(0);
            link.setProperty(PROPERTY_VIDEOID, vpID);
        }
        final String filenameBase = getFilenameBase(link);
        if (!link.isNameSet()) {
            /* Fallback */
            link.setName(filenameBase + getExt(link));
        }
        if (link.getPluginPatternMatcher().matches(TYPE_OLD)) {
            /* Legacy handling TODO: Remove in 05/2023 */
            logger.info("Going through legacy handling...");
            final BbcComiPlayerCrawler crawlerPlugin = (BbcComiPlayerCrawler) this.getNewPluginForDecryptInstance(BbcComiPlayerCrawler.getPluginDomains().get(numberofFoundMedia)[0]);
            final ArrayList<DownloadLink> results = crawlerPlugin.decryptIt(new CryptedLink(BbcComDecrypter.generateInternalVideoURL(this.getFID(link))), null);
            DownloadLink result = null;
            for (final DownloadLink resultTmp : results) {
                if (isVideo(resultTmp)) {
                    result = resultTmp;
                    break;
                }
            }
            if (result == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /* Update pluginPatternMatcher so next time legacy handling is not used anymore. */
            link.setPluginPatternMatcher(result.getPluginPatternMatcher());
        }
        if (!isDownload) {
            final String directurl = link.getPluginPatternMatcher();
            if (isVideo(link)) {
                checkFFProbe(link, "Check a HLS Stream");
                br.getPage(directurl);
                /* Check for offline and GEO-blocked */
                this.connectionErrorhandling(br.getHttpConnection());
                final List<M3U8Playlist> list = M3U8Playlist.parseM3U8(br);
                final HLSDownloader downloader = new HLSDownloader(link, br, br.getURL(), list);
                final StreamInfo streamInfo = downloader.getProbe();
                if (streamInfo == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final long estimatedFilesize = downloader.getEstimatedSize();
                    if (estimatedFilesize > 0) {
                        link.setDownloadSize(estimatedFilesize);
                    }
                }
            } else {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(directurl);
                    this.connectionErrorhandling(con);
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
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
        if (con.getResponseCode() == 403) {
            errorGeoBlocked();
        } else if (con.getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public static String getFilenameBase(final DownloadLink link) {
        String filenameBase;
        if (link.hasProperty(PROPERTY_TITLE)) {
            filenameBase = link.getStringProperty(PROPERTY_TITLE);
            filenameBase = link.getStringProperty(PROPERTY_TV_BRAND, "bbc") + "_" + filenameBase;
            if (link.hasProperty(PROPERTY_DATE)) {
                filenameBase = link.getStringProperty(PROPERTY_DATE) + "_" + filenameBase;
            }
        } else {
            /* Fallback */
            filenameBase = getFID(link.getPluginPatternMatcher());
        }
        if (link.hasProperty(PROPERTY_QUALITY_IDENTIFICATOR)) {
            filenameBase += "_" + link.getStringProperty(PROPERTY_QUALITY_IDENTIFICATOR);
        }
        return filenameBase;
    }

    public static String getFilename(final DownloadLink link) {
        return getFilenameBase(link) + getExt(link);
    }

    public static void errorGeoBlocked() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-Blocked and/or account required");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        final String directurl = link.getPluginPatternMatcher();
        if (isVideo(link)) {
            checkFFmpeg(link, "Download a HLS Stream");
            br.getPage(directurl);
            /* Check for offline and GEO-blocked */
            this.connectionErrorhandling(br.getHttpConnection());
            final List<M3U8Playlist> list = M3U8Playlist.parseM3U8(br);
            this.dl = new HLSDownloader(link, br, br.getURL(), list);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, directurl, false, 1);
            this.connectionErrorhandling(dl.getConnection());
            dl.startDownload();
        }
    }

    /**
     * Given width may not always be exactly what we have in our quality selection but we need an exact value to make the user selection
     * work properly!
     */
    public String getHeightForQualitySelection(final int height) {
        final String heightselect;
        if (height > 0 && height <= 200) {
            heightselect = "170";
        } else if (height > 200 && height <= 300) {
            heightselect = "270";
        } else if (height > 300 && height <= 400) {
            heightselect = "360";
        } else if (height > 400 && height <= 500) {
            heightselect = "480";
        } else if (height > 500 && height <= 600) {
            heightselect = "570";
        } else if (height > 600 && height <= 800) {
            heightselect = "720";
        } else if (height > 800 && height <= 1080) {
            heightselect = "1080";
        } else {
            /* Either unknown quality or audio (0x0) */
            heightselect = Integer.toString(height);
        }
        return heightselect;
    }

    public String getConfiguredVideoFramerate() {
        final int selection = this.getPluginConfig().getIntegerProperty(SETTING_SELECTED_VIDEO_FORMAT, 0);
        final String selectedResolution = FORMATS[selection];
        if (selectedResolution.contains("x")) {
            final String framerate = selectedResolution.split("@")[1];
            return framerate;
        } else {
            /* BEST selection */
            return selectedResolution;
        }
    }

    public String getConfiguredVideoHeight() {
        final int selection = this.getPluginConfig().getIntegerProperty(SETTING_SELECTED_VIDEO_FORMAT, 0);
        final String selectedResolution = FORMATS[selection];
        if (selectedResolution.contains("x")) {
            final String height = new Regex(selectedResolution, "\\d+x(\\d+)").getMatch(0);
            return height;
        } else {
            /* BEST selection */
            return selectedResolution;
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SETTING_SELECTED_VIDEO_FORMAT, FORMATS, "Select preferred video resolution:").setDefaultValue(0));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_ATTEMPT_FHD_WORKAROUND, "Try to download 1080p 50fps streams if not officially available?\r\nOnly has an effect when best quality is chosen!\r\nWarning: This may lead to download failures!").setDefaultValue(default_SETTING_ATTEMPT_FHD_WORKAROUND));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), SETTING_CRAWL_SUBTITLE, "Crawl subtitle?").setDefaultValue(default_SETTING_CRAWL_SUBTITLE));
    }

    /* The list of qualities displayed to the user */
    private final String[]      FORMATS                                = new String[] { "BEST", "1920x1080@50", "1920x1080@25", "1280x720@50", "1280x720@25", "1024x576@50", "1024x576@25", "768x432@50", "768x432@25", "640x360@25", "480x270@25", "320x180@25" };
    private final String        SETTING_SELECTED_VIDEO_FORMAT          = "SELECTED_VIDEO_FORMAT";
    private final String        SETTING_ATTEMPT_FHD_WORKAROUND         = "ATTEMPT_FDH_WORKAROUND";
    public static final String  SETTING_CRAWL_SUBTITLE                 = "CRAWL_SUBTITLE";
    private final boolean       default_SETTING_ATTEMPT_FHD_WORKAROUND = false;
    public static final boolean default_SETTING_CRAWL_SUBTITLE         = true;

    public boolean isAttemptToDownloadUnofficialFullHD() {
        return this.getPluginConfig().getBooleanProperty(SETTING_ATTEMPT_FHD_WORKAROUND, default_SETTING_ATTEMPT_FHD_WORKAROUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}