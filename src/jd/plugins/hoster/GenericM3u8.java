//    jDownloader - Downloadmanager
//    Copyright (C) 2014  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.net.MalformedURLException;
import java.net.URL;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.config.GenericM3u8DecrypterConfig;
import org.jdownloader.plugins.components.hls.HlsContainer.CODEC;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "M3u8" }, urls = { "m3u8s?://.+" })
public class GenericM3u8 extends PluginForHost {
    public static final String PRESET_NAME_PROPERTY               = "preSetName";
    public static final String PROPERTY_HEIGHT                    = "height";
    public static final String PROPERTY_WIDTH                     = "width";
    public static final String PROPERTY_BANDWIDTH                 = "hlsBandwidth";
    public static final String PROPERTY_BANDWIDTH_AVERAGE         = "hlsBandwidthAverage";
    public static final String PROPERTY_BITRATE                   = "bitrate";
    public static final String PROPERTY_FRAME_RATE                = "framerate";
    public static final String PROPERTY_FFMPEG_CODEC_TYPE         = "ffmpeg_codec_type";
    public static final String PROPERTY_FFMPEG_CODEC_NAME         = "ffmpeg_codec_name";
    public static final String PROPERTY_M3U8_CODECS               = "m3u8_codecs";
    public static final String PROPERTY_M3U8_NAME                 = "m3u8_name";
    public static final String PROPERTY_DURATION_ESTIMATED_MILLIS = "duration_estimated_millis";

    public GenericM3u8(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getHost(final DownloadLink link, final Account account, boolean includeSubdomain) {
        if (link != null) {
            return Browser.getHost(link.getPluginPatternMatcher(), includeSubdomain);
        } else {
            return super.getHost(link, account, includeSubdomain);
        }
    }

    @Override
    public boolean isSpeedLimited(final DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.GENERIC };
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        if (link.getPluginPatternMatcher().startsWith("m3u8")) {
            final String url = "http" + link.getPluginPatternMatcher().substring(4);
            link.setPluginPatternMatcher(url);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, link.getPluginPatternMatcher());
    }

