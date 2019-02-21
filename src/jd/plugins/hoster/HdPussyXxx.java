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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hdpussy.xxx" }, urls = { "https?://(?:www\\.)?hdpussy\\.xxx/video/([a-f0-9]{32})/" })
public class HdPussyXxx extends PluginForHost {
    public HdPussyXxx(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://hdpussy.xxx/";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(downloadLink.getPluginPatternMatcher());
            dllink = downloadLink.getPluginPatternMatcher();
            if (!con.getContentType().contains("html")) {
                /* 2019-02-21: Directurl */
                downloadLink.setDownloadSize(con.getLongContentLength());
                downloadLink.setFinalFileName(getLinkID(downloadLink) + ".mp4");
                return AvailableStatus.TRUE;
            } else {
                br.followConnection();
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">This page not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<div class=\"title\\-box\">([^<>\"]*?)<b>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)\\| HD Pussy XXX</title>").getMatch(0);
        }
        if (filename == null) {
            /* Fallback to URL-filename */
            filename = new Regex(downloadLink.getDownloadURL(), "([a-f0-9]{32})/$").getMatch(0);
        }
        dllink = br.getRegex("file[\t\n\r ]*?:[\t\n\r ]*?\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String ext = null;
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            if (!dllink.startsWith("http")) {
                /* E.g. missing videosource, player will show error 'No playable sources found' --> Offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            ext = getFileNameExtensionFromString(dllink, ".flv");
        }
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        filename = Encoding.htmlDecode(filename).trim();
        downloadLink.setFinalFileName(filename + ext);
        /* Do NOT check for filesize as their directurls often time out which would make this process really really slow! */
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            final long responsecode = dl.getConnection().getResponseCode();
            if (responsecode == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
            } else if (responsecode == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
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
