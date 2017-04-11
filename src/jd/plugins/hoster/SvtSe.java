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

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "svt.se" }, urls = { "https?://(?:www\\.)?(?:svt|svtplay)\\.se/.+" })
public class SvtSe extends PluginForHost {

    public SvtSe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.svtplay.se/";
    }

    private String                        videoid = null;
    private LinkedHashMap<String, Object> entries = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.getPage(link.getDownloadURL());
        this.videoid = this.br.getRegex("data\\-video\\-id=\"([^<>\"]*?)\"").getMatch(0);
        /* 404 --> Offline, videoid not found --> No video on page --> Offline */
        if (this.br.getHttpConnection().getResponseCode() == 404 || this.videoid == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setLinkID(this.videoid);
        br.getPage("http://www.svt.se/videoplayer-api/video/" + this.videoid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Strange result on 404: {"message":"To many retry attempts to video API","status":404} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());

        final String channel = "svtplay";
        final String date = (String) JavaScriptEngineFactory.walkJson(entries, "rights/date/forDate");
        final String title = (String) entries.get("programTitle");
        final String subtitle = (String) entries.get("episodeTitle");
        if (title == null || subtitle == null || channel == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String date_formatted = formatDate(date);
        String filename = "";
        if (date_formatted != null) {
            filename += date_formatted + "_";
        }
        filename += channel + "_" + title + " - " + subtitle + ".mp4";
        filename = encodeUnicode(filename);

        link.setFinalFileName(filename);

        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String hls_master = null;
        final ArrayList<Object> ressourcelist = (ArrayList) entries.get("videoReferences");
        for (final Object videoo : ressourcelist) {
            this.entries = (LinkedHashMap<String, Object>) videoo;
            final String format = (String) entries.get("format");
            if (format == null) {
                continue;
            }
            if (format.equals("hls")) {
                hls_master = (String) entries.get("url");
                break;
            }
        }
        if (hls_master == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage(hls_master);
        if (this.br.getHttpConnection().getResponseCode() == 403) {
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

    private String formatDate(String input) {
        if (input == null) {
            return null;
        }
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