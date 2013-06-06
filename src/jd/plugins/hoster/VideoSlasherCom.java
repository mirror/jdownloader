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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videoslasher.com" }, urls = { "http://(www\\.)?videoslasher\\.com/video/[A-Z0-9]+" }, flags = { 0 })
public class VideoSlasherCom extends PluginForHost {

    public VideoSlasherCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.videoslasher.com/terms";
    }

    private static final String MAINTENANCE         = ">The service VideoSlasher is currently down for maintenance";
    private static final String MAINTENANCEUSERTEXT = JDL.L("hoster.videoslashercom.errors.undermaintenance", "This server is under Maintenance");

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        // Invalid link
        if (br.getURL().equals("http://www.videoslasher.com/404") || br.getURL().equals("http://www.videoslasher.com/login")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Link offline
        if (br.containsHTML(">This material has been removed due to infringement")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(MAINTENANCE)) {
            link.getLinkStatus().setStatusText(MAINTENANCEUSERTEXT);
            return AvailableStatus.TRUE;
        }
        final Regex fileInfo = br.getRegex("<h1 style=\"margin:20px 20px 10px 20px\">([^<>\"]*?) \\((\\d+(,\\d+)? [A-Za-z]{1,5})\\)</h1>");
        String filename = fileInfo.getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?) \\| VideoSlasher\\.com</title>").getMatch(0);
        final String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".flv");
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(MAINTENANCE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, MAINTENANCEUSERTEXT, 2 * 60 * 60 * 1000l);
        br.postPage(br.getURL(), "foo=bar&confirm=Close+Ad+and+Watch+as+Free+User");
        if (br.containsHTML(">This material has been removed due to infringement|>Not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String playlist = br.getRegex("\\'(/playlist/[a-z0-9]+)\\'").getMatch(0);
        if (playlist == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.videoslasher.com" + playlist);
        if (br.containsHTML("/img/temp_not_available\\.gif\"")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        String dllink = br.getRegex("\"(http://(www\\.)?[a-z0-9]+\\.videoslasher\\.com/free/[^<>\"]*?)\" ").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -10);
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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}