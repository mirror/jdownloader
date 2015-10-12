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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "voyeurhit.com" }, urls = { "http://(www\\.)?voyeurhit\\.com/videos/[^/]+" }, flags = { 0 })
public class VoyeurHitCom extends PluginForHost {

    // Porn_get_file_/videos/_basic Version 0.X
    private String dllink = null;

    public VoyeurHitCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://voyeurhit.com/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    /* Extension which will be used if no correct extension is found */
    private static final String default_Extension = ".mp4";

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!br.getURL().contains("voyeurhit.com/videos/") || br.containsHTML(">Oops! Page not found|This video is a private video")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        // video_url: 'http://voyeurhit.com/get_file/1/020fe1f099fb36263435405241261583/16000/16688/16688_lq.mp4/?br=293',
        // 'file': "http://voyeurhit.com/get_file/1/020fe1f099fb36263435405241261583/16000/16688/16688_lq.mp4/?br=293",
        dllink = br.getRegex("video_url: \'(http://.*?)/\\?br=\\d+\'").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\'file\': \"(http://voyeurhit.com/get_file/.*?)/\\?br=\\d+\"").getMatch(0);
        }
        if (filename == null || dllink == null) {
            logger.info("filename = " + filename + ", dllink = " + dllink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        String ext = dllink.substring(dllink.lastIndexOf("."));
        /* Make sure that we get a correct extension */
        if (ext == null || !ext.matches("\\.[A-Za-z0-9]{3,5}")) {
            ext = default_Extension;
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        if (downloadLink.getDownloadSize() > 0) { // Get size once only
            return AvailableStatus.TRUE;
        } else {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    /* Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com */
                    final String redirect_url = this.br.getHttpConnection().getRequest().getUrl();
                    con = this.br.openHeadConnection(redirect_url);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
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
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 3);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
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