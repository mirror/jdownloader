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
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4sync.com" }, urls = { "http://(www\\.)?4sync\\.com/(rar|file)/[A-Za-z0-9]+" }, flags = { 0 })
public class FourSyncCom extends PluginForHost {

    public FourSyncCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.4sync.com/terms.jsp";
    }

    public void correctDownloadLink(final DownloadLink link) {
        final Regex reg = new Regex(link.getDownloadURL(), "4sync\\.com/[a-z0-9]+/([A-Za-z0-9]+)");
        link.setUrlDownload("http://www.4sync.com/file/" + reg.getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.4sync.com/", "4langcookie", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">4Sync \\- Page not found<|>The webpage you've requested wasn't found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex finfo = br.getRegex("class=\"img\"/><b>([^<>\"]*?)</b> \\(([^<>\"]*?)\\) <table align=\"center\"");
        String filename = br.getRegex("<span id=\"fileNameTextSpan\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null) filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) filename = br.getRegex("alt=\"([^<>\"]*?)\"/><br><br> Click here to download this file").getMatch(0);
        if (filename == null) filename = finfo.getMatch(0);
        String filesize = br.getRegex("title=\"Size: ([^<>\"]*?)\">").getMatch(0);
        if (filesize == null) filesize = finfo.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", "")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.getURL().contains("errorMaxSessions=MAX_IP")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
        final String dllink = br.getRegex("\"(http://[a-z0-9]+\\.4sync\\.com/download/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            if (!br.containsHTML("Download now\\&nbsp;")) throw new PluginException(LinkStatus.ERROR_RETRY);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("errorMaxSessions=MAX_IP")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}