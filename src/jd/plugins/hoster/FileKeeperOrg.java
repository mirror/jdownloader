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
import java.net.URL;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filekeeper.org" }, urls = { "http://[\\w\\.]*?filekeeper\\.org/download/[0-9a-zA-Z]+/([\\(\\)0-9A-Za-z.-_% ]+|[/]+/[\\(\\)0-9A-Za-z.-_% ])" }, flags = { 0 })
public class FileKeeperOrg extends PluginForHost {

    public FileKeeperOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.it7.net/contact/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        URLConnectionAdapter con = br.openGetConnection(link.getDownloadURL());
        String check = con.getContentType();
        if (check.contains("application")) {
            link.setFinalFileName(Plugin.getFileNameFromHeader(con));
            link.setDownloadSize(con.getLongContentLength());
            con.disconnect();
            return AvailableStatus.TRUE;
        }
        br.followConnection();
        if (br.containsHTML("404 Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("/download/.*?\">(http://.*?)<").getMatch(0);
        String filesize = br.getRegex("</a>.*?dash;(.*?)<br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(Plugin.getFileNameFromURL(new URL(filename)));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace("&nbsp;", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
        if (!dl.getConnection().getContentType().contains("application")) {
            br.followConnection();
            jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
            if (!dl.getConnection().getContentType().contains("application")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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