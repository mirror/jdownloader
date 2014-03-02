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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "slutload.com" }, urls = { "http://(www\\.)?slutload\\.com/(video/[A-Za-z0-9\\-_]+/[A-Za-z0-9]+|(embed_player|watch)/[A-Za-z0-9]+)" }, flags = { 0 })
public class SlutLoadCom extends PluginForHost {

    public SlutLoadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_EMBED = "http://(www\\.)?slutload\\.com/embed_player/[A-Za-z0-9]+";
    private static final String TYPE_OLD   = "http://(www\\.)?slutload\\.com/watch/[A-Za-z0-9]+";

    @Override
    public String getAGBLink() {
        return "http://www.slutload.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(TYPE_EMBED) || link.getDownloadURL().matches(TYPE_OLD)) {
            link.setUrlDownload("http://www.slutload.com/video/" + System.currentTimeMillis() + "/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0));
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.getResponseCode() == 410) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        br.followConnection();
        if (br.getURL().equals("http://www.slutload.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (!br.getURL().contains("slutload.com/")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String filename = br.getRegex("class=\"videoHD\"><b>([^<>\"]*?)</b>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<p><b>Title:</b>([^<>\"]*?)</p>").getMatch(0);
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("data-url=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://v\\-ec\\.slutload\\-media\\.com/.*?\\.flv\\?.*?)\"").getMatch(0);
        }
        if (dllink == null) {
            /**
             * Some videos are officially not available but still work when embedded in other sites, lets try to download those too
             */
            br.getPage("http://emb.slutload.com/xplayerconfig/" + new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0) + ".css");

            dllink = br.getRegex("\\&ec_seek=;URL: (http://[^<>\"]+);type:").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}