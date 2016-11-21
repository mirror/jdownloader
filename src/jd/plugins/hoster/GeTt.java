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
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ge.tt" }, urls = { "http://(?:www\\.)?((?:open|api|proxy)\\.)?ge\\.tt/(api/)?\\d/files/[0-9a-zA-z]+/\\d+/blob(\\?download)?" })
public class GeTt extends PluginForHost {

    private String              dllink               = null;
    private boolean             server_issues        = false;
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
        dllink = null;
        server_issues = false;
        br = new Browser();
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().matches("https?://(?:www\\.)?(open|api|proxy)(\\d+)?\\.ge\\.tt/\\d/[A-Za-z0-9]+/.+")) {
            br.getPage("http://ge.tt");
            if (downloadLink.getContentUrl() != null) {
                br.getPage(downloadLink.getContentUrl());
            }
            final Browser brc = br.cloneBrowser();
            brc.getPage(downloadLink.getDownloadURL());
            if (brc.containsHTML("No htmlCode read") || br.containsHTML(">404 Not Found<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = brc.getRedirectLocation();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.contains(LIMITREACHED)) {
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.gett.trafficlimit", LIMITREACHEDUSERTEXT));
                return AvailableStatus.TRUE;
            }
        } else {
            dllink = downloadLink.getDownloadURL();
            // correction for people who have added old links
            if (dllink.contains("//open")) {
                dllink = dllink.replace("//open", "//api") + "?download";
            }
        }
        // In case the link redirects to the finallink
        final Browser brc = br.cloneBrowser();
        brc.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = brc.openGetConnection(dllink);
            if (!con.getContentType().contains("html") && con.isOK()) {
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setFinalFileName(getFileNameFromHeader(con));
                // contains redirects
                dllink = brc.getURL();
            } else {
                server_issues = true;
            }
            return AvailableStatus.TRUE;
        } catch (final BrowserException b) {
            // typically there is a redirect link, and this will contain the filename.. but since request failed... it still as redirect
            final String filename = extractFileNameFromURL(brc.getRedirectLocation());
            if (filename != null) {
                downloadLink.setName(filename);
            }
            final String cause = b.getCause().toString();
            if (StringUtils.contains(cause, "java.net.UnknownHostException")) {
                // dns issue on the redirect (very common!)
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "DNS Issue: Host file server could be offline!");
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Connectivity Issue");
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (dllink.contains(LIMITREACHED)) {
            // Limit is on the file, reconnect doesn't remove it
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, LIMITREACHEDUSERTEXT, 30 * 60 * 1000l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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