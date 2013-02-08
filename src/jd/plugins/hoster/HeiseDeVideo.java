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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "heise.de" }, urls = { "http://(www\\.)?heise\\.de/video/artikel/[A-Za-z0-9\\-_]+\\d+\\.html" }, flags = { 0 })
public class HeiseDeVideo extends PluginForHost {

    public HeiseDeVideo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.heise.de/Kontakt-4864.html";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(">Fehlermeldung<|>404 \\- File not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<meta name=\"fulltitle\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("<meta name=\"DC\\.title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim()).replace(":", " - ");
        String ext = null;
        DLLINK = br.getRegex("\"(http://(www\\.)?heise\\.de/videout/info[^<>\"]*?)\"").getMatch(0);
        if (DLLINK != null) {
            br.getPage(DLLINK);
            // ios formats also exist
            final String[] formats = { "mp4", "webm" };
            final String qualities[] = { "720", "360", "270", "180" };
            boolean stop = false;
            for (final String format : formats) {
                String tempLinks = br.getRegex("(\"" + format + "\":\\{\".*?\"\\}\\})").getMatch(0);
                if (tempLinks != null) {
                    for (final String quality : qualities) {
                        DLLINK = br.getRegex("\"" + quality + "\":\\{\"url\":\"(http://[^<>\"]*?)\"").getMatch(0);
                        if (DLLINK != null && linkOk(downloadLink)) {
                            filename += "_" + quality + "p";
                            ext = "." + format;
                            stop = true;
                            break;
                        }
                    }
                }
                if (stop) break;
            }
        }
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(filename + ext);
        DLLINK = Encoding.htmlDecode(DLLINK);
        return AvailableStatus.TRUE;

    }

    private boolean linkOk(final DownloadLink downloadLink) throws IOException {
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (con.getResponseCode() == 403) return false;
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                return true;
            } else {
                return false;
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
