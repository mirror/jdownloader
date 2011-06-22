//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datagrad.ru" }, urls = { "http://[a-zA-Z-0-9.]{0,}datagrad.ru/download/[0-9]{1,}" }, flags = { 0 })
public class DatagradRU extends PluginForHost {

    public DatagradRU(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.datagrad.ru/auth/hosting.php";
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.setFollowRedirects(true);
        URLConnectionAdapter con = br.openGetConnection(downloadLink.getDownloadURL());
        if (con.isContentDisposition()) {
            downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(con));
            downloadLink.setDownloadSize(con.getLongContentLength());
            con.disconnect();
            return AvailableStatus.TRUE;
        } else {
            return AvailableStatus.FALSE;
        }
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        // requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("the dllink doesn't seem to be a file, following the connection...");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
