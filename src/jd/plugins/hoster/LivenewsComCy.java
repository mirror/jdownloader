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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "livenews.com.cy" }, urls = { "https?://(?:www\\.)?media\\.livenews\\.com\\.cy/DesktopModules/IST\\.MediaServices/LiveMediaPlayer\\.aspx\\?itemid=\\d+" })
public class LivenewsComCy extends PluginForHost {

    public LivenewsComCy(PluginWrapper wrapper) {
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
    private String               dllink_hls        = null;
    private boolean              server_issues     = false;

    @Override
    public String getAGBLink() {
        return "http://media.livenews.com.cy/Home/tabid/184/ctl/Terms/language/en-US/Default.aspx";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final String playlist_json = this.br.getRegex("playlist\\s*?:\\s*?(\\[.*?\\]),").getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (playlist_json == null) {
            /* No playlist --> No player --> Nothing to download */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "itemid=(\\d+)").getMatch(0);

        HashMap<String, Object> entries = null;
        // final Object classo = JavaScriptEngineFactory.jsonToJavaMap(playlist_json);
        ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(playlist_json);
        entries = (HashMap<String, Object>) ressourcelist.get(ressourcelist.size() - 1);
        ressourcelist = (ArrayList<Object>) entries.get("sources");

        String url_temp = null;
        for (final Object videoo : ressourcelist) {
            entries = (HashMap<String, Object>) videoo;
            final Object videourlo = entries.get("file");
            if (videourlo != null && videourlo instanceof String) {
                url_temp = (String) videourlo;
                if (url_temp.contains(".m3u8")) {
                    dllink_hls = url_temp;
                } else if (url_temp.contains(".mp4")) {
                    dllink = url_temp;
                }
            }
        }

        String filename = dllink != null ? new Regex(dllink, "([^/]+)\\.(?:mp4|m3u8)").getMatch(0) : null;
        if (filename == null) {
            filename = url_filename;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = default_extension;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setFinalFileName(filename);
        // if (dllink != null) {
        // dllink = Encoding.htmlDecode(dllink);
        // URLConnectionAdapter con = null;
        // try {
        // con = br.openHeadConnection(dllink);
        // if (!con.getContentType().contains("html")) {
        // link.setDownloadSize(con.getLongContentLength());
        // link.setProperty("directlink", dllink);
        // } else {
        // server_issues = true;
        // }
        // } finally {
        // try {
        // con.disconnect();
        // } catch (final Throwable e) {
        // }
        // }
        // }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null && dllink_hls == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (this.dllink_hls != null) {
            this.br.getPage(this.dllink_hls);
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            this.dllink_hls = hlsbest.downloadurl;
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, this.dllink_hls);
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
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
