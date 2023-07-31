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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "girlfriendvideos.com" }, urls = { "https?://(?:www\\.)?girlfriendvideos\\.com/members/[a-z]/[a-z0-9\\-_]+/(\\d+)\\.php" })
public class GirlfriendvideosCom extends PluginForHost {
    public GirlfriendvideosCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://girlfriendvideos.com/help.php";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("This video has been removed")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains("/members/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // String filename = br.getRegex("<title>Girlfriend Videos \\- ([^<>\"]*?)</title>").getMatch(0); -> Free User-Submitted ...
        String filename = br.getRegex("\"name\"\\s*:\\s*\"([^\"]+)\"").getMatch(0);
        if (dllink == null) {
            if (br.containsHTML("file=[a-z]/[a-z0-9\\-_]+/\\d+\\.flv\"")) {
                dllink = "http://" + this.getHost() + "/videos/" + new Regex(link.getDownloadURL(), "members/([a-z]/[a-z0-9\\-_]+/\\d+)").getMatch(0) + ".flv";
            } else {
                dllink = br.getRegex("\"(/videos/[a-z]/[a-z0-9\\-_]+/\\d+\\.mp4)").getMatch(0);
                if (dllink != null) {
                    dllink = "http://" + this.getHost() + dllink;
                }
            }
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename);
            filename = filename.trim();
            final String ext = getFileNameExtensionFromString(dllink, ".mp4");
            if (!filename.endsWith(ext)) {
                filename += ext;
            }
            link.setFinalFileName(filename);
        }
        /* 2021-08-26: Server will return wrong information when checking a lot of URLs at the same time --> Disabled filesize check. */
        final boolean allowFilesizeCheck = false;
        if (dllink != null && !isDownload && allowFilesizeCheck) {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
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
