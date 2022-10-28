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

import java.net.URL;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.Stream;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
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
    public static final String PRESET_NAME_PROPERTY = "preSetName";

    public GenericM3u8(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getHost(final DownloadLink link, final Account account, boolean includeSubdomain) {
        if (link != null) {
            return Browser.getHost(link.getPluginPatternMatcher(), includeSubdomain);
        }
        return super.getHost(link, account, includeSubdomain);
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

    public AvailableStatus requestFileInformation(final DownloadLink link, final String dllink) throws Exception {
        checkFFProbe(link, "Download a HLS Stream");
        this.setBrowserExclusive();
        final String cookiesString = link.getStringProperty("cookies");
        if (cookiesString != null) {
            final String host = Browser.getHost(dllink);
            br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
        }
        final String referer = link.getStringProperty("Referer");
        if (referer != null) {
            br.getPage(referer);
            br.followRedirect();
        }
        final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
        final StreamInfo streamInfo = downloader.getProbe();
        if (downloader.isEncrypted()) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Encrypted HLS(" + downloader.getEncryptionMethod() + ") is not supported!");
        } else if (streamInfo == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final int hlsBandwidth = link.getIntegerProperty("hlsBandwidth", -1);
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
        String videoq = null;
        String audioq = null;
        String extension = "m4a";
        for (Stream s : streamInfo.getStreams()) {
            if ("video".equalsIgnoreCase(s.getCodec_type())) {
                extension = "mp4";
                if (s.getHeight() > 0) {
                    videoq = s.getHeight() + "p";
                }
            } else if ("audio".equalsIgnoreCase(s.getCodec_type())) {
                if (s.getBit_rate() != null) {
                    if (s.getCodec_name() != null) {
                        audioq = s.getCodec_name() + " " + (Integer.parseInt(s.getBit_rate()) / 1024) + "kbits";
                    } else {
                        audioq = (Integer.parseInt(s.getBit_rate()) / 1024) + "kbits";
                    }
                } else {
                    if (s.getCodec_name() != null) {
                        audioq = s.getCodec_name();
                    }
                }
            }
        }
        if (link.getFinalFileName() == null) {
            String name = link.getStringProperty(PRESET_NAME_PROPERTY);
            if (name == null) {
                name = link.isNameSet() ? link.getName() : getFileNameFromURL(new URL(link.getPluginPatternMatcher()));
            }
            if (StringUtils.endsWithCaseInsensitive(name, ".m3u8")) {
                name = name.substring(0, name.length() - 5);
            }
            if (videoq != null && audioq != null) {
                name += " (" + videoq + "_" + audioq + ")";
            } else if (videoq != null) {
                name += " (" + videoq + ")";
            } else if (audioq != null) {
                name += " (" + audioq + ")";
                if (StringUtils.containsIgnoreCase(audioq, "mp3")) {
                    extension = "mp3";
                }
            }
            name += "." + extension;
            link.setFinalFileName(name);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleFree(link, link.getPluginPatternMatcher());
    }

    public void handleFree(final DownloadLink url, final String dllink) throws Exception {
        checkFFmpeg(url, "Download a HLS Stream");
        final String cookiesString = url.getStringProperty("cookies", null);
        if (cookiesString != null) {
            final String host = Browser.getHost(dllink);
            br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
        }
        final String referer = url.getStringProperty("Referer", null);
        if (referer != null) {
            br.getPage(referer);
            br.followRedirect();
        }
        dl = new HLSDownloader(url, br, dllink);
        dl.startDownload();
    }

    /** Converts given URL into an URL which this plugin can handle. */
    public static String createURLForThisPlugin(final String url) {
        final String protocolPart = new Regex(url, "(?i)^http(s?://)").getMatch(0);
        if (protocolPart != null) {
            return url.replaceFirst("https?://", "m3u8" + protocolPart);
        } else {
            return url;
        }
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