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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "perfectgirls.net" }, urls = { "http://([a-z]+\\.)?perfectgirlsdecrypted\\.net/gal/\\d+/.{0,1}" })
public class PerfectGirlsNet extends PluginForHost {
    public PerfectGirlsNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.perfectgirls.net/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("perfectgirlsdecrypted.net/", "perfectgirls.net/"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("No htmlCode read|src=\"http://(www\\.)?dachix\\.com/flashplayer/flvplayer\\.swf\"|\"http://(www\\.)?deviantclip\\.com/flashplayer/flvplayer\\.swf\"|thumbs/misc/not_available\\.gif")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?) ::: PERFECT GIRLS</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("").getMatch(0);
        }
        String[] reses = { "720", "480", "360" };
        for (String res : reses) {
            dllink = br.getRegex("<source src=\"([^<>\"]+)\" res=\"" + res + "\"").getMatch(0);
            if (dllink != null) {
                checkDllink(downloadLink, dllink);
            }
            if (dllink != null) {
                break;
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("<source src=\"([^<>\"]+)\"").getMatch(0);
        }
        if (filename == null || dllink == null) {
            logger.info("filename: " + filename + ", dllink: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        final String ext = getFileNameExtensionFromString(dllink, ".mp4");
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        return AvailableStatus.TRUE;
    }

    private String checkDllink(final DownloadLink link, final String flink) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(flink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                dllink = flink;
            } else {
                dllink = null;
            }
        } catch (final Throwable e) {
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
