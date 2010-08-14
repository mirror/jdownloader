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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tnaflix.com" }, urls = { "http://[\\w\\.]*?tnaflix\\.com/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)" }, flags = { 0 })
public class TnaFlixCom extends PluginForHost {

    public TnaFlixCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tnaflix.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("errormsg=true")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (downloadLink.getDownloadURL().contains("viewkey=")) {
                downloadLink.setUrlDownload(br.getRedirectLocation());
                br.getPage(downloadLink.getDownloadURL());
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = br.getRegex("<title>(.*?), Free Porn.*?</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("playIcon\">(.*?)</h2>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("videoDescription\">(.*?)</span>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("description\" content=\"(.*?)Watch Free").getMatch(0);
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        if (!filename.endsWith(".")) {
            downloadLink.setFinalFileName(filename + ".flv");
        } else {
            downloadLink.setFinalFileName(filename + "flv");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String vidlink = br.getRegex("addVariable\\('config', '(.*?)'\\)").getMatch(0);
        if (vidlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        vidlink = Encoding.urlDecode((vidlink), true);
        if (!vidlink.contains("cdn.tnaflix.com")) vidlink = "http://cdn.tnaflix.com/" + vidlink;
        br.getPage(vidlink);
        String dllink = br.getRegex("<file>(.*?)</file>").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("amp;", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
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
