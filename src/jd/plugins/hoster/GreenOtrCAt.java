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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "green.otr-c.at" }, urls = { "http://(www\\.)?green\\.otr\\-c\\.at/download/[^<>\"\\']+\\.otrkey" }, flags = { 0 })
public class GreenOtrCAt extends PluginForHost {

    public GreenOtrCAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://green.otr-c.at";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Die Datei befindet sich nicht mehr auf dem Server|<BR>Sorry\\!</B>)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filesize = br.getRegex("<p>Dateigröße: ([^<>\"]*?)</p>").getMatch(0);
        if (filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(new Regex(link.getDownloadURL(), "green\\.otr\\-c\\.at/\\?file=(.+)").getMatch(0)));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "freelink");
        if (dllink == null) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage(br.getURL(), "");
            dllink = br.getRegex("\"link\": \"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* TODO: Fix wait queue handling */
            // for (int i = 0; i <= 400; i++) {
            // if (br.containsHTML("(>Download\\-Link|Der Link ist 24 Stunden lang )")) {
            // break;
            // }
            // br.getPage(downloadLink.getDownloadURL() + "/?start");
            // sleep(27 * 1000l, downloadLink);
            // final String position = br.getRegex(">Position: (\\d+)</TD>").getMatch(0);
            // if (position != null) {
            // logger.info("Warteschlange Position: " + position);
            // downloadLink.getLinkStatus().setStatusText("Warteschlange Position: " + position);
            // }
            // }
            // dllink = br.getRegex("<input type=\"text\" style=\"width:400px;\" value=\"(http://[^<>\"\\']+)\"").getMatch(0);
            // if (dllink == null) {
            // dllink = br.getRegex("<B>Download\\-Link:</B><BR>[\t\n\r ]+<A HREF=\"(http://[^<>\"\\']+)\"").getMatch(0);
            // if (dllink == null) {
            // dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/otrkey/[a-z0-9]+/[^<>\"\\']+)\"").getMatch(0);
            // }
            // }
            // if (dllink == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}