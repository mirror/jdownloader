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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "svt.se" }, urls = { "https?://(?:www\\.)?(?:svt|svtplay)\\.se/.+" })
public class SvtSe extends PluginForHost {
    public SvtSe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.svtplay.se/";
    }

    private String              videoid = null;
    private Map<String, Object> entries = null;

    @SuppressWarnings({ "deprecation" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.getPage(link.getDownloadURL());
        final String id = new Regex(link.getDownloadURL(), "/video/(\\d+)").getMatch(0);
        final String query = br.getRegex("\"query\"\\s*:\\s*(\\{.*?\\})").getMatch(0);
        if (query != null && id != null) {
            final Map<String, Object> queryMap = restoreFromString(query, TypeRef.MAP);
            if (id.equals(queryMap.get("videoId"))) {
                videoid = (String) queryMap.get("id");
                if (StringUtils.isEmpty(videoid)) {
                    videoid = (String) queryMap.get("modalId");
                }
            }
        }
        if (StringUtils.isEmpty(videoid)) {
            videoid = this.br.getRegex("\"top-area-play-button\"\\s*href\\s*=\\s*\"/video/" + id + "[^\"]*?(?:modal)?Id=(.*?)\"").getMatch(0);
            if (StringUtils.isEmpty(videoid)) {
                videoid = this.br.getRegex("/video/" + id + "[^\"]*?modalId=(.*?)\"").getMatch(0);
                if (StringUtils.isEmpty(videoid)) {
                    this.videoid = this.br.getRegex("data\\-video\\-id\\s*=\\s*\"([^<>\"]+?)\"").getMatch(0);
                    if (StringUtils.isEmpty(videoid)) {
                        videoid = this.br.getRegex("\"videoSvtId\"\\s*:\\s*\"(.*?)\"").getMatch(0);
                        if (StringUtils.isEmpty(videoid)) {
                            videoid = this.br.getRegex("\\\\\"videoSvtId\\\\\"\\s*:\\s*\\\\\"(.*?)\\\\\"").getMatch(0);
                        }
                    }
                }
            }
        }
        /* 404 --> Offline, videoid not found --> No video on page --> Offline */
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (StringUtils.isEmpty(videoid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setLinkID(this.videoid);
        br.getPage("https://api.svt.se/video/" + this.videoid);
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* Strange result on 404: {"message":"To many retry attempts to video API","status":404} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        if (((Boolean) entries.get("live")).booleanValue()) {
            logger.info("Livestreams are not supported");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String date = (String) JavaScriptEngineFactory.walkJson(entries, "rights/date/forDate");
        String title = (String) entries.get("programTitle");
        String subtitle = (String) entries.get("episodeTitle");
        if (subtitle == null) {
            subtitle = "";
        }
        final String date_formatted = formatDate(date);
        String filename = "";
        if (date_formatted != null) {
            filename += date_formatted + "_";
        }
        filename += "svtplay" + "_";
        if (!StringUtils.isEmpty(title)) {
            filename += title;
        }
        filename += subtitle + ".mp4";
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String hls_master = null;
        final List<Object> ressourcelist = (List) entries.get("videoReferences");
        for (final Object videoo : ressourcelist) {
            this.entries = (Map<String, Object>) videoo;
            final String format = (String) entries.get("format");
            if (format == null) {
                continue;
            }
            if (format.equals("hls")) {
                hls_master = (String) entries.get("url");
                if (hls_master != null) {
                    break;
                }
            }
        }
        if (StringUtils.isEmpty(hls_master)) {
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
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, url_hls);
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