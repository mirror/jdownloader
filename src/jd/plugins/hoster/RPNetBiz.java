//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rpnet.biz" }, urls = { "http://(www\\.)?dl[^\\.]*.rpnet\\.biz/download/.*/([^/\\s]+)?" }, flags = { 0 })
public class RPNetBiz extends PluginForHost {

    public RPNetBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("http://www.", "http://"));
    }

    @Override
    public String getAGBLink() {
        return "https://premium.rpnet.biz/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // tested with 20 seems fine.
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        // requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), true, 0);
        URLConnectionAdapter con = dl.getConnection();
        List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
        if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) {
            try {
                br.followConnection();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getDownloadURL());
            List<Integer> allowedResponseCodes = Arrays.asList(200, 206);
            if (!allowedResponseCodes.contains(con.getResponseCode()) || con.getContentType().contains("html") || con.getResponseMessage().contains("Download doesn't exist for given Hash/ID/Key")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            link.setName(getFileNameFromHeader(con));
            link.setDownloadSize(con.getLongContentLength());
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}