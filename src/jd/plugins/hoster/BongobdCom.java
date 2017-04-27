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
import jd.plugins.components.SiteType.SiteTemplate;

import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bongobd.com" }, urls = { "https?://(?:www\\.)?bongobd\\.com/(?:en|bn)/watch\\?v=[A-Za-z0-9]+" })
public class BongobdCom extends PluginForHost {

    public BongobdCom(PluginWrapper wrapper) {
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

    private static final boolean prefer_hls        = true;

    private String               dllink            = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://tn.com.ar/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.containsHTML("var videoId\\s*?=\\s*?\"\";")) {
            /* 2017-04-27: Serverside broken / offline video */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "v=([A-Za-z0-9]+)$").getMatch(0);
        String filename = br.getRegex("id=\"current\\-video\\-title\">([^<>\"]+)<").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }

        String partner_id = this.br.getRegex("/p/(\\d+)").getMatch(0);
        if (partner_id == null) {
            partner_id = "1868701";
        }
        // String uiconf_id = this.br.getRegex("data\\-uiconfig=\"(\\d+)\"").getMatch(0);
        // if (uiconf_id == null) {
        // uiconf_id = "0000000";
        // }
        String sp = this.br.getRegex("/sp/(\\d+)").getMatch(0);
        if (sp == null) {
            sp = partner_id;
        }
        final String entry_id = this.br.getRegex("var\\s*?videoId\\s*?=\\s*?\"([^<>\"\\']+)\";").getMatch(0);

        if (entry_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        long filesize_max = 0;
        final String flavorids_text = this.br.getRegex("flavorIds/([A-Za-z0-9\\-_,]+)").getMatch(0);
        if (flavorids_text != null) {
            if (prefer_hls) {
                dllink = String.format("http://cdnbakmi.kaltura.com/p/%s/sp/%s/playManifest/entryId/%s/flavorIds/%s/forceproxy/true/format/applehttp/protocol/https/a.m3u8", partner_id, sp, entry_id, flavorids_text);
                this.br.getPage(dllink);
                final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                if (hlsbest != null) {
                    checkFFProbe(link, "Download a HLS Stream");
                    dllink = hlsbest.getDownloadurl();
                    final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
                    final StreamInfo streamInfo = downloader.getProbe();
                    if (streamInfo == null) {
                        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        server_issues = true;
                    } else {
                        filesize_max = downloader.getEstimatedSize();
                    }
                }
            } else {
                final String[] flavorids = flavorids_text.split(",");
                URLConnectionAdapter con = null;
                String dllink_temp;
                for (final String flavorid : flavorids) {
                    dllink_temp = String.format("http://cdnbakmi.kaltura.com/p/%s/sp/%s/playManifest/entryId/%s/flavorId/%s/format/url/protocol/https/a.mp4", partner_id, sp, entry_id, flavorid);
                    try {
                        con = br.openHeadConnection(dllink_temp);
                        if (!con.getContentType().contains("html")) {
                            if (con.getLongContentLength() > filesize_max) {
                                filesize_max = con.getLongContentLength();
                                dllink = dllink_temp;
                            }
                            server_issues = false;
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
            }
        }

        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext;
        if (dllink != null && !dllink.contains("m3u8")) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        if (filesize_max > 0) {
            link.setDownloadSize(filesize_max);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
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
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KalturaVideoPlatform;
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
