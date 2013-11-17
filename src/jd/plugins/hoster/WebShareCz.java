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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "webshare.cz" }, urls = { "https?://(www\\.)?webshare\\.cz/(\\?fhash=[A-Za-z0-9]+|[A-Za-z0-9]{10}|(#/)?file/[a-z0-9]+)" }, flags = { 0 })
public class WebShareCz extends PluginForHost {

    public WebShareCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://webshare.cz/podminky.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://webshare.cz/file/" + new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0) + "/");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("https://webshare.cz/api/file_info/", "wst=&ident=" + getFID(link));
        if (br.containsHTML("<status>FATAL</status>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = getXMLtagValue("name");
        final String filesize = getXMLtagValue("size");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.postPage("https://webshare.cz/api/file_link/", "wst=&ident=" + getFID(downloadLink));
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.getHeaders().put("Referer", null);
        br.getHeaders().put("X-Requested-With", null);
        final String dllink = getXMLtagValue("link");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(>Požadovaný soubor nebyl nalezen|>Requested file not found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.getURL().contains("error=")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getXMLtagValue(final String tagname) {
        return br.getRegex("<" + tagname + ">([^<>\"]*?)</" + tagname + ">").getMatch(0);
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "file/([A-Za-z0-9]+)/").getMatch(0);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}