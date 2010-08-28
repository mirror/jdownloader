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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedload.to" }, urls = { "http://[\\w\\.]*?speedload\\.to/FILE[A-Z0-9]+" }, flags = { 0 })
public class SpeedLoadTo extends PluginForHost {

    public SpeedLoadTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://speedload.to/Disclaimer.go";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<h1>The desired file could not be found\\.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("name=\"minVersion\"/>[\t\n\r ]+<param value=\"(.*?)\" name=\"movieTitle\"").getMatch(0);
        if (filename == null) filename = br.getRegex("movietitle=\"(.*?)\"").getMatch(0);
        dllink = br.getRegex("autoplay=\"true\" custommode=\"none\" src=\"(.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://speedload\\.to/FILE[A-Z0-9]+\\.plong)\"").getMatch(0);
        if (filename == null || dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        downloadLink.setFinalFileName(filename);
        String filesize = br.getRegex("class=\"Label\">Größe:</td>[\t\n\r ]+<td class=\"ValueX\">(.*?)</td>").getMatch(0);
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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
