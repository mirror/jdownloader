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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "alphatv.gr" }, urls = { "https?://(?:www\\.)?alphatv\\.gr/.+vtype=[a-z0-9]+\\&vid=\\d+.+" })
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

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "id=(\\d+)").getMatch(0);
    }

    /** Plugin for old website layout: rev: 39318 */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        prepBR(this.br);
        br.getHeaders().put("x-requested-with", "XMLHttpRequest");
        String parameters = new Regex(link.getPluginPatternMatcher(), "(vid=\\d+.*?\\&showId=\\d+)").getMatch(0);
        if (parameters == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage("https://www.alphatv.gr/ajax/Isobar.AlphaTv.Components.PopUpVideo.PopUpVideo.PlayMedia/?" + parameters);
        if (isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = getFilename(this.br);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    public static String getFilename(final Browser br) {
        String title = br.getRegex("id\\s*=\\s*\"currentVideoTitle\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        if (title == null) {
            title = br.getRegex("/([^/]+)\\.mp4").getMatch(0);
            if (title == null) {
                title = getFilenameFromUrl(br.getURL());
                if (title == null) {
                    /* Final fallback - this should never happen! */
                    title = br.getURL();
                }
            }
        }
        final String date = br.getRegex("/(\\d{8})_([^/]+)\\.mp4").getMatch(0);
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String hls_master = null;
        /* 2018-11-15: http streaming is the only streaming method at the moment! */
        String json = br.getRegex("id=\"currentvideourl\" data-plugin-k?player=\"(\\{[^\"]+\\})\"").getMatch(0);
        String url_http = this.br.getRegex("id=\"currentvideourl\" data\\-url=\"(https?://[^\"]+)\"").getMatch(0);
        if (url_http == null && json != null) {
            json = Encoding.htmlDecode(json);
            url_http = PluginJSonUtils.getJson(json, "url");
        }
        if (hls_master == null && url_http == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_http != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, url_http, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
                }
            }
        } else if (hls_master != null) {
            /* If no rtmp url is available, download HLS */
            br.getPage(hls_master);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, url_hls);
        }
        dl.startDownload();
    }

    public static String formatDate(final String input) {
        if (input == null) {
            return null;
        }
        final long date = TimeFormatter.getMilliSeconds(input, "yyyyMMdd", Locale.GERMAN);
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
        return br.getHttpConnection().getResponseCode() == 404;
    }

    public static String getFilenameFromUrl(final String url) {
        return new Regex(url, "shows?/([^/\\?&]+)").getMatch(0);
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