//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videos.sapo.pt" }, urls = { "http://(\\w+\\.)?videos\\.sapo\\.(pt|cv|ao|mz|tl)/\\w{20}" }, flags = { 0 })
public class VideosSapo extends PluginForHost {

    public VideosSapo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://seguranca.sapo.pt/termosdeutilizacao/videos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL().concat("/rss"));
        String filename = br.getRegex("<item>.*?<title><!\\[CDATA\\[(.*?)\\]\\]></title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(filename.trim() + ".mp4");
        try {
            if (!br.openGetConnection(downloadLink.getDownloadURL().concat("/mov/1?player=INTERNO&time=").concat(Long.toString(System.currentTimeMillis() / 1000))).getContentType().contains("html")) {
                downloadLink.setDownloadSize(br.getHttpConnection().getLongContentLength());
                return AvailableStatus.TRUE;
            }
        } finally {
            try {
                if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
            } catch (Throwable e) {
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL().concat("/mov/1?player=INTERNO&time=".concat(Long.toString(System.currentTimeMillis() / 1000))), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}