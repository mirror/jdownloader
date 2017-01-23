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

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tn.com.ar" }, urls = { "https?://(?:www\\.)?tn\\.com\\.ar/programas/[a-z0-9\\-]+/[a-z0-9\\-]+_\\d+" })
public class TnComAr extends PluginForHost {

    public TnComAr(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: https
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
        if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("\"Invalid input\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "programas/([a-z0-9\\-]+/[a-z0-9\\-]+)_\\d+").getMatch(0).replace("/", "_");
        String filename = br.getRegex("name=\"twitter:title\" content=\"([^<>\"]+)\\- TN\\.com\\.ar\"").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }

        String partner_id = this.br.getRegex("data\\-pid=\"(\\d+)\"").getMatch(0);
        if (partner_id == null) {
            partner_id = "107";
        }
        String uiconf_id = this.br.getRegex("data\\-uiconfig=\"(\\d+)\"").getMatch(0);
        if (uiconf_id == null) {
            uiconf_id = "11601650";
        }
        String sp = this.br.getRegex("data\\-sp=\"(\\d+)\"").getMatch(0);
        if (sp == null) {
            sp = "10700";
        }
        final String entry_id = this.br.getRegex("data\\-entryid=\"([^<>\"]+)\"").getMatch(0);

        if (entry_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        this.br.getPage("https://vodgc.com/html5/html5lib/v2.49/mwEmbedFrame.php?&wid=_"
                + partner_id
                + "&uiconf_id="
                + uiconf_id
                + "&cache_st=0&entry_id="
                + entry_id
                + "&flashvars[thumbnailUrl]=http%3A%2F%2Fcdn.tn.com.ar%2Fsites%2Fdefault%2Ffiles%2Fstyles%2F650x365%2Fpublic%2F2017%2F01%2F17%2Fpc3.jpg&flashvars[autoplay]=false&flashvars[streamerType]=auto&flashvars[IframeCustomPluginCss1]=http%3A%2F%2Fcdn.tn.com.ar%2Fsites%2Fall%2Fthemes%2Fhudson%2Fdist%2Fkaltura.css&flashvars[durationLabel.prefix]=%7C%20&flashvars[volumeControl]=%7B%22layout%22%3A%22vertical%22%7D&flashvars[strings]=%7B%22mwe-embedplayer-play_clip%22%3A%22Reproducir%22%2C%22mwe-embedplayer-pause_clip%22%3A%22Pausa%22%2C%22mwe-embedplayer-player_fullscreen%22%3A%22Pantalla%20Completa%22%2C%22mwe-embedplayer-player_closefullscreen%22%3A%22Salir%20pantalla%20completa%22%2C%22mwe-embedplayer-volume-mute%22%3A%22Mutear%22%2C%22mwe-embedplayer-volume-unmute%22%3A%22Sonido%20On%22%2C%22mwe-embedplayer-next_clip%22%3A%22Siguiente%22%2C%22mwe-embedplayer-prev_clip%22%3A%22Anterior%22%2C%22mwe-embedplayer-replay%22%3A%22Volver%20a%20Ver%22%7D&flashvars[vast]=%7B%22numPreroll%22%3A1%2C%22prerollUrl%22%3A%22http%3A%2F%2Fads.e-planning.net%2Feb%2F4%2F14f7c%2Fnota_tn%2Fpreroll%3Fo%3Dv%26ma%3D1%26vv%3D3%22%2C%22numPostroll%22%3A1%2C%22postrollUrl%22%3A%22http%3A%2F%2Fads.e-planning.net%2Feb%2F4%2F14f7c%2Fnota_tn%2Fpostroll%3Fo%3Dv%26ma%3D1%26vv%3D3%22%7D&flashvars[autoPlay]=true&playerId=kaltura_player_588624dd37ad9&ServiceUrl=https%3A%2F%2Fvodgc.com&CdnUrl=https%3A%2F%2Fstreaming.vodgc.com&ServiceBase=%2Fapi_v3%2Findex.php%3Fservice%3D&UseManifestUrls=true&forceMobileHTML5=true&urid=2.49&callback=mwi_kalturaplayer588624dd37ad90");
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
            final Object size = entries.get("size");
            if (flavourid == null) {
                continue;
            }
            max_bitrate_temp = JavaScriptEngineFactory.toLong(entries.get("bitrate"), 0);
            if (max_bitrate_temp > max_bitrate) {
                dllink = "https://vodgc.com/p/" + partner_id + "/sp/" + sp + "/playManifest/entryId/" + entry_id + "/flavorId/" + flavourid + "/format/url/protocol/https/a.mp4";
                max_bitrate = max_bitrate_temp;
                if (size != null && size instanceof Long) {
                    filesize = (long) size;
                }
                filesize = JavaScriptEngineFactory.toLong(entries.get("size"), 0);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext;
        if (dllink != null) {
            ext = getFileNameExtensionFromString(dllink, default_extension);
        } else {
            ext = default_extension;
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        if (filesize > 0) {
            filesize = filesize * 1024;
        } else if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    filesize = con.getLongContentLength();
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
        link.setFinalFileName(filename);
        if (filesize > 0) {
            link.setDownloadSize(filesize);
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
