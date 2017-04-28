//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "funk.net" }, urls = { "https?://(?:www\\.)?funk\\.net/([^/]+)/[a-f0-9]{24}/items/[a-f0-9]{24}" })
public class FunkNet extends PluginForHost {

    public FunkNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:

    /* Extension which will be used if no correct extension is found */
    private static final String  default_extension = ".mp4";
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;

    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "https://www.funk.net/funk";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String geturl = new Regex(link.getDownloadURL(), "(.+)/items/[a-f0-9]+$").getMatch(0);
        br.getPage(geturl);
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("error\\-container")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "serien/(.+)").getMatch(0);
        String filename = br.getRegex("<title>([^<>\"]+) \\| [^<>]+</title>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }

        final String player_embed_url = this.br.getRegex("(\\.kaltura\\.com/p/\\d+/sp/\\d+/embedIframeJs/[^<>\"]+)\"").getMatch(0);
        if (player_embed_url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String partner_id = new Regex(player_embed_url, "/partner_id/(\\d+)").getMatch(0);
        if (partner_id == null) {
            partner_id = "_1985051";
        }
        String uiconf_id = this.br.getRegex("uiconf_id/(\\d+)").getMatch(0);
        if (uiconf_id == null) {
            uiconf_id = "35472181";
        }
        String sp = new Regex(player_embed_url, "/sp/(\\d+)/").getMatch(0);
        if (sp == null) {
            sp = "198505100";
        }
        String entry_id = new Regex(player_embed_url, "/entry_id/([^/]+)").getMatch(0);
        if (entry_id == null) {
            entry_id = new Regex(player_embed_url, "entry_id=([^\\&=]+)").getMatch(0);
        }
        if (entry_id == null) {
            entry_id = this.br.getRegex("renderPlayer\\(\\'media[a-z0-9]+\\', \\'([^<>\"\\']+)\\'\\)").getMatch(0);
        }

        if (entry_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        this.br.getPage("https://cdnapisec.kaltura.com/html5/html5lib/v2.51/mwEmbedFrame.php?&wid=_" + partner_id + "&uiconf_id=" + uiconf_id + "&entry_id=" + entry_id + "&flashvars[autoPlay]=false&flashvars[EmbedPlayer.OverlayControls]=true&flashvars[EmbedPlayer.EnableNativeChromeFullscreen]=true&flashvars[EmbedPlayer.EnableIpadNativeFullscreen]=true&flashvars[LeadWithHLSOnJs]=true&flashvars[hlsjs.plugin]=true&flashvars[IframeCustomPluginCss1]=%2Fcss%2Fstyle.css&flashvars[localizationCode]=de&flashvars[strings]=%7B%22de%22%3A%7B%22mwe-embedplayer-volume-mute%22%3A%22Ton%20aus%22%2C%22mwe-embedplayer-volume-unmute%22%3A%22Ton%20an%22%2C%22mwe-timedtext-no-subtitles%22%3A%22Kein%20Untertitel%22%7D%7D&playerId=media58f78ed8e4b0c62b74eba239&forceMobileHTML5=true&urid=2.51&protocol=https&callback=mwi_media58f78ed8e4b0c62b74eba2390");
        String js = this.br.getRegex("kalturaIframePackageData = (\\{.*?\\}\\}\\});").getMatch(0);
        if (js == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        js = js.replace("\\\"", "\"");
        js = new Regex(js, "\"flavorAssets\":(\\[.*?\\])").getMatch(0);
        if (js == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(js);
        long filesize = 0;
        long max_bitrate = 0;
        long max_bitrate_temp = 0;
        LinkedHashMap<String, Object> entries = null;
        for (final Object videoo : ressourcelist) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String flavourid = (String) entries.get("id");
            if (flavourid == null) {
                continue;
            }
            max_bitrate_temp = JavaScriptEngineFactory.toLong(entries.get("bitrate"), 0);
            if (max_bitrate_temp > max_bitrate) {
                // dllink = "https://cdnapisec.kaltura.com/p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id +
                // "/flavorId/" + flavourid + "/format/url/protocol/https/a.mp4";
                dllink = "https://cdnapisec.kaltura.com/p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id + "/flavorId/" + flavourid + "/format/applehttp/protocol/https/a.m3u8?referrer=aHR0cHM6Ly93d3cuZnVuay5uZXQ&responseFormat=jsonp&callback=";
                max_bitrate = max_bitrate_temp;
                filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(dllink);
        dllink = PluginJSonUtils.getJson(this.br, "url");
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (!StringUtils.isEmpty(dllink)) {
            link.setFinalFileName(filename);
            if (dllink.contains(".m3u8")) {
                checkFFProbe(link, "Download a HLS Stream");
                br.getPage(dllink);
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                dllink = hlsbest.getDownloadurl();
                final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
                final StreamInfo streamInfo = downloader.getProbe();
                if (streamInfo == null) {
                    // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    server_issues = true;
                } else {
                    final long estimatedSize = downloader.getEstimatedSize();
                    if (estimatedSize > 0) {
                        link.setDownloadSize(estimatedSize);
                    }
                }
            } else {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        link.setDownloadSize(con.getLongContentLength());
                        link.setProperty("directlink", dllink);
                    } else {
                        server_issues = true;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } else {
            /* We cannot be sure whether we have the correct extension or not! */
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, this.dllink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
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

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KalturaVideoPlatform;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
