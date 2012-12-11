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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ge.tt" }, urls = { "http://((www|open)\\.)?ge\\.tt/(api/)?\\d/files/[0-9a-zA-z]+/\\d+/blob(\\?download)?" }, flags = { 0 })
public class GeTt extends PluginForHost {

    private String              DLLINK               = null;
    private static final String LIMITREACHED         = "overloaded.html";
    private static final String LIMITREACHEDUSERTEXT = "Traffic limit for this file is reached";

    public GeTt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ge.tt/#terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        final Browser brc = br.cloneBrowser();
        if (downloadLink.getDownloadURL().matches("http://(www\\.)?api(\\d+)?\\.ge\\.tt/\\d/[A-Za-z0-9]+/.+")) {
            br.getPage("http://ge.tt");
            brc.getPage(downloadLink.getDownloadURL());
            if (brc.containsHTML("No htmlCode read") || br.containsHTML(">404 Not Found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            DLLINK = brc.getRedirectLocation();
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (DLLINK.contains(LIMITREACHED)) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.gett.trafficlimit", LIMITREACHEDUSERTEXT));
                return AvailableStatus.TRUE;
            }
        } else {
            DLLINK = downloadLink.getDownloadURL();
        }
        // In case the link redirects to the finallink
        brc.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setFinalFileName(getFileNameFromHeader(con));
            } else {
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // Limit is on the file, reconnect doesn't remove it
        if (DLLINK.contains(LIMITREACHED)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, LIMITREACHEDUSERTEXT, 30 * 60 * 1000l);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
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