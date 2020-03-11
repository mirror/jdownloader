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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "SimpleTubes" }, urls = { "http://(www\\.)?(anyporn|sexu|xbabe)\\.com/(\\d+|videos/[^<>/]+)/" })
public class SimpleTubes extends PluginForHost {

    private String dllink            = null;
    private String customFavIconHost = null;

    public SimpleTubes(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (br.getURL().contains("anyporn.com")) {
            // video_url: 'http://anyporn.com/get_file/1/def52ed5f66c5c8d91b5f55dc61a20d9/6000/6xxx/6xxx.mp4/'
            dllink = br.getRegex("video_url: \'(http://.*?)/\'").getMatch(0);
        } else if (br.getURL().contains("sexu.com")) {
            filename = br.getRegex("<title>(.*?) - Sexu.Com</title>").getMatch(0);
            // file":"http:\/\/v.sexu.com\/key=hSuSvdcrVR.,end=1415352725\/sexu\/8a\/374976-480p-x.mp4","label":"480p
            dllink = br.getRegex("file\":\"(http:[^<>:]+)\",\"label\":\"480p").getMatch(0);
            dllink = dllink.replace("\\", "");
        } else if (br.getURL().contains("xbabe.com")) {
            filename = br.getRegex("<title>(.*?) - XBabe</title>").getMatch(0);
            // video_alt_url: 'http://xbabe.com/get_file/3/eec3d5edbe58da4bbd6cee5769cde927/95000/95918/95918_360p.mp4/?br=453'
            dllink = br.getRegex("video_alt_url: \'(http://.*?/)\\?br=\\d+\'").getMatch(0);
        }
        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        filename = filename.trim();
        String ext = dllink.substring(dllink.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = ".flv";
        }
        ext = ext.replace("/", ""); // .mp4/ => .mp4
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
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

    public String getCustomFavIconURL(final DownloadLink link) {
        if (link != null) {
            final String domain = Browser.getHost(link.getDownloadURL(), true);
            if (domain != null) {
                return domain;
            }
        }
        if (this.customFavIconHost != null) {
            return this.customFavIconHost;
        }
        return null;
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