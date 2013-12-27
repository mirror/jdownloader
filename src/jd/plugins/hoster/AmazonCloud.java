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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amazon.com" }, urls = { "http://(www\\.)?amazon\\.(de|es)/gp/drive/share\\?ie=UTF8\\&s=[A-Za-z0-9\\-_]+" }, flags = { 0 })
public class AmazonCloud extends PluginForHost {

    public AmazonCloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.amazon.de/gp/help/customer/display.html/ref=ap_footer_condition_of_use?ie=UTF8&nodeId=505048&pop-up=1";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("id=\"error_page\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("fileName = \"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("fSize = \"(\\d+)\"").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String deviceserial = br.getRegex("sNum = \"([^<>\"]*?)\"").getMatch(0);
        if (deviceserial == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String domain = new Regex(br.getURL(), "(amazon\\.[a-z]+)/").getMatch(0);
        final String shareid = new Regex(downloadLink.getDownloadURL(), "\\&s=(.+)").getMatch(0);
        final String getlink = "http://www." + domain + "/gp/drive/share/downloadFile.html?_=" + System.currentTimeMillis() + "&sharedId=" + Encoding.urlEncode(shareid) + "&download=TRUE&deviceType=ubid&deviceSerialNumber=" + deviceserial;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(getlink);
        final String dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}