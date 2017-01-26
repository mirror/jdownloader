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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "iceporn.com" }, urls = { "https?://(?:www\\.)?iceporn\\.com/embed/\\d+|https?://(?:www\\.)?iceporn\\.com/video/\\d+(?:/[a-z0-9\\-]+)?" })
public class IcepornCom extends PluginForHost {

    /* Tags: viptube.com, nuvid.com */
    private String dllink = null;

    public IcepornCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.iceporn.com/static/terms";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.iceporn.com/video/" + new Regex(link.getDownloadURL(), "/(?:video|embed)/(\\d+)").getMatch(0));
    }

    private void getDllink() throws IOException {
        final boolean preferMobile = true;
        if (preferMobile) {
            /* Usually we'll get a .mp4 here, quality is lower than via website. */
            final String videoid = new Regex(this.br.getURL(), "/video/(\\d+)").getMatch(0);
            if (videoid != null) {
                this.br.getPage("http://m." + this.getHost() + "/video/" + videoid);
                dllink = this.br.getRegex("<source src=\"(http[^<>\"]+\\.(?:mp4|flv)[^<>\"\\']+)").getMatch(0);
            }
        } else {
            /* 2017-01-23: pkey generation is wrong - fallback to mobile version of the website! */
            final String h = this.br.getRegex("h=([a-z0-9]+)\\'").getMatch(0);
            final String t = this.br.getRegex("t=([0-9]+)").getMatch(0);
            String vkey = this.br.getRegex("vkey=([a-z0-9]+)").getMatch(0);
            if (vkey == null) {
                /* 2017-01-23 */
                vkey = this.br.getRegex("vkey=\\'\\s*?\\+\\s*?\\'([a-z0-9]+)\\'").getMatch(0);
            }
            if (h != null && t != null && vkey != null) {
                br.getPage("http://www." + this.getHost() + "/player_config/?h=" + h + "&check_speed=1&t=" + t + "&vkey=" + vkey + "&pkey=" + JDHash.getMD5(vkey + Encoding.Base64Decode("null_TODO")) + "&aid=&domain_id=");
                dllink = br.getRegex("<video_file>(http://.*?)</video_file>").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("<video_file><\\!\\[CDATA\\[(http://.*?)\\]\\]></video_file>").getMatch(0);
                }
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = new Regex(downloadLink.getDownloadURL(), "/video/(\\d+)").getMatch(0);
        String filename = br.getRegex("<title>([^<>\"]+) \\- Porn Tube[^<>]*?</title>").getMatch(0);
        if (filename == null) {
            /* Fallback */
            filename = fid;
        }
        getDllink();
        final String ext;
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            ext = getFileNameExtensionFromString(dllink, ".mp4");
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                if (con.getResponseCode() == 404) {
                    /*
                     * Small workaround for buggy servers that redirect and fail if the Referer is wrong then. Examples: hdzog.com
                     */
                    final String redirect_url = con.getRequest().getUrl();
                    con = br2.openHeadConnection(redirect_url);
                }
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            ext = ".mp4";
        }
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return null;
    }

}