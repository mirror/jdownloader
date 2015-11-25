//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tvi.iol.pt", "tvi24.iol.pt", "tviplayer.iol.pt" }, urls = { "http://(?:www\\.)?tvi\\.iol\\.pt/mediacenter\\.html\\?(load=\\d+\\&gal_id=\\d+|mul_id=\\d+\\&load=\\d+&pagina=\\d+\\&pos=\\d+)", "http://(?:www\\.)?tvi24\\.iol\\.pt/videos/[^/]+/[^/]+/[^/]+", "http://(?:www\\.)?tviplayer\\.iol\\.pt/programa/[^/]+/[^/]+/video/[^/]+" }, flags = { 32, 32, 32 })
public class TviIolPt extends PluginForHost {

    private String clipUrl              = null;
    private String clipNetConnectionUrl = null;

    public TviIolPt(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://tviplayer.iol.pt/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String type_1 = "http://(?:www\\.)?tvi\\.iol\\.pt/mediacenter\\.html\\?(load=\\d+\\&gal_id=\\d+|mul_id=\\d+\\&load=\\d+&pagina=\\d+\\&pos=\\d+)";
    private static final String type_2 = "http://(?:www\\.)?tvi24\\.iol\\.pt/.+";
    private static final String type_3 = "http://(?:www\\.)?tviplayer\\.iol\\.pt/programa/[^/]+/[^/]+/video/[^/]+";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename;
        String ext;
        if (downloadLink.getDownloadURL().matches(type_1)) {
            final String id = br.getRegex("createPlayer\\((\\n\\s+)?(\\d+),").getMatch(1);
            if (id == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.getHost().contains("tvi24")) {
                filename = br.getRegex("<title>(.*?)>").getMatch(0);
            } else {
                filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
            }
            if (filename == null) {
                if (br.getHost().contains("tvi24")) {
                    filename = br.getRegex("<div class=\"article-promo\">.*?<h1>(.*?)</h1>").getMatch(0);
                } else {
                    filename = br.getRegex("<h1><span>.*?-.*?- (.*?)</span></h1>").getMatch(0);
                }
            }
            br.getPage("http://www.tvi.iol.pt/config.html?id=" + id);
            clipUrl = br.getRegex("file\":\"(.*?)\"").getMatch(0);
            clipNetConnectionUrl = br.getRegex("baseURL\":\\s+?\"(.*?)\"").getMatch(0);
            if (clipUrl == null || clipNetConnectionUrl == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ext = ".flv";
        } else {
            filename = this.br.getRegex("title: \\'([^<>\"]*?)\\',").getMatch(0);
            if (downloadLink.getDownloadURL().matches(type_2)) {
                /* type_2 */
                if (filename == null) {
                    /* Fallback to url-filename */
                    filename = new Regex(downloadLink.getDownloadURL(), "tvi24\\.iol\\.pt/[^/]+/([^/]+)").getMatch(0);
                }
            } else {
                /* type_3 */
                if (filename == null) {
                    /* Fallback to url-filename */
                    filename = new Regex(downloadLink.getDownloadURL(), "/programa/([^/]+/[^/]+)/").getMatch(0).replace("/", "_");
                }
            }
            ext = ".mp4";
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        filename += ext;
        downloadLink.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getDownloadURL().matches(type_1)) {
            /* rtmp download */
            final String swfUrl = "http://www.tvi.iol.pt/flashplayers/player-52.swf";
            final String dllink = clipNetConnectionUrl + "/" + clipUrl;

            if (dllink.startsWith("rtmp")) {
                dl = new RTMPDownload(this, downloadLink, dllink);
                final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

                rtmp.setPlayPath(clipUrl);
                rtmp.setSwfVfy(swfUrl);
                rtmp.setUrl(clipNetConnectionUrl);
                rtmp.setTimeOut(-1);
                rtmp.setResume(true);

                ((RTMPDownload) dl).startDownload();
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* hls download */
            String dllink = this.br.getRegex("videoUrl: \\'(http[^<>\"\\']*?)\\'").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(dllink);
            dllink = null;
            final String[] medias = this.br.getRegex("#EXT-X-STREAM-INF([^\r\n]+[\r\n]+[^\r\n]+)").getColumn(-1);
            if (medias == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            long bandwidth_highest = 0;
            for (final String media : medias) {
                // name = quality
                // final String quality = new Regex(media, "NAME=\"(.*?)\"").getMatch(0);
                final String bw = new Regex(media, "BANDWIDTH=(\\d+)").getMatch(0);
                final long bandwidth_temp = Long.parseLong(bw);
                if (bandwidth_temp > bandwidth_highest) {
                    bandwidth_highest = bandwidth_temp;
                    dllink = new Regex(media, "chunklist[^<>\"\r\n\t+]+[^\r\n]+").getMatch(-1);
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = this.br.getBaseURL() + dllink;
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        }
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
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}