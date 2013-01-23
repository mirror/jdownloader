//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 19117 $", interfaceVersion = 2, names = { "temp-share.com" }, urls = { "https?://(www\\.)?temp-share\\.com/show/[a-zA-Z0-9]{9}" }, flags = { 0 })
public class TempShareCom extends PluginForHost {

    private static final String mainPage = "http://temp-share.com";

    /**
     * @author raztoki
     */
    public TempShareCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public void prepBrowser(final Browser br) {
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
    }

    @Override
    public String getAGBLink() {
        return mainPage + "/company/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(br);
        br.getPage(link.getDownloadURL());

        if (br.containsHTML(">Time has expired for this file.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filter = "<h1>([^<]+)</h1><div></div><h2>(\\d+(\\.\\d+)? (B|KB|MB|GB)) </h2>";
        String filename = br.getRegex(filter).getMatch(0);
        if (filename == null) br.getRegex("<title>Temp Share : (.*?) \\|").getMatch(0);
        String filesize = br.getRegex(filter).getMatch(1);

        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        if (filesize != null && !filesize.equals("")) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("<a href=\\'(/download/[a-z0-9]+)").getMatch(0);
        if (dllink == null) {
            logger.warning("Could not find 'dllink', please report this issue to the JDownloader Development Team");
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}