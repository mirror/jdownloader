//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "miloyski.com" }, urls = { "http://(www\\.)?(beta\\.)?miloyski\\.com/video\\.html\\?i=[a-z0-9]+" }, flags = { 0 })
public class MiloyskiCom extends PluginForHost {

    public MiloyskiCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://beta.miloyski.com/terms_of_use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">404 Error: video does not exist\\!")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = br.getRegex("<title>([^<>\"]*?)\\- Miloyski</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("target=\"_blank\">Watch ([^<>\"]*?) Videos</a>").getMatch(0);
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Form dlf = br.getForm(0);
        if (dlf == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dlf.remove("submit");
        dlf.put("submit", "download");
        /* Waittime can be skipped */
        // final String waittime = br.getRegex("id=\"count\">(\\d+)<").getMatch(0);
        // int wait = 9;
        // if (waittime != null) wait = Integer.parseInt(waittime);
        // this.sleep(wait * 1001l, downloadLink);
        br.submitForm(dlf);
        String dllink = br.getRegex("SaveToDisk\\(\\'(http://[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(https?://f\\.miloyski.com/files/[^<>\"]*?)\\'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        try {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("http://beta.miloyski.com/track.php", "server=f");
        } catch (final Throwable e) {

        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}