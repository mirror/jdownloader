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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

/*Similar websites: bca-onlive.de, asscompact.de*/
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alphatv.gr" }, urls = { "https?://(?:www\\.)?alphatvdecrypted\\.gr/shows/.+" })
public class AlphatvGr extends PluginForHost {
    public AlphatvGr(PluginWrapper wrapper) {
        super(wrapper);
        // setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.alphatv.gr/terms";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("alphatvdecrypted.gr/", "alphatv.gr/"));
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepBR(this.br);
        br.getPage(link.getDownloadURL());
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = getFilename(this.br);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    public static String getFilename(final Browser br) {
        final String date = br.getRegex("property=\"article:published_time\" content=\"([^<>\"]*?)\"").getMatch(0);
        String title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            title = getFilenameFromUrl(br.getURL());
            if (title == null) {
                /* Final fallback - this should never happen! */
                title = br.getURL();
            }
        }
        String filename = "";
        final String date_formatted = formatDate(date);
        if (date_formatted != null) {
            filename = date_formatted + "_";
        }
        filename += "alphatv_" + title + ".mp4";
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicodeStatic(filename);
        return filename;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String hls_master = this.br.getRegex("(?:\\'|\")(http://[^<>\"]*?\\.m3u8)(?:\\'|\")").getMatch(0);
        final String url_rtmp = this.br.getRegex("(?:\\'|\")(rtmp://[^<>\"]*?\\.mp4)(?:\\'|\")").getMatch(0);
        /* 2018-03-29: http streaming is new and the only streaming method at the moment! */
        String url_http = this.br.getRegex("file\\s*?:\\s*?window\\.[^\"]+\\(\"(path[^<>\"]+\\.mp4)\"\\)\\s*?\\}").getMatch(0);
        if (hls_master == null && url_rtmp == null && url_http == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_http != null) {
            br.getPage("http://www.alphatv.gr/st/st.php?i=" + Encoding.urlEncode(url_http));
            url_http = PluginJSonUtils.getJson(this.br, "o0");
            if (StringUtils.isEmpty(url_http)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url_http, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        } else if (hls_master != null) {
            /* If no rtmp url is available, download HLS */
            br.getPage(hls_master);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
        } else if (url_rtmp != null) {
            /* Prefer rtmp download */
            final Regex rtmp_regex = new Regex(url_rtmp, "(^rtmp://[^/]+/)([^/]+)/(_definst_/)?(mp4:.+)");
            String rtmp_host = rtmp_regex.getMatch(0);
            final String rtmp_app = rtmp_regex.getMatch(1);
            final String url_playpath = rtmp_regex.getMatch(3);
            if (url_playpath == null || rtmp_app == null || rtmp_host == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            rtmp_host += rtmp_app;
            dl = new RTMPDownload(this, downloadLink, url_rtmp);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPlayPath(url_playpath);
            rtmp.setPageUrl(this.br.getURL());
            rtmp.setSwfVfy("http://static.adman.gr/jwplayer.flash.swf");
            // rtmp.setFlashVer("WIN 18,0,0,194");
            rtmp.setApp(rtmp_app);
            rtmp.setUrl(url_rtmp);
            rtmp.setResume(false);
        }
        dl.startDownload();
    }

    public static String formatDate(String input) {
        if (input == null) {
            return null;
        }
        // 2015-06-28T15:00:00+03:00
        /* 2015-06-23T20:15:00+02:00 --> 2015-06-23T20:15:00+0200 */
        input = input.substring(0, input.lastIndexOf(":")) + "00";
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd'T'HH:mm:ssZ", Locale.GERMAN);
        String formattedDate = null;
        final String targetFormat = "yyyy-MM-dd";
        Date theDate = new Date(date);
        try {
            final SimpleDateFormat formatter = new SimpleDateFormat(targetFormat);
            formattedDate = formatter.format(theDate);
        } catch (Exception e) {
            /* prevent input error killing plugin */
            formattedDate = input;
        }
        return formattedDate;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("jwplayer\\.flash\\.swf\"");
    }

    public static String getFilenameFromUrl(final String url) {
        return new Regex(url, "([^/]+)$").getMatch(0);
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "FAST_LINKCHECK", "Enable fast link check? (file name and size won't be shown correctly until downloadstart)").setDefaultValue(true));
    }

    public static String encodeUnicodeStatic(final String input) {
        if (input != null) {
            String output = input;
            output = output.replace(":", ";");
            output = output.replace("|", "¦");
            output = output.replace("<", "[");
            output = output.replace(">", "]");
            output = output.replace("/", "⁄");
            output = output.replace("\\", "∖");
            output = output.replace("*", "#");
            output = output.replace("?", "¿");
            output = output.replace("!", "¡");
            output = output.replace("\"", "'");
            return output;
        }
        return null;
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
}