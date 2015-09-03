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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "srf.ch", "rts.ch", "rsi.ch", "rtr.ch", "swissinfo.ch" }, urls = { "^https?://(?:www\\.)?srf\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$", "^https?://(?:www\\.)?rts\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$", "^https?://(?:www\\.)?rsi\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$", "^https?://(?:www\\.)?rtr\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$", "^https?://(?:www\\.)?play\\.swissinfo\\.ch/play/.+\\?id=[A-Za-z0-9\\-]+$" }, flags = { 0, 0, 0, 0, 0 })
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
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
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

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String domainpart = new Regex(downloadLink.getDownloadURL(), "https?://(?:www\\.)?([A-Za-z0-9\\.]+)\\.ch/").getMatch(0);
        final String videoid = new Regex(downloadLink.getDownloadURL(), "\\?id=([A-Za-z0-9\\-]+)").getMatch(0);
        final String channelname = convertDomainPartToShortChannelName(domainpart);
        this.br.getPage("http://il.srgssr.ch/integrationlayer/1.0/ue/" + channelname + "/video/play/" + videoid + ".xml");
        final String url_hls_master = this.br.getRegex("<url quality=\"(?:HD|SD)\">(http[^<>\"]*?\\.m3u8)</url>").getMatch(0);
        final String url_rtmp = this.br.getRegex("<url quality=\"(?:HD|HQ|MQ|SQ|SD)\">(rtmpe?://[^<>\"]*?\\.flv)</url>").getMatch(0);
        if (url_hls_master == null && url_rtmp == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Prefer hls over rtmp but sometimes only one of both is available. */
        if (url_hls_master != null) {
            logger.info("Downloading hls");
            this.br.getPage(url_hls_master);
            final String[] medias = this.br.getRegex("#EXT-X-STREAM-INF([^\r\n]+[\r\n]+[^\r\n]+)").getColumn(-1);
            if (medias == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String url_hls = null;
            long bandwidth_highest = 0;
            for (final String media : medias) {
                final String bw = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
                final long bandwidth_temp = Long.parseLong(bw);
                if (bandwidth_temp > bandwidth_highest) {
                    bandwidth_highest = bandwidth_temp;
                    url_hls = new Regex(media, "https?://[^\r\n]+").getMatch(-1);
                }
            }
            if (url_hls == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            try {
            } catch (final Throwable e) {
                this.br.getPage("http://il.srgssr.ch/integrationlayer/1.0/ue/" + channelname + "/video/" + videoid + "/clicked.xml");
            }
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
            /* Hash is wrong (static) but server will accept it anyways so good enough for us now :) */
            rtmp.setSwfUrl("http://tp.srgssr.ch/assets/lib/srg-technical-player/f2ff86c6a1f230060e46122086a7326f-player.swf");
            rtmp.setResume(true);
            ((RTMPDownload) dl).startDownload();
        }
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

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
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