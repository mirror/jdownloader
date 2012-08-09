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
import jd.http.RandomUserAgent;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nowdownload.eu" }, urls = { "http://(www\\.)?nowdownload\\.(eu|co)/dl/[a-z0-9]+" }, flags = { 0 })
public class NowDownloadEu extends PluginForHost {

    public NowDownloadEu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.nowdownload.eu/terms.php";
    }

    private static final String MAINPAGE = "http://www.nowdownload.eu";
    private static final String ua       = RandomUserAgent.generate();

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("nowdownload.co/", "nowdownload.eu/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">This file does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final Regex fileInfo = br.getRegex(">Downloading</span> <br> (.*?) ([\\d+\\.]+ (B|KB|MB|GB|TB))");
        String filename = fileInfo.getMatch(0).replace("<br>", "");
        String filesize = fileInfo.getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = (checkDirectLink(downloadLink, "directlink"));
        if (dllink == null) {
            final String tokenPage = br.getRegex("\"(/api/token\\.php\\?token=[a-z0-9]+)\"").getMatch(0);
            final String continuePage = br.getRegex("\"(/dl2/[a-z0-9]+/[a-z0-9]+)\"").getMatch(0);
            if (tokenPage == null || continuePage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            int wait = 30;
            final String waittime = br.getRegex("\\.countdown\\(\\{until: \\+(\\d+),").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getPage(MAINPAGE + tokenPage);
            sleep(wait * 1001l, downloadLink);
            br.getPage(MAINPAGE + continuePage);
            dllink = br.getRegex("\"(http://f\\d+\\.nowdownload\\.eu/dl/[a-z0-9]+/[a-z0-9]+/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("<p class=\"top30\"><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String filename = new Regex(dllink, ".+/[^_]+_(.+)").getMatch(0);
            if (filename != null) downloadLink.setFinalFileName(Encoding.urlDecode(filename, false));
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
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