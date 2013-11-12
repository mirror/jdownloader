//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "veehd.com" }, urls = { "http://(www\\.)?veehd\\.com/video/\\d+" }, flags = { 0 })
public class VeeHdCom extends PluginForHost {

    public VeeHdCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://veehd.com/guidelines";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This is a private video") || br.getURL().contains("/?removed=") || br.containsHTML("This video has been removed due")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2 style=\"\">([^<>]*?) \\| <font").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>]*?) on Veehd</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()).replace("\"", "'") + ".avi");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String ts = br.getRegex("var ts = \"([^<>\"]*?)\"").getMatch(0);
        final String sign = br.getRegex("var sgn = \"([^<>\"]*?)\"").getMatch(0);
        final String frame = br.getRegex("\"(/vpi\\?h=[^<>\"]*?)\"").getMatch(0);
        if (frame == null || ts == null || sign == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Pretend to use their stupid toolbar
        br.postPage("http://veehd.com/xhrp", "v=c2&p=1&ts=" + Encoding.urlEncode(ts) + "&sgn=" + Encoding.urlEncode(sign));
        br.getPage("http://veehd.com" + frame);
        String dllink = br.getRegex("\"(http://v\\d+\\.veehd\\.com/dl/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            // Only available when plugin needed
            if (dllink == null) dllink = br.getRegex("<embed type=\"video/divx\" src=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
        if (dllink == null) {
            if (br.containsHTML("Plugin to watch this video")) logger.warning("Maybe toolbar fail!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink.trim());
        String finalfilename = downloadLink.getName();
        if (dllink.contains(".mp4")) finalfilename = finalfilename.replace(".avi", ".mp4");
        downloadLink.setFinalFileName(finalfilename);
        // More chunks possible but will cause server errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 30 * 30 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        // More downloads possible but will cause server errors
        return 3;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}