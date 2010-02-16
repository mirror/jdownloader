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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fucktube.com" }, urls = { "http://[\\w\\.]*?fucktube\\.com/video/[0-9]+/.{1}" }, flags = { 0 })
public class FckTubeCom extends PluginForHost {

    public FckTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.fucktube.com/legal/2257";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://www.fucktube.com", "checked_yes", "1");
        br.getPage(link.getDownloadURL());
        String refreshed = br.getRegex("content=\"0;url=(.*?)\"").getMatch(0);
        if (refreshed != null) {
            link.setUrlDownload(refreshed);
            br.getPage(refreshed);
        }
        if (br.containsHTML("(font-size: 24px\">404</font>|This page cannot be displayed at the current time|If you continue to receive this error)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div id=\"video_header\">.*?<h1>(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<div style=\"height:40px\">.*?<h2>(.*?)</h2>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("</div>.*?<img alt=\"(.*?)\"").getMatch(0);
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String dllink = br.getRegex("embedVideo\\(.*?,\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("('|\")(http://media[0-9]+\\.fucktube\\.com/.*?\\.flv)('|\")").getMatch(1);
        if (dllink != null) {
            Browser br2 = br.cloneBrowser();
            URLConnectionAdapter con = br2.openGetConnection(dllink);
            link.setDownloadSize(con.getLongContentLength());
        }
        filename = filename + ".flv";
        link.setFinalFileName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("embedVideo\\(.*?,\"(http.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("('|\")(http://media[0-9]+\\.fucktube\\.com/.*?\\.flv)('|\")").getMatch(1);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}