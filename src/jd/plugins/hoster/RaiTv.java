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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rai.tv" }, urls = { "https?://rai_host_plugin_notneeded_at_the_moment" })
public class RaiTv extends PluginForHost {

    public RaiTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.rai.it/dl/rai/text/ContentItem-5a0d5bc3-9f0e-4f6b-8b65-13dd14385123.html";
    }

    private static final String TYPE_CONTENTITEM                     = ".+/dl/[^<>\"]+/ContentItem\\-[a-f0-9\\-]+\\.html$";
    private String              dllink                               = null;
    private boolean             possibleNotDownloadableMSSilverlight = false;

    private Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    /*
     * Example for json for single video:
     * http://www.raiplay.it/video/2017/04/Il-tempo-e-la-Storia---Athanasius-Kircher-090ef888-7dea-4f8b-b5fb-a985dae7a07f.html?json
     */
    /** THX: https://github.com/nightflyer73/plugin.video.raitv/tree/master/resources/lib */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        possibleNotDownloadableMSSilverlight = false;
        this.setBrowserExclusive();
        prepBR(this.br);
        this.br.getPage(link.getDownloadURL());
        /* Do NOT use value of "videoURL_MP4" here! */
        /* E.g. http://www.rai.tv/dl/RaiTV/programmi/media/ContentItem-70996227-7fec-4be9-bc49-ba0a8104305a.html */
        dllink = this.br.getRegex("var[\t\n\r ]*?videoURL[\t\n\r ]*?=[\t\n\r ]*?\"(http://[^<>\"]+)\"").getMatch(0);
        String content_id_from_url = null;
        if (link.getDownloadURL().matches(TYPE_CONTENTITEM)) {
            content_id_from_url = new Regex(link.getDownloadURL(), "(\\-[a-f0-9\\-]+)\\.html$").getMatch(0);
        }
        final String contentset_id = this.br.getRegex("var[\t\n\r ]*?urlTop[\t\n\r ]*?=[\t\n\r ]*?\"[^<>\"]+/ContentSet([A-Za-z0-9\\-]+)\\.html").getMatch(0);
        final String content_id_from_html = this.br.getRegex("id=\"ContentItem(\\-[a-f0-9\\-]+)\"").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404 || (contentset_id == null && content_id_from_html == null && dllink == null)) {
            /* Probably not a video/offline */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = null;
        String extension = ".mp4";
        String date = null;
        String date_formatted = null;
        String description = null;
        if (dllink != null) {
            /* Streamurls directly in html */
            filename = this.br.getRegex("id=\"idMedia\">([^<>]+)<").getMatch(0);
            date = this.br.getRegex("id=\"myGenDate\">(\\d{2}\\-\\d{2}\\-\\d{4} \\d{2}:\\d{2})<").getMatch(0);
            possibleNotDownloadableMSSilverlight = this.br.containsHTML("id=\"silverlightControlHost\"");
        } else {
            LinkedHashMap<String, Object> entries = null;
            if (content_id_from_html != null) {
                /* Easiest way to find videoinfo */
                this.br.getPage("http://www.rai.tv/dl/RaiTV/programmi/media/ContentItem" + content_id_from_html + ".html?json");
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            }
            if (entries == null) {
                final ArrayList<Object> ressourcelist;
                final String list_json_from_html = this.br.getRegex("\"list\"[\t\n\r ]*?:[\t\n\r ]*?(\\[.*?\\}[\t\n\r ]*?\\])").getMatch(0);
                if (list_json_from_html != null) {
                    ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(list_json_from_html);
                } else {
                    br.getPage("http://www.rai.tv/dl/RaiTV/ondemand/ContentSet" + contentset_id + ".html?json");
                    if (br.getHttpConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                    ressourcelist = (ArrayList<Object>) entries.get("list");
                }

                if (content_id_from_url == null) {
                    /* Hm probably not a video */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                String content_id_temp = null;
                boolean foundVideoInfo = false;
                for (final Object videoo : ressourcelist) {
                    entries = (LinkedHashMap<String, Object>) videoo;
                    content_id_temp = (String) entries.get("itemId");
                    if (content_id_temp != null && content_id_temp.contains(content_id_from_url)) {
                        foundVideoInfo = true;
                        break;
                    }
                }
                if (!foundVideoInfo) {
                    /* Probably offline ... */
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            date = (String) entries.get("date");
            filename = (String) entries.get("name");
            description = (String) entries.get("desc");
            final String type = (String) entries.get("type");
            if (type.equalsIgnoreCase("RaiTv Media Video Item")) {
            } else {
                /* TODO */
                logger.warning("Unsupported media type!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            extension = "mp4";
            dllink = (String) entries.get("h264");
            if (dllink == null || dllink.equals("")) {
                dllink = (String) entries.get("m3u8");
                extension = "mp4";
            }
            if (dllink == null || dllink.equals("")) {
                dllink = (String) entries.get("wmv");
                extension = "wmv";
            }
            if (dllink == null || dllink.equals("")) {
                dllink = (String) entries.get("mediaUri");
                extension = "mp4";
            }
        }
        if (filename == null) {
            filename = content_id_from_url;
        }
        if (description != null && link.getComment() == null) {
            link.setComment(description);
        }
        date_formatted = formatDate(date);
        if (date_formatted != null) {
            filename = date_formatted + "_";
        }
        filename += "raitv_" + filename + "." + extension;
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (dllink.contains(".m3u8")) {
            /* hls */
            /* Access hls master */
            this.br.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else {
            /* http */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
        }
    }

    public static Browser prepVideoBrowser(final Browser br) {
        /*
         * 2016-08-24: Do NOT use the Apache User-Agent anymore - because of it we often got .ism MS Silverlight streams instead of http
         * urls.
         */
        /* Rai.tv android app User-Agent - not necessarily needed! */
        // br.getHeaders().put("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:48.0) Gecko/20100101 Firefox/48.0");
        return br;
    }

    public static void accessCont(final Browser br, final String cont) throws IOException {
        /**
         * # output=20 url in body<br />
         * # output=23 HTTP 302 redirect<br />
         * # output=25 url and other parameters in body, space separated<br />
         * # output=44 XML (not well formatted) in body<br />
         * # output=45 XML (website standard) in body<br />
         * # output=47 json in body<br />
         * # pl=native,flash,silverlight<br />
         * # BY DEFAULT (website): pl=mon,flash,native,silverlight<br />
         * # A stream will be returned depending on the UA (and pl parameter?)<br />
         */
        br.getPage("http://mediapolisvod.rai.it/relinker/relinkerServlet.htm?cont=" + cont + "&output=45&pl=native,flash,silverlight&_=" + System.currentTimeMillis());
    }

    public static String getDllink(final Browser br) {
        String dllink = br.getRegex("<url type=\"content\">([^<>\"]+)<").getMatch(0);
        if (dllink != null && dllink.startsWith("mms://")) {
            /* Convert mms to http */
            dllink = dllink.replace("mms://", "http://");
        }
        return dllink;
    }

    public static String getContFromRelinkerUrl(final String relinker) {
        return new Regex(relinker, "cont=([^<>\"=\\&]+)").getMatch(0);
    }

    public static boolean dllinkIsDownloadable(final String dllink) {
        if (dllink == null || !dllink.startsWith("http")) {
            return false;
        }
        return true;
    }

    public static String formatDate(final String input) {
        if (input == null) {
            return null;
        }
        final long date;
        if (input.matches("\\d{2}/\\d{2}/\\d{4}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd/MM/yyyy", Locale.ENGLISH);
        } else if (input.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        } else if (input.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd", Locale.ENGLISH);
        } else if (input.matches("\\d{2}\\-\\d{2}\\-\\d{4}")) {
            date = TimeFormatter.getMilliSeconds(input, "dd-MM-yyyy", Locale.ENGLISH);
        } else {
            date = TimeFormatter.getMilliSeconds(input, "dd-MM-yyyy HH:mm", Locale.ENGLISH);
        }
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