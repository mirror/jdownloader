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
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "francetelevisions.fr" }, urls = { "http://francetelevisionsdecrypted/[A-Za-z0-9\\-_]+@[A-Za-z0-9\\-_]+" })
public class FrancetelevisionsCom extends PluginForHost {
    public FrancetelevisionsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.francetv.fr/";
    }

    Map<String, Object> entries = null;

    /**
     * Basically all french public TV stations use 1 network / API and we only need this one API request to get all information needed to
     * download their videos :)
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        this.br.setAllowedResponseCodes(400);
        this.br.setFollowRedirects(true);
        final String[] videoinfo = link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/") + 1).split("@");
        final String videoid = videoinfo[0];
        /* 2017-05-10: The 'catalogue' parameter is not required anymore or at least not for all videoids! */
        final String catalogue = videoinfo[1].equals("null") ? "" : videoinfo[1];
        // device_type=desktop -> mpd -> we can convert to m3u8 (manifest.mpd to master.m3u8) but drm protected
        // device_type=mobile -> m3u8, but drm protected
        final String json = this.br.getPage("https://player.webservices.francetelevisions.fr/v1/videos/" + videoid + "?country_code=FR&catalogue=" + catalogue + "&device_type=desktop&browser=chrome");
        final long responsecode = this.br.getHttpConnection().getResponseCode();
        if (responsecode == 400 || responsecode == 404 || json == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        String title = (String) JavaScriptEngineFactory.walkJson(entries, "meta/title");
        if (StringUtils.isEmpty(title)) {
            title = (String) JavaScriptEngineFactory.walkJson(entries, "markers/npaw/title");
        }
        String subtitle = (String) JavaScriptEngineFactory.walkJson(entries, "meta/additional_title");
        if (StringUtils.isEmpty(subtitle)) {
            subtitle = (String) JavaScriptEngineFactory.walkJson(entries, "markers/npaw/title_episode");
        }
        final String date = (String) JavaScriptEngineFactory.walkJson(entries, "meta/broadcasted_at");
        String channel = (String) JavaScriptEngineFactory.walkJson(entries, "markers/npaw/channel");
        final String seasonEpisode = (String) JavaScriptEngineFactory.walkJson(entries, "markers/estat/newLevel4");
        if (StringUtils.isEmpty(title)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.isEmpty(channel)) {
            /* Fallback to default channel */
            channel = "francetelevisions";
        }
        String filename = "";
        if (!StringUtils.isEmpty(date)) {
            /* Yap - the date is not always given! */
            final String date_formatted = formatDate(date);
            filename += date_formatted + "_";
        }
        filename += channel + "_" + title;
        if (StringUtils.isNotEmpty(seasonEpisode)) {
            filename += "_" + seasonEpisode;
        }
        if (!StringUtils.isEmpty(subtitle)) {
            filename += " - " + subtitle;
        }
        filename += ".mp4";
        filename = Encoding.htmlDecode(filename).trim();
        link.setFinalFileName(filename);
        if (Boolean.TRUE.equals(JavaScriptEngineFactory.walkJson(entries, "video/drm"))) {
            final String type = StringUtils.valueOrEmpty(StringUtils.valueOfOrNull(JavaScriptEngineFactory.walkJson(entries, "video/drm_type")));
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "DRM '" + type + "' protected");
        } else if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            // 89353
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "unsupported streaming format");
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String hls_master = null;
        String url_http = null;
        String url_temp = null;
        final ArrayList<Object> ressourcelist = new ArrayList<Object>();
        ressourcelist.add(entries.get("video"));
        final ArrayList<String> hls = new ArrayList<String>();
        for (final Object videoo : ressourcelist) {
            final Map<String, Object> entries = (Map<String, Object>) videoo;
            String format = (String) entries.get("format");
            url_temp = (String) entries.get("url");
            if (StringUtils.isEmpty(format) || StringUtils.isEmpty(url_temp)) {
                continue;
            } else if ("dash".equals(format)) {
                if (false) {
                    // convert to hls, but drm protected
                    format = "hls";
                    url_temp = url_temp.replace("manifest.mpd", "master.m3u8");
                } else {
                    // unsupported,89353
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "unsupported streaming format");
                }
            }
            if ("mp4-dp".equals(format)) {
                url_http = url_temp;
            } else if ("hls".equals(format) || "m3u8".equals(format)) {
                for (final String host : new String[] { "hdfauthftv-a.akamaihd.net", "hdfauth.francetv.fr" }) {
                    try {
                        final Browser brc = br.cloneBrowser();
                        brc.setFollowRedirects(true);
                        brc.getPage("https://" + host + "/esi/TA?format=json&url=" + Encoding.urlEncode(url_temp));
                        final Map<String, Object> response = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                        final String url = (String) response.get("url");
                        if (StringUtils.isNotEmpty(url)) {
                            final Browser brc2 = br.cloneBrowser();
                            brc2.getPage(url);
                            if (brc2.getHttpConnection().isOK() && !hls.contains(url)) {
                                hls.add(url);
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        logger.log(e);
                    }
                }
                if (StringUtils.isEmpty(hls_master)) {
                    hls_master = url_temp;
                }
            }
        }
        if (hls.size() > 0) {
            logger.info("hls:" + hls);
            hls_master = hls.get(0);
        }
        if (hls_master == null && url_http == null) {
            /*
             * That should never happen. Even geo-blocked users should get the final downloadlinks even though download attempts will end up
             * in response code 403 which is handled correctly below.
             */
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_http != null) {
            /* Download http (rare case!) */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url_http, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else {
            // this.br.getPage("http://hdfauthftv-a.akamaihd.net/esi/TA?format=json&url=" + Encoding.urlEncode(hls_master) +
            // "&callback=_jsonp_loader_callback_request_2");
            // final String json = this.br.getRegex("^_jsonp_loader_callback_request_2\\((.+)\\)$").getMatch(0);
            // entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
            // hls_master = (String) entries.get("url");
            // this.br.getPage(hls_master);
            /* Download hls */
            br.getPage(hls_master);
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "This content is not available in your country");
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
        }
        dl.startDownload();
    }

    private String formatDate(final String input) {
        final long date = TimeFormatter.getMilliSeconds(input, "yyyy'-'MM'-'dd'T'HH:mm", Locale.FRANCE);
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