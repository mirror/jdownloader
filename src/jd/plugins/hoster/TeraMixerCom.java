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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "teramixer.com" }, urls = { "http://(www\\.)?teramixer\\.com/(embed/)?[a-z0-9]+" }, flags = { 0 })
public class TeraMixerCom extends PluginForHost {

    public TeraMixerCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.teramixer.com/terms.html";
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        final String fid = new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        link.setUrlDownload("http://www.teramixer.com/" + fid);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", jd.plugins.hoster.MediafireCom.stringUserAgent());
        br.getPage(link.getDownloadURL());
        if (br.getURL().contains("/404.html")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">You have reached the maximum concurrent downloads")) {
            link.getLinkStatus().setStatusText("Cannot get file information while limit is reached");
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("<title>([^<>\"]*?) \\- Teramixer</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("Filename[\t\n\r ]+</td>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        }
        String filesize = br.getRegex("class=\"icon\\-hdd\"></i> ([^<>\"]*?) </span>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\\((\\d+(\\.\\d+)? [A-Za-z]{2,5})\\)</h3>").getMatch(0);
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filesize = filesize.replace("ko", "kb");
        filesize = filesize.replace("Mo", "MB");
        filesize = filesize.replace("Go", "GB");
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (br.containsHTML(">You have reached the maximum concurrent downloads")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
            }
            dllink = br.getRegex("var filepath = \\'([A-Za-z0-9=]+)\\'").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Encoding.Base64Decode(dllink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
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