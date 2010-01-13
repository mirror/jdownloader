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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xnxx.com" }, urls = { "http://[\\w\\.]*?video\\.xnxx\\.com/video[0-9]+" }, flags = { 0 })
public class XnXxCom extends PluginForHost {

    public XnXxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.xnxx.com/contact.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        // The regex only takes the short urls but these ones redirect to the
        // real ones to if follow redirects is false the plugin doesn't work at
        // all!
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(Page not found|This page may be in preparation, please check back in a few minutes)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)- XNXX\\.COM</title>").getMatch(0);
        if (filename == null) {
            br.getRegex("<span class=\"style5\"><strong>(.*?)</strong>").getMatch(0);
            if (filename == null) {
                br.getRegex("name=description content=\"(.*?)free sex video").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (!br.containsHTML(".mp4")) {
            downloadLink.setFinalFileName(filename.trim() + ".flv");
        } else {
            downloadLink.setFinalFileName(filename.trim() + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("flv_url=(http.*?)\\&amp;").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
