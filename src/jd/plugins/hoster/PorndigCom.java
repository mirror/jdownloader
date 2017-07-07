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
import java.util.HashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porndig.com" }, urls = { "https?://(?:www\\.)?porndig\\.com/videos/\\d+/[a-z0-9\\-]+\\.html" })
public class PorndigCom extends PluginForHost {

    public PorndigCom(PluginWrapper wrapper) {
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
        return "https://www.porndig.com/tos";
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
        }
        final String url_filename = new Regex(link.getDownloadURL(), "videos/\\d+/([a-z0-9\\-]+)").getMatch(0);
        String filename = br.getRegex("<title>([^<>\"]+) \\- PornDig\\.com</title>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext;
        if (!StringUtils.isEmpty(dllink)) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        final String url_embed = this.br.getRegex("<iframe[^<>]*?src=\"(http[^<>\"]+)\"").getMatch(0);
        if (url_embed == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(url_embed);
        final String jssource = br.getRegex("\"sources\"[\t\n\r ]*?:[\t\n\r ]*?(\\[.*?\\])").getMatch(0);
        if (jssource != null) {
            try {
                HashMap<String, Object> entries = null;
                Object quality_temp_o = null;
                long quality_temp = 0;
                long quality_best = 0;
                String dllink_temp = null;
                final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(jssource);
                for (final Object videoo : ressourcelist) {
                    entries = (HashMap<String, Object>) videoo;
                    dllink_temp = (String) entries.get("file");
                    quality_temp_o = entries.get("label");
                    if (quality_temp_o != null && quality_temp_o instanceof Long) {
                        quality_temp = JavaScriptEngineFactory.toLong(quality_temp_o, 0);
                    } else if (quality_temp_o != null && quality_temp_o instanceof String) {
                        /* E.g. '360p' */
                        quality_temp = Long.parseLong(new Regex((String) quality_temp_o, "(\\d+)p").getMatch(0));
                    }
                    if (StringUtils.isEmpty(dllink_temp) || quality_temp == 0) {
                        continue;
                    }
                    if (quality_temp > quality_best) {
                        quality_best = quality_temp;
                        dllink = dllink_temp;
                    }
                }
                if (!StringUtils.isEmpty(dllink)) {
                    logger.info("BEST handling for multiple video source succeeded");
                }
            } catch (final Throwable e) {
                logger.info("BEST handling for multiple video source failed");
            }
        } else {
            // <video> <source
            int best = 0;
            String hls = null;
            final String[] sources = br.getRegex("<source[^>]+>").getColumn(-1);
            if (sources != null && sources.length > 0) {
                for (final String source : sources) {
                    final String url = new Regex(source, "src\\s*=\\s*('|\")(.*?)\\1").getMatch(1);
                    if (url.contains(".m3u8")) {
                        // typically this is single entry.
                        hls = url;
                        continue;
                    }
                    final String label = new Regex(source, "label\\s*=\\s*('|\")(\\d+)p?\\1").getMatch(1);
                    if (label == null || url == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final int p = Integer.parseInt(label);
                    if (best < p) {
                        dllink = url;
                        best = p;
                    }
                }
                // prefer non hls over hls, as hls core can't chunk at this stage.
                if (dllink == null && hls != null) {
                    // hls has multiple qualities....
                    final Browser br2 = br.cloneBrowser();
                    br2.getPage(hls);
                    final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br2));
                    if (hlsbest == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    dllink = hlsbest.getDownloadurl();
                }
            }

        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains("m3u8") || dllink.contains("//ahhls.") || dllink.contains("media=hls")) {
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, dllink);
            dl.startDownload();
        } else {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
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
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
