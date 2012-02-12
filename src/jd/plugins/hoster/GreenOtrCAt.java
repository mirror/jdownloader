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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "green.otr-c.at" }, urls = { "http://(www\\.)?green\\.otr\\-c\\.at/(\\?file=|download/)[^<>\"\\']+\\.otrkey" }, flags = { 0 })
public class GreenOtrCAt extends PluginForHost {

    public GreenOtrCAt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/?file=", "/download/"));
    }

    @Override
    public String getAGBLink() {
        return "http://green.otr-c.at";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Die Datei befindet sich nicht mehr auf dem Server|<BR>Sorry\\!</B>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(Encoding.htmlDecode(new Regex(link.getDownloadURL(), "green\\.otr\\-c\\.at/\\?file=(.+)").getMatch(0)));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        if (dllink == null) {
            for (int i = 0; i <= 400; i++) {
                if (br.containsHTML("(>Download\\-Link|Der Link ist 24 Stunden lang )")) break;
                br.getPage(downloadLink.getDownloadURL() + "/?start");
                sleep(27 * 1000l, downloadLink);
                final String position = br.getRegex(">Position: (\\d+)</TD>").getMatch(0);
                if (position != null) {
                    logger.info("Warteschlange Position: " + position);
                    downloadLink.getLinkStatus().setStatusText("Warteschlange Position: " + position);
                }
            }
            dllink = br.getRegex("<input type=\"text\" style=\"width:400px;\" value=\"(http://[^<>\"\\']+)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<B>Download\\-Link:</B><BR>[\t\n\r ]+<A HREF=\"(http://[^<>\"\\']+)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/otrkey/[a-z0-9]+/[^<>\"\\']+)\"").getMatch(0);
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}