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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "amazon.com" }, urls = { "https://amazondecrypted\\.com/\\d+" }, flags = { 0 })
public class AmazonCloud extends PluginForHost {

    public AmazonCloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.amazon.de/gp/help/customer/display.html/ref=ap_footer_condition_of_use?ie=UTF8&nodeId=505048&pop-up=1";
    }

    public AvailableStatus requestFileInformationOld(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String url = "https://www.amazon.com/clouddrive/share?s=" + link.getStringProperty("plain_folder_id");
        br.getPage(url);
        if (br.containsHTML("id=\"error_page\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("fileName = \"([^<>\"]*?)\"").getMatch(0);
        final String filesize = br.getRegex("fSize = \"(\\d+)\"").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if ("old20140922".equals(link.getStringProperty("type"))) {
            return requestFileInformationOld(link);
        }
        if (link.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!link.getDownloadURL().matches("https://amazondecrypted\\.com/\\d+")) {
            /* Check if user still has old links in his list --> Invalid */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        this.setBrowserExclusive();
        prepBR();
        final String plain_folder_id = link.getStringProperty("plain_folder_id", null);
        final String plain_domain = link.getStringProperty("plain_domain", null);
        br.getPage("https://www." + plain_domain + "/drive/v1/shares/" + plain_folder_id + "?customerId=0&ContentType=JSON&asset=ALL");
        if (br.containsHTML("id=\"error_page\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_name", null);
        final String filesize = link.getStringProperty("plain_size", null);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    public void handleFreeOld(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformationOld(downloadLink);
        final String deviceserial = br.getRegex("sNum = \"([^<>\"]*?)\"").getMatch(0);
        if (deviceserial == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String domain = new Regex(br.getURL(), "(amazon\\.[a-z]+)/").getMatch(0);
        final String shareid = downloadLink.getStringProperty("plain_folder_id");
        final String getlink = "http://www." + domain + "/gp/drive/share/downloadFile.html?_=" + System.currentTimeMillis() + "&sharedId=" + Encoding.urlEncode(shareid) + "&download=TRUE&deviceType=ubid&deviceSerialNumber=" + deviceserial;
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getPage(getlink);
        final String dllink = br.getRegex("\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if ("old20140922".equals(downloadLink.getStringProperty("type"))) {
            handleFreeOld(downloadLink);
            return;
        }
        requestFileInformation(downloadLink);
        final String dllink = downloadLink.getStringProperty("plain_directlink", null);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepBR() {
        br.setFollowRedirects(true);
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
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