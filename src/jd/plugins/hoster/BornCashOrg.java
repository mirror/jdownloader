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

import org.appwork.utils.formatter.SizeFormatter;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "borncash.org" }, urls = { "http://(www\\.)?borncash\\.org/(load/|download/\\?a=|dw/\\?a=)\\d+" })
public class BornCashOrg extends PluginForHost {

    public BornCashOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.borncash.org/sogl.htm";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.borncash.org/load/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL() + "&lang=en");
        if (br.containsHTML("borncash\\.org/dw/del") || br.containsHTML("redirectfiles\\.ru/error/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // meta redirect
        if (br.containsHTML("<meta http-equiv=")) {
            final String redirect = br.getRegex("<meta http-equiv=('|\")refresh\\1\\s*content=(\"|')\\d+;url=(.*?)\\2").getMatch(2);
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(redirect);
        }
        final String filename = br.getRegex("file\"><b>([^<>]+)</b>").getMatch(0);
        final String filesize = br.getRegex("file\"><b>[^<>]+</b>\\s*\\(([\\d\\.]+\\s*[KMG]B)\\)<").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            br.postPage(br.getURL(), "form_1=");
            // br.postPage(br.getURL(), "form_3=");
            br.postPage(br.getURL(), "off_free=");
            int wait = 60;
            final String waittime = br.getRegex("sec=(\\d+);").getMatch(0);
            if (waittime != null) {
                wait = Integer.parseInt(waittime);
            }
            dllink = br.getRegex("\"(http://(www\\.)?borncash\\.org/dw/zagruska\\.php\\?url=http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // sleep(wait * 1001l, downloadLink);
            br.getPage(dllink);
            dllink = br.getRegex("HTTP\\-EQUIV=\\'Refresh\\' CONTENT=\\'\\d+; URL=(http://[^<>\"]*?)\\'>").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Скачивать файлы без VIP доступа Вы можете не чаще")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 60 * 1001l);
            }
            // This should never happen
            if (br.containsHTML("There is a limit on the number of <b>simultaneous</b>")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 2 * 60 * 1000l);
            }
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
            } catch (Exception e) {
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