//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hardsextube.com" }, urls = { "http://(www\\.)?hardsextube\\.com/(video|embed)/\\d+" }, flags = { 0 })
public class HardSexTubeCom extends PluginForHost {

    public String dllink = null;

    public HardSexTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.hardsextube.com/register/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.hardsextube.com/video/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + "/");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "(\\d+)/$").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (!br.getURL().contains("/video/") || br.getRedirectLocation() != null || br.getHttpConnection().getResponseCode() == 302 || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<meta itemprop=\"name\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h1 class=\"title-block\">([^<>\"]*?)</h1>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final boolean normalViaSite = false;
        if (normalViaSite) {
            final String name = br.getRegex("\\&flvserver=(http://[^<>\"]*?)\\&").getMatch(0);
            final String path = br.getRegex("\\&flv=(/content[^<>\"]*?)\\&start=").getMatch(0);
            if (name == null || path == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(name + path);
        } else {
            // Via the embedded video stuff we can get the final link without
            // having to decrypt anything
            br.setFollowRedirects(false);
            final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)/$").getMatch(0);
            br.getPage("http://www.hardsextube.com/embed/" + fid + "/");
            final String redirect = br.getRedirectLocation();
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String name = new Regex(redirect, "\\&flvserver=(http://[^<>\"]*?)\\&").getMatch(0);
            final String path = new Regex(redirect, "\\&flv=(/embed[^<>\"]*?)\\&start=").getMatch(0);
            if (name == null || path == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.htmlDecode(name + path);
            // br.getPage("http://vidii.hardsextube.com/video/" + fid + "/confige.xml");
            // final String cdnurl = br.getRegex("\"(/cdnurl\\.php[^<>\"]*?)\"").getMatch(0);
            // br.getPage("http://www.hardsextube.com" + cdnurl);
            // br.getPage("http://www.hardsextube.com/cdnurl.php?eid=" + new Regex(downloadLink.getDownloadURL(), "(\\d+)/$").getMatch(0) +
            // "&start=0");
            // dllink = br.getRedirectLocation();
            // if (dllink == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
        }
        filename = filename.trim();
        String ext = new Regex(dllink, ".+(\\..*?)$").getMatch(0);
        if (ext == null) {
            ext = ".flv";
        } else if (ext.contains(".mp4")) {
            ext = ".mp4";
        }
        downloadLink.setFinalFileName(filename + ext);

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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