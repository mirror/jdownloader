//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anitube.info" }, urls = { "http://(www\\.)?anitube\\.(co|tv|com\\.br|jp|se|info)/video/\\d+/[a-z0-9\\-]+" }, flags = { 0 })
public class AnitubeCo extends PluginForHost {

    // note: .co, .tv, .com.br, .jp, .se don't respond only .info -raztoki20160716
    // https://www.facebook.com/anitubebr/ or google "anitube"

    private String dllink = null;

    public AnitubeCo(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.anitube.se/terms";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("\\.(co|tv|com\\.br|jp|se|info)", ".info"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // provider blocks some subnets on http gateway, unknown what ranges.
        if (br.containsHTML(">403 Forbidden<") && br.containsHTML(">nginx/[\\d+\\.]+<")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "IP Blocked: Provider prevents access based on IP address.");
        }
        if (br.containsHTML("Unfortunately it's impossible to access the site from your current geographic location")) {
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.getURL().contains("error.php?type=video_missing")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?)\\- AniTube\\! Animes Online</title>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String[] matches = br.getRegex("((?:https?:)?//(?:www\\.)?anitube\\.[^ \"']+/config\\.php\\?(?:vid)?=[0-9a-f]+)").getColumn(0);
        if (matches == null || matches.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String match : matches) {
            final Browser br = this.br.cloneBrowser();
            br.getPage(match);
            final String[] qualities = { "720p HD", "360p SD", "480" };
            for (final String quality : qualities) {
                dllink = br.getRegex("file\\s*:\\s*\"(https?://[^<>\"]*?)\",\\s*label:\\s*\"" + quality + "\"").getMatch(0);
                if (dllink != null) {
                    break;
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    filename = filename.trim();
                    downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + getFileNameExtensionFromString(dllink, ".mp4"));
                    break;
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML("Unfortunately it's impossible to access the site from your current geographic location")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Your country is blocked!");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
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
    }

}