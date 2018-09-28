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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "vine.co" }, urls = { "https?://(www\\.)?vine\\.co/v/[A-Za-z0-9]+" })
public class VineCo extends PluginForHost {
    public VineCo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public String getAGBLink() {
        return "https://vine.co/terms";
    }

    @Override
    public String getMirrorID(DownloadLink link) {
        if (link != null && StringUtils.equals(getHost(), link.getHost())) {
            final String ret = link.getDownloadURL().substring(link.getDownloadURL().lastIndexOf("/") + 1);
            if (ret != null) {
                return getHost() + "://" + ret;
            } else {
                return null;
            }
        } else {
            return super.getMirrorID(link);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        this.br.setAllowedResponseCodes(410);
        final String fid = downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("/") + 1);
        downloadLink.setLinkID(getHost() + "://" + fid);
        br.getPage(String.format("https://archive.%s/posts/%s.json", this.getHost(), fid));
        final int responsecode = this.br.getHttpConnection().getResponseCode();
        if (responsecode == 403 || responsecode == 404 || responsecode == 410) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJsonValue(this.br, "description");
        if (filename == null || filename.equals("")) {
            /* Fallback */
            filename = fid;
        }
        dllink = PluginJSonUtils.getJsonValue(this.br, "videoUrl");
        if (dllink == null || dllink.equals("")) {
            logger.info("filename: " + filename + ", DLLINK: " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        /* Include linkid in filename to avoid false positive duplicate! */
        filename = filename + ".mp4";
        downloadLink.setFinalFileName(filename);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                server_issues = true;
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* We only download small files, chunkload makes no sense */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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
