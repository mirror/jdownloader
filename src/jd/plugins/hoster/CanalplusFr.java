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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "canalplus.fr" }, urls = { "https?://(?:www\\.)?canalplus\\.fr/[^<>\"]+\\.html\\?vid=\\d+" })
public class CanalplusFr extends PluginForHost {

    public CanalplusFr(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.canalplus.fr/";
    }

    private static final String default_Extension    = ".mp4";

    private boolean             geoblocked           = false;

    private String              videoid              = null;
    private String              http_highest_quality = null;
    private String              hls_master           = null;

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        http_highest_quality = null;
        hls_master = null;
        this.geoblocked = false;
        String ext = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.videoid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        link.setLinkID(this.videoid);
        /* Without format=json we will get XML */
        br.getPage("http://service.canal-plus.com/video/rest/getVideosLiees/cplus/" + this.videoid + "?format=json");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Object jsono = JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        if (jsono == null || !(jsono instanceof ArrayList)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<Object> ressourcelist = (ArrayList) jsono;
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) ressourcelist.get(0);
        boolean found_target_json = false;
        String url_temp = null;
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            url_temp = (String) entries.get("URL");
            if (url_temp == null) {
                continue;
            }
            if (url_temp.contains("?vid=" + this.videoid)) {
                found_target_json = true;
                break;
            }
        }
        if (!found_target_json) {
            /* That should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final LinkedHashMap<String, Object> videos = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "MEDIA/VIDEOS");
        entries = (LinkedHashMap<String, Object>) entries.get("INFOS");

        final String description = (String) entries.get("DESCRIPTION");
        final String channel = (String) entries.get("AUTEUR");
        final String date = (String) JavaScriptEngineFactory.walkJson(entries, "PUBLICATION/DATE");
        final String title = (String) JavaScriptEngineFactory.walkJson(entries, "TITRAGE/TITRE");
        final String subtitle = (String) JavaScriptEngineFactory.walkJson(entries, "TITRAGE/SOUS_TITRE");
        if (title == null || subtitle == null || channel == null || date == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String date_formatted = formatDate(date);
        String filename = date_formatted + "_" + channel + "_" + title + " - " + subtitle;
        filename = encodeUnicode(filename);

        long http_max_bitrate = 0;
        long http_temp_bitrate = 0;
        for (final Map.Entry<String, Object> entry : videos.entrySet()) {
            final String type = entry.getKey();
            final String url = (String) entry.getValue();
            if (url == null) {
                continue;
            }
            if (type.equals("HDS")) {
                continue;
            } else if (type.equals("HLS")) {
                hls_master = url;
                continue;
            }

            final String bitrate = new Regex(url, "_(\\d+)k\\.mp4").getMatch(0);
            if (bitrate != null) {
                http_temp_bitrate = Long.parseLong(bitrate);
            } else {
                /* Use dummy bitrates */
                if (url.contains("video_H")) {
                    http_temp_bitrate = 200;
                } else {
                    /* video_L */
                    http_temp_bitrate = 100;
                }
            }
            if (http_temp_bitrate > http_max_bitrate) {
                http_max_bitrate = http_temp_bitrate;
                http_highest_quality = url;
            }

        }

        /* Try to get filesize from http url */
        if (http_highest_quality != null) {
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br.openHeadConnection(http_highest_quality);
                } catch (final BrowserException e) {
                    /* Fallback to hls if possible */
                    http_highest_quality = null;
                }
                if (!con.getContentType().contains("html")) {
                    final String filename_temp = getFileNameFromHeader(con);
                    if ("blocage.flv".equals(filename_temp)) {
                        this.geoblocked = true;
                    } else {
                        link.setDownloadSize(con.getLongContentLength());
                    }
                    /* They sometimes have .flv videos */
                    ext = getFileNameExtensionFromString(http_highest_quality, default_Extension);
                } else {
                    /* Fallback to hls if possible */
                    http_highest_quality = null;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            ext = ".mp4";
        }

        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        filename += ext;

        link.setFinalFileName(filename);
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (this.geoblocked) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
        }
        if (http_highest_quality == null && hls_master == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (http_highest_quality != null) {
            /* http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, http_highest_quality, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } else {
            this.br.getPage(this.hls_master);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                this.geoblocked = true;
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        }
    }

    private String formatDate(final String input) {
        if (input == null) {
            return null;
        }
        final long date = TimeFormatter.getMilliSeconds(input, "dd/MM/yyyy", Locale.FRANCE);
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