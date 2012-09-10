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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gametrailers.com" }, urls = { "http://[0-9a-z\\.]+\\.mtvnservices\\.com/.*?\\?__gda__=\\d+_[0-9a-f]+" }, flags = { 0 })
public class GameTrailersCom extends PluginForHost {

    public GameTrailersCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.gametrailers.com/about/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        String dllink = downloadLink.getDownloadURL();
        Browser br2 = br.cloneBrowser();
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                br.getPage("http://www.gametrailers.com/feeds/mediagen/?uri=" + Encoding.urlEncode(downloadLink.getStringProperty("CONTENTID", null)) + "&forceProgressive=true");
                dllink = br.getRegex("<src>(http://.*?)</src>").getMatch(0);
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setUrlDownload(dllink);
                try {
                    br2.openGetConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        downloadLink.setDownloadSize(con.getLongContentLength());
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        long gt = (Long) downloadLink.getProperty("GRABBEDTIME", -1l);
        if (gt > 0) {
            /* Expiration date has not been verified */
            if (System.currentTimeMillis() - gt >= 24 * 60 * 60 * 1000) requestFileInformation(downloadLink);
        }
        String linkurl = downloadLink.getDownloadURL();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, linkurl, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink seems not to be a file!");
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