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

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.downloader.hls.M3U8Playlist;
import org.jdownloader.plugins.components.config.MediathekProperties;
import org.jdownloader.plugins.components.config.WatchboxDeConfigInterface;
import org.jdownloader.plugins.config.PluginConfigInterface;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MediathekHelper;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "watchbox.de" }, urls = { "watchbox.dedecrypted://.+" })
public class WatchboxDe extends PluginForHost {
    private String  dllink        = null;
    private boolean server_issues = false;

    /** Tags: ex website: clipfish.de */
    public WatchboxDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(final DownloadLink link) {
        final String real_domain = new Regex(link.getDownloadURL(), "^(.+)decrypted://").getMatch(0);
        if (real_domain != null) {
            link.setUrlDownload(link.getDownloadURL().replace(real_domain + "decrypted://", "https://"));
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.watchbox.de/";
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        server_issues = false;
        prepBR(this.br);
        dllink = link.getDownloadURL();
        checkFFProbe(link, "Download a HLS Stream");
        final MediathekProperties data = link.bindData(MediathekProperties.class);
        final String filename = MediathekHelper.getMediathekFilename(link, data, false, true);
        final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
        final long hlsBandwidth = data.getBandwidth();
        if (hlsBandwidth > 0) {
            for (M3U8Playlist playList : downloader.getPlayLists()) {
                playList.setAverageBandwidth(hlsBandwidth);
            }
        }
        final long estimatedSize = downloader.getEstimatedSize();
        if (estimatedSize > 0) {
            link.setDownloadSize(estimatedSize);
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        checkFFmpeg(downloadLink, "Download a HLS Stream");
        dl = new HLSDownloader(downloadLink, br, dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public String getDescription() {
        return "Lade Videos von watchbox.de herunter";
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return WatchboxDeConfigInterface.class;
    }
}