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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hardsextube.com" }, urls = { "http://(www\\.)?hardsextube\\.com/video/[0-9]+/" }, flags = { 0 })
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getURL().contains("?removed=1")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)\\- HardSexTube").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<div style=\\'margin\\-top:\\-10px; height:15px\\'> \\&raquo; <b>(.*?)</b>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<div id=\\'tabdetails\\' style=\" \">.*?<h1>(.*?)</h1>").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final boolean normalViaSite = false;
        if (normalViaSite) {
            final String name = br.getRegex("Name=\"FLVServer\" Value=\"(http://[^<>\"]*?)\"").getMatch(0);
            final String path = br.getRegex("Name=\"FLV\" Value=\"(/[^<>\"]*?)\"").getMatch(0);
            if (name == null || path == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dllink = Encoding.htmlDecode(name + path);
        } else {
            // Via the embedded video stuff we can get the final link without
            // having to decrypt anything
            final String fid = new Regex(downloadLink.getDownloadURL(), "(\\d+)/$").getMatch(0);
            br.getPage("http://vidii.hardsextube.com/video/" + fid + "/confige.xml");
            br.getPage("http://www.hardsextube.com/cdnurl.php?eid=" + new Regex(downloadLink.getDownloadURL(), "(\\d+)/$").getMatch(0) + "&start=0");
            dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = filename.trim();
        String ext = new Regex(dllink, ".+(\\..*?)$").getMatch(0);
        if (ext == null)
            ext = ".flv";
        else if (ext.contains(".mp4")) ext = ".mp4";
        downloadLink.setFinalFileName(filename + ext);

        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dllink);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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