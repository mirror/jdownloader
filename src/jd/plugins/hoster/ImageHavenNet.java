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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imagehaven.net" }, urls = { "http://[\\w\\.]*?[a-z]{1,4}[0-9]{1,2}\\.imagehaven\\.net/img\\.php\\?id=.+\\.[a-z]+" }, flags = { 0 })
public class ImageHavenNet extends PluginForHost {

    private String checklink;

    public ImageHavenNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://imagehaven.net/index.php?page=tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        checklink = null;
        br.getPage(downloadLink.getDownloadURL());
        String server = new Regex((downloadLink.getDownloadURL()), "(http://[a-z]{1,4}[0-9]{1,2}\\.imagehaven\\.net)").getMatch(0);
        if (br.containsHTML("This ad is shown once a day.<")) {
            br.getPage(downloadLink.getDownloadURL());
        }
        checklink = br.getRegex("<img src='\\.(.*?)'").getMatch(0);
        if (checklink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        checklink = server + checklink;
        URLConnectionAdapter con = br.openGetConnection(checklink);
        if ((con.getContentType().contains("html"))) {
            br.followConnection();
            if (br.containsHTML("404 - Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            downloadLink.setDownloadSize(con.getLongContentLength());
            con.disconnect();
        }
        String filename = new Regex(checklink, "imagehaven\\.net/images/.*?/.*?/.*?_(.*?\\.[a-zA-Z]+)").getMatch(0);
        if (filename != null) {
            downloadLink.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, checklink, true, 0);
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