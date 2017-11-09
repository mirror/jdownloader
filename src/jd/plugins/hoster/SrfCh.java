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
import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "srf.ch", "rts.ch", "rsi.ch", "rtr.ch", "swissinfo.ch" }, urls = { "^https?://(?:www\\.)?srf\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+.*$", "^https?://(?:www\\.)?rts\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$", "^https?://(?:www\\.)?rsi\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$", "^https?://(?:www\\.)?rtr\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$", "^https?://(?:www\\.)?play\\.swissinfo\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$" })
public class SrfCh extends PluginForHost {
    @SuppressWarnings("deprecation")
    public SrfCh(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.srf.ch/allgemeines/impressum";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(410);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta name=\"title\" content=\"([^<>]*?) \\- Play [^\"]+\"").getMatch(0);
        if (filename == null) {
            /* Get filename via url */
            filename = new Regex(link.getDownloadURL(), "/([^/]+)\\?id=.+").getMatch(0);
            filename = filename.replace("-", " ");
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename.trim()) + ".mp4";
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String domainpart = new Regex(downloadLink.getDownloadURL(), "https?://(?:www\\.)?([A-Za-z0-9\\.]+)\\.ch/").getMatch(0);
        final String videoid = new Regex(downloadLink.getDownloadURL(), "\\?id=([A-Za-z0-9\\-]+)").getMatch(0);
        final String channelname = convertDomainPartToShortChannelName(domainpart);
        /* xml also possible: http://il.srgssr.ch/integrationlayer/1.0/<channelname>/srf/video/play/<videoid>.xml */
        this.br.getPage("http://il.srgssr.ch/integrationlayer/1.0/ue/" + channelname + "/video/play/" + videoid + ".json");
        String url_http_download = null;
        String url_hls_master = null;
        String url_rtmp = null;
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("Video");
        LinkedHashMap<String, Object> temp = null;
        ArrayList<Object> ressourcelist = null;
        /* Try to find http downloadurl (not always available) */
        try {
            ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "Downloads/Download");
            for (final Object streamtypeo : ressourcelist) {
                temp = (LinkedHashMap<String, Object>) streamtypeo;
                final String protocol = (String) temp.get("@protocol");
                if (protocol == null) {
                    continue;
                }
                if (protocol.equals("HTTP")) {
                    url_http_download = this.findBestQualityForStreamtype(temp);
                    break;
                }
            }
        } catch (final Throwable e) {
        }
        /* Try to find hls master (usually available) */
        try {
            ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "Playlists/Playlist");
            for (final Object streamtypeo : ressourcelist) {
                temp = (LinkedHashMap<String, Object>) streamtypeo;
                final String protocol = (String) temp.get("@protocol");
                if (protocol == null) {
                    continue;
                }
                if (protocol.equals("HTTP-HLS")) {
                    url_hls_master = this.findBestQualityForStreamtype(temp);
                    break;
                }
            }
        } catch (final Throwable e) {
        }
        /* Try to find rtmp url (sometimes available, sometimes the only streamtype available) */
        try {
            ressourcelist = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "Playlists/Playlist");
            for (final Object streamtypeo : ressourcelist) {
                temp = (LinkedHashMap<String, Object>) streamtypeo;
                final String protocol = (String) temp.get("@protocol");
                if (protocol == null) {
                    continue;
                }
                if (protocol.equals("RTMP")) {
                    url_rtmp = findBestQualityForStreamtype(temp);
                    break;
                }
            }
        } catch (final Throwable e) {
        }
        if (url_http_download == null && url_hls_master == null && url_rtmp == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (url_http_download != null) {
            /* Prefer http download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url_http_download, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.getHttpConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 3 * 60 * 1000l);
                } else if (br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 3 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
            this.dl.startDownload();
        } else if (url_hls_master != null) {
            /* Prefer hls over rtmp but sometimes only one of both is available. */
            logger.info("Downloading hls");
            this.br.getPage(url_hls_master);
            // try {
            // this.br.cloneBrowser().getPage("http://il.srgssr.ch/integrationlayer/1.0/ue/" + channelname + "/video/" + videoid +
            // "/clicked.xml");
            // } catch (final Throwable e) {
            // }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else {
            logger.info("Downloading rtmp");
            final String app = "ondemand";
            final String playpath = new Regex(url_rtmp, app + "/" + "(.+)\\.flv$").getMatch(0);
            try {
                dl = new RTMPDownload(this, downloadLink, url_rtmp);
            } catch (final NoClassDefFoundError e) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "RTMPDownload class missing");
            }
            /* Setup rtmp connection */
            jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setUrl(url_rtmp);
            rtmp.setPlayPath(playpath);
            rtmp.setApp("ondemand");
            rtmp.setFlashVer("WIN 18,0,0,232");
            /* Hash is wrong (static) but server will accept it anyways so good enough for us for now :) */
            rtmp.setSwfUrl("http://tp.srgssr.ch/assets/lib/srg-technical-player/f2ff86c6a1f230060e46122086a7326f-player.swf");
            rtmp.setResume(true);
            ((RTMPDownload) dl).startDownload();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private String findBestQualityForStreamtype(LinkedHashMap<String, Object> temp) {
        final String[] possibleQualities = { "HD", "HQ", "MQ", "SQ", "SD" };
        String best_quality = null;
        ArrayList<Object> ressourcelist2 = (ArrayList) temp.get("url");
        for (final String possible_quality : possibleQualities) {
            for (final Object hlso : ressourcelist2) {
                temp = (LinkedHashMap<String, Object>) hlso;
                final String quality = (String) temp.get("@quality");
                if (quality == null) {
                    continue;
                }
                if (quality.equals(possible_quality)) {
                    /* We found the best available quality */
                    best_quality = (String) temp.get("text");
                    return best_quality;
                }
            }
        }
        return best_quality;
    }

    private String convertDomainPartToShortChannelName(final String input) {
        final String output;
        if (input.equals("play.swissinfo")) {
            output = "swi";
        } else {
            output = input;
        }
        return output;
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