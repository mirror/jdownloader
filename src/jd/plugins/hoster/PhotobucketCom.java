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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "photobucket.com" }, urls = { "https?://(?:www\\.)?(media\\.photobucket\\.com/.+|gs\\d+\\.photobucket\\.com/groups/[A-Za-z0-9]+/[A-Za-z0-9]+/\\?action=view\\&current=[^<>\"/]+|(?:[A-Za-z0-9]+\\.)?photobucket\\.com/.*?/media/[^/]+(?:\\.html)?)" })
public class PhotobucketCom extends PluginForHost {
    public PhotobucketCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other:
    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://photobucket.com/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        dllink = null;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.looksLikeDownloadableContent(con)) {
                dllink = link.getPluginPatternMatcher();
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                if (con.isContentDisposition()) {
                    link.setFinalFileName(getFileNameFromHeader(con));
                }
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
        dllink = PluginJSonUtils.getJsonValue(br, "originalUrl");
        if (dllink == null) {
            dllink = PluginJSonUtils.getJsonValue(br, "fullsizeUrl");
        }
        if (StringUtils.isEmpty(dllink)) {
            if (!this.canHandle(this.br.getURL())) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = Encoding.htmlDecode(dllink);
        try {
            con = br.openHeadConnection(dllink);
            if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!con.getContentType().contains("html")) {
                String server_filename = getFileNameFromHeader(con);
                server_filename = server_filename.replace("~original", "");
                link.setDownloadSize(con.getLongContentLength());
                link.setFinalFileName(server_filename);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setProperty("directlink", dllink);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
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
