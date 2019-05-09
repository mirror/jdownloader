//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hds.HDSContainer;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tf1.fr" }, urls = { "https?://(?:www\\.)?(wat\\.tv/video/.*?|tf1\\.fr/.+/videos/[A-Za-z0-9\\-_]+)\\.html" })
public class Tf1Fr extends PluginForHost {
    public Tf1Fr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wat.tv/cgu";
    }

    @Override
    public String rewriteHost(String host) {
        if ("wat.tv".equals(getHost())) {
            if (host == null || "tf1.fr".equals(host)) {
                return "tf1.fr";
            }
        }
        return super.rewriteHost(host);
    }

    /* 2016-04-22: Changed domain from wat.tv to tf1.fr - everything else mostly stays the same */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        String filename = null;
        setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = br.getRegex("<meta name=\"name\" content=\"(.*?)\"").getMatch(0);
        if (filename == null || filename.equals("")) {
            filename = br.getRegex("\\'premium:([^<>\"\\']*?)\\'").getMatch(0);
        }
        if (filename == null || filename.equals("")) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
        }
        if (filename == null || filename.equals("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = encodeUnicode(filename);
        if (filename.endsWith(" - ")) {
            filename = filename.replaceFirst(" \\- $", "");
        }
        filename = Encoding.htmlDecode(filename.trim());
        downloadLink.setName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    public String getFinalLink(final String video_id) throws Exception {
        if (video_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Browser br2 = br.cloneBrowser();
        br2.getPage("http://www.wat.tv/get/webhtml/" + video_id);
        final Map<String, Object> response = JSonStorage.restoreFromString(br2.toString(), TypeRef.HASHMAP);
        return response != null ? (String) response.get("hls") : null;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String video_id = br.getRegex("data-watid=\"(\\d+)\"").getMatch(0);
        if (video_id == null) {
            video_id = br.getRegex("<meta property=\"og:video(:secure_url)?\" content=\"[^\"]+(\\d{6,8})\">").getMatch(1);
            if (video_id == null) {
                video_id = br.getRegex("xtpage = \"[^;]+video\\-(\\d{6,8})\";").getMatch(0);
            }
        }
        String finallink = getFinalLink(video_id);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (finallink.startsWith("rtmp")) {
            /* Old */
            if (System.getProperty("jd.revision.jdownloaderrevision") == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "JD2 BETA needed!");
            }
            /**
             * NOT WORKING IN RTMPDUMP
             */
            final String nw = "rtmpdump";
            if (nw.equals("rtmpdump")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Not supported yet!");
            }
            finallink = finallink.replaceAll("^.*?:", "rtmpe:");
            finallink = finallink.replaceAll("watestreaming/", "watestreaming/#");
            dl = new RTMPDownload(this, downloadLink, finallink);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
            rtmp.setUrl(finallink.substring(0, finallink.indexOf("#")));
            rtmp.setPlayPath(finallink.substring(finallink.indexOf("#") + 1));
            rtmp.setApp(new Regex(finallink.substring(0, finallink.indexOf("#")), "[a-zA-Z]+://.*?/(.*?)$").getMatch(0));
            rtmp.setSwfVfy("http://www.wat.tv/images/v40/PlayerWat.swf");
            rtmp.setResume(true);
            ((RTMPDownload) dl).startDownload();
        } else if (finallink.contains(".f4m?")) {
            // HDS
            br.getPage(finallink);
            final List<HDSContainer> all = HDSContainer.getHDSQualities(br);
            if (all == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                final HDSContainer read = HDSContainer.read(downloadLink);
                final HDSContainer hit;
                if (read != null) {
                    hit = HDSContainer.getBestMatchingContainer(all, read);
                } else {
                    hit = HDSContainer.findBestVideoByResolution(all);
                }
                if (hit == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    hit.write(downloadLink);
                    final HDSDownloader dl = new HDSDownloader(downloadLink, br, hit.getFragmentURL());
                    this.dl = dl;
                    dl.setEstimatedDuration(hit.getDuration());
                    dl.startDownload();
                }
            }
        } else if (finallink.contains(".m3u8")) {
            // HLS
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            final String m3u8 = finallink.replaceAll("(&(min|max)_bitrate=\\d+)", "");
            final List<HlsContainer> qualities = HlsContainer.getHlsQualities(br, m3u8);
            final HlsContainer best = HlsContainer.findBestVideoByBandwidth(qualities);
            if (best == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new HLSDownloader(downloadLink, br, best.getDownloadurl());
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (link != null) {
            HDSContainer.clear(link);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }
}