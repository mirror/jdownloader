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
import jd.controlling.AccountController;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "free-way.meContainer" }, urls = { "http://free\\-way\\.me/decryptedcontainerlink/[a-z0-9]+" }, flags = { 0 })
public class FreeWayMeContainer extends PluginForHost {

    public FreeWayMeContainer(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "https://www.free-way.me/impressum";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("free-way.me");
        Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa == null) AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            downloadLink.getLinkStatus().setStatusText("Kann Links nur mit aktiviertem Premium Account prüfen und laden!");
            return AvailableStatus.TRUE;
        }
        // Account found? Add it so it can be used within this plugin
        AccountController.getInstance().addAccount(this, aa);
        DLLINK = "https://www.free-way.me/load.php?containerid=" + new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0) + "&alias=" + downloadLink.getStringProperty("contname") + "&pass=" + JDHash.getSHA1(JDHash.getMD5(downloadLink.getStringProperty("password"))) + "&user=" + Encoding.urlEncode(aa.getUser()) + "&pw=" + Encoding.urlEncode(aa.getPass());
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            } else {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
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
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Kann Links nur mit aktiviertem Premium Account prüfen und laden!");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, false, 1);
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
