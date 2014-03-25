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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tnaflix.com" }, urls = { "http://(www\\.)?tnaflix\\.com/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)" }, flags = { 0 })
public class TnaFlixCom extends PluginForHost {

    public TnaFlixCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.tnaflix.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String configLink = br.getRegex("addVariable\\(\\'config\\', \\'(http.*?)\\'").getMatch(0);
        if (configLink == null) configLink = br.getRegex("flashvars.config.*?escape\\(.*?(http.*?)\"").getMatch(0);
        if (configLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(Encoding.htmlDecode(configLink));
        String dllink = br.getRegex("<file>(http://.*?)</file>").getMatch(0);
        if (dllink == null) dllink = br.getRegex("<videolink>(http://.*?)</videoLink>").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://tnaflix.com/", "content_filter2", "type%3Dstraight%26filter%3Dcams");
        br.setCookie("http://tnaflix.com/", "content_filter3", "type%3Dstraight%2Ctranny%2Cgay%26filter%3Dcams");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("class=\"errorPage page404\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("errormsg=true")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (downloadLink.getDownloadURL().contains("viewkey=")) {
                downloadLink.setUrlDownload(br.getRedirectLocation());
                br.getPage(downloadLink.getDownloadURL());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String filename = br.getRegex("<title>(.*?), Free Porn.*?</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("playIcon\">(.*?)</h2>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(filename.trim() + ".flv");
        return AvailableStatus.TRUE;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
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