    private String getReferer(final DownloadLink link) {
        return link.getStringProperty("Referer", link.getReferrerUrl());
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final String dllink) throws Exception {
        checkFFProbe(link, "Check a HLS Stream");
        this.setBrowserExclusive();
        final String cookiesString = link.getStringProperty("cookies");
        if (cookiesString != null) {
            final String host = Browser.getHost(dllink);
            br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
        }
        final String referer = getReferer(link);
        if (referer != null) {
            br.getPage(referer);
            br.followRedirect();
        }
        final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
        final StreamInfo streamInfo = downloader.getProbe();
        if (downloader.isEncrypted()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Encrypted HLS(" + downloader.getEncryptionMethod() + ") is not supported!");
        } else if (streamInfo == null) {
            /* Invalid/broken stream */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int hlsBandwidth = link.getIntegerProperty(PROPERTY_BANDWIDTH, 0);
        if (hlsBandwidth > 0) {
            for (M3U8Playlist playList : downloader.getPlayLists()) {
                playList.setAverageBandwidth(hlsBandwidth);
            }
        }
        final long estimatedSize = downloader.getEstimatedSize();
        if (link.getKnownDownloadSize() == -1) {
            link.setDownloadSize(estimatedSize);
        } else {
            link.setDownloadSize(Math.max(link.getKnownDownloadSize(), estimatedSize));
        }
        /* TODO: Maybe always set current estimated filesize except if download has been started before(?) */
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.setDownloadSize(estimatedSize);
        }
        for (final Stream s : streamInfo.getStreams()) {
            final int bitrate = s.parseBitrate();
            if (bitrate != -1) {
                link.setProperty(PROPERTY_BITRATE, bitrate);
            }
            if ("video".equalsIgnoreCase(s.getCodec_type())) {
                link.setProperty(PROPERTY_FFMPEG_CODEC_TYPE, "video");
                link.setProperty(PROPERTY_FFMPEG_CODEC_NAME, s.getCodec_name());
                link.setProperty(PROPERTY_HEIGHT, s.getHeight());
                link.setProperty(PROPERTY_WIDTH, s.getWidth());
            } else if ("audio".equalsIgnoreCase(s.getCodec_type())) {
                link.setProperty(PROPERTY_FFMPEG_CODEC_TYPE, "audio");
                link.setProperty(PROPERTY_FFMPEG_CODEC_NAME, s.getCodec_name());
            }
        }
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.setFinalFileName(null);
            link.setName(null);
        }
        setFilename(link, true);
        return AvailableStatus.TRUE;
    }

    public static void setFilename(final DownloadLink link, final boolean setFinalFilename) throws MalformedURLException {
        if (link.getFinalFileName() != null) {
            /**
             * No not modify filename once final name has been set. </br>
             * This e.g. allows other plugins/crawlers to set desired filenames telling this plugin not to use the default filenames down
             * below.
             */
            return;
        }
        final int videoHeight = link.getIntegerProperty(PROPERTY_HEIGHT, 0);
        final int bitrate = link.getIntegerProperty(PROPERTY_BITRATE, -1);
        final int bandwidth = link.getIntegerProperty(PROPERTY_BANDWIDTH, 0);
        String name = link.getStringProperty(PRESET_NAME_PROPERTY);
        if (name == null) {
            name = link.isNameSet() ? link.getName() : getFileNameFromURL(new URL(link.getPluginPatternMatcher().replaceFirst("m3u8s?", "https://")));
        }
        /* .m3u8 is not a valid file extension and we don't want to have this in our filename */
        if (StringUtils.endsWithCaseInsensitive(name, ".m3u8")) {
            name = name.substring(0, name.length() - 5);
        }
        String assumedFileExtension = null;
        final String m3u8CodecsString = link.getStringProperty(PROPERTY_M3U8_CODECS);
        final String codecType = link.getStringProperty(PROPERTY_FFMPEG_CODEC_TYPE);
        final String codedName = link.getStringProperty(PROPERTY_FFMPEG_CODEC_NAME);
        if (m3u8CodecsString != null) {
            final CODEC codec = CODEC.parse(m3u8CodecsString);
            assumedFileExtension = codec.getDefaultExtension();
            if (assumedFileExtension == null) {
                // Unknown/new codec -> Not good
            }
        }
        String audioq = null;
        String videoq = null;
        if ("video".equalsIgnoreCase(codecType) || videoHeight > 0) {
            if (assumedFileExtension == null) {
                assumedFileExtension = "mp4";
            }
            if (videoHeight > 0) {
                videoq = videoHeight + "p";
            }
        } else if ("audio".equalsIgnoreCase(codecType)) {
            if (StringUtils.containsIgnoreCase(audioq, "mp3")) {
                assumedFileExtension = "mp3";
            }
            if (bitrate != -1) {
                if (codedName != null) {
                    audioq = codedName + " " + (bitrate / 1024) + "kbits";
                } else {
                    audioq = (bitrate / 1024) + "kbits";
                }
            } else {
                if (codedName != null) {
                    audioq = codedName;
                }
            }
        }
        if (assumedFileExtension == null) {
            /* Final fallback */
            assumedFileExtension = "m4a";
        }
        if (videoq != null && audioq != null) {
            name += " (" + videoq + "_" + audioq + ")";
        } else if (videoq != null) {
            name += " (" + videoq + ")";
        } else if (audioq != null) {
            name += " (" + audioq + ")";
        }
        final boolean includeBandwidthInFilename;
        if (videoq == null && audioq == null) {
            /* Force bandwidth value in filename */
            includeBandwidthInFilename = true;
        } else {
            /* Add bandwidth value to filename if user wants it */
            includeBandwidthInFilename = PluginJsonConfig.get(GenericM3u8DecrypterConfig.class).isAddBandwidthValueToFilenames();
        }
        if (includeBandwidthInFilename && bandwidth > 0) {
            name += "_bw_" + bandwidth;
        }
        name += "." + assumedFileExtension;
        if (setFinalFilename) {
            link.setFinalFileName(name);
        } else {
            link.setName(name);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleFree(link, link.getPluginPatternMatcher());
    }

    public void handleFree(final DownloadLink link, final String dllink) throws Exception {
        checkFFmpeg(link, "Download a HLS Stream");
        final String cookiesString = link.getStringProperty("cookies");
        if (cookiesString != null) {
            final String host = Browser.getHost(dllink);
            br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
        }
        final String referer = this.getReferer(link);
        if (referer != null) {
            br.getPage(referer);
            br.followRedirect();
        }
        dl = new HLSDownloader(link, br, dllink);
        dl.startDownload();
    }

    /** Converts given URL into an URL which this plugin can handle. */
    public static String createURLForThisPlugin(final String url) {
        return url == null ? null : url.replaceFirst("^http(s?://)", "m3u8$1");
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* This is a generic plugin. Captchas are never required for direct HLS downloads. */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }
}