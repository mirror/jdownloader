//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "beeg.com" }, urls = { "http://(www\\.)?beeg\\.com/((?!section)[a-z0-9\\-]+/[a-z0-9\\-]+|\\d+)" }, flags = { 0 })
public class BeegCom extends PluginForHost {

    private String DLLINK = null;

    public BeegCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://beeg.com/contacts/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL().toLowerCase());
        // Link offline
        if (br.containsHTML("(<h1>404 error \\- Page Not found</h1>|<title>beeg\\. \\— Page Not Found\\. \\(Error 404\\)</title>|<p>In about 5 seconds, you will be automatically redirected to the main page| the page you’re looking for can\\'t be found\\. May be invalid or outdated\\.</h2>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Invalid link
        if (br.containsHTML("404 2") || br.getURL().equals("http://beeg.com/")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?) \\(Bang Bros[^<>]+</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<meta name=\"description\" content=\"([^\"<>]+)\"").getMatch(0);
        DLLINK = br.getRegex("\\'file\\': \\'(https?://[^\\'\"\\,]+)\\'").getMatch(0);
        if (DLLINK == null) DLLINK = br.getRegex("\\'file\\'(,|: )\\'(http://.*?)\\'").getMatch(1);
        if (DLLINK == null) DLLINK = br.getRegex("\\'(http://\\d+\\.video\\.mystreamservice\\.com/default/[a-z0-9\\-]+\\.flv)\\'").getMatch(0);
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
        if (ext == null || ext.length() > 5) ext = ".flv";
        filename = filename.trim();
        downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